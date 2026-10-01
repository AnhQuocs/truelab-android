package dev.anhquocs.truelab.core.domain.team.usecase

import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.domain.match.model.TeamSummary
import dev.anhquocs.truelab.core.domain.team.model.TeamDetail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GetHeadToHeadComparisonUseCaseTest {

    private val useCase = GetHeadToHeadComparisonUseCase()

    private val arsenal = TeamDetail(
        id = 1,
        name = "Arsenal",
        logo = "arsenal.png",
        leagueName = "Premier League",
        eloRating = 1600.0,
        formScore = 7.5
    )

    private val chelsea = TeamDetail(
        id = 2,
        name = "Chelsea",
        logo = "chelsea.png",
        leagueName = "Premier League",
        eloRating = 1550.0,
        formScore = 6.0
    )

    private val liverpool = TeamDetail(
        id = 3,
        name = "Liverpool",
        logo = "liverpool.png",
        leagueName = "Premier League",
        eloRating = 1620.0,
        formScore = 8.0
    )

    @Test
    fun `invoke with multiple H2H matches calculates correct records and percentages`() {
        val h2hMatches = listOf(
            createMatch(id = 101L, homeId = 1, awayId = 2, homeScore = 2, awayScore = 1, status = MatchStatus.ENDED, date = "2024-01-01"),
            createMatch(id = 102L, homeId = 2, awayId = 1, homeScore = 3, awayScore = 1, status = MatchStatus.ENDED, date = "2024-02-01"),
            createMatch(id = 103L, homeId = 1, awayId = 2, homeScore = 1, awayScore = 1, status = MatchStatus.ENDED, date = "2024-03-01"),
            createMatch(id = 104L, homeId = 2, awayId = 1, homeScore = 0, awayScore = 2, status = MatchStatus.ENDED, date = "2024-04-01")
        )

        val summary = useCase(
            teamA = arsenal,
            teamB = chelsea,
            h2hMatches = h2hMatches
        )

        assertEquals(4, summary.totalMatches)
        assertEquals(2, summary.teamAWins) // match 101 (2-1) and match 104 (away win 0-2)
        assertEquals(1, summary.draws)     // match 103 (1-1)
        assertEquals(1, summary.teamBWins) // match 102 (home win 3-1)
        assertEquals(6, summary.teamAGoals) // 2 + 1 + 1 + 2 = 6
        assertEquals(5, summary.teamBGoals) // 1 + 3 + 1 + 0 = 5

        assertEquals(0.50, summary.teamAWinPercent, 0.001)
        assertEquals(0.25, summary.drawPercent, 0.001)
        assertEquals(0.25, summary.teamBWinPercent, 0.001)
        assertEquals(4, summary.h2hMatches.size)
    }

    @Test
    fun `invoke with empty H2H matches returns zero metrics without NaN`() {
        val summary = useCase(
            teamA = arsenal,
            teamB = liverpool,
            h2hMatches = emptyList()
        )

        assertEquals(0, summary.totalMatches)
        assertEquals(0, summary.teamAWins)
        assertEquals(0, summary.draws)
        assertEquals(0, summary.teamBWins)
        assertEquals(0, summary.teamAGoals)
        assertEquals(0, summary.teamBGoals)
        assertEquals(0.0, summary.teamAWinPercent, 0.001)
        assertEquals(0.0, summary.drawPercent, 0.001)
        assertEquals(0.0, summary.teamBWinPercent, 0.001)
        assertEquals(0, summary.h2hMatches.size)
    }

    @Test
    fun `invoke verifies team A and team B symmetry when swapped`() {
        val h2hMatches = listOf(
            createMatch(id = 201L, homeId = 1, awayId = 2, homeScore = 3, awayScore = 0, status = MatchStatus.ENDED, date = "2024-01-01"),
            createMatch(id = 202L, homeId = 2, awayId = 1, homeScore = 1, awayScore = 1, status = MatchStatus.ENDED, date = "2024-02-01")
        )

        val summaryAB = useCase(teamA = arsenal, teamB = chelsea, h2hMatches = h2hMatches)
        val summaryBA = useCase(teamA = chelsea, teamB = arsenal, h2hMatches = h2hMatches)

        assertEquals(summaryAB.totalMatches, summaryBA.totalMatches)
        assertEquals(summaryAB.teamAWins, summaryBA.teamBWins)
        assertEquals(summaryAB.teamBWins, summaryBA.teamAWins)
        assertEquals(summaryAB.draws, summaryBA.draws)
        assertEquals(summaryAB.teamAGoals, summaryBA.teamBGoals)
        assertEquals(summaryAB.teamBGoals, summaryBA.teamAGoals)
    }

    @Test
    fun `invoke filters out non-ended or irrelevant matches`() {
        val matches = listOf(
            createMatch(id = 301L, homeId = 1, awayId = 2, homeScore = 2, awayScore = 0, status = MatchStatus.ENDED, date = "2024-01-01"),
            createMatch(id = 302L, homeId = 1, awayId = 2, homeScore = null, awayScore = null, status = MatchStatus.SCHEDULED, date = "2024-05-01"),
            createMatch(id = 303L, homeId = 1, awayId = 3, homeScore = 1, awayScore = 0, status = MatchStatus.ENDED, date = "2024-02-01") // Arsenal vs Liverpool
        )

        val summary = useCase(teamA = arsenal, teamB = chelsea, h2hMatches = matches)
        assertEquals(1, summary.totalMatches)
        assertEquals(1, summary.teamAWins)
        assertEquals(0, summary.teamBWins)
    }

    @Test
    fun `invoke computes form and statistics when recent matches provided`() {
        val recentA = listOf(
            createMatch(id = 401L, homeId = 1, awayId = 2, homeScore = 2, awayScore = 1, status = MatchStatus.ENDED, date = "2024-01-01"),
            createMatch(id = 402L, homeId = 1, awayId = 3, homeScore = 3, awayScore = 0, status = MatchStatus.ENDED, date = "2024-01-08")
        )
        val recentB = listOf(
            createMatch(id = 403L, homeId = 2, awayId = 3, homeScore = 0, awayScore = 1, status = MatchStatus.ENDED, date = "2024-01-05")
        )

        val summary = useCase(
            teamA = arsenal,
            teamB = chelsea,
            h2hMatches = emptyList(),
            teamARecentMatches = recentA,
            teamBRecentMatches = recentB
        )

        assertNotNull(summary.teamAForm)
        assertNotNull(summary.teamBForm)
        assertNotNull(summary.teamAStats)
        assertNotNull(summary.teamBStats)
        assertEquals(2, summary.teamAHomeAwaySplits.totalSplit.played)
        assertEquals(1, summary.teamBHomeAwaySplits.totalSplit.played)
        assertEquals(2, summary.teamARecentMatches.size)
        assertEquals(1, summary.teamBRecentMatches.size)
        assertEquals(401L, summary.teamARecentMatches[0].id) // oldest match first in timeline (2024-01-01 before 2024-01-08)
        assertEquals(402L, summary.teamARecentMatches[1].id)
    }

    @Test
    fun `invoke limits recent matches to top 5 ended in chronological ascending date order`() {
        val recentA = (1..8).map { i ->
            createMatch(
                id = 500L + i,
                homeId = 1,
                awayId = 10 + i,
                homeScore = 1,
                awayScore = 0,
                status = MatchStatus.ENDED,
                date = "2024-02-0$i"
            )
        }

        val summary = useCase(
            teamA = arsenal,
            teamB = chelsea,
            h2hMatches = emptyList(),
            teamARecentMatches = recentA,
            teamBRecentMatches = emptyList()
        )

        assertEquals(5, summary.teamARecentMatches.size)
        // Top 5 recent matches should be 504..508, sorted oldest -> newest (504, 505, 506, 507, 508)
        assertEquals(504L, summary.teamARecentMatches[0].id)
        assertEquals(508L, summary.teamARecentMatches[4].id)
        assertEquals(0, summary.teamBRecentMatches.size)
    }



    private fun createMatch(
        id: Long,
        homeId: Int,
        awayId: Int,
        homeScore: Int?,
        awayScore: Int?,
        status: MatchStatus,
        date: String
    ): Match = Match(
        id = id,
        homeTeam = TeamSummary(homeId, "Team $homeId", null),
        awayTeam = TeamSummary(awayId, "Team $awayId", null),
        homeScore = homeScore,
        awayScore = awayScore,
        startTimeDate = date,
        status = status
    )
}
