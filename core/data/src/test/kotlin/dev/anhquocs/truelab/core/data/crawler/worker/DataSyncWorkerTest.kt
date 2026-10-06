package dev.anhquocs.truelab.core.data.crawler.worker

import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import dev.anhquocs.truelab.core.data.crawler.DataSyncEngine
import dev.anhquocs.truelab.core.data.crawler.cache.DatasetCategory
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
import dev.anhquocs.truelab.core.data.odds.remote.api.OddsApi
import dev.anhquocs.truelab.core.data.odds.remote.dto.OddsHistoryResponse
import dev.anhquocs.truelab.core.data.odds.remote.dto.OddsRecord
import dev.anhquocs.truelab.core.data.prediction.local.dao.PredictionDao
import dev.anhquocs.truelab.core.data.ranking.local.dao.RankingDao
import dev.anhquocs.truelab.core.data.ranking.remote.api.RankingApi
import dev.anhquocs.truelab.core.data.ranking.remote.dto.SeasonRankResponse
import dev.anhquocs.truelab.core.data.remote.dto.BaseResponse
import dev.anhquocs.truelab.core.data.remote.dto.MetaResponse
import dev.anhquocs.truelab.core.data.team.local.dao.TeamDao
import dev.anhquocs.truelab.core.data.team.local.entity.TeamEntity
import dev.anhquocs.truelab.core.domain.metadata.model.DatasetMetadata
import dev.anhquocs.truelab.core.domain.metadata.repository.DatasetMetadataRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.UUID

class DataSyncWorkerTest {

    private val json = Json { ignoreUnknownKeys = true }

    private class WorkerTestTeamDao : TeamDao {
        val teams = mutableMapOf<Int, TeamEntity>()
        override fun insertTeams(teams: List<TeamEntity>): LongArray {
            teams.forEach { this.teams[it.id] = it }
            return LongArray(teams.size) { (it + 1).toLong() }
        }
        override fun getTeamById(teamId: Int) = throw NotImplementedError()
        override fun searchTeams(query: String) = throw NotImplementedError()
        override fun searchTeams(query: String, limit: Int) = throw NotImplementedError()
        override fun getTeams(limit: Int) = throw NotImplementedError()
    }

    private class WorkerTestMatchDao : MatchDao {
        val matches = mutableMapOf<Long, MatchEntity>()
        override fun insertMatches(matches: List<MatchEntity>): LongArray {
            matches.forEach { this.matches[it.id] = it }
            return LongArray(matches.size) { (it + 1).toLong() }
        }
        override fun upsertMatches(matches: List<MatchEntity>): LongArray {
            matches.forEach { this.matches[it.id] = it }
            return LongArray(matches.size) { (it + 1).toLong() }
        }
        override fun getMatchById(matchId: Long) = throw NotImplementedError()
        override fun getMatchesPaged(limit: Int, offset: Int) = throw NotImplementedError()
        override fun getMatchesByDate(date: String) = throw NotImplementedError()
        override fun getMatchesByStatus(status: String) = throw NotImplementedError()
        override fun getH2HMatches(teamAId: Int, teamBId: Int) = throw NotImplementedError()
        override fun getRecentMatchesForTeam(teamId: Int, limit: Int) = throw NotImplementedError()
        override fun getMatchesByLeague(leagueId: Int) = throw NotImplementedError()
        override fun getMatchesByLeagueAndSeason(leagueId: Int, season: String) = throw NotImplementedError()
        override fun getAllMatches() = throw NotImplementedError()
        override fun getPredictableMatches(limit: Int) = throw NotImplementedError()
        override fun searchMatches(query: String, limit: Int) = throw NotImplementedError()
        override fun getPredictableMatchesFiltered(
            startDateUtc: String?,
            endDateUtc: String?,
            isPastDate: Boolean,
            isFutureDate: Boolean,
            leagueId: Int?,
            statusFilter: String,
            searchQuery: String?,
            limit: Int
        ) = throw NotImplementedError()
    }

    private class WorkerTestMatchApi : MatchApi {
        var lastRequestedDate: String? = null
        var callCount = 0
        var errorToThrow: Throwable? = null

        override suspend fun getMatches(
            date: String, status: Int?, page: Int, pageSize: Int, sort: String
        ): BaseResponse<MatchInfoDetailResponseBase> {
            callCount++
            lastRequestedDate = date
            errorToThrow?.let { throw it }
            return BaseResponse(
                200, "OK", MatchInfoDetailResponseBase(
                    listOf(MatchRecord(id = 3001L, homeTeam = TeamInfo(1, "LIV", ""), awayTeam = TeamInfo(2, "MCI", ""), homeScore = 2, awayScore = 2, startTimeDate = "$date 17:30:00", status = "8", competitionId = 927)),
                    MetaResponse(1, 1)
                )
            )
        }

        override suspend fun getSeasonMatches(
            seasonId: Long,
            status: Int,
            pageSize: Int,
            page: Int
        ): BaseResponse<MatchInfoDetailResponseBase> = BaseResponse(
            200, "OK", MatchInfoDetailResponseBase(emptyList(), MetaResponse(1, 1))
        )
    }

    private class WorkerTestOddsApi : OddsApi {
        override suspend fun getOdds(matchId: Long) = BaseResponse(200, "OK", emptyList<OddsRecord>())
        override suspend fun getOddsHistory(matchId: Long, companyId: Int?, oddsType: String?) =
            BaseResponse(200, "OK", OddsHistoryResponse(emptyList()))
    }

    private class WorkerTestRankingApi : RankingApi {
        override suspend fun getSeasonRanking(matchId: Long) = BaseResponse(200, "OK", emptyList<SeasonRankResponse>())
    }

    private class WorkerTestMetadataRepository : DatasetMetadataRepository {
        val metadataFlow = MutableStateFlow<DatasetMetadata?>(null)
        var lastRefreshedTimestamp: Long? = null
        override fun getMetadata(key: String): Flow<DatasetMetadata?> = metadataFlow
        override suspend fun refreshSnapshot(timestamp: Long): Result<DatasetMetadata> {
            lastRefreshedTimestamp = timestamp
            val updated = DatasetMetadata(
                lastSyncTimestamp = timestamp,
                totalMatches = 10, totalTeams = 20, totalOddsRecords = 50,
                totalLeagues = 5, totalSeasons = 5
            )
            metadataFlow.value = updated
            return Result.success(updated)
        }
    }

    private class WorkerTestOddsDao : OddsDao {
        val odds = mutableListOf<dev.anhquocs.truelab.core.data.odds.local.entity.OddsEntity>()
        override fun insertOdds(odds: List<dev.anhquocs.truelab.core.data.odds.local.entity.OddsEntity>): LongArray {
            this.odds.addAll(odds)
            return LongArray(odds.size) { (it + 1).toLong() }
        }
        override fun getOddsHistory(matchId: Long, companyId: Int?, oddsType: String?) = throw NotImplementedError()
        override fun getOddsForMatch(matchId: Long) = throw NotImplementedError()
        override fun getOddsListForMatch(matchId: Long): List<dev.anhquocs.truelab.core.data.odds.local.entity.OddsEntity> = odds.filter { it.matchId == matchId }
        override fun getLatestOddsForMatch(matchId: Long) = throw NotImplementedError()
        override fun getLatestPreMatchEuropeanOddsForAllMatches(): Flow<List<dev.anhquocs.truelab.core.data.odds.local.entity.OddsEntity>> = flowOf(emptyList())
    }

    private class WorkerTestRankingDao : RankingDao {
        val rankings = mutableListOf<dev.anhquocs.truelab.core.data.ranking.local.entity.SeasonRankingEntity>()
        override fun insertRankings(rankings: List<dev.anhquocs.truelab.core.data.ranking.local.entity.SeasonRankingEntity>): LongArray {
            this.rankings.addAll(rankings)
            return LongArray(rankings.size) { (it + 1).toLong() }
        }
        override fun getRankingsForMatch(matchId: Long) = throw NotImplementedError()
        override fun getLatestRankingForTeam(teamId: Int) = throw NotImplementedError()
        override fun getLatestSeasonRankings() = throw NotImplementedError()
    }

    private lateinit var teamDao: WorkerTestTeamDao
    private lateinit var matchDao: WorkerTestMatchDao
    private lateinit var oddsDao: WorkerTestOddsDao
    private lateinit var rankingDao: WorkerTestRankingDao
    private lateinit var matchApi: WorkerTestMatchApi
    private lateinit var oddsApi: WorkerTestOddsApi
    private lateinit var rankingApi: WorkerTestRankingApi
    private lateinit var metadataRepo: WorkerTestMetadataRepository
    private lateinit var testDatabase: TrueLabDatabase

    @Before
    fun setUp() {
        teamDao = WorkerTestTeamDao()
        matchDao = WorkerTestMatchDao()
        oddsDao = WorkerTestOddsDao()
        rankingDao = WorkerTestRankingDao()
        matchApi = WorkerTestMatchApi()
        oddsApi = WorkerTestOddsApi()
        rankingApi = WorkerTestRankingApi()
        metadataRepo = WorkerTestMetadataRepository()

        leagueDao = WorkerTestLeagueDao()

        testDatabase = object : TrueLabDatabase() {
            override fun matchDao(): MatchDao = matchDao
            override fun teamDao(): TeamDao = teamDao
            override fun oddsDao(): OddsDao = oddsDao
            override fun rankingDao(): RankingDao = rankingDao
            override fun leagueDao(): dev.anhquocs.truelab.core.data.league.local.dao.LeagueDao = leagueDao
            override fun seasonDao(): dev.anhquocs.truelab.core.data.league.local.dao.SeasonDao = throw NotImplementedError()
            override fun datasetMetadataDao(): dev.anhquocs.truelab.core.data.metadata.local.dao.DatasetMetadataDao = throw NotImplementedError()
            override fun predictionDao(): dev.anhquocs.truelab.core.data.prediction.local.dao.PredictionDao = throw NotImplementedError()
            override fun clearAllTables() {}
            override fun createInvalidationTracker() = androidx.room.InvalidationTracker(this, "teams", "matches", "odds", "season_rankings")
            override fun createOpenHelper(config: androidx.room.DatabaseConfiguration) = throw UnsupportedOperationException()
            override fun <T> runInTransaction(body: java.util.concurrent.Callable<T>): T = body.call()
            override fun runInTransaction(body: Runnable) = body.run()
        }
    }

    private lateinit var leagueDao: dev.anhquocs.truelab.core.data.league.local.dao.LeagueDao

    private class WorkerTestLeagueDao : dev.anhquocs.truelab.core.data.league.local.dao.LeagueDao {
        override fun insertLeagues(leagues: List<dev.anhquocs.truelab.core.data.league.local.entity.LeagueEntity>): LongArray =
            LongArray(leagues.size) { (it + 1).toLong() }
        override fun getLeagues(): Flow<List<dev.anhquocs.truelab.core.data.league.local.entity.LeagueEntity>> = flowOf(emptyList())
        override fun getLeagueById(leagueId: Int): Flow<dev.anhquocs.truelab.core.data.league.local.entity.LeagueEntity?> = flowOf(null)
    }

    private fun createWorkerParameters(data: Data = Data.EMPTY): WorkerParameters {
        val constructor = WorkerParameters::class.java.declaredConstructors[0].apply { isAccessible = true }
        val dummyFuture = object : com.google.common.util.concurrent.ListenableFuture<Void?> {
            override fun cancel(mayInterruptIfRunning: Boolean) = false
            override fun isCancelled() = false
            override fun isDone() = true
            override fun get(): Void? = null
            override fun get(t: Long, u: java.util.concurrent.TimeUnit): Void? = null
            override fun addListener(r: Runnable, e: java.util.concurrent.Executor) = r.run()
        }
        val params = Array(constructor.parameterTypes.size) { i ->
            when (val type = constructor.parameterTypes[i]) {
                UUID::class.java -> UUID.randomUUID()
                Data::class.java -> data
                java.util.Collection::class.java, Set::class.java, List::class.java -> emptySet<String>()
                WorkerParameters.RuntimeExtras::class.java -> WorkerParameters.RuntimeExtras()
                Int::class.javaPrimitiveType -> 1
                java.util.concurrent.Executor::class.java -> java.util.concurrent.Executors.newSingleThreadExecutor()
                kotlin.coroutines.CoroutineContext::class.java -> kotlinx.coroutines.Dispatchers.Unconfined
                androidx.work.WorkerFactory::class.java -> object : androidx.work.WorkerFactory() {
                    override fun createWorker(appContext: android.content.Context, workerClassName: String, workerParameters: WorkerParameters) = null
                }
                androidx.work.ProgressUpdater::class.java -> androidx.work.ProgressUpdater { _, _, _ -> dummyFuture }
                androidx.work.ForegroundUpdater::class.java -> androidx.work.ForegroundUpdater { _, _, _ -> dummyFuture }
                else -> null
            }
        }
        return constructor.newInstance(*params) as WorkerParameters
    }

    private class FakeContext : android.content.ContextWrapper(null)

    private fun createSyncEngine(retryExecutor: RetryExecutor = RetryExecutor()): DataSyncEngine {
        return DataSyncEngine(
            matchApi = matchApi,
            oddsApi = oddsApi,
            rankingApi = rankingApi,
            database = testDatabase,
            json = json,
            metadataRepository = metadataRepo,
            retryExecutor = retryExecutor
        )
    }

    private fun createWorker(syncEngine: DataSyncEngine, data: Data = Data.EMPTY): DataSyncWorker {
        return DataSyncWorker(
            appContext = FakeContext(),
            workerParams = createWorkerParameters(data),
            dataSyncEngine = syncEngine
        )
    }

    @Test
    fun `worker returns success when data sync succeeds`() = runTest {
        val worker = createWorker(createSyncEngine())
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(1, matchApi.callCount)
        assertEquals(1, matchDao.matches.size)
    }

    @Test
    fun `worker returns retry on retryable failure`() = runTest {
        matchApi.errorToThrow = SocketTimeoutException("Connection timed out")
        val syncEngine = createSyncEngine(
            RetryExecutor(classifier = DefaultRetryClassifier(), defaultPolicy = RetryPolicy(maxAttempts = 1))
        )
        val worker = createWorker(syncEngine)

        assertEquals(ListenableWorker.Result.retry(), worker.doWork())
    }

    @Test
    fun `worker returns failure on non-retryable failure`() = runTest {
        matchApi.errorToThrow = IllegalArgumentException("Bad request parameter")
        val syncEngine = createSyncEngine(
            RetryExecutor(classifier = DefaultRetryClassifier(), defaultPolicy = RetryPolicy(maxAttempts = 1))
        )
        val worker = createWorker(syncEngine)

        assertEquals(ListenableWorker.Result.failure(), worker.doWork())
    }

    @Test
    fun `worker rethrows CancellationException and does not swallow`() = runTest {
        matchApi.errorToThrow = CancellationException("Job was cancelled")
        val worker = createWorker(createSyncEngine())

        try {
            worker.doWork()
            fail("Expected CancellationException to be rethrown")
        } catch (e: CancellationException) {
            assertEquals("Job was cancelled", e.message)
        }
    }

    @Test
    fun `worker reads input data parameters correctly`() = runTest {
        val inputData = Data.Builder()
            .putString(DataSyncWorker.KEY_TARGET_DATE, "2024-06-20")
            .putBoolean(DataSyncWorker.KEY_FORCE_REFRESH, true)
            .putString(DataSyncWorker.KEY_DATASET_CATEGORY, DatasetCategory.LIVE_MATCHES.name)
            .build()
        val worker = createWorker(createSyncEngine(), inputData)

        assertEquals(ListenableWorker.Result.success(), worker.doWork())
        assertEquals("2024-06-20", matchApi.lastRequestedDate)
    }

    @Test
    fun `worker falls back to SCHEDULED_MATCHES when category name is invalid`() = runTest {
        val inputData = Data.Builder()
            .putString(DataSyncWorker.KEY_DATASET_CATEGORY, "INVALID_CATEGORY_NAME")
            .build()
        val worker = createWorker(createSyncEngine(), inputData)

        assertEquals(ListenableWorker.Result.success(), worker.doWork())
    }

    @Test
    fun `worker uses default date format when target_date is not provided`() = runTest {
        val worker = createWorker(createSyncEngine())

        assertEquals(ListenableWorker.Result.success(), worker.doWork())
        val datePattern = Regex("""\d{4}-\d{2}-\d{2}""")
        val requestedDate = matchApi.lastRequestedDate
        org.junit.Assert.assertNotNull(requestedDate)
        org.junit.Assert.assertTrue(requestedDate!!.matches(datePattern))
    }
}
