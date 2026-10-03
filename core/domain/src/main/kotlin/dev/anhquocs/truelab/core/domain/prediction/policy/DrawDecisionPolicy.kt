package dev.anhquocs.truelab.core.domain.prediction.policy

import dev.anhquocs.truelab.core.algorithm.prediction.OutcomeProbabilities
import dev.anhquocs.truelab.core.domain.prediction.model.DrawMarginConfig
import dev.anhquocs.truelab.core.domain.prediction.model.DrawModelingStrategy
import dev.anhquocs.truelab.core.domain.prediction.model.DrawStrategyConfig
import kotlin.math.abs

/**
 * Chính sách ra quyết định kết quả dự đoán (Draw Decision Policy).
 *
 * Nhiệm vụ duy nhất: Xác định nhãn dự đoán cuối cùng ("HOME_WIN", "DRAW", "AWAY_WIN") từ vector xác suất
 * (homeWinProb, drawProb, awayWinProb) theo chiến lược [DrawModelingStrategy].
 */
object DrawDecisionPolicy {

    /**
     * Quyết định nhãn kết quả dự đoán dựa trên vector xác suất và cấu hình chiến lược.
     *
     * @param probabilities Phân phối xác suất 3 chiều từ WeightedScorer.
     * @param strategyConfig Cấu hình chiến lược Draw Modeling.
     * @return Chuỗi nhãn kết quả dự đoán ("HOME_WIN", "DRAW", "AWAY_WIN").
     */
    fun resolvePredictedOutcome(
        probabilities: OutcomeProbabilities,
        strategyConfig: DrawStrategyConfig = DrawStrategyConfig.DEFAULT
    ): String {
        return when (strategyConfig.strategy) {
            DrawModelingStrategy.BASELINE -> {
                probabilities.predictedOutcome.name
            }
            DrawModelingStrategy.DECISION_MARGIN -> {
                applyDecisionMargin(
                    probabilities = probabilities,
                    config = strategyConfig.marginConfig
                )
            }
            DrawModelingStrategy.DYNAMIC_DRAW_PRIOR -> {
                // Candidate B uses natural argmax over the dynamically calibrated probabilities
                probabilities.predictedOutcome.name
            }
        }
    }

    /**
     * Áp dụng quy tắc phân ngưỡng chênh lệch (Candidate A: Decision Margin / Relative Threshold):
     *
     * IF |P_Home - P_Away| < deltaMargin AND P_Draw >= thetaMinProb
     *    -> "DRAW"
     * ELSE
     *    -> baseline predictedOutcome.name
     *
     * @param probabilities Phân phối xác suất 3 chiều từ WeightedScorer.
     * @param config Cấu hình tham số ngưỡng deltaMargin và thetaMinProb.
     * @return Chuỗi nhãn kết quả dự đoán.
     */
    fun applyDecisionMargin(
        probabilities: OutcomeProbabilities,
        config: DrawMarginConfig
    ): String {
        val homeAwayMargin = abs(probabilities.homeWinProb - probabilities.awayWinProb)
        val drawProb = probabilities.drawProb

        return if (homeAwayMargin < config.deltaMargin && drawProb >= config.thetaMinProb) {
            "DRAW"
        } else {
            probabilities.predictedOutcome.name
        }
    }
}
