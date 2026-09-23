package dev.anhquocs.truelab.core.domain.prediction.transformer

import dev.anhquocs.truelab.core.algorithm.prediction.Signal3Way
import dev.anhquocs.truelab.core.domain.prediction.model.PredictionWeightConfig

/**
 * Chuyển đổi hiệu suất bàn thắng trung bình gần đây của 2 đội thành [Signal3Way] với Clamped Linear Mapping.
 */
object GoalsSignalTransformer {

    fun transform(
        homeMeanScored: Double?,
        homeMeanConceded: Double?,
        awayMeanScored: Double?,
        awayMeanConceded: Double?,
        config: PredictionWeightConfig = PredictionWeightConfig.DEFAULT
    ): Signal3Way {
        val hScored = homeMeanScored?.takeIf { it.isFinite() && it >= 0.0 } ?: 1.3
        val hConceded = homeMeanConceded?.takeIf { it.isFinite() && it >= 0.0 } ?: 1.3
        val aScored = awayMeanScored?.takeIf { it.isFinite() && it >= 0.0 } ?: 1.1
        val aConceded = awayMeanConceded?.takeIf { it.isFinite() && it >= 0.0 } ?: 1.5

        val lambdaHome = (hScored + aConceded) / 2.0
        val lambdaAway = (aScored + hConceded) / 2.0
        val deltaLambda = lambdaHome - lambdaAway

        val drawProb = config.baselineDrawProb
        val nonDrawWeight = 1.0 - drawProb
        val baseHome = nonDrawWeight / 2.0 // 0.37 khi drawProb = 0.26

        val rawHomeProb = baseHome + deltaLambda * config.goalsSensitivity
        val clampedHomeProb = rawHomeProb.coerceIn(config.goalsMinProbHome, config.goalsMaxProbHome)
        val awayProb = nonDrawWeight - clampedHomeProb

        return Signal3Way(
            homeProb = clampedHomeProb,
            drawProb = drawProb,
            awayProb = awayProb,
            weight = config.goalsWeight,
            name = "Goals"
        )
    }
}
