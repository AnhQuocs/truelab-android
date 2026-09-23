package dev.anhquocs.truelab.core.domain.prediction.transformer

import dev.anhquocs.truelab.core.algorithm.prediction.Signal3Way
import dev.anhquocs.truelab.core.algorithm.rating.EloRatingCalculator
import dev.anhquocs.truelab.core.algorithm.rating.RatingCalculator
import dev.anhquocs.truelab.core.domain.prediction.model.PredictionWeightConfig

/**
 * Chuyển đổi điểm Elo của 2 đội thành [Signal3Way] với Baseline Draw Decomposition.
 */
object EloSignalTransformer {

    fun transform(
        homeElo: Double?,
        awayElo: Double?,
        config: PredictionWeightConfig = PredictionWeightConfig.DEFAULT,
        ratingCalculator: RatingCalculator = EloRatingCalculator()
    ): Signal3Way {
        val hElo = if (homeElo != null && homeElo.isFinite() && homeElo > 0.0) homeElo else 1500.0
        val aElo = if (awayElo != null && awayElo.isFinite() && awayElo > 0.0) awayElo else 1500.0

        val expectedHome = ratingCalculator.expectedScore(hElo, aElo)
        val expectedAway = 1.0 - expectedHome

        val drawProb = config.baselineDrawProb
        val nonDrawWeight = 1.0 - drawProb

        val homeProb = expectedHome * nonDrawWeight
        val awayProb = expectedAway * nonDrawWeight

        return Signal3Way(
            homeProb = homeProb,
            drawProb = drawProb,
            awayProb = awayProb,
            weight = config.eloWeight,
            name = "Elo"
        )
    }
}
