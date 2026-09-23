package dev.anhquocs.truelab.core.domain.prediction.usecase

import dev.anhquocs.truelab.core.algorithm.evaluation.FormScore
import dev.anhquocs.truelab.core.algorithm.evaluation.LinearDecayFormEvaluator
import dev.anhquocs.truelab.core.algorithm.prediction.DefaultWeightedScorer
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.domain.match.model.TeamSummary
import dev.anhquocs.truelab.core.domain.odds.model.OddsRecordItem
import dev.anhquocs.truelab.core.domain.prediction.model.MatchPredictionContext
import dev.anhquocs.truelab.core.domain.prediction.model.PredictionWeightConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PredictMatchOutcomeUseCaseTest {

    private lateinit var useCase: PredictMatchOutcomeUseCase

    @Before
    fun setUp() {
        useCase = PredictMatchOutcomeUseCase(
            weightedScorer = DefaultWeightedScorer(),
            formEvaluator = LinearDecayFormEvaluator(),
            defaultConfig = PredictionWeightConfig.DEFAULT
        )
    }

    private fun createMatch(
        id: Long,
        homeTeamId: Int,
        awayTeamId: Int,
        homeScore: Int?,
        awayScore: Int?,
        status: MatchStatus = MatchStatus.ENDED
    ): Match {
        return Match(
            id = id,
            homeTeam = TeamSummary(id = homeTeamId, name = "Team $homeTeamId"),
            awayTeam = TeamSummary(id = awayTeamId, name = "Team $awayTeamId"),
            homeScore = homeScore,
            awayScore = awayScore,
            startTimeDate = "2026-09-20 20:00",
            status = status
        )
    }

    @Test
    fun `happy path with all 6 signals produces valid prediction result summing to 1_0`() {
        val homeRecentMatches = listOf(
            createMatch(101, homeTeamId = 1, awayTeamId = 10, homeScore = 2, awayScore = 0),
            createMatch(102, homeTeamId = 11, awayTeamId = 1, homeScore = 1, awayScore = 3),
            createMatch(103, homeTeamId = 1, awayTeamId = 12, homeScore = 1, awayScore = 1),
            createMatch(104, homeTeamId = 13, awayTeamId = 1, homeScore = 0, awayScore = 2),
            createMatch(105, homeTeamId = 1, awayTeamId = 14, homeScore = 3, awayScore = 1)
        )

        val awayRecentMatches = listOf(
            createMatch(201, homeTeamId = 2, awayTeamId = 20, homeScore = 0, awayScore = 1),
            createMatch(202, homeTeamId = 21, awayTeamId = 2, homeScore = 2, awayScore = 0),
            createMatch(203, homeTeamId = 2, awayTeamId = 22, homeScore = 1, awayScore = 1),
            createMatch(204, homeTeamId = 23, awayTeamId = 2, homeScore = 3, awayScore = 0),
            createMatch(205, homeTeamId = 2, awayTeamId = 24, homeScore = 0, awayScore = 2)
        )

        val h2hMatches = listOf(
            createMatch(301, homeTeamId = 1, awayTeamId = 2, homeScore = 2, awayScore = 1),
            createMatch(302, homeTeamId = 2, awayTeamId = 1, homeScore = 0, awayScore = 0),
            createMatch(303, homeTeamId = 1, awayTeamId = 2, homeScore = 1, awayScore = 0)
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
            isNeutralVenue = false
        )

        val result = useCase(context)

        assertEquals(999L, result.matchId)
        assertEquals("Weighted Scoring", result.algorithmName)

        assertTrue(result.homeWinProb in 0.0..1.0)
        assertTrue(result.drawProb in 0.0..1.0)
        assertTrue(result.awayWinProb in 0.0..1.0)

        val sum = result.homeWinProb + result.drawProb + result.awayWinProb
        assertEquals(1.0, sum, 1e-4)

        // With stronger Elo, Form, Odds, H2H, and HomeAdvantage, Home Win should be predicted
        assertTrue(result.homeWinProb > result.awayWinProb)
        assertTrue(result.homeWinProb > result.drawProb)
        assertEquals("HOME_WIN", result.predictedOutcome)
        assertEquals(result.homeWinProb, result.confidenceScore, 1e-6)
    }

    @Test
    fun `missing odds assigns weight 0 to odds and prediction still succeeds with remaining signals`() {
        val context = MatchPredictionContext(
            matchId = 888L,
            homeTeamId = 1,
            awayTeamId = 2,
            homeElo = 1600.0,
            awayElo = 1600.0,
            latestOdds = null, // Missing odds
            isNeutralVenue = false
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
            homeAdvantageWeight = 0.0
        )
    }

    @Test
    fun `neutral venue modifies home advantage signal symmetry`() {
        val symmetricConfig = PredictionWeightConfig(
            h2hPriorHome = 0.37,
            h2hPriorDraw = 0.26,
            h2hPriorAway = 0.37
        )

        val standardContext = MatchPredictionContext(
            matchId = 111L,
            homeTeamId = 1,
            awayTeamId = 2,
            homeElo = 1500.0,
            awayElo = 1500.0,
            homeMeanScored = 1.2,
            homeMeanConceded = 1.2,
            awayMeanScored = 1.2,
            awayMeanConceded = 1.2,
            homeWins = 10,
            draws = 5,
            awayWins = 10,
            isNeutralVenue = false
        )

        val neutralContext = MatchPredictionContext(
            matchId = 111L,
            homeTeamId = 1,
            awayTeamId = 2,
            homeElo = 1500.0,
            awayElo = 1500.0,
            homeMeanScored = 1.2,
            homeMeanConceded = 1.2,
            awayMeanScored = 1.2,
            awayMeanConceded = 1.2,
            homeWins = 10,
            draws = 5,
            awayWins = 10,
            isNeutralVenue = true
        )

        val standardResult = useCase(standardContext, symmetricConfig)
        val neutralResult = useCase(neutralContext, symmetricConfig)

        // On home ground with equal features, home win probability is higher than away due to HomeAdvantage signal
        assertTrue(standardResult.homeWinProb > standardResult.awayWinProb)
        assertTrue(standardResult.homeWinProb > neutralResult.homeWinProb)

        // On neutral venue with identical stats and symmetric prior, home and away win probabilities are symmetric
        assertEquals(neutralResult.homeWinProb, neutralResult.awayWinProb, 1e-4)
    }
}
