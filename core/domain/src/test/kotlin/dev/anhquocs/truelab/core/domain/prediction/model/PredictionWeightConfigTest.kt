package dev.anhquocs.truelab.core.domain.prediction.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PredictionWeightConfigTest {

    @Test
    fun `default config matches FR-14 weights exactly and sums to 1_0`() {
        val config = PredictionWeightConfig.DEFAULT

        assertEquals(0.25, config.formWeight, 1e-6)
        assertEquals(0.20, config.eloWeight, 1e-6)
        assertEquals(0.20, config.oddsWeight, 1e-6)
        assertEquals(0.15, config.goalsWeight, 1e-6)
        assertEquals(0.10, config.h2hWeight, 1e-6)
        assertEquals(0.10, config.homeAdvantageWeight, 1e-6)

        val totalWeight = config.formWeight + config.eloWeight + config.oddsWeight +
            config.goalsWeight + config.h2hWeight + config.homeAdvantageWeight
        assertEquals(1.00, totalWeight, 1e-6)
    }

    @Test
    fun `default config heuristic parameters match modeling spec`() {
        val config = PredictionWeightConfig.DEFAULT

        assertEquals(0.26, config.baselineDrawProb, 1e-6)
        assertEquals(0.10, config.formSmoothingEpsilon, 1e-6)
        assertEquals(3.0, config.h2hPriorK, 1e-6)
        assertEquals(0.45, config.h2hPriorHome, 1e-6)
        assertEquals(0.27, config.h2hPriorDraw, 1e-6)
        assertEquals(0.28, config.h2hPriorAway, 1e-6)
        assertEquals(0.46, config.homeAdvantageProbHome, 1e-6)
        assertEquals(0.26, config.homeAdvantageProbDraw, 1e-6)
        assertEquals(0.28, config.homeAdvantageProbAway, 1e-6)
        assertEquals(0.15, config.goalsSensitivity, 1e-6)
        assertEquals(0.05, config.goalsMinProbHome, 1e-6)
        assertEquals(0.69, config.goalsMaxProbHome, 1e-6)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative weight throws IllegalArgumentException`() {
        PredictionWeightConfig(formWeight = -0.1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zero total weight throws IllegalArgumentException`() {
        PredictionWeightConfig(
            formWeight = 0.0,
            eloWeight = 0.0,
            oddsWeight = 0.0,
            goalsWeight = 0.0,
            h2hWeight = 0.0,
            homeAdvantageWeight = 0.0
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid baselineDrawProb outside 0 to 1 throws IllegalArgumentException`() {
        PredictionWeightConfig(baselineDrawProb = 1.5)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid H2H prior sum throws IllegalArgumentException`() {
        PredictionWeightConfig(h2hPriorHome = 0.5, h2hPriorDraw = 0.5, h2hPriorAway = 0.5)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid HomeAdvantage prior sum throws IllegalArgumentException`() {
        PredictionWeightConfig(homeAdvantageProbHome = 0.9, homeAdvantageProbDraw = 0.9, homeAdvantageProbAway = 0.9)
    }
}
