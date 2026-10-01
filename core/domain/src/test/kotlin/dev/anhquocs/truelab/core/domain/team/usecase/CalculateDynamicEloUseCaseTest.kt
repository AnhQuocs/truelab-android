package dev.anhquocs.truelab.core.domain.team.usecase

import dev.anhquocs.truelab.core.algorithm.rating.EloRatingCalculator
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.domain.match.model.TeamSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CalculateDynamicEloUseCaseTest {

    private lateinit var useCase: CalculateDynamicEloUseCase

    @Before
    fun setUp() {
        useCase = CalculateDynamicEloUseCase(EloRatingCalculator())
    }

    private fun createMatch(
        id: Long,
        homeId: Int,
        awayId: Int,
        homeScore: Int?,
        awayScore: Int?,
        startTime: String,
        status: MatchStatus = MatchStatus.ENDED
    ): Match {
        return Match(
            id = id,
            homeTeam = TeamSummary(id = homeId, name = "Team $homeId"),
            awayTeam = TeamSummary(id = awayId, name = "Team $awayId"),
            homeScore = homeScore,
            awayScore = awayScore,
            startTimeDate = startTime,
            status = status
        )
    }

    @Test
    fun invoke_withEmptyMatches_returnsInitialEloMap() {
        val initialMap = mapOf(1 to 1500.0, 2 to 1500.0)
        val result = useCase(emptyList(), initialEloMap = initialMap)
        assertEquals(1500.0, result[1] ?: 0.0, 0.01)
        assertEquals(1500.0, result[2] ?: 0.0, 0.01)
    }

    @Test
    fun invoke_chronologicalReplay_updatesRatingsCorrectly() {
        val matches = listOf(
            createMatch(1L, homeId = 1, awayId = 2, homeScore = 2, awayScore = 0, startTime = "2026-01-01T10:00:00Z"),
            createMatch(2L, homeId = 1, awayId = 3, homeScore = 3, awayScore = 1, startTime = "2026-01-08T10:00:00Z")
        )

        val result = useCase(matches, beforeTimestamp = "2026-01-10T00:00:00Z")

        // Team 1 won twice starting from 1500.0 -> Elo should be > 1500.0
        val team1Elo = result[1] ?: 1500.0
        val team2Elo = result[2] ?: 1500.0
        val team3Elo = result[3] ?: 1500.0

        assertTrue("Team 1 won two matches, rating must increase", team1Elo > 1520.0)
        assertTrue("Team 2 lost, rating must decrease", team2Elo < 1500.0)
        assertTrue("Team 3 lost, rating must decrease", team3Elo < 1500.0)
    }

    @Test
    fun invoke_zeroTemporalLeakage_excludesMatchesAfterThreshold() {
        val matches = listOf(
            createMatch(1L, homeId = 1, awayId = 2, homeScore = 2, awayScore = 0, startTime = "2026-01-01T10:00:00Z"),
            createMatch(2L, homeId = 1, awayId = 2, homeScore = 0, awayScore = 5, startTime = "2026-01-15T10:00:00Z") // Future match
        )

        // Only replay before Jan 10
        val result = useCase(matches, beforeTimestamp = "2026-01-10T10:00:00Z")

        val team1Elo = result[1] ?: 1500.0
        val team2Elo = result[2] ?: 1500.0

        // Team 1 won match 1, match 2 should NOT be included
        assertTrue("Team 1 rating should reflect only match 1", team1Elo > 1500.0)
        assertTrue("Team 2 rating should reflect only match 1", team2Elo < 1500.0)
    }

    @Test
    fun invoke_ignoresUnfinishedOrNullScoreMatches() {
        val matches = listOf(
            createMatch(1L, homeId = 1, awayId = 2, homeScore = null, awayScore = null, startTime = "2026-01-01T10:00:00Z", status = MatchStatus.SCHEDULED)
        )

        val result = useCase(matches)
        assertEquals(1500.0, result[1] ?: 1500.0, 0.01)
        assertEquals(1500.0, result[2] ?: 1500.0, 0.01)
    }
}
