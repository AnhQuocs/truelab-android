package dev.anhquocs.truelab.core.data.crawler

import dev.anhquocs.truelab.core.data.crawler.cache.CacheFreshnessChecker
import dev.anhquocs.truelab.core.data.crawler.cache.DataFreshnessPolicy
import dev.anhquocs.truelab.core.data.crawler.cache.DatasetCategory
import dev.anhquocs.truelab.core.data.crawler.cache.DefaultCacheFreshnessChecker
import dev.anhquocs.truelab.core.data.crawler.model.SyncResult
import dev.anhquocs.truelab.core.data.crawler.retry.DefaultRetryClassifier
import dev.anhquocs.truelab.core.data.crawler.retry.RetryExecutor
import dev.anhquocs.truelab.core.data.crawler.retry.RetryPolicy
import dev.anhquocs.truelab.core.data.league.local.dao.LeagueDao
import dev.anhquocs.truelab.core.data.league.local.dao.SeasonDao
import dev.anhquocs.truelab.core.data.local.database.TrueLabDatabase
import dev.anhquocs.truelab.core.data.match.local.dao.MatchDao
import dev.anhquocs.truelab.core.data.match.local.entity.MatchEntity
import dev.anhquocs.truelab.core.data.match.local.entity.MatchWithTeams
import dev.anhquocs.truelab.core.data.match.remote.api.MatchApi
import dev.anhquocs.truelab.core.data.match.remote.dto.MatchInfoDetailResponseBase
import dev.anhquocs.truelab.core.data.match.remote.dto.MatchRecord
import dev.anhquocs.truelab.core.data.match.remote.dto.TeamInfo
import dev.anhquocs.truelab.core.data.metadata.local.dao.DatasetMetadataDao
import dev.anhquocs.truelab.core.data.odds.local.dao.OddsDao
import dev.anhquocs.truelab.core.data.odds.local.entity.OddsEntity
import dev.anhquocs.truelab.core.data.odds.remote.api.OddsApi
import dev.anhquocs.truelab.core.data.odds.remote.dto.OddsHistoryResponse
import dev.anhquocs.truelab.core.data.odds.remote.dto.OddsRecord
import dev.anhquocs.truelab.core.data.prediction.local.dao.PredictionDao
import dev.anhquocs.truelab.core.data.ranking.local.dao.RankingDao
import dev.anhquocs.truelab.core.data.ranking.local.entity.SeasonRankingEntity
import dev.anhquocs.truelab.core.data.ranking.remote.api.RankingApi
import dev.anhquocs.truelab.core.data.ranking.remote.dto.SeasonRankResponse
import dev.anhquocs.truelab.core.data.remote.dto.BaseResponse
import dev.anhquocs.truelab.core.data.remote.dto.MetaResponse
import dev.anhquocs.truelab.core.data.team.local.dao.TeamDao
import dev.anhquocs.truelab.core.data.team.local.entity.TeamEntity
import dev.anhquocs.truelab.core.domain.metadata.model.DatasetMetadata
import dev.anhquocs.truelab.core.domain.metadata.repository.DatasetMetadataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class DataSyncEngineCacheTest {

    private val json = Json { ignoreUnknownKeys = true }

    private class CacheTestTeamDao : TeamDao {
        val teams = mutableMapOf<Int, TeamEntity>()
        override fun insertTeams(teams: List<TeamEntity>): LongArray {
            teams.forEach { if (!this.teams.containsKey(it.id)) this.teams[it.id] = it }
            return LongArray(teams.size) { (it + 1).toLong() }
        }
        override fun getTeamById(teamId: Int): Flow<TeamEntity?> = throw NotImplementedError()
        override fun searchTeams(query: String): Flow<List<TeamEntity>> = throw NotImplementedError()
    }

    private class CacheTestMatchDao : MatchDao {
        val matches = mutableMapOf<Long, MatchEntity>()
        override fun insertMatches(matches: List<MatchEntity>): LongArray {
            matches.forEach { this.matches[it.id] = it }
            return LongArray(matches.size) { (it + 1).toLong() }
        }
        override fun getMatchById(matchId: Long): Flow<MatchWithTeams?> = throw NotImplementedError()
        override fun getMatchesPaged(limit: Int, offset: Int): Flow<List<MatchWithTeams>> = throw NotImplementedError()
        override fun getMatchesByDate(date: String): Flow<List<MatchWithTeams>> = throw NotImplementedError()
        override fun getMatchesByStatus(status: String): Flow<List<MatchWithTeams>> = throw NotImplementedError()
        override fun getH2HMatches(teamAId: Int, teamBId: Int): Flow<List<MatchWithTeams>> = throw NotImplementedError()
        override fun getRecentMatchesForTeam(teamId: Int, limit: Int): Flow<List<MatchWithTeams>> = throw NotImplementedError()
        override fun getMatchesByLeague(leagueId: Int): Flow<List<MatchWithTeams>> = throw NotImplementedError()
        override fun getMatchesByLeagueAndSeason(leagueId: Int, season: String): Flow<List<MatchWithTeams>> = throw NotImplementedError()
    }

    private class CacheTestMatchApi : MatchApi {
        var callCount = 0
        var errorToThrow: Throwable? = null

        override suspend fun getMatches(date: String, status: Int?, page: Int, pageSize: Int, sort: String): BaseResponse<MatchInfoDetailResponseBase> {
            callCount++
            errorToThrow?.let { throw it }

            return BaseResponse(
                statusCode = 200,
                message = "OK",
                data = MatchInfoDetailResponseBase(
                    data = listOf(
                        MatchRecord(
                            id = 2001L,
                            homeTeam = TeamInfo(1, "Arsenal", "arsenal.png"),
                            awayTeam = TeamInfo(2, "Chelsea", "chelsea.png"),
                            homeScore = 3,
                            awayScore = 1,
                            startTimeDate = "2024-05-10 15:00:00",
                            status = "8"
                        )
                    ),
                    meta = MetaResponse(currentPage = 1, totalPage = 1)
                )
            )
        }
    }

    private class CacheTestOddsApi : OddsApi {
        override suspend fun getOdds(matchId: Long): BaseResponse<List<OddsRecord>> = BaseResponse(200, "OK", emptyList())
        override suspend fun getOddsHistory(matchId: Long, companyId: Int?, oddsType: String?): BaseResponse<OddsHistoryResponse> =
            BaseResponse(200, "OK", OddsHistoryResponse(emptyList()))
    }

    private class CacheTestRankingApi : RankingApi {
        override suspend fun getSeasonRanking(matchId: Long): BaseResponse<List<SeasonRankResponse>> = BaseResponse(200, "OK", emptyList())
    }

    private class FakeMetadataRepository : DatasetMetadataRepository {
        val metadataFlow = MutableStateFlow<DatasetMetadata?>(null)
        var lastRefreshedTimestamp: Long? = null

        override fun getMetadata(key: String): Flow<DatasetMetadata?> = metadataFlow

        override suspend fun refreshSnapshot(timestamp: Long): Result<DatasetMetadata> {
            lastRefreshedTimestamp = timestamp
            val updated = DatasetMetadata(
                lastSyncTimestamp = timestamp,
                totalMatches = 10,
                totalTeams = 20,
                totalOddsRecords = 50,
                totalLeagues = 5,
                totalSeasons = 5
            )
            metadataFlow.value = updated
            return Result.success(updated)
        }
    }

    private lateinit var teamDao: CacheTestTeamDao
    private lateinit var matchDao: CacheTestMatchDao
    private lateinit var testDatabase: TrueLabDatabase
    private lateinit var matchApi: CacheTestMatchApi
    private lateinit var oddsApi: CacheTestOddsApi
    private lateinit var rankingApi: CacheTestRankingApi
    private lateinit var metadataRepo: FakeMetadataRepository

    @Before
    fun setUp() {
        teamDao = CacheTestTeamDao()
        matchDao = CacheTestMatchDao()
        matchApi = CacheTestMatchApi()
        oddsApi = CacheTestOddsApi()
        rankingApi = CacheTestRankingApi()
        metadataRepo = FakeMetadataRepository()

        testDatabase = object : TrueLabDatabase() {
            override fun matchDao(): MatchDao = matchDao
            override fun teamDao(): TeamDao = teamDao
            override fun oddsDao(): OddsDao = throw NotImplementedError()
            override fun rankingDao(): RankingDao = throw NotImplementedError()
            override fun leagueDao(): LeagueDao = throw NotImplementedError()
            override fun seasonDao(): SeasonDao = throw NotImplementedError()
            override fun datasetMetadataDao(): DatasetMetadataDao = throw NotImplementedError()
            override fun predictionDao(): PredictionDao = throw NotImplementedError()
            override fun clearAllTables() {}
            override fun createInvalidationTracker(): androidx.room.InvalidationTracker =
                androidx.room.InvalidationTracker(this, "teams", "matches")
            override fun createOpenHelper(config: androidx.room.DatabaseConfiguration): androidx.sqlite.db.SupportSQLiteOpenHelper =
                throw UnsupportedOperationException()
            override fun <T> runInTransaction(body: java.util.concurrent.Callable<T>): T = body.call()
            override fun runInTransaction(body: Runnable) = body.run()
        }
    }

    @Test
    fun `fresh cache with forceRefresh false skips remote API call`() = runTest {
        val syncTime = 1_000_000L
        val currentTime = 1_100_000L // 100s elapsed (TTL is 900s)

        metadataRepo.metadataFlow.value = DatasetMetadata(
            lastSyncTimestamp = syncTime,
            totalMatches = 5,
            totalTeams = 10,
            totalOddsRecords = 15,
            totalLeagues = 1,
            totalSeasons = 1
        )

        val syncEngine = DataSyncEngine(
            matchApi = matchApi,
            oddsApi = oddsApi,
            rankingApi = rankingApi,
            database = testDatabase,
            json = json,
            metadataRepository = metadataRepo,
            timeProvider = { currentTime }
        )

        val result = syncEngine.syncFullPipelineForDate("2024-05-10", forceRefresh = false)

        assertTrue(result is SyncResult.Success)
        assertEquals(0, matchApi.callCount) // Remote API was NOT called
        assertEquals(0, (result as SyncResult.Success).data.matchesSynced)
        assertEquals(syncTime, result.data.timestamp)
        assertNull(metadataRepo.lastRefreshedTimestamp)
    }

    @Test
    fun `stale cache with forceRefresh false executes remote API call`() = runTest {
        val syncTime = 1_000_000L
        val currentTime = 2_000_000L // 1000s elapsed (exceeds default TTL 900s)

        metadataRepo.metadataFlow.value = DatasetMetadata(
            lastSyncTimestamp = syncTime,
            totalMatches = 5,
            totalTeams = 10,
            totalOddsRecords = 15,
            totalLeagues = 1,
            totalSeasons = 1
        )

        val syncEngine = DataSyncEngine(
            matchApi = matchApi,
            oddsApi = oddsApi,
            rankingApi = rankingApi,
            database = testDatabase,
            json = json,
            metadataRepository = metadataRepo,
            timeProvider = { currentTime }
        )

        val result = syncEngine.syncFullPipelineForDate("2024-05-10", forceRefresh = false)

        assertTrue(result is SyncResult.Success)
        assertEquals(1, matchApi.callCount) // Remote API was called
        assertEquals(1, (result as SyncResult.Success).data.matchesSynced)
        assertEquals(currentTime, metadataRepo.lastRefreshedTimestamp)
        assertEquals(1, matchDao.matches.size)
    }

    @Test
    fun `fresh cache with forceRefresh true bypasses cache and calls remote API`() = runTest {
        val syncTime = 1_000_000L
        val currentTime = 1_050_000L // 50s elapsed (fresh)

        metadataRepo.metadataFlow.value = DatasetMetadata(
            lastSyncTimestamp = syncTime,
            totalMatches = 5,
            totalTeams = 10,
            totalOddsRecords = 15,
            totalLeagues = 1,
            totalSeasons = 1
        )

        val syncEngine = DataSyncEngine(
            matchApi = matchApi,
            oddsApi = oddsApi,
            rankingApi = rankingApi,
            database = testDatabase,
            json = json,
            metadataRepository = metadataRepo,
            timeProvider = { currentTime }
        )

        val result = syncEngine.syncFullPipelineForDate("2024-05-10", forceRefresh = true)

        assertTrue(result is SyncResult.Success)
        assertEquals(1, matchApi.callCount) // Remote API was called due to forceRefresh
        assertEquals(1, (result as SyncResult.Success).data.matchesSynced)
        assertEquals(currentTime, metadataRepo.lastRefreshedTimestamp)
    }

    @Test
    fun `category specific TTL evaluation behaves accurately`() = runTest {
        val syncTime = 1_000_000L
        val currentTime = 1_600_000L // 600s (10 mins) elapsed

        metadataRepo.metadataFlow.value = DatasetMetadata(
            lastSyncTimestamp = syncTime,
            totalMatches = 5,
            totalTeams = 10,
            totalOddsRecords = 15,
            totalLeagues = 1,
            totalSeasons = 1
        )

        val syncEngine = DataSyncEngine(
            matchApi = matchApi,
            oddsApi = oddsApi,
            rankingApi = rankingApi,
            database = testDatabase,
            json = json,
            metadataRepository = metadataRepo,
            timeProvider = { currentTime }
        )

        // 1. LIVE_MATCHES (TTL 5 mins = 300s): 600s elapsed is STALE -> calls API
        val liveResult = syncEngine.syncFullPipelineForDate(
            date = "2024-05-10",
            forceRefresh = false,
            category = DatasetCategory.LIVE_MATCHES
        )
        assertTrue(liveResult is SyncResult.Success)
        assertEquals(1, matchApi.callCount)

        // 2. SCHEDULED_MATCHES (TTL 30 mins = 1800s): 600s elapsed is FRESH -> does not call API again
        val scheduledResult = syncEngine.syncFullPipelineForDate(
            date = "2024-05-10",
            forceRefresh = false,
            category = DatasetCategory.SCHEDULED_MATCHES
        )
        assertTrue(scheduledResult is SyncResult.Success)
        assertEquals(1, matchApi.callCount) // callCount remains 1
    }

    @Test
    fun `stale cache with network sync failure preserves existing local data and does not update metadata`() = runTest {
        // Pre-populate database with existing match
        matchDao.matches[1001L] = MatchEntity(
            id = 1001L,
            homeTeamId = 1,
            awayTeamId = 2,
            homeScore = 0,
            awayScore = 0,
            startTimeDate = "2024-05-10 12:00:00",
            status = "1",
            leagueId = null,
            season = null
        )

        val syncTime = 1_000_000L
        val currentTime = 2_000_000L // Stale

        metadataRepo.metadataFlow.value = DatasetMetadata(
            lastSyncTimestamp = syncTime,
            totalMatches = 1,
            totalTeams = 2,
            totalOddsRecords = 0,
            totalLeagues = 0,
            totalSeasons = 0
        )

        matchApi.errorToThrow = IOException("Server down")

        val syncEngine = DataSyncEngine(
            matchApi = matchApi,
            oddsApi = oddsApi,
            rankingApi = rankingApi,
            database = testDatabase,
            json = json,
            metadataRepository = metadataRepo,
            retryExecutor = RetryExecutor(
                classifier = DefaultRetryClassifier(),
                defaultPolicy = RetryPolicy(maxAttempts = 1)
            ),
            timeProvider = { currentTime }
        )

        val result = syncEngine.syncFullPipelineForDate("2024-05-10", forceRefresh = false)

        assertTrue(result is SyncResult.Failure)
        assertEquals(1, matchDao.matches.size) // Existing data preserved
        assertNotNull(matchDao.matches[1001L])
        assertNull(metadataRepo.lastRefreshedTimestamp) // Metadata NOT refreshed
    }

    @Test
    fun `syncMatchesByDate helper honors forceRefresh and category`() = runTest {
        val syncTime = 1_000_000L
        val currentTime = 1_100_000L // Fresh (100s elapsed)

        metadataRepo.metadataFlow.value = DatasetMetadata(
            lastSyncTimestamp = syncTime,
            totalMatches = 5,
            totalTeams = 10,
            totalOddsRecords = 15,
            totalLeagues = 1,
            totalSeasons = 1
        )

        val syncEngine = DataSyncEngine(
            matchApi = matchApi,
            oddsApi = oddsApi,
            rankingApi = rankingApi,
            database = testDatabase,
            json = json,
            metadataRepository = metadataRepo,
            timeProvider = { currentTime }
        )

        val result = syncEngine.syncMatchesByDate("2024-05-10", forceRefresh = false)

        assertTrue(result.isSuccess)
        assertEquals(0, matchApi.callCount) // Skipped remote sync
    }
}
