package dev.anhquocs.truelab.core.algorithm.prediction

/**
 * Interface contract định nghĩa các thuật toán dự đoán và tính điểm trọng số (Weighted Scoring).
 */
interface WeightedScorer {

    /**
     * Tính điểm tổng hợp trọng số từ danh sách đặc trưng [WeightedFeature].
     *
     * Công thức: S = Σ(w_i * x_i) / Σw_i
     *
     * @param features Danh sách các đặc trưng (mỗi đặc trưng có score trong [0.0, 1.0] và weight >= 0.0).
     * @return Điểm tổng hợp trọng số trong đoạn [0.0, 1.0].
     * @throws IllegalArgumentException nếu danh sách rỗng, có score/weight không hữu hạn,
     *         score ngoài [0.0, 1.0], weight < 0.0, hoặc tổng weight <= 0.0.
     */
    fun calculateScore(
        features: List<WeightedFeature>
    ): Double

    /**
     * Generic selector overload cho phép tính điểm trọng số trực tiếp trên tập dữ liệu tùy biến.
     *
     * @param dataset Danh sách đối tượng đầu vào.
     * @param scoreSelector Hàm trích xuất giá trị điểm số (phải thuộc [0.0, 1.0] và hữu hạn).
     * @param weightSelector Hàm trích xuất trọng số (phải >= 0.0 và hữu hạn).
     * @return Điểm tổng hợp trọng số trong đoạn [0.0, 1.0].
     * @throws IllegalArgumentException nếu danh sách rỗng, hoặc có giá trị không hợp lệ.
     */
    fun <T> calculateScore(
        dataset: List<T>,
        scoreSelector: (T) -> Double,
        weightSelector: (T) -> Double
    ): Double

    /**
     * Chuẩn hóa 3 điểm số thô thành phân phối xác suất [OutcomeProbabilities].
     *
     * P(H) = homeScore / total
     * P(D) = drawScore / total
     * P(A) = awayScore / total
     *
     * @param homeScore Điểm thô của đội nhà (>= 0.0, hữu hạn).
     * @param drawScore Điểm thô của kết quả hòa (>= 0.0, hữu hạn).
     * @param awayScore Điểm thô của đội khách (>= 0.0, hữu hạn).
     * @return Đối tượng [OutcomeProbabilities] chứa 3 xác suất, kết quả dự đoán và độ tin cậy.
     * @throws IllegalArgumentException nếu có điểm âm, không hữu hạn, hoặc tổng 3 điểm <= 0.0.
     */
    fun predict3Way(
        homeScore: Double,
        drawScore: Double,
        awayScore: Double
    ): OutcomeProbabilities

    /**
     * Dự đoán phân phối xác suất dựa trên tổ hợp tuyến tính (Linear Mixture) từ danh sách các tín hiệu [Signal3Way].
     *
     * P_c = Σ(w_k * p_k,c) / Σw_k cho c in {Home, Draw, Away}.
     *
     * @param signals Danh sách các tín hiệu xác suất 3 chiều kèm trọng số.
     * @return Đối tượng [OutcomeProbabilities] chứa 3 xác suất, kết quả dự đoán và độ tin cậy.
     * @throws IllegalArgumentException nếu danh sách rỗng, có xác suất ngoài [0.0, 1.0], không hữu hạn,
     *         tổng xác suất mỗi tín hiệu không xấp xỉ 1.0, trọng số âm, hoặc tổng trọng số <= 0.0.
     */
    fun predictOutcome(
        signals: List<Signal3Way>
    ): OutcomeProbabilities
}
