package dev.anhquocs.truelab.core.domain.benchmark.usecase

import dev.anhquocs.truelab.core.algorithm.searching.BinarySearch
import dev.anhquocs.truelab.core.algorithm.searching.LinearSearch
import dev.anhquocs.truelab.core.algorithm.sorting.MergeSort
import dev.anhquocs.truelab.core.algorithm.sorting.QuickSort
import dev.anhquocs.truelab.core.domain.benchmark.generator.SyntheticDatasetGenerator
import dev.anhquocs.truelab.core.domain.benchmark.model.BenchmarkMeasurement
import dev.anhquocs.truelab.core.domain.benchmark.model.ComparisonBenchmarkResult
import dev.anhquocs.truelab.core.domain.benchmark.model.ComprehensiveBenchmarkSuiteResult

/**
 * UseCase thuần túy (Pure Kotlin/JVM) thực thi bộ đo lường hiệu năng thuật toán (Benchmark Suite).
 *
 * Quy trình xử lý:
 * 1. Warm-up JVM (chạy thử các thuật toán nhiều lần lặp để JIT Compiler tối ưu hóa mã byte).
 * 2. Đo lường hiệu năng Search (Linear Search vs Binary Search) trên các kích thước N = 1K, 10K, 50K.
 * 3. Đo lường hiệu năng Sort (Quick Sort vs Merge Sort) trên các kích thước N = 1K, 10K, 50K.
 * 4. Tính toán thời gian trung bình (executionTimeMs) bằng System.nanoTime() và hệ số tăng tốc (speedupFactor).
 */
class RunAlgorithmBenchmarkUseCase(
    private val linearSearch: LinearSearch<Int, Int> = LinearSearch(),
    private val binarySearch: BinarySearch<Int, Int> = BinarySearch(),
    private val quickSort: QuickSort<Int> = QuickSort(),
    private val mergeSort: MergeSort<Int> = MergeSort()
) {

    /**
     * Thực thi toàn bộ bộ đo lường hiệu năng.
     *
     * @param datasetSizes Danh sách các kích thước dữ liệu cần đo lường (mặc định [1000, 10000, 50000]).
     * @param iterationsPerSize Số lần lặp đo lường trên mỗi kích thước để tính trung bình (mặc định 5).
     * @param warmupIterations Số lần lặp khởi động JVM trước khi đo lường (mặc định 100).
     * @param seed Hạt giống ngẫu nhiên tạo dataset (mặc định [SyntheticDatasetGenerator.DEFAULT_SEED]).
     * @return [ComprehensiveBenchmarkSuiteResult] chứa toàn bộ kết quả đo lường và so sánh.
     */
    operator fun invoke(
        datasetSizes: List<Int> = listOf(1_000, 10_000, 50_000),
        iterationsPerSize: Int = 5,
        warmupIterations: Int = 100,
        seed: Long = SyntheticDatasetGenerator.DEFAULT_SEED
    ): ComprehensiveBenchmarkSuiteResult {
        // 1. Warm-up JVM để ổn định JIT compiler
        performJvmWarmup(warmupIterations, seed)

        val searchBenchmarks = mutableListOf<ComparisonBenchmarkResult>()
        val sortBenchmarks = mutableListOf<ComparisonBenchmarkResult>()

        val validIterations = iterationsPerSize.coerceAtLeast(1)

        var blackhole = 0

        // 2. Chạy Search Benchmarks
        for (size in datasetSizes) {
            val sortedDataset = SyntheticDatasetGenerator.generateSortedIntList(size, seed)
            val target = if (sortedDataset.isNotEmpty()) sortedDataset[sortedDataset.size / 2] else 0

            // Đo Linear Search
            var linearTotalNanos = 0L
            repeat(validIterations) {
                val start = System.nanoTime()
                val idx = linearSearch.search(sortedDataset, target) { it }
                val end = System.nanoTime()
                blackhole += idx
                linearTotalNanos += (end - start)
            }
            val linearTimeMs = (linearTotalNanos.toDouble() / validIterations) / 1_000_000.0

            val linearMeasurement = BenchmarkMeasurement(
                algorithmName = "Linear Search",
                algorithmType = "SEARCH",
                datasetSize = size,
                executionTimeMs = linearTimeMs,
                iterationsRun = validIterations
            )

            // Đo Binary Search
            var binaryTotalNanos = 0L
            repeat(validIterations) {
                val start = System.nanoTime()
                val idx = binarySearch.search(sortedDataset, target) { it }
                val end = System.nanoTime()
                blackhole += idx
                binaryTotalNanos += (end - start)
            }
            val binaryTimeMs = (binaryTotalNanos.toDouble() / validIterations) / 1_000_000.0

            val binaryMeasurement = BenchmarkMeasurement(
                algorithmName = "Binary Search",
                algorithmType = "SEARCH",
                datasetSize = size,
                executionTimeMs = binaryTimeMs,
                iterationsRun = validIterations
            )

            searchBenchmarks.add(createComparison(linearMeasurement, binaryMeasurement))
        }

        // 3. Chạy Sort Benchmarks
        for (size in datasetSizes) {
            val randomDataset = SyntheticDatasetGenerator.generateRandomIntList(size, seed)
            val comparator = naturalOrder<Int>()

            // Đo Quick Sort
            var quickTotalNanos = 0L
            repeat(validIterations) {
                val start = System.nanoTime()
                val sorted = quickSort.sort(randomDataset, comparator)
                val end = System.nanoTime()
                blackhole += sorted.size
                quickTotalNanos += (end - start)
            }
            val quickTimeMs = (quickTotalNanos.toDouble() / validIterations) / 1_000_000.0

            val quickMeasurement = BenchmarkMeasurement(
                algorithmName = "Quick Sort",
                algorithmType = "SORT",
                datasetSize = size,
                executionTimeMs = quickTimeMs,
                iterationsRun = validIterations
            )

            // Đo Merge Sort
            var mergeTotalNanos = 0L
            repeat(validIterations) {
                val start = System.nanoTime()
                val sorted = mergeSort.sort(randomDataset, comparator)
                val end = System.nanoTime()
                blackhole += sorted.size
                mergeTotalNanos += (end - start)
            }
            val mergeTimeMs = (mergeTotalNanos.toDouble() / validIterations) / 1_000_000.0

            val mergeMeasurement = BenchmarkMeasurement(
                algorithmName = "Merge Sort",
                algorithmType = "SORT",
                datasetSize = size,
                executionTimeMs = mergeTimeMs,
                iterationsRun = validIterations
            )

            sortBenchmarks.add(createComparison(quickMeasurement, mergeMeasurement))
        }

        return ComprehensiveBenchmarkSuiteResult(
            searchBenchmarks = searchBenchmarks,
            sortBenchmarks = sortBenchmarks,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun performJvmWarmup(iterations: Int, seed: Long) {
        if (iterations <= 0) return
        val warmupDataset = SyntheticDatasetGenerator.generateSortedIntList(200, seed)
        val target = if (warmupDataset.isNotEmpty()) warmupDataset[warmupDataset.size / 2] else 0
        val randomWarmup = SyntheticDatasetGenerator.generateRandomIntList(200, seed)
        val comparator = naturalOrder<Int>()

        var warmupSink = 0
        repeat(iterations) {
            warmupSink += linearSearch.search(warmupDataset, target) { it }
            warmupSink += binarySearch.search(warmupDataset, target) { it }
            warmupSink += quickSort.sort(randomWarmup, comparator).size
            warmupSink += mergeSort.sort(randomWarmup, comparator).size
        }
    }

    private fun createComparison(
        measurementA: BenchmarkMeasurement,
        measurementB: BenchmarkMeasurement
    ): ComparisonBenchmarkResult {
        val timeA = measurementA.executionTimeMs
        val timeB = measurementB.executionTimeMs

        val faster = when {
            timeA < timeB -> measurementA.algorithmName
            timeB < timeA -> measurementB.algorithmName
            else -> "Equal"
        }

        val speedupFactor = when {
            timeA <= 0.0 || timeB <= 0.0 -> 1.0
            timeA < timeB -> timeB / timeA
            timeB < timeA -> timeA / timeB
            else -> 1.0
        }

        return ComparisonBenchmarkResult(
            algorithmA = measurementA,
            algorithmB = measurementB,
            speedupFactor = speedupFactor,
            fasterAlgorithm = faster
        )
    }
}
