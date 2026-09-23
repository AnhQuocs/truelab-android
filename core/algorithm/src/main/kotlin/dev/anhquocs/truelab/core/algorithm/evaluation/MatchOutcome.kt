package dev.anhquocs.truelab.core.algorithm.evaluation

/**
 * Đại diện cho kết quả thi đấu của một đội bóng trong một trận đấu cụ thể.
 *
 * @property points Điểm số quy ước tương ứng theo luật bóng đá chuẩn (Thắng = 3, Hòa = 1, Thua = 0).
 */
enum class MatchOutcome(val points: Double) {
    WIN(3.0),
    DRAW(1.0),
    LOSS(0.0)
}
