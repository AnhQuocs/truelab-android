package dev.anhquocs.truelab.core.algorithm.trend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Dummy data class để kiểm thử API Generic Selector.
 */
data class TestOddsPoint(
    val matchId: String,
    val timestamp: Long,
    val odds: Double
)

class MovingAverageAlgorithmsTest {

    private val calculator: MovingAverageCalculator = SimpleMovingAverageCalculator()
    private val epsilon = 1e-9

    // --- 1. Basic Correctness Tests ---

    @Test
    fun `calculate - computes correct SMA for standard dataset with window 3`() {
        val dataset = listOf(1.0, 2.0, 3.0, 4.0, 5.0)
        val result = calculator.calculate(dataset, windowSize = 3)

        val expected = listOf(2.0, 3.0, 4.0)
        assertEquals(expected.size, result.size)
        for (i in expected.indices) {
            assertEquals(expected[i], result[i], epsilon)
        }
    }

    @Test
    fun `calculate - handles window size 1 returning identical values`() {
        val dataset = listOf(1.5, 2.5, 3.5)
        val result = calculator.calculate(dataset, windowSize = 1)

        assertEquals(dataset.size, result.size)
        for (i in dataset.indices) {
            assertEquals(dataset[i], result[i], epsilon)
        }
    }

    @Test
    fun `calculate - computes correct SMA for even window size 2 and 4`() {
        val dataset = listOf(2.0, 4.0, 6.0, 8.0)

        // Window = 2 -> [(2+4)/2, (4+6)/2, (6+8)/2] = [3.0, 5.0, 7.0]
        val resultWindow2 = calculator.calculate(dataset, windowSize = 2)
        val expected2 = listOf(3.0, 5.0, 7.0)
        assertEquals(expected2.size, resultWindow2.size)
        for (i in expected2.indices) {
            assertEquals(expected2[i], resultWindow2[i], epsilon)
        }

        // Window = 4 -> [(2+4+6+8)/4] = [5.0]
        val resultWindow4 = calculator.calculate(dataset, windowSize = 4)
        val expected4 = listOf(5.0)
        assertEquals(expected4.size, resultWindow4.size)
        assertEquals(expected4[0], resultWindow4[0], epsilon)
    }

    @Test
    fun `calculate - window equal to dataset size returns single mean value`() {
        val dataset = listOf(10.0, 20.0, 30.0)
        val result = calculator.calculate(dataset, windowSize = 3)

        assertEquals(1, result.size)
        assertEquals(20.0, result[0], epsilon)
    }

    // --- 2. Edge Cases Tests ---

    @Test(expected = IllegalArgumentException::class)
    fun `edge case - window size zero throws IllegalArgumentException`() {
        calculator.calculate(listOf(1.0, 2.0), windowSize = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `edge case - negative window size throws IllegalArgumentException`() {
        calculator.calculate(listOf(1.0, 2.0), windowSize = -3)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `edge case - window size zero on empty dataset throws IllegalArgumentException before empty check`() {
        calculator.calculate(emptyList(), windowSize = 0)
    }

    @Test
    fun `edge case - empty dataset with valid window returns empty list`() {
        val result = calculator.calculate(emptyList(), windowSize = 3)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `edge case - window size larger than dataset returns empty list`() {
        val dataset = listOf(1.0, 2.0, 3.0)
        val result = calculator.calculate(dataset, windowSize = 5)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `edge case - single element dataset`() {
        val dataset = listOf(42.0)

        // window = 1 -> [42.0]
        val resultWindow1 = calculator.calculate(dataset, windowSize = 1)
        assertEquals(1, resultWindow1.size)
        assertEquals(42.0, resultWindow1[0], epsilon)

        // window = 2 > size -> empty
        val resultWindow2 = calculator.calculate(dataset, windowSize = 2)
        assertTrue(resultWindow2.isEmpty())
    }

    // --- 3. Numerical Cases Tests ---

    @Test
    fun `numerical - handles decimal odds values with precision`() {
        val odds = listOf(1.80, 1.90, 2.10, 2.00, 2.20)
        val result = calculator.calculate(odds, windowSize = 3)

        // SMA[0] = (1.80 + 1.90 + 2.10) / 3 = 5.80 / 3 ≈ 1.9333333333333333
        // SMA[1] = (1.90 + 2.10 + 2.00) / 3 = 6.00 / 3 = 2.0
        // SMA[2] = (2.10 + 2.00 + 2.20) / 3 = 6.30 / 3 = 2.1
        assertEquals(3, result.size)
        assertEquals(5.80 / 3.0, result[0], epsilon)
        assertEquals(2.0, result[1], epsilon)
        assertEquals(2.1, result[2], epsilon)
    }

    @Test
    fun `numerical - handles negative values correctly`() {
        val dataset = listOf(-4.0, -2.0, 0.0, 2.0, 4.0)
        val result = calculator.calculate(dataset, windowSize = 3)

        // [-2.0, 0.0, 2.0]
        val expected = listOf(-2.0, 0.0, 2.0)
        assertEquals(expected.size, result.size)
        for (i in expected.indices) {
            assertEquals(expected[i], result[i], epsilon)
        }
    }

    @Test
    fun `numerical - all identical values maintain constant average`() {
        val dataset = listOf(3.5, 3.5, 3.5, 3.5, 3.5)
        val result = calculator.calculate(dataset, windowSize = 2)

        assertEquals(4, result.size)
        for (v in result) {
            assertEquals(3.5, v, epsilon)
        }
    }

    @Test
    fun `numerical - propagates NaN according to IEEE-754`() {
        val dataset = listOf(1.0, Double.NaN, 3.0, 4.0)
        val result = calculator.calculate(dataset, windowSize = 2)

        // Window [1.0, NaN] -> NaN
        // Window [NaN, 3.0] -> NaN
        // Window [3.0, 4.0] -> 3.5
        assertEquals(3, result.size)
        assertTrue(result[0].isNaN())
        assertTrue(result[1].isNaN())
        assertEquals(3.5, result[2], epsilon)
    }

    @Test
    fun `numerical - POSITIVE_INFINITY sliding out of window recovers finite average correctly`() {
        // Window 1: [POS_INF, 2.0] -> POS_INF
        // Window 2: [2.0, 4.0] -> 3.0 (recovers cleanly without NaN contamination)
        // Window 3: [4.0, 6.0] -> 5.0
        val dataset = listOf(Double.POSITIVE_INFINITY, 2.0, 4.0, 6.0)
        val result = calculator.calculate(dataset, windowSize = 2)

        assertEquals(3, result.size)
        assertEquals(Double.POSITIVE_INFINITY, result[0], epsilon)
        assertEquals(3.0, result[1], epsilon)
        assertEquals(5.0, result[2], epsilon)
    }

    @Test
    fun `numerical - NEGATIVE_INFINITY sliding out of window recovers finite average correctly`() {
        // Window 1: [NEG_INF, 2.0] -> NEG_INF
        // Window 2: [2.0, 4.0] -> 3.0 (recovers cleanly without NaN contamination)
        // Window 3: [4.0, 6.0] -> 5.0
        val dataset = listOf(Double.NEGATIVE_INFINITY, 2.0, 4.0, 6.0)
        val result = calculator.calculate(dataset, windowSize = 2)

        assertEquals(3, result.size)
        assertEquals(Double.NEGATIVE_INFINITY, result[0], epsilon)
        assertEquals(3.0, result[1], epsilon)
        assertEquals(5.0, result[2], epsilon)
    }

    @Test
    fun `numerical - window containing both positive and negative infinity yields NaN`() {
        val dataset = listOf(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 10.0)
        val result = calculator.calculate(dataset, windowSize = 2)

        // Window [+Inf, -Inf] -> NaN (IEEE-754: +Inf + -Inf = NaN)
        // Window [-Inf, 10.0] -> -Inf
        assertEquals(2, result.size)
        assertTrue(result[0].isNaN())
        assertEquals(Double.NEGATIVE_INFINITY, result[1], epsilon)
    }

    @Test
    fun `numerical - dense and all NaN dataset processes in linear time without O(k) recomputation`() {
        val allNan = List(1000) { Double.NaN }
        val result = calculator.calculate(allNan, windowSize = 50)

        assertEquals(951, result.size)
        for (v in result) {
            assertTrue(v.isNaN())
        }
    }

    // --- 4. Immutability Tests ---

    @Test
    fun `immutability - does not mutate original input list`() {
        val original = listOf(10.0, 20.0, 30.0, 40.0)
        val copyBefore = original.toList()

        calculator.calculate(original, windowSize = 2)

        assertEquals(copyBefore, original)
    }

    // --- 5. Generic Selector API Tests ---

    @Test
    fun `generic selector - computes SMA on domain-like objects correctly`() {
        val oddsHistory = listOf(
            TestOddsPoint("M1", timestamp = 1000L, odds = 1.80),
            TestOddsPoint("M1", timestamp = 2000L, odds = 1.90),
            TestOddsPoint("M1", timestamp = 3000L, odds = 2.10),
            TestOddsPoint("M1", timestamp = 4000L, odds = 2.00)
        )

        val result = calculator.calculate(oddsHistory, windowSize = 3) { it.odds }

        assertEquals(2, result.size)
        assertEquals(5.80 / 3.0, result[0], epsilon)
        assertEquals(2.0, result[1], epsilon)
    }

    // --- 6. Stress Test ---

    @Test
    fun `stress test - processes large dataset N 50000 with linear performance`() {
        val size = 50_000
        val windowSize = 100
        val random = Random(42)
        val largeDataset = List(size) { random.nextDouble(1.0, 10.0) }

        val startTime = System.currentTimeMillis()
        val result = calculator.calculate(largeDataset, windowSize)
        val elapsedMs = System.currentTimeMillis() - startTime

        println("Stress Test N = $size, Window = $windowSize executed in $elapsedMs ms")

        val expectedSize = size - windowSize + 1
        assertEquals(expectedSize, result.size)

        // Verify first and last window mathematically
        var firstWindowSum = 0.0
        for (i in 0 until windowSize) {
            firstWindowSum += largeDataset[i]
        }
        assertEquals(firstWindowSum / windowSize, result[0], epsilon)

        var lastWindowSum = 0.0
        for (i in (size - windowSize) until size) {
            lastWindowSum += largeDataset[i]
        }
        assertEquals(lastWindowSum / windowSize, result[result.size - 1], epsilon)
    }
}
