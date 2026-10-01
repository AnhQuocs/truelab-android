package dev.anhquocs.truelab.core.data.crawler

import dev.anhquocs.truelab.core.data.crawler.model.SyncResult
import dev.anhquocs.truelab.core.data.league.local.dao.LeagueDao
import dev.anhquocs.truelab.core.data.league.local.dao.SeasonDao
import dev.anhquocs.truelab.core.data.league.local.entity.LeagueEntity
import dev.anhquocs.truelab.core.data.league.local.entity.SeasonEntity
import dev.anhquocs.truelab.core.data.league.remote.api.CompetitionApi
import dev.anhquocs.truelab.core.data.league.remote.dto.CompetitionItemDto
import dev.anhquocs.truelab.core.data.league.remote.dto.CompetitionListResponseBase
import dev.anhquocs.truelab.core.data.league.remote.dto.CompetitionMetaResponse
import dev.anhquocs.truelab.core.data.league.remote.dto.SeasonDto
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
import dev.anhquocs.truelab.core.domain.metadata.model.DatasetMetadata
import dev.anhquocs.truelab.core.domain.metadata.repository.DatasetMetadataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DataSyncEngineBootstrapTest {
    private val json = Json { ignoreUnknownKeys = true }

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

    private class FakeTeamDao : TeamDao {
        val teams = mutableMapOf<Int, TeamEntity>()
        override fun insertTeams(teams: List<TeamEntity>): LongArray {
            teams.forEach { if (!this.teams.containsKey(it.id)) this.teams[it.id] = it }
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

    private class FakeMatchApi : MatchApi {
        var responseToReturn: BaseResponse<MatchInfoDetailResponseBase>? = null
        override suspend fun getMatches(date: String, status: Int?, page: Int, pageSize: Int, sort: String): BaseResponse<MatchInfoDetailResponseBase> {
            return responseToReturn ?: BaseResponse(statusCode = 200, message = "OK", data = MatchInfoDetailResponseBase(data = emptyList(), meta = MetaResponse(1, 1)))
        }
        override suspend fun getSeasonMatches(
            seasonId: Long,
            status: Int,
            pageSize: Int,
            page: Int
        ): BaseResponse<MatchInfoDetailResponseBase> {
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

    private class FakeCompetitionApi : CompetitionApi {
        var competitionsPages = mutableMapOf<Int, BaseResponse<CompetitionListResponseBase>>()
        var seasonsMap = mutableMapOf<Long, BaseResponse<List<SeasonDto>>>()
        var seasonErrorForCompetition: Long? = null

        override suspend fun getCompetitionsList(page: Int, pageSize: Int): BaseResponse<CompetitionListResponseBase> {
            return competitionsPages[page] ?: BaseResponse(
                statusCode = 200,
                message = "OK",
                data = CompetitionListResponseBase(data = emptyList(), meta = CompetitionMetaResponse(currentPage = page, lastPage = 1))
            )
        }

        override suspend fun getCompetitionSeasons(competitionId: Long): BaseResponse<List<SeasonDto>> {
            if (seasonErrorForCompetition == competitionId) {
                throw RuntimeException("Seasons error for competition $competitionId")
            }
            return seasonsMap[competitionId] ?: BaseResponse(statusCode = 200, message = "OK", data = emptyList())
        }
    }

    private class FakeMetadataRepository : DatasetMetadataRepository {
        var totalLeaguesCount = 0
        var totalSeasonsCount = 0
        override fun getMetadata(key: String): Flow<DatasetMetadata?> = flowOf(null)
        override suspend fun refreshSnapshot(timestamp: Long): Result<DatasetMetadata> {
            return Result.success(DatasetMetadata(lastSyncTimestamp = timestamp, totalMatches = 0, totalTeams = 0, totalOddsRecords = 0, totalLeagues = totalLeaguesCount, totalSeasons = totalSeasonsCount))
        }
    }

    private class TestDb(
        val teamDaoImpl: FakeTeamDao,
        val matchDaoImpl: FakeMatchDao,
        val leagueDaoImpl: FakeLeagueDao,
        val seasonDaoImpl: FakeSeasonDao
    ) : TrueLabDatabase() {
        override fun teamDao(): TeamDao = teamDaoImpl
        override fun matchDao(): MatchDao = matchDaoImpl
        override fun leagueDao(): LeagueDao = leagueDaoImpl
        override fun seasonDao(): SeasonDao = seasonDaoImpl
        override fun oddsDao(): OddsDao = throw NotImplementedError()
        override fun rankingDao(): RankingDao = throw NotImplementedError()
        override fun predictionDao(): dev.anhquocs.truelab.core.data.prediction.local.dao.PredictionDao = throw NotImplementedError()
        override fun datasetMetadataDao(): dev.anhquocs.truelab.core.data.metadata.local.dao.DatasetMetadataDao = throw NotImplementedError()
        override fun clearAllTables() {}
        override fun createInvalidationTracker(): androidx.room.InvalidationTracker = androidx.room.InvalidationTracker(this, "leagues", "seasons")
        override fun createOpenHelper(config: androidx.room.DatabaseConfiguration): androidx.sqlite.db.SupportSQLiteOpenHelper = throw UnsupportedOperationException()
        override fun runInTransaction(body: Runnable) { body.run() }
        override fun <V : Any?> runInTransaction(body: java.util.concurrent.Callable<V>): V = body.call()
    }

    private lateinit var fakeLeagueDao: FakeLeagueDao
    private lateinit var fakeSeasonDao: FakeSeasonDao
    private lateinit var fakeTeamDao: FakeTeamDao
    private lateinit var fakeMatchDao: FakeMatchDao
    private lateinit var fakeMatchApi: FakeMatchApi
    private lateinit var fakeCompetitionApi: FakeCompetitionApi
    private lateinit var fakeMetadataRepo: FakeMetadataRepository
    private lateinit var testDb: TestDb
    private lateinit var syncEngine: DataSyncEngine

    @Before
    fun setup() {
        fakeLeagueDao = FakeLeagueDao()
        fakeSeasonDao = FakeSeasonDao()
        fakeTeamDao = FakeTeamDao()
        fakeMatchDao = FakeMatchDao()
        fakeMatchApi = FakeMatchApi()
        fakeCompetitionApi = FakeCompetitionApi()
        fakeMetadataRepo = FakeMetadataRepository()

        testDb = TestDb(fakeTeamDao, fakeMatchDao, fakeLeagueDao, fakeSeasonDao)

        syncEngine = DataSyncEngine(
            matchApi = fakeMatchApi,
            oddsApi = FakeOddsApi(),
            rankingApi = FakeRankingApi(),
            competitionApi = fakeCompetitionApi,
            database = testDb,
            json = json,
            metadataRepository = fakeMetadataRepo
        )
    }

    @Test
    fun syncLeaguesAndSeasonsFromRemote_success_insertsLeaguesAndSeasons() = runTest {
        val comp1 = CompetitionItemDto(id = 1515, name = "FIFA World Cup", shortName = "World Cup", logo = "wc.png", categoryId = 1, countryId = 0)
        val comp2 = CompetitionItemDto(id = 39, name = "Premier League", shortName = "EPL", logo = "epl.png", categoryId = 1, countryId = 1)

        fakeCompetitionApi.competitionsPages[1] = BaseResponse(
            statusCode = 200, message = "OK",
            data = CompetitionListResponseBase(data = listOf(comp1, comp2), meta = CompetitionMetaResponse(currentPage = 1, lastPage = 1, perPage = 50, total = 2))
        )
        fakeCompetitionApi.seasonsMap[1515L] = BaseResponse(
            statusCode = 200, message = "OK",
            data = listOf(SeasonDto(id = 28277L, competitionId = 1515L, year = "2026", isCurrent = 1))
        )
        fakeCompetitionApi.seasonsMap[39L] = BaseResponse(
            statusCode = 200, message = "OK",
            data = listOf(SeasonDto(id = 1001L, competitionId = 39L, year = "2024", isCurrent = 1))
        )

        val result = syncEngine.syncLeaguesAndSeasonsFromRemote(maxPages = 1, syncSeasons = true)

        assertTrue(result is SyncResult.Success)
        assertEquals(4, (result as SyncResult.Success).data) // 2 leagues + 2 seasons
        assertEquals(2, fakeLeagueDao.leagues.size)
        assertEquals(2, fakeSeasonDao.seasons.size)
        assertEquals("FIFA World Cup", fakeLeagueDao.leagues[1515]?.name)
        assertEquals("2026", fakeSeasonDao.seasons["28277"]?.name)
    }

    @Test
    fun syncLeaguesAndSeasonsFromRemote_pagination_fetchesUpToMaxPages() = runTest {
        val page1Comp = CompetitionItemDto(id = 1, name = "League 1")
        val page2Comp = CompetitionItemDto(id = 2, name = "League 2")

        fakeCompetitionApi.competitionsPages[1] = BaseResponse(
            statusCode = 200, message = "OK",
            data = CompetitionListResponseBase(data = listOf(page1Comp), meta = CompetitionMetaResponse(currentPage = 1, lastPage = 5, perPage = 1, total = 5))
        )
        fakeCompetitionApi.competitionsPages[2] = BaseResponse(
            statusCode = 200, message = "OK",
            data = CompetitionListResponseBase(data = listOf(page2Comp), meta = CompetitionMetaResponse(currentPage = 2, lastPage = 5, perPage = 1, total = 5))
        )

        val result = syncEngine.syncLeaguesAndSeasonsFromRemote(maxPages = 2, syncSeasons = false)

        assertTrue(result is SyncResult.Success)
        assertEquals(2, (result as SyncResult.Success).data)
        assertEquals(2, fakeLeagueDao.leagues.size)
    }

    @Test
    fun syncLeaguesAndSeasonsFromRemote_individualSeasonFailure_continuesOtherCompetitions() = runTest {
        val comp1 = CompetitionItemDto(id = 101, name = "Broken Season League")
        val comp2 = CompetitionItemDto(id = 102, name = "Healthy League")

        fakeCompetitionApi.competitionsPages[1] = BaseResponse(
            statusCode = 200, message = "OK",
            data = CompetitionListResponseBase(data = listOf(comp1, comp2), meta = CompetitionMetaResponse(currentPage = 1, lastPage = 1))
        )
        fakeCompetitionApi.seasonErrorForCompetition = 101L
        fakeCompetitionApi.seasonsMap[102L] = BaseResponse(
            statusCode = 200, message = "OK",
            data = listOf(SeasonDto(id = 202L, competitionId = 102L, year = "2024", isCurrent = 1))
        )

        val result = syncEngine.syncLeaguesAndSeasonsFromRemote(maxPages = 1, syncSeasons = true)

        assertTrue(result is SyncResult.Success)
        // 2 leagues + 1 healthy season = 3 synced
        assertEquals(3, (result as SyncResult.Success).data)
        assertEquals(2, fakeLeagueDao.leagues.size)
        assertEquals(1, fakeSeasonDao.seasons.size)
    }

    @Test
    fun syncFullPipelineForDate_withCompetitionIdInMatch_linksMatchToLeague() = runTest {
        val match = MatchRecord(
            id = 5001L,
            competitionId = 1515,
            competition = CompetitionSummaryInfo(id = 1515, name = "FIFA World Cup", shortName = "World Cup"),
            homeTeam = TeamInfo(1, "Team A"),
            awayTeam = TeamInfo(2, "Team B"),
            homeScore = 1,
            awayScore = 0,
            startTimeDate = "2026-09-27T10:00:00Z",
            status = "ended"
        )

        fakeMatchApi.responseToReturn = BaseResponse(
            statusCode = 200, message = "OK",
            data = MatchInfoDetailResponseBase(data = listOf(match), meta = MetaResponse(currentPage = 1, totalPage = 1))
        )

        val result = syncEngine.syncFullPipelineForDate("2026-09-27", syncLeaguesAndSeasons = false)

        assertTrue(result is SyncResult.Success)
        // Verify league was inserted from match record
        assertNotNull(fakeLeagueDao.leagues[1515])
        assertEquals("FIFA World Cup", fakeLeagueDao.leagues[1515]?.name)
        // Verify match entity has leagueId = 1515
        assertEquals(1515, fakeMatchDao.matches[5001L]?.leagueId)
    }
}
