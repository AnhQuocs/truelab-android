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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DataSyncEngineTest {
    private val json = Json { ignoreUnknownKeys = true }

    private class FakeTeamDao : TeamDao {
        val teams = mutableMapOf<Int, TeamEntity>()
        override fun insertTeams(teams: List<TeamEntity>): LongArray {
            teams.forEach { team -> if (!this.teams.containsKey(team.id)) this.teams[team.id] = team }
            return LongArray(teams.size) { (it + 1).toLong() }
        }
        override fun getTeamById(teamId: Int): Flow<TeamEntity?> = flowOf(teams[teamId])
        override fun searchTeams(query: String): Flow<List<TeamEntity>> =
            flowOf(teams.values.filter { it.name.contains(query, ignoreCase = true) })
        override fun searchTeams(query: String, limit: Int): Flow<List<TeamEntity>> =
            flowOf(teams.values.filter { it.name.contains(query, ignoreCase = true) }.take(limit))
        override fun getTeams(limit: Int): Flow<List<TeamEntity>> =
            flowOf(teams.values.take(limit))
    }

    private class FakeMatchDao : MatchDao {
        val matches = mutableMapOf<Long, MatchEntity>()
        var errorToThrow: Throwable? = null
        override fun insertMatches(matches: List<MatchEntity>): LongArray {
            errorToThrow?.let { throw it }
            matches.forEach { this.matches[it.id] = it }
            return LongArray(matches.size) { (it + 1).toLong() }
        }
        override fun upsertMatches(matches: List<MatchEntity>): LongArray {
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

    private class FakeMatchApi : MatchApi {
        var responseToReturn: BaseResponse<MatchInfoDetailResponseBase>? = null
        var errorToThrow: Throwable? = null
        override suspend fun getMatches(date: String, status: Int?, page: Int, pageSize: Int, sort: String): BaseResponse<MatchInfoDetailResponseBase> {
            errorToThrow?.let { throw it }
            return responseToReturn ?: BaseResponse(statusCode = 200, message = "OK", data = MatchInfoDetailResponseBase(data = emptyList(), meta = MetaResponse(1, 1)))
        }
        override suspend fun getSeasonMatches(seasonId: Long, status: Int, pageSize: Int, page: Int): BaseResponse<MatchInfoDetailResponseBase> {
            errorToThrow?.let { throw it }
            return responseToReturn ?: BaseResponse(statusCode = 200, message = "OK", data = MatchInfoDetailResponseBase(data = emptyList(), meta = MetaResponse(1, 1)))
        }
    }

    private class FakeOddsApi : OddsApi {
        override suspend fun getOdds(matchId: Long): BaseResponse<List<OddsRecord>> = BaseResponse(200, "OK", emptyList())
        override suspend fun getOddsHistory(matchId: Long, companyId: Int?, oddsType: String?): BaseResponse<OddsHistoryResponse> =
            BaseResponse(200, "OK", OddsHistoryResponse(emptyList()))
    }

    private class FakeRankingApi : RankingApi {
        override suspend fun getSeasonRanking(matchId: Long): BaseResponse<List<SeasonRankResponse>> = BaseResponse(200, "OK", emptyList())
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
            MatchRecord(id = 1001L, competitionId = 927, homeTeam = TeamInfo(1, "Arsenal", "arsenal.png"), awayTeam = TeamInfo(2, "Chelsea", "chelsea.png"), homeScore = 2, awayScore = 1, startTimeDate = "2024-05-10 15:00:00", status = "8"),
            MatchRecord(id = 1002L, competitionId = 927, homeTeam = TeamInfo(1, "Arsenal", "arsenal.png"), awayTeam = TeamInfo(3, "Liverpool", "liverpool.png"), homeScore = 0, awayScore = 0, startTimeDate = "2024-05-10 18:00:00", status = "8")
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
            competitionId = 927,
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
            competitionId = 927,
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

    @Test
    fun syncFullPipelineForDate_qualityPolicy_acceptsWhitelistedCompetition() = runTest {
        val premierLeagueMatch = MatchRecord(
            id = 5001L,
            competitionId = 927, // English Premier League (Tier 1 Whitelist)
            competition = CompetitionSummaryInfo(927, "English Premier League"),
            homeTeam = TeamInfo(10, "Man City", "mci.png"),
            awayTeam = TeamInfo(11, "Liverpool", "liv.png"),
            homeScore = 3,
            awayScore = 1,
            startTimeDate = "2026-10-01 15:00:00",
            status = "8"
        )
        fakeMatchApi.responseToReturn = BaseResponse(
            statusCode = 200,
            message = "OK",
            data = MatchInfoDetailResponseBase(
                data = listOf(premierLeagueMatch),
                meta = MetaResponse(currentPage = 1, totalPage = 1)
            )
        )

        val result = syncEngine.syncFullPipelineForDate("2026-10-01")

        assertTrue(result is SyncResult.Success)
        assertEquals(1, (result as SyncResult.Success).data.matchesSynced)
        assertEquals(2, result.data.teamsSynced)
        assertEquals(1, fakeMatchDao.matches.size)
        assertEquals(2, fakeTeamDao.teams.size)
        assertEquals(1, fakeLeagueDao.leagues.size)
        assertTrue(fakeMatchDao.matches.containsKey(5001L))
    }

    @Test
    fun syncFullPipelineForDate_qualityPolicy_rejectsFriendlyMatch() = runTest {
        val friendlyMatch = MatchRecord(
            id = 5002L,
            competitionId = 820, // International Friendly (Blacklisted)
            competition = CompetitionSummaryInfo(820, "International Friendly"),
            homeTeam = TeamInfo(20, "France", "fr.png"),
            awayTeam = TeamInfo(21, "Germany", "de.png"),
            homeScore = 2,
            awayScore = 2,
            startTimeDate = "2026-10-01 19:45:00",
            status = "8"
        )
        fakeMatchApi.responseToReturn = BaseResponse(
            statusCode = 200,
            message = "OK",
            data = MatchInfoDetailResponseBase(
                data = listOf(friendlyMatch),
                meta = MetaResponse(currentPage = 1, totalPage = 1)
            )
        )

        val result = syncEngine.syncFullPipelineForDate("2026-10-01")

        assertTrue(result is SyncResult.Success)
        assertEquals(0, (result as SyncResult.Success).data.matchesSynced)
        assertEquals(0, result.data.teamsSynced)
        assertEquals(0, fakeMatchDao.matches.size)
        assertEquals(0, fakeTeamDao.teams.size)
        assertEquals(0, fakeLeagueDao.leagues.size)
    }

    @Test
    fun syncFullPipelineForDate_qualityPolicy_rejectsYouthMatch() = runTest {
        val youthMatch = MatchRecord(
            id = 5003L,
            competition = CompetitionSummaryInfo(10999, "UEFA European U17 Championship"),
            homeTeam = TeamInfo(30, "Venezuela U17", "ven.png"),
            awayTeam = TeamInfo(31, "England U17", "eng.png"),
            homeScore = 1,
            awayScore = 0,
            startTimeDate = "2026-10-01 16:30:00",
            status = "8"
        )
        fakeMatchApi.responseToReturn = BaseResponse(
            statusCode = 200,
            message = "OK",
            data = MatchInfoDetailResponseBase(
                data = listOf(youthMatch),
                meta = MetaResponse(currentPage = 1, totalPage = 1)
            )
        )

        val result = syncEngine.syncFullPipelineForDate("2026-10-01")

        assertTrue(result is SyncResult.Success)
        assertEquals(0, (result as SyncResult.Success).data.matchesSynced)
        assertEquals(0, fakeMatchDao.matches.size)
        assertEquals(0, fakeTeamDao.teams.size)
        assertEquals(0, fakeLeagueDao.leagues.size)
    }

    @Test
    fun syncFullPipelineForDate_qualityPolicy_rejectsAmbiguousQuarantineMatch() = runTest {
        val ambiguousMatch = MatchRecord(
            id = 5004L,
            competitionId = 999999, // Unknown competition
            competition = CompetitionSummaryInfo(999999, "Random Local Tournament 2026"),
            homeTeam = TeamInfo(40, "Local Team A", "a.png"),
            awayTeam = TeamInfo(41, "Local Team B", "b.png"),
            homeScore = 0,
            awayScore = 0,
            startTimeDate = "2026-10-01 14:00:00",
            status = "8"
        )
        fakeMatchApi.responseToReturn = BaseResponse(
            statusCode = 200,
            message = "OK",
            data = MatchInfoDetailResponseBase(
                data = listOf(ambiguousMatch),
                meta = MetaResponse(currentPage = 1, totalPage = 1)
            )
        )

        val result = syncEngine.syncFullPipelineForDate("2026-10-01")

        assertTrue(result is SyncResult.Success)
        assertEquals(0, (result as SyncResult.Success).data.matchesSynced)
        assertEquals(0, fakeMatchDao.matches.size)
    }

    @Test
    fun syncFullPipelineForDate_qualityPolicy_acceptsOfficialInternational() = runTest {
        val internationalMatch = MatchRecord(
            id = 5005L,
            competitionId = 2484, // CONCACAF Nations League (Tier 3 Whitelist)
            competition = CompetitionSummaryInfo(2484, "CONCACAF Nations League"),
            homeTeam = TeamInfo(50, "USA", "usa.png"),
            awayTeam = TeamInfo(51, "Mexico", "mex.png"),
            homeScore = 1,
            awayScore = 1,
            startTimeDate = "2026-10-01 20:00:00",
            status = "8"
        )
        fakeMatchApi.responseToReturn = BaseResponse(
            statusCode = 200,
            message = "OK",
            data = MatchInfoDetailResponseBase(
                data = listOf(internationalMatch),
                meta = MetaResponse(currentPage = 1, totalPage = 1)
            )
        )

        val result = syncEngine.syncFullPipelineForDate("2026-10-01")

        assertTrue(result is SyncResult.Success)
        assertEquals(1, (result as SyncResult.Success).data.matchesSynced)
        assertEquals(1, fakeMatchDao.matches.size)
        assertTrue(fakeMatchDao.matches.containsKey(5005L))
    }

    @Test
    fun syncFullPipelineForDate_qualityPolicy_acceptsAseanCupVietnamVsPakistan() = runTest {
        val aseanCupMatch = MatchRecord(
            id = 908567L,
            competitionId = 2239057, // FIFA ASEAN Cup (Tier 3 Whitelist)
            competition = CompetitionSummaryInfo(2239057, "FIFA ASEAN Cup", "FIFA ASEAN Cup"),
            homeTeam = TeamInfo(127052, "Vietnam", "vietnam.png"),
            awayTeam = TeamInfo(137359, "Pakistan", "pakistan.png"),
            homeScore = 0,
            awayScore = 0,
            startTimeDate = "2026-10-02 16:00:00",
            status = "0" // Scheduled / Pending
        )
        fakeMatchApi.responseToReturn = BaseResponse(
            statusCode = 200,
            message = "OK",
            data = MatchInfoDetailResponseBase(
                data = listOf(aseanCupMatch),
                meta = MetaResponse(currentPage = 1, totalPage = 1)
            )
        )

        val result = syncEngine.syncFullPipelineForDate("2026-10-02")

        assertTrue(result is SyncResult.Success)
        assertEquals(1, (result as SyncResult.Success).data.matchesSynced)
        assertEquals(1, fakeMatchDao.matches.size)
        assertTrue(fakeMatchDao.matches.containsKey(908567L))
        assertEquals(127052, fakeMatchDao.matches[908567L]?.homeTeamId)
        assertEquals(137359, fakeMatchDao.matches[908567L]?.awayTeamId)
        assertEquals(1, fakeLeagueDao.leagues.size)
        assertTrue(fakeLeagueDao.leagues.containsKey(2239057))
        assertEquals(2239057, fakeLeagueDao.leagues[2239057]?.id)
    }

    @Test
    fun syncFullPipelineForDate_qualityPolicy_preservesLiveAndUpcomingMatchesForAcceptedCompetitions() = runTest {
        val liveMatch = MatchRecord(
            id = 5006L,
            competitionId = 1398, // UEFA Champions League (Tier 1)
            competition = CompetitionSummaryInfo(1398, "UEFA Champions League"),
            homeTeam = TeamInfo(60, "Real Madrid", "rma.png"),
            awayTeam = TeamInfo(61, "Bayern Munich", "bay.png"),
            homeScore = 1,
            awayScore = 0,
            startTimeDate = "2026-10-01 21:00:00",
            status = "1" // Live (In-Play)
        )
        val upcomingMatch = MatchRecord(
            id = 5007L,
            competitionId = 1398, // UEFA Champions League (Tier 1)
            competition = CompetitionSummaryInfo(1398, "UEFA Champions League"),
            homeTeam = TeamInfo(62, "PSG", "psg.png"),
            awayTeam = TeamInfo(63, "Inter Milan", "int.png"),
            homeScore = 0,
            awayScore = 0,
            startTimeDate = "2026-10-01 23:00:00",
            status = "0" // Pending (Upcoming)
        )
        fakeMatchApi.responseToReturn = BaseResponse(
            statusCode = 200,
            message = "OK",
            data = MatchInfoDetailResponseBase(
                data = listOf(liveMatch, upcomingMatch),
                meta = MetaResponse(currentPage = 1, totalPage = 1)
            )
        )

        val result = syncEngine.syncFullPipelineForDate("2026-10-01")

        assertTrue(result is SyncResult.Success)
        assertEquals(2, (result as SyncResult.Success).data.matchesSynced)
        assertEquals(2, fakeMatchDao.matches.size)
        assertEquals("1", fakeMatchDao.matches[5006L]?.status)
        assertEquals("0", fakeMatchDao.matches[5007L]?.status)
    }

    @Test
    fun syncFullPipelineForDate_metadataIsolation_rejectedMatchDoesNotCreateOrphanLeaguesOrTeams() = runTest {
        val validMatch = MatchRecord(
            id = 5008L,
            competitionId = 954, // La Liga (Tier 1)
            competition = CompetitionSummaryInfo(954, "Spanish La Liga"),
            homeTeam = TeamInfo(70, "Barcelona", "fcb.png"),
            awayTeam = TeamInfo(71, "Atletico Madrid", "atm.png"),
            homeScore = 2,
            awayScore = 0,
            startTimeDate = "2026-10-01 16:00:00",
            status = "8"
        )
        val rejectedYouthMatch = MatchRecord(
            id = 5009L,
            competitionId = 1379, // CFA U20 League (Blacklisted)
            competition = CompetitionSummaryInfo(1379, "Chinese Football Association U-20 League"),
            homeTeam = TeamInfo(80, "Beijing U20", "bj.png"),
            awayTeam = TeamInfo(81, "Shanghai U20", "sh.png"),
            homeScore = 0,
            awayScore = 0,
            startTimeDate = "2026-10-01 14:00:00",
            status = "8"
        )
        fakeMatchApi.responseToReturn = BaseResponse(
            statusCode = 200,
            message = "OK",
            data = MatchInfoDetailResponseBase(
                data = listOf(validMatch, rejectedYouthMatch),
                meta = MetaResponse(currentPage = 1, totalPage = 1)
            )
        )

        val result = syncEngine.syncFullPipelineForDate("2026-10-01")

        assertTrue(result is SyncResult.Success)
        assertEquals(1, (result as SyncResult.Success).data.matchesSynced)
        assertEquals(2, result.data.teamsSynced)
        assertEquals(1, fakeMatchDao.matches.size)
        assertEquals(2, fakeTeamDao.teams.size)
        assertEquals(1, fakeLeagueDao.leagues.size)

        // Verify ONLY valid match, league and teams were saved
        assertTrue(fakeMatchDao.matches.containsKey(5008L))
        assertTrue(!fakeMatchDao.matches.containsKey(5009L))
        assertTrue(fakeTeamDao.teams.containsKey(70))
        assertTrue(fakeTeamDao.teams.containsKey(71))
        assertTrue(!fakeTeamDao.teams.containsKey(80))
        assertTrue(!fakeTeamDao.teams.containsKey(81))
        assertTrue(fakeLeagueDao.leagues.containsKey(954))
        assertTrue(!fakeLeagueDao.leagues.containsKey(1379))
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

    @Test
    fun syncFullPipelineForDate_batchFailure_doesNotTriggerMetadataRefresh() = runTest {
        val fakeRepo = FakeDatasetMetadataRepository()
        fakeMatchApi.errorToThrow = RuntimeException("API error 500")

        val engineWithRepo = DataSyncEngine(
            matchApi = fakeMatchApi,
            oddsApi = fakeOddsApi,
            rankingApi = fakeRankingApi,
            database = testDatabase,
            json = json,
            metadataRepository = fakeRepo
        )

        val result = engineWithRepo.syncFullPipelineForDate("2024-05-10")

        assertTrue(result is SyncResult.Failure)
        org.junit.Assert.assertNull(fakeRepo.refreshedTimestamp)
    }
}

