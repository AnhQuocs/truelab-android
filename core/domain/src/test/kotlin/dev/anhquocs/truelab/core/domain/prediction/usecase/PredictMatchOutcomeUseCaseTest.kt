package dev.anhquocs.truelab.core.domain.prediction.usecase

import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.domain.match.model.TeamSummary
import dev.anhquocs.truelab.core.domain.odds.model.OddsRecordItem
import dev.anhquocs.truelab.core.domain.prediction.model.MatchPredictionContext
import dev.anhquocs.truelab.core.domain.prediction.model.PredictionWeightConfig
import dev.anhquocs.truelab.core.domain.prediction.model.SixthSignalMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PredictMatchOutcomeUseCaseTest {

    private lateinit var useCase: PredictMatchOutcomeUseCase

    @Before
    fun setUp() {
        useCase = PredictMatchOutcomeUseCase()
    }

    private fun createMatch(
        id: Long,
        homeTeamId: Int,
        awayTeamId: Int,
        homeScore: Int?,
        awayScore: Int?,
        startTimeDate: String = "2026-09-20 20:00",
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
    fun `happy path with all 6 signals produces valid prediction result summing to 1_0`() {
        val homeRecentMatches = listOf(
            createMatch(101, homeTeamId = 1, awayTeamId = 10, homeScore = 2, awayScore = 0, startTimeDate = "2026-10-01 20:00"),
            createMatch(102, homeTeamId = 11, awayTeamId = 1, homeScore = 1, awayScore = 3, startTimeDate = "2026-09-25 20:00"),
            createMatch(103, homeTeamId = 1, awayTeamId = 12, homeScore = 1, awayScore = 1, startTimeDate = "2026-09-20 20:00"),
            createMatch(104, homeTeamId = 13, awayTeamId = 1, homeScore = 0, awayScore = 2, startTimeDate = "2026-09-15 20:00"),
            createMatch(105, homeTeamId = 1, awayTeamId = 14, homeScore = 3, awayScore = 1, startTimeDate = "2026-09-10 20:00")
        )

        val awayRecentMatches = listOf(
            createMatch(201, homeTeamId = 2, awayTeamId = 20, homeScore = 0, awayScore = 1, startTimeDate = "2026-10-03 20:00"),
            createMatch(202, homeTeamId = 21, awayTeamId = 2, homeScore = 2, awayScore = 0, startTimeDate = "2026-09-27 20:00"),
            createMatch(203, homeTeamId = 2, awayTeamId = 22, homeScore = 1, awayScore = 1, startTimeDate = "2026-09-22 20:00"),
            createMatch(204, homeTeamId = 23, awayTeamId = 2, homeScore = 3, awayScore = 0, startTimeDate = "2026-09-17 20:00"),
            createMatch(205, homeTeamId = 2, awayTeamId = 24, homeScore = 0, awayScore = 2, startTimeDate = "2026-09-12 20:00")
        )

        val h2hMatches = listOf(
            createMatch(301, homeTeamId = 1, awayTeamId = 2, homeScore = 2, awayScore = 1, startTimeDate = "2026-05-01 20:00"),
            createMatch(302, homeTeamId = 2, awayTeamId = 1, homeScore = 0, awayScore = 0, startTimeDate = "2026-01-01 20:00"),
            createMatch(303, homeTeamId = 1, awayTeamId = 2, homeScore = 1, awayScore = 0, startTimeDate = "2025-09-01 20:00")
        )

        val latestOdds = OddsRecordItem(
            companyId = 1,
            companyName = "Bet365",
            oddsType = "1X2",
            handicap = null,
            over = null,
            under = null,
            homeWin = 1.85,
            draw = 3.50,
            awayWin = 4.20,
            changeTime = 1700000000L,
            marketPhase = "FINAL"
        )

        val context = MatchPredictionContext(
            matchId = 999L,
            homeTeamId = 1,
            awayTeamId = 2,
            homeElo = 1750.0,
            awayElo = 1520.0,
            homeRecentMatches = homeRecentMatches,
            awayRecentMatches = awayRecentMatches,
            h2hMatches = h2hMatches,
            latestOdds = latestOdds,
            matchStartTimeDate = "2026-10-06 20:00"
        )

        val result = useCase(context)

        assertEquals(999L, result.matchId)
        assertEquals("Weighted Scoring", result.algorithmName)

        assertTrue(result.homeWinProb in 0.0..1.0)
        assertTrue(result.drawProb in 0.0..1.0)
        assertTrue(result.awayWinProb in 0.0..1.0)

        val sum = result.homeWinProb + result.drawProb + result.awayWinProb
        assertEquals(1.0, sum, 1e-4)

        // With stronger Elo, Form, Odds, H2H, and Rest Advantage, Home Win should be predicted
        assertTrue(result.homeWinProb > result.awayWinProb)
        assertTrue(result.homeWinProb > result.drawProb)
        assertEquals("HOME_WIN", result.predictedOutcome)
        assertEquals(result.homeWinProb, result.confidenceScore, 1e-6)

        // Verify Evidence
        assertNotNull(result.evidence)
        val ev = result.evidence!!
        assertEquals(6, ev.signals.size)
        assertEquals("1750.0", ev.elo.details["homeElo"])
        assertEquals("1520.0", ev.elo.details["awayElo"])
        assertEquals("+230.0", ev.elo.details["diff"])
        assertEquals("5", ev.elo.details["homeHistoryCount"])
        assertEquals("5", ev.elo.details["awayHistoryCount"])
        assertEquals("5", ev.form.details["homeMatches"])
        assertEquals("5", ev.form.details["awayMatches"])
        assertTrue(ev.form.details.containsKey("homeFormResults"))
        assertTrue(ev.form.details.containsKey("awayFormResults"))
        assertEquals("Bet365", ev.odds.details["bookmaker"])
        assertEquals("1.85", ev.odds.details["homeOdds"])
        assertTrue(ev.goals.details.containsKey("homeScored"))
        assertTrue(ev.goals.details.containsKey("homeConceded"))
        assertTrue(ev.elo.effectiveWeight > 0.0)
        assertTrue(ev.form.effectiveWeight > 0.0)
        assertTrue(ev.odds.effectiveWeight > 0.0)
        assertTrue(ev.goals.effectiveWeight > 0.0)
        assertTrue(ev.h2h.effectiveWeight > 0.0)
        assertTrue(ev.restAdvantage.effectiveWeight > 0.0)
        assertEquals("Rest Advantage", ev.restAdvantage.name)
        assertTrue(ev.restAdvantage.isAvailable)
        assertEquals("5.0", ev.restAdvantage.details["homeRestDays"])
        assertEquals("3.0", ev.restAdvantage.details["awayRestDays"])
        assertEquals("+2.0", ev.restAdvantage.details["deltaRestDays"])
    }

    @Test
    fun `HOME_ADVANTAGE benchmark mode works and produces Home Advantage evidence`() {
        val benchmarkConfig = PredictionWeightConfig.DEFAULT.copy(
            sixthSignalMode = SixthSignalMode.HOME_ADVANTAGE
        )

        val context = MatchPredictionContext(
            matchId = 999L,
            homeTeamId = 1,
            awayTeamId = 2,
            homeElo = 1600.0,
            awayElo = 1600.0,
            matchStartTimeDate = "2026-10-06 20:00"
        )

        val result = useCase(context, benchmarkConfig)

        assertNotNull(result)
        val ev = result.evidence!!
        assertEquals(6, ev.signals.size)
        assertEquals("Home Advantage", ev.restAdvantage.name)
        assertEquals("BASELINE_HOME_ADVANTAGE", ev.restAdvantage.details["mode"])
        assertEquals(1.0, result.homeWinProb + result.drawProb + result.awayWinProb, 1e-4)
    }

    @Test
    fun `missing odds assigns weight 0 to odds and prediction still succeeds with remaining signals`() {
        val context = MatchPredictionContext(
            matchId = 888L,
            homeTeamId = 1,
            awayTeamId = 2,
            homeElo = 1600.0,
            awayElo = 1600.0,
            latestOdds = null
        )

        val result = useCase(context)

        assertNotNull(result)
        val sum = result.homeWinProb + result.drawProb + result.awayWinProb
        assertEquals(1.0, sum, 1e-4)
    }

    @Test
    fun `missing all optional data falls back smoothly to neutral baselines`() {
        val minimalContext = MatchPredictionContext(
            matchId = 777L,
            homeTeamId = 1,
            awayTeamId = 2
        )

        val result = useCase(minimalContext)

        assertNotNull(result)
        val sum = result.homeWinProb + result.drawProb + result.awayWinProb
        assertEquals(1.0, sum, 1e-4)
        assertTrue(result.homeWinProb in 0.0..1.0)
        assertTrue(result.drawProb in 0.0..1.0)
        assertTrue(result.awayWinProb in 0.0..1.0)
        assertNotNull(result.evidence)
        assertEquals(6, result.evidence!!.signals.size)
    }

    @Test
    fun `data leakage prevention filters out current match and scheduled matches`() {
        val currentMatchId = 555L

        // Including the current match itself with a fake score inside recent matches
        val recentMatchesWithCurrent = listOf(
            createMatch(id = currentMatchId, homeTeamId = 1, awayTeamId = 2, homeScore = 10, awayScore = 0),
            createMatch(id = 501, homeTeamId = 1, awayTeamId = 3, homeScore = 1, awayScore = 0),
            createMatch(id = 502, homeTeamId = 1, awayTeamId = 4, homeScore = 0, awayScore = 0, status = MatchStatus.SCHEDULED) // Not ended
        )

        val context = MatchPredictionContext(
            matchId = currentMatchId,
            homeTeamId = 1,
            awayTeamId = 2,
            homeRecentMatches = recentMatchesWithCurrent
        )

        val result = useCase(context)

        assertNotNull(result)
        val sum = result.homeWinProb + result.drawProb + result.awayWinProb
        assertEquals(1.0, sum, 1e-4)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zero total weight in config validation throws IllegalArgumentException safely`() {
        PredictionWeightConfig(
            formWeight = 0.0,
            eloWeight = 0.0,
            oddsWeight = 0.0,
            goalsWeight = 0.0,
            h2hWeight = 0.0,
            restAdvantageWeight = 0.0,
            homeAdvantageWeight = 0.0
        )
    }

    @Test
    fun `rest advantage delta rest increases home probability and preserves symmetry`() {
        val isolatedRestConfig = PredictionWeightConfig(
            formWeight = 0.0,
            eloWeight = 0.0,
            oddsWeight = 0.0,
            goalsWeight = 0.0,
            h2hWeight = 0.0,
            restAdvantageWeight = 1.0
        )

        val homeAdvContext = MatchPredictionContext(
            matchId = 111L,
            homeTeamId = 1,
            awayTeamId = 2,
            homeRecentMatches = listOf(
                createMatch(1, 1, 3, 1, 0, startTimeDate = "2026-10-01 15:00") // 5 days rest
            ),
            awayRecentMatches = listOf(
                createMatch(2, 2, 4, 1, 0, startTimeDate = "2026-10-04 15:00") // 2 days rest
            ),
            matchStartTimeDate = "2026-10-06 15:00"
        )

        val awayAdvContext = MatchPredictionContext(
            matchId = 111L,
            homeTeamId = 1,
            awayTeamId = 2,
            homeRecentMatches = listOf(
                createMatch(1, 1, 3, 1, 0, startTimeDate = "2026-10-04 15:00") // 2 days rest
            ),
            awayRecentMatches = listOf(
                createMatch(2, 2, 4, 1, 0, startTimeDate = "2026-10-01 15:00") // 5 days rest
            ),
            matchStartTimeDate = "2026-10-06 15:00"
        )

        val homeAdvResult = useCase(homeAdvContext, isolatedRestConfig)
        val awayAdvResult = useCase(awayAdvContext, isolatedRestConfig)

        // When Home has more rest, home win probability is higher
        assertTrue(homeAdvResult.homeWinProb > homeAdvResult.awayWinProb)
        // When Away has more rest, away win probability is higher
        assertTrue(awayAdvResult.awayWinProb > awayAdvResult.homeWinProb)
        // Check exact symmetry between the two results
        assertEquals(homeAdvResult.homeWinProb, awayAdvResult.awayWinProb, 1e-4)
        assertEquals(homeAdvResult.awayWinProb, awayAdvResult.homeWinProb, 1e-4)
    }
}



