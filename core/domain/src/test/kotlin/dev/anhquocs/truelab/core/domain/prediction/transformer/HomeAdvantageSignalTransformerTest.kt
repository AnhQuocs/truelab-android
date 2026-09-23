package dev.anhquocs.truelab.core.domain.prediction.transformer

import dev.anhquocs.truelab.core.domain.prediction.model.PredictionWeightConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeAdvantageSignalTransformerTest {

    private val config = PredictionWeightConfig.DEFAULT

    @Test
    fun `standard home advantage produces baseline probabilities and sums to 1_0`() {
        val signal = HomeAdvantageSignalTransformer.transform(isNeutralVenue = false, config = config)

        assertEquals("Home Advantage", signal.name)
        assertEquals(config.homeAdvantageWeight, signal.weight, 1e-6)
        assertEquals(0.46, signal.homeProb, 1e-6)
        assertEquals(0.26, signal.drawProb, 1e-6)
        assertEquals(0.28, signal.awayProb, 1e-6)

        val sum = signal.homeProb + signal.drawProb + signal.awayProb
        assertEquals(1.0, sum, 1e-4)
    }

    @Test
    fun `neutral venue splits non-draw probability equally and sums to 1_0`() {
        val signal = HomeAdvantageSignalTransformer.transform(isNeutralVenue = true, config = config)

        assertEquals("Neutral Venue", signal.name)
        assertEquals(config.homeAdvantageWeight, signal.weight, 1e-6)
        assertEquals(0.37, signal.homeProb, 1e-6)
        assertEquals(0.26, signal.drawProb, 1e-6)
        assertEquals(0.37, signal.awayProb, 1e-6)

        val sum = signal.homeProb + signal.drawProb + signal.awayProb
        assertEquals(1.0, sum, 1e-4)
    }
}
