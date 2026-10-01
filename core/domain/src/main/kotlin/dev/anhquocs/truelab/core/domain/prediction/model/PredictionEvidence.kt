package dev.anhquocs.truelab.core.domain.prediction.model

/**
 * Chi tiết bằng chứng và đóng góp của từng tín hiệu dự đoán (Signal Evidence).
 *
 * @property name Tên tín hiệu (Elo, Form, Odds, Goals, H2H, Home Advantage).
 * @property homeProb Xác suất đội nhà thắng từ riêng tín hiệu này.
 * @property drawProb Xác suất hòa từ riêng tín hiệu này.
 * @property awayProb Xác suất đội khách thắng từ riêng tín hiệu này.
 * @property rawWeight Trọng số cấu hình danh định của tín hiệu (từ [PredictionWeightConfig]).
 * @property effectiveWeight Trọng số thực tế sau khi chuẩn hóa theo tổng trọng số các tín hiệu khả dụng.
 * @property contributionHome Đóng góp của tín hiệu vào xác suất chung của đội nhà (effectiveWeight * homeProb).
 * @property contributionDraw Đóng góp của tín hiệu vào xác suất hòa chung (effectiveWeight * drawProb).
 * @property contributionAway Đóng góp của tín hiệu vào xác suất chung của đội khách (effectiveWeight * awayProb).
 * @property isAvailable Trạng thái khả dụng của tín hiệu (false nếu thiếu dữ liệu odds hoặc không áp dụng).
 * @property details Thông tin chi tiết các giá trị đầu vào cụ thể (Elo ratings, form scores, odds snapshot, goals, h2h counts).
 */
data class SignalEvidence(
    val name: String,
    val homeProb: Double,
    val drawProb: Double,
    val awayProb: Double,
    val rawWeight: Double,
    val effectiveWeight: Double,
    val contributionHome: Double,
    val contributionDraw: Double,
    val contributionAway: Double,
    val isAvailable: Boolean = true,
    val details: Map<String, String> = emptyMap()
)

/**
 * Tập hợp toàn bộ bằng chứng và dữ liệu đầu vào thực tế được Prediction Engine sử dụng.
 */
data class PredictionEvidence(
    val elo: SignalEvidence,
    val form: SignalEvidence,
    val odds: SignalEvidence,
    val goals: SignalEvidence,
    val h2h: SignalEvidence,
    val homeAdvantage: SignalEvidence,
    val totalWeight: Double,
    val signals: List<SignalEvidence> = listOf(elo, form, odds, goals, h2h, homeAdvantage)
)
