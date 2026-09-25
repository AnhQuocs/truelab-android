package dev.anhquocs.truelab.core.data.crawler

import dev.anhquocs.truelab.core.data.crawler.model.SyncResult
import dev.anhquocs.truelab.core.data.league.local.dao.LeagueDao
import dev.anhquocs.truelab.core.data.league.local.dao.SeasonDao
import dev.anhquocs.truelab.core.data.league.local.entity.LeagueEntity
import dev.anhquocs.truelab.core.data.league.local.entity.SeasonEntity
import dev.anhquocs.truelab.core.data.local.database.TrueLabDatabase
import dev.anhquocs.truelab.core.data.match.local.dao.MatchDao
import dev.anhquocs.truelab.core.data.match.local.entity.MatchEntity
import dev.anhquocs.truelab.core.data.match.local.entity.MatchWithTeams
import dev.anhquocs.truelab.core.data.match.remote.api.MatchApi
import dev.anhquocs.truelab.core.data.match.remote.dto.MatchInfoDetailResponseBase
import dev.anhquocs.truelab.core.data.match.remote.dto.MatchRecord
import dev.anhquocs.truelab.core.data.match.remote.dto.TeamInfo
import dev.anhquocs.truelab.core.data.odds.local.dao.OddsDao
import dev.anhquocs.truelab.core.data.odds.local.entity.OddsEntity
import dev.anhquocs.truelab.core.data.odds.remote.api.OddsApi
import dev.anhquocs.truelab.core.data.odds.remote.dto.OddsHistoryResponse
import dev.anhquocs.truelab.core.data.odds.remote.dto.OddsRecord
import dev.anhquocs.truelab.core.data.ranking.local.dao.RankingDao
import dev.anhquocs.truelab.core.data.ranking.local.entity.SeasonRankingEntity
import dev.anhquocs.truelab.core.data.ranking.remote.api.RankingApi
import dev.anhquocs.truelab.core.data.ranking.remote.dto.SeasonRankResponse
import dev.anhquocs.truelab.core.data.remote.dto.BaseResponse
import dev.anhquocs.truelab.core.data.remote.dto.MetaResponse
import dev.anhquocs.truelab.core.data.team.local.dao.TeamDao
import dev.anhquocs.truelab.core.data.team.local.entity.TeamEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DataSyncEngineTest {
    private val json = Json { ignoreUnknownKeys = true }

    private class FakeTeamDao : TeamDao {
        val teams = mutableMapOf<Int, TeamEntity>()

        override fun insertTeams(teams: List<TeamEntity>): LongArray {
            teams.forEach { team ->
                // IGNORE strategy simulation: only put if absent to protect Elo/Form
                if (!this.teams.containsKey(team.id)) {
                    this.teams[team.id] = team
                }
            }
            return LongArray(teams.size) { (it + 1).toLong() }
        }

        override fun getTeamById(teamId: Int): Flow<TeamEntity?> = flowOf(teams[teamId])

        override fun searchTeams(query: String): Flow<List<TeamEntity>> =
            flowOf(teams.values.filter { it.name.contains(query, ignoreCase = true) })
    }

    private class FakeMatchDao : MatchDao {
        val matches = mutableMapOf<Long, MatchEntity>()
        var errorToThrow: Throwable? = null
        override fun insertMatches(matches: List<MatchEntity>): LongArray {
            errorToThrow?.let { throw it }
            matches.forEach { this.matches[it.id] = it }
            return LongArray(matches.size) { (it + 1).toLong() }
        }
        override fun getMatchById(matchId: Long): Flow<MatchWithTeams?> = flowOf(null)
        override fun getMatchesPaged(limit: Int, offset: Int): Flow<List<MatchWithTeams>> = flowOf(emptyList())
        override fun getMatchesByDate(date: String): Flow<List<MatchWithTeams>> = flowOf(emptyList())
        override fun getMatchesByStatus(status: String): Flow<List<MatchWithTeams>> = flowOf(emptyList())
        override fun getH2HMatches(teamAId: Int, teamBId: Int): Flow<List<MatchWithTeams>> = flowOf(emptyList())
        override fun getRecentMatchesForTeam(teamId: Int, limit: Int): Flow<List<MatchWithTeams>> = flowOf(emptyList())
        override fun getMatchesByLeague(leagueId: Int): Flow<List<MatchWithTeams>> = flowOf(emptyList())
        override fun getMatchesByLeagueAndSeason(leagueId: Int, season: String): Flow<List<MatchWithTeams>> = flowOf(emptyList())
    }

    private class FakeLeagueDao : LeagueDao {
        val leagues = mutableMapOf<Int, LeagueEntity>()
        override fun insertLeagues(leagues: List<LeagueEntity>): LongArray {
            leagues.forEach { this.leagues[it.id] = it }
            return LongArray(leagues.size) { (it + 1).toLong() }
        }
        override fun getLeagues(): Flow<List<LeagueEntity>> = flowOf(leagues.values.toList())
        override fun getLeagueById(leagueId: Int): Flow<LeagueEntity?> = flowOf(leagues[leagueId])
    }

    private class FakeSeasonDao : SeasonDao {
        val seasons = mutableMapOf<String, SeasonEntity>()
        override fun insertSeasons(seasons: List<SeasonEntity>): LongArray {
            seasons.forEach { this.seasons[it.id] = it }
            return LongArray(seasons.size) { (it + 1).toLong() }
        }
        override fun getSeasonsByLeague(leagueId: Int): Flow<List<SeasonEntity>> =
            flowOf(seasons.values.filter { it.leagueId == leagueId })
        override fun getCurrentSeason(leagueId: Int): Flow<SeasonEntity?> =
            flowOf(seasons.values.find { it.leagueId == leagueId && it.isCurrent })
    }

    private class FakeOddsDao : OddsDao {
        val oddsList = mutableListOf<OddsEntity>()
        override fun insertOdds(odds: List<OddsEntity>): LongArray {
            oddsList.addAll(odds)
            return LongArray(odds.size) { (it + 1).toLong() }
        }
        override fun getOddsHistory(matchId: Long, companyId: Int?, oddsType: String?): Flow<List<OddsEntity>> = flowOf(emptyList())
        override fun getLatestOddsForMatch(matchId: Long): Flow<List<OddsEntity>> = flowOf(emptyList())
    }

    private class FakeRankingDao : RankingDao {
        val rankings = mutableListOf<SeasonRankingEntity>()
        override fun insertRankings(rankings: List<SeasonRankingEntity>): LongArray {
            this.rankings.addAll(rankings)
            return LongArray(rankings.size) { (it + 1).toLong() }
        }
        override fun getRankingsForMatch(matchId: Long): Flow<List<SeasonRankingEntity>> = flowOf(emptyList())
        override fun getLatestRankingForTeam(teamId: Int): Flow<SeasonRankingEntity?> = flowOf(null)
        override fun getLatestSeasonRankings(): Flow<List<SeasonRankingEntity>> = flowOf(emptyList())
    }

    private class FakeMatchApi : MatchApi {
        var responseToReturn: BaseResponse<MatchInfoDetailResponseBase>? = null
        var errorToThrow: Throwable? = null

        override suspend fun getMatches(
            date: String, status: Int, page: Int, pageSize: Int, sort: String
        ): BaseResponse<MatchInfoDetailResponseBase> {
            errorToThrow?.let { throw it }
            return responseToReturn ?: BaseResponse(
                statusCode = 200, message = "OK",
                data = MatchInfoDetailResponseBase(data = emptyList(), meta = MetaResponse(currentPage = 1, totalPage = 1))
            )
        }
    }

    private class FakeOddsApi : OddsApi {
        override suspend fun getOdds(matchId: Long): BaseResponse<List<OddsRecord>> =
            BaseResponse(statusCode = 200, message = "OK", data = emptyList())

        override suspend fun getOddsHistory(matchId: Long, companyId: Int?, oddsType: String?): BaseResponse<OddsHistoryResponse> =
            BaseResponse(statusCode = 200, message = "OK", data = OddsHistoryResponse(data = emptyList()))
    }

    private class FakeRankingApi : RankingApi {
        override suspend fun getSeasonRanking(matchId: Long): BaseResponse<List<SeasonRankResponse>> =
            BaseResponse(statusCode = 200, message = "OK", data = emptyList())
    }

    private class TestTrueLabDatabase(
        val teamDaoImpl: FakeTeamDao,
        val matchDaoImpl: FakeMatchDao,
        val leagueDaoImpl: FakeLeagueDao,
        val seasonDaoImpl: FakeSeasonDao,
        val oddsDaoImpl: FakeOddsDao,
        val rankingDaoImpl: FakeRankingDao
    ) : TrueLabDatabase() {
        override fun teamDao(): TeamDao = teamDaoImpl
        override fun matchDao(): MatchDao = matchDaoImpl
        override fun leagueDao(): LeagueDao = leagueDaoImpl
        override fun seasonDao(): SeasonDao = seasonDaoImpl
        override fun oddsDao(): OddsDao = oddsDaoImpl
        override fun rankingDao(): RankingDao = rankingDaoImpl
        override fun predictionDao(): dev.anhquocs.truelab.core.data.prediction.local.dao.PredictionDao = throw NotImplementedError()
        override fun datasetMetadataDao(): dev.anhquocs.truelab.core.data.metadata.local.dao.DatasetMetadataDao = throw NotImplementedError()
        override fun clearAllTables() {}
        override fun createInvalidationTracker(): androidx.room.InvalidationTracker =
            androidx.room.InvalidationTracker(this, "teams", "matches")
        override fun createOpenHelper(config: androidx.room.DatabaseConfiguration): androidx.sqlite.db.SupportSQLiteOpenHelper =
            throw UnsupportedOperationException()
        override fun runInTransaction(body: Runnable) { body.run() }
        override fun <V : Any?> runInTransaction(body: java.util.concurrent.Callable<V>): V = body.call()
    }

    private lateinit var fakeMatchApi: FakeMatchApi
    private lateinit var fakeOddsApi: FakeOddsApi
    private lateinit var fakeRankingApi: FakeRankingApi
    private lateinit var fakeTeamDao: FakeTeamDao
    private lateinit var fakeMatchDao: FakeMatchDao
    private lateinit var fakeLeagueDao: FakeLeagueDao
    private lateinit var fakeSeasonDao: FakeSeasonDao
    private lateinit var fakeOddsDao: FakeOddsDao
    private lateinit var fakeRankingDao: FakeRankingDao
    private lateinit var testDatabase: TestTrueLabDatabase
    private lateinit var syncEngine: DataSyncEngine

    @Before
    fun setup() {
        fakeMatchApi = FakeMatchApi()
        fakeOddsApi = FakeOddsApi()
        fakeRankingApi = FakeRankingApi()
        fakeTeamDao = FakeTeamDao()
        fakeMatchDao = FakeMatchDao()
        fakeLeagueDao = FakeLeagueDao()
        fakeSeasonDao = FakeSeasonDao()
        fakeOddsDao = FakeOddsDao()
        fakeRankingDao = FakeRankingDao()

        testDatabase = TestTrueLabDatabase(
            fakeTeamDao,
            fakeMatchDao,
            fakeLeagueDao,
            fakeSeasonDao,
            fakeOddsDao,
            fakeRankingDao
        )

        syncEngine = DataSyncEngine(
            matchApi = fakeMatchApi,
            oddsApi = fakeOddsApi,
            rankingApi = fakeRankingApi,
            database = testDatabase,
            json = json
        )
    }

    @Test
    fun syncFullPipelineForDate_success_insertsTeamsAndMatchesDeterministically() = runTest {
        val matches = listOf(
            MatchRecord(id = 1001L, homeTeam = TeamInfo(1, "Arsenal", "arsenal.png"), awayTeam = TeamInfo(2, "Chelsea", "chelsea.png"), homeScore = 2, awayScore = 1, startTimeDate = "2024-05-10 15:00:00", status = "8"),
            MatchRecord(id = 1002L, homeTeam = TeamInfo(1, "Arsenal", "arsenal.png"), awayTeam = TeamInfo(3, "Liverpool", "liverpool.png"), homeScore = 0, awayScore = 0, startTimeDate = "2024-05-10 18:00:00", status = "8")
        )

        fakeMatchApi.responseToReturn = BaseResponse(
            statusCode = 200,
            message = "OK",
            data = MatchInfoDetailResponseBase(
                data = matches,
                meta = MetaResponse(currentPage = 1, totalPage = 1)
            )
        )

        var hookCalled = false
        val result = syncEngine.syncFullPipelineForDate("2024-05-10", syncOddsAndRankings = false) { timestamp, matchesCount, teamsCount ->
            hookCalled = true
            assertEquals(2, matchesCount)
            assertEquals(3, teamsCount)
        }

        assertTrue(result is SyncResult.Success)
        val summary = (result as SyncResult.Success).data
        assertEquals(2, summary.matchesSynced)
        assertEquals(3, summary.teamsSynced)
        assertTrue(hookCalled)

        // Verify DB content
        assertEquals(3, fakeTeamDao.teams.size)
        assertEquals(2, fakeMatchDao.matches.size)
    }

    @Test
    fun syncFullPipelineForDate_failure_propagatesError() = runTest {
        fakeMatchApi.errorToThrow = RuntimeException("Network timeout")

        val result = syncEngine.syncFullPipelineForDate("2024-05-10")

        assertTrue(result is SyncResult.Failure)
        val failure = result as SyncResult.Failure
        assertEquals("Network timeout", failure.error.message)
    }

    @Test
    fun syncLeaguesAndSeasons_success_insertsInTransaction() = runTest {
        val leagues = listOf(
            LeagueEntity(id = 39, name = "Premier League", country = "England"),
            LeagueEntity(id = 140, name = "La Liga", country = "Spain")
        )
        val seasons = listOf(
            SeasonEntity(id = "39_2024", leagueId = 39, name = "2023-2024", year = 2024, isCurrent = true)
        )

        val result = syncEngine.syncLeaguesAndSeasons(leagues, seasons)

        assertTrue(result is SyncResult.Success)
        assertEquals(3, (result as SyncResult.Success).data)
        assertEquals(2, fakeLeagueDao.leagues.size)
        assertEquals(1, fakeSeasonDao.seasons.size)
    }

    @Test
    fun idempotency_repeatedSyncMatches_doesNotDuplicateOrOverwriteElo() = runTest {
        // Pre-populate Arsenal with calculated Elo
        fakeTeamDao.teams[1] = TeamEntity(id = 1, name = "Arsenal", logo = "old.png", leagueName = "EPL", eloRating = 1750.0, formScore = 0.85)

        val match = MatchRecord(
            id = 1001L,
            homeTeam = TeamInfo(id = 1, name = "Arsenal", logo = "new.png"),
            awayTeam = TeamInfo(id = 2, name = "Chelsea", logo = "chelsea.png"),
            homeScore = 2,
            awayScore = 1,
            startTimeDate = "2024-05-10 15:00:00",
            status = "8"
        )

        fakeMatchApi.responseToReturn = BaseResponse(
            statusCode = 200,
            message = "OK",
            data = MatchInfoDetailResponseBase(
                data = listOf(match),
                meta = MetaResponse(currentPage = 1, totalPage = 1)
            )
        )

        // Run sync 1st time
        syncEngine.syncFullPipelineForDate("2024-05-10")
        // Run sync 2nd time
        syncEngine.syncFullPipelineForDate("2024-05-10")

        // Elo Rating should NOT be overwritten (remains 1750.0)
        assertEquals(1750.0, fakeTeamDao.teams[1]?.eloRating ?: 0.0, 0.001)
        assertEquals(0.85, fakeTeamDao.teams[1]?.formScore ?: 0.0, 0.001)
        // Matches count remains 1 (idempotent)
        assertEquals(1, fakeMatchDao.matches.size)
    }

    @Test
    fun syncFullPipelineForDate_databaseExceptionInBatch_returnsFailureResult() = runTest {
        val match = MatchRecord(
            id = 1001L,
            homeTeam = TeamInfo(id = 1, name = "Arsenal", logo = "arsenal.png"),
            awayTeam = TeamInfo(id = 2, name = "Chelsea", logo = "chelsea.png"),
            homeScore = 2,
            awayScore = 1,
            startTimeDate = "2024-05-10 15:00:00",
            status = "8"
        )
        fakeMatchApi.responseToReturn = BaseResponse(
            statusCode = 200,
            message = "OK",
            data = MatchInfoDetailResponseBase(
                data = listOf(match),
                meta = MetaResponse(currentPage = 1, totalPage = 1)
            )
        )
        fakeMatchDao.errorToThrow = RuntimeException("Disk full")

        val result = syncEngine.syncFullPipelineForDate("2024-05-10")

        assertTrue(result is SyncResult.Failure)
        assertEquals("Disk full", (result as SyncResult.Failure).error.message)
    }

    private class FakeDatasetMetadataRepository : dev.anhquocs.truelab.core.domain.metadata.repository.DatasetMetadataRepository {
        var refreshedTimestamp: Long? = null
        override fun getMetadata(key: String): Flow<dev.anhquocs.truelab.core.domain.metadata.model.DatasetMetadata?> = flowOf(null)
        override suspend fun refreshSnapshot(timestamp: Long): Result<dev.anhquocs.truelab.core.domain.metadata.model.DatasetMetadata> {
            refreshedTimestamp = timestamp
            return Result.success(dev.anhquocs.truelab.core.domain.metadata.model.DatasetMetadata(lastSyncTimestamp = timestamp, totalMatches = 0, totalTeams = 0, totalOddsRecords = 0, totalLeagues = 0, totalSeasons = 0))
        }
    }

    @Test
    fun syncFullPipelineForDate_withMetadataRepository_triggersRefreshSnapshot() = runTest {
        val fakeRepo = FakeDatasetMetadataRepository()
        val engineWithRepo = DataSyncEngine(
            matchApi = fakeMatchApi,
            oddsApi = fakeOddsApi,
            rankingApi = fakeRankingApi,
            database = testDatabase,
            json = json,
            metadataRepository = fakeRepo
        )

        val result = engineWithRepo.syncFullPipelineForDate("2024-05-10")

        assertTrue(result is SyncResult.Success)
        org.junit.Assert.assertNotNull(fakeRepo.refreshedTimestamp)
    }

    @Test
    fun syncMatchesByDate_backwardCompatibility_returnsSuccessResult() = runTest {
        fakeMatchApi.responseToReturn = BaseResponse(
            statusCode = 200,
            message = "OK",
            data = MatchInfoDetailResponseBase(
                data = emptyList(),
                meta = MetaResponse(currentPage = 1, totalPage = 1)
            )
        )

        val result = syncEngine.syncMatchesByDate("2024-05-10")

        assertTrue(result.isSuccess)
    }
}
