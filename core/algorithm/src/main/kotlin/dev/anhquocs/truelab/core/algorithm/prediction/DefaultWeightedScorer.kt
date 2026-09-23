package dev.anhquocs.truelab.core.algorithm.prediction

import kotlin.math.abs

/**
 * Triển khai thuật toán tính điểm và dự đoán theo mô hình trọng số (Weighted Scoring).
 *
 * Đặc điểm thuật toán:
 * - Thuần túy Kotlin/JVM, độc lập hoàn toàn với Android SDK, Room DB và Domain Entities.
 * - Độ phức tạp thời gian: O(m) cho [calculateScore] và [predictOutcome], O(1) cho [predict3Way].
 * - Độ phức tạp không gian phụ trợ (Auxiliary Space): O(1) tuyệt đối.
 * - Quy tắc Tie-breaking đối xứng, tất định, không phụ thuộc vào thứ tự khai báo enum.
 */
class DefaultWeightedScorer : WeightedScorer {

    override fun calculateScore(features: List<WeightedFeature>): Double {
        require(features.isNotEmpty()) { "Features list must not be empty" }

        var weightedSum = 0.0
        var totalWeight = 0.0

        for (feature in features) {
            val score = feature.score
            val weight = feature.weight

            require(score.isFinite()) { "Feature score must be finite, but was $score" }
            require(score in 0.0..1.0) { "Feature score must be in range [0.0, 1.0], but was $score" }
            require(weight.isFinite()) { "Feature weight must be finite, but was $weight" }
            require(weight >= 0.0) { "Feature weight must be non-negative, but was $weight" }

            weightedSum += weight * score
            totalWeight += weight
        }

        require(totalWeight > 0.0) { "Total weight must be positive, but was $totalWeight" }

        return weightedSum / totalWeight
    }

    override fun <T> calculateScore(
        dataset: List<T>,
        scoreSelector: (T) -> Double,
        weightSelector: (T) -> Double
    ): Double {
        require(dataset.isNotEmpty()) { "Dataset must not be empty" }

        var weightedSum = 0.0
        var totalWeight = 0.0

        for (item in dataset) {
            val score = scoreSelector(item)
            val weight = weightSelector(item)

            require(score.isFinite()) { "Feature score must be finite, but was $score" }
            require(score in 0.0..1.0) { "Feature score must be in range [0.0, 1.0], but was $score" }
            require(weight.isFinite()) { "Feature weight must be finite, but was $weight" }
            require(weight >= 0.0) { "Feature weight must be non-negative, but was $weight" }

            weightedSum += weight * score
            totalWeight += weight
        }

        require(totalWeight > 0.0) { "Total weight must be positive, but was $totalWeight" }

        return weightedSum / totalWeight
    }

    override fun predict3Way(
        homeScore: Double,
        drawScore: Double,
        awayScore: Double
    ): OutcomeProbabilities {
        require(homeScore.isFinite()) { "homeScore must be finite, but was $homeScore" }
        require(drawScore.isFinite()) { "drawScore must be finite, but was $drawScore" }
        require(awayScore.isFinite()) { "awayScore must be finite, but was $awayScore" }

        require(homeScore >= 0.0) { "homeScore must be non-negative, but was $homeScore" }
        require(drawScore >= 0.0) { "drawScore must be non-negative, but was $drawScore" }
        require(awayScore >= 0.0) { "awayScore must be non-negative, but was $awayScore" }

        val total = homeScore + drawScore + awayScore
        require(total > 0.0) { "Total 3-way score must be positive, but was $total" }

        val homeProb = homeScore / total
        val drawProb = drawScore / total
        val awayProb = awayScore / total

        return resolveOutcomeProbabilities(homeProb, drawProb, awayProb)
    }

    override fun predictOutcome(signals: List<Signal3Way>): OutcomeProbabilities {
        require(signals.isNotEmpty()) { "Signals list must not be empty" }

        var weightedHome = 0.0
        var weightedDraw = 0.0
        var weightedAway = 0.0
        var totalWeight = 0.0

        for (signal in signals) {
            val hp = signal.homeProb
            val dp = signal.drawProb
            val ap = signal.awayProb
            val w = signal.weight

            require(hp.isFinite()) { "homeProb must be finite, but was $hp" }
            require(dp.isFinite()) { "drawProb must be finite, but was $dp" }
            require(ap.isFinite()) { "awayProb must be finite, but was $ap" }

            require(hp in 0.0..1.0) { "homeProb must be in range [0.0, 1.0], but was $hp" }
            require(dp in 0.0..1.0) { "drawProb must be in range [0.0, 1.0], but was $dp" }
            require(ap in 0.0..1.0) { "awayProb must be in range [0.0, 1.0], but was $ap" }

            val probSum = hp + dp + ap
            require(abs(probSum - 1.0) <= PROBABILITY_SUM_TOLERANCE) {
                "Probabilities of signal '${signal.name}' must sum to 1.0 (±$PROBABILITY_SUM_TOLERANCE), but was $probSum"
            }

            require(w.isFinite()) { "Signal weight must be finite, but was $w" }
            require(w >= 0.0) { "Signal weight must be non-negative, but was $w" }

            weightedHome += w * hp
            weightedDraw += w * dp
            weightedAway += w * ap
            totalWeight += w
        }

        require(totalWeight > 0.0) { "Total weight must be positive, but was $totalWeight" }

        val homeProb = weightedHome / totalWeight
        val drawProb = weightedDraw / totalWeight
        val awayProb = weightedAway / totalWeight

        return resolveOutcomeProbabilities(homeProb, drawProb, awayProb)
    }

    /**
     * Xác định kết quả dự đoán và độ tin cậy theo quy tắc phân định đối xứng (Tie-Breaking Rule):
     *
     * - Nếu chỉ có đúng 1 outcome đạt xác suất lớn nhất (maxProb) -> chọn outcome đó.
     * - Nếu từ 2 outcome trở lên cùng đạt xác suất lớn nhất -> chọn [PredictedOutcome.DRAW].
     *
     * Bao phủ toàn bộ các trường hợp hòa điểm:
     * 1. HOME == AWAY > DRAW -> DRAW
     * 2. HOME == DRAW > AWAY -> DRAW
     * 3. DRAW == AWAY > HOME -> DRAW
     * 4. HOME == DRAW == AWAY -> DRAW
     */
    private fun resolveOutcomeProbabilities(
        homeProb: Double,
        drawProb: Double,
        awayProb: Double
    ): OutcomeProbabilities {
        val maxProb = maxOf(homeProb, drawProb, awayProb)

        val homeIsMax = homeProb == maxProb
        val drawIsMax = drawProb == maxProb
        val awayIsMax = awayProb == maxProb

        val maxCount = (if (homeIsMax) 1 else 0) +
                       (if (drawIsMax) 1 else 0) +
                       (if (awayIsMax) 1 else 0)

        val outcome = if (maxCount >= 2) {
            PredictedOutcome.DRAW
        } else {
            when {
                homeIsMax -> PredictedOutcome.HOME_WIN
                awayIsMax -> PredictedOutcome.AWAY_WIN
                else -> PredictedOutcome.DRAW
            }
        }

        return OutcomeProbabilities(
            homeWinProb = homeProb,
            drawProb = drawProb,
            awayWinProb = awayProb,
            predictedOutcome = outcome,
            confidenceScore = maxProb
        )
    }

    companion object {
        const val PROBABILITY_SUM_TOLERANCE: Double = 1e-4
    }
}
