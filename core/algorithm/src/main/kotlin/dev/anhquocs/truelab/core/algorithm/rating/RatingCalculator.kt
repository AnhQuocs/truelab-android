package dev.anhquocs.truelab.core.algorithm.rating

/**
 * Interface contract định nghĩa các thuật toán đánh giá sức mạnh tương đối (Elo Rating System).
 */
interface RatingCalculator {

    /**
     * Tính xác suất điểm số kỳ vọng (Expected Score) của Đội A khi đối đầu với Đội B.
     *
     * @param rating Điểm Elo hiện tại của Đội A.
     * @param opponentRating Điểm Elo hiện tại của Đội B.
     * @return Giá trị xác suất kỳ vọng trong khoảng (0.0, 1.0).
     * @throws IllegalArgumentException nếu điểm số là NaN hoặc vô hạn.
     */
    fun expectedScore(
        rating: Double,
        opponentRating: Double
    ): Double

    /**
     * Cập nhật điểm Elo cho một đội dựa trên kết quả thực tế sử dụng hệ số K-factor mặc định của implementation.
     *
     * @param rating Điểm Elo hiện tại của đội.
     * @param opponentRating Điểm Elo hiện tại của đối thủ.
     * @param actualScore Kết quả thực tế (1.0 = Thắng, 0.5 = Hòa, 0.0 = Thua).
     * @return Điểm Elo mới sau cập nhật.
     * @throws IllegalArgumentException nếu actualScore không thuộc [0.0, 1.0] hoặc rating không hữu hạn.
     */
    fun updateRating(
        rating: Double,
        opponentRating: Double,
        actualScore: Double
    ): Double

    /**
     * Cập nhật điểm Elo cho một đội dựa trên kết quả thực tế với hệ số K-factor tùy biến.
     *
     * @param rating Điểm Elo hiện tại của đội.
     * @param opponentRating Điểm Elo hiện tại của đối thủ.
     * @param actualScore Kết quả thực tế (1.0 = Thắng, 0.5 = Hòa, 0.0 = Thua).
     * @param kFactor Hệ số K-factor (phải > 0.0 và hữu hạn).
     * @return Điểm Elo mới sau cập nhật.
     * @throws IllegalArgumentException nếu actualScore không thuộc [0.0, 1.0], kFactor <= 0.0 hoặc rating không hữu hạn.
     */
    fun updateRating(
        rating: Double,
        opponentRating: Double,
        actualScore: Double,
        kFactor: Double
    ): Double

    /**
     * Tính toán cập nhật điểm Elo đồng thời cho cả hai đội sau một trận đấu sử dụng hệ số K-factor mặc định,
     * tự động suy ra actualScoreB = 1.0 - actualScoreA và bảo toàn nguyên tắc zero-sum tuyệt đối.
     *
     * @param ratingA Điểm Elo hiện tại của Đội A.
     * @param ratingB Điểm Elo hiện tại của Đội B.
     * @param actualScoreA Kết quả thực tế của Đội A (1.0 = Thắng, 0.5 = Hòa, 0.0 = Thua).
     * @return Đối tượng [RatingMatchResult] chứa điểm mới của cả hai đội.
     * @throws IllegalArgumentException nếu actualScoreA không thuộc [0.0, 1.0] hoặc rating không hữu hạn.
     */
    fun calculateMatch(
        ratingA: Double,
        ratingB: Double,
        actualScoreA: Double
    ): RatingMatchResult

    /**
     * Tính toán cập nhật điểm Elo đồng thời cho cả hai đội sau một trận đấu với hệ số K-factor tùy biến,
     * tự động suy ra actualScoreB = 1.0 - actualScoreA và bảo toàn nguyên tắc zero-sum tuyệt đối.
     *
     * @param ratingA Điểm Elo hiện tại của Đội A.
     * @param ratingB Điểm Elo hiện tại của Đội B.
     * @param actualScoreA Kết quả thực tế của Đội A (1.0 = Thắng, 0.5 = Hòa, 0.0 = Thua).
     * @param kFactor Hệ số K-factor (phải > 0.0 và hữu hạn).
     * @return Đối tượng [RatingMatchResult] chứa điểm mới của cả hai đội.
     * @throws IllegalArgumentException nếu actualScoreA không thuộc [0.0, 1.0], kFactor <= 0.0 hoặc rating không hữu hạn.
     */
    fun calculateMatch(
        ratingA: Double,
        ratingB: Double,
        actualScoreA: Double,
        kFactor: Double
    ): RatingMatchResult

    companion object {
        const val DEFAULT_K: Double = 32.0
        const val DEFAULT_INITIAL_RATING: Double = 1500.0
    }
}
