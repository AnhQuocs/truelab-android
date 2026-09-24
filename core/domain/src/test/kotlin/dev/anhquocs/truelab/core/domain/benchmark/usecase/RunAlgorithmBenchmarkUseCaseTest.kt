package dev.anhquocs.truelab.core.domain.benchmark.usecase

import dev.anhquocs.truelab.core.domain.benchmark.generator.SyntheticDatasetGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RunAlgorithmBenchmarkUseCaseTest {

    private lateinit var useCase: RunAlgorithmBenchmarkUseCase

    @Before
    fun setUp() {
        useCase = RunAlgorithmBenchmarkUseCase()
    }

    @Test
    fun `1 Benchmark execution returns comprehensive result structure with default dataset sizes`() {
        val result = useCase(
            datasetSizes = listOf(1_000, 10_000),
            iterationsPerSize = 2,
            warmupIterations = 5
        )

        assertNotNull(result)
        assertTrue(result.timestamp > 0)
        assertEquals(2, result.searchBenchmarks.size)
        assertEquals(2, result.sortBenchmarks.size)
    }

    @Test
    fun `2 Search benchmark measurements contain valid algorithm names types and non-negative times`() {
        val result = useCase(
            datasetSizes = listOf(500),
            iterationsPerSize = 2,
            warmupIterations = 2
        )

        val searchComparison = result.searchBenchmarks[0]
        val linear = searchComparison.algorithmA
        val binary = searchComparison.algorithmB

        assertEquals("Linear Search", linear.algorithmName)
        assertEquals("SEARCH", linear.algorithmType)
        assertEquals(500, linear.datasetSize)
        assertEquals(2, linear.iterationsRun)
        assertTrue("Linear execution time should be non-negative", linear.executionTimeMs >= 0.0)

        assertEquals("Binary Search", binary.algorithmName)
        assertEquals("SEARCH", binary.algorithmType)
        assertEquals(500, binary.datasetSize)
        assertEquals(2, binary.iterationsRun)
        assertTrue("Binary execution time should be non-negative", binary.executionTimeMs >= 0.0)

        assertTrue("Speedup factor should be >= 1.0", searchComparison.speedupFactor >= 1.0)
        assertTrue(
            "Faster algorithm should be one of measured or Equal",
            searchComparison.fasterAlgorithm in setOf("Linear Search", "Binary Search", "Equal")
        )
    }

    @Test
    fun `3 Sort benchmark measurements contain valid algorithm names types and non-negative times`() {
        val result = useCase(
            datasetSizes = listOf(500),
            iterationsPerSize = 2,
            warmupIterations = 2
        )

        val sortComparison = result.sortBenchmarks[0]
        val quick = sortComparison.algorithmA
        val merge = sortComparison.algorithmB

        assertEquals("Quick Sort", quick.algorithmName)
        assertEquals("SORT", quick.algorithmType)
        assertEquals(500, quick.datasetSize)
        assertEquals(2, quick.iterationsRun)
        assertTrue("QuickSort execution time should be non-negative", quick.executionTimeMs >= 0.0)

        assertEquals("Merge Sort", merge.algorithmName)
        assertEquals("SORT", merge.algorithmType)
        assertEquals(500, merge.datasetSize)
        assertEquals(2, merge.iterationsRun)
        assertTrue("MergeSort execution time should be non-negative", merge.executionTimeMs >= 0.0)

        assertTrue("Speedup factor should be >= 1.0", sortComparison.speedupFactor >= 1.0)
        assertTrue(
            "Faster algorithm should be one of measured or Equal",
            sortComparison.fasterAlgorithm in setOf("Quick Sort", "Merge Sort", "Equal")
        )
    }

    @Test
    fun `4 Empty dataset sizes list returns empty benchmark comparisons safely`() {
        val result = useCase(
            datasetSizes = emptyList(),
            iterationsPerSize = 1,
            warmupIterations = 0
        )

        assertTrue(result.searchBenchmarks.isEmpty())
        assertTrue(result.sortBenchmarks.isEmpty())
        assertTrue(result.timestamp > 0)
    }

    @Test
    fun `5 Zero iterations per size is coerced to at least one iteration`() {
        val result = useCase(
            datasetSizes = listOf(100),
            iterationsPerSize = 0,
            warmupIterations = 0
        )

        assertEquals(1, result.searchBenchmarks[0].algorithmA.iterationsRun)
        assertEquals(1, result.sortBenchmarks[0].algorithmA.iterationsRun)
    }

    @Test
    fun `6 Zero warmup iterations executes measurements without failure`() {
        val result = useCase(
            datasetSizes = listOf(200),
            iterationsPerSize = 1,
            warmupIterations = 0
        )

        assertEquals(1, result.searchBenchmarks.size)
        assertEquals(1, result.sortBenchmarks.size)
    }

    @Test
    fun `7 Search and sort algorithms produce mathematically correct outputs on synthetic data`() {
        val linear = dev.anhquocs.truelab.core.algorithm.searching.LinearSearch<Int, Int>()
        val binary = dev.anhquocs.truelab.core.algorithm.searching.BinarySearch<Int, Int>()
        val quick = dev.anhquocs.truelab.core.algorithm.sorting.QuickSort<Int>()
        val merge = dev.anhquocs.truelab.core.algorithm.sorting.MergeSort<Int>()

        val sortedList = SyntheticDatasetGenerator.generateSortedIntList(100, seed = 42L)
        val target = sortedList[50]

        val linearIdx = linear.search(sortedList, target) { it }
        val binaryIdx = binary.search(sortedList, target) { it }

        assertTrue(linearIdx >= 0)
        assertTrue(binaryIdx >= 0)
        assertEquals(target, sortedList[linearIdx])
        assertEquals(target, sortedList[binaryIdx])

        val randomList = SyntheticDatasetGenerator.generateRandomIntList(100, seed = 42L)
        val quickSorted = quick.sort(randomList, naturalOrder())
        val mergeSorted = merge.sort(randomList, naturalOrder())

        assertEquals(100, quickSorted.size)
        assertEquals(100, mergeSorted.size)
        assertEquals(quickSorted, mergeSorted)
        for (i in 0 until quickSorted.size - 1) {
            assertTrue(quickSorted[i] <= quickSorted[i + 1])
        }
    }
}

