package dev.anhquocs.truelab.core.algorithm.statistics

/**
 * Đóng gói toàn bộ các chỉ số thống kê mô tả tổng hợp của một tập dữ liệu số.
 *
 * @property count Số lượng phần tử trong tập dữ liệu.
 * @property mean Giá trị trung bình số học (Arithmetic Mean).
 * @property median Giá trị trung vị (Median).
 * @property min Giá trị nhỏ nhất (Minimum).
 * @property max Giá trị lớn nhất (Maximum).
 * @property range Khoảng biến thiên (Range = Max - Min).
 * @property populationVariance Phương sai tổng thể (chia cho N).
 * @property sampleVariance Phương sai mẫu hiệu chỉnh Bessel (chia cho N - 1).
 * @property populationStandardDeviation Độ lệch chuẩn tổng thể (căn bậc hai của Population Variance).
 * @property sampleStandardDeviation Độ lệch chuẩn mẫu (căn bậc hai của Sample Variance).
 * @property skewness Hệ số bất đối xứng mô-men (Moment Coefficient of Skewness).
 */
data class DescriptiveStatistics(
    val count: Int,
    val mean: Double,
    val median: Double,
    val min: Double,
    val max: Double,
    val range: Double,
    val populationVariance: Double,
    val sampleVariance: Double,
    val populationStandardDeviation: Double,
    val sampleStandardDeviation: Double,
    val skewness: Double
)
