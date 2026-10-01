package dev.anhquocs.truelab.core.data.team.repository

import dev.anhquocs.truelab.core.data.ranking.local.dao.RankingDao
import dev.anhquocs.truelab.core.data.team.local.dao.TeamDao
import dev.anhquocs.truelab.core.data.team.local.entity.TeamEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TeamRepositoryImplTest {

    private lateinit var fakeTeamDao: FakeTeamDao
    private lateinit var fakeRankingDao: FakeRankingDao
    private lateinit var repository: TeamRepositoryImpl
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        fakeTeamDao = FakeTeamDao()
        fakeRankingDao = FakeRankingDao()
        repository = TeamRepositoryImpl(
            teamDao = fakeTeamDao,
            rankingDao = fakeRankingDao,
            json = json
        )
    }

    @Test
    fun getTeams_passesLimitToDao() = runTest {
        val teams = (1..100).map { id ->
            TeamEntity(id = id, name = "Team $id", logo = null, leagueName = "League", eloRating = 1500.0)
        }
        fakeTeamDao.storedTeams = teams

        val result = repository.getTeams(limit = 20).first()

        assertEquals(20, result.size)
        assertEquals("Team 1", result.first().name)
        assertEquals(20, fakeTeamDao.lastGetTeamsLimit)
    }

    @Test
    fun searchTeams_passesQueryAndLimitToDao() = runTest {
        val teams = (1..100).map { id ->
            TeamEntity(id = id, name = "Arsenal $id", logo = null, leagueName = "League", eloRating = 1500.0)
        }
        fakeTeamDao.storedTeams = teams

        val result = repository.searchTeams("Arsenal", limit = 15).first()

        assertEquals(15, result.size)
        assertEquals("Arsenal", fakeTeamDao.lastSearchQuery)
        assertEquals(15, fakeTeamDao.lastSearchLimit)
    }

    private class FakeTeamDao : TeamDao {
        var storedTeams = listOf<TeamEntity>()
        var lastGetTeamsLimit: Int? = null
        var lastSearchQuery: String? = null
        var lastSearchLimit: Int? = null

        override fun insertTeams(teams: List<TeamEntity>): LongArray = LongArray(0)

        override fun getTeamById(teamId: Int): Flow<TeamEntity?> =
            flowOf(storedTeams.find { it.id == teamId })

        override fun searchTeams(query: String): Flow<List<TeamEntity>> {
            lastSearchQuery = query
            val filtered = if (query.isBlank()) storedTeams else storedTeams.filter { it.name.contains(query, ignoreCase = true) }
            return flowOf(filtered)
        }

        override fun searchTeams(query: String, limit: Int): Flow<List<TeamEntity>> {
            lastSearchQuery = query
            lastSearchLimit = limit
            val filtered = if (query.isBlank()) storedTeams else storedTeams.filter { it.name.contains(query, ignoreCase = true) }
            return flowOf(filtered.take(limit))
        }

        override fun getTeams(limit: Int): Flow<List<TeamEntity>> {
            lastGetTeamsLimit = limit
            return flowOf(storedTeams.take(limit))
        }
    }

    private class FakeRankingDao : RankingDao {
        override fun insertRankings(rankings: List<dev.anhquocs.truelab.core.data.ranking.local.entity.SeasonRankingEntity>): LongArray = LongArray(0)
        override fun getRankingsForMatch(matchId: Long): Flow<List<dev.anhquocs.truelab.core.data.ranking.local.entity.SeasonRankingEntity>> = flowOf(emptyList())
        override fun getLatestRankingForTeam(teamId: Int): Flow<dev.anhquocs.truelab.core.data.ranking.local.entity.SeasonRankingEntity?> = flowOf(null)
        override fun getLatestSeasonRankings(): Flow<List<dev.anhquocs.truelab.core.data.ranking.local.entity.SeasonRankingEntity>> = flowOf(emptyList())
    }
}
