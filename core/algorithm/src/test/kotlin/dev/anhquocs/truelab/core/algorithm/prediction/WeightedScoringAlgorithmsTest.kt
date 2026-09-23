package dev.anhquocs.truelab.core.algorithm.prediction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeightedScoringAlgorithmsTest {

    private val scorer: WeightedScorer = DefaultWeightedScorer()
    private val epsilon: Double = 1e-9

    // =========================================================================
    // Group 1 – Weighted Scoring (Tests 1–12)
    // =========================================================================

    @Test
    fun `calculateScore - single feature returns exact score`() {
        val features = listOf(
            WeightedFeature(score = 0.75, weight = 10.0, name = "SingleFeature")
        )
        val result = scorer.calculateScore(features)
        assertEquals(0.75, result, epsilon)
    }

    @Test
    fun `calculateScore - equal weights equals arithmetic mean`() {
        val features = listOf(
            WeightedFeature(score = 0.2, weight = 1.0),
            WeightedFeature(score = 0.4, weight = 1.0),
            WeightedFeature(score = 0.6, weight = 1.0)
        )
        // (0.2 + 0.4 + 0.6) / 3 = 0.4
        val result = scorer.calculateScore(features)
        assertEquals(0.4, result, epsilon)
    }

    @Test
    fun `calculateScore - unequal weights calculates correct weighted average`() {
        val features = listOf(
            WeightedFeature(score = 0.8, weight = 3.0),
            WeightedFeature(score = 0.4, weight = 1.0)
        )
        // (0.8 * 3.0 + 0.4 * 1.0) / 4.0 = (2.4 + 0.4) / 4.0 = 2.8 / 4.0 = 0.7
        val result = scorer.calculateScore(features)
        assertEquals(0.7, result, epsilon)
    }

    @Test
    fun `calculateScore - zero weight feature does not affect result`() {
        val featuresWithZero = listOf(
            WeightedFeature(score = 0.6, weight = 2.0),
            WeightedFeature(score = 0.1, weight = 0.0)
        )
        val featuresWithoutZero = listOf(
            WeightedFeature(score = 0.6, weight = 2.0)
        )
        val resultWithZero = scorer.calculateScore(featuresWithZero)
        val resultWithoutZero = scorer.calculateScore(featuresWithoutZero)

        assertEquals(0.6, resultWithZero, epsilon)
        assertEquals(resultWithoutZero, resultWithZero, epsilon)
    }

    @Test
    fun `calculateScore - scale invariance when multiplying weights by constant`() {
        val original = listOf(
            WeightedFeature(score = 0.3, weight = 2.0),
            WeightedFeature(score = 0.7, weight = 3.0),
            WeightedFeature(score = 0.5, weight = 5.0)
        )
        val scaled = listOf(
            WeightedFeature(score = 0.3, weight = 200.0),
            WeightedFeature(score = 0.7, weight = 300.0),
            WeightedFeature(score = 0.5, weight = 500.0)
        )
        val resOriginal = scorer.calculateScore(original)
        val resScaled = scorer.calculateScore(scaled)

        assertEquals(resOriginal, resScaled, epsilon)
    }

    @Test
    fun `calculateScore - boundary values (all zeros and all ones)`() {
        val allZeros = listOf(
            WeightedFeature(score = 0.0, weight = 2.5),
            WeightedFeature(score = 0.0, weight = 7.5)
        )
        val allOnes = listOf(
            WeightedFeature(score = 1.0, weight = 1.0),
            WeightedFeature(score = 1.0, weight = 9.0)
        )

        assertEquals(0.0, scorer.calculateScore(allZeros), epsilon)
        assertEquals(1.0, scorer.calculateScore(allOnes), epsilon)
    }

    @Test
    fun `calculateScore - generic selector overload matches direct list calculation`() {
        data class MetricItem(val name: String, val rawValue: Double, val importance: Double)

        val items = listOf(
            MetricItem("A", 0.85, 2.0),
            MetricItem("B", 0.45, 3.0),
            MetricItem("C", 0.60, 5.0)
        )

        val directFeatures = items.map { WeightedFeature(it.rawValue, it.importance, it.name) }

        val directResult = scorer.calculateScore(directFeatures)
        val selectorResult = scorer.calculateScore(
            dataset = items,
            scoreSelector = { it.rawValue },
            weightSelector = { it.importance }
        )

        assertEquals(directResult, selectorResult, epsilon)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `calculateScore - throws on empty list`() {
        scorer.calculateScore(emptyList())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `calculateScore - throws on negative weight`() {
        val features = listOf(
            WeightedFeature(score = 0.5, weight = -1.0)
        )
        scorer.calculateScore(features)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `calculateScore - throws on all weights zero`() {
        val features = listOf(
            WeightedFeature(score = 0.5, weight = 0.0),
            WeightedFeature(score = 0.8, weight = 0.0)
        )
        scorer.calculateScore(features)
    }

    @Test
    fun `calculateScore - throws on NaN or infinite score or weight`() {
        assertThrowsIllegalArgument {
            scorer.calculateScore(listOf(WeightedFeature(Double.NaN, 1.0)))
        }
        assertThrowsIllegalArgument {
            scorer.calculateScore(listOf(WeightedFeature(0.5, Double.NaN)))
        }
        assertThrowsIllegalArgument {
            scorer.calculateScore(listOf(WeightedFeature(Double.POSITIVE_INFINITY, 1.0)))
        }
        assertThrowsIllegalArgument {
            scorer.calculateScore(listOf(WeightedFeature(0.5, Double.POSITIVE_INFINITY)))
        }
        assertThrowsIllegalArgument {
            scorer.calculateScore(listOf(WeightedFeature(Double.NEGATIVE_INFINITY, 1.0)))
        }
        assertThrowsIllegalArgument {
            scorer.calculateScore(listOf(WeightedFeature(0.5, Double.NEGATIVE_INFINITY)))
        }
    }

    @Test
    fun `calculateScore - throws on score outside range 0 to 1`() {
        assertThrowsIllegalArgument {
            scorer.calculateScore(listOf(WeightedFeature(-0.01, 1.0)))
        }
        assertThrowsIllegalArgument {
            scorer.calculateScore(listOf(WeightedFeature(1.01, 1.0)))
        }
    }

    // =========================================================================
    // Group 2 – 3-Way Outcome Normalization (Tests 13–19)
    // =========================================================================

    @Test
    fun `predict3Way - dominant home score predicts HOME_WIN`() {
        val result = scorer.predict3Way(homeScore = 8.0, drawScore = 1.0, awayScore = 1.0)

        assertEquals(0.8, result.homeWinProb, epsilon)
        assertEquals(0.1, result.drawProb, epsilon)
        assertEquals(0.1, result.awayWinProb, epsilon)
        assertEquals(PredictedOutcome.HOME_WIN, result.predictedOutcome)
        assertEquals(0.8, result.confidenceScore, epsilon)
    }

    @Test
    fun `predict3Way - dominant draw score predicts DRAW`() {
        val result = scorer.predict3Way(homeScore = 1.0, drawScore = 8.0, awayScore = 1.0)

        assertEquals(0.1, result.homeWinProb, epsilon)
        assertEquals(0.8, result.drawProb, epsilon)
        assertEquals(0.1, result.awayWinProb, epsilon)
        assertEquals(PredictedOutcome.DRAW, result.predictedOutcome)
        assertEquals(0.8, result.confidenceScore, epsilon)
    }

    @Test
    fun `predict3Way - dominant away score predicts AWAY_WIN`() {
        val result = scorer.predict3Way(homeScore = 1.0, drawScore = 1.0, awayScore = 8.0)

        assertEquals(0.1, result.homeWinProb, epsilon)
        assertEquals(0.1, result.drawProb, epsilon)
        assertEquals(0.8, result.awayWinProb, epsilon)
        assertEquals(PredictedOutcome.AWAY_WIN, result.predictedOutcome)
        assertEquals(0.8, result.confidenceScore, epsilon)
    }

    @Test
    fun `predict3Way - probabilities sum to 1_0 within 1e-9 tolerance`() {
        val triplets = listOf(
            Triple(3.7, 2.1, 4.2),
            Triple(100.0, 50.0, 25.0),
            Triple(0.001, 0.002, 0.003),
            Triple(1.0, 0.0, 0.0)
        )
        for ((h, d, a) in triplets) {
            val res = scorer.predict3Way(h, d, a)
            val sum = res.homeWinProb + res.drawProb + res.awayWinProb
            assertEquals(1.0, sum, epsilon)
        }
    }

    @Test
    fun `predict3Way - throws on negative score`() {
        assertThrowsIllegalArgument { scorer.predict3Way(-1.0, 2.0, 3.0) }
        assertThrowsIllegalArgument { scorer.predict3Way(1.0, -2.0, 3.0) }
        assertThrowsIllegalArgument { scorer.predict3Way(1.0, 2.0, -3.0) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `predict3Way - throws on all zeros`() {
        scorer.predict3Way(0.0, 0.0, 0.0)
    }

    @Test
    fun `predict3Way - throws on NaN or infinite score`() {
        assertThrowsIllegalArgument { scorer.predict3Way(Double.NaN, 1.0, 1.0) }
        assertThrowsIllegalArgument { scorer.predict3Way(1.0, Double.POSITIVE_INFINITY, 1.0) }
        assertThrowsIllegalArgument { scorer.predict3Way(1.0, 1.0, Double.NEGATIVE_INFINITY) }
    }

    // =========================================================================
    // Group 3 – Tie-Breaking Rules (Tests 20–23)
    // =========================================================================

    @Test
    fun `predict3Way - tie HOME == AWAY greater than DRAW yields DRAW`() {
        // P(H) = 4.0/10.0 = 0.4, P(A) = 4.0/10.0 = 0.4, P(D) = 2.0/10.0 = 0.2
        val result = scorer.predict3Way(homeScore = 4.0, drawScore = 2.0, awayScore = 4.0)

        assertEquals(0.4, result.homeWinProb, epsilon)
        assertEquals(0.2, result.drawProb, epsilon)
        assertEquals(0.4, result.awayWinProb, epsilon)
        assertEquals(PredictedOutcome.DRAW, result.predictedOutcome)
        assertEquals(0.4, result.confidenceScore, epsilon)
    }

    @Test
    fun `predict3Way - tie HOME == DRAW greater than AWAY yields DRAW`() {
        // P(H) = 4.0/10.0 = 0.4, P(D) = 4.0/10.0 = 0.4, P(A) = 2.0/10.0 = 0.2
        val result = scorer.predict3Way(homeScore = 4.0, drawScore = 4.0, awayScore = 2.0)

        assertEquals(0.4, result.homeWinProb, epsilon)
        assertEquals(0.4, result.drawProb, epsilon)
        assertEquals(0.2, result.awayWinProb, epsilon)
        assertEquals(PredictedOutcome.DRAW, result.predictedOutcome)
        assertEquals(0.4, result.confidenceScore, epsilon)
    }

    @Test
    fun `predict3Way - tie DRAW == AWAY greater than HOME yields DRAW`() {
        // P(D) = 4.0/10.0 = 0.4, P(A) = 4.0/10.0 = 0.4, P(H) = 2.0/10.0 = 0.2
        val result = scorer.predict3Way(homeScore = 2.0, drawScore = 4.0, awayScore = 4.0)

        assertEquals(0.2, result.homeWinProb, epsilon)
        assertEquals(0.4, result.drawProb, epsilon)
        assertEquals(0.4, result.awayWinProb, epsilon)
        assertEquals(PredictedOutcome.DRAW, result.predictedOutcome)
        assertEquals(0.4, result.confidenceScore, epsilon)
    }

    @Test
    fun `predict3Way - tie HOME == DRAW == AWAY yields DRAW`() {
        // All equal: P(H) = P(D) = P(A) = 1/3
        val result = scorer.predict3Way(homeScore = 3.0, drawScore = 3.0, awayScore = 3.0)

        assertEquals(1.0 / 3.0, result.homeWinProb, epsilon)
        assertEquals(1.0 / 3.0, result.drawProb, epsilon)
        assertEquals(1.0 / 3.0, result.awayWinProb, epsilon)
        assertEquals(PredictedOutcome.DRAW, result.predictedOutcome)
        assertEquals(1.0 / 3.0, result.confidenceScore, epsilon)
    }

    // =========================================================================
    // Group 4 – Linear Mixture & Determinism (Tests 24–27)
    // =========================================================================

    @Test
    fun `predictOutcome - linear mixture computes correct combined probabilities`() {
        val signals = listOf(
            Signal3Way(homeProb = 0.6, drawProb = 0.2, awayProb = 0.2, weight = 1.0, name = "SignalA"),
            Signal3Way(homeProb = 0.4, drawProb = 0.4, awayProb = 0.2, weight = 1.0, name = "SignalB")
        )
        // Combined:
        // P(H) = (0.6 + 0.4) / 2 = 0.5
        // P(D) = (0.2 + 0.4) / 2 = 0.3
        // P(A) = (0.2 + 0.2) / 2 = 0.2
        val result = scorer.predictOutcome(signals)

        assertEquals(0.5, result.homeWinProb, epsilon)
        assertEquals(0.3, result.drawProb, epsilon)
        assertEquals(0.2, result.awayWinProb, epsilon)
        assertEquals(PredictedOutcome.HOME_WIN, result.predictedOutcome)
        assertEquals(0.5, result.confidenceScore, epsilon)
    }

    @Test
    fun `predictOutcome - tie breaking in linear mixture yields DRAW`() {
        val signals = listOf(
            Signal3Way(homeProb = 0.6, drawProb = 0.2, awayProb = 0.2, weight = 1.0),
            Signal3Way(homeProb = 0.2, drawProb = 0.2, awayProb = 0.6, weight = 1.0)
        )
        // Combined: P(H) = 0.4, P(D) = 0.2, P(A) = 0.4 -> HOME == AWAY > DRAW -> DRAW
        val result = scorer.predictOutcome(signals)

        assertEquals(0.4, result.homeWinProb, epsilon)
        assertEquals(0.2, result.drawProb, epsilon)
        assertEquals(0.4, result.awayWinProb, epsilon)
        assertEquals(PredictedOutcome.DRAW, result.predictedOutcome)
        assertEquals(0.4, result.confidenceScore, epsilon)
    }

    @Test
    fun `predictOutcome - confidence score equals max probability`() {
        val signals = listOf(
            Signal3Way(homeProb = 0.1, drawProb = 0.2, awayProb = 0.7, weight = 2.0),
            Signal3Way(homeProb = 0.2, drawProb = 0.3, awayProb = 0.5, weight = 1.0)
        )
        val result = scorer.predictOutcome(signals)
        val expectedMax = maxOf(result.homeWinProb, result.drawProb, result.awayWinProb)

        assertEquals(expectedMax, result.confidenceScore, epsilon)
        assertEquals(PredictedOutcome.AWAY_WIN, result.predictedOutcome)
    }

    @Test
    fun `predictOutcome - immutability and determinism across repeated invocations`() {
        val signals = listOf(
            Signal3Way(homeProb = 0.5, drawProb = 0.3, awayProb = 0.2, weight = 1.5),
            Signal3Way(homeProb = 0.3, drawProb = 0.4, awayProb = 0.3, weight = 2.5)
        )

        val run1 = scorer.predictOutcome(signals)
        val run2 = scorer.predictOutcome(signals)

        assertEquals(run1.homeWinProb, run2.homeWinProb, epsilon)
        assertEquals(run1.drawProb, run2.drawProb, epsilon)
        assertEquals(run1.awayWinProb, run2.awayWinProb, epsilon)
        assertEquals(run1.predictedOutcome, run2.predictedOutcome)
        assertEquals(run1.confidenceScore, run2.confidenceScore, epsilon)
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private fun assertThrowsIllegalArgument(block: () -> Unit) {
        var thrown = false
        try {
            block()
        } catch (e: IllegalArgumentException) {
            thrown = true
        }
        assertTrue("Expected IllegalArgumentException was not thrown", thrown)
    }
}
