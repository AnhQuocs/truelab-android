package dev.anhquocs.truelab.core.domain.prediction.transformer

import dev.anhquocs.truelab.core.domain.odds.model.OddsRecordItem
import dev.anhquocs.truelab.core.domain.prediction.model.PredictionWeightConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OddsSignalTransformerTest {

    private val config = PredictionWeightConfig.DEFAULT

    @Test
    fun `valid odds transformed to Signal3Way with sum approximately 1_0 and in range 0 to 1`() {
        val oddsItem = OddsRecordItem(
            companyId = 1,
            companyName = "Bet365",
            oddsType = "1X2",
            handicap = null,
            over = null,
            under = null,
            homeWin = 2.10,
            draw = 3.40,
            awayWin = 3.60,
            changeTime = 1700000000L,
            marketPhase = "LIVE"
        )

        val signal = OddsSignalTransformer.transform(oddsItem, config)

        assertEquals("Odds", signal.name)
        assertEquals(config.oddsWeight, signal.weight, 1e-6)

        assertTrue(signal.homeProb in 0.0..1.0)
        assertTrue(signal.drawProb in 0.0..1.0)
        assertTrue(signal.awayProb in 0.0..1.0)

        val sum = signal.homeProb + signal.drawProb + signal.awayProb
        assertEquals(1.0, sum, 1e-4)

        // Verifying expected implied probability direction (2.10 is favorite -> highest prob)
        assertTrue(signal.homeProb > signal.drawProb)
        assertTrue(signal.homeProb > signal.awayProb)
    }

    @Test
    fun `null odds item falls back to weight 0 and valid sum 1_0 without crash`() {
        val signal = OddsSignalTransformer.transform(null, config)

        assertEquals("Odds (Unavailable)", signal.name)
        assertEquals(0.0, signal.weight, 1e-6)

        val sum = signal.homeProb + signal.drawProb + signal.awayProb
        assertEquals(1.0, sum, 1e-4)
        assertTrue(signal.homeProb in 0.0..1.0)
        assertTrue(signal.drawProb in 0.0..1.0)
        assertTrue(signal.awayProb in 0.0..1.0)
    }

    @Test
    fun `invalid negative or zero odds fall back gracefully with weight 0`() {
        val invalidItem = OddsRecordItem(
            companyId = 1,
            companyName = "Test",
            oddsType = "1X2",
            handicap = null,
            over = null,
            under = null,
            homeWin = -1.5,
            draw = 0.0,
            awayWin = 2.0,
            changeTime = 1700000000L,
            marketPhase = null
        )

        val signal = OddsSignalTransformer.transform(invalidItem, config)

        assertEquals("Odds (Unavailable)", signal.name)
        assertEquals(0.0, signal.weight, 1e-6)
        assertEquals(1.0, signal.homeProb + signal.drawProb + signal.awayProb, 1e-4)
    }
}
