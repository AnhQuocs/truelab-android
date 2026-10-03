package dev.anhquocs.truelab.core.domain.evaluation.model

import dev.anhquocs.truelab.core.domain.prediction.model.DynamicDrawPriorConfig
import java.util.Locale

/**
 * Lưới tham số quét không gian (Parameter Search Space Grid) cho Candidate B: Dynamic Draw Prior.
 *
 * Không gian tìm kiếm:
 * - P_D,max ∈ {0.32, 0.34, 0.36, 0.38} (4 giá trị)
 * - σ_Elo ∈ {0.75, 1.00, 1.25, 1.50} (4 giá trị)
 * - σ_Form ∈ {0.15, 0.20, 0.25, 0.30} (4 giá trị)
 * - Cố định P_D,min = 0.12
 *
 * Tổng số cấu hình: 4 × 4 × 4 = 64 cấu hình.
 */
object DrawParameterGrid {

    val PD_MAX_VALUES = listOf(0.32, 0.34, 0.36, 0.38)
    val SIGMA_ELO_VALUES = listOf(0.75, 1.00, 1.25, 1.50)
    val SIGMA_FORM_VALUES = listOf(0.15, 0.20, 0.25, 0.30)
    const val PD_MIN_FIXED = 0.12

    /**
     * Sinh danh sách đầy đủ đúng 64 cấu hình theo thứ tự tất định (Deterministic Order).
     */
    fun generateCandidateBGrid(): List<DynamicDrawPriorConfig> {
        val grid = ArrayList<DynamicDrawPriorConfig>(64)
        for (pdMax in PD_MAX_VALUES) {
            for (sElo in SIGMA_ELO_VALUES) {
                for (sForm in SIGMA_FORM_VALUES) {
                    grid.add(
                        DynamicDrawPriorConfig(
                            maxDrawProb = pdMax,
                            minDrawProb = PD_MIN_FIXED,
                            eloSigma = sElo,
                            formSigma = sForm
                        )
                    )
                }
            }
        }
        return grid
    }
}

/**
 * Lưới tham số quét không gian (Parameter Search Space Grid) cho Candidate A: Decision Margin.
 *
 * Không gian tìm kiếm:
 * - δ (deltaMargin) ∈ {0.02, 0.03, 0.04, 0.05, 0.06, 0.07, 0.08} (7 giá trị)
 * - θ (thetaMinProb) ∈ {0.250, 0.255, 0.260, 0.265, 0.270, 0.275} (6 giá trị)
 *
 * Tổng số cấu hình: 7 × 6 = 42 cấu hình.
 */
object DrawMarginParameterGrid {

    val DELTA_VALUES = listOf(0.02, 0.03, 0.04, 0.05, 0.06, 0.07, 0.08)
    val THETA_VALUES = listOf(0.250, 0.255, 0.260, 0.265, 0.270, 0.275)

    /**
     * Sinh danh sách đầy đủ đúng 42 cấu hình theo thứ tự tất định (Deterministic Order).
     */
    fun generateCandidateAGrid(): List<dev.anhquocs.truelab.core.domain.prediction.model.DrawMarginConfig> {
        val grid = ArrayList<dev.anhquocs.truelab.core.domain.prediction.model.DrawMarginConfig>(42)
        for (delta in DELTA_VALUES) {
            for (theta in THETA_VALUES) {
                grid.add(
                    dev.anhquocs.truelab.core.domain.prediction.model.DrawMarginConfig(
                        deltaMargin = delta,
                        thetaMinProb = theta
                    )
                )
            }
        }
        return grid
    }
}

/**
 * Cấu hình kiểm soát quá trình tối ưu hóa tham số cho Candidate A (Decision Margin).
 */
data class CandidateAOptimizationConfig(
    val f1Tolerance: Double = 0.03,
    val validationRatio: Double = 0.70
) {
    init {
        require(f1Tolerance >= 0.0 && f1Tolerance.isFinite()) {
            "f1Tolerance must be non-negative and finite, but was $f1Tolerance"
        }
        require(validationRatio in 0.0..1.0 && validationRatio.isFinite()) {
            "validationRatio must be in range [0.0, 1.0], but was $validationRatio"
        }
    }
}

/**
 * Bản ghi kết quả đánh giá cho một cấu hình tham số Candidate A trong Grid Search.
 */
data class CandidateAGridEvaluationRecord(
    val config: dev.anhquocs.truelab.core.domain.prediction.model.DrawMarginConfig,
    val sampleCount: Int,
    val accuracy: Double,
    val macroPrecision: Double,
    val macroRecall: Double,
    val macroF1: Double,
    val homeF1: Double,
    val drawF1: Double,
    val awayF1: Double,
    val drawPrecision: Double,
    val drawRecall: Double,
    val eligible: Boolean,
    val homeF1Delta: Double,
    val awayF1Delta: Double,
    val macroF1Delta: Double,
    val confusionMatrix: ConfusionMatrix3Way
)

/**
 * Kết quả tổng thể của phiên quét siêu tham số Grid Search cho Candidate A.
 */
data class CandidateAOptimizationResult(
    val baselineResult: ModelEvaluationResult,
    val totalConfigurations: Int,
    val eligibleConfigurationsCount: Int,
    val bestConfiguration: CandidateAGridEvaluationRecord?,
    val allRecords: List<CandidateAGridEvaluationRecord>
) {
    fun topConfigurations(limit: Int = 10): List<CandidateAGridEvaluationRecord> =
        allRecords.take(limit)
}

/**
 * Bộ phân tách tập dữ liệu theo thứ tự thời gian (Temporal Dataset Splitter).
 *
 * Tuyệt đối không xáo trộn (No Shuffle), đảm bảo chống rò rỉ dữ liệu tương lai (No Data Leakage):
 * - Earliest [validationRatio] (mặc định 70%) -> Calibration / Validation Set (Dùng để tối ưu tham số).
 * - Remaining (1 - validationRatio) (mặc định 30%) -> Independent Test Set (Dùng cho đánh giá độc lập ở Phase B5).
 */
object TemporalDatasetSplitter {

    data class SplitResult<T>(
        val calibrationValidation: List<T>,
        val independentTest: List<T>
    )

    fun <T> splitChronological(
        items: List<T>,
        validationRatio: Double = 0.70
    ): SplitResult<T> {
        require(validationRatio in 0.0..1.0 && validationRatio.isFinite()) {
            "validationRatio must be in range [0.0, 1.0], but was $validationRatio"
        }
        if (items.isEmpty()) {
            return SplitResult(emptyList(), emptyList())
        }
        val splitIndex = (items.size * validationRatio).toInt().coerceIn(0, items.size)
        return SplitResult(
            calibrationValidation = items.subList(0, splitIndex),
            independentTest = items.subList(splitIndex, items.size)
        )
    }
}

/**
 * Cấu hình tham số kiểm soát quá trình Grid Search Optimization.
 *
 * @property f1Tolerance Dung sai suy giảm F1 tối đa cho phép của Home và Away so với Baseline (mặc định 0.03).
 * @property validationRatio Tỷ lệ phân tách tập dữ liệu thời gian cho Calibration/Validation (mặc định 0.70).
 */
data class CandidateBOptimizationConfig(
    val f1Tolerance: Double = 0.03,
    val validationRatio: Double = 0.70
) {
    init {
        require(f1Tolerance >= 0.0 && f1Tolerance.isFinite()) {
            "f1Tolerance must be non-negative and finite, but was $f1Tolerance"
        }
        require(validationRatio in 0.0..1.0 && validationRatio.isFinite()) {
            "validationRatio must be in range [0.0, 1.0], but was $validationRatio"
        }
    }
}

/**
 * Bản ghi kết quả đánh giá cho một cấu hình tham số trong Grid Search.
 *
 * @property config Cấu hình DynamicDrawPriorConfig được đánh giá.
 * @property sampleCount Số lượng trận đấu trong tập đánh giá.
 * @property accuracy Độ chính xác tổng thể.
 * @property macroPrecision Macro Precision trên 3 lớp.
 * @property macroRecall Macro Recall trên 3 lớp.
 * @property macroF1 Macro F1 trên 3 lớp (Hàm mục tiêu chính).
 * @property homeF1 F1-Score của lớp HOME_WIN.
 * @property drawF1 F1-Score của lớp DRAW.
 * @property awayF1 F1-Score của lớp AWAY_WIN.
 * @property drawPrecision Precision của lớp DRAW.
 * @property drawRecall Recall của lớp DRAW.
 * @property eligible Đánh dấu cấu hình có thỏa mãn ràng buộc F1 Home/Away không suy giảm quá f1Tolerance.
 * @property homeF1Delta Độ lệch Home F1 so với Baseline (Candidate B - Baseline).
 * @property awayF1Delta Độ lệch Away F1 so với Baseline (Candidate B - Baseline).
 * @property macroF1Delta Độ lệch Macro F1 so với Baseline (Candidate B - Baseline).
 * @property confusionMatrix Ma trận nhầm lẫn 3 chiều.
 */
data class CandidateBGridEvaluationRecord(
    val config: DynamicDrawPriorConfig,
    val sampleCount: Int,
    val accuracy: Double,
    val macroPrecision: Double,
    val macroRecall: Double,
    val macroF1: Double,
    val homeF1: Double,
    val drawF1: Double,
    val awayF1: Double,
    val drawPrecision: Double,
    val drawRecall: Double,
    val eligible: Boolean,
    val homeF1Delta: Double,
    val awayF1Delta: Double,
    val macroF1Delta: Double,
    val confusionMatrix: ConfusionMatrix3Way
)

/**
 * Kết quả tổng thể của phiên quét siêu tham số Grid Search cho Candidate B.
 *
 * @property baselineResult Kết quả đối chứng Baseline trên cùng tập dữ liệu.
 * @property totalConfigurations Tổng số cấu hình đã quét (thông thường là 64).
 * @property eligibleConfigurationsCount Số lượng cấu hình đạt chuẩn ràng buộc.
 * @property bestConfiguration Cấu hình tốt nhất được chọn lọc (nếu có).
 * @property allRecords Toàn bộ danh sách bản ghi đánh giá đã được sắp xếp theo thứ tự ưu tiên.
 */
data class CandidateBOptimizationResult(
    val baselineResult: ModelEvaluationResult,
    val totalConfigurations: Int,
    val eligibleConfigurationsCount: Int,
    val bestConfiguration: CandidateBGridEvaluationRecord?,
    val allRecords: List<CandidateBGridEvaluationRecord>
) {
    fun topConfigurations(limit: Int = 10): List<CandidateBGridEvaluationRecord> =
        allRecords.take(limit)

    /**
     * Xuất báo cáo bảng Markdown tổng kết Top cấu hình phục vụ phân tích.
     */
    fun formatReportTable(limit: Int = 10): String {
        val sb = StringBuilder()
        sb.append("| Rank | PD_max | σ_Elo | σ_Form | Macro F1 | Draw F1 | Draw Rec | Home F1 | Away F1 | Acc | Eligible |\n")
        sb.append("|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|\n")
        allRecords.take(limit).forEachIndexed { index, r ->
            val rank = index + 1
            val cfg = r.config
            sb.append(
                String.format(
                    Locale.US,
                    "| #%d | %.2f | %.2f | %.2f | %.4f | %.4f | %.4f | %.4f | %.4f | %.4f | %s |\n",
                    rank, cfg.maxDrawProb, cfg.eloSigma, cfg.formSigma,
                    r.macroF1, r.drawF1, r.drawRecall, r.homeF1, r.awayF1, r.accuracy,
                    if (r.eligible) "YES" else "NO"
                )
            )
        }
        return sb.toString()
    }
}
