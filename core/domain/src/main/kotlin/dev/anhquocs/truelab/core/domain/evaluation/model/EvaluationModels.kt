package dev.anhquocs.truelab.core.domain.evaluation.model

/**
 * Ma trận nhầm lẫn 3 chiều (3x3 Confusion Matrix) cho bài toán dự đoán kết quả trận đấu bóng đá.
 *
 * Định hướng (Orientation):
 *                         Dự đoán (Predicted)
 *                    HOME_WIN   DRAW   AWAY_WIN
 * Thực tế HOME_WIN [ homeAsHome, homeAsDraw, homeAsAway ]
 * Thực tế DRAW     [ drawAsHome, drawAsDraw, drawAsAway ]
 * Thực tế AWAY_WIN [ awayAsHome, awayAsDraw, awayAsAway ]
 *
 * @property homeAsHome Số mẫu thực tế là HOME_WIN và được dự đoán là HOME_WIN (True Home).
 * @property homeAsDraw Số mẫu thực tế là HOME_WIN nhưng bị dự đoán nhầm là DRAW.
 * @property homeAsAway Số mẫu thực tế là HOME_WIN nhưng bị dự đoán nhầm là AWAY_WIN.
 * @property drawAsHome Số mẫu thực tế là DRAW nhưng bị dự đoán nhầm là HOME_WIN.
 * @property drawAsDraw Số mẫu thực tế là DRAW và được dự đoán là DRAW (True Draw).
 * @property drawAsAway Số mẫu thực tế là DRAW nhưng bị dự đoán nhầm là AWAY_WIN.
 * @property awayAsHome Số mẫu thực tế là AWAY_WIN nhưng bị dự đoán nhầm là HOME_WIN.
 * @property awayAsDraw Số mẫu thực tế là AWAY_WIN nhưng bị dự đoán nhầm là DRAW.
 * @property awayAsAway Số mẫu thực tế là AWAY_WIN và được dự đoán là AWAY_WIN (True Away).
 */
data class ConfusionMatrix3Way(
    val homeAsHome: Int = 0,
    val homeAsDraw: Int = 0,
    val homeAsAway: Int = 0,
    val drawAsHome: Int = 0,
    val drawAsDraw: Int = 0,
    val drawAsAway: Int = 0,
    val awayAsHome: Int = 0,
    val awayAsDraw: Int = 0,
    val awayAsAway: Int = 0
) {
    /**
     * Tổng số mẫu đánh giá trong ma trận.
     */
    val totalSamples: Int
        get() = homeAsHome + homeAsDraw + homeAsAway +
            drawAsHome + drawAsDraw + drawAsAway +
            awayAsHome + awayAsDraw + awayAsAway

    /**
     * Tổng số mẫu được dự đoán chính xác (nằm trên đường chéo chính).
     */
    val correctPredictions: Int
        get() = homeAsHome + drawAsDraw + awayAsAway
}

/**
 * Chỉ số đánh giá chi tiết cho từng lớp kết quả (Class-level Metrics).
 *
 * @property precision Độ chuẩn xác (TP / tổng số lần dự đoán lớp này).
 * @property recall Độ bao phủ / Độ nhạy (TP / tổng số mẫu thực tế thuộc lớp này).
 * @property f1Score Điểm F1 trung hòa giữa Precision và Recall: 2 * (P * R) / (P + R).
 * @property support Số lượng mẫu thực tế thuộc lớp này trong tập đánh giá.
 */
data class ClassEvaluationMetrics(
    val precision: Double,
    val recall: Double,
    val f1Score: Double,
    val support: Int
)

/**
 * Kết quả đánh giá tổng thể mô hình dự đoán (Overall Model Evaluation Result).
 *
 * @property accuracy Độ chính xác tổng thể (correct / total).
 * @property macroPrecision Trung bình cộng Precision của 3 lớp (Home, Draw, Away).
 * @property macroRecall Trung bình cộng Recall của 3 lớp (Home, Draw, Away).
 * @property macroF1 Trung bình cộng F1-Score của 3 lớp (Home, Draw, Away).
 * @property homeMetrics Chỉ số đánh giá chi tiết cho lớp HOME_WIN.
 * @property drawMetrics Chỉ số đánh giá chi tiết cho lớp DRAW.
 * @property awayMetrics Chỉ số đánh giá chi tiết cho lớp AWAY_WIN.
 * @property confusionMatrix Ma trận nhầm lẫn 3 chiều tương ứng.
 * @property totalEvaluated Tổng số mẫu đã được đánh giá.
 */
data class ModelEvaluationResult(
    val accuracy: Double,
    val macroPrecision: Double,
    val macroRecall: Double,
    val macroF1: Double,
    val homeMetrics: ClassEvaluationMetrics,
    val drawMetrics: ClassEvaluationMetrics,
    val awayMetrics: ClassEvaluationMetrics,
    val confusionMatrix: ConfusionMatrix3Way,
    val totalEvaluated: Int
)
