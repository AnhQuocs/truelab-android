package dev.anhquocs.truelab.core.domain.prediction.transformer

import dev.anhquocs.truelab.core.algorithm.prediction.Signal3Way
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.odds.selector.PreMatchOddsSelector
import dev.anhquocs.truelab.core.domain.prediction.model.PredictionWeightConfig
import kotlin.math.tanh

/**
 * Chuyển đổi mức độ chênh lệch thời gian nghỉ ngơi giữa hai đội thành [Signal3Way]
 * theo công thức Hyperbolic Tangent chuẩn hóa (Phase R2/R4).
 */
object RestAdvantageSignalTransformer {

    /**
     * Chuyển đổi từ dữ liệu trận đấu trước đó và thời điểm kickoff của trận mục tiêu.
     */
    fun transform(
        homePreviousMatch: Match?,
        awayPreviousMatch: Match?,
        targetKickoffTime: String?,
        config: PredictionWeightConfig = PredictionWeightConfig.DEFAULT
    ): Signal3Way {
        val drawProb = config.baselineDrawProb
        val neutralProb = (1.0 - drawProb) / 2.0
        val weight = config.restAdvantageWeight

        if (homePreviousMatch == null || awayPreviousMatch == null || targetKickoffTime.isNullOrBlank()) {
            return createNeutralSignal(neutralProb, drawProb, weight)
        }

        val targetEpoch = PreMatchOddsSelector.parseKickoffEpochSeconds(targetKickoffTime)
        val homePrevEpoch = PreMatchOddsSelector.parseKickoffEpochSeconds(homePreviousMatch.startTimeDate)
        val awayPrevEpoch = PreMatchOddsSelector.parseKickoffEpochSeconds(awayPreviousMatch.startTimeDate)

        if (targetEpoch == null || homePrevEpoch == null || awayPrevEpoch == null) {
            return createNeutralSignal(neutralProb, drawProb, weight)
        }

        // Strict temporal safety: Previous match must strictly precede target kickoff
        if (homePrevEpoch >= targetEpoch || awayPrevEpoch >= targetEpoch) {
            return createNeutralSignal(neutralProb, drawProb, weight)
        }

        val homeRestDays = (targetEpoch - homePrevEpoch) / 86400.0
        val awayRestDays = (targetEpoch - awayPrevEpoch) / 86400.0
        val deltaRest = homeRestDays - awayRestDays

        return transform(deltaRest, config)
    }

    /**
     * Chuyển đổi trực tiếp từ giá trị deltaRest (đơn vị ngày).
     */
    fun transform(
        deltaRest: Double,
        config: PredictionWeightConfig = PredictionWeightConfig.DEFAULT
    ): Signal3Way {
        val drawProb = config.baselineDrawProb
        val neutralProb = (1.0 - drawProb) / 2.0
        val weight = config.restAdvantageWeight

        if (!deltaRest.isFinite()) {
            return createNeutralSignal(neutralProb, drawProb, weight)
        }

        val sensitivity = config.restAdvantageSensitivity
        val maxShift = config.restAdvantageMaxShift

        val bias = tanh(deltaRest / sensitivity)
        val shift = maxShift * bias

        val homeProb = neutralProb + shift
        val awayProb = neutralProb - shift

        return Signal3Way(
            homeProb = homeProb,
            drawProb = drawProb,
            awayProb = awayProb,
            weight = weight,
            name = "Rest Advantage"
        )
    }

    private fun createNeutralSignal(
        neutralProb: Double,
        drawProb: Double,
        weight: Double
    ): Signal3Way {
        return Signal3Way(
            homeProb = neutralProb,
            drawProb = drawProb,
            awayProb = neutralProb,
            weight = weight,
            name = "Rest Advantage"
        )
    }
}
