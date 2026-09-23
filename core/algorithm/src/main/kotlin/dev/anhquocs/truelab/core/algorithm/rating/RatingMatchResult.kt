package dev.anhquocs.truelab.core.algorithm.rating

/**
 * Đóng gói kết quả cập nhật Elo cho cả hai đội sau một trận đấu.
 *
 * @property newRatingA Điểm Elo mới của Đội A sau cập nhật.
 * @property newRatingB Điểm Elo mới của Đội B sau cập nhật.
 * @property ratingChange Lượng điểm biến thiên của Đội A (Đội B biến thiên ngược dấu: -ratingChange).
 * @property expectedScoreA Xác suất kỳ vọng chiến thắng của Đội A (trong khoảng (0.0, 1.0)).
 * @property expectedScoreB Xác suất kỳ vọng chiến thắng của Đội B (bằng 1.0 - expectedScoreA).
 */
data class RatingMatchResult(
    val newRatingA: Double,
    val newRatingB: Double,
    val ratingChange: Double,
    val expectedScoreA: Double,
    val expectedScoreB: Double
)
