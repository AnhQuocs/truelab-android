package dev.anhquocs.truelab.core.domain.prediction.transformer

import dev.anhquocs.truelab.core.domain.odds.model.OddsRecordItem
import dev.anhquocs.truelab.core.domain.prediction.model.CandidateCConfig
import dev.anhquocs.truelab.core.domain.prediction.model.PredictionWeightConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OverUnderSignalTransformerTest {

    private val config = PredictionWeightConfig.DEFAULT
    private val candidateCConfig = CandidateCConfig.DEFAULT_CALIBRATED

    @Test
    fun `valid OU odds correctly transformed to Signal3Way with sum approximately 1_0`() {
        val ouItem = OddsRecordItem(
            companyId = 1,
            companyName = "Pinnacle",
            oddsType = "bs",
            handicap = 2.50, // Total goals line
            over = 0.90,     // HK odds 0.90 -> Decimal 1.90
            under = 0.90,    // HK odds 0.90 -> Decimal 1.90
            changeTime = 1700000000L
        )

        val signal = OverUnderSignalTransformer.transform(ouItem, config, candidateCConfig)

        assertEquals("Over/Under", signal.name)
        assertEquals(candidateCConfig.ouWeight, signal.weight, 1e-6)

        assertTrue(signal.homeProb in 0.0..1.0)
        assertTrue(signal.drawProb in 0.0..1.0)
        assertTrue(signal.awayProb in 0.0..1.0)

        val sum = signal.homeProb + signal.drawProb + signal.awayProb
        assertEquals(1.0, sum, 1e-4)

        // Equal distribution for Home and Away in O/U
        assertEquals(signal.homeProb, signal.awayProb, 1e-6)
    }

    @Test
    fun `low goal line increases draw probability modulation`() {
        val lowGoalItem = OddsRecordItem(
            companyId = 1,
            companyName = "Pinnacle",
            oddsType = "bs",
            handicap = 1.75, // Very low expected goals
            over = 0.90,
            under = 0.90,
            changeTime = 1700000000L
        )

        val highGoalItem = OddsRecordItem(
            companyId = 1,
            companyName = "Pinnacle",
            oddsType = "bs",
            handicap = 3.50, // Very high expected goals
            over = 0.90,
            under = 0.90,
            changeTime = 1700000000L
        )

        val lowSignal = OverUnderSignalTransformer.transform(lowGoalItem, config, candidateCConfig)
        val highSignal = OverUnderSignalTransformer.transform(highGoalItem, config, candidateCConfig)

        assertTrue("Low goal line should have higher draw probability than high goal line", lowSignal.drawProb > highSignal.drawProb)
    }

    @Test
    fun `null OU item falls back to weight 0 and valid sum 1_0 without crash`() {
        val signal = OverUnderSignalTransformer.transform(null, config, candidateCConfig)

        assertEquals("Over/Under (Unavailable)", signal.name)
        assertEquals(0.0, signal.weight, 1e-6)

        val sum = signal.homeProb + signal.drawProb + signal.awayProb
        assertEquals(1.0, sum, 1e-4)
    }

    @Test
    fun `invalid zero or negative handicap falls back gracefully`() {
        val invalidItem = OddsRecordItem(
            companyId = 1,
            companyName = "Test",
            oddsType = "bs",
            handicap = 0.0, // Invalid physical goal line
            over = 0.90,
            under = 0.90,
            changeTime = 1700000000L
        )

        val signal = OverUnderSignalTransformer.transform(invalidItem, config, candidateCConfig)

        assertEquals("Over/Under (Unavailable)", signal.name)
        assertEquals(0.0, signal.weight, 1e-6)
    }
}
