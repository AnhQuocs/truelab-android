package dev.anhquocs.truelab.core.algorithm.statistics

import dev.anhquocs.truelab.core.algorithm.sorting.QuickSort
import dev.anhquocs.truelab.core.algorithm.sorting.SortAlgorithm
import kotlin.math.sqrt

/**
 * Triển khai các thuật toán Thống kê Mô tả (Descriptive Statistics).
 *
 * - Áp dụng thuật toán Two-Pass với độ lệch chuẩn hóa (Shifted Deviations) để đảm bảo ổn định số học,
 *   triệt tiêu sai số làm tròn số thực (Floating-point cancellation).
 * - Sử dụng [SortAlgorithm] (mặc định là [QuickSort]) để tìm trung vị (Median) mà không làm biến đổi (mutate)
 *   dataset đầu vào, bảo toàn tính bất biến (Immutability).
 *
 * @param sortAlgorithm Thuật toán sắp xếp sử dụng cho tính toán Median, mặc định là [QuickSort].
 */
class DescriptiveStatisticsCalculator(
    private val sortAlgorithm: SortAlgorithm<Double> = QuickSort()
) : StatisticsCalculator {

    private val doubleComparator = Comparator<Double> { a, b -> a.compareTo(b) }

    override fun mean(dataset: List<Double>): Double {
        if (dataset.isEmpty()) return Double.NaN
        var sum = 0.0
        for (i in dataset.indices) {
            sum += dataset[i]
        }
        return sum / dataset.size
    }

    override fun median(dataset: List<Double>): Double {
        if (dataset.isEmpty()) return Double.NaN
        val sorted = sortAlgorithm.sort(dataset, doubleComparator)
        val n = sorted.size
        return if (n % 2 != 0) {
            sorted[n / 2]
        } else {
            (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0
        }
    }

    override fun min(dataset: List<Double>): Double {
        if (dataset.isEmpty()) return Double.NaN
        var minVal = dataset[0]
        for (i in 1 until dataset.size) {
            val v = dataset[i]
            if (v < minVal) minVal = v
        }
        return minVal
    }

    override fun max(dataset: List<Double>): Double {
        if (dataset.isEmpty()) return Double.NaN
        var maxVal = dataset[0]
        for (i in 1 until dataset.size) {
            val v = dataset[i]
            if (v > maxVal) maxVal = v
        }
        return maxVal
    }

    override fun range(dataset: List<Double>): Double {
        if (dataset.isEmpty()) return Double.NaN
        var minVal = dataset[0]
        var maxVal = dataset[0]
        for (i in 1 until dataset.size) {
            val v = dataset[i]
            if (v < minVal) minVal = v
            if (v > maxVal) maxVal = v
        }
        return maxVal - minVal
    }

    override fun populationVariance(dataset: List<Double>): Double {
        val n = dataset.size
        if (n == 0) return Double.NaN
        if (n == 1) return 0.0

        val m = mean(dataset)
        var sumSquaredDiff = 0.0
        for (i in dataset.indices) {
            val diff = dataset[i] - m
            sumSquaredDiff += diff * diff
        }
        return sumSquaredDiff / n
    }

    override fun sampleVariance(dataset: List<Double>): Double {
        val n = dataset.size
        if (n < 2) return Double.NaN

        val m = mean(dataset)
        var sumSquaredDiff = 0.0
        for (i in dataset.indices) {
            val diff = dataset[i] - m
            sumSquaredDiff += diff * diff
        }
        return sumSquaredDiff / (n - 1)
    }

    override fun populationStandardDeviation(dataset: List<Double>): Double {
        val variance = populationVariance(dataset)
        return if (variance.isNaN()) Double.NaN else sqrt(variance)
    }

    override fun sampleStandardDeviation(dataset: List<Double>): Double {
        val variance = sampleVariance(dataset)
        return if (variance.isNaN()) Double.NaN else sqrt(variance)
    }

    override fun skewness(dataset: List<Double>): Double {
        val n = dataset.size
        if (n == 0) return Double.NaN
        if (n == 1) return 0.0

        val m = mean(dataset)
        var m2Sum = 0.0
        var m3Sum = 0.0
        for (i in dataset.indices) {
            val diff = dataset[i] - m
            val diffSq = diff * diff
            m2Sum += diffSq
            m3Sum += diffSq * diff
        }

        val popVariance = m2Sum / n
        val popStdDev = sqrt(popVariance)

        // Nếu phương sai bằng 0, phân phối hoàn toàn đối xứng xung quanh giá trị trung bình
        if (popStdDev == 0.0) return 0.0

        val m3 = m3Sum / n
        val stdDevCubed = popStdDev * popStdDev * popStdDev
        return m3 / stdDevCubed
    }

    override fun summarize(dataset: List<Double>): DescriptiveStatistics {
        val n = dataset.size
        if (n == 0) {
            return DescriptiveStatistics(
                count = 0,
                mean = Double.NaN,
                median = Double.NaN,
                min = Double.NaN,
                max = Double.NaN,
                range = Double.NaN,
                populationVariance = Double.NaN,
                sampleVariance = Double.NaN,
                populationStandardDeviation = Double.NaN,
                sampleStandardDeviation = Double.NaN,
                skewness = Double.NaN
            )
        }

        // Phase 1: Single-pass thu thập Count, Sum, Min, Max
        var sum = dataset[0]
        var minVal = dataset[0]
        var maxVal = dataset[0]

        for (i in 1 until n) {
            val v = dataset[i]
            sum += v
            if (v < minVal) minVal = v
            if (v > maxVal) maxVal = v
        }

        val meanVal = sum / n
        val rangeVal = maxVal - minVal

        // Phase 2: Second-pass tính các mô-men trung tâm M2 (Variance) và M3 (Skewness)
        var m2Sum = 0.0
        var m3Sum = 0.0

        for (i in 0 until n) {
            val diff = dataset[i] - meanVal
            val diffSq = diff * diff
            m2Sum += diffSq
            m3Sum += diffSq * diff
        }

        val popVariance = m2Sum / n
        val sampleVariance = if (n >= 2) m2Sum / (n - 1) else Double.NaN
        val popStdDev = sqrt(popVariance)
        val sampleStdDev = if (n >= 2) sqrt(sampleVariance) else Double.NaN

        val skewnessVal = if (popStdDev == 0.0 || n == 1) {
            0.0
        } else {
            val m3 = m3Sum / n
            m3 / (popStdDev * popStdDev * popStdDev)
        }

        // Phase 3: Sắp xếp một lần để trích xuất Median
        val sorted = sortAlgorithm.sort(dataset, doubleComparator)
        val medianVal = if (n % 2 != 0) {
            sorted[n / 2]
        } else {
            (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0
        }

        return DescriptiveStatistics(
            count = n,
            mean = meanVal,
            median = medianVal,
            min = minVal,
            max = maxVal,
            range = rangeVal,
            populationVariance = popVariance,
            sampleVariance = sampleVariance,
            populationStandardDeviation = popStdDev,
            sampleStandardDeviation = sampleStdDev,
            skewness = skewnessVal
        )
    }
}
