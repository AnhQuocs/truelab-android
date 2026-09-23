package dev.anhquocs.truelab.core.domain.prediction.transformer

import dev.anhquocs.truelab.core.algorithm.prediction.Signal3Way
import dev.anhquocs.truelab.core.domain.prediction.model.PredictionWeightConfig

/**
 * Chuyển đổi thống kê lịch sử đối đầu (H2H) thành [Signal3Way] bằng Laplace Smoothing / Bayesian Prior.
 */
object H2hSignalTransformer {

    fun transform(
        homeWins: Int,
        draws: Int,
        awayWins: Int,
        config: PredictionWeightConfig = PredictionWeightConfig.DEFAULT
    ): Signal3Way {
        val hw = if (homeWins >= 0) homeWins else 0
        val d = if (draws >= 0) draws else 0
        val aw = if (awayWins >= 0) awayWins else 0

        val n = (hw + d + aw).toDouble()
        val k = config.h2hPriorK

        val denominator = n + k

        val homeProb = (hw + k * config.h2hPriorHome) / denominator
        val drawProb = (d + k * config.h2hPriorDraw) / denominator
        val awayProb = (aw + k * config.h2hPriorAway) / denominator

        return Signal3Way(
            homeProb = homeProb,
            drawProb = drawProb,
            awayProb = awayProb,
            weight = config.h2hWeight,
            name = "H2H"
        )
    }
}
