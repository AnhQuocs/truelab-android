package dev.anhquocs.truelab.core.domain.prediction.transformer

import dev.anhquocs.truelab.core.algorithm.prediction.Signal3Way
import dev.anhquocs.truelab.core.domain.odds.model.OddsRecordItem
import dev.anhquocs.truelab.core.domain.prediction.model.PredictionWeightConfig

/**
 * Chuyển đổi dữ liệu tỷ lệ cược nhà cái thành [Signal3Way].
 */
object OddsSignalTransformer {

    fun transform(
        oddsItem: OddsRecordItem?,
        config: PredictionWeightConfig = PredictionWeightConfig.DEFAULT
    ): Signal3Way {
        val implied = oddsItem?.calculateImpliedProbability()
        return if (implied != null &&
            implied.homeProb.isFinite() && implied.drawProb.isFinite() && implied.awayProb.isFinite() &&
            implied.homeProb in 0.0..1.0 && implied.drawProb in 0.0..1.0 && implied.awayProb in 0.0..1.0
        ) {
            Signal3Way(
                homeProb = implied.homeProb,
                drawProb = implied.drawProb,
                awayProb = implied.awayProb,
                weight = config.oddsWeight,
                name = "Odds"
            )
        } else {
            // Missing / invalid odds fallback: gán weight = 0.0 với xác suất trung tính
            val drawProb = config.baselineDrawProb
            val splitProb = (1.0 - drawProb) / 2.0
            Signal3Way(
                homeProb = splitProb,
                drawProb = drawProb,
                awayProb = splitProb,
                weight = 0.0,
                name = "Odds (Unavailable)"
            )
        }
    }
}
