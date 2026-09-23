package dev.anhquocs.truelab.core.algorithm.rating

import kotlin.math.pow

/**
 * Triển khai thuật toán đánh giá Elo tiêu chuẩn (Standard Elo Rating System).
 *
 * - Độ phức tạp thời gian: O(1) tuyệt đối cho mọi hàm.
 * - Độ phức tạp không gian phụ trợ (Auxiliary Space): O(1) tuyệt đối.
 * - Bảo toàn nguyên tắc Zero-sum conservation tuyệt đối giữa hai đội khi cùng sử dụng hệ số K.
 * - Độc lập hoàn toàn với Android SDK, Room Database và Domain Entities.
 *
 * @param defaultKFactor Hệ số K-factor mặc định (mặc định là [RatingCalculator.DEFAULT_K] = 32.0).
 */
class EloRatingCalculator(
    private val defaultKFactor: Double = RatingCalculator.DEFAULT_K
) : RatingCalculator {

    init {
        require(defaultKFactor > 0.0 && defaultKFactor.isFinite()) {
            "defaultKFactor must be greater than 0 and finite, but was $defaultKFactor"
        }
    }

    override fun expectedScore(rating: Double, opponentRating: Double): Double {
        validateRating(rating, "rating")
        validateRating(opponentRating, "opponentRating")

        val exponent = (opponentRating - rating) / 400.0
        return 1.0 / (1.0 + 10.0.pow(exponent))
    }

    override fun updateRating(
        rating: Double,
        opponentRating: Double,
        actualScore: Double
    ): Double = updateRating(rating, opponentRating, actualScore, defaultKFactor)

    override fun updateRating(
        rating: Double,
        opponentRating: Double,
        actualScore: Double,
        kFactor: Double
    ): Double {
        validateActualScore(actualScore)
        validateKFactor(kFactor)

        val expScore = expectedScore(rating, opponentRating)
        val ratingChange = kFactor * (actualScore - expScore)
        return rating + ratingChange
    }

    override fun calculateMatch(
        ratingA: Double,
        ratingB: Double,
        actualScoreA: Double
    ): RatingMatchResult = calculateMatch(ratingA, ratingB, actualScoreA, defaultKFactor)

    override fun calculateMatch(
        ratingA: Double,
        ratingB: Double,
        actualScoreA: Double,
        kFactor: Double
    ): RatingMatchResult {
        validateActualScore(actualScoreA)
        validateKFactor(kFactor)

        val expScoreA = expectedScore(ratingA, ratingB)
        val expScoreB = 1.0 - expScoreA
        val ratingChange = kFactor * (actualScoreA - expScoreA)

        return RatingMatchResult(
            newRatingA = ratingA + ratingChange,
            newRatingB = ratingB - ratingChange,
            ratingChange = ratingChange,
            expectedScoreA = expScoreA,
            expectedScoreB = expScoreB
        )
    }

    private fun validateRating(rating: Double, name: String) {
        require(rating.isFinite()) {
            "$name must be finite, but was $rating"
        }
    }

    private fun validateActualScore(score: Double) {
        require(score in 0.0..1.0 && score.isFinite()) {
            "actualScore must be in range [0.0, 1.0] and finite, but was $score"
        }
    }

    private fun validateKFactor(k: Double) {
        require(k > 0.0 && k.isFinite()) {
            "kFactor must be greater than 0 and finite, but was $k"
        }
    }
}
