package dev.anhquocs.truelab.core.data.crawler

import android.util.Log
import androidx.room.withTransaction
import dev.anhquocs.truelab.core.data.local.database.TrueLabDatabase
import dev.anhquocs.truelab.core.data.local.mapper.RoomMappers.toAwayTeamEntity
import dev.anhquocs.truelab.core.data.local.mapper.RoomMappers.toEntity
import dev.anhquocs.truelab.core.data.local.mapper.RoomMappers.toHomeTeamEntity
import dev.anhquocs.truelab.core.data.local.mapper.RoomMappers.toMatchEntity
import dev.anhquocs.truelab.core.data.match.remote.api.MatchApi
import dev.anhquocs.truelab.core.data.odds.remote.api.OddsApi
import dev.anhquocs.truelab.core.data.ranking.remote.api.RankingApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

class DataSyncEngine @Inject constructor(
    private val matchApi: MatchApi,
    private val oddsApi: OddsApi,
    private val rankingApi: RankingApi,
    private val database: TrueLabDatabase,
    private val json: Json
) {
    /**
     * Đồng bộ danh sách trận đấu theo ngày, sử dụng Pagination.
     * Xử lý Batch size = 50 (mặc định của API).
     * 
     * @param date Ngày cần lấy dữ liệu (yyyy-MM-dd)
     * @return Result chứa trạng thái thành công hoặc thất bại.
     */
    suspend fun syncMatchesByDate(date: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var currentPage = 1
            var totalPages = 1

            while (currentPage <= totalPages) {
                // 1. Fetch API
                val response = matchApi.getMatches(date = date, page = currentPage)
                val matchRecords = response.data.data

                if (matchRecords.isEmpty()) break

                // 2. Map DTO -> Entities
                // Deduplicate teams to avoid inserting the same team multiple times in one batch
                val teams = matchRecords.flatMap {
                    listOf(it.toHomeTeamEntity(), it.toAwayTeamEntity())
                }.distinctBy { it.id }

                val matches = matchRecords.map { it.toMatchEntity() }

                // 3. Batch Insert via Transaction (Atomicity per page)
                // Using REPLACE strategy handles Upserts gracefully.
                database.withTransaction {
                    database.teamDao().insertTeams(teams)
                    database.matchDao().insertMatches(matches)
                }

                // 4. Update Pagination state
                totalPages = response.data.meta?.totalPage ?: 1
                currentPage++
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DataSyncEngine", "Error syncing matches for date: $date", e)
            Result.failure(e)
        }
    }

    /**
     * Đồng bộ Lịch sử Tỷ lệ kèo cho 1 trận đấu cụ thể.
     */
    suspend fun syncOddsHistoryForMatch(matchId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Note: In real-world, we might limit this to Top 5 Bookmakers to save DB space
            // For now, fetch overall history based on the available API endpoint.
            val response = oddsApi.getOddsHistory(matchId)
            val oddsRecords = response.data.data

            if (oddsRecords.isNotEmpty()) {
                val oddsEntities = oddsRecords.map { it.toEntity(matchId) }
                
                database.withTransaction {
                    database.oddsDao().insertOdds(oddsEntities)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DataSyncEngine", "Error syncing odds for match: $matchId", e)
            Result.failure(e)
        }
    }

    /**
     * Đồng bộ Bảng xếp hạng mùa giải tại thời điểm diễn ra trận đấu.
     * Giải quyết vấn đề GAP: Lưu trực tiếp matchId vào SeasonRankingEntity.
     */
    suspend fun syncSeasonRankingForMatch(matchId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = rankingApi.getSeasonRanking(matchId)
            val rankRecords = response.data

            if (rankRecords.isNotEmpty()) {
                val rankEntities = rankRecords.map { it.toEntity(matchId, json) }
                
                database.withTransaction {
                    database.rankingDao().insertRankings(rankEntities)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DataSyncEngine", "Error syncing ranking for match: $matchId", e)
            Result.failure(e)
        }
    }
}
