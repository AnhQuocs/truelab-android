package dev.anhquocs.truelab.core.domain.evaluation.usecase

import dev.anhquocs.truelab.core.domain.evaluation.model.ConfusionMatrix3Way
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CalculateEvaluationMetricsUseCaseTest {

    private lateinit var useCase: CalculateEvaluationMetricsUseCase
    private val delta = 1e-6

    @Before
    fun setUp() {
        useCase = CalculateEvaluationMetricsUseCase()
    }

    @Test
    fun `1 Perfect prediction returns 1_0 for accuracy precision recall and F1`() {
        // 2 HOME_WIN, 2 DRAW, 2 AWAY_WIN - all predicted correctly
        val dataset = listOf(
            Pair("HOME_WIN", "HOME_WIN"),
            Pair("HOME_WIN", "HOME_WIN"),
            Pair("DRAW", "DRAW"),
            Pair("DRAW", "DRAW"),
            Pair("AWAY_WIN", "AWAY_WIN"),
            Pair("AWAY_WIN", "AWAY_WIN")
        )

        val result = useCase(dataset)

        assertEquals(1.0, result.accuracy, delta)
        assertEquals(1.0, result.macroPrecision, delta)
        assertEquals(1.0, result.macroRecall, delta)
        assertEquals(1.0, result.macroF1, delta)
        assertEquals(6, result.totalEvaluated)

        // Check each class
        assertEquals(1.0, result.homeMetrics.precision, delta)
        assertEquals(1.0, result.homeMetrics.recall, delta)
        assertEquals(1.0, result.homeMetrics.f1Score, delta)
        assertEquals(2, result.homeMetrics.support)

        assertEquals(1.0, result.drawMetrics.precision, delta)
        assertEquals(1.0, result.drawMetrics.recall, delta)
        assertEquals(1.0, result.drawMetrics.f1Score, delta)
        assertEquals(2, result.drawMetrics.support)

        assertEquals(1.0, result.awayMetrics.precision, delta)
        assertEquals(1.0, result.awayMetrics.recall, delta)
        assertEquals(1.0, result.awayMetrics.f1Score, delta)
        assertEquals(2, result.awayMetrics.support)

        // Diagonal elements
        assertEquals(2, result.confusionMatrix.homeAsHome)
        assertEquals(2, result.confusionMatrix.drawAsDraw)
        assertEquals(2, result.confusionMatrix.awayAsAway)
        assertEquals(6, result.confusionMatrix.correctPredictions)
        assertEquals(6, result.confusionMatrix.totalSamples)
    }

    @Test
    fun `2 Completely wrong predictions return 0_0 accuracy and 0_0 F1`() {
        // Actual is HOME_WIN but predicted DRAW; Actual is DRAW but predicted AWAY_WIN; Actual is AWAY_WIN but predicted HOME_WIN
        val dataset = listOf(
            Pair("DRAW", "HOME_WIN"),
            Pair("AWAY_WIN", "DRAW"),
            Pair("HOME_WIN", "AWAY_WIN")
        )

        val result = useCase(dataset)

        assertEquals(0.0, result.accuracy, delta)
        assertEquals(0.0, result.macroPrecision, delta)
        assertEquals(0.0, result.macroRecall, delta)
        assertEquals(0.0, result.macroF1, delta)
        assertEquals(0, result.confusionMatrix.correctPredictions)
        assertEquals(3, result.totalEvaluated)

        assertEquals(0.0, result.homeMetrics.precision, delta)
        assertEquals(0.0, result.homeMetrics.recall, delta)
        assertEquals(0.0, result.homeMetrics.f1Score, delta)

        assertEquals(0.0, result.drawMetrics.precision, delta)
        assertEquals(0.0, result.drawMetrics.recall, delta)
        assertEquals(0.0, result.drawMetrics.f1Score, delta)

        assertEquals(0.0, result.awayMetrics.precision, delta)
        assertEquals(0.0, result.awayMetrics.recall, delta)
        assertEquals(0.0, result.awayMetrics.f1Score, delta)
    }

    @Test
    fun `3 Mixed dataset across 3 classes computes exact metrics and confusion matrix`() {
        // Hand-crafted dataset:
        // Actual HOME:
        //   - 3 predicted HOME
        //   - 1 predicted DRAW
        //   - 0 predicted AWAY
        //   Total actual HOME = 4
        //
        // Actual DRAW:
        //   - 1 predicted HOME
        //   - 2 predicted DRAW
        //   - 1 predicted AWAY
        //   Total actual DRAW = 4
        //
        // Actual AWAY:
        //   - 0 predicted HOME
        //   - 1 predicted DRAW
        //   - 3 predicted AWAY
        //   Total actual AWAY = 4
        //
        // Total samples = 12
        // Correct predictions = 3 + 2 + 3 = 8
        // Total predicted HOME = 3 + 1 + 0 = 4
        // Total predicted DRAW = 1 + 2 + 1 = 4
        // Total predicted AWAY = 0 + 1 + 3 = 4

        val dataset = listOf(
            // Actual HOME (4)
            Pair("HOME_WIN", "HOME_WIN"),
            Pair("HOME_WIN", "HOME_WIN"),
            Pair("HOME_WIN", "HOME_WIN"),
            Pair("DRAW", "HOME_WIN"),

            // Actual DRAW (4)
            Pair("HOME_WIN", "DRAW"),
            Pair("DRAW", "DRAW"),
            Pair("DRAW", "DRAW"),
            Pair("AWAY_WIN", "DRAW"),

            // Actual AWAY (4)
            Pair("DRAW", "AWAY_WIN"),
            Pair("AWAY_WIN", "AWAY_WIN"),
            Pair("AWAY_WIN", "AWAY_WIN"),
            Pair("AWAY_WIN", "AWAY_WIN")
        )

        val result = useCase(dataset)

        // Accuracy = 8 / 12 = 2/3 ≈ 0.666667
        assertEquals(8.0 / 12.0, result.accuracy, delta)
        assertEquals(12, result.totalEvaluated)

        // Home metrics:
        // Precision = 3 / 4 = 0.75
        // Recall = 3 / 4 = 0.75
        // F1 = 0.75
        assertEquals(0.75, result.homeMetrics.precision, delta)
        assertEquals(0.75, result.homeMetrics.recall, delta)
        assertEquals(0.75, result.homeMetrics.f1Score, delta)
        assertEquals(4, result.homeMetrics.support)

        // Draw metrics:
        // Precision = 2 / 4 = 0.50
        // Recall = 2 / 4 = 0.50
        // F1 = 0.50
        assertEquals(0.50, result.drawMetrics.precision, delta)
        assertEquals(0.50, result.drawMetrics.recall, delta)
        assertEquals(0.50, result.drawMetrics.f1Score, delta)
        assertEquals(4, result.drawMetrics.support)

        // Away metrics:
        // Precision = 3 / 4 = 0.75
        // Recall = 3 / 4 = 0.75
        // F1 = 0.75
        assertEquals(0.75, result.awayMetrics.precision, delta)
        assertEquals(0.75, result.awayMetrics.recall, delta)
        assertEquals(0.75, result.awayMetrics.f1Score, delta)
        assertEquals(4, result.awayMetrics.support)

        // Macro metrics:
        // Macro P = (0.75 + 0.50 + 0.75) / 3 = 2.0 / 3 = 0.666667
        // Macro R = (0.75 + 0.50 + 0.75) / 3 = 0.666667
        // Macro F1 = (0.75 + 0.50 + 0.75) / 3 = 0.666667
        assertEquals(2.0 / 3.0, result.macroPrecision, delta)
        assertEquals(2.0 / 3.0, result.macroRecall, delta)
        assertEquals(2.0 / 3.0, result.macroF1, delta)

        // Verify all 9 cells of Confusion Matrix
        val cm = result.confusionMatrix
        assertEquals(3, cm.homeAsHome)
        assertEquals(1, cm.homeAsDraw)
        assertEquals(0, cm.homeAsAway)

        assertEquals(1, cm.drawAsHome)
        assertEquals(2, cm.drawAsDraw)
        assertEquals(1, cm.drawAsAway)

        assertEquals(0, cm.awayAsHome)
        assertEquals(1, cm.awayAsDraw)
        assertEquals(3, cm.awayAsAway)

        assertEquals(12, cm.totalSamples)
        assertEquals(8, cm.correctPredictions)
    }

    @Test
    fun `4 Empty dataset returns all zero metrics without crashing or dividing by zero`() {
        val result = useCase(emptyList())

        assertEquals(0.0, result.accuracy, delta)
        assertEquals(0.0, result.macroPrecision, delta)
        assertEquals(0.0, result.macroRecall, delta)
        assertEquals(0.0, result.macroF1, delta)
        assertEquals(0, result.totalEvaluated)

        assertEquals(0.0, result.homeMetrics.precision, delta)
        assertEquals(0.0, result.homeMetrics.recall, delta)
        assertEquals(0.0, result.homeMetrics.f1Score, delta)
        assertEquals(0, result.homeMetrics.support)

        assertEquals(0.0, result.drawMetrics.precision, delta)
        assertEquals(0.0, result.drawMetrics.recall, delta)
        assertEquals(0.0, result.drawMetrics.f1Score, delta)
        assertEquals(0, result.drawMetrics.support)

        assertEquals(0.0, result.awayMetrics.precision, delta)
        assertEquals(0.0, result.awayMetrics.recall, delta)
        assertEquals(0.0, result.awayMetrics.f1Score, delta)
        assertEquals(0, result.awayMetrics.support)

        assertEquals(0, result.confusionMatrix.totalSamples)
        assertEquals(0, result.confusionMatrix.correctPredictions)
    }

    @Test
    fun `5 Class missing in actual labels handles zero recall and support safely`() {
        // Dataset contains only HOME_WIN and AWAY_WIN in actual labels. No DRAWs occurred.
        val dataset = listOf(
            Pair("HOME_WIN", "HOME_WIN"),
            Pair("DRAW", "HOME_WIN"),
            Pair("AWAY_WIN", "AWAY_WIN")
        )

        val result = useCase(dataset)

        assertEquals(2.0 / 3.0, result.accuracy, delta)

        // Draw class has support = 0, recall = 0.0, precision = 0 / 1 = 0.0, F1 = 0.0
        assertEquals(0, result.drawMetrics.support)
        assertEquals(0.0, result.drawMetrics.recall, delta)
        assertEquals(0.0, result.drawMetrics.precision, delta)
        assertEquals(0.0, result.drawMetrics.f1Score, delta)

        // Home class: actual = 2, pred = 1, TP = 1 -> P = 1/1 = 1.0, R = 1/2 = 0.5, F1 = 2*(1*0.5)/(1.5) = 2/3
        assertEquals(2, result.homeMetrics.support)
        assertEquals(1.0, result.homeMetrics.precision, delta)
        assertEquals(0.5, result.homeMetrics.recall, delta)
        assertEquals(2.0 / 3.0, result.homeMetrics.f1Score, delta)

        // Away class: actual = 1, pred = 1, TP = 1 -> P = 1.0, R = 1.0, F1 = 1.0
        assertEquals(1, result.awayMetrics.support)
        assertEquals(1.0, result.awayMetrics.precision, delta)
        assertEquals(1.0, result.awayMetrics.recall, delta)
        assertEquals(1.0, result.awayMetrics.f1Score, delta)
    }

    @Test
    fun `6 Class missing in predicted labels handles zero precision safely`() {
        // Model never predicted DRAW
        val dataset = listOf(
            Pair("HOME_WIN", "HOME_WIN"),
            Pair("HOME_WIN", "DRAW"),
            Pair("AWAY_WIN", "AWAY_WIN")
        )

        val result = useCase(dataset)

        // Draw class: pred = 0, TP = 0 -> Precision = 0.0, Recall = 0/1 = 0.0, F1 = 0.0, support = 1
        assertEquals(0.0, result.drawMetrics.precision, delta)
        assertEquals(0.0, result.drawMetrics.recall, delta)
        assertEquals(0.0, result.drawMetrics.f1Score, delta)
        assertEquals(1, result.drawMetrics.support)
    }

    @Test
    fun `7 Zero division edge case produces 0_0 rather than NaN or Infinity`() {
        // Only 1 match evaluated
        val dataset = listOf(Pair("HOME_WIN", "HOME_WIN"))
        val result = useCase(dataset)

        // Draw and Away have 0 predictions and 0 actuals
        assertTrue(!result.drawMetrics.precision.isNaN())
        assertTrue(!result.drawMetrics.precision.isInfinite())
        assertEquals(0.0, result.drawMetrics.precision, delta)
        assertEquals(0.0, result.drawMetrics.recall, delta)
        assertEquals(0.0, result.drawMetrics.f1Score, delta)

        assertTrue(!result.awayMetrics.precision.isNaN())
        assertTrue(!result.awayMetrics.precision.isInfinite())
        assertEquals(0.0, result.awayMetrics.precision, delta)
        assertEquals(0.0, result.awayMetrics.recall, delta)
        assertEquals(0.0, result.awayMetrics.f1Score, delta)

        assertTrue(!result.macroPrecision.isNaN())
        assertTrue(!result.macroRecall.isNaN())
        assertTrue(!result.macroF1.isNaN())
    }

    @Test
    fun `8 Confusion matrix orientation and cell counts are verified strictly`() {
        // Place exactly 1 item in each of the 9 possible (pred, actual) combinations
        val dataset = listOf(
            Pair("HOME_WIN", "HOME_WIN"), // homeAsHome
            Pair("DRAW", "HOME_WIN"),     // homeAsDraw
            Pair("AWAY_WIN", "HOME_WIN"), // homeAsAway

            Pair("HOME_WIN", "DRAW"),     // drawAsHome
            Pair("DRAW", "DRAW"),         // drawAsDraw
            Pair("AWAY_WIN", "DRAW"),     // drawAsAway

            Pair("HOME_WIN", "AWAY_WIN"), // awayAsHome
            Pair("DRAW", "AWAY_WIN"),     // awayAsDraw
            Pair("AWAY_WIN", "AWAY_WIN")  // awayAsAway
        )

        val result = useCase(dataset)
        val cm = result.confusionMatrix

        assertEquals(1, cm.homeAsHome)
        assertEquals(1, cm.homeAsDraw)
        assertEquals(1, cm.homeAsAway)

        assertEquals(1, cm.drawAsHome)
        assertEquals(1, cm.drawAsDraw)
        assertEquals(1, cm.drawAsAway)

        assertEquals(1, cm.awayAsHome)
        assertEquals(1, cm.awayAsDraw)
        assertEquals(1, cm.awayAsAway)

        assertEquals(9, cm.totalSamples)
        assertEquals(3, cm.correctPredictions)
        assertEquals(3.0 / 9.0, result.accuracy, delta)
    }

    @Test
    fun `9 Macro precision is calculated as exact arithmetic mean of 3 class precisions`() {
        // Home P = 1.0, Draw P = 0.5, Away P = 0.0 -> Macro P = 1.5 / 3 = 0.5
        val dataset = listOf(
            Pair("HOME_WIN", "HOME_WIN"),
            Pair("DRAW", "DRAW"),
            Pair("DRAW", "AWAY_WIN"),
            Pair("AWAY_WIN", "HOME_WIN")
        )

        val result = useCase(dataset)

        val expectedMacroP = (result.homeMetrics.precision + result.drawMetrics.precision + result.awayMetrics.precision) / 3.0
        assertEquals(expectedMacroP, result.macroPrecision, delta)
    }

    @Test
    fun `10 Macro recall is calculated as exact arithmetic mean of 3 class recalls`() {
        val dataset = listOf(
            Pair("HOME_WIN", "HOME_WIN"),
            Pair("DRAW", "DRAW"),
            Pair("HOME_WIN", "AWAY_WIN")
        )

        val result = useCase(dataset)

        val expectedMacroR = (result.homeMetrics.recall + result.drawMetrics.recall + result.awayMetrics.recall) / 3.0
        assertEquals(expectedMacroR, result.macroRecall, delta)
    }

    @Test
    fun `11 Macro F1 is calculated as exact arithmetic mean of 3 class F1 scores`() {
        val dataset = listOf(
            Pair("HOME_WIN", "HOME_WIN"),
            Pair("DRAW", "DRAW"),
            Pair("AWAY_WIN", "DRAW"),
            Pair("AWAY_WIN", "AWAY_WIN")
        )

        val result = useCase(dataset)

        val expectedMacroF1 = (result.homeMetrics.f1Score + result.drawMetrics.f1Score + result.awayMetrics.f1Score) / 3.0
        assertEquals(expectedMacroF1, result.macroF1, delta)
    }

    @Test
    fun `12 Invalid predicted or actual outcome label throws IllegalArgumentException`() {
        val invalidPred = listOf(Pair("INVALID_LABEL", "HOME_WIN"))
        val exception1 = assertThrows(IllegalArgumentException::class.java) {
            useCase(invalidPred)
        }
        assertTrue(exception1.message?.contains("Invalid predicted outcome label") == true)

        val invalidActual = listOf(Pair("HOME_WIN", "UNKNOWN_OUTCOME"))
        val exception2 = assertThrows(IllegalArgumentException::class.java) {
            useCase(invalidActual)
        }
        assertTrue(exception2.message?.contains("Invalid actual outcome label") == true)
    }
}
