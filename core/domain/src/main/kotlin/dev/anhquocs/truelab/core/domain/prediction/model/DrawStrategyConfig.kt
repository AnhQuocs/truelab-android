package dev.anhquocs.truelab.core.domain.prediction.model

/**
 * Cấu hình tham số cho chiến lược phân ngưỡng chênh lệch (Candidate A: Decision Margin).
 *
 * @property deltaMargin Ngưỡng chênh lệch tuyệt đối |P_Home - P_Away| (mặc định 0.04 - Đóng băng Calibration/Test).
 * @property thetaMinProb Ngưỡng xác suất hòa tối thiểu P_Draw (mặc định 0.255 - Đóng băng Calibration/Test).
 */
data class DrawMarginConfig(
    val deltaMargin: Double = 0.04,
    val thetaMinProb: Double = 0.255
) {
    init {
        require(deltaMargin > 0.0 && deltaMargin.isFinite()) { "deltaMargin must be positive and finite, but was $deltaMargin" }
        require(thetaMinProb in 0.0..1.0 && thetaMinProb.isFinite()) { "thetaMinProb must be in range [0.0, 1.0], but was $thetaMinProb" }
    }
}

/**
 * Cấu hình tham số cho chiến lược tiên nghiệm hòa động (Candidate B: Dynamic Draw Prior).
 *
 * @property maxDrawProb Xác suất hòa cực đại khi hai đội cân bằng tuyệt đối (Δ = 0) (mặc định 0.38 - Đóng băng Calibration).
 * @property minDrawProb Xác suất hòa cực tiểu khi chênh lệch sức mạnh lớn (mặc định 0.12 - Đóng băng Calibration).
 * @property eloSigma Độ rộng hàm Gauss theo thang Elo (mặc định 1.0 - Đóng băng Calibration).
 * @property formSigma Độ rộng hàm Gauss theo thang Phong độ (mặc định 0.30 - Đóng băng Calibration).
 */
data class DynamicDrawPriorConfig(
    val maxDrawProb: Double = 0.38,
    val minDrawProb: Double = 0.12,
    val eloSigma: Double = 1.0,
    val formSigma: Double = 0.30
) {
    init {
        require(maxDrawProb in 0.0..1.0 && maxDrawProb.isFinite()) { "maxDrawProb must be in range [0.0, 1.0], but was $maxDrawProb" }
        require(minDrawProb in 0.0..1.0 && minDrawProb.isFinite()) { "minDrawProb must be in range [0.0, 1.0], but was $minDrawProb" }
        require(minDrawProb <= maxDrawProb) { "minDrawProb ($minDrawProb) must be <= maxDrawProb ($maxDrawProb)" }
        require(eloSigma > 0.0 && eloSigma.isFinite()) { "eloSigma must be positive and finite, but was $eloSigma" }
        require(formSigma > 0.0 && formSigma.isFinite()) { "formSigma must be positive and finite, but was $formSigma" }
    }
}

/**
 * Cấu hình tổng hợp chiến lược mô hình hóa kết quả Hòa cho Prediction Pipeline.
 *
 * @property strategy Chiến lược được kích hoạt (mặc định [DrawModelingStrategy.DECISION_MARGIN]).
 * @property marginConfig Cấu hình tham số cho Candidate A.
 * @property dynamicPriorConfig Cấu hình tham số cho Candidate B.
 */
data class DrawStrategyConfig(
    val strategy: DrawModelingStrategy = DrawModelingStrategy.DECISION_MARGIN,
    val marginConfig: DrawMarginConfig = DrawMarginConfig(),
    val dynamicPriorConfig: DynamicDrawPriorConfig = DynamicDrawPriorConfig()
) {
    companion object {
        val DEFAULT = DrawStrategyConfig(strategy = DrawModelingStrategy.DECISION_MARGIN)
    }
}
