package dev.anhquocs.truelab.core.domain.prediction.transformer

import dev.anhquocs.truelab.core.domain.odds.model.OddsRecordItem
import dev.anhquocs.truelab.core.domain.prediction.model.CandidateCConfig
import dev.anhquocs.truelab.core.domain.prediction.model.PredictionWeightConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AsianHandicapSignalTransformerTest {

    private val config = PredictionWeightConfig.DEFAULT
    private val candidateCConfig = CandidateCConfig.DEFAULT_CALIBRATED

    @Test
    fun `valid AH odds correctly transformed to Signal3Way with sum approximately 1_0`() {
        val ahItem = OddsRecordItem(
            companyId = 1,
            companyName = "Pinnacle",
            oddsType = "asia",
            handicap = 0.50, // Home gives 0.5 ball advantage
            homeWin = 0.90,  // HK odds 0.90 -> Decimal 1.90
            awayWin = 0.95,  // HK odds 0.95 -> Decimal 1.95
            changeTime = 1700000000L
        )

        val signal = AsianHandicapSignalTransformer.transform(ahItem, config, candidateCConfig)

        assertEquals("Asian Handicap", signal.name)
        assertEquals(candidateCConfig.ahWeight, signal.weight, 1e-6)

        assertTrue(signal.homeProb in 0.0..1.0)
        assertTrue(signal.drawProb in 0.0..1.0)
        assertTrue(signal.awayProb in 0.0..1.0)

        val sum = signal.homeProb + signal.drawProb + signal.awayProb
        assertEquals(1.0, sum, 1e-4)

        // Home gives 0.5 handicap -> Home is expected to have higher win probability than Away
        assertTrue(signal.homeProb > signal.awayProb)
    }

    @Test
    fun `negative AH handicap correctly reflects Away favorite`() {
        val ahItem = OddsRecordItem(
            companyId = 1,
            companyName = "Pinnacle",
            oddsType = "asia",
            handicap = -0.75, // Away gives 0.75 ball
            homeWin = 0.92,
            awayWin = 0.92,
            changeTime = 1700000000L
        )

        val signal = AsianHandicapSignalTransformer.transform(ahItem, config, candidateCConfig)

        assertEquals("Asian Handicap", signal.name)
        assertTrue(signal.awayProb > signal.homeProb)
        val sum = signal.homeProb + signal.drawProb + signal.awayProb
        assertEquals(1.0, sum, 1e-4)
    }

    @Test
    fun `null AH item falls back to weight 0 and valid sum 1_0 without crash`() {
        val signal = AsianHandicapSignalTransformer.transform(null, config, candidateCConfig)

        assertEquals("Asian Handicap (Unavailable)", signal.name)
        assertEquals(0.0, signal.weight, 1e-6)

        val sum = signal.homeProb + signal.drawProb + signal.awayProb
        assertEquals(1.0, sum, 1e-4)
    }

    @Test
    fun `invalid price or handicap falls back gracefully`() {
        val invalidItem = OddsRecordItem(
            companyId = 1,
            companyName = "Test",
            oddsType = "asia",
            handicap = null,
            homeWin = 0.0,
            awayWin = -0.5,
            changeTime = 1700000000L
        )

        val signal = AsianHandicapSignalTransformer.transform(invalidItem, config, candidateCConfig)

        assertEquals("Asian Handicap (Unavailable)", signal.name)
        assertEquals(0.0, signal.weight, 1e-6)
    }

    @Test
    fun `disabled Candidate C config returns weight 0`() {
        val ahItem = OddsRecordItem(
            companyId = 1,
            companyName = "Pinnacle",
            oddsType = "asia",
            handicap = 0.25,
            homeWin = 0.90,
            awayWin = 0.90,
            changeTime = 1700000000L
        )

        val signal = AsianHandicapSignalTransformer.transform(ahItem, config, CandidateCConfig.DISABLED)

        assertEquals(0.0, signal.weight, 1e-6)
    }
}
