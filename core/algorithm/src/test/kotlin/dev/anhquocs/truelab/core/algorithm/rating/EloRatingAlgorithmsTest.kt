package dev.anhquocs.truelab.core.algorithm.rating

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class EloRatingAlgorithmsTest {

    private val calculator: RatingCalculator = EloRatingCalculator()
    private val epsilon = 1e-9

    // --- 1. Expected Score Mathematical Verification ---

    @Test
    fun `expectedScore - equal ratings returns 0_5`() {
        val ratingA = 1500.0
        val ratingB = 1500.0

        val expA = calculator.expectedScore(ratingA, ratingB)
        val expB = calculator.expectedScore(ratingB, ratingA)

        assertEquals(0.5, expA, epsilon)
        assertEquals(0.5, expB, epsilon)
    }

    @Test
    fun `expectedScore - higher rated team has expected score greater than 0_5`() {
        val ratingA = 1700.0
        val ratingB = 1500.0

        // E_A = 1 / (1 + 10^((1500 - 1700) / 400)) = 1 / (1 + 10^(-0.5)) ≈ 0.7597469266
        val expected = 1.0 / (1.0 + 10.0.pow(-0.5))
        val actual = calculator.expectedScore(ratingA, ratingB)

        assertEquals(expected, actual, epsilon)
        assertTrue("Expected score for stronger team must be > 0.5", actual > 0.5)
    }

    @Test
    fun `expectedScore - lower rated team has expected score less than 0_5`() {
        val ratingA = 1300.0
        val ratingB = 1500.0

        // E_A = 1 / (1 + 10^((1500 - 1300) / 400)) = 1 / (1 + 10^(0.5)) ≈ 0.2402530733
        val expected = 1.0 / (1.0 + 10.0.pow(0.5))
        val actual = calculator.expectedScore(ratingA, ratingB)

        assertEquals(expected, actual, epsilon)
        assertTrue("Expected score for weaker team must be < 0.5", actual < 0.5)
    }

    @Test
    fun `expectedScore - probabilities sum to exactly 1_0`() {
        val testPairs = listOf(
            Pair(1500.0, 1500.0),
            Pair(1850.0, 1200.0),
            Pair(900.0, 2100.0),
            Pair(1600.0, 1590.0)
        )

        for ((rA, rB) in testPairs) {
            val expA = calculator.expectedScore(rA, rB)
            val expB = calculator.expectedScore(rB, rA)
            assertEquals("Probabilities must sum to 1.0 for pair ($rA, $rB)", 1.0, expA + expB, epsilon)
        }
    }

    // --- 2. Single Team Rating Update ---

    @Test
    fun `updateRating - win actualScore 1_0 increases rating for winner`() {
        val rating = 1500.0
        val opponent = 1500.0
        val kFactor = 32.0

        // E = 0.5 -> delta = 32 * (1.0 - 0.5) = +16.0
        val newRating = calculator.updateRating(rating, opponent, actualScore = 1.0, kFactor = kFactor)

        assertEquals(1516.0, newRating, epsilon)
        assertTrue("Winner rating must increase", newRating > rating)
    }

    @Test
    fun `updateRating - loss actualScore 0_0 decreases rating for loser`() {
        val rating = 1500.0
        val opponent = 1500.0
        val kFactor = 32.0

        // E = 0.5 -> delta = 32 * (0.0 - 0.5) = -16.0
        val newRating = calculator.updateRating(rating, opponent, actualScore = 0.0, kFactor = kFactor)

        assertEquals(1484.0, newRating, epsilon)
        assertTrue("Loser rating must decrease", newRating < rating)
    }

    @Test
    fun `updateRating - draw actualScore 0_5 between equal teams causes zero rating change`() {
        val rating = 1500.0
        val opponent = 1500.0
        val kFactor = 32.0

        // E = 0.5 -> delta = 32 * (0.5 - 0.5) = 0.0
        val newRating = calculator.updateRating(rating, opponent, actualScore = 0.5, kFactor = kFactor)

        assertEquals(1500.0, newRating, epsilon)
    }

    @Test
    fun `updateRating - draw causes higher rated team to lose rating`() {
        val higherRating = 1700.0
        val lowerRating = 1500.0
        val kFactor = 32.0

        // Higher rated team expected score > 0.5 -> (0.5 - E) < 0 -> rating drops
        val newHigherRating = calculator.updateRating(higherRating, lowerRating, actualScore = 0.5, kFactor = kFactor)

        assertTrue("Higher rated team must lose rating on a draw against weaker team", newHigherRating < higherRating)
    }

    @Test
    fun `updateRating - draw causes lower rated team to gain rating`() {
        val lowerRating = 1300.0
        val higherRating = 1500.0
        val kFactor = 32.0

        // Lower rated team expected score < 0.5 -> (0.5 - E) > 0 -> rating gains
        val newLowerRating = calculator.updateRating(lowerRating, higherRating, actualScore = 0.5, kFactor = kFactor)

        assertTrue("Lower rated team must gain rating on a draw against stronger team", newLowerRating > lowerRating)
    }

    // --- 3. Match Calculation & Zero-Sum Conservation ---

    @Test
    fun `calculateMatch - maintains zero-sum rating conservation for all outcomes`() {
        val ratingA = 1650.0
        val ratingB = 1420.0
        val kFactor = 32.0

        val outcomes = listOf(1.0, 0.5, 0.0)

        for (actualScoreA in outcomes) {
            val result = calculator.calculateMatch(ratingA, ratingB, actualScoreA, kFactor)

            // Rating conservation: sum of ratings before equals sum of ratings after
            val initialSum = ratingA + ratingB
            val updatedSum = result.newRatingA + result.newRatingB
            assertEquals("Total rating points must be preserved", initialSum, updatedSum, epsilon)

            // deltaA + deltaB = 0
            val deltaA = result.newRatingA - ratingA
            val deltaB = result.newRatingB - ratingB
            assertEquals("Deltas must be opposite and sum to zero", 0.0, deltaA + deltaB, epsilon)
            assertEquals("ratingChange must match deltaA", deltaA, result.ratingChange, epsilon)

            // Expected scores must sum to 1.0
            assertEquals(1.0, result.expectedScoreA + result.expectedScoreB, epsilon)
        }
    }

    @Test
    fun `calculateMatch - scales rating change proportionally with kFactor`() {
        val ratingA = 1500.0
        val ratingB = 1500.0
        val actualScoreA = 1.0

        val resultK32 = calculator.calculateMatch(ratingA, ratingB, actualScoreA, kFactor = 32.0)
        val resultK64 = calculator.calculateMatch(ratingA, ratingB, actualScoreA, kFactor = 64.0)

        assertEquals(16.0, resultK32.ratingChange, epsilon)
        assertEquals(32.0, resultK64.ratingChange, epsilon)
        assertEquals(resultK32.ratingChange * 2.0, resultK64.ratingChange, epsilon)
    }

    // --- 4. Edge Cases ---

    @Test
    fun `edge case - handles extreme rating differences without overflow or NaN`() {
        val titanRating = 3500.0
        val noviceRating = 1000.0

        val expTitan = calculator.expectedScore(titanRating, noviceRating)
        val expNovice = calculator.expectedScore(noviceRating, titanRating)

        assertTrue("Titan expected score must be extremely close to 1.0", expTitan > 0.999999)
        assertTrue("Novice expected score must be extremely close to 0.0", expNovice < 0.000001)
        assertEquals(1.0, expTitan + expNovice, epsilon)

        // Titan wins -> barely gains points
        val matchResult = calculator.calculateMatch(titanRating, noviceRating, actualScoreA = 1.0)
        assertTrue("Titan rating change should be tiny", matchResult.ratingChange < 0.01)
        assertTrue("Both ratings must remain finite", matchResult.newRatingA.isFinite() && matchResult.newRatingB.isFinite())
    }

    @Test
    fun `edge case - handles negative ratings correctly`() {
        val ratingA = -200.0
        val ratingB = -400.0

        // Difference is (-400 - (-200)) = -200. A is higher rated than B
        val expA = calculator.expectedScore(ratingA, ratingB)
        assertTrue("A is higher rated despite negative values", expA > 0.5)

        val result = calculator.calculateMatch(ratingA, ratingB, actualScoreA = 1.0)
        assertEquals(ratingA + ratingB, result.newRatingA + result.newRatingB, epsilon)
    }

    // --- 5. Precondition Validations ---

    @Test(expected = IllegalArgumentException::class)
    fun `validation - kFactor zero throws IllegalArgumentException`() {
        calculator.updateRating(1500.0, 1500.0, actualScore = 1.0, kFactor = 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validation - kFactor negative throws IllegalArgumentException`() {
        calculator.updateRating(1500.0, 1500.0, actualScore = 1.0, kFactor = -32.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validation - actualScore greater than 1_0 throws IllegalArgumentException`() {
        calculator.updateRating(1500.0, 1500.0, actualScore = 1.5)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validation - actualScore less than 0_0 throws IllegalArgumentException`() {
        calculator.updateRating(1500.0, 1500.0, actualScore = -0.1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validation - non-finite rating NaN throws IllegalArgumentException`() {
        calculator.expectedScore(Double.NaN, 1500.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validation - non-finite rating Infinity throws IllegalArgumentException`() {
        calculator.expectedScore(1500.0, Double.POSITIVE_INFINITY)
    }

    // --- 6. Constructor defaultKFactor & Method Override Tests ---

    @Test
    fun `constructor defaultKFactor - is used when kFactor is omitted`() {
        val customCalc: RatingCalculator = EloRatingCalculator(defaultKFactor = 20.0)

        // Equal teams, Win -> E = 0.5, delta = 20.0 * (1.0 - 0.5) = 10.0
        val updated = customCalc.updateRating(1500.0, 1500.0, actualScore = 1.0)
        assertEquals(1510.0, updated, epsilon)

        val match = customCalc.calculateMatch(1500.0, 1500.0, actualScoreA = 1.0)
        assertEquals(10.0, match.ratingChange, epsilon)
        assertEquals(1510.0, match.newRatingA, epsilon)
        assertEquals(1490.0, match.newRatingB, epsilon)
    }

    @Test
    fun `method kFactor - explicit kFactor overrides constructor defaultKFactor`() {
        val customCalc: RatingCalculator = EloRatingCalculator(defaultKFactor = 20.0)

        // Pass kFactor = 50.0 explicitly -> delta = 50.0 * (1.0 - 0.5) = 25.0
        val updated = customCalc.updateRating(1500.0, 1500.0, actualScore = 1.0, kFactor = 50.0)
        assertEquals(1525.0, updated, epsilon)

        val match = customCalc.calculateMatch(1500.0, 1500.0, actualScoreA = 1.0, kFactor = 50.0)
        assertEquals(25.0, match.ratingChange, epsilon)
        assertEquals(1525.0, match.newRatingA, epsilon)
        assertEquals(1475.0, match.newRatingB, epsilon)
    }

    @Test
    fun `constructor validation - invalid defaultKFactor throws IllegalArgumentException`() {
        val invalidValues = listOf(0.0, -10.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)
        for (invalidK in invalidValues) {
            var thrown = false
            try {
                EloRatingCalculator(defaultKFactor = invalidK)
            } catch (e: IllegalArgumentException) {
                thrown = true
            }
            assertTrue("Expected IllegalArgumentException for defaultKFactor = $invalidK", thrown)
        }
    }
}
