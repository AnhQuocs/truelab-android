package dev.anhquocs.truelab.core.domain.prediction.transformer

import dev.anhquocs.truelab.core.domain.prediction.model.PredictionWeightConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalsSignalTransformerTest {

    private val config = PredictionWeightConfig.DEFAULT

    @Test
    fun `equal expected goals yields symmetric probabilities with baseline draw and sum 1_0`() {
        val signal = GoalsSignalTransformer.transform(
            homeMeanScored = 1.5,
            homeMeanConceded = 1.0,
            awayMeanScored = 1.5,
            awayMeanConceded = 1.0,
            config = config
        )

        assertEquals("Goals", signal.name)
        assertEquals(config.goalsWeight, signal.weight, 1e-6)
        assertEquals(0.26, signal.drawProb, 1e-6)
        assertEquals(0.37, signal.homeProb, 1e-6)
        assertEquals(0.37, signal.awayProb, 1e-6)

        val sum = signal.homeProb + signal.drawProb + signal.awayProb
        assertEquals(1.0, sum, 1e-4)
    }

    @Test
    fun `higher home expected goal difference increases home probability smoothly`() {
        val signal = GoalsSignalTransformer.transform(
            homeMeanScored = 2.5,
            homeMeanConceded = 0.5,
            awayMeanScored = 0.8,
            awayMeanConceded = 2.0,
            config = config
        )

        assertTrue(signal.homeProb > signal.awayProb)
        assertEquals(0.26, signal.drawProb, 1e-6)
        val sum = signal.homeProb + signal.drawProb + signal.awayProb
        assertEquals(1.0, sum, 1e-4)
    }

    @Test
    fun `extreme positive deltaLambda clamps home probability to max bound 0_69 and sums to 1_0`() {
        val signal = GoalsSignalTransformer.transform(
            homeMeanScored = 10.0,
            homeMeanConceded = 0.0,
            awayMeanScored = 0.0,
            awayMeanConceded = 10.0,
            config = config
        )

        assertEquals(0.69, signal.homeProb, 1e-6)
        assertEquals(0.26, signal.drawProb, 1e-6)
        assertEquals(0.05, signal.awayProb, 1e-6)

        val sum = signal.homeProb + signal.drawProb + signal.awayProb
        assertEquals(1.0, sum, 1e-4)
    }

    @Test
    fun `extreme negative deltaLambda clamps home probability to min bound 0_05 and sums to 1_0`() {
        val signal = GoalsSignalTransformer.transform(
            homeMeanScored = 0.0,
            homeMeanConceded = 10.0,
            awayMeanScored = 10.0,
            awayMeanConceded = 0.0,
            config = config
        )

        assertEquals(0.05, signal.homeProb, 1e-6)
        assertEquals(0.26, signal.drawProb, 1e-6)
        assertEquals(0.69, signal.awayProb, 1e-6)

        val sum = signal.homeProb + signal.drawProb + signal.awayProb
        assertEquals(1.0, sum, 1e-4)
    }

    @Test
    fun `null inputs fallback to reasonable defaults safely`() {
        val signal = GoalsSignalTransformer.transform(
            homeMeanScored = null,
            homeMeanConceded = null,
            awayMeanScored = null,
            awayMeanConceded = null,
            config = config
        )

        assertTrue(signal.homeProb in 0.0..1.0)
        assertTrue(signal.awayProb in 0.0..1.0)
        assertEquals(0.26, signal.drawProb, 1e-6)

        val sum = signal.homeProb + signal.drawProb + signal.awayProb
        assertEquals(1.0, sum, 1e-4)
    }
}
