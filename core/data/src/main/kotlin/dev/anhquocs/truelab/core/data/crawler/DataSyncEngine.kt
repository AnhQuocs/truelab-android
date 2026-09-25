package dev.anhquocs.truelab.core.data.crawler

import dev.anhquocs.truelab.core.data.crawler.model.SyncResult
import dev.anhquocs.truelab.core.data.crawler.model.SyncSummary
import dev.anhquocs.truelab.core.data.crawler.retry.RetryExecutor
import dev.anhquocs.truelab.core.data.league.local.entity.LeagueEntity
import dev.anhquocs.truelab.core.data.league.local.entity.SeasonEntity
import dev.anhquocs.truelab.core.data.local.database.TrueLabDatabase
import dev.anhquocs.truelab.core.data.local.mapper.RoomMappers.toAwayTeamEntity
import dev.anhquocs.truelab.core.data.local.mapper.RoomMappers.toEntity
import dev.anhquocs.truelab.core.data.local.mapper.RoomMappers.toHomeTeamEntity
import dev.anhquocs.truelab.core.data.local.mapper.RoomMappers.toMatchEntity
import dev.anhquocs.truelab.core.data.match.remote.api.MatchApi
import dev.anhquocs.truelab.core.data.odds.remote.api.OddsApi
import dev.anhquocs.truelab.core.data.ranking.remote.api.RankingApi
import dev.anhquocs.truelab.core.domain.metadata.repository.DatasetMetadataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * DataSyncEngine provides an idempotent, deterministic data synchronization foundation.
 *
 * Adheres to the multi-level dependency order:
 * Leagues/Seasons -> Teams -> Matches -> Rankings/Odds -> Metadata Hook
 */
class DataSyncEngine @Inject constructor(
    private val matchApi: MatchApi,
    private val oddsApi: OddsApi,
    private val rankingApi: RankingApi,
    private val database: TrueLabDatabase,
    private val json: Json,
    private val metadataRepository: DatasetMetadataRepository? = null,
    private val retryExecutor: RetryExecutor = RetryExecutor()
) {

    /**
     * Đồng bộ đầy đủ danh sách trận đấu và dữ liệu liên quan theo ngày với thứ tự phụ thuộc xác định:
     * 1. Teams (Deduplicated & Inserted with IGNORE to preserve Elo/Form)
     * 2. Matches (Batch inserted with REPLACE)
     * 3. Rankings & Odds (Optional per match)
     * 4. Metadata Hook
     */
    suspend fun syncFullPipelineForDate(
        date: String,
        syncOddsAndRankings: Boolean = false,
        onMetadataHook: (suspend (timestamp: Long, matchesSynced: Int, teamsSynced: Int) -> Unit)? = null
    ): SyncResult<SyncSummary> = withContext(Dispatchers.IO) {
        try {
            var currentPage = 1
            var totalPages = 1
            var totalMatchesSynced = 0
            var totalTeamsSynced = 0
            var totalOddsSynced = 0
            var totalRankingsSynced = 0

            while (currentPage <= totalPages) {
                // 1. Fetch remote matches page with retry
                val response = retryExecutor.execute {
                    matchApi.getMatches(date = date, page = currentPage)
                }
                val matchRecords = response.data.data

                if (matchRecords.isEmpty()) break

                // 2. Map & Deduplicate Teams per batch
                val teams = matchRecords.flatMap {
                    listOf(it.toHomeTeamEntity(), it.toAwayTeamEntity())
                }.distinctBy { it.id }

                val matches = matchRecords.map { it.toMatchEntity() }

                // 3. Atomic Transaction per page (Teams -> Matches)
                database.runInTransaction {
                    database.teamDao().insertTeams(teams)
                    database.matchDao().insertMatches(matches)
                }

                totalMatchesSynced += matches.size
                totalTeamsSynced += teams.size

                // 4. Optional secondary synchronization (Rankings & Odds per match)
                if (syncOddsAndRankings) {
                    for (match in matches) {
                        val oddsCount = syncOddsInternal(match.id)
                        val rankCount = syncRankingInternal(match.id)
                        totalOddsSynced += oddsCount
                        totalRankingsSynced += rankCount
                    }
                }

                // 5. Update pagination state
                totalPages = response.data.meta?.totalPage ?: 1
                currentPage++
            }

            val timestamp = System.currentTimeMillis()

            // 6. Metadata Hook execution
            if (onMetadataHook != null) {
                onMetadataHook(timestamp, totalMatchesSynced, totalTeamsSynced)
            } else {
                metadataRepository?.refreshSnapshot(timestamp)
            }

            SyncResult.Success(
                SyncSummary(
                    matchesSynced = totalMatchesSynced,
                    teamsSynced = totalTeamsSynced,
                    oddsRecordsSynced = totalOddsSynced,
                    rankingsSynced = totalRankingsSynced,
                    timestamp = timestamp
                )
            )
        } catch (e: Exception) {
            SyncResult.Failure(e)
        }
    }

    /**
     * Đồng bộ Leagues & Seasons cục bộ hoặc từ dữ liệu cấu hình có sẵn.
     * Đảm bảo thứ tự Leagues -> Seasons trong 1 Transaction nguyên tử.
     */
    suspend fun syncLeaguesAndSeasons(
        leagues: List<LeagueEntity>,
        seasons: List<SeasonEntity>
    ): SyncResult<Int> = withContext(Dispatchers.IO) {
        try {
            database.runInTransaction {
                if (leagues.isNotEmpty()) {
                    database.leagueDao().insertLeagues(leagues)
                }
                if (seasons.isNotEmpty()) {
                    database.seasonDao().insertSeasons(seasons)
                }
            }
            SyncResult.Success(leagues.size + seasons.size)
        } catch (e: Exception) {
            SyncResult.Failure(e)
        }
    }

    /**
     * Backward-compatible helper method for syncing matches by date.
     */
    suspend fun syncMatchesByDate(date: String): Result<Unit> = withContext(Dispatchers.IO) {
        when (val result = syncFullPipelineForDate(date, syncOddsAndRankings = false)) {
            is SyncResult.Success -> Result.success(Unit)
            is SyncResult.Failure -> Result.failure(result.error)
        }
    }

    /**
     * Đồng bộ Lịch sử Tỷ lệ kèo cho 1 trận đấu cụ thể (Atomic Transaction).
     */
    suspend fun syncOddsHistoryForMatch(matchId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            syncOddsInternal(matchId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Đồng bộ Bảng xếp hạng mùa giải tại thời điểm diễn ra trận đấu (Atomic Transaction).
     */
    suspend fun syncSeasonRankingForMatch(matchId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            syncRankingInternal(matchId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun syncOddsInternal(matchId: Long): Int {
        val response = retryExecutor.execute {
            oddsApi.getOddsHistory(matchId)
        }
        val oddsRecords = response.data.data

        if (oddsRecords.isNotEmpty()) {
            val oddsEntities = oddsRecords.map { it.toEntity(matchId) }
            database.runInTransaction {
                database.oddsDao().insertOdds(oddsEntities)
            }
            return oddsEntities.size
        }
        return 0
    }

    private suspend fun syncRankingInternal(matchId: Long): Int {
        val response = retryExecutor.execute {
            rankingApi.getSeasonRanking(matchId)
        }
        val rankRecords = response.data

        if (rankRecords.isNotEmpty()) {
            val rankEntities = rankRecords.map { it.toEntity(matchId, json) }
            database.runInTransaction {
                database.rankingDao().insertRankings(rankEntities)
            }
            return rankEntities.size
        }
        return 0
    }
}
