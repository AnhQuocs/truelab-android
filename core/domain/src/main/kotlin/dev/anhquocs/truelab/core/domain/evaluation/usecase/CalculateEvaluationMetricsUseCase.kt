package dev.anhquocs.truelab.core.domain.evaluation.usecase

import dev.anhquocs.truelab.core.domain.evaluation.model.ClassEvaluationMetrics
import dev.anhquocs.truelab.core.domain.evaluation.model.ConfusionMatrix3Way
import dev.anhquocs.truelab.core.domain.evaluation.model.ModelEvaluationResult

/**
 * UseCase thuần túy (Pure Kotlin/JVM) tính toán các chỉ số đánh giá phân loại đa lớp (Multi-class Classification Metrics)
 * và Ma trận nhầm lẫn 3 chiều (3x3 Confusion Matrix) cho bài toán dự đoán kết quả trận đấu bóng đá.
 *
 * Các nhãn hợp lệ:
 * - "HOME_WIN" (Đội nhà thắng)
 * - "DRAW" (Hòa)
 * - "AWAY_WIN" (Đội khách thắng)
 */
class CalculateEvaluationMetricsUseCase {

    companion object {
        const val LABEL_HOME_WIN = "HOME_WIN"
        const val LABEL_DRAW = "DRAW"
        const val LABEL_AWAY_WIN = "AWAY_WIN"

        val VALID_LABELS = setOf(LABEL_HOME_WIN, LABEL_DRAW, LABEL_AWAY_WIN)
    }

    /**
     * Thực hiện tính toán chỉ số đánh giá và ma trận nhầm lẫn.
     *
     * @param predictions Danh sách các cặp kết quả (predictedOutcome, actualOutcome).
     *        Trong đó:
     *        - [Pair.first]: Kết quả mô hình dự đoán.
     *        - [Pair.second]: Kết quả thực tế của trận đấu.
     * @return [ModelEvaluationResult] chứa đầy đủ ma trận nhầm lẫn và các chỉ số thống kê.
     * @throws IllegalArgumentException nếu xuất hiện nhãn kết quả không thuộc [VALID_LABELS].
     */
    operator fun invoke(
        predictions: List<Pair<String, String>>
    ): ModelEvaluationResult {
        if (predictions.isEmpty()) {
            return emptyResult()
        }

        var homeAsHome = 0
        var homeAsDraw = 0
        var homeAsAway = 0

        var drawAsHome = 0
        var drawAsDraw = 0
        var drawAsAway = 0

        var awayAsHome = 0
        var awayAsDraw = 0
        var awayAsAway = 0

        for ((predicted, actual) in predictions) {
            require(predicted in VALID_LABELS) {
                "Invalid predicted outcome label: '$predicted'. Valid labels are: $VALID_LABELS"
            }
            require(actual in VALID_LABELS) {
                "Invalid actual outcome label: '$actual'. Valid labels are: $VALID_LABELS"
            }

            when (actual) {
                LABEL_HOME_WIN -> when (predicted) {
                    LABEL_HOME_WIN -> homeAsHome++
                    LABEL_DRAW -> homeAsDraw++
                    LABEL_AWAY_WIN -> homeAsAway++
                }
                LABEL_DRAW -> when (predicted) {
                    LABEL_HOME_WIN -> drawAsHome++
                    LABEL_DRAW -> drawAsDraw++
                    LABEL_AWAY_WIN -> drawAsAway++
                }
                LABEL_AWAY_WIN -> when (predicted) {
                    LABEL_HOME_WIN -> awayAsHome++
                    LABEL_DRAW -> awayAsDraw++
                    LABEL_AWAY_WIN -> awayAsAway++
                }
            }
        }

        val confusionMatrix = ConfusionMatrix3Way(
            homeAsHome = homeAsHome,
            homeAsDraw = homeAsDraw,
            homeAsAway = homeAsAway,
            drawAsHome = drawAsHome,
            drawAsDraw = drawAsDraw,
            drawAsAway = drawAsAway,
            awayAsHome = awayAsHome,
            awayAsDraw = awayAsDraw,
            awayAsAway = awayAsAway
        )

        // Class HOME_WIN
        val actualHome = homeAsHome + homeAsDraw + homeAsAway
        val predHome = homeAsHome + drawAsHome + awayAsHome
        val homePrecision = if (predHome > 0) homeAsHome.toDouble() / predHome else 0.0
        val homeRecall = if (actualHome > 0) homeAsHome.toDouble() / actualHome else 0.0
        val homeF1 = calculateF1(homePrecision, homeRecall)
        val homeMetrics = ClassEvaluationMetrics(
            precision = homePrecision,
            recall = homeRecall,
            f1Score = homeF1,
            support = actualHome
        )

        // Class DRAW
        val actualDraw = drawAsHome + drawAsDraw + drawAsAway
        val predDraw = homeAsDraw + drawAsDraw + awayAsDraw
        val drawPrecision = if (predDraw > 0) drawAsDraw.toDouble() / predDraw else 0.0
        val drawRecall = if (actualDraw > 0) drawAsDraw.toDouble() / actualDraw else 0.0
        val drawF1 = calculateF1(drawPrecision, drawRecall)
        val drawMetrics = ClassEvaluationMetrics(
            precision = drawPrecision,
            recall = drawRecall,
            f1Score = drawF1,
            support = actualDraw
        )

        // Class AWAY_WIN
        val actualAway = awayAsHome + awayAsDraw + awayAsAway
        val predAway = homeAsAway + drawAsAway + awayAsAway
        val awayPrecision = if (predAway > 0) awayAsAway.toDouble() / predAway else 0.0
        val awayRecall = if (actualAway > 0) awayAsAway.toDouble() / actualAway else 0.0
        val awayF1 = calculateF1(awayPrecision, awayRecall)
        val awayMetrics = ClassEvaluationMetrics(
            precision = awayPrecision,
            recall = awayRecall,
            f1Score = awayF1,
            support = actualAway
        )

        // Overall & Macro Metrics
        val total = predictions.size
        val correct = confusionMatrix.correctPredictions
        val accuracy = if (total > 0) correct.toDouble() / total else 0.0

        val macroPrecision = (homePrecision + drawPrecision + awayPrecision) / 3.0
        val macroRecall = (homeRecall + drawRecall + awayRecall) / 3.0
        val macroF1 = (homeF1 + drawF1 + awayF1) / 3.0

        return ModelEvaluationResult(
            accuracy = accuracy,
            macroPrecision = macroPrecision,
            macroRecall = macroRecall,
            macroF1 = macroF1,
            homeMetrics = homeMetrics,
            drawMetrics = drawMetrics,
            awayMetrics = awayMetrics,
            confusionMatrix = confusionMatrix,
            totalEvaluated = total
        )
    }

    private fun calculateF1(precision: Double, recall: Double): Double {
        val sum = precision + recall
        return if (sum > 0.0) {
            2.0 * precision * recall / sum
        } else {
            0.0
        }
    }

    private fun emptyResult(): ModelEvaluationResult {
        val emptyClassMetrics = ClassEvaluationMetrics(
            precision = 0.0,
            recall = 0.0,
            f1Score = 0.0,
            support = 0
        )
        return ModelEvaluationResult(
            accuracy = 0.0,
            macroPrecision = 0.0,
            macroRecall = 0.0,
            macroF1 = 0.0,
            homeMetrics = emptyClassMetrics,
            drawMetrics = emptyClassMetrics,
            awayMetrics = emptyClassMetrics,
            confusionMatrix = ConfusionMatrix3Way(),
            totalEvaluated = 0
        )
    }
}
