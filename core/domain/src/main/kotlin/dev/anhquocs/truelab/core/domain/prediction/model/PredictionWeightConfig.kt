package dev.anhquocs.truelab.core.domain.prediction.model

import kotlin.math.abs

/**
 * Chế độ lựa chọn tín hiệu thứ 6 trong Prediction Pipeline.
 *
 * @property REST_ADVANTAGE Chế độ mặc định trong Production (Ưu thế ngày nghỉ).
 * @property HOME_ADVANTAGE Chế độ phục vụ Evaluation / Benchmark Baseline đối soát.
 */
enum class SixthSignalMode {
    REST_ADVANTAGE,
    HOME_ADVANTAGE
}

/**
 * Cấu hình trọng số và các tham số mô hình hóa tín hiệu dự đoán theo đặc tả FR-14 và Domain Signal Modeling Spec.
 *
 * @property formWeight Trọng số tín hiệu Phong độ (mặc định 0.25 - 25%).
 * @property eloWeight Trọng số tín hiệu Elo Rating (mặc định 0.20 - 20%).
 * @property oddsWeight Trọng số tín hiệu Odds Nhà cái (mặc định 0.20 - 20%).
 * @property goalsWeight Trọng số tín hiệu Hiệu suất bàn thắng (mặc định 0.15 - 15%).
 * @property h2hWeight Trọng số tín hiệu Lịch sử đối đầu (mặc định 0.10 - 10%).
 * @property restAdvantageWeight Trọng số tín hiệu Ưu thế ngày nghỉ (mặc định 0.10 - 10%).
 * @property homeAdvantageWeight Trọng số tín hiệu Ưu thế sân nhà dùng cho Baseline Benchmark (mặc định 0.10 - 10%).
 * @property sixthSignalMode Chế độ tín hiệu thứ 6 (mặc định REST_ADVANTAGE).
 * @property baselineDrawProb Xác suất hòa cơ sở mặc định (mặc định 0.26 - Heuristic).
 * @property formSmoothingEpsilon Hằng số làm mịn phong độ (mặc định 0.10 - Heuristic).
 * @property h2hPriorK Trọng số mẫu giả định cho Laplace smoothing H2H (mặc định 3.0 - Heuristic).
 * @property h2hPriorHome Xác suất thắng sân nhà tiên nghiệm H2H (mặc định 0.45).
 * @property h2hPriorDraw Xác suất hòa tiên nghiệm H2H (mặc định 0.27).
 * @property h2hPriorAway Xác suất khách thắng tiên nghiệm H2H (mặc định 0.28).
 * @property restAdvantageSensitivity Hệ số độ nhạy ngày nghỉ cho Rest Advantage (mặc định 3.0 ngày).
 * @property restAdvantageMaxShift Độ lệch xác suất tối đa cho Rest Advantage (mặc định 0.09).
 * @property homeAdvantageProbHome Xác suất đội nhà thắng cơ sở cho HomeAdvantage Baseline (mặc định 0.46).
 * @property homeAdvantageProbDraw Xác suất hòa cơ sở cho HomeAdvantage Baseline (mặc định 0.26).
 * @property homeAdvantageProbAway Xác suất đội khách thắng cơ sở cho HomeAdvantage Baseline (mặc định 0.28).
 * @property goalsSensitivity Hệ số độ nhạy chênh lệch bàn thắng kỳ vọng (mặc định 0.15).
 * @property goalsMinProbHome Xác suất thắng tối thiểu của đội nhà trong Goals Signal (mặc định 0.05).
 * @property goalsMaxProbHome Xác suất thắng tối đa của đội nhà trong Goals Signal (mặc định 0.69).
 */
data class PredictionWeightConfig(
    val formWeight: Double = 0.25,
    val eloWeight: Double = 0.20,
    val oddsWeight: Double = 0.20,
    val goalsWeight: Double = 0.15,
    val h2hWeight: Double = 0.10,
    val restAdvantageWeight: Double = 0.10,
    val homeAdvantageWeight: Double = restAdvantageWeight,
    val sixthSignalMode: SixthSignalMode = SixthSignalMode.REST_ADVANTAGE,
    val baselineDrawProb: Double = 0.26,
    val formSmoothingEpsilon: Double = 0.10,
    val h2hPriorK: Double = 3.0,
    val h2hPriorHome: Double = 0.45,
    val h2hPriorDraw: Double = 0.27,
    val h2hPriorAway: Double = 0.28,
    val restAdvantageSensitivity: Double = 3.0,
    val restAdvantageMaxShift: Double = 0.09,
    val homeAdvantageProbHome: Double = 0.46,
    val homeAdvantageProbDraw: Double = 0.26,
    val homeAdvantageProbAway: Double = 0.28,
    val goalsSensitivity: Double = 0.15,
    val goalsMinProbHome: Double = 0.05,
    val goalsMaxProbHome: Double = 0.69
) {
    init {
        require(formWeight >= 0.0 && formWeight.isFinite()) { "formWeight must be non-negative and finite" }
        require(eloWeight >= 0.0 && eloWeight.isFinite()) { "eloWeight must be non-negative and finite" }
        require(oddsWeight >= 0.0 && oddsWeight.isFinite()) { "oddsWeight must be non-negative and finite" }
        require(goalsWeight >= 0.0 && goalsWeight.isFinite()) { "goalsWeight must be non-negative and finite" }
        require(h2hWeight >= 0.0 && h2hWeight.isFinite()) { "h2hWeight must be non-negative and finite" }
        require(restAdvantageWeight >= 0.0 && restAdvantageWeight.isFinite()) { "restAdvantageWeight must be non-negative and finite" }
        require(homeAdvantageWeight >= 0.0 && homeAdvantageWeight.isFinite()) { "homeAdvantageWeight must be non-negative and finite" }

        val activeSixthWeight = if (sixthSignalMode == SixthSignalMode.REST_ADVANTAGE) restAdvantageWeight else homeAdvantageWeight
        val totalWeight = formWeight + eloWeight + oddsWeight + goalsWeight + h2hWeight + activeSixthWeight
        require(totalWeight > 0.0 && totalWeight.isFinite()) { "Total weight must be positive and finite" }

        require(baselineDrawProb in 0.0..1.0 && baselineDrawProb.isFinite()) { "baselineDrawProb must be in [0.0, 1.0]" }
        require(formSmoothingEpsilon >= 0.0 && formSmoothingEpsilon.isFinite()) { "formSmoothingEpsilon must be non-negative and finite" }
        require(h2hPriorK >= 0.0 && h2hPriorK.isFinite()) { "h2hPriorK must be non-negative and finite" }
        require(restAdvantageSensitivity > 0.0 && restAdvantageSensitivity.isFinite()) { "restAdvantageSensitivity must be positive and finite" }
        require(restAdvantageMaxShift >= 0.0 && restAdvantageMaxShift <= (1.0 - baselineDrawProb) / 2.0 && restAdvantageMaxShift.isFinite()) {
            "restAdvantageMaxShift must be within [0.0, (1 - baselineDrawProb)/2]"
        }

        val h2hPriorSum = h2hPriorHome + h2hPriorDraw + h2hPriorAway
        require(abs(h2hPriorSum - 1.0) <= 1e-4) { "H2H prior probabilities must sum to approximately 1.0" }

        val homeAdvSum = homeAdvantageProbHome + homeAdvantageProbDraw + homeAdvantageProbAway
        require(abs(homeAdvSum - 1.0) <= 1e-4) { "Home advantage probabilities must sum to approximately 1.0" }

        require(goalsSensitivity >= 0.0 && goalsSensitivity.isFinite()) { "goalsSensitivity must be non-negative and finite" }
        require(goalsMinProbHome >= 0.0 && goalsMaxProbHome <= (1.0 - baselineDrawProb + 1e-4) && goalsMinProbHome <= goalsMaxProbHome) {
            "Goals min/max probabilities must be valid bounds within [0, 1 - baselineDrawProb]"
        }
    }

    companion object {
        val DEFAULT = PredictionWeightConfig()
    }
}
