package dev.anhquocs.truelab.core.data.match.repository

import dev.anhquocs.truelab.core.data.match.local.dao.MatchDao
import dev.anhquocs.truelab.core.data.match.local.entity.MatchEntity
import dev.anhquocs.truelab.core.data.match.local.entity.MatchWithTeams
import dev.anhquocs.truelab.core.data.team.local.entity.TeamEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MatchRepositoryImplTest {

    private class FakeMatchDao : MatchDao {
        val matches = mutableListOf<MatchWithTeams>()

        override fun insertMatches(matches: List<MatchEntity>): LongArray = LongArray(matches.size)
        override fun upsertMatches(matches: List<MatchEntity>): LongArray = LongArray(matches.size)

        override fun getMatchById(matchId: Long): Flow<MatchWithTeams?> =
            flowOf(matches.find { it.match.id == matchId })

        override fun getMatchesPaged(limit: Int, offset: Int): Flow<List<MatchWithTeams>> =
            flowOf(matches.drop(offset).take(limit))

        override fun getMatchesByDate(date: String): Flow<List<MatchWithTeams>> =
            flowOf(matches.filter { it.match.startTimeDate.startsWith(date) })

        override fun getMatchesByStatus(status: String): Flow<List<MatchWithTeams>> =
            flowOf(matches.filter { it.match.status == status })

        override fun getH2HMatches(teamAId: Int, teamBId: Int): Flow<List<MatchWithTeams>> =
            flowOf(matches.filter {
                (it.match.homeTeamId == teamAId && it.match.awayTeamId == teamBId) ||
                (it.match.homeTeamId == teamBId && it.match.awayTeamId == teamAId)
            })

        override fun getRecentMatchesForTeam(teamId: Int, limit: Int): Flow<List<MatchWithTeams>> =
            flowOf(matches.filter {
                it.match.homeTeamId == teamId || it.match.awayTeamId == teamId
            }.take(limit))

        override fun getMatchesByLeague(leagueId: Int): Flow<List<MatchWithTeams>> =
            flowOf(matches.filter { it.match.leagueId == leagueId })

        override fun getMatchesByLeagueAndSeason(leagueId: Int, season: String): Flow<List<MatchWithTeams>> =
            flowOf(matches.filter { it.match.leagueId == leagueId && it.match.season == season })

        override fun getAllMatches(): Flow<List<MatchWithTeams>> =
            flowOf(matches.sortedBy { it.match.startTimeDate })

        override fun getPredictableMatches(limit: Int): Flow<List<MatchWithTeams>> =
            flowOf(matches.take(limit))

        override fun searchMatches(query: String, limit: Int): Flow<List<MatchWithTeams>> =
            flowOf(matches.filter {
                it.homeTeam.name.contains(query, ignoreCase = true) ||
                it.awayTeam.name.contains(query, ignoreCase = true)
            }.take(limit))

        override fun getPredictableMatchesFiltered(
            startDateUtc: String?,
            endDateUtc: String?,
            isPastDate: Boolean,
            isFutureDate: Boolean,
            leagueId: Int?,
            statusFilter: String,
            searchQuery: String?,
            limit: Int
        ): Flow<List<MatchWithTeams>> = flowOf(
            matches.filter { item ->
                val normalizedStart = item.match.startTimeDate.replace(" ", "T")
                val dateMatch = (startDateUtc == null || normalizedStart >= startDateUtc) &&
                    (endDateUtc == null || normalizedStart < endDateUtc)
                val leagueMatch = leagueId == null || item.match.leagueId == leagueId
                val statusMatch = when {
                    statusFilter == "FINISHED" -> item.match.status in listOf("8", "ended", "determined", "finished", "ft", "aet", "pen")
                    isPastDate -> true
                    isFutureDate -> true
                    statusFilter == "LIVE_AND_UPCOMING" -> item.match.status in listOf("live", "pending", "1", "0")
                    statusFilter == "LIVE" -> item.match.status in listOf("live", "1")
                    statusFilter == "UPCOMING" -> item.match.status in listOf("pending", "0")
                    else -> true
                }
                val searchMatch = searchQuery.isNullOrBlank() ||
                    item.homeTeam.name.contains(searchQuery, ignoreCase = true) ||
                    item.awayTeam.name.contains(searchQuery, ignoreCase = true)
                dateMatch && leagueMatch && statusMatch && searchMatch
            }.take(limit)
        )
    }

    @Test
    fun getMatchesByLeagueAndSeason_returnsFilteredDomainMatches() = runTest {
        val dao = FakeMatchDao()
        val repo = MatchRepositoryImpl(dao)

        val teamA = TeamEntity(id = 1, name = "Arsenal", logo = null, leagueName = "EPL")
        val teamB = TeamEntity(id = 2, name = "Chelsea", logo = null, leagueName = "EPL")
        val teamC = TeamEntity(id = 3, name = "Barcelona", logo = null, leagueName = "La Liga")

        dao.matches.addAll(
            listOf(
                MatchWithTeams(
                    match = MatchEntity(
                        id = 101L,
                        homeTeamId = 1,
                        awayTeamId = 2,
                        homeScore = 2,
                        awayScore = 0,
                        startTimeDate = "2024-05-01 15:00:00",
                        status = "8",
                        leagueId = 39,
                        season = "2023-2024"
                    ),
                    homeTeam = teamA,
                    awayTeam = teamB
                ),
                MatchWithTeams(
                    match = MatchEntity(
                        id = 102L,
                        homeTeamId = 1,
                        awayTeamId = 2,
                        homeScore = 1,
                        awayScore = 1,
                        startTimeDate = "2023-05-01 15:00:00",
                        status = "8",
                        leagueId = 39,
                        season = "2022-2023"
                    ),
                    homeTeam = teamA,
                    awayTeam = teamB
                ),
                MatchWithTeams(
                    match = MatchEntity(
                        id = 103L,
                        homeTeamId = 3,
                        awayTeamId = 1,
                        homeScore = 3,
                        awayScore = 1,
                        startTimeDate = "2024-05-01 15:00:00",
                        status = "8",
                        leagueId = 140,
                        season = "2023-2024"
                    ),
                    homeTeam = teamC,
                    awayTeam = teamA
                )
            )
        )

        val epl2024 = repo.getMatchesByLeagueAndSeason(39, "2023-2024").first()

        assertEquals(1, epl2024.size)
        assertEquals(101L, epl2024[0].id)
        assertEquals("Arsenal", epl2024[0].homeTeam.name)
        assertEquals("Chelsea", epl2024[0].awayTeam.name)
        assertEquals(39, epl2024[0].leagueId)
        assertEquals("2023-2024", epl2024[0].season)
    }

    @Test
    fun getMatchesByLeagueAndSeason_noMatchesFound_returnsEmptyList() = runTest {
        val dao = FakeMatchDao()
        val repo = MatchRepositoryImpl(dao)

        val result = repo.getMatchesByLeagueAndSeason(999, "2023-2024").first()
        org.junit.Assert.assertTrue(result.isEmpty())
    }

    @Test
    fun getMatches_byDate_returnsMappedMatches() = runTest {
        val dao = FakeMatchDao()
        val repo = MatchRepositoryImpl(dao)

        val teamA = TeamEntity(id = 1, name = "Arsenal", logo = null, leagueName = "EPL")
        val teamB = TeamEntity(id = 2, name = "Chelsea", logo = null, leagueName = "EPL")
        dao.matches.add(
            MatchWithTeams(
                match = MatchEntity(
                    id = 101L,
                    homeTeamId = 1,
                    awayTeamId = 2,
                    homeScore = 2,
                    awayScore = 0,
                    startTimeDate = "2024-05-01 15:00:00",
                    status = "8"
                ),
                homeTeam = teamA,
                awayTeam = teamB
            )
        )

        val matches = repo.getMatches("2024-05-01").first()
        assertEquals(1, matches.size)
        assertEquals(101L, matches[0].id)
        assertEquals("Arsenal", matches[0].homeTeam.name)
    }

    @Test
    fun getMatchDetail_returnsSingleMappedMatch() = runTest {
        val dao = FakeMatchDao()
        val repo = MatchRepositoryImpl(dao)

        val teamA = TeamEntity(id = 1, name = "Arsenal", logo = null, leagueName = "EPL")
        val teamB = TeamEntity(id = 2, name = "Chelsea", logo = null, leagueName = "EPL")
        dao.matches.add(
            MatchWithTeams(
                match = MatchEntity(
                    id = 101L,
                    homeTeamId = 1,
                    awayTeamId = 2,
                    homeScore = 2,
                    awayScore = 0,
                    startTimeDate = "2024-05-01 15:00:00",
                    status = "8"
                ),
                homeTeam = teamA,
                awayTeam = teamB
            )
        )

        val match = repo.getMatchDetail(101L).first()
        org.junit.Assert.assertNotNull(match)
        assertEquals(101L, match?.id)

        val notFound = repo.getMatchDetail(999L).first()
        org.junit.Assert.assertNull(notFound)
    }

    @Test
    fun getRecentMatchesForTeam_returnsTeamMatchesUpToLimit() = runTest {
        val dao = FakeMatchDao()
        val repo = MatchRepositoryImpl(dao)

        val teamA = TeamEntity(id = 1, name = "Arsenal", logo = null, leagueName = "EPL")
        val teamB = TeamEntity(id = 2, name = "Chelsea", logo = null, leagueName = "EPL")

        for (i in 1..10) {
            dao.matches.add(
                MatchWithTeams(
                    match = MatchEntity(
                        id = i.toLong(),
                        homeTeamId = 1,
                        awayTeamId = 2,
                        homeScore = 1,
                        awayScore = 0,
                        startTimeDate = "2024-05-0$i 15:00:00",
                        status = "8"
                    ),
                    homeTeam = teamA,
                    awayTeam = teamB
                )
            )
        }

        val recentMatches = repo.getRecentMatchesForTeam(1, limit = 5).first()
        assertEquals(5, recentMatches.size)
    }

    @Test
    fun getH2HMatches_returnsMatchesBetweenTwoTeamsBothHomeAndAway() = runTest {
        val dao = FakeMatchDao()
        val repo = MatchRepositoryImpl(dao)

        val team1 = TeamEntity(id = 1, name = "Arsenal", logo = null, leagueName = "EPL")
        val team2 = TeamEntity(id = 2, name = "Chelsea", logo = null, leagueName = "EPL")
        val team3 = TeamEntity(id = 3, name = "Liverpool", logo = null, leagueName = "EPL")

        dao.matches.addAll(
            listOf(
                MatchWithTeams(
                    match = MatchEntity(1L, 1, 2, 2, 1, "2024-01-01", "8"),
                    homeTeam = team1,
                    awayTeam = team2
                ),
                MatchWithTeams(
                    match = MatchEntity(2L, 2, 1, 0, 0, "2024-02-01", "8"),
                    homeTeam = team2,
                    awayTeam = team1
                ),
                MatchWithTeams(
                    match = MatchEntity(3L, 1, 3, 3, 0, "2024-03-01", "8"),
                    homeTeam = team1,
                    awayTeam = team3
                )
            )
        )

        val h2h = repo.getH2HMatches(1, 2).first()
        assertEquals(2, h2h.size)
        assertEquals(listOf(1L, 2L), h2h.map { it.id })
    }

    @Test
    fun getAllMatches_returnsAllMappedMatchesSortedByDate() = runTest {
        val dao = FakeMatchDao()
        val repo = MatchRepositoryImpl(dao)

        val team1 = TeamEntity(id = 1, name = "Arsenal", logo = null, leagueName = "EPL")
        val team2 = TeamEntity(id = 2, name = "Chelsea", logo = null, leagueName = "EPL")

        dao.matches.addAll(
            listOf(
                MatchWithTeams(
                    match = MatchEntity(2L, 2, 1, 0, 0, "2024-02-01", "8"),
                    homeTeam = team2,
                    awayTeam = team1
                ),
                MatchWithTeams(
                    match = MatchEntity(1L, 1, 2, 2, 1, "2024-01-01", "8"),
                    homeTeam = team1,
                    awayTeam = team2
                )
            )
        )

        val allMatches = repo.getAllMatches().first()
        assertEquals(2, allMatches.size)
        assertEquals(1L, allMatches[0].id)
        assertEquals(2L, allMatches[1].id)
    }

    @Test
    fun getPredictableMatchesFiltered_filtersProperlyByDateLeagueAndStatus() = runTest {
        val dao = FakeMatchDao()
        val repo = MatchRepositoryImpl(dao)

        val team1 = TeamEntity(id = 1, name = "Arsenal", logo = null, leagueName = "EPL")
        val team2 = TeamEntity(id = 2, name = "Chelsea", logo = null, leagueName = "EPL")

        dao.matches.addAll(
            listOf(
                MatchWithTeams(
                    match = MatchEntity(1L, 1, 2, 1, 0, "2026-04-01 15:00:00", "live", leagueId = 10, minutes = "45+2"),
                    homeTeam = team1,
                    awayTeam = team2
                ),
                MatchWithTeams(
                    match = MatchEntity(2L, 2, 1, null, null, "2026-04-01 18:00:00", "pending", leagueId = 10),
                    homeTeam = team2,
                    awayTeam = team1
                ),
                MatchWithTeams(
                    match = MatchEntity(3L, 1, 2, 2, 0, "2026-04-01 20:00:00", "ended", leagueId = 10),
                    homeTeam = team1,
                    awayTeam = team2
                )
            )
        )

        val liveOnly = repo.getPredictableMatchesFiltered(
            datePrefix = "2026-04-01",
            leagueId = 10,
            statusFilter = dev.anhquocs.truelab.core.domain.match.model.PredictionStatusFilter.LIVE,
            limit = 50
        ).first()

        assertEquals(1, liveOnly.size)
        assertEquals(1L, liveOnly[0].id)
        assertEquals("45+2", liveOnly[0].minutes)
        assertEquals(dev.anhquocs.truelab.core.domain.match.model.MatchStatus.IN_PROGRESS, liveOnly[0].status)

        val liveAndUpcoming = repo.getPredictableMatchesFiltered(
            datePrefix = "2026-04-01",
            leagueId = 10,
            statusFilter = dev.anhquocs.truelab.core.domain.match.model.PredictionStatusFilter.LIVE_AND_UPCOMING,
            limit = 50
        ).first()

        assertEquals(2, liveAndUpcoming.size)
        assertEquals(listOf(1L, 2L), liveAndUpcoming.map { it.id })
    }
}

