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
import dev.anhquocs.truelab.core.data.match.remote.dto.CompetitionSummaryInfo
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZoneId

class DataSyncEngineRuntimeSyncTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val vietnamZone = ZoneId.of("Asia/Ho_Chi_Minh")

    private class FakeTeamDao : TeamDao {
        val teams = mutableMapOf<Int, TeamEntity>()
        override fun insertTeams(teams: List<TeamEntity>): LongArray {
            teams.forEach { team -> if (!this.teams.containsKey(team.id)) this.teams[team.id] = team }
            return LongArray(teams.size) { (it + 1).toLong() }
        }
        override fun getTeamById(teamId: Int): Flow<TeamEntity?> = flowOf(teams[teamId])
        override fun searchTeams(query: String): Flow<List<TeamEntity>> = flowOf(emptyList())
        override fun searchTeams(query: String, limit: Int): Flow<List<TeamEntity>> = flowOf(emptyList())
        override fun getTeams(limit: Int): Flow<List<TeamEntity>> = flowOf(emptyList())
    }

    private class FakeMatchDao : MatchDao {
        val matches = mutableMapOf<Long, MatchEntity>()
        override fun insertMatches(matches: List<MatchEntity>): LongArray {
            matches.forEach { this.matches[it.id] = it }
            return LongArray(matches.size) { (it + 1).toLong() }
        }
        override fun upsertMatches(matches: List<MatchEntity>): LongArray {
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
        override fun getAllMatches(): Flow<List<MatchWithTeams>> = flowOf(emptyList())
        override fun getPredictableMatches(limit: Int): Flow<List<MatchWithTeams>> = flowOf(emptyList())
        override fun searchMatches(query: String, limit: Int): Flow<List<MatchWithTeams>> = flowOf(emptyList())
        override fun getPredictableMatchesFiltered(
            startDateUtc: String?,
            endDateUtc: String?,
            isPastDate: Boolean,
            isFutureDate: Boolean,
            leagueId: Int?,
            statusFilter: String,
            searchQuery: String?,
            limit: Int
        ): Flow<List<MatchWithTeams>> = flowOf(emptyList())
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
        override fun getOddsForMatch(matchId: Long): Flow<List<OddsEntity>> = flowOf(oddsList.filter { it.matchId == matchId })
        override fun getOddsListForMatch(matchId: Long): List<OddsEntity> = oddsList.filter { it.matchId == matchId }
        override fun getLatestOddsForMatch(matchId: Long): Flow<List<OddsEntity>> = flowOf(emptyList())
        override fun getLatestPreMatchEuropeanOddsForAllMatches(): Flow<List<OddsEntity>> = flowOf(emptyList())
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
        override fun createInvalidationTracker(): androidx.room.InvalidationTracker = androidx.room.InvalidationTracker(this, "teams", "matches")
        override fun createOpenHelper(config: androidx.room.DatabaseConfiguration): androidx.sqlite.db.SupportSQLiteOpenHelper = throw UnsupportedOperationException()
        override fun runInTransaction(body: Runnable) { body.run() }
        override fun <V : Any?> runInTransaction(body: java.util.concurrent.Callable<V>): V = body.call()
    }

    private class FakeMatchApi : MatchApi {
        val responsesByDateAndPage = mutableMapOf<Pair<String, Int>, BaseResponse<MatchInfoDetailResponseBase>>()
        val requestedCalls = mutableListOf<Pair<String, Int>>()

        override suspend fun getMatches(
            date: String,
            status: Int?,
            page: Int,
            pageSize: Int,
            sort: String
        ): BaseResponse<MatchInfoDetailResponseBase> {
            requestedCalls.add(date to page)
            return responsesByDateAndPage[date to page] ?: BaseResponse(
                statusCode = 200,
                message = "OK",
                data = MatchInfoDetailResponseBase(data = emptyList(), meta = MetaResponse(1, 1, 1))
            )
        }

        override suspend fun getSeasonMatches(
            seasonId: Long,
            status: Int,
            pageSize: Int,
            page: Int
        ): BaseResponse<MatchInfoDetailResponseBase> {
            return BaseResponse(
                statusCode = 200,
                message = "OK",
                data = MatchInfoDetailResponseBase(data = emptyList(), meta = MetaResponse(1, 1, 1))
            )
        }
    }

    private lateinit var matchApi: FakeMatchApi
    private lateinit var teamDao: FakeTeamDao
    private lateinit var matchDao: FakeMatchDao
    private lateinit var leagueDao: FakeLeagueDao
    private lateinit var syncEngine: DataSyncEngine

    @Before
    fun setup() {
        matchApi = FakeMatchApi()
        teamDao = FakeTeamDao()
        matchDao = FakeMatchDao()
        leagueDao = FakeLeagueDao()

        val testDb = TestTrueLabDatabase(
            teamDaoImpl = teamDao,
            matchDaoImpl = matchDao,
            leagueDaoImpl = leagueDao,
            seasonDaoImpl = FakeSeasonDao(),
            oddsDaoImpl = FakeOddsDao(),
            rankingDaoImpl = FakeRankingDao()
        )

        syncEngine = DataSyncEngine(
            matchApi = matchApi,
            oddsApi = object : OddsApi {
                override suspend fun getOdds(matchId: Long): BaseResponse<List<OddsRecord>> = BaseResponse(200, "OK", emptyList())
                override suspend fun getOddsHistory(matchId: Long, companyId: Int?, oddsType: String?): BaseResponse<OddsHistoryResponse> = BaseResponse(200, "OK", OddsHistoryResponse(emptyList()))
            },
            rankingApi = object : RankingApi {
                override suspend fun getSeasonRanking(matchId: Long): BaseResponse<List<SeasonRankResponse>> = BaseResponse(200, "OK", emptyList())
            },
            competitionApi = null,
            database = testDb,
            json = json
        )
    }

    private fun createSampleMatch(
        id: Long,
        startTimeDate: String,
        status: String = "ended",
        homeScore: Int = 2,
        awayScore: Int = 2,
        competitionId: Int = 100
    ): MatchRecord {
        return MatchRecord(
            id = id,
            homeTeam = TeamInfo(id = 1, name = "Greece"),
            awayTeam = TeamInfo(id = 2, name = "Netherlands"),
            homeScore = homeScore,
            awayScore = awayScore,
            startTimeDate = startTimeDate,
            status = status,
            minutes = "FT",
            competitionId = competitionId,
            competition = CompetitionSummaryInfo(id = competitionId, name = "UEFA Nations League")
        )
    }

    @Test
    fun syncMatchesForLocalDate_greeceMatch129243OnPreviousUtcDate_persistedCorrectly() = runTest {
        // Match 129243 is on 2026-10-01 18:45:00 UTC (which is 01:45 VN on 2026-10-02)
        val match129243 = createSampleMatch(129243L, "2026-10-01T18:45:00Z", status = "ended", homeScore = 2, awayScore = 2)

        matchApi.responsesByDateAndPage["2026-10-01" to 1] = BaseResponse(
            statusCode = 200,
            message = "OK",
            data = MatchInfoDetailResponseBase(
                data = listOf(match129243),
                meta = MetaResponse(currentPage = 1, lastPage = 1)
            )
        )

        val match202 = createSampleMatch(202L, "2026-10-02T10:00:00Z", status = "pending", homeScore = 0, awayScore = 0)
        matchApi.responsesByDateAndPage["2026-10-02" to 1] = BaseResponse(
            statusCode = 200,
            message = "OK",
            data = MatchInfoDetailResponseBase(
                data = listOf(match202),
                meta = MetaResponse(currentPage = 1, lastPage = 1)
            )
        )

        val result = syncEngine.syncMatchesForLocalDate("2026-10-02", vietnamZone, forceRefresh = true)

        assertTrue(result is SyncResult.Success)
        val summary = (result as SyncResult.Success).data
        assertEquals(2, summary.matchesSynced)

        // Verify Match 129243 in Room MatchDao
        val savedGreeceMatch = matchDao.matches[129243L]
        assertNotNull(savedGreeceMatch)
        assertEquals("ended", savedGreeceMatch?.status)
        assertEquals(2, savedGreeceMatch?.homeScore)
        assertEquals(2, savedGreeceMatch?.awayScore)
        assertEquals("2026-10-01T18:45:00Z", savedGreeceMatch?.startTimeDate)

        // Verify both UTC dates were queried
        assertTrue(matchApi.requestedCalls.contains("2026-10-01" to 1))
        assertTrue(matchApi.requestedCalls.contains("2026-10-02" to 1))
    }

    @Test
    fun syncMatchesForLocalDate_multiPagePagination_fetchesAllPages() = runTest {
        val m1 = createSampleMatch(1L, "2026-10-02T02:00:00Z")
        val m2 = createSampleMatch(2L, "2026-10-02T05:00:00Z")

        matchApi.responsesByDateAndPage["2026-10-02" to 1] = BaseResponse(
            statusCode = 200,
            message = "OK",
            data = MatchInfoDetailResponseBase(
                data = listOf(m1),
                meta = MetaResponse(currentPage = 1, lastPage = 2)
            )
        )
        matchApi.responsesByDateAndPage["2026-10-02" to 2] = BaseResponse(
            statusCode = 200,
            message = "OK",
            data = MatchInfoDetailResponseBase(
                data = listOf(m2),
                meta = MetaResponse(currentPage = 2, lastPage = 2)
            )
        )

        val result = syncEngine.syncMatchesForLocalDate("2026-10-02", vietnamZone, forceRefresh = true)

        assertTrue(result is SyncResult.Success)
        assertTrue(matchApi.requestedCalls.contains("2026-10-02" to 1))
        assertTrue(matchApi.requestedCalls.contains("2026-10-02" to 2))
        assertEquals(2, matchDao.matches.size)
    }

    @Test
    fun syncMatchesForLocalDate_filtersOutMatchesOutsideLocalDayWindow() = runTest {
        // 2026-10-01 16:00:00Z is 23:00 VN on 2026-10-01 (Outside 2026-10-02 VN)
        val outsideMatch = createSampleMatch(999L, "2026-10-01T16:00:00Z")
        // 2026-10-01 18:00:00Z is 01:00 VN on 2026-10-02 (Inside 2026-10-02 VN)
        val insideMatch = createSampleMatch(1000L, "2026-10-01T18:00:00Z")

        matchApi.responsesByDateAndPage["2026-10-01" to 1] = BaseResponse(
            statusCode = 200,
            message = "OK",
            data = MatchInfoDetailResponseBase(
                data = listOf(outsideMatch, insideMatch),
                meta = MetaResponse(currentPage = 1, lastPage = 1)
            )
        )

        val result = syncEngine.syncMatchesForLocalDate("2026-10-02", vietnamZone, forceRefresh = true)

        assertTrue(result is SyncResult.Success)
        assertNull(matchDao.matches[999L])
        assertNotNull(matchDao.matches[1000L])
    }
}
