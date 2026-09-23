package dev.anhquocs.truelab.core.algorithm.trend

/**
 * Interface contract định nghĩa các thuật toán tính toán Đường trung bình động (Moving Average)
 * nhằm làm mịn chuỗi biến động dữ liệu theo thời gian.
 */
interface MovingAverageCalculator {

    /**
     * Tính toán Simple Moving Average (SMA) trên danh sách số thực với kích thước cửa sổ [windowSize].
     *
     * @param dataset Danh sách các giá trị số thực theo thứ tự thời gian.
     * @param windowSize Kích thước cửa sổ trượt (phải > 0).
     * @return Danh sách các giá trị trung bình động hợp lệ (kích thước N - windowSize + 1 khi N >= windowSize).
     * @throws IllegalArgumentException nếu [windowSize] <= 0.
     */
    fun calculate(dataset: List<Double>, windowSize: Int): List<Double>

    /**
     * Overload hỗ trợ Generic Selector cho phép tính toán trên danh sách đối tượng tùy biến
     * mà không làm biến đổi danh sách gốc.
     *
     * @param dataset Danh sách đối tượng đầu vào.
     * @param windowSize Kích thước cửa sổ trượt (phải > 0).
     * @param selector Hàm trích xuất giá trị số thực cần tính toán từ đối tượng.
     * @return Danh sách các giá trị trung bình động.
     * @throws IllegalArgumentException nếu [windowSize] <= 0.
     */
    fun <T> calculate(
        dataset: List<T>,
        windowSize: Int,
        selector: (T) -> Double
    ): List<Double> = calculate(dataset.map(selector), windowSize)
}
