package dev.anhquocs.truelab.core.domain.prediction.transformer

import dev.anhquocs.truelab.core.algorithm.evaluation.FormScore
import dev.anhquocs.truelab.core.domain.prediction.model.DrawModelingStrategy
import dev.anhquocs.truelab.core.domain.prediction.model.DrawStrategyConfig
import dev.anhquocs.truelab.core.domain.prediction.model.PredictionWeightConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormSignalTransformerTest {

    private val config = PredictionWeightConfig.DEFAULT
    private val dynamicStrategyConfig = DrawStrategyConfig(strategy = DrawModelingStrategy.DYNAMIC_DRAW_PRIOR)

    private fun createFormScore(score: Double): FormScore {
        return FormScore(
            score = score,
            rawScore = score,
            matchesCount = 5,
            wins = 3,
            draws = 1,
            losses = 1,
            totalPoints = 10.0,
            maxPoints = 15.0
        )
    }

    @Test
    fun `equal form scores yields symmetric probabilities with baseline draw`() {
        val formH = createFormScore(75.0)
        val formA = createFormScore(75.0)

        val signal = FormSignalTransformer.transform(formH, formA, config)

        assertEquals("Form", signal.name)
        assertEquals(config.formWeight, signal.weight, 1e-6)
        assertEquals(0.26, signal.drawProb, 1e-6)
        assertEquals(0.37, signal.homeProb, 1e-6)
        assertEquals(0.37, signal.awayProb, 1e-6)

        val sum = signal.homeProb + signal.drawProb + signal.awayProb
        assertEquals(1.0, sum, 1e-4)
    }

    @Test
    fun `extreme form difference 100 vs 0 avoids zero probability due to smoothing epsilon`() {
        val formH = createFormScore(100.0)
        val formA = createFormScore(0.0)

        val signal = FormSignalTransformer.transform(formH, formA, config)

        assertTrue(signal.homeProb > 0.60)
        assertTrue(signal.awayProb > 0.05) // Non-zero due to epsilon = 0.10
        assertEquals(0.26, signal.drawProb, 1e-6)

        val sum = signal.homeProb + signal.drawProb + signal.awayProb
        assertEquals(1.0, sum, 1e-4)
    }

    @Test
    fun `both form scores 0 handles division by zero gracefully`() {
        val formH = createFormScore(0.0)
        val formA = createFormScore(0.0)

        val signal = FormSignalTransformer.transform(formH, formA, config)

        assertEquals(0.37, signal.homeProb, 1e-6)
        assertEquals(0.26, signal.drawProb, 1e-6)
        assertEquals(0.37, signal.awayProb, 1e-6)
        assertEquals(1.0, signal.homeProb + signal.drawProb + signal.awayProb, 1e-4)
    }

    @Test
    fun `null form scores fall back to default neutral form safely`() {
        val signal = FormSignalTransformer.transform(null, null, config)

        assertEquals(0.37, signal.homeProb, 1e-6)
        assertEquals(0.26, signal.drawProb, 1e-6)
        assertEquals(0.37, signal.awayProb, 1e-6)
        assertEquals(1.0, signal.homeProb + signal.drawProb + signal.awayProb, 1e-4)
    }

    @Test
    fun `equal form scores with DYNAMIC_DRAW_PRIOR produces maxDrawProb 0_36 and sum 1_0`() {
        val formH = createFormScore(60.0)
        val formA = createFormScore(60.0)

        val signal = FormSignalTransformer.transform(
            homeForm = formH,
            awayForm = formA,
            config = config,
            drawStrategyConfig = dynamicStrategyConfig
        )

        assertEquals(0.38, signal.drawProb, 1e-6)
        assertEquals(0.31, signal.homeProb, 1e-2)
        assertEquals(0.31, signal.awayProb, 1e-2)
        assertTrue(signal.drawProb > signal.homeProb)

        val sum = signal.homeProb + signal.drawProb + signal.awayProb
        assertEquals(1.0, sum, 1e-6)
    }

    @Test
    fun `large form difference with DYNAMIC_DRAW_PRIOR reduces drawProb towards minDrawProb`() {
        val formH = createFormScore(90.0)
        val formA = createFormScore(10.0) // delta = 0.80 -> exp(-0.8^2 / (2 * 0.25^2)) = exp(-0.64 / 0.125) = exp(-5.12) approx 0.006

        val signal = FormSignalTransformer.transform(
            homeForm = formH,
            awayForm = formA,
            config = config,
            drawStrategyConfig = dynamicStrategyConfig
        )

        assertTrue("Draw prob (${signal.drawProb}) must be lower than 0.15 for large form difference", signal.drawProb < 0.15)
        assertTrue(signal.homeProb > 0.70)
        val sum = signal.homeProb + signal.drawProb + signal.awayProb
        assertEquals(1.0, sum, 1e-6)
    }

    @Test
    fun `missing form scores with DYNAMIC_DRAW_PRIOR safely falls back to neutral with maxDrawProb`() {
        val signal = FormSignalTransformer.transform(
            homeForm = null,
            awayForm = null,
            config = config,
            drawStrategyConfig = dynamicStrategyConfig
        )

        assertEquals(0.38, signal.drawProb, 1e-6)
        assertEquals(0.31, signal.homeProb, 1e-2)
        assertEquals(0.31, signal.awayProb, 1e-2)
        assertEquals(1.0, signal.homeProb + signal.drawProb + signal.awayProb, 1e-6)
    }
}
