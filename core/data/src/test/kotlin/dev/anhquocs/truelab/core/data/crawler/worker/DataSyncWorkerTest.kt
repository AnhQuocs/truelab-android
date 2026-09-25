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
    }

    private class WorkerTestMatchDao : MatchDao {
        val matches = mutableMapOf<Long, MatchEntity>()
        override fun insertMatches(matches: List<MatchEntity>): LongArray {
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
    }

    private class WorkerTestMatchApi : MatchApi {
        var lastRequestedDate: String? = null
        var callCount = 0
        var errorToThrow: Throwable? = null

        override suspend fun getMatches(
            date: String, status: Int, page: Int, pageSize: Int, sort: String
        ): BaseResponse<MatchInfoDetailResponseBase> {
            callCount++
            lastRequestedDate = date
            errorToThrow?.let { throw it }
            return BaseResponse(
                200, "OK", MatchInfoDetailResponseBase(
                    listOf(MatchRecord(3001L, TeamInfo(1, "LIV", ""), TeamInfo(2, "MCI", ""), 2, 2, "$date 17:30:00", "8")),
                    MetaResponse(1, 1)
                )
            )
        }
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

    private lateinit var teamDao: WorkerTestTeamDao
    private lateinit var matchDao: WorkerTestMatchDao
    private lateinit var matchApi: WorkerTestMatchApi
    private lateinit var oddsApi: WorkerTestOddsApi
    private lateinit var rankingApi: WorkerTestRankingApi
    private lateinit var metadataRepo: WorkerTestMetadataRepository
    private lateinit var testDatabase: TrueLabDatabase

    @Before
    fun setUp() {
        teamDao = WorkerTestTeamDao()
        matchDao = WorkerTestMatchDao()
        matchApi = WorkerTestMatchApi()
        oddsApi = WorkerTestOddsApi()
        rankingApi = WorkerTestRankingApi()
        metadataRepo = WorkerTestMetadataRepository()

        testDatabase = object : TrueLabDatabase() {
            override fun matchDao(): MatchDao = matchDao
            override fun teamDao(): TeamDao = teamDao
            override fun oddsDao() = throw NotImplementedError()
            override fun rankingDao() = throw NotImplementedError()
            override fun leagueDao() = throw NotImplementedError()
            override fun seasonDao() = throw NotImplementedError()
            override fun datasetMetadataDao() = throw NotImplementedError()
            override fun predictionDao() = throw NotImplementedError()
            override fun clearAllTables() {}
            override fun createInvalidationTracker() = androidx.room.InvalidationTracker(this, "teams", "matches")
            override fun createOpenHelper(config: androidx.room.DatabaseConfiguration) = throw UnsupportedOperationException()
            override fun <T> runInTransaction(body: java.util.concurrent.Callable<T>): T = body.call()
            override fun runInTransaction(body: Runnable) = body.run()
        }
    }

    private fun createWorkerParameters(data: Data = Data.EMPTY): WorkerParameters {
        val constructor = WorkerParameters::class.java.declaredConstructors[0]
        constructor.isAccessible = true
        val params = arrayOfNulls<Any>(constructor.parameterTypes.size)
        val dummyFuture = object : com.google.common.util.concurrent.ListenableFuture<Void?> {
            override fun cancel(mayInterruptIfRunning: Boolean) = false
            override fun isCancelled() = false
            override fun isDone() = true
            override fun get(): Void? = null
            override fun get(t: Long, u: java.util.concurrent.TimeUnit): Void? = null
            override fun addListener(r: Runnable, e: java.util.concurrent.Executor) = r.run()
        }
        for (i in constructor.parameterTypes.indices) {
            val type = constructor.parameterTypes[i]
            params[i] = when {
                type == UUID::class.java -> UUID.randomUUID()
                type == Data::class.java -> data
                type == java.util.Collection::class.java || type == Set::class.java || type == List::class.java -> emptySet<String>()
                type == WorkerParameters.RuntimeExtras::class.java -> WorkerParameters.RuntimeExtras()
                type == Int::class.javaPrimitiveType -> 1
                type == java.util.concurrent.Executor::class.java -> java.util.concurrent.Executors.newSingleThreadExecutor()
                type == kotlin.coroutines.CoroutineContext::class.java -> kotlinx.coroutines.Dispatchers.Unconfined
                type == androidx.work.WorkerFactory::class.java -> object : androidx.work.WorkerFactory() {
                    override fun createWorker(appContext: android.content.Context, workerClassName: String, workerParameters: WorkerParameters) = null
                }
                type == androidx.work.ProgressUpdater::class.java -> androidx.work.ProgressUpdater { _, _, _ -> dummyFuture }
                type == androidx.work.ForegroundUpdater::class.java -> androidx.work.ForegroundUpdater { _, _, _ -> dummyFuture }
                else -> null
            }
        }
        return constructor.newInstance(*params) as WorkerParameters
    }

    private class FakeContext : android.content.ContextWrapper(null)

    @Test
    fun `worker returns success when data sync succeeds`() = runTest {
        val syncEngine = DataSyncEngine(
            matchApi = matchApi,
            oddsApi = oddsApi,
            rankingApi = rankingApi,
            database = testDatabase,
            json = json,
            metadataRepository = metadataRepo
        )

        val worker = DataSyncWorker(
            appContext = FakeContext(),
            workerParams = createWorkerParameters(),
            dataSyncEngine = syncEngine
        )

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(1, matchApi.callCount)
        assertEquals(1, matchDao.matches.size)
    }

    @Test
    fun `worker returns retry on retryable failure`() = runTest {
        matchApi.errorToThrow = SocketTimeoutException("Connection timed out")

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
            )
        )

        val worker = DataSyncWorker(
            appContext = FakeContext(),
            workerParams = createWorkerParameters(),
            dataSyncEngine = syncEngine
        )

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun `worker returns failure on non-retryable failure`() = runTest {
        matchApi.errorToThrow = IllegalArgumentException("Bad request parameter")

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
            )
        )

        val worker = DataSyncWorker(
            appContext = FakeContext(),
            workerParams = createWorkerParameters(),
            dataSyncEngine = syncEngine
        )

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun `worker rethrows CancellationException and does not swallow`() = runTest {
        matchApi.errorToThrow = CancellationException("Job was cancelled")

        val syncEngine = DataSyncEngine(
            matchApi = matchApi,
            oddsApi = oddsApi,
            rankingApi = rankingApi,
            database = testDatabase,
            json = json,
            metadataRepository = metadataRepo
        )

        val worker = DataSyncWorker(
            appContext = FakeContext(),
            workerParams = createWorkerParameters(),
            dataSyncEngine = syncEngine
        )

        try {
            worker.doWork()
            fail("Expected CancellationException to be rethrown")
        } catch (e: CancellationException) {
            assertEquals("Job was cancelled", e.message)
        }
    }

    @Test
    fun `worker reads input data parameters correctly`() = runTest {
        val syncEngine = DataSyncEngine(
            matchApi = matchApi,
            oddsApi = oddsApi,
            rankingApi = rankingApi,
            database = testDatabase,
            json = json,
            metadataRepository = metadataRepo
        )

        val inputData = Data.Builder()
            .putString(DataSyncWorker.KEY_TARGET_DATE, "2024-06-20")
            .putBoolean(DataSyncWorker.KEY_FORCE_REFRESH, true)
            .putString(DataSyncWorker.KEY_DATASET_CATEGORY, DatasetCategory.LIVE_MATCHES.name)
            .build()

        val worker = DataSyncWorker(
            appContext = FakeContext(),
            workerParams = createWorkerParameters(inputData),
            dataSyncEngine = syncEngine
        )

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals("2024-06-20", matchApi.lastRequestedDate)
    }

    @Test
    fun `worker falls back to SCHEDULED_MATCHES when category name is invalid`() = runTest {
        val syncEngine = DataSyncEngine(
            matchApi = matchApi,
            oddsApi = oddsApi,
            rankingApi = rankingApi,
            database = testDatabase,
            json = json,
            metadataRepository = metadataRepo
        )

        val inputData = Data.Builder()
            .putString(DataSyncWorker.KEY_DATASET_CATEGORY, "INVALID_CATEGORY_NAME")
            .build()

        val worker = DataSyncWorker(
            appContext = FakeContext(),
            workerParams = createWorkerParameters(inputData),
            dataSyncEngine = syncEngine
        )

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `worker uses default date format when target_date is not provided`() = runTest {
        val syncEngine = DataSyncEngine(
            matchApi = matchApi,
            oddsApi = oddsApi,
            rankingApi = rankingApi,
            database = testDatabase,
            json = json,
            metadataRepository = metadataRepo
        )

        val worker = DataSyncWorker(
            appContext = FakeContext(),
            workerParams = createWorkerParameters(),
            dataSyncEngine = syncEngine
        )

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        // Verify last requested date matches yyyy-MM-dd pattern
        val datePattern = Regex("""\d{4}-\d{2}-\d{2}""")
        val requestedDate = matchApi.lastRequestedDate
        org.junit.Assert.assertNotNull(requestedDate)
        org.junit.Assert.assertTrue(requestedDate!!.matches(datePattern))
    }
}
