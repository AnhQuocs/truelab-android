package dev.anhquocs.truelab.core.domain.prediction.transformer

import dev.anhquocs.truelab.core.algorithm.prediction.Signal3Way
import dev.anhquocs.truelab.core.domain.prediction.model.PredictionWeightConfig

/**
 * Chuyển đổi tín hiệu ưu thế sân nhà thành [Signal3Way] độc lập theo đặc tả FR-14.
 */
object HomeAdvantageSignalTransformer {

    fun transform(
        isNeutralVenue: Boolean = false,
        config: PredictionWeightConfig = PredictionWeightConfig.DEFAULT
    ): Signal3Way {
        return if (isNeutralVenue) {
            val drawProb = config.baselineDrawProb
            val splitProb = (1.0 - drawProb) / 2.0
            Signal3Way(
                homeProb = splitProb,
                drawProb = drawProb,
                awayProb = splitProb,
                weight = config.homeAdvantageWeight,
                name = "Neutral Venue"
            )
        } else {
            Signal3Way(
                homeProb = config.homeAdvantageProbHome,
                drawProb = config.homeAdvantageProbDraw,
                awayProb = config.homeAdvantageProbAway,
                weight = config.homeAdvantageWeight,
                name = "Home Advantage"
            )
        }
    }
}
