package dev.anhquocs.truelab.core.data.crawler.util

import androidx.room.DatabaseConfiguration
import androidx.room.InvalidationTracker
import androidx.sqlite.db.SupportSQLiteOpenHelper
import dev.anhquocs.truelab.core.data.league.local.dao.LeagueDao
import dev.anhquocs.truelab.core.data.league.local.dao.SeasonDao
import dev.anhquocs.truelab.core.data.local.database.TrueLabDatabase
import dev.anhquocs.truelab.core.data.match.local.dao.MatchDao
import dev.anhquocs.truelab.core.data.match.local.entity.MatchEntity
import dev.anhquocs.truelab.core.data.match.local.entity.MatchWithTeams
import dev.anhquocs.truelab.core.data.match.remote.dto.CompetitionSummaryInfo
import dev.anhquocs.truelab.core.data.match.remote.dto.MatchRecord
import dev.anhquocs.truelab.core.data.match.remote.dto.TeamInfo
import dev.anhquocs.truelab.core.data.metadata.local.dao.DatasetMetadataDao
import dev.anhquocs.truelab.core.data.odds.local.dao.OddsDao
import dev.anhquocs.truelab.core.data.prediction.local.dao.PredictionDao
import dev.anhquocs.truelab.core.data.ranking.local.dao.RankingDao
import dev.anhquocs.truelab.core.data.team.local.dao.TeamDao
import dev.anhquocs.truelab.core.data.team.local.entity.TeamEntity
import dev.anhquocs.truelab.core.data.team.local.entity.TeamWithLeagueItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IncrementalEloUpdaterTest {

    private lateinit var database: TestTrueLabDatabase
    private lateinit var fakeTeamDao: TestTeamDao
    private lateinit var fakeMatchDao: TestMatchDao

    private val teamHome = TeamEntity(id = 1, name = "Arsenal", logo = null, leagueName = "Premier League", eloRating = 1800.0)
    private val teamAway = TeamEntity(id = 2, name = "Chelsea", logo = null, leagueName = "Premier League", eloRating = 1600.0)

    @Before
    fun setUp() {
        fakeTeamDao = TestTeamDao()
        fakeMatchDao = TestMatchDao()
        database = TestTrueLabDatabase(fakeTeamDao, fakeMatchDao)

        fakeTeamDao.teams[1] = teamHome
        fakeTeamDao.teams[2] = teamAway
    }

    @Test
    fun `updateEloForMatches with new ended match updates both home and away Elo`() {
        // Arsenal (1800) vs Chelsea (1600), Arsenal wins 2 - 1
        val match = createMatchRecord(
            id = 101L,
            homeId = 1,
            awayId = 2,
            homeScore = 2,
            awayScore = 1,
            status = "ended"
        )

        IncrementalEloUpdater.updateEloForMatches(database, listOf(match))

        val updatedHome = fakeTeamDao.teams[1]?.eloRating ?: 0.0
        val updatedAway = fakeTeamDao.teams[2]?.eloRating ?: 0.0

        // Expected score for Arsenal (1800) vs Chelsea (1600): ~0.76
        // Actual score = 1.0 -> Delta = 32 * (1.0 - 0.76) = ~+7.7
        assertTrue(updatedHome > 1800.0)
        assertTrue(updatedAway < 1600.0)
        assertEquals(3400.0, updatedHome + updatedAway, 0.001) // Zero-sum conservation
    }

    @Test
    fun `updateEloForMatches skips already ended match for idempotency`() {
        // Match 102 was already recorded as ended in DB
        fakeMatchDao.matches[102L] = MatchEntity(
            id = 102L,
            homeTeamId = 1,
            awayTeamId = 2,
            homeScore = 2,
            awayScore = 1,
            startTimeDate = "2026-03-01T15:00:00",
            status = "ended"
        )

        val match = createMatchRecord(
            id = 102L,
            homeId = 1,
            awayId = 2,
            homeScore = 2,
            awayScore = 1,
            status = "ended"
        )

        IncrementalEloUpdater.updateEloForMatches(database, listOf(match))

        // Ratings should remain unchanged
        assertEquals(1800.0, fakeTeamDao.teams[1]?.eloRating ?: 0.0, 0.001)
        assertEquals(1600.0, fakeTeamDao.teams[2]?.eloRating ?: 0.0, 0.001)
    }

    @Test
    fun `updateEloForMatches with pending or live match does not update Elo`() {
        val liveMatch = createMatchRecord(
            id = 103L,
            homeId = 1,
            awayId = 2,
            homeScore = 1,
            awayScore = 0,
            status = "live"
        )

        IncrementalEloUpdater.updateEloForMatches(database, listOf(liveMatch))

        assertEquals(1800.0, fakeTeamDao.teams[1]?.eloRating ?: 0.0, 0.001)
        assertEquals(1600.0, fakeTeamDao.teams[2]?.eloRating ?: 0.0, 0.001)
    }

    private fun createMatchRecord(
        id: Long,
        homeId: Int,
        awayId: Int,
        homeScore: Int,
        awayScore: Int,
        status: String
    ): MatchRecord = MatchRecord(
        id = id,
        homeTeam = TeamInfo(id = homeId, name = "Team $homeId"),
        awayTeam = TeamInfo(id = awayId, name = "Team $awayId"),
        homeScore = homeScore,
        awayScore = awayScore,
        status = status,
        startTimeDate = "2026-03-01T15:00:00Z",
        competition = CompetitionSummaryInfo(id = 10, name = "Premier League")
    )

    private class TestTrueLabDatabase(
        val teamDaoImpl: TestTeamDao,
        val matchDaoImpl: TestMatchDao
    ) : TrueLabDatabase() {
        override fun teamDao(): TeamDao = teamDaoImpl
        override fun matchDao(): MatchDao = matchDaoImpl
        override fun oddsDao(): OddsDao = throw NotImplementedError()
        override fun rankingDao(): RankingDao = throw NotImplementedError()
        override fun predictionDao(): PredictionDao = throw NotImplementedError()
        override fun leagueDao(): LeagueDao = throw NotImplementedError()
        override fun seasonDao(): SeasonDao = throw NotImplementedError()
        override fun datasetMetadataDao(): DatasetMetadataDao = throw NotImplementedError()
        override fun clearAllTables() {}
        override fun createInvalidationTracker(): InvalidationTracker = InvalidationTracker(this, "teams", "matches")
        override fun createOpenHelper(config: DatabaseConfiguration): SupportSQLiteOpenHelper = throw UnsupportedOperationException()
    }

    private class TestTeamDao : TeamDao {
        val teams = mutableMapOf<Int, TeamEntity>()
        override fun insertTeams(teams: List<TeamEntity>): LongArray = LongArray(0)
        override fun getTeamById(teamId: Int): Flow<TeamEntity?> = flowOf(teams[teamId])
        override fun getTeamByIdDirect(teamId: Int): TeamEntity? = teams[teamId]
        override fun updateTeamElo(teamId: Int, eloRating: Double) {
            teams[teamId]?.let { teams[teamId] = it.copy(eloRating = eloRating) }
        }
        override fun searchTeams(query: String): Flow<List<TeamEntity>> = flowOf(emptyList())
        override fun searchTeams(query: String, limit: Int): Flow<List<TeamEntity>> = flowOf(emptyList())
        override fun getTeams(limit: Int): Flow<List<TeamEntity>> = flowOf(emptyList())
        override fun getTeamsPaged(query: String?, sortBy: String, limit: Int, offset: Int): Flow<List<TeamWithLeagueItem>> = flowOf(emptyList())
    }

    private class TestMatchDao : MatchDao {
        val matches = mutableMapOf<Long, MatchEntity>()
        override fun insertMatches(matches: List<MatchEntity>): LongArray = LongArray(0)
        override fun upsertMatches(matches: List<MatchEntity>): LongArray = LongArray(0)
        override fun getMatchById(matchId: Long): Flow<MatchWithTeams?> = flowOf(null)
        override fun getMatchByIdDirect(matchId: Long): MatchEntity? = matches[matchId]
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
        override fun getMatchesFilteredPaged(
            query: String?,
            leagueId: Int?,
            statusFilter: String,
            sortBy: String,
            limit: Int,
            offset: Int
        ): Flow<List<MatchWithTeams>> = flowOf(emptyList())
    }
}
