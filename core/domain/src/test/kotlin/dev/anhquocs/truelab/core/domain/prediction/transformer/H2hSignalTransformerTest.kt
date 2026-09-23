package dev.anhquocs.truelab.core.domain.prediction.transformer

import dev.anhquocs.truelab.core.domain.prediction.model.PredictionWeightConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class H2hSignalTransformerTest {

    private val config = PredictionWeightConfig.DEFAULT

    @Test
    fun `N equals 0 matches baseline prior distribution exactly`() {
        val signal = H2hSignalTransformer.transform(homeWins = 0, draws = 0, awayWins = 0, config = config)

        assertEquals("H2H", signal.name)
        assertEquals(config.h2hWeight, signal.weight, 1e-6)
        assertEquals(0.45, signal.homeProb, 1e-6)
        assertEquals(0.27, signal.drawProb, 1e-6)
        assertEquals(0.28, signal.awayProb, 1e-6)

        val sum = signal.homeProb + signal.drawProb + signal.awayProb
        assertEquals(1.0, sum, 1e-4)
    }

    @Test
    fun `N equals 1 with 1 home win applies Laplace smoothing smoothly`() {
        val signal = H2hSignalTransformer.transform(homeWins = 1, draws = 0, awayWins = 0, config = config)

        // (1 + 3 * 0.45) / (1 + 3) = 2.35 / 4 = 0.5875
        assertEquals(0.5875, signal.homeProb, 1e-6)
        // (0 + 3 * 0.27) / 4 = 0.81 / 4 = 0.2025
        assertEquals(0.2025, signal.drawProb, 1e-6)
        // (0 + 3 * 0.28) / 4 = 0.84 / 4 = 0.2100
        assertEquals(0.2100, signal.awayProb, 1e-6)

        val sum = signal.homeProb + signal.drawProb + signal.awayProb
        assertEquals(1.0, sum, 1e-4)
    }

    @Test
    fun `large N dominates prior and sums to 1_0`() {
        val signal = H2hSignalTransformer.transform(homeWins = 10, draws = 2, awayWins = 1, config = config)

        assertTrue(signal.homeProb > 0.70)
        val sum = signal.homeProb + signal.drawProb + signal.awayProb
        assertEquals(1.0, sum, 1e-4)
    }

    @Test
    fun `negative counts are sanitized to 0 safely`() {
        val signal = H2hSignalTransformer.transform(homeWins = -2, draws = -1, awayWins = -5, config = config)

        assertEquals(0.45, signal.homeProb, 1e-6)
        assertEquals(0.27, signal.drawProb, 1e-6)
        assertEquals(0.28, signal.awayProb, 1e-6)
        assertEquals(1.0, signal.homeProb + signal.drawProb + signal.awayProb, 1e-4)
    }
}
