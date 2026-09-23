package dev.anhquocs.truelab.core.algorithm.evaluation

/**
 * Interface contract định nghĩa các thuật toán đánh giá phong độ thi đấu (Form Evaluation).
 */
interface FormEvaluator {

    /**
     * Đánh giá phong độ dựa trên danh sách kết quả thi đấu theo thứ tự thời gian tăng dần
     * (phần tử cuối danh sách là trận mới nhất).
     *
     * @param outcomes Danh sách kết quả thi đấu.
     * @param windowSize Kích thước cửa sổ đánh giá (mặc định là 5, phải > 0).
     * @return Đối tượng [FormScore].
     * @throws IllegalArgumentException nếu [windowSize] <= 0.
     */
    fun evaluate(outcomes: List<MatchOutcome>, windowSize: Int = 5): FormScore

    /**
     * Generic Selector Overload cho phép tính toán trực tiếp trên danh sách đối tượng tùy biến
     * mà không tạo mảng trung gian.
     *
     * @param dataset Danh sách đối tượng đầu vào.
     * @param windowSize Kích thước cửa sổ đánh giá (mặc định là 5, phải > 0).
     * @param selector Hàm trích xuất [MatchOutcome] tương ứng từ đối tượng.
     * @return Đối tượng [FormScore].
     * @throws IllegalArgumentException nếu [windowSize] <= 0.
     */
    fun <T> evaluate(
        dataset: List<T>,
        windowSize: Int = 5,
        selector: (T) -> MatchOutcome
    ): FormScore
}
