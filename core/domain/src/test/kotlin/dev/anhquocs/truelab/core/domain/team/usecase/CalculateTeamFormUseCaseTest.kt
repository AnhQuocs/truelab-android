package dev.anhquocs.truelab.core.domain.team.usecase

import dev.anhquocs.truelab.core.algorithm.evaluation.LinearDecayFormEvaluator
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.domain.match.model.TeamSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class CalculateTeamFormUseCaseTest {

    private lateinit var useCase: CalculateTeamFormUseCase

    @Before
    fun setUp() {
        useCase = CalculateTeamFormUseCase(
            formEvaluator = LinearDecayFormEvaluator()
        )
    }

    private fun createMatch(
        id: Long,
        homeTeamId: Int,
        awayTeamId: Int,
        homeScore: Int?,
        awayScore: Int?,
        startTimeDate: String = "2026-09-01 20:00",
        status: MatchStatus = MatchStatus.ENDED
    ): Match {
        return Match(
            id = id,
            homeTeam = TeamSummary(id = homeTeamId, name = "Team $homeTeamId"),
            awayTeam = TeamSummary(id = awayTeamId, name = "Team $awayTeamId"),
            homeScore = homeScore,
            awayScore = awayScore,
            startTimeDate = startTimeDate,
            status = status
        )
    }

    @Test
    fun `evaluates 5 matches sequence W-W-D-L-W with expected FormScore`() {
        // Chronological order: W, W, D, L, W
        // Weights: 1, 2, 3, 4, 5
        // Pts: 3, 3, 1, 0, 3
        // Total points = 10, max points = 15 -> rawScore = 66.67%
        // Weighted sum = 1*3 + 2*3 + 3*1 + 4*0 + 5*3 = 3 + 6 + 3 + 0 + 15 = 27
        // Total weights * 3 = 15 * 3 = 45 -> score = 27 / 45 * 100 = 60.0%
        val matches = listOf(
            createMatch(1, homeTeamId = 1, awayTeamId = 2, homeScore = 2, awayScore = 1, startTimeDate = "2026-09-01 18:00"), // W
            createMatch(2, homeTeamId = 3, awayTeamId = 1, homeScore = 0, awayScore = 2, startTimeDate = "2026-09-05 18:00"), // W
            createMatch(3, homeTeamId = 1, awayTeamId = 4, homeScore = 1, awayScore = 1, startTimeDate = "2026-09-10 18:00"), // D
            createMatch(4, homeTeamId = 5, awayTeamId = 1, homeScore = 3, awayScore = 1, startTimeDate = "2026-09-15 18:00"), // L
            createMatch(5, homeTeamId = 1, awayTeamId = 6, homeScore = 4, awayScore = 0, startTimeDate = "2026-09-20 18:00")  // W
        )

        val result = useCase(teamId = 1, matches = matches, windowSize = 5)

        assertEquals(5, result.matchesCount)
        assertEquals(3, result.wins)
        assertEquals(1, result.draws)
        assertEquals(1, result.losses)
        assertEquals(10.0, result.totalPoints, 0.001)
        assertEquals(15.0, result.maxPoints, 0.001)
        assertEquals((10.0 / 15.0) * 100.0, result.rawScore, 0.001)
        assertEquals(60.0, result.score, 0.001)
    }

    @Test
    fun `evaluates correctly when input list is in reverse chronological order`() {
        // Reverse order (newest first): W, L, D, W, W
        val matches = listOf(
            createMatch(5, homeTeamId = 1, awayTeamId = 6, homeScore = 4, awayScore = 0, startTimeDate = "2026-09-20 18:00"), // W (newest)
            createMatch(4, homeTeamId = 5, awayTeamId = 1, homeScore = 3, awayScore = 1, startTimeDate = "2026-09-15 18:00"), // L
            createMatch(3, homeTeamId = 1, awayTeamId = 4, homeScore = 1, awayScore = 1, startTimeDate = "2026-09-10 18:00"), // D
            createMatch(2, homeTeamId = 3, awayTeamId = 1, homeScore = 0, awayScore = 2, startTimeDate = "2026-09-05 18:00"), // W
            createMatch(1, homeTeamId = 1, awayTeamId = 2, homeScore = 2, awayScore = 1, startTimeDate = "2026-09-01 18:00")  // W (oldest)
        )

        val result = useCase(teamId = 1, matches = matches, windowSize = 5)

        // After chronological sorting, sequence is W, W, D, L, W
        assertEquals(5, result.matchesCount)
        assertEquals(3, result.wins)
        assertEquals(1, result.draws)
        assertEquals(1, result.losses)
        assertEquals(60.0, result.score, 0.001)
        assertEquals((10.0 / 15.0) * 100.0, result.rawScore, 0.001)
    }

    @Test
    fun `evaluates correctly when match count N is less than windowSize`() {
        // 2 matches with windowSize = 5
        // Chronological: W (weight 1, pts 3), L (weight 2, pts 0)
        // Total points = 3.0, max points = 2 * 3 = 6.0 -> rawScore = 50.0%
        // Weighted sum = 1*3 + 2*0 = 3.0
        // Total weights * 3 = (1 + 2) * 3 = 9.0 -> score = 3.0 / 9.0 * 100 = 33.3333%
        val matches = listOf(
            createMatch(1, homeTeamId = 1, awayTeamId = 2, homeScore = 1, awayScore = 0, startTimeDate = "2026-09-01 18:00"),
            createMatch(2, homeTeamId = 1, awayTeamId = 3, homeScore = 0, awayScore = 2, startTimeDate = "2026-09-05 18:00")
        )

        val result = useCase(teamId = 1, matches = matches, windowSize = 5)

        assertEquals(2, result.matchesCount)
        assertEquals(1, result.wins)
        assertEquals(0, result.draws)
        assertEquals(1, result.losses)
        assertEquals(3.0, result.totalPoints, 0.001)
        assertEquals(6.0, result.maxPoints, 0.001)
        assertEquals(50.0, result.rawScore, 0.001)
    }

    @Test
    fun `evaluates correctly when match count N is greater than windowSize`() {
        // 7 matches with windowSize = 5
        // Chronological: L, L, W, W, D, L, W
        // The first 2 older matches (L, L) should be outside the window of 5
        // The last 5 matches are: W (wt 1), W (wt 2), D (wt 3), L (wt 4), W (wt 5)
        // Score should be identical to the 5-match sequence W-W-D-L-W (60.0%)
        val matches = listOf(
            createMatch(10, homeTeamId = 1, awayTeamId = 9, homeScore = 0, awayScore = 3, startTimeDate = "2026-08-20 18:00"), // L (ignored)
            createMatch(11, homeTeamId = 1, awayTeamId = 8, homeScore = 1, awayScore = 2, startTimeDate = "2026-08-25 18:00"), // L (ignored)
            createMatch(1, homeTeamId = 1, awayTeamId = 2, homeScore = 2, awayScore = 1, startTimeDate = "2026-09-01 18:00"),  // W
            createMatch(2, homeTeamId = 3, awayTeamId = 1, homeScore = 0, awayScore = 2, startTimeDate = "2026-09-05 18:00"),  // W
            createMatch(3, homeTeamId = 1, awayTeamId = 4, homeScore = 1, awayScore = 1, startTimeDate = "2026-09-10 18:00"),  // D
            createMatch(4, homeTeamId = 5, awayTeamId = 1, homeScore = 3, awayScore = 1, startTimeDate = "2026-09-15 18:00"),  // L
            createMatch(5, homeTeamId = 1, awayTeamId = 6, homeScore = 4, awayScore = 0, startTimeDate = "2026-09-20 18:00")   // W
        )

        val result = useCase(teamId = 1, matches = matches, windowSize = 5)

        assertEquals(5, result.matchesCount)
        assertEquals(3, result.wins)
        assertEquals(1, result.draws)
        assertEquals(1, result.losses)
        assertEquals(10.0, result.totalPoints, 0.001)
        assertEquals(15.0, result.maxPoints, 0.001)
        assertEquals(60.0, result.score, 0.001)
    }


    @Test
    fun `returns empty FormScore when matches list is empty`() {
        val result = useCase(teamId = 1, matches = emptyList(), windowSize = 5)

        assertEquals(0, result.matchesCount)
        assertEquals(0.0, result.score, 0.0)
        assertEquals(0.0, result.rawScore, 0.0)
        assertEquals(0, result.wins)
        assertEquals(0, result.draws)
        assertEquals(0, result.losses)
        assertEquals(0.0, result.totalPoints, 0.0)
        assertEquals(0.0, result.maxPoints, 0.0)
    }

    @Test
    fun `ignores unfinished matches`() {
        val matches = listOf(
            createMatch(1, homeTeamId = 1, awayTeamId = 2, homeScore = 2, awayScore = 0, status = MatchStatus.ENDED),
            createMatch(2, homeTeamId = 1, awayTeamId = 3, homeScore = 0, awayScore = 0, status = MatchStatus.IN_PROGRESS),
            createMatch(3, homeTeamId = 1, awayTeamId = 4, homeScore = null, awayScore = null, status = MatchStatus.SCHEDULED)
        )

        val result = useCase(teamId = 1, matches = matches, windowSize = 5)

        assertEquals(1, result.matchesCount)
        assertEquals(1, result.wins)
        assertEquals(100.0, result.score, 0.001)
    }

    @Test
    fun `ignores matches with null homeScore or awayScore`() {
        val matches = listOf(
            createMatch(1, homeTeamId = 1, awayTeamId = 2, homeScore = 3, awayScore = 1, status = MatchStatus.ENDED),
            createMatch(2, homeTeamId = 1, awayTeamId = 3, homeScore = null, awayScore = 1, status = MatchStatus.ENDED),
            createMatch(3, homeTeamId = 1, awayTeamId = 4, homeScore = 2, awayScore = null, status = MatchStatus.ENDED)
        )

        val result = useCase(teamId = 1, matches = matches, windowSize = 5)

        assertEquals(1, result.matchesCount)
        assertEquals(1, result.wins)
        assertEquals(100.0, result.score, 0.001)
    }

    @Test
    fun `excludes currentMatchId to prevent data leakage`() {
        val matches = listOf(
            createMatch(100, homeTeamId = 1, awayTeamId = 2, homeScore = 2, awayScore = 0, startTimeDate = "2026-09-01 18:00"),
            createMatch(101, homeTeamId = 1, awayTeamId = 3, homeScore = 3, awayScore = 0, startTimeDate = "2026-09-05 18:00") // Current match
        )

        val result = useCase(teamId = 1, matches = matches, windowSize = 5, currentMatchId = 101L)

        assertEquals(1, result.matchesCount)
        assertEquals(1, result.wins)
        assertEquals(100.0, result.score, 0.001)
    }

    @Test
    fun `correctly maps outcomes when team is at home`() {
        val matches = listOf(
            createMatch(1, homeTeamId = 1, awayTeamId = 2, homeScore = 2, awayScore = 0, startTimeDate = "2026-09-01 18:00"), // Home Win
            createMatch(2, homeTeamId = 1, awayTeamId = 3, homeScore = 1, awayScore = 1, startTimeDate = "2026-09-05 18:00"), // Home Draw
            createMatch(3, homeTeamId = 1, awayTeamId = 4, homeScore = 0, awayScore = 1, startTimeDate = "2026-09-10 18:00")  // Home Loss
        )

        val result = useCase(teamId = 1, matches = matches, windowSize = 5)

        assertEquals(3, result.matchesCount)
        assertEquals(1, result.wins)
        assertEquals(1, result.draws)
        assertEquals(1, result.losses)
    }

    @Test
    fun `correctly maps outcomes when team is at away`() {
        val matches = listOf(
            createMatch(1, homeTeamId = 2, awayTeamId = 1, homeScore = 0, awayScore = 2, startTimeDate = "2026-09-01 18:00"), // Away Win
            createMatch(2, homeTeamId = 3, awayTeamId = 1, homeScore = 1, awayScore = 1, startTimeDate = "2026-09-05 18:00"), // Away Draw
            createMatch(3, homeTeamId = 4, awayTeamId = 1, homeScore = 3, awayScore = 0, startTimeDate = "2026-09-10 18:00")  // Away Loss
        )

        val result = useCase(teamId = 1, matches = matches, windowSize = 5)

        assertEquals(3, result.matchesCount)
        assertEquals(1, result.wins)
        assertEquals(1, result.draws)
        assertEquals(1, result.losses)
    }

    @Test
    fun `does not mutate input dataset`() {
        val original = listOf(
            createMatch(2, homeTeamId = 1, awayTeamId = 3, homeScore = 0, awayScore = 1, startTimeDate = "2026-09-10 18:00"),
            createMatch(1, homeTeamId = 1, awayTeamId = 2, homeScore = 2, awayScore = 0, startTimeDate = "2026-09-01 18:00")
        )
        val copy = original.toList()

        useCase(teamId = 1, matches = original, windowSize = 5)

        assertEquals(copy, original)
        assertEquals(2L, original[0].id)
        assertEquals(1L, original[1].id)
    }

    @Test
    fun `ignores matches where team is not involved`() {
        val matches = listOf(
            createMatch(1, homeTeamId = 1, awayTeamId = 2, homeScore = 2, awayScore = 0, startTimeDate = "2026-09-01 18:00"),
            createMatch(2, homeTeamId = 88, awayTeamId = 99, homeScore = 1, awayScore = 1, startTimeDate = "2026-09-05 18:00")
        )

        val result = useCase(teamId = 1, matches = matches, windowSize = 5)

        assertEquals(1, result.matchesCount)
        assertEquals(1, result.wins)
    }

    @Test
    fun `throws IllegalArgumentException when windowSize is less than or equal to zero`() {
        assertThrows(IllegalArgumentException::class.java) {
            useCase(teamId = 1, matches = emptyList(), windowSize = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            useCase(teamId = 1, matches = emptyList(), windowSize = -1)
        }
    }
}
