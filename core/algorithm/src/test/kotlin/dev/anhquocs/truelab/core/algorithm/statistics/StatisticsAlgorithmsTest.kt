package dev.anhquocs.truelab.core.algorithm.statistics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

/**
 * Dummy data class để kiểm thử API Generic Selector.
 */
data class TestMatchData(
    val matchId: String,
    val homeGoals: Double,
    val homeOdds: Double
)

class StatisticsAlgorithmsTest {

    private val calculator: StatisticsCalculator = DescriptiveStatisticsCalculator()
    private val epsilon = 1e-9

    // --- 1. Basic Metrics Tests ---

    @Test
    fun `mean - calculates correct average for standard dataset`() {
        val dataset = listOf(1.0, 2.0, 3.0, 4.0, 5.0)
        assertEquals(3.0, calculator.mean(dataset), epsilon)
    }

    @Test
    fun `mean - handles negative and decimal values correctly`() {
        val dataset = listOf(-2.5, 0.0, 2.5, 5.0)
        assertEquals(1.25, calculator.mean(dataset), epsilon)
    }

    @Test
    fun `median - calculates correct median for odd sized dataset`() {
        val dataset = listOf(9.0, 1.0, 5.0, 3.0, 7.0) // Sorted: 1, 3, 5, 7, 9 -> 5
        assertEquals(5.0, calculator.median(dataset), epsilon)
    }

    @Test
    fun `median - calculates correct median for even sized dataset`() {
        val dataset = listOf(8.0, 2.0, 6.0, 4.0) // Sorted: 2, 4, 6, 8 -> (4 + 6) / 2 = 5.0
        assertEquals(5.0, calculator.median(dataset), epsilon)
    }

    @Test
    fun `median - does not mutate original input list`() {
        val original = listOf(10.0, 2.0, 5.0)
        val copyBefore = original.toList()
        calculator.median(original)
        assertEquals(copyBefore, original)
    }

    @Test
    fun `min max range - calculates correctly`() {
        val dataset = listOf(-5.0, 12.0, 3.0, -10.0, 8.5)
        assertEquals(-10.0, calculator.min(dataset), epsilon)
        assertEquals(12.0, calculator.max(dataset), epsilon)
        assertEquals(22.0, calculator.range(dataset), epsilon)
    }

    @Test
    fun `variance and stdDev - calculates population and sample correctly`() {
        // Dataset chuẩn: 2, 4, 4, 4, 5, 5, 7, 9
        // N = 8, Mean = 5.0, Sum((x - 5)^2) = 9 + 1 + 1 + 1 + 0 + 0 + 4 + 16 = 32
        // PopVar = 32 / 8 = 4.0 -> PopStdDev = 2.0
        // SampleVar = 32 / 7 ≈ 4.571428571428571 -> SampleStdDev ≈ 2.138089935299395
        val dataset = listOf(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0)

        assertEquals(4.0, calculator.populationVariance(dataset), epsilon)
        assertEquals(2.0, calculator.populationStandardDeviation(dataset), epsilon)

        val expectedSampleVar = 32.0 / 7.0
        assertEquals(expectedSampleVar, calculator.sampleVariance(dataset), epsilon)
        assertEquals(kotlin.math.sqrt(expectedSampleVar), calculator.sampleStandardDeviation(dataset), epsilon)
    }

    @Test
    fun `skewness - symmetric distribution has zero skewness`() {
        val symmetric = listOf(1.0, 2.0, 3.0, 4.0, 5.0)
        assertEquals(0.0, calculator.skewness(symmetric), epsilon)
    }

    @Test
    fun `skewness - right skewed distribution has positive skewness`() {
        // Đa số giá trị nhỏ, có ngoại lai lớn ở bên phải -> Lệch phải (positive)
        val rightSkewed = listOf(1.0, 2.0, 2.0, 3.0, 15.0)
        assertTrue(calculator.skewness(rightSkewed) > 0.0)
    }

    @Test
    fun `skewness - left skewed distribution has negative skewness`() {
        // Đa số giá trị lớn, có ngoại lai nhỏ ở bên trái -> Lệch trái (negative)
        val leftSkewed = listOf(1.0, 10.0, 11.0, 11.0, 12.0)
        assertTrue(calculator.skewness(leftSkewed) < 0.0)
    }

    // --- 2. summarize() Container Test ---

    @Test
    fun `summarize - returns identical metrics to individual calls`() {
        val dataset = listOf(1.5, 2.8, 3.2, 4.0, 5.5, 6.1, 7.9)
        val summary = calculator.summarize(dataset)

        assertEquals(dataset.size, summary.count)
        assertEquals(calculator.mean(dataset), summary.mean, epsilon)
        assertEquals(calculator.median(dataset), summary.median, epsilon)
        assertEquals(calculator.min(dataset), summary.min, epsilon)
        assertEquals(calculator.max(dataset), summary.max, epsilon)
        assertEquals(calculator.range(dataset), summary.range, epsilon)
        assertEquals(calculator.populationVariance(dataset), summary.populationVariance, epsilon)
        assertEquals(calculator.sampleVariance(dataset), summary.sampleVariance, epsilon)
        assertEquals(calculator.populationStandardDeviation(dataset), summary.populationStandardDeviation, epsilon)
        assertEquals(calculator.sampleStandardDeviation(dataset), summary.sampleStandardDeviation, epsilon)
        assertEquals(calculator.skewness(dataset), summary.skewness, epsilon)
    }

    // --- 3. Generic Selector API Tests ---

    @Test
    fun `generic selector - computes metrics correctly on domain objects`() {
        val matches = listOf(
            TestMatchData("M1", homeGoals = 2.0, homeOdds = 1.85),
            TestMatchData("M2", homeGoals = 0.0, homeOdds = 2.10),
            TestMatchData("M3", homeGoals = 4.0, homeOdds = 1.50)
        )

        // Mean goals: (2 + 0 + 4) / 3 = 2.0
        assertEquals(2.0, calculator.mean(matches) { it.homeGoals }, epsilon)

        // Min / Max odds
        assertEquals(1.50, calculator.min(matches) { it.homeOdds }, epsilon)
        assertEquals(2.10, calculator.max(matches) { it.homeOdds }, epsilon)

        // Summarize on odds
        val oddsSummary = calculator.summarize(matches) { it.homeOdds }
        assertEquals(3, oddsSummary.count)
        assertEquals(1.85, oddsSummary.median, epsilon)
    }

    // --- 4. Edge Cases Tests ---

    @Test
    fun `edge case - empty dataset returns NaN and count 0`() {
        val empty = emptyList<Double>()

        assertTrue(calculator.mean(empty).isNaN())
        assertTrue(calculator.median(empty).isNaN())
        assertTrue(calculator.min(empty).isNaN())
        assertTrue(calculator.max(empty).isNaN())
        assertTrue(calculator.range(empty).isNaN())
        assertTrue(calculator.populationVariance(empty).isNaN())
        assertTrue(calculator.sampleVariance(empty).isNaN())
        assertTrue(calculator.populationStandardDeviation(empty).isNaN())
        assertTrue(calculator.sampleStandardDeviation(empty).isNaN())
        assertTrue(calculator.skewness(empty).isNaN())

        val summary = calculator.summarize(empty)
        assertEquals(0, summary.count)
        assertTrue(summary.mean.isNaN())
        assertTrue(summary.median.isNaN())
        assertTrue(summary.min.isNaN())
        assertTrue(summary.max.isNaN())
        assertTrue(summary.range.isNaN())
        assertTrue(summary.populationVariance.isNaN())
        assertTrue(summary.sampleVariance.isNaN())
        assertTrue(summary.populationStandardDeviation.isNaN())
        assertTrue(summary.sampleStandardDeviation.isNaN())
        assertTrue(summary.skewness.isNaN())
    }

    @Test
    fun `edge case - single element dataset`() {
        val single = listOf(42.0)

        assertEquals(42.0, calculator.mean(single), epsilon)
        assertEquals(42.0, calculator.median(single), epsilon)
        assertEquals(42.0, calculator.min(single), epsilon)
        assertEquals(42.0, calculator.max(single), epsilon)
        assertEquals(0.0, calculator.range(single), epsilon)
        assertEquals(0.0, calculator.populationVariance(single), epsilon)
        assertEquals(0.0, calculator.populationStandardDeviation(single), epsilon)
        assertTrue(calculator.sampleVariance(single).isNaN()) // N < 2
        assertTrue(calculator.sampleStandardDeviation(single).isNaN())
        assertEquals(0.0, calculator.skewness(single), epsilon)

        val summary = calculator.summarize(single)
        assertEquals(1, summary.count)
        assertEquals(42.0, summary.mean, epsilon)
        assertEquals(42.0, summary.median, epsilon)
        assertEquals(0.0, summary.range, epsilon)
        assertEquals(0.0, summary.populationVariance, epsilon)
        assertTrue(summary.sampleVariance.isNaN())
        assertEquals(0.0, summary.skewness, epsilon)
    }

    @Test
    fun `edge case - all identical values`() {
        val identical = listOf(5.0, 5.0, 5.0, 5.0, 5.0)

        assertEquals(5.0, calculator.mean(identical), epsilon)
        assertEquals(5.0, calculator.median(identical), epsilon)
        assertEquals(5.0, calculator.min(identical), epsilon)
        assertEquals(5.0, calculator.max(identical), epsilon)
        assertEquals(0.0, calculator.range(identical), epsilon)
        assertEquals(0.0, calculator.populationVariance(identical), epsilon)
        assertEquals(0.0, calculator.sampleVariance(identical), epsilon)
        assertEquals(0.0, calculator.populationStandardDeviation(identical), epsilon)
        assertEquals(0.0, calculator.sampleStandardDeviation(identical), epsilon)
        assertEquals(0.0, calculator.skewness(identical), epsilon) // Zero variance -> 0.0
    }

    // --- 5. Stress Test Large Dataset ---

    @Test
    fun `stress test - processes large dataset safely without exception or overflow`() {
        val size = 50_000
        val random = Random(42)
        val largeDataset = List(size) { random.nextDouble(0.0, 100.0) }

        val startTime = System.currentTimeMillis()
        val summary = calculator.summarize(largeDataset)
        val elapsedMs = System.currentTimeMillis() - startTime

        println("Stress Test N = $size executed in $elapsedMs ms")

        assertEquals(size, summary.count)
        assertTrue("Min must be <= Max", summary.min <= summary.max)
        assertEquals(summary.max - summary.min, summary.range, epsilon)
        assertTrue("Mean should be roughly around 50", abs(summary.mean - 50.0) < 1.0)
        assertTrue("Median should be roughly around 50", abs(summary.median - 50.0) < 1.0)
        assertTrue("Variance must be positive", summary.populationVariance > 0.0)
        assertTrue("Sample variance must be positive", summary.sampleVariance > 0.0)
        assertTrue("PopStdDev must be positive", summary.populationStandardDeviation > 0.0)
        assertTrue("SampleStdDev must be positive", summary.sampleStandardDeviation > 0.0)
        assertTrue("Uniform distribution skewness should be close to 0", abs(summary.skewness) < 0.2)
    }
}
