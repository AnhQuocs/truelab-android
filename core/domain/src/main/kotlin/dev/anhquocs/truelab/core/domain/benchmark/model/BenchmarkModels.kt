package dev.anhquocs.truelab.core.domain.benchmark.model

/**
 * Đo lường hiệu năng thực thi của một thuật toán đơn lẻ trên một kích thước tập dữ liệu nhất định.
 *
 * @property algorithmName Tên thuật toán (ví dụ: "Linear Search", "Binary Search", "Quick Sort", "Merge Sort").
 * @property algorithmType Loại thuật toán ("SEARCH" hoặc "SORT").
 * @property datasetSize Kích thước tập dữ liệu đầu vào (ví dụ: 1000, 10000, 50000).
 * @property executionTimeMs Thời gian thực thi trung bình tính bằng mili-giây (ms).
 * @property iterationsRun Số lần lặp đo lường thực tế để lấy giá trị trung bình.
 */
data class BenchmarkMeasurement(
    val algorithmName: String,
    val algorithmType: String,
    val datasetSize: Int,
    val executionTimeMs: Double,
    val iterationsRun: Int
)

/**
 * Kết quả so sánh hiệu năng giữa hai thuật toán cùng loại trên cùng một tập dữ liệu.
 *
 * @property algorithmA Đo lường của thuật toán A.
 * @property algorithmB Đo lường của thuật toán B.
 * @property speedupFactor Hệ số tăng tốc (thời gian chạy chậm hơn / thời gian chạy nhanh hơn, >= 1.0).
 * @property fasterAlgorithm Tên thuật toán có thời gian thực thi nhanh hơn ("Equal" nếu bằng nhau).
 */
data class ComparisonBenchmarkResult(
    val algorithmA: BenchmarkMeasurement,
    val algorithmB: BenchmarkMeasurement,
    val speedupFactor: Double,
    val fasterAlgorithm: String
)

/**
 * Đóng gói toàn bộ kết quả đo lường benchmark của hệ thống cho các nhóm Search và Sort.
 *
 * @property searchBenchmarks Danh sách so sánh các thuật toán tìm kiếm (Linear Search vs Binary Search) trên các kích thước dữ liệu.
 * @property sortBenchmarks Danh sách so sánh các thuật toán sắp xếp (Quick Sort vs Merge Sort) trên các kích thước dữ liệu.
 * @property timestamp Thời điểm thực thi benchmark (epoch milliseconds).
 */
data class ComprehensiveBenchmarkSuiteResult(
    val searchBenchmarks: List<ComparisonBenchmarkResult>,
    val sortBenchmarks: List<ComparisonBenchmarkResult>,
    val timestamp: Long
)
