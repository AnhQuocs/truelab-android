package dev.anhquocs.truelab.core.domain.prediction.transformer

import dev.anhquocs.truelab.core.algorithm.evaluation.FormScore
import dev.anhquocs.truelab.core.algorithm.prediction.Signal3Way
import dev.anhquocs.truelab.core.domain.prediction.model.DrawModelingStrategy
import dev.anhquocs.truelab.core.domain.prediction.model.DrawStrategyConfig
import dev.anhquocs.truelab.core.domain.prediction.model.PredictionWeightConfig
import dev.anhquocs.truelab.core.domain.prediction.policy.DynamicDrawPriorCalculator

/**
 * Chuyển đổi điểm phong độ [FormScore] (thang [0, 100]) của 2 đội thành [Signal3Way] với Baseline Draw hoặc Dynamic Draw Prior Decomposition.
 */
object FormSignalTransformer {

    fun transform(
        homeForm: FormScore?,
        awayForm: FormScore?,
        config: PredictionWeightConfig = PredictionWeightConfig.DEFAULT,
        drawStrategyConfig: DrawStrategyConfig = DrawStrategyConfig.DEFAULT
    ): Signal3Way {
        val rawH = homeForm?.score?.takeIf { it.isFinite() && it in 0.0..100.0 }
        val rawA = awayForm?.score?.takeIf { it.isFinite() && it in 0.0..100.0 }

        val sH = (rawH ?: 50.0) / 100.0
        val sA = (rawA ?: 50.0) / 100.0

        val eps = config.formSmoothingEpsilon
        val rH = (sH + eps) / (sH + sA + 2.0 * eps)
        val rA = 1.0 - rH

        val drawProb = when (drawStrategyConfig.strategy) {
            DrawModelingStrategy.DYNAMIC_DRAW_PRIOR -> {
                val deltaForm = sH - sA
                DynamicDrawPriorCalculator.computePrior(
                    delta = deltaForm,
                    sigma = drawStrategyConfig.dynamicPriorConfig.formSigma,
                    maxDrawProb = drawStrategyConfig.dynamicPriorConfig.maxDrawProb,
                    minDrawProb = drawStrategyConfig.dynamicPriorConfig.minDrawProb
                )
            }
            DrawModelingStrategy.BASELINE,
            DrawModelingStrategy.DECISION_MARGIN -> {
                config.baselineDrawProb
            }
        }

        val nonDrawWeight = 1.0 - drawProb

        val homeProb = rH * nonDrawWeight
        val awayProb = rA * nonDrawWeight

        return Signal3Way(
            homeProb = homeProb,
            drawProb = drawProb,
            awayProb = awayProb,
            weight = config.formWeight,
            name = "Form"
        )
    }
}
