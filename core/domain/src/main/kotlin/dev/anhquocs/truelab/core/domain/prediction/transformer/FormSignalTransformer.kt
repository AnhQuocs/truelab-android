package dev.anhquocs.truelab.core.domain.prediction.transformer

import dev.anhquocs.truelab.core.algorithm.evaluation.FormScore
import dev.anhquocs.truelab.core.algorithm.prediction.Signal3Way
import dev.anhquocs.truelab.core.domain.prediction.model.PredictionWeightConfig

/**
 * Chuyển đổi điểm phong độ [FormScore] (thang [0, 100]) của 2 đội thành [Signal3Way] với Relative Strength & Smoothing.
 */
object FormSignalTransformer {

    fun transform(
        homeForm: FormScore?,
        awayForm: FormScore?,
        config: PredictionWeightConfig = PredictionWeightConfig.DEFAULT
    ): Signal3Way {
        val rawH = homeForm?.score?.takeIf { it.isFinite() && it in 0.0..100.0 }
        val rawA = awayForm?.score?.takeIf { it.isFinite() && it in 0.0..100.0 }

        val sH = (rawH ?: 50.0) / 100.0
        val sA = (rawA ?: 50.0) / 100.0

        val eps = config.formSmoothingEpsilon
        val rH = (sH + eps) / (sH + sA + 2.0 * eps)
        val rA = 1.0 - rH

        val drawProb = config.baselineDrawProb
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
