package dev.anhquocs.truelab.core.domain.evaluation.model

import dev.anhquocs.truelab.core.domain.prediction.model.DrawMarginConfig
import dev.anhquocs.truelab.core.domain.prediction.model.DynamicDrawPriorConfig
import java.util.Locale

/**
 * Kết quả đánh giá đối chứng toàn diện 3 chiều (3-Way Evaluation) trên tập dữ liệu kiểm thử độc lập (Independent Test Set).
 *
 * @property totalSamples Số lượng trận đấu trong tập Test độc lập (30% sau cùng).
 * @property baselineResult Kết quả đánh giá của nhóm đối chứng Baseline.
 * @property baselineBrierScore Điểm Brier Score của Baseline.
 * @property candidateAResult Kết quả đánh giá của Candidate A với tham số đã đóng băng.
 * @property candidateABrierScore Điểm Brier Score của Candidate A.
 * @property candidateAFrozenConfig Cấu hình tham số đã đóng băng của Candidate A.
 * @property candidateBResult Kết quả đánh giá của Candidate B với tham số đã đóng băng.
 * @property candidateBBrierScore Điểm Brier Score của Candidate B.
 * @property candidateBFrozenConfig Cấu hình tham số đã đóng băng của Candidate B.
 */
data class IndependentTestEvaluationResult(
    val totalSamples: Int,
    val baselineResult: ModelEvaluationResult,
    val baselineBrierScore: Double,
    val candidateAResult: ModelEvaluationResult,
    val candidateABrierScore: Double,
    val candidateAFrozenConfig: DrawMarginConfig,
    val candidateBResult: ModelEvaluationResult,
    val candidateBBrierScore: Double,
    val candidateBFrozenConfig: DynamicDrawPriorConfig
) {

    /**
     * Tính độ lệch hiệu năng (Metric Delta) của Candidate A so với Baseline trên Test Set.
     */
    fun candidateADeltas(): ModelMetricDeltas = computeDeltas(candidateAResult, baselineResult)

    /**
     * Tính độ lệch hiệu năng (Metric Delta) của Candidate B so với Baseline trên Test Set.
     */
    fun candidateBDeltas(): ModelMetricDeltas = computeDeltas(candidateBResult, baselineResult)

    private fun computeDeltas(target: ModelEvaluationResult, baseline: ModelEvaluationResult): ModelMetricDeltas {
        return ModelMetricDeltas(
            accuracyDelta = target.accuracy - baseline.accuracy,
            macroPrecisionDelta = target.macroPrecision - baseline.macroPrecision,
            macroRecallDelta = target.macroRecall - baseline.macroRecall,
            macroF1Delta = target.macroF1 - baseline.macroF1,
            homeF1Delta = target.homeMetrics.f1Score - baseline.homeMetrics.f1Score,
            drawF1Delta = target.drawMetrics.f1Score - baseline.drawMetrics.f1Score,
            awayF1Delta = target.awayMetrics.f1Score - baseline.awayMetrics.f1Score,
            drawPrecisionDelta = target.drawMetrics.precision - baseline.drawMetrics.precision,
            drawRecallDelta = target.drawMetrics.recall - baseline.drawMetrics.recall
        )
    }
}

/**
 * Bản ghi độ lệch chỉ số giữa mô hình thử nghiệm và Baseline.
 */
data class ModelMetricDeltas(
    val accuracyDelta: Double,
    val macroPrecisionDelta: Double,
    val macroRecallDelta: Double,
    val macroF1Delta: Double,
    val homeF1Delta: Double,
    val drawF1Delta: Double,
    val awayF1Delta: Double,
    val drawPrecisionDelta: Double,
    val drawRecallDelta: Double
)

/**
 * Khoảng cách tổng quát hóa (Generalization Gap = Test Metric - Calibration Metric).
 */
data class GeneralizationGapResult(
    val modelName: String,
    val calibrationMacroF1: Double,
    val testMacroF1: Double,
    val macroF1Gap: Double,
    val calibrationDrawF1: Double,
    val testDrawF1: Double,
    val drawF1Gap: Double,
    val calibrationDrawRecall: Double,
    val testDrawRecall: Double,
    val drawRecallGap: Double,
    val calibrationAccuracy: Double,
    val testAccuracy: Double,
    val accuracyGap: Double
)
