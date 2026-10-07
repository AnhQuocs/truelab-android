package dev.anhquocs.truelab.core.domain.prediction.model

/**
 * Cấu hình thực nghiệm cho Candidate C (Asian Handicap & Over/Under Incremental Signals).
 *
 * @property isEnabled Bật/tắt chế độ tích hợp Candidate C trong experimental pipeline.
 * @property ahWeight Trọng số thực nghiệm của tín hiệu Asian Handicap (w_c1).
 * @property ouWeight Trọng số thực nghiệm của tín hiệu Over/Under (w_c2).
 * @property alpha Hệ số nhạy giá cho Asian Handicap (α).
 * @property beta Hệ số độ dốc Logistic cho Asian Handicap (β).
 * @property gamma Hệ số suy giảm hòa cho Asian Handicap (γ).
 * @property eta Hệ số nhạy giá cho Over/Under (η).
 * @property kappa Hệ số nhạy hòa theo tổng bàn thắng kỳ vọng cho Over/Under (κ).
 * @property minDrawProb Giới hạn kẹp xác suất hòa dưới cho Over/Under.
 * @property maxDrawProb Giới hạn kẹp xác suất hòa trên cho Over/Under.
 */
data class CandidateCConfig(
    val isEnabled: Boolean = false,
    val ahWeight: Double = 0.20,
    val ouWeight: Double = 0.03,
    val alpha: Double = 0.80,
    val beta: Double = 1.40,
    val gamma: Double = 0.10,
    val eta: Double = 0.20,
    val kappa: Double = 0.04,
    val minDrawProb: Double = 0.16,
    val maxDrawProb: Double = 0.38
) {
    init {
        require(ahWeight >= 0.0 && ahWeight.isFinite()) { "ahWeight must be non-negative and finite" }
        require(ouWeight >= 0.0 && ouWeight.isFinite()) { "ouWeight must be non-negative and finite" }
        require(alpha >= 0.0 && alpha.isFinite()) { "alpha must be non-negative and finite" }
        require(beta > 0.0 && beta.isFinite()) { "beta must be positive and finite" }
        require(gamma >= 0.0 && gamma.isFinite()) { "gamma must be non-negative and finite" }
        require(eta >= 0.0 && eta.isFinite()) { "eta must be non-negative and finite" }
        require(kappa >= 0.0 && kappa.isFinite()) { "kappa must be non-negative and finite" }
        require(minDrawProb in 0.0..1.0 && maxDrawProb in 0.0..1.0 && minDrawProb <= maxDrawProb) {
            "minDrawProb and maxDrawProb must be valid probabilities with min <= max"
        }
    }

    companion object {
        val DISABLED = CandidateCConfig(isEnabled = false)
        val DEFAULT_CALIBRATED = CandidateCConfig(
            isEnabled = true,
            ahWeight = 0.20,
            ouWeight = 0.03,
            alpha = 0.80,
            beta = 1.60,
            gamma = 0.50,
            eta = 0.20,
            kappa = 0.12
        )
    }
}
