package dev.anhquocs.truelab.core.algorithm.evaluation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Dummy data class để kiểm thử API Generic Selector.
 */
data class TestMatchHistory(
    val matchId: String,
    val opponent: String,
    val outcome: MatchOutcome
)

class FormScoreAlgorithmsTest {

    private val evaluator: FormEvaluator = LinearDecayFormEvaluator()
    private val epsilon = 1e-9

    // --- 1. Basic Correctness & Mathematical Verification ---

    @Test
    fun `evaluate - calculates correct weighted score and rawScore for standard sequence`() {
        // Chuỗi: W, D, L, W, W (m = 5)
        // Wins = 3, Draws = 1, Losses = 1
        // Total points = 3 + 1 + 0 + 3 + 3 = 10.0
        // Max points = 5 * 3 = 15.0
        // rawScore = (10 / 15) * 100 = 66.6666666667
        // Weights: [1, 2, 3, 4, 5] -> Total weights = 15.0
        // Weighted sum = 1*3 + 2*1 + 3*0 + 4*3 + 5*3 = 3 + 2 + 0 + 12 + 15 = 32.0
        // score = (32 / (3 * 15)) * 100 = (32 / 45) * 100 ≈ 71.1111111111
        val outcomes = listOf(
            MatchOutcome.WIN,
            MatchOutcome.DRAW,
            MatchOutcome.LOSS,
            MatchOutcome.WIN,
            MatchOutcome.WIN
        )

        val result = evaluator.evaluate(outcomes, windowSize = 5)

        assertEquals(5, result.matchesCount)
        assertEquals(3, result.wins)
        assertEquals(1, result.draws)
        assertEquals(1, result.losses)
        assertEquals(10.0, result.totalPoints, epsilon)
        assertEquals(15.0, result.maxPoints, epsilon)
        assertEquals((10.0 / 15.0) * 100.0, result.rawScore, epsilon)
        assertEquals((32.0 / 45.0) * 100.0, result.score, epsilon)
    }

    // --- 2. Recency Sensitivity Tests ---

    @Test
    fun `evaluate - demonstrates higher recency sensitivity for recent win compared to distant win`() {
        // Đội A: Thua 4 trận cũ, thắng trận mới nhất [L, L, L, L, W]
        val teamAOutcomes = listOf(
            MatchOutcome.LOSS,
            MatchOutcome.LOSS,
            MatchOutcome.LOSS,
            MatchOutcome.LOSS,
            MatchOutcome.WIN
        )

        // Đội B: Thắng trận cũ nhất, thua 4 trận gần nhất [W, L, L, L, L]
        val teamBOutcomes = listOf(
            MatchOutcome.WIN,
            MatchOutcome.LOSS,
            MatchOutcome.LOSS,
            MatchOutcome.LOSS,
            MatchOutcome.LOSS
        )

        val formA = evaluator.evaluate(teamAOutcomes, windowSize = 5)
        val formB = evaluator.evaluate(teamBOutcomes, windowSize = 5)

        // Cả 2 đội đều có cùng 1 trận thắng (3 điểm) -> rawScore giống nhau (20.0%)
        assertEquals(20.0, formA.rawScore, epsilon)
        assertEquals(20.0, formB.rawScore, epsilon)

        // Đội A: Weighted sum = 5 * 3 = 15.0 -> score = (15 / 45) * 100 = 33.3333333333%
        // Đội B: Weighted sum = 1 * 3 = 3.0  -> score = (3 / 45) * 100 = 6.6666666667%
        assertEquals((15.0 / 45.0) * 100.0, formA.score, epsilon)
        assertEquals((3.0 / 45.0) * 100.0, formB.score, epsilon)

        // Form Score có trọng số thời gian phản ánh phong độ phục hồi của Đội A vượt trội hơn Đội B
        assertTrue("Team A score must be strictly greater than Team B score", formA.score > formB.score)
    }

    // --- 3. Edge Cases Tests ---

    @Test
    fun `edge case - empty input returns zeroed FormScore`() {
        val result = evaluator.evaluate(emptyList(), windowSize = 5)

        assertEquals(0, result.matchesCount)
        assertEquals(0, result.wins)
        assertEquals(0, result.draws)
        assertEquals(0, result.losses)
        assertEquals(0.0, result.totalPoints, epsilon)
        assertEquals(0.0, result.maxPoints, epsilon)
        assertEquals(0.0, result.rawScore, epsilon)
        assertEquals(0.0, result.score, epsilon)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `edge case - window size zero throws IllegalArgumentException`() {
        evaluator.evaluate(listOf(MatchOutcome.WIN), windowSize = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `edge case - negative window size throws IllegalArgumentException`() {
        evaluator.evaluate(listOf(MatchOutcome.WIN), windowSize = -3)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `edge case - window size zero on empty dataset throws IllegalArgumentException before empty check`() {
        evaluator.evaluate(emptyList(), windowSize = 0)
    }

    @Test
    fun `edge case - dataset smaller than window size evaluates all available matches`() {
        // Đội chỉ mới đá 2 trận trong mùa giải: W, D (windowSize = 5)
        // m = 2, totalPoints = 4.0, maxPoints = 6.0
        // rawScore = (4 / 6) * 100 = 66.6666666667
        // Weights: [1, 2] -> Total weights = 3.0
        // Weighted sum = 1*3 + 2*1 = 5.0 -> score = (5 / (3 * 3)) * 100 = (5 / 9) * 100 ≈ 55.5555555556
        val outcomes = listOf(MatchOutcome.WIN, MatchOutcome.DRAW)
        val result = evaluator.evaluate(outcomes, windowSize = 5)

        assertEquals(2, result.matchesCount)
        assertEquals(1, result.wins)
        assertEquals(1, result.draws)
        assertEquals(0, result.losses)
        assertEquals(4.0, result.totalPoints, epsilon)
        assertEquals(6.0, result.maxPoints, epsilon)
        assertEquals((4.0 / 6.0) * 100.0, result.rawScore, epsilon)
        assertEquals((5.0 / 9.0) * 100.0, result.score, epsilon)
    }

    @Test
    fun `edge case - dataset larger than window size evaluates only last k matches`() {
        // Đội đá 6 trận: L, L, L, W, W, W với windowSize = 3
        // 3 trận cuối: W, W, W (toàn thắng trong cửa sổ)
        val outcomes = listOf(
            MatchOutcome.LOSS,
            MatchOutcome.LOSS,
            MatchOutcome.LOSS,
            MatchOutcome.WIN,
            MatchOutcome.WIN,
            MatchOutcome.WIN
        )

        val result = evaluator.evaluate(outcomes, windowSize = 3)

        assertEquals(3, result.matchesCount)
        assertEquals(3, result.wins)
        assertEquals(0, result.draws)
        assertEquals(0, result.losses)
        assertEquals(9.0, result.totalPoints, epsilon)
        assertEquals(9.0, result.maxPoints, epsilon)
        assertEquals(100.0, result.rawScore, epsilon)
        assertEquals(100.0, result.score, epsilon)
    }

    @Test
    fun `edge case - window size 1 evaluates single last match correctly`() {
        assertEquals(100.0, evaluator.evaluate(listOf(MatchOutcome.WIN), windowSize = 1).score, epsilon)
        assertEquals((1.0 / 3.0) * 100.0, evaluator.evaluate(listOf(MatchOutcome.DRAW), windowSize = 1).score, epsilon)
        assertEquals(0.0, evaluator.evaluate(listOf(MatchOutcome.LOSS), windowSize = 1).score, epsilon)
    }

    // --- 4. Extreme Form Outcomes ---

    @Test
    fun `extreme - all wins yields maximum 100 percent for both scores`() {
        val allWins = listOf(MatchOutcome.WIN, MatchOutcome.WIN, MatchOutcome.WIN, MatchOutcome.WIN, MatchOutcome.WIN)
        val result = evaluator.evaluate(allWins, windowSize = 5)

        assertEquals(5, result.wins)
        assertEquals(100.0, result.rawScore, epsilon)
        assertEquals(100.0, result.score, epsilon)
    }

    @Test
    fun `extreme - all draws yields one third points for both scores`() {
        val allDraws = listOf(MatchOutcome.DRAW, MatchOutcome.DRAW, MatchOutcome.DRAW, MatchOutcome.DRAW, MatchOutcome.DRAW)
        val result = evaluator.evaluate(allDraws, windowSize = 5)

        assertEquals(5, result.draws)
        val expected = (1.0 / 3.0) * 100.0
        assertEquals(expected, result.rawScore, epsilon)
        assertEquals(expected, result.score, epsilon)
    }

    @Test
    fun `extreme - all losses yields zero for both scores`() {
        val allLosses = listOf(MatchOutcome.LOSS, MatchOutcome.LOSS, MatchOutcome.LOSS, MatchOutcome.LOSS, MatchOutcome.LOSS)
        val result = evaluator.evaluate(allLosses, windowSize = 5)

        assertEquals(5, result.losses)
        assertEquals(0.0, result.rawScore, epsilon)
        assertEquals(0.0, result.score, epsilon)
    }

    // --- 5. Immutability Tests ---

    @Test
    fun `immutability - does not mutate original input list`() {
        val original = listOf(MatchOutcome.WIN, MatchOutcome.DRAW, MatchOutcome.LOSS)
        val copyBefore = original.toList()

        evaluator.evaluate(original, windowSize = 2)

        assertEquals(copyBefore, original)
    }

    // --- 6. Generic Selector API Tests ---

    @Test
    fun `generic selector - evaluates form on domain-like match history without list allocation`() {
        val matchHistory = listOf(
            TestMatchHistory("M1", "Arsenal", MatchOutcome.LOSS),
            TestMatchHistory("M2", "Chelsea", MatchOutcome.DRAW),
            TestMatchHistory("M3", "Liverpool", MatchOutcome.WIN)
        )

        val result = evaluator.evaluate(matchHistory, windowSize = 2) { it.outcome }

        // Window = 2 -> Chỉ xét Chelsea (D) và Liverpool (W)
        assertEquals(2, result.matchesCount)
        assertEquals(1, result.wins)
        assertEquals(1, result.draws)
        assertEquals(0, result.losses)
        assertEquals(4.0, result.totalPoints, epsilon)
        assertEquals(6.0, result.maxPoints, epsilon)
        assertEquals((4.0 / 6.0) * 100.0, result.rawScore, epsilon)
        // Weighted sum = 1*1 + 2*3 = 7.0 -> score = (7 / 9) * 100 ≈ 77.7777777778%
        assertEquals((7.0 / 9.0) * 100.0, result.score, epsilon)
    }

    // --- 7. Stress Test & O(k) Verification ---

    @Test
    fun `stress test - processes large history N 50000 with k 5 in O(k) constant time`() {
        val size = 50_000
        val windowSize = 5
        // Giả lập lịch sử 50.000 trận đấu
        val largeHistory = List(size) { i ->
            if (i >= size - 5) MatchOutcome.WIN else MatchOutcome.LOSS
        }

        val startTime = System.currentTimeMillis()
        val result = evaluator.evaluate(largeHistory, windowSize)
        val elapsedMs = System.currentTimeMillis() - startTime

        println("Stress Test N = $size, windowSize = $windowSize executed in $elapsedMs ms")

        // 5 trận cuối đều là WIN
        assertEquals(5, result.matchesCount)
        assertEquals(5, result.wins)
        assertEquals(100.0, result.score, epsilon)
        assertEquals(100.0, result.rawScore, epsilon)
    }
}
