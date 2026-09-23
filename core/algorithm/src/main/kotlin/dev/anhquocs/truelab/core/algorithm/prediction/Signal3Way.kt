package dev.anhquocs.truelab.core.algorithm.prediction

/**
 * Đóng gói một tín hiệu xác suất 3 chiều cho một trận đấu kèm trọng số.
 *
 * @property homeProb Xác suất đội nhà thắng (thuộc [0.0, 1.0] và hữu hạn).
 * @property drawProb Xác suất hai đội hòa (thuộc [0.0, 1.0] và hữu hạn).
 * @property awayProb Xác suất đội khách thắng (thuộc [0.0, 1.0] và hữu hạn).
 * @property weight Trọng số của tín hiệu (phải >= 0.0 và hữu hạn).
 * @property name Tên hoặc nhãn mô tả tín hiệu (tùy chọn, mặc định rỗng).
 */
data class Signal3Way(
    val homeProb: Double,
    val drawProb: Double,
    val awayProb: Double,
    val weight: Double,
    val name: String = ""
)
