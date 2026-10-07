package dev.anhquocs.truelab.core.domain.prediction.transformer

import dev.anhquocs.truelab.core.algorithm.prediction.Signal3Way
import dev.anhquocs.truelab.core.domain.odds.model.OddsRecordItem
import dev.anhquocs.truelab.core.domain.prediction.model.CandidateCConfig
import dev.anhquocs.truelab.core.domain.prediction.model.PredictionWeightConfig
import kotlin.math.exp

/**
 * Chuyển đổi dữ liệu Kèo Chấp Châu Á (Asian Handicap - asia) thành [Signal3Way] (Candidate C1).
 *
 * Chuẩn hóa giá:
 * Decimal_home = HK_home + 1.0, Decimal_away = HK_away + 1.0
 * P_fair(Home) = (1/Decimal_home) / (1/Decimal_home + 1/Decimal_away)
 *
 * Ánh xạ độ lệch ưu thế:
 * Δ_AH = handicap + α * (P_fair(Home) - 0.5)
 * P_raw(H) = (1 - P0_D) / (1 + exp(-β * Δ_AH))
 * P_raw(A) = (1 - P0_D) - P_raw(H)
 * P_raw(D) = P0_D * exp(-γ * Δ_AH^2)
 */
object AsianHandicapSignalTransformer {

    fun transform(
        ahItem: OddsRecordItem?,
        config: PredictionWeightConfig = PredictionWeightConfig.DEFAULT,
        candidateCConfig: CandidateCConfig = CandidateCConfig.DEFAULT_CALIBRATED
    ): Signal3Way {
        if (ahItem == null || !candidateCConfig.isEnabled || candidateCConfig.ahWeight <= 0.0) {
            return fallbackSignal(config.baselineDrawProb)
        }

        val hLine = ahItem.handicap ?: return fallbackSignal(config.baselineDrawProb)
        val hkHome = ahItem.homeWin ?: return fallbackSignal(config.baselineDrawProb)
        val hkAway = ahItem.awayWin ?: return fallbackSignal(config.baselineDrawProb)

        if (hkHome <= 0.0 || hkAway <= 0.0 || !hkHome.isFinite() || !hkAway.isFinite() || !hLine.isFinite()) {
            return fallbackSignal(config.baselineDrawProb)
        }

        val decHome = hkHome + 1.0
        val decAway = hkAway + 1.0
        val pRawHome = 1.0 / decHome
        val pRawAway = 1.0 / decAway
        val sumRaw = pRawHome + pRawAway
        if (sumRaw <= 0.0 || !sumRaw.isFinite()) {
            return fallbackSignal(config.baselineDrawProb)
        }

        val pFairHome = pRawHome / sumRaw
        val p0Draw = config.baselineDrawProb

        val deltaAh = hLine + candidateCConfig.alpha * (pFairHome - 0.5)
        val rawH = (1.0 - p0Draw) / (1.0 + exp(-candidateCConfig.beta * deltaAh))
        val rawA = (1.0 - p0Draw) - rawH
        val rawD = p0Draw * exp(-candidateCConfig.gamma * deltaAh * deltaAh)

        val total = rawH + rawD + rawA
        if (total <= 0.0 || !total.isFinite()) {
            return fallbackSignal(p0Draw)
        }

        val pH = rawH / total
        val pD = rawD / total
        val pA = rawA / total

        return if (pH.isFinite() && pD.isFinite() && pA.isFinite() && pH in 0.0..1.0 && pD in 0.0..1.0 && pA in 0.0..1.0) {
            Signal3Way(
                homeProb = pH,
                drawProb = pD,
                awayProb = pA,
                weight = candidateCConfig.ahWeight,
                name = "Asian Handicap"
            )
        } else {
            fallbackSignal(p0Draw)
        }
    }

    private fun fallbackSignal(baselineDrawProb: Double): Signal3Way {
        val split = (1.0 - baselineDrawProb) / 2.0
        return Signal3Way(
            homeProb = split,
            drawProb = baselineDrawProb,
            awayProb = split,
            weight = 0.0,
            name = "Asian Handicap (Unavailable)"
        )
    }
}
