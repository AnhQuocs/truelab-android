package dev.anhquocs.truelab.core.domain.prediction.policy

import dev.anhquocs.truelab.core.algorithm.prediction.OutcomeProbabilities
import dev.anhquocs.truelab.core.algorithm.prediction.PredictedOutcome
import dev.anhquocs.truelab.core.domain.prediction.model.DrawDecisionRule
import dev.anhquocs.truelab.core.domain.prediction.model.DrawMarginConfig
import dev.anhquocs.truelab.core.domain.prediction.model.DrawStrategyConfig
import dev.anhquocs.truelab.core.domain.prediction.model.RelativeDrawGapConfig
import kotlin.math.abs

/**
 * Chính sách ra quyết định kết quả dự đoán (Draw Decision Policy).
 *
 * Nhiệm vụ duy nhất: Xác định nhãn dự đoán cuối cùng ("HOME_WIN", "DRAW", "AWAY_WIN") từ vector xác suất
 * (homeWinProb, drawProb, awayWinProb) theo quy tắc quyết định [DrawDecisionRule].
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
        return when (strategyConfig.resolvedDecisionRule) {
            DrawDecisionRule.RAW_ARGMAX -> {
                probabilities.predictedOutcome.name
            }
            DrawDecisionRule.DECISION_MARGIN -> {
                applyDecisionMargin(
                    probabilities = probabilities,
                    config = strategyConfig.marginConfig
                )
            }
            DrawDecisionRule.RELATIVE_DRAW_GAP -> {
                applyRelativeDrawGap(
                    probabilities = probabilities,
                    config = strategyConfig.relativeGapConfig
                )
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

    /**
     * Áp dụng quy tắc khoảng cách hòa tương đối (Candidate B: Relative Draw Gap):
     *
     * drawDeficit = max(P_Home, P_Away) - P_Draw
     *
     * IF raw argmax is DRAW -> "DRAW"
     * ELSE IF drawDeficit <= tauThreshold -> "DRAW"
     * ELSE -> baseline predictedOutcome.name
     *
     * @param probabilities Phân phối xác suất 3 chiều từ WeightedScorer.
     * @param config Cấu hình tham số ngưỡng dung sai tauThreshold.
     * @return Chuỗi nhãn kết quả dự đoán.
     */
    fun applyRelativeDrawGap(
        probabilities: OutcomeProbabilities,
        config: RelativeDrawGapConfig
    ): String {
        if (probabilities.predictedOutcome == PredictedOutcome.DRAW) {
            return "DRAW"
        }
        val maxHomeAway = maxOf(probabilities.homeWinProb, probabilities.awayWinProb)
        val drawDeficit = maxHomeAway - probabilities.drawProb

        return if (drawDeficit <= config.tauThreshold) {
            "DRAW"
        } else {
            probabilities.predictedOutcome.name
        }
    }
}

