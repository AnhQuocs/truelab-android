package dev.anhquocs.truelab.core.domain.prediction.transformer

import dev.anhquocs.truelab.core.domain.prediction.model.PredictionWeightConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EloSignalTransformerTest {

    private val config = PredictionWeightConfig.DEFAULT

    @Test
    fun `equal Elo ratings yields symmetric home and away probabilities and baseline draw`() {
        val signal = EloSignalTransformer.transform(homeElo = 1500.0, awayElo = 1500.0, config = config)

        assertEquals("Elo", signal.name)
        assertEquals(config.eloWeight, signal.weight, 1e-6)
        assertEquals(0.26, signal.drawProb, 1e-6)
        assertEquals(0.37, signal.homeProb, 1e-6)
        assertEquals(0.37, signal.awayProb, 1e-6)

        val sum = signal.homeProb + signal.drawProb + signal.awayProb
        assertEquals(1.0, sum, 1e-4)
    }

    @Test
    fun `higher home Elo produces higher home probability while preserving draw and sum 1_0`() {
        val signal = EloSignalTransformer.transform(homeElo = 1800.0, awayElo = 1400.0, config = config)

        assertTrue(signal.homeProb > signal.awayProb)
        assertEquals(0.26, signal.drawProb, 1e-6)
        assertTrue(signal.homeProb in 0.0..1.0)
        assertTrue(signal.awayProb in 0.0..1.0)

        val sum = signal.homeProb + signal.drawProb + signal.awayProb
        assertEquals(1.0, sum, 1e-4)
    }

    @Test
    fun `higher away Elo produces higher away probability while preserving draw and sum 1_0`() {
        val signal = EloSignalTransformer.transform(homeElo = 1300.0, awayElo = 1700.0, config = config)

        assertTrue(signal.awayProb > signal.homeProb)
        assertEquals(0.26, signal.drawProb, 1e-6)

        val sum = signal.homeProb + signal.drawProb + signal.awayProb
        assertEquals(1.0, sum, 1e-4)
    }

    @Test
    fun `null or invalid Elo values fallback safely to default 1500_0`() {
        val signal = EloSignalTransformer.transform(homeElo = null, awayElo = -100.0, config = config)

        assertEquals(0.37, signal.homeProb, 1e-6)
        assertEquals(0.26, signal.drawProb, 1e-6)
        assertEquals(0.37, signal.awayProb, 1e-6)
        assertEquals(1.0, signal.homeProb + signal.drawProb + signal.awayProb, 1e-4)
    }
}
