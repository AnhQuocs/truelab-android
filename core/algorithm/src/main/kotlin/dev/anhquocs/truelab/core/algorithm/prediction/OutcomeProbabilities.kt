package dev.anhquocs.truelab.core.algorithm.prediction

/**
 * Đóng gói kết quả phân phối xác suất và nhận định dự đoán.
 *
 * @property homeWinProb Xác suất chiến thắng của đội nhà trong đoạn [0.0, 1.0].
 * @property drawProb Xác suất kết quả hòa trong đoạn [0.0, 1.0].
 * @property awayWinProb Xác suất chiến thắng của đội khách trong đoạn [0.0, 1.0].
 * @property predictedOutcome Kết quả dự đoán có xác suất cao nhất (hoặc [PredictedOutcome.DRAW] nếu hòa điểm).
 * @property confidenceScore Mức độ tin cậy của dự đoán, bằng max(homeWinProb, drawProb, awayWinProb).
 */
data class OutcomeProbabilities(
    val homeWinProb: Double,
    val drawProb: Double,
    val awayWinProb: Double,
    val predictedOutcome: PredictedOutcome,
    val confidenceScore: Double
)
