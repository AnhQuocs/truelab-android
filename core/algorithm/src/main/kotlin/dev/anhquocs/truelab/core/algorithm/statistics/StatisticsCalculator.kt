package dev.anhquocs.truelab.core.algorithm.statistics

/**
 * Interface contract định nghĩa các thuật toán tính toán thống kê mô tả.
 */
interface StatisticsCalculator {

    /**
     * Tính giá trị trung bình số học (Arithmetic Mean).
     * @return Mean, hoặc Double.NaN nếu danh sách rỗng.
     */
    fun mean(dataset: List<Double>): Double

    /**
     * Tính giá trị trung vị (Median).
     * @return Median, hoặc Double.NaN nếu danh sách rỗng.
     */
    fun median(dataset: List<Double>): Double

    /**
     * Tìm giá trị nhỏ nhất (Minimum).
     * @return Min, hoặc Double.NaN nếu danh sách rỗng.
     */
    fun min(dataset: List<Double>): Double

    /**
     * Tìm giá trị lớn nhất (Maximum).
     * @return Max, hoặc Double.NaN nếu danh sách rỗng.
     */
    fun max(dataset: List<Double>): Double

    /**
     * Tính khoảng biến thiên (Range = Max - Min).
     * @return Range, hoặc Double.NaN nếu danh sách rỗng.
     */
    fun range(dataset: List<Double>): Double

    /**
     * Tính phương sai tổng thể (Population Variance, chia cho N).
     * @return Population Variance, hoặc Double.NaN nếu danh sách rỗng.
     */
    fun populationVariance(dataset: List<Double>): Double

    /**
     * Tính phương sai mẫu hiệu chỉnh Bessel (Sample Variance, chia cho N - 1).
     * @return Sample Variance, hoặc Double.NaN nếu N < 2.
     */
    fun sampleVariance(dataset: List<Double>): Double

    /**
     * Tính độ lệch chuẩn tổng thể (Population Standard Deviation).
     * @return Population StdDev, hoặc Double.NaN nếu danh sách rỗng.
     */
    fun populationStandardDeviation(dataset: List<Double>): Double

    /**
     * Tính độ lệch chuẩn mẫu (Sample Standard Deviation).
     * @return Sample StdDev, hoặc Double.NaN nếu N < 2.
     */
    fun sampleStandardDeviation(dataset: List<Double>): Double

    /**
     * Tính hệ số bất đối xứng mô-men (Moment Coefficient of Skewness).
     * @return Skewness, hoặc Double.NaN nếu danh sách rỗng, 0.0 nếu độ lệch chuẩn bằng 0.
     */
    fun skewness(dataset: List<Double>): Double

    /**
     * Tính toán tổng hợp toàn bộ các chỉ số thống kê mô tả trong một chu trình tối ưu.
     * @return Đối tượng [DescriptiveStatistics].
     */
    fun summarize(dataset: List<Double>): DescriptiveStatistics

    // --- Generic overloads with selector lambda ---

    fun <T> mean(dataset: List<T>, selector: (T) -> Double): Double =
        mean(dataset.map(selector))

    fun <T> median(dataset: List<T>, selector: (T) -> Double): Double =
        median(dataset.map(selector))

    fun <T> min(dataset: List<T>, selector: (T) -> Double): Double =
        min(dataset.map(selector))

    fun <T> max(dataset: List<T>, selector: (T) -> Double): Double =
        max(dataset.map(selector))

    fun <T> range(dataset: List<T>, selector: (T) -> Double): Double =
        range(dataset.map(selector))

    fun <T> populationVariance(dataset: List<T>, selector: (T) -> Double): Double =
        populationVariance(dataset.map(selector))

    fun <T> sampleVariance(dataset: List<T>, selector: (T) -> Double): Double =
        sampleVariance(dataset.map(selector))

    fun <T> populationStandardDeviation(dataset: List<T>, selector: (T) -> Double): Double =
        populationStandardDeviation(dataset.map(selector))

    fun <T> sampleStandardDeviation(dataset: List<T>, selector: (T) -> Double): Double =
        sampleStandardDeviation(dataset.map(selector))

    fun <T> skewness(dataset: List<T>, selector: (T) -> Double): Double =
        skewness(dataset.map(selector))

    fun <T> summarize(dataset: List<T>, selector: (T) -> Double): DescriptiveStatistics =
        summarize(dataset.map(selector))
}
