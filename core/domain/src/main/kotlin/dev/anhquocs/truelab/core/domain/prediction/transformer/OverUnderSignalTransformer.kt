package dev.anhquocs.truelab.core.domain.prediction.transformer

import dev.anhquocs.truelab.core.algorithm.prediction.Signal3Way
import dev.anhquocs.truelab.core.domain.odds.model.OddsRecordItem
import dev.anhquocs.truelab.core.domain.prediction.model.CandidateCConfig
import dev.anhquocs.truelab.core.domain.prediction.model.PredictionWeightConfig

/**
 * Chuyển đổi dữ liệu Kèo Tài/Xỉu Tổng số bàn thắng (Over/Under - bs) thành [Signal3Way] (Candidate C2).
 *
 * Chuẩn hóa giá:
 * Decimal_over = HK_over + 1.0, Decimal_under = HK_under + 1.0
 * P_fair(Over) = (1/Decimal_over) / (1/Decimal_over + 1/Decimal_under)
 *
 * Ánh xạ tổng bàn thắng kỳ vọng và điều biến hòa:
 * λ_total = handicap + η * (P_fair(Over) - 0.5)
 * P(D) = clamp(P0_D + κ * (2.50 - λ_total), minDrawProb, maxDrawProb)
 * P(H) = (1.0 - P(D)) * 0.5
 * P(A) = (1.0 - P(D)) * 0.5
 */
object OverUnderSignalTransformer {

    fun transform(
        ouItem: OddsRecordItem?,
        config: PredictionWeightConfig = PredictionWeightConfig.DEFAULT,
        candidateCConfig: CandidateCConfig = CandidateCConfig.DEFAULT_CALIBRATED
    ): Signal3Way {
        if (ouItem == null || !candidateCConfig.isEnabled || candidateCConfig.ouWeight <= 0.0) {
            return fallbackSignal(config.baselineDrawProb)
        }

        val gLine = ouItem.handicap ?: return fallbackSignal(config.baselineDrawProb)
        val hkOver = ouItem.over ?: return fallbackSignal(config.baselineDrawProb)
        val hkUnder = ouItem.under ?: return fallbackSignal(config.baselineDrawProb)

        if (hkOver <= 0.0 || hkUnder <= 0.0 || gLine <= 0.0 || !hkOver.isFinite() || !hkUnder.isFinite() || !gLine.isFinite()) {
            return fallbackSignal(config.baselineDrawProb)
        }

        val decOver = hkOver + 1.0
        val decUnder = hkUnder + 1.0
        val pRawOver = 1.0 / decOver
        val pRawUnder = 1.0 / decUnder
        val sumRaw = pRawOver + pRawUnder
        if (sumRaw <= 0.0 || !sumRaw.isFinite()) {
            return fallbackSignal(config.baselineDrawProb)
        }

        val pFairOver = pRawOver / sumRaw
        val p0Draw = config.baselineDrawProb

        val lambdaTotal = gLine + candidateCConfig.eta * (pFairOver - 0.5)
        val rawDrawMod = p0Draw + candidateCConfig.kappa * (2.50 - lambdaTotal)
        val pD = rawDrawMod.coerceIn(candidateCConfig.minDrawProb, candidateCConfig.maxDrawProb)
        val pH = (1.0 - pD) * 0.5
        val pA = (1.0 - pD) * 0.5

        return if (pH.isFinite() && pD.isFinite() && pA.isFinite() && pH in 0.0..1.0 && pD in 0.0..1.0 && pA in 0.0..1.0) {
            Signal3Way(
                homeProb = pH,
                drawProb = pD,
                awayProb = pA,
                weight = candidateCConfig.ouWeight,
                name = "Over/Under"
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
            name = "Over/Under (Unavailable)"
        )
    }
}
