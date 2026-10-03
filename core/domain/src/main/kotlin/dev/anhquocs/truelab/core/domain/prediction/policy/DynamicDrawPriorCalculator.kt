package dev.anhquocs.truelab.core.domain.prediction.policy

import kotlin.math.exp

/**
 * Bộ tính toán tiên nghiệm hòa động (Candidate B: Dynamic Draw Prior Calculator).
 *
 * Áp dụng hàm mật độ Gaussian đối xứng theo độ lệch sức mạnh chuẩn hóa [delta]:
 * P_D(delta) = P_D,min + (P_D,max - P_D,min) * exp(-delta^2 / (2 * sigma^2))
 */
object DynamicDrawPriorCalculator {

    /**
     * Tính toán xác suất hòa tiên nghiệm động P_D(delta).
     *
     * @param delta Độ lệch sức mạnh chuẩn hóa giữa Home và Away (delta = 0 khi 2 đội cân bằng).
     * @param sigma Tham số độ rộng hàm Gaussian (eloSigma hoặc formSigma).
     * @param maxDrawProb Xác suất hòa cực đại tại delta = 0 (mặc định 0.36).
     * @param minDrawProb Xác suất hòa cực tiểu khi |delta| lớn (mặc định 0.12).
     * @return Xác suất hòa động P_D(delta) nằm trong đoạn [minDrawProb, maxDrawProb].
     */
    fun computePrior(
        delta: Double,
        sigma: Double,
        maxDrawProb: Double = 0.36,
        minDrawProb: Double = 0.12
    ): Double {
        if (!delta.isFinite() || !sigma.isFinite() || sigma <= 0.0) {
            return maxDrawProb.coerceIn(0.0, 1.0)
        }
        val minP = minDrawProb.coerceIn(0.0, 1.0)
        val maxP = maxDrawProb.coerceIn(minP, 1.0)

        val exponent = -(delta * delta) / (2.0 * sigma * sigma)
        val factor = exp(exponent)
        val prior = minP + (maxP - minP) * factor

        return prior.coerceIn(minP, maxP)
    }
}
