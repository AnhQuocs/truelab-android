package dev.anhquocs.truelab.core.domain.prediction.policy

import dev.anhquocs.truelab.core.algorithm.prediction.OutcomeProbabilities
import dev.anhquocs.truelab.core.algorithm.prediction.PredictedOutcome
import dev.anhquocs.truelab.core.domain.prediction.model.DrawMarginConfig
import dev.anhquocs.truelab.core.domain.prediction.model.DrawModelingStrategy
import dev.anhquocs.truelab.core.domain.prediction.model.DrawStrategyConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Unit test cho [DrawDecisionPolicy] (Phase B2 - Candidate A: Decision Margin / Relative Threshold).
 *
 * Kiểm tra toàn diện mọi nhánh điều kiện và ranh giới (Boundary conditions):
 * 1. Strong Home -> HOME_WIN
 * 2. Strong Away -> AWAY_WIN
 * 3. Balanced + Draw threshold satisfied -> DRAW
 * 4. Balanced but Draw prob too low -> HOME_WIN (argmax)
 * 5. Margin exactly delta -> argmax (không thỏa < delta)
 * 6. Draw prob exactly theta -> DRAW (thỏa >= theta)
 * 7. Both conditions fail -> argmax
 * 8. Probability preservation -> Không thay đổi xác suất thô
 * 9. Strategy selection -> BASELINE giữ nguyên argmax, DECISION_MARGIN áp dụng policy
 * 10. DYNAMIC_DRAW_PRIOR placeholder -> fail-fast UnsupportedOperationException
 */
class DrawDecisionPolicyTest {

    private val defaultConfig = DrawMarginConfig(
        deltaMargin = 0.04,
        thetaMinProb = 0.265
    )

    private fun createProbabilities(
        homeProb: Double,
        drawProb: Double,
        awayProb: Double
    ): OutcomeProbabilities {
        val maxProb = maxOf(homeProb, drawProb, awayProb)
        val outcome = when {
            homeProb == maxProb -> PredictedOutcome.HOME_WIN
            awayProb == maxProb -> PredictedOutcome.AWAY_WIN
            else -> PredictedOutcome.DRAW
        }
        return OutcomeProbabilities(
            homeWinProb = homeProb,
            drawProb = drawProb,
            awayWinProb = awayProb,
            predictedOutcome = outcome,
            confidenceScore = maxProb
        )
    }

    @Test
    fun `Test 1 - strong Home (|PH - PA| greater than delta) retains baseline HOME_WIN`() {
        // PH = 0.50, PD = 0.27, PA = 0.23 -> |PH - PA| = 0.27 > 0.04
        val probs = createProbabilities(0.50, 0.27, 0.23)

        val outcome = DrawDecisionPolicy.applyDecisionMargin(probs, defaultConfig)

        assertEquals("HOME_WIN", outcome)
    }

    @Test
    fun `Test 2 - strong Away (|PA - PH| greater than delta) retains baseline AWAY_WIN`() {
        // PH = 0.20, PD = 0.27, PA = 0.53 -> |PH - PA| = 0.33 > 0.04
        val probs = createProbabilities(0.20, 0.27, 0.53)

        val outcome = DrawDecisionPolicy.applyDecisionMargin(probs, defaultConfig)

        assertEquals("AWAY_WIN", outcome)
    }

    @Test
    fun `Test 3 - balanced match and Draw threshold satisfied predicts DRAW`() {
        // PH = 0.370, PD = 0.270, PA = 0.360 -> |PH - PA| = 0.010 < 0.04, PD = 0.270 >= 0.265
        val probs = createProbabilities(0.370, 0.270, 0.360)

        // Baseline argmax would predict HOME_WIN
        assertEquals(PredictedOutcome.HOME_WIN, probs.predictedOutcome)

        val outcome = DrawDecisionPolicy.applyDecisionMargin(probs, defaultConfig)

        // Decision margin correctly predicts DRAW
        assertEquals("DRAW", outcome)
    }

    @Test
    fun `Test 4 - balanced match but Draw probability too low retains baseline argmax`() {
        // PH = 0.385, PD = 0.240, PA = 0.375 -> |PH - PA| = 0.010 < 0.04, but PD = 0.240 < 0.265
        val probs = createProbabilities(0.385, 0.240, 0.375)

        val outcome = DrawDecisionPolicy.applyDecisionMargin(probs, defaultConfig)

        assertEquals("HOME_WIN", outcome)
    }

    @Test
    fun `Test 5 - margin exactly equal to delta margin does not trigger draw`() {
        // Delta = 0.040. PH = 0.385, PA = 0.345 -> |PH - PA| = 0.040 (exactly equal delta)
        // PD = 0.270 >= 0.265
        val probs = createProbabilities(0.385, 0.270, 0.345)

        val outcome = DrawDecisionPolicy.applyDecisionMargin(probs, defaultConfig)

        // Strict inequality |PH - PA| < delta means exact delta does NOT trigger DRAW
        assertEquals("HOME_WIN", outcome)
    }

    @Test
    fun `Test 6 - draw probability exactly equal to theta threshold triggers draw when balanced`() {
        // Theta = 0.265. PD = 0.265 (exactly equal theta). PH = 0.370, PA = 0.365 -> |PH - PA| = 0.005 < 0.04
        val probs = createProbabilities(0.370, 0.265, 0.365)

        val outcome = DrawDecisionPolicy.applyDecisionMargin(probs, defaultConfig)

        // Inequality PD >= theta means exact theta DOES trigger DRAW
        assertEquals("DRAW", outcome)
    }

    @Test
    fun `Test 7 - both margin and draw threshold fail retains baseline argmax`() {
        // PH = 0.48, PD = 0.22, PA = 0.30 -> |PH - PA| = 0.18 > 0.04 and PD = 0.22 < 0.265
        val probs = createProbabilities(0.48, 0.22, 0.30)

        val outcome = DrawDecisionPolicy.applyDecisionMargin(probs, defaultConfig)

        assertEquals("HOME_WIN", outcome)
    }

    @Test
    fun `Test 8 - probability preservation confirms raw probabilities are never altered`() {
        val probs = createProbabilities(0.370, 0.270, 0.360)

        val outcome = DrawDecisionPolicy.resolvePredictedOutcome(
            probabilities = probs,
            strategyConfig = DrawStrategyConfig(strategy = DrawModelingStrategy.DECISION_MARGIN)
        )

        assertEquals("DRAW", outcome)
        // Ensure probability values are strictly intact
        assertEquals(0.370, probs.homeWinProb, 1e-6)
        assertEquals(0.270, probs.drawProb, 1e-6)
        assertEquals(0.360, probs.awayWinProb, 1e-6)
    }

    @Test
    fun `Test 9 - strategy selection correctly differentiates BASELINE vs DECISION_MARGIN`() {
        val probs = createProbabilities(0.370, 0.270, 0.360)

        val baselineOutcome = DrawDecisionPolicy.resolvePredictedOutcome(
            probabilities = probs,
            strategyConfig = DrawStrategyConfig(strategy = DrawModelingStrategy.BASELINE)
        )
        val candidateAOutcome = DrawDecisionPolicy.resolvePredictedOutcome(
            probabilities = probs,
            strategyConfig = DrawStrategyConfig(strategy = DrawModelingStrategy.DECISION_MARGIN)
        )

        assertEquals("HOME_WIN", baselineOutcome)
        assertEquals("DRAW", candidateAOutcome)
    }

    @Test
    fun `Test 10 - DYNAMIC_DRAW_PRIOR resolves outcome via natural argmax`() {
        val drawDominantProbs = createProbabilities(0.310, 0.380, 0.310)
        val homeDominantProbs = createProbabilities(0.550, 0.250, 0.200)

        val drawOutcome = DrawDecisionPolicy.resolvePredictedOutcome(
            probabilities = drawDominantProbs,
            strategyConfig = DrawStrategyConfig(strategy = DrawModelingStrategy.DYNAMIC_DRAW_PRIOR)
        )
        val homeOutcome = DrawDecisionPolicy.resolvePredictedOutcome(
            probabilities = homeDominantProbs,
            strategyConfig = DrawStrategyConfig(strategy = DrawModelingStrategy.DYNAMIC_DRAW_PRIOR)
        )

        assertEquals("DRAW", drawOutcome)
        assertEquals("HOME_WIN", homeOutcome)
    }
}
