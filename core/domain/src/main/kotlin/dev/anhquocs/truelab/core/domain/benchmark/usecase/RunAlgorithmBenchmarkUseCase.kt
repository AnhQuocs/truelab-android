package dev.anhquocs.truelab.core.domain.benchmark.usecase

import dev.anhquocs.truelab.core.algorithm.searching.BinarySearch
import dev.anhquocs.truelab.core.algorithm.searching.LinearSearch
import dev.anhquocs.truelab.core.algorithm.sorting.MergeSort
import dev.anhquocs.truelab.core.algorithm.sorting.QuickSort
import dev.anhquocs.truelab.core.domain.benchmark.generator.SyntheticDatasetGenerator
import dev.anhquocs.truelab.core.domain.benchmark.model.BenchmarkMeasurement
import dev.anhquocs.truelab.core.domain.benchmark.model.ComparisonBenchmarkResult
import dev.anhquocs.truelab.core.domain.benchmark.model.ComprehensiveBenchmarkSuiteResult
import dev.anhquocs.truelab.core.domain.match.model.Match

class RunAlgorithmBenchmarkUseCase(
    private val linearSearch: LinearSearch<Int, Int> = LinearSearch(),
    private val binarySearch: BinarySearch<Int, Int> = BinarySearch(),
    private val quickSort: QuickSort<Int> = QuickSort(),
    private val mergeSort: MergeSort<Int> = MergeSort()
) {
    operator fun invoke(
        datasetSizes: List<Int> = listOf(1_000, 10_000, 50_000),
        iterationsPerSize: Int = 5,
        warmupIterations: Int = 100,
        seed: Long = SyntheticDatasetGenerator.DEFAULT_SEED,
        matches: List<Match>? = null,
        searchQuery: String = "",
        searchLimit: Int = 5
    ): ComprehensiveBenchmarkSuiteResult {
        return if (matches != null) {
            runRealMatchBenchmark(matches, datasetSizes, iterationsPerSize, warmupIterations, searchQuery, searchLimit)
        } else {
            runSyntheticBenchmark(datasetSizes, iterationsPerSize, warmupIterations, seed)
        }
    }

    private fun runSyntheticBenchmark(
        datasetSizes: List<Int>,
        iterationsPerSize: Int,
        warmupIterations: Int,
        seed: Long
    ): ComprehensiveBenchmarkSuiteResult {
        performJvmWarmup(warmupIterations, seed)
        val iterations = iterationsPerSize.coerceAtLeast(1)
        val searchResults = datasetSizes.map { size ->
            val dataset = SyntheticDatasetGenerator.generateSortedIntList(size, seed)
            val target = dataset.getOrElse(dataset.size / 2) { 0 }
            val linear = measure(iterations) { linearSearch.search(dataset, target) { it } }
            val binary = measure(iterations) { binarySearch.search(dataset, target) { it } }
            compare(
                BenchmarkMeasurement("Linear Search", "SEARCH", size, linear, iterations),
                BenchmarkMeasurement("Binary Search", "SEARCH", size, binary, iterations)
            )
        }
        val sortResults = datasetSizes.map { size ->
            val dataset = SyntheticDatasetGenerator.generateRandomIntList(size, seed)
            val comparator = naturalOrder<Int>()
            val quick = measure(iterations) { quickSort.sort(dataset, comparator) }
            val merge = measure(iterations) { mergeSort.sort(dataset, comparator) }
            compare(
                BenchmarkMeasurement("Quick Sort", "SORT", size, quick, iterations),
                BenchmarkMeasurement("Merge Sort", "SORT", size, merge, iterations)
            )
        }
        return ComprehensiveBenchmarkSuiteResult(searchResults, sortResults, System.currentTimeMillis())
    }

    private fun runRealMatchBenchmark(
        matches: List<Match>,
        datasetSizes: List<Int>,
        iterationsPerSize: Int,
        warmupIterations: Int,
        searchQuery: String,
        searchLimit: Int
    ): ComprehensiveBenchmarkSuiteResult {
        performJvmWarmup(warmupIterations, SyntheticDatasetGenerator.DEFAULT_SEED)
        val iterations = iterationsPerSize.coerceAtLeast(1)
        val sourceMatches = matches.sortedBy { it.id }
        val query = searchQuery.trim().lowercase()
        val searchResults = datasetSizes.map { requestedSize ->
            val dataset = sourceMatches.take(requestedSize.coerceAtMost(sourceMatches.size))
            val preparationStart = System.nanoTime()
            val teamNames = dataset
                .flatMap { listOf(it.homeTeam.name, it.awayTeam.name) }
                .distinctBy { it.trim().lowercase() }
                .sortedBy { it.trim().lowercase() }
            val preparationMs = elapsedMs(preparationStart)
            val matchedItems = dataset.count {
                it.homeTeam.name.trim().equals(searchQuery.trim(), ignoreCase = true) ||
                    it.awayTeam.name.trim().equals(searchQuery.trim(), ignoreCase = true)
            }.coerceAtMost(searchLimit.coerceAtLeast(0))
            val linear = measure(iterations) {
                LinearSearch<String, String>().search(teamNames, query) { it.trim().lowercase() }
            }
            val binary = measure(iterations) {
                BinarySearch<String, String>().search(teamNames, query) { it.trim().lowercase() }
            }
            compare(
                BenchmarkMeasurement("Linear Search", "SEARCH", dataset.size, linear, iterations, matchedItems, preparationMs),
                BenchmarkMeasurement("Binary Search", "SEARCH", dataset.size, binary, iterations, matchedItems, preparationMs)
            )
        }
        val sortResults = datasetSizes.map { requestedSize ->
            val dataset = sourceMatches.take(requestedSize.coerceAtMost(sourceMatches.size))
            val preparationStart = System.nanoTime()
            val sortInput = dataset.toList()
            val preparationMs = elapsedMs(preparationStart)
            val comparator = matchComparator()
            val quick = measure(iterations) { quickSortForMatches(sortInput, comparator) }
            val merge = measure(iterations) { mergeSortForMatches(sortInput, comparator) }
            compare(
                BenchmarkMeasurement("Quick Sort", "SORT", dataset.size, quick, iterations, preparationTimeMs = preparationMs),
                BenchmarkMeasurement("Merge Sort", "SORT", dataset.size, merge, iterations, preparationTimeMs = preparationMs)
            )
        }
        return ComprehensiveBenchmarkSuiteResult(searchResults, sortResults, System.currentTimeMillis())
    }

    private fun quickSortForMatches(matches: List<Match>, comparator: Comparator<Match>): List<Match> {
        @Suppress("UNCHECKED_CAST")
        return QuickSort<Match>().sort(matches, comparator)
    }

    private fun mergeSortForMatches(matches: List<Match>, comparator: Comparator<Match>): List<Match> {
        @Suppress("UNCHECKED_CAST")
        return MergeSort<Match>().sort(matches, comparator)
    }

    private fun <T> measure(iterations: Int, block: () -> T): Double {
        var total = 0L
        repeat(iterations) {
            val start = System.nanoTime()
            block()
            total += System.nanoTime() - start
        }
        return total.toDouble() / iterations / 1_000_000.0
    }

    private fun elapsedMs(start: Long): Double =
        (System.nanoTime() - start).toDouble() / 1_000_000.0

    private fun compare(a: BenchmarkMeasurement, b: BenchmarkMeasurement): ComparisonBenchmarkResult {
        val faster = when {
            a.executionTimeMs < b.executionTimeMs -> a.algorithmName
            b.executionTimeMs < a.executionTimeMs -> b.algorithmName
            else -> "Equal"
        }
        val speedup = when {
            a.executionTimeMs <= 0.0 || b.executionTimeMs <= 0.0 -> 1.0
            a.executionTimeMs < b.executionTimeMs -> b.executionTimeMs / a.executionTimeMs
            else -> a.executionTimeMs / b.executionTimeMs
        }
        return ComparisonBenchmarkResult(a, b, speedup, faster)
    }

    private fun matchComparator(): Comparator<Match> = compareBy(
        { it.homeTeam.name.trim().lowercase() },
        { it.awayTeam.name.trim().lowercase() },
        { it.startTimeDate },
        { it.id }
    )

    private fun performJvmWarmup(iterations: Int, seed: Long) {
        if (iterations <= 0) return
        val dataset = SyntheticDatasetGenerator.generateSortedIntList(200, seed)
        val target = dataset[dataset.size / 2]
        repeat(iterations) {
            linearSearch.search(dataset, target) { it }
            binarySearch.search(dataset, target) { it }
        }
    }
}
