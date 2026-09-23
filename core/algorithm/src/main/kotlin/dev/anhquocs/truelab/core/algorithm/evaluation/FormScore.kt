package dev.anhquocs.truelab.core.algorithm.evaluation

/**
 * Đóng gói toàn bộ các chỉ số đánh giá phong độ thi đấu của một đội bóng.
 *
 * @property score Điểm phong độ có trọng số thời gian (Linear Time-Decay) trên thang [0.0, 100.0].
 * @property rawScore Điểm phong độ không trọng số (Unweighted Standard) trên thang [0.0, 100.0].
 * @property matchesCount Số trận thực tế được đưa vào tính toán (m = min(windowSize, dataset.size)).
 * @property wins Số trận thắng trong cửa sổ đánh giá.
 * @property draws Số trận hòa trong cửa sổ đánh giá.
 * @property losses Số trận thua trong cửa sổ đánh giá.
 * @property totalPoints Tổng điểm thực tế giành được (W=3, D=1, L=0).
 * @property maxPoints Điểm tối đa có thể đạt được trong số trận tương ứng (3 * matchesCount).
 */
data class FormScore(
    val score: Double,
    val rawScore: Double,
    val matchesCount: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val totalPoints: Double,
    val maxPoints: Double
)
