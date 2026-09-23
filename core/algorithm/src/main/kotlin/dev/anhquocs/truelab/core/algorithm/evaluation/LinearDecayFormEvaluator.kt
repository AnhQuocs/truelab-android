package dev.anhquocs.truelab.core.algorithm.evaluation

/**
 * Triển khai thuật toán đánh giá phong độ thi đấu áp dụng mô hình trọng số thời gian tuyến tính
 * (Linear Time-Decay Weighted Form).
 *
 * - Độ phức tạp thời gian: O(k) với k là kích thước cửa sổ (mặc định 5).
 * - Độ phức tạp không gian phụ trợ (Auxiliary Space): O(1) tuyệt đối.
 * - Duyệt trực tiếp danh sách từ `startIndex = maxOf(0, n - windowSize)` mà không tạo slice, sublist
 *   hay intermediate collection.
 * - Hỗ trợ Generic Selector xử lý on-the-fly, không phân bổ bộ nhớ O(n) trung gian.
 */
class LinearDecayFormEvaluator : FormEvaluator {

    override fun evaluate(outcomes: List<MatchOutcome>, windowSize: Int): FormScore =
        evaluateInternal(outcomes, windowSize) { it }

    override fun <T> evaluate(
        dataset: List<T>,
        windowSize: Int,
        selector: (T) -> MatchOutcome
    ): FormScore = evaluateInternal(dataset, windowSize, selector)

    private inline fun <T> evaluateInternal(
        dataset: List<T>,
        windowSize: Int,
        selector: (T) -> MatchOutcome
    ): FormScore {
        require(windowSize > 0) {
            "windowSize must be greater than 0, but was $windowSize"
        }

        if (dataset.isEmpty()) {
            return FormScore(
                score = 0.0,
                rawScore = 0.0,
                matchesCount = 0,
                wins = 0,
                draws = 0,
                losses = 0,
                totalPoints = 0.0,
                maxPoints = 0.0
            )
        }

        val n = dataset.size
        val startIndex = maxOf(0, n - windowSize)
        val matchesCount = n - startIndex

        var wins = 0
        var draws = 0
        var losses = 0
        var totalPoints = 0.0
        var weightedPointsSum = 0.0
        var totalWeights = 0.0

        for (i in startIndex until n) {
            val outcome = selector(dataset[i])
            val points = outcome.points

            when (outcome) {
                MatchOutcome.WIN -> wins++
                MatchOutcome.DRAW -> draws++
                MatchOutcome.LOSS -> losses++
            }

            totalPoints += points
            val weight = (i - startIndex + 1).toDouble()
            weightedPointsSum += weight * points
            totalWeights += weight
        }

        val maxPoints = matchesCount * 3.0
        val rawScore = (totalPoints / maxPoints) * 100.0
        val score = (weightedPointsSum / (3.0 * totalWeights)) * 100.0

        return FormScore(
            score = score,
            rawScore = rawScore,
            matchesCount = matchesCount,
            wins = wins,
            draws = draws,
            losses = losses,
            totalPoints = totalPoints,
            maxPoints = maxPoints
        )
    }
}
