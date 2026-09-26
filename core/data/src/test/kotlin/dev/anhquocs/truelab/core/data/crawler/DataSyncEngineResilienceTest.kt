package dev.anhquocs.truelab.core.data.crawler

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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.SocketTimeoutException

class DataSyncEngineResilienceTest {

    private val json = Json { ignoreUnknownKeys = true }

    private class ResilientFakeTeamDao : TeamDao {
        val teams = mutableMapOf<Int, TeamEntity>()
        override fun insertTeams(teams: List<TeamEntity>): LongArray {
            teams.forEach { if (!this.teams.containsKey(it.id)) this.teams[it.id] = it }
            return LongArray(teams.size) { (it + 1).toLong() }
        }
        override fun getTeamById(teamId: Int): Flow<TeamEntity?> = flowOf(teams[teamId])
        override fun searchTeams(query: String): Flow<List<TeamEntity>> =
            flowOf(teams.values.filter { it.name.contains(query, ignoreCase = true) })
    }

    private class ResilientFakeMatchDao : MatchDao {
        val matches = mutableMapOf<Long, MatchEntity>()
        override fun insertMatches(matches: List<MatchEntity>): LongArray {
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

    private class ResilientFakeOddsDao : OddsDao {
        val oddsList = mutableListOf<OddsEntity>()
        override fun insertOdds(odds: List<OddsEntity>): LongArray {
            oddsList.addAll(odds)
            return LongArray(odds.size) { (it + 1).toLong() }
        }
        override fun getOddsHistory(matchId: Long, companyId: Int?, oddsType: String?): Flow<List<OddsEntity>> = flowOf(emptyList())
        override fun getLatestOddsForMatch(matchId: Long): Flow<List<OddsEntity>> = flowOf(emptyList())
    }

    private class ResilientFakeRankingDao : RankingDao {
        val rankings = mutableListOf<SeasonRankingEntity>()
        override fun insertRankings(rankings: List<SeasonRankingEntity>): LongArray {
            this.rankings.addAll(rankings)
            return LongArray(rankings.size) { (it + 1).toLong() }
        }
        override fun getRankingsForMatch(matchId: Long): Flow<List<SeasonRankingEntity>> = flowOf(emptyList())
        override fun getLatestRankingForTeam(teamId: Int): Flow<SeasonRankingEntity?> = flowOf(null)
        override fun getLatestSeasonRankings(): Flow<List<SeasonRankingEntity>> = flowOf(emptyList())
    }

    private class ResilientFakeMatchApi : MatchApi {
        var callCount = 0
        var failuresBeforeSuccess = 0
        var errorToThrow: Throwable? = null
        var responseToReturn: BaseResponse<MatchInfoDetailResponseBase>? = null

        override suspend fun getMatches(
            date: String,
            status: Int?,
            page: Int,
            pageSize: Int,
            sort: String
        ): BaseResponse<MatchInfoDetailResponseBase> {
            callCount++
            if (callCount <= failuresBeforeSuccess) {
                throw errorToThrow ?: SocketTimeoutException("Simulated timeout")
            }
            if (errorToThrow != null && failuresBeforeSuccess == 0) {
                throw errorToThrow!!
            }
            return responseToReturn ?: BaseResponse(
                statusCode = 200,
                message = "OK",
                data = MatchInfoDetailResponseBase(
                    data = listOf(
                        MatchRecord(
                            id = 101L,
                            homeTeam = TeamInfo(1, "Arsenal", "arsenal.png"),
                            awayTeam = TeamInfo(2, "Chelsea", "chelsea.png"),
                            homeScore = 2,
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

    private class ResilientFakeOddsApi : OddsApi {
        var callCount = 0
        var failuresBeforeSuccess = 0
        override suspend fun getOdds(matchId: Long): BaseResponse<List<OddsRecord>> = BaseResponse(200, "OK", emptyList())

        override suspend fun getOddsHistory(
            matchId: Long,
            companyId: Int?,
            oddsType: String?
        ): BaseResponse<OddsHistoryResponse> {
            callCount++
            if (callCount <= failuresBeforeSuccess) {
                throw SocketTimeoutException("Odds timeout attempt $callCount")
            }
            return BaseResponse(
                statusCode = 200,
                message = "OK",
                data = OddsHistoryResponse(
                    data = listOf(
                        OddsRecord(
                            companyId = 8,
                            oddsType = "1",
                            handicap = 0.5,
                            over = 1.9,
                            under = 1.9,
                            homeWin = 1.95,
                            draw = 3.40,
                            awayWin = 3.80,
                            changeTime = 1715340000L
                        )
                    )
                )
            )
        }
    }

    private class ResilientFakeRankingApi : RankingApi {
        var callCount = 0
        var failuresBeforeSuccess = 0
        override suspend fun getSeasonRanking(matchId: Long): BaseResponse<List<SeasonRankResponse>> {
            callCount++
            if (callCount <= failuresBeforeSuccess) {
                throw SocketTimeoutException("Ranking timeout attempt $callCount")
            }
            return BaseResponse(
                statusCode = 200,
                message = "OK",
                data = listOf(
                    SeasonRankResponse(
                        teamId = 1,
                        position = 1,
                        won = 20,
                        draw = 5,
                        loss = 3,
                        goalDiff = 35
                    )
                )
            )
        }
    }

    private lateinit var teamDao: ResilientFakeTeamDao
    private lateinit var matchDao: ResilientFakeMatchDao
    private lateinit var oddsDao: ResilientFakeOddsDao
    private lateinit var rankingDao: ResilientFakeRankingDao
    private lateinit var testDatabase: TrueLabDatabase
    private lateinit var fakeMatchApi: ResilientFakeMatchApi
    private lateinit var fakeOddsApi: ResilientFakeOddsApi
    private lateinit var fakeRankingApi: ResilientFakeRankingApi

    @Before
    fun setUp() {
        teamDao = ResilientFakeTeamDao()
        matchDao = ResilientFakeMatchDao()
        oddsDao = ResilientFakeOddsDao()
        rankingDao = ResilientFakeRankingDao()
        fakeMatchApi = ResilientFakeMatchApi()
        fakeOddsApi = ResilientFakeOddsApi()
        fakeRankingApi = ResilientFakeRankingApi()

        testDatabase = object : TrueLabDatabase() {
            override fun matchDao(): MatchDao = matchDao
            override fun teamDao(): TeamDao = teamDao
            override fun oddsDao(): OddsDao = oddsDao
            override fun rankingDao(): RankingDao = rankingDao
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
    fun `transient failure on matchApi succeeds after 1 retry`() = runTest {
        fakeMatchApi.failuresBeforeSuccess = 1
        val recordedDelays = mutableListOf<Long>()

        val retryExecutor = RetryExecutor(
            classifier = DefaultRetryClassifier(),
            defaultPolicy = RetryPolicy(maxAttempts = 3, jitter = false),
            delayProvider = { recordedDelays.add(it) }
        )

        val syncEngine = DataSyncEngine(
            matchApi = fakeMatchApi,
            oddsApi = fakeOddsApi,
            rankingApi = fakeRankingApi,
            database = testDatabase,
            json = json,
            retryExecutor = retryExecutor
        )

        val result = syncEngine.syncFullPipelineForDate("2024-05-10")

        assertTrue(result is SyncResult.Success)
        assertEquals(2, fakeMatchApi.callCount)
        assertEquals(listOf(1000L), recordedDelays)
        assertEquals(1, matchDao.matches.size)
        assertEquals(2, teamDao.teams.size)
    }

    @Test
    fun `exhausted retry on matchApi returns SyncResult Failure`() = runTest {
        fakeMatchApi.failuresBeforeSuccess = 5
        val recordedDelays = mutableListOf<Long>()

        val retryExecutor = RetryExecutor(
            classifier = DefaultRetryClassifier(),
            defaultPolicy = RetryPolicy(maxAttempts = 3, jitter = false),
            delayProvider = { recordedDelays.add(it) }
        )

        val syncEngine = DataSyncEngine(
            matchApi = fakeMatchApi,
            oddsApi = fakeOddsApi,
            rankingApi = fakeRankingApi,
            database = testDatabase,
            json = json,
            retryExecutor = retryExecutor
        )

        val result = syncEngine.syncFullPipelineForDate("2024-05-10")

        assertTrue(result is SyncResult.Failure)
        assertEquals(3, fakeMatchApi.callCount)
        assertEquals(listOf(1000L, 2000L), recordedDelays)
        assertEquals(0, matchDao.matches.size)
    }

    @Test
    fun `non retryable http 404 on matchApi fails immediately with 1 attempt`() = runTest {
        val body = "{}".toResponseBody("application/json".toMediaType())
        val http404 = HttpException(Response.error<Any>(404, body))
        fakeMatchApi.errorToThrow = http404

        val recordedDelays = mutableListOf<Long>()

        val retryExecutor = RetryExecutor(
            classifier = DefaultRetryClassifier(),
            defaultPolicy = RetryPolicy(maxAttempts = 3, jitter = false),
            delayProvider = { recordedDelays.add(it) }
        )

        val syncEngine = DataSyncEngine(
            matchApi = fakeMatchApi,
            oddsApi = fakeOddsApi,
            rankingApi = fakeRankingApi,
            database = testDatabase,
            json = json,
            retryExecutor = retryExecutor
        )

        val result = syncEngine.syncFullPipelineForDate("2024-05-10")

        assertTrue(result is SyncResult.Failure)
        assertEquals(1, fakeMatchApi.callCount)
        assertTrue(recordedDelays.isEmpty())
    }

    @Test
    fun `transient failure on oddsApi succeeds after retry`() = runTest {
        fakeOddsApi.failuresBeforeSuccess = 1
        val recordedDelays = mutableListOf<Long>()

        val retryExecutor = RetryExecutor(
            classifier = DefaultRetryClassifier(),
            defaultPolicy = RetryPolicy(maxAttempts = 3, jitter = false),
            delayProvider = { recordedDelays.add(it) }
        )

        val syncEngine = DataSyncEngine(
            matchApi = fakeMatchApi,
            oddsApi = fakeOddsApi,
            rankingApi = fakeRankingApi,
            database = testDatabase,
            json = json,
            retryExecutor = retryExecutor
        )

        val result = syncEngine.syncOddsHistoryForMatch(101L)

        assertTrue(result.isSuccess)
        assertEquals(2, fakeOddsApi.callCount)
        assertEquals(listOf(1000L), recordedDelays)
        assertEquals(1, oddsDao.oddsList.size)
    }

    @Test
    fun `transient failure on rankingApi succeeds after retry`() = runTest {
        fakeRankingApi.failuresBeforeSuccess = 1
        val recordedDelays = mutableListOf<Long>()

        val retryExecutor = RetryExecutor(
            classifier = DefaultRetryClassifier(),
            defaultPolicy = RetryPolicy(maxAttempts = 3, jitter = false),
            delayProvider = { recordedDelays.add(it) }
        )

        val syncEngine = DataSyncEngine(
            matchApi = fakeMatchApi,
            oddsApi = fakeOddsApi,
            rankingApi = fakeRankingApi,
            database = testDatabase,
            json = json,
            retryExecutor = retryExecutor
        )

        val result = syncEngine.syncSeasonRankingForMatch(101L)

        assertTrue(result.isSuccess)
        assertEquals(2, fakeRankingApi.callCount)
        assertEquals(listOf(1000L), recordedDelays)
        assertEquals(1, rankingDao.rankings.size)
    }
}
