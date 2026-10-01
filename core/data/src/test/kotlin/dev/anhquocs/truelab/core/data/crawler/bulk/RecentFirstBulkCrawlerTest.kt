package dev.anhquocs.truelab.core.data.crawler.bulk

import androidx.room.DatabaseConfiguration
import androidx.room.InvalidationTracker
import androidx.sqlite.db.SupportSQLiteOpenHelper
import dev.anhquocs.truelab.core.data.crawler.checkpoint.CheckpointManager
import dev.anhquocs.truelab.core.data.crawler.checkpoint.CrawlerCheckpoint
import dev.anhquocs.truelab.core.data.crawler.policy.DefaultCompetitionQualityPolicy
import dev.anhquocs.truelab.core.data.crawler.policy.QuarantineManager
import dev.anhquocs.truelab.core.data.league.local.dao.LeagueDao
import dev.anhquocs.truelab.core.data.league.local.dao.SeasonDao
import dev.anhquocs.truelab.core.data.league.local.entity.LeagueEntity
import dev.anhquocs.truelab.core.data.local.database.TrueLabDatabase
import dev.anhquocs.truelab.core.data.match.local.dao.MatchDao
import dev.anhquocs.truelab.core.data.match.local.entity.MatchEntity
import dev.anhquocs.truelab.core.data.match.local.entity.MatchWithTeams
import dev.anhquocs.truelab.core.data.match.remote.api.MatchApi
import dev.anhquocs.truelab.core.data.match.remote.dto.CompetitionSummaryInfo
import dev.anhquocs.truelab.core.data.match.remote.dto.MatchInfoDetailResponseBase
import dev.anhquocs.truelab.core.data.match.remote.dto.MatchRecord
import dev.anhquocs.truelab.core.data.match.remote.dto.TeamInfo
import dev.anhquocs.truelab.core.data.metadata.local.dao.DatasetMetadataDao
import dev.anhquocs.truelab.core.data.odds.local.dao.OddsDao
import dev.anhquocs.truelab.core.data.prediction.local.dao.PredictionDao
import dev.anhquocs.truelab.core.data.ranking.local.dao.RankingDao
import dev.anhquocs.truelab.core.data.remote.dto.BaseResponse
import dev.anhquocs.truelab.core.data.remote.dto.MetaResponse
import dev.anhquocs.truelab.core.data.team.local.dao.TeamDao
import dev.anhquocs.truelab.core.data.team.local.entity.TeamEntity
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RecentFirstBulkCrawlerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var fakeMatchApi: FakeMatchApi
    private lateinit var fakeDatabase: FakeTrueLabDatabase
    private lateinit var quarantineManager: QuarantineManager
    private lateinit var checkpointManager: CheckpointManager

    @Before
    fun setup() {
        fakeMatchApi = FakeMatchApi()
        fakeDatabase = FakeTrueLabDatabase()
        quarantineManager = QuarantineManager()
        checkpointManager = CheckpointManager()
    }

    @Test
    fun `test crawler paginates dynamically and stops at target matches`() = runTest {
        val checkpointFile = tempFolder.newFile("checkpoint.json")
        val quarantineFile = tempFolder.newFile("quarantine.json")

        // Setup 2 days of matches
        // Day 1 (2026-10-01): 2 pages (3 matches page 1, 2 matches page 2)
        fakeMatchApi.responses["2026-10-01" to 1] = createResponse(
            listOf(
                createMatch(1L, 927, "Premier League", "Arsenal", "Chelsea"),
                createMatch(2L, 954, "La Liga", "Real Madrid", "Barcelona"),
                createMatch(3L, 820, "International Friendly", "Team A", "Team B") // Rejected
            ),
            lastPage = 2
        )
        fakeMatchApi.responses["2026-10-01" to 2] = createResponse(
            listOf(
                createMatch(4L, 999, "Serie A", "Juventus", "Milan"),
                createMatch(5L, 1017, "Bundesliga", "Bayern", "Dortmund")
            ),
            lastPage = 2
        )

        // Day 2 (2026-09-30): 1 page (2 matches)
        fakeMatchApi.responses["2026-09-30" to 1] = createResponse(
            listOf(
                createMatch(6L, 927, "Premier League", "Liverpool", "City"),
                createMatch(7L, 7777, "Unknown League", "X", "Y") // Quarantined
            ),
            lastPage = 1
        )

        val crawler = RecentFirstBulkCrawler(
            matchApi = fakeMatchApi,
            database = fakeDatabase,
            qualityPolicy = DefaultCompetitionQualityPolicy(quarantineManager),
            quarantineManager = quarantineManager,
            checkpointManager = checkpointManager,
            checkpointFile = checkpointFile,
            quarantineFile = quarantineFile,
            interPageDelayMs = 0L,
            interDayDelayMs = 0L
        )

        val result = crawler.crawlUntilTarget(
            targetUniqueMatches = 5,
            initialStartDate = "2026-10-01"
        )

        assertEquals(5, result.acceptedUniqueMatches)
        assertEquals(2, result.totalProcessedDays)
        assertTrue(result.isTargetReached)
        assertEquals(5, fakeDatabase.fakeMatchDao.matches.size)
        assertEquals(1, quarantineManager.count())

        val savedCheckpoint = checkpointManager.loadCheckpoint(checkpointFile)
        assertNotNull(savedCheckpoint)
        assertEquals(5, savedCheckpoint?.currentAcceptedUniqueMatches)
    }

    @Test
    fun `test resume from saved checkpoint continues from next day`() = runTest {
        val checkpointFile = tempFolder.newFile("checkpoint_resume.json")
        checkpointManager.saveCheckpoint(
            checkpointFile,
            CrawlerCheckpoint(
                targetUniqueMatches = 10,
                currentAcceptedUniqueMatches = 4,
                lastProcessedDate = "2026-10-01",
                totalProcessedDays = 1
            )
        )

        // Day 2026-09-30 (since last processed was 2026-10-01)
        fakeMatchApi.responses["2026-09-30" to 1] = createResponse(
            listOf(
                createMatch(10L, 927, "Premier League", "Spurs", "Villa"),
                createMatch(11L, 927, "Premier League", "Newcastle", "Wolves")
            ),
            lastPage = 1
        )

        val crawler = RecentFirstBulkCrawler(
            matchApi = fakeMatchApi,
            database = fakeDatabase,
            qualityPolicy = DefaultCompetitionQualityPolicy(quarantineManager),
            quarantineManager = quarantineManager,
            checkpointManager = checkpointManager,
            checkpointFile = checkpointFile,
            interPageDelayMs = 0L,
            interDayDelayMs = 0L
        )

        val result = crawler.crawlUntilTarget(
            targetUniqueMatches = 6,
            initialStartDate = "2026-10-01"
        )

        assertEquals(6, result.acceptedUniqueMatches)
        assertEquals(2, result.totalProcessedDays)
    }

    private fun createMatch(
        id: Long,
        compId: Int,
        compName: String,
        homeTeam: String,
        awayTeam: String
    ): MatchRecord {
        return MatchRecord(
            id = id,
            homeTeam = TeamInfo(id = (id * 10).toInt(), name = homeTeam, logo = null),
            awayTeam = TeamInfo(id = (id * 10 + 1).toInt(), name = awayTeam, logo = null),
            homeScore = 1,
            awayScore = 0,
            startTimeDate = "2026-10-01 10:00:00",
            status = "8",
            competitionId = compId,
            competition = CompetitionSummaryInfo(id = compId, name = compName, shortName = null, logo = null)
        )
    }

    private fun createResponse(matches: List<MatchRecord>, lastPage: Int): BaseResponse<MatchInfoDetailResponseBase> {
        return BaseResponse(
            statusCode = 200,
            message = "success",
            data = MatchInfoDetailResponseBase(
                data = matches,
                meta = MetaResponse(
                    totalCount = matches.size * lastPage,
                    currentPage = 1,
                    perPage = 50,
                    totalPage = lastPage,
                    lastPage = lastPage
                )
            )
        )
    }

    private class FakeMatchApi : MatchApi {
        val responses = mutableMapOf<Pair<String, Int>, BaseResponse<MatchInfoDetailResponseBase>>()

        override suspend fun getMatches(
            date: String,
            status: Int?,
            page: Int,
            pageSize: Int,
            sort: String
        ): BaseResponse<MatchInfoDetailResponseBase> {
            val key = date to page
            return responses[key] ?: BaseResponse(
                statusCode = 200,
                message = "success",
                data = MatchInfoDetailResponseBase(
                    data = emptyList(),
                    meta = MetaResponse(totalCount = 0, currentPage = page, perPage = pageSize, totalPage = 1, lastPage = 1)
                )
            )
        }

        override suspend fun getSeasonMatches(seasonId: Long, status: Int, pageSize: Int, page: Int): BaseResponse<MatchInfoDetailResponseBase> = TODO()
    }

    private class FakeTrueLabDatabase : TrueLabDatabase() {
        val fakeMatchDao = FakeMatchDao()
        val fakeTeamDao = FakeTeamDao()
        val fakeLeagueDao = FakeLeagueDao()

        override fun matchDao(): MatchDao = fakeMatchDao
        override fun teamDao(): TeamDao = fakeTeamDao
        override fun leagueDao(): LeagueDao = fakeLeagueDao
        override fun oddsDao(): OddsDao = TODO()
        override fun rankingDao(): RankingDao = TODO()
        override fun predictionDao(): PredictionDao = TODO()
        override fun seasonDao(): SeasonDao = TODO()
        override fun datasetMetadataDao(): DatasetMetadataDao = TODO()
        override fun clearAllTables() {}
        override fun createInvalidationTracker(): InvalidationTracker = InvalidationTracker(this, "teams", "matches")
        override fun createOpenHelper(config: DatabaseConfiguration): SupportSQLiteOpenHelper = throw UnsupportedOperationException()
        override fun runInTransaction(body: Runnable) { body.run() }
        override fun <T> runInTransaction(body: java.util.concurrent.Callable<T>): T = body.call()
    }

    private class FakeMatchDao : MatchDao {
        val matches = mutableMapOf<Long, MatchEntity>()
        override fun upsertMatches(matches: List<MatchEntity>): LongArray {
            matches.forEach { this.matches[it.id] = it }
            return LongArray(matches.size) { (it + 1).toLong() }
        }
        override fun insertMatches(matches: List<MatchEntity>): LongArray = upsertMatches(matches)
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

    private class FakeLeagueDao : LeagueDao {
        val leagues = mutableMapOf<Int, LeagueEntity>()
        override fun insertLeagues(leagues: List<LeagueEntity>): LongArray {
            leagues.forEach { if (!this.leagues.containsKey(it.id)) this.leagues[it.id] = it }
            return LongArray(leagues.size) { (it + 1).toLong() }
        }
        override fun getLeagueById(leagueId: Int): Flow<LeagueEntity?> = flowOf(leagues[leagueId])
        override fun getLeagues(): Flow<List<LeagueEntity>> = flowOf(leagues.values.toList())
    }
}
