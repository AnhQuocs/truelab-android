package dev.anhquocs.truelab.core.algorithm.prediction

/**
 * Đóng gói một đặc trưng số học đơn lẻ cùng trọng số của nó.
 *
 * @property score Điểm số đã chuẩn hóa của đặc trưng (phải thuộc [0.0, 1.0] và hữu hạn).
 * @property weight Trọng số của đặc trưng (phải >= 0.0 và hữu hạn).
 * @property name Tên hoặc nhãn mô tả đặc trưng (tùy chọn, mặc định rỗng).
 */
data class WeightedFeature(
    val score: Double,
    val weight: Double,
    val name: String = ""
)
