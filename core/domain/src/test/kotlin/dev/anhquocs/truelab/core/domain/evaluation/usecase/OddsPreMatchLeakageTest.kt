package dev.anhquocs.truelab.core.domain.evaluation.usecase

import dev.anhquocs.truelab.core.domain.odds.model.OddsRecordItem
import dev.anhquocs.truelab.core.domain.prediction.model.PredictionWeightConfig
import dev.anhquocs.truelab.core.domain.prediction.transformer.OddsSignalTransformer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OddsPreMatchLeakageTest {

    @Test
    fun `valid european odds are correctly mapped to 3-way implied probabilities`() {
        // European 1X2 odds: Home 2.0, Draw 3.2, Away 4.0
        val oddsItem = OddsRecordItem(
            companyId = 1,
            companyName = "Bet365",
            oddsType = "eu",
            handicap = null,
            over = null,
            under = null,
            homeWin = 2.0,
            draw = 3.2,
            awayWin = 4.0,
            changeTime = 1700000000L,
            marketPhase = "instant"
        )

        val signal = OddsSignalTransformer.transform(oddsItem, PredictionWeightConfig.DEFAULT)

        assertEquals(PredictionWeightConfig.DEFAULT.oddsWeight, signal.weight, 1e-6)
        assertTrue("homeProb should be positive", signal.homeProb > 0.0)
        assertTrue("drawProb should be positive", signal.drawProb > 0.0)
        assertTrue("awayProb should be positive", signal.awayProb > 0.0)
        assertEquals(1.0, signal.homeProb + signal.drawProb + signal.awayProb, 1e-4)

        // Implied: 1/2.0 = 0.50, 1/3.2 = 0.3125, 1/4.0 = 0.25. Sum = 1.0625
        // Normalized Home: 0.50 / 1.0625 ≈ 0.4706
        // Normalized Draw: 0.3125 / 1.0625 ≈ 0.2941
        // Normalized Away: 0.25 / 1.0625 ≈ 0.2353
        assertEquals(0.4706, signal.homeProb, 1e-2)
        assertEquals(0.2941, signal.drawProb, 1e-2)
        assertEquals(0.2353, signal.awayProb, 1e-2)
    }

    @Test
    fun `null or zero odds fallback to zero weight with neutral probabilities`() {
        val signalNull = OddsSignalTransformer.transform(null, PredictionWeightConfig.DEFAULT)

        assertEquals(0.0, signalNull.weight, 1e-6)
        assertEquals(PredictionWeightConfig.DEFAULT.baselineDrawProb, signalNull.drawProb, 1e-6)
        assertEquals(1.0, signalNull.homeProb + signalNull.drawProb + signalNull.awayProb, 1e-4)

        val invalidOdds = OddsRecordItem(
            companyId = 1,
            companyName = "Invalid",
            oddsType = "eu",
            handicap = null,
            over = null,
            under = null,
            homeWin = 0.0,
            draw = -1.0,
            awayWin = 0.0,
            changeTime = 1700000000L,
            marketPhase = "instant"
        )

        val signalInvalid = OddsSignalTransformer.transform(invalidOdds, PredictionWeightConfig.DEFAULT)
        assertEquals(0.0, signalInvalid.weight, 1e-6)
        assertEquals(1.0, signalInvalid.homeProb + signalInvalid.drawProb + signalInvalid.awayProb, 1e-4)
    }
}
