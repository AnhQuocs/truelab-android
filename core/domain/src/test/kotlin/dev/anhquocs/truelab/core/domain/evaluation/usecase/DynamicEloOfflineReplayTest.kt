package dev.anhquocs.truelab.core.domain.evaluation.usecase

import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.domain.match.model.TeamSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DynamicEloOfflineReplayTest {

    private val useCase = BacktestPredictionUseCase()

    private val teamA = TeamSummary(id = 1, name = "Team Alpha")
    private val teamB = TeamSummary(id = 2, name = "Team Beta")
    private val teamC = TeamSummary(id = 3, name = "Team Gamma")

    private fun createMatch(
        id: Long,
        homeTeam: TeamSummary,
        awayTeam: TeamSummary,
        homeScore: Int,
        awayScore: Int,
        startTimeDate: String
    ): Match {
        return Match(
            id = id,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            homeScore = homeScore,
            awayScore = awayScore,
            startTimeDate = startTimeDate,
            status = MatchStatus.ENDED
        )
    }

    @Test
    fun `sequential match replay updates Elo after each resolved match without future leakage`() {
        // Match 1: Alpha (1500) vs Beta (1500) -> Alpha wins 3-0 on 2026-01-01
        val match1 = createMatch(1L, teamA, teamB, 3, 0, "2026-01-01T15:00:00")

        // Match 2: Alpha vs Gamma (1500) on 2026-01-08
        // When Match 2 is predicted, Alpha's Elo should reflect the WIN from Match 1 (> 1500.0)
        val match2 = createMatch(2L, teamA, teamC, 1, 0, "2026-01-08T15:00:00")

        // Match 3: Beta vs Gamma on 2026-01-15
        // Beta's Elo should reflect the LOSS from Match 1 (< 1500.0)
        val match3 = createMatch(3L, teamB, teamC, 0, 1, "2026-01-15T15:00:00")

        val initialElo = mapOf(1 to 1500.0, 2 to 1500.0, 3 to 1500.0)

        val result = useCase(
            matches = listOf(match3, match1, match2), // scrambled input order
            teamEloMap = initialElo
        )

        assertEquals(3, result.records.size)

        // 1. In match1: Alpha vs Beta, both start at 1500.0. Elo probabilities should be symmetric
        val rec1 = result.records[0]
        assertEquals(1L, rec1.matchId)

        // 2. In match2: Alpha vs Gamma. Alpha won match1 so Alpha Elo > 1500.0, Gamma Elo = 1500.0.
        // Therefore, homeWinProb (Alpha) > awayWinProb (Gamma)
        val rec2 = result.records[1]
        assertEquals(2L, rec2.matchId)
        assertTrue(
            "Expected Alpha homeWinProb > Gamma awayWinProb after Alpha win in Match 1, but home=${rec2.homeWinProb}, away=${rec2.awayWinProb}",
            rec2.homeWinProb > rec2.awayWinProb
        )

        // 3. In match3: Beta vs Gamma. Beta lost match1 so Beta Elo < 1500.0, Gamma lost match2 so Gamma Elo < 1500.0.
        val rec3 = result.records[2]
        assertEquals(3L, rec3.matchId)
    }
}
