package dev.anhquocs.truelab.core.domain.team.usecase

import dev.anhquocs.truelab.core.algorithm.rating.EloRatingCalculator
import dev.anhquocs.truelab.core.algorithm.rating.RatingCalculator
import dev.anhquocs.truelab.core.algorithm.rating.RatingMatchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CalculateEloRatingUseCaseTest {

    private lateinit var useCase: CalculateEloRatingUseCase

    @Before
    fun setUp() {
        useCase = CalculateEloRatingUseCase(
            ratingCalculator = EloRatingCalculator()
        )
    }

    @Test
    fun `equal ratings produce expected score of 0_5`() {
        val expected = useCase.calculateExpectedScore(1500.0, 1500.0)
        assertEquals(0.5, expected, 1e-6)
    }

    @Test
    fun `higher rated team has expected score greater than 0_5`() {
        val expected = useCase.calculateExpectedScore(1600.0, 1400.0)
        assertTrue(expected > 0.5)
        // 1.0 / (1.0 + 10^(-200/400)) = 1 / (1 + 10^-0.5) ~= 0.7597469
        assertEquals(0.7597469, expected, 1e-4)
    }

    @Test
    fun `lower rated team has expected score less than 0_5`() {
        val expected = useCase.calculateExpectedScore(1400.0, 1600.0)
        assertTrue(expected < 0.5)
        assertEquals(1.0 - 0.7597469, expected, 1e-4)
    }

    @Test
    fun `home win increases home rating and decreases away rating`() {
        val result = useCase.calculateMatchResult(
            ratingHome = 1500.0,
            ratingAway = 1500.0,
            homeScore = 2,
            awayScore = 0
        )

        assertTrue(result.newRatingA > 1500.0)
        assertTrue(result.newRatingB < 1500.0)
        assertTrue(result.ratingChange > 0.0)
        assertEquals(1500.0 + 16.0, result.newRatingA, 1e-6) // K=32 * (1.0 - 0.5) = +16.0
        assertEquals(1500.0 - 16.0, result.newRatingB, 1e-6)
        assertEquals(16.0, result.ratingChange, 1e-6)
    }

    @Test
    fun `away win decreases home rating and increases away rating`() {
        val result = useCase.calculateMatchResult(
            ratingHome = 1500.0,
            ratingAway = 1500.0,
            homeScore = 0,
            awayScore = 1
        )

        assertTrue(result.newRatingA < 1500.0)
        assertTrue(result.newRatingB > 1500.0)
        assertTrue(result.ratingChange < 0.0)
        assertEquals(1500.0 - 16.0, result.newRatingA, 1e-6) // K=32 * (0.0 - 0.5) = -16.0
        assertEquals(1500.0 + 16.0, result.newRatingB, 1e-6)
        assertEquals(-16.0, result.ratingChange, 1e-6)
    }

    @Test
    fun `draw with equal ratings preserves ratings and maintains zero-sum`() {
        val result = useCase.calculateMatchResult(
            ratingHome = 1500.0,
            ratingAway = 1500.0,
            homeScore = 1,
            awayScore = 1
        )

        assertEquals(0.0, result.ratingChange, 1e-6)
        assertEquals(1500.0, result.newRatingA, 1e-6)
        assertEquals(1500.0, result.newRatingB, 1e-6)
        assertEquals(3000.0, result.newRatingA + result.newRatingB, 1e-6)
    }

    @Test
    fun `draw with unequal ratings adjusts rating toward underdog`() {
        // Stronger home team draws with weaker away team
        val result = useCase.calculateMatchResult(
            ratingHome = 1600.0,
            ratingAway = 1400.0,
            homeScore = 2,
            awayScore = 2
        )

        // Strong team expected ~0.76, achieved 0.5 -> ratingChange < 0
        assertTrue(result.ratingChange < 0.0)
        assertTrue(result.newRatingA < 1600.0)
        assertTrue(result.newRatingB > 1400.0)
        assertEquals(3000.0, result.newRatingA + result.newRatingB, 1e-6)
    }

    @Test
    fun `explicit kFactor scales rating change proportionally`() {
        val defaultResult = useCase.calculateMatchResult(
            ratingHome = 1500.0,
            ratingAway = 1500.0,
            homeScore = 2,
            awayScore = 1,
            kFactor = 32.0
        )

        val halfKResult = useCase.calculateMatchResult(
            ratingHome = 1500.0,
            ratingAway = 1500.0,
            homeScore = 2,
            awayScore = 1,
            kFactor = 16.0
        )

        assertEquals(16.0, defaultResult.ratingChange, 1e-6)
        assertEquals(8.0, halfKResult.ratingChange, 1e-6)
        assertEquals(defaultResult.ratingChange / 2.0, halfKResult.ratingChange, 1e-6)
    }

    @Test
    fun `omitting kFactor uses DEFAULT_K of 32_0`() {
        val resultWithoutK = useCase.calculateMatchResult(
            ratingHome = 1500.0,
            ratingAway = 1500.0,
            homeScore = 3,
            awayScore = 1
        )

        val resultWithExplicitDefaultK = useCase.calculateMatchResult(
            ratingHome = 1500.0,
            ratingAway = 1500.0,
            homeScore = 3,
            awayScore = 1,
            kFactor = RatingCalculator.DEFAULT_K
        )

        assertEquals(resultWithExplicitDefaultK.ratingChange, resultWithoutK.ratingChange, 1e-6)
        assertEquals(resultWithExplicitDefaultK.newRatingA, resultWithoutK.newRatingA, 1e-6)
        assertEquals(resultWithExplicitDefaultK.newRatingB, resultWithoutK.newRatingB, 1e-6)
    }

    @Test
    fun `preserves absolute zero-sum conservation across all outcomes`() {
        val ratingHome = 1750.0
        val ratingAway = 1520.0
        val initialTotal = ratingHome + ratingAway

        // Win
        val winResult = useCase.calculateMatchResult(ratingHome, ratingAway, 2, 0)
        assertEquals(initialTotal, winResult.newRatingA + winResult.newRatingB, 1e-9)
        assertEquals(0.0, winResult.ratingChange + (-winResult.ratingChange), 1e-9)

        // Draw
        val drawResult = useCase.calculateMatchResult(ratingHome, ratingAway, 1, 1)
        assertEquals(initialTotal, drawResult.newRatingA + drawResult.newRatingB, 1e-9)

        // Loss
        val lossResult = useCase.calculateMatchResult(ratingHome, ratingAway, 0, 3)
        assertEquals(initialTotal, lossResult.newRatingA + lossResult.newRatingB, 1e-9)
    }

    @Test
    fun `delegates correctly to injected RatingCalculator`() {
        var calculateMatchCalled = false
        val customCalculator = object : RatingCalculator {
            override fun expectedScore(rating: Double, opponentRating: Double): Double = 0.42
            override fun updateRating(rating: Double, opponentRating: Double, actualScore: Double): Double = 1510.0
            override fun updateRating(rating: Double, opponentRating: Double, actualScore: Double, kFactor: Double): Double = 1510.0
            override fun calculateMatch(ratingA: Double, ratingB: Double, actualScoreA: Double): RatingMatchResult =
                calculateMatch(ratingA, ratingB, actualScoreA, RatingCalculator.DEFAULT_K)
            override fun calculateMatch(ratingA: Double, ratingB: Double, actualScoreA: Double, kFactor: Double): RatingMatchResult {
                calculateMatchCalled = true
                assertEquals(1.0, actualScoreA, 1e-6) // homeScore 2 > awayScore 1
                return RatingMatchResult(1510.0, 1490.0, 10.0, 0.42, 0.58)
            }
        }

        val customUseCase = CalculateEloRatingUseCase(customCalculator)
        val expected = customUseCase.calculateExpectedScore(1500.0, 1500.0)
        assertEquals(0.42, expected, 1e-6)

        val matchResult = customUseCase.calculateMatchResult(1500.0, 1500.0, 2, 1)
        assertTrue(calculateMatchCalled)
        assertEquals(1510.0, matchResult.newRatingA, 1e-6)
    }

    @Test
    fun `propagates IllegalArgumentException when invalid rating is passed`() {
        assertThrows(IllegalArgumentException::class.java) {
            useCase.calculateExpectedScore(Double.NaN, 1500.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            useCase.calculateExpectedScore(1500.0, Double.POSITIVE_INFINITY)
        }
        assertThrows(IllegalArgumentException::class.java) {
            useCase.calculateMatchResult(Double.NaN, 1500.0, 1, 0)
        }
    }

    @Test
    fun `propagates IllegalArgumentException when kFactor is non-positive or invalid`() {
        assertThrows(IllegalArgumentException::class.java) {
            useCase.calculateMatchResult(1500.0, 1500.0, 1, 0, kFactor = 0.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            useCase.calculateMatchResult(1500.0, 1500.0, 1, 0, kFactor = -10.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            useCase.calculateMatchResult(1500.0, 1500.0, 1, 0, kFactor = Double.NaN)
        }
    }
}
