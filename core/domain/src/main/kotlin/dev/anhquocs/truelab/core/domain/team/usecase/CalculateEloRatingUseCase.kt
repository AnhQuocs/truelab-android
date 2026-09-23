package dev.anhquocs.truelab.core.domain.team.usecase

import dev.anhquocs.truelab.core.algorithm.rating.EloRatingCalculator
import dev.anhquocs.truelab.core.algorithm.rating.RatingCalculator
import dev.anhquocs.truelab.core.algorithm.rating.RatingMatchResult

/**
 * UseCase tính toán xác suất kỳ vọng và cập nhật điểm Elo đối đầu giữa 2 đội bóng dựa trên thuật toán Standard Elo (Phase 6).
 *
 * UseCase này là pure computation, độc lập với Repository và Data persistence, không gây side-effect.
 *
 * @param ratingCalculator Thuật toán tính điểm Elo (mặc định là [EloRatingCalculator]).
 */
class CalculateEloRatingUseCase(
    private val ratingCalculator: RatingCalculator = EloRatingCalculator()
) {

    /**
     * Tính xác suất điểm số kỳ vọng (Expected Score) của Đội A khi đối đầu với Đội B.
     *
     * @param ratingA Điểm Elo hiện tại của Đội A.
     * @param ratingB Điểm Elo hiện tại của Đội B.
     * @return Xác suất điểm số kỳ vọng trong khoảng (0.0, 1.0).
     */
    fun calculateExpectedScore(
        ratingA: Double,
        ratingB: Double
    ): Double = ratingCalculator.expectedScore(ratingA, ratingB)

    /**
     * Tính toán cập nhật điểm Elo đối kháng sau khi trận đấu kết thúc (Zero-Sum Conservation).
     *
     * Ánh xạ tỷ số trận đấu:
     * - homeScore > awayScore -> actualScoreHome = 1.0 (Home Win)
     * - homeScore == awayScore -> actualScoreHome = 0.5 (Draw)
     * - homeScore < awayScore -> actualScoreHome = 0.0 (Away Win)
     *
     * @param ratingHome Điểm Elo hiện tại của đội nhà.
     * @param ratingAway Điểm Elo hiện tại của đội khách.
     * @param homeScore Số bàn thắng thực tế của đội nhà.
     * @param awayScore Số bàn thắng thực tế của đội khách.
     * @param kFactor Hệ số K-factor (mặc định là [RatingCalculator.DEFAULT_K] = 32.0).
     * @return [RatingMatchResult] chứa điểm mới của cả hai đội và lượng biến thiên ratingChange.
     */
    fun calculateMatchResult(
        ratingHome: Double,
        ratingAway: Double,
        homeScore: Int,
        awayScore: Int,
        kFactor: Double = RatingCalculator.DEFAULT_K
    ): RatingMatchResult {
        val actualScoreHome = when {
            homeScore > awayScore -> 1.0
            homeScore == awayScore -> 0.5
            else -> 0.0
        }
        return ratingCalculator.calculateMatch(ratingHome, ratingAway, actualScoreHome, kFactor)
    }
}
