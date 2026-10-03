package dev.anhquocs.truelab.core.domain.evaluation.usecase

import dev.anhquocs.truelab.core.algorithm.rating.EloRatingCalculator
import dev.anhquocs.truelab.core.algorithm.rating.RatingCalculator
import dev.anhquocs.truelab.core.domain.evaluation.model.IndependentTestEvaluationResult
import dev.anhquocs.truelab.core.domain.evaluation.model.TemporalDatasetSplitter
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.odds.model.OddsRecordItem
import dev.anhquocs.truelab.core.domain.prediction.model.DrawMarginConfig
import dev.anhquocs.truelab.core.domain.prediction.model.DrawModelingStrategy
import dev.anhquocs.truelab.core.domain.prediction.model.DrawStrategyConfig
import dev.anhquocs.truelab.core.domain.prediction.model.DynamicDrawPriorConfig
import dev.anhquocs.truelab.core.domain.prediction.model.MatchPredictionContext
import dev.anhquocs.truelab.core.domain.prediction.usecase.PredictMatchOutcomeUseCase

/**
 * UseCase thuần túy (Pure Kotlin/JVM) thực hiện đánh giá đối chứng toàn diện 3 chiều
 * (Baseline vs Frozen Candidate A vs Frozen Candidate B) trên tập dữ liệu kiểm thử độc lập (Independent Test Set).
 *
 * Nguyên tắc bất biến:
 * 1. Các tham số của Candidate A và Candidate B được đóng băng (Frozen) hoàn toàn từ pha Calibration.
 * 2. Cùng đánh giá trên một tập bối cảnh trận đấu [testContexts] duy nhất theo thứ tự thời gian.
 * 3. Tuyệt đối không rò rỉ thông tin tương lai, không tối ưu tham số trên tập Test này.
 * 4. Tính toán đầy đủ Multi-class Metrics, Confusion Matrix và Multi-class Brier Score cho cả 3 mô hình.
 */
class EvaluateDrawModelsOnIndependentTestUseCase(
    private val predictMatchOutcomeUseCase: PredictMatchOutcomeUseCase = PredictMatchOutcomeUseCase(),
    private val calculateEvaluationMetricsUseCase: CalculateEvaluationMetricsUseCase = CalculateEvaluationMetricsUseCase(),
    private val eloRatingCalculator: RatingCalculator = EloRatingCalculator()
) {

    operator fun invoke(
        testContexts: List<Pair<MatchPredictionContext, String>>,
        frozenCandidateAConfig: DrawMarginConfig,
        frozenCandidateBConfig: DynamicDrawPriorConfig
    ): IndependentTestEvaluationResult {
        val totalSamples = testContexts.size

        // 1. Chạy mô hình Baseline (Control Group)
        val baselineStrategy = DrawStrategyConfig(strategy = DrawModelingStrategy.BASELINE)
        val baselinePredictions = ArrayList<Pair<String, String>>(totalSamples)
        val baselineProbSamples = ArrayList<CalculateEvaluationMetricsUseCase.ProbabilisticEvaluationSample>(totalSamples)

        for ((ctx, actual) in testContexts) {
            val pred = predictMatchOutcomeUseCase(context = ctx, drawStrategyConfig = baselineStrategy)
            baselinePredictions.add(Pair(pred.predictedOutcome, actual))
            baselineProbSamples.add(
                CalculateEvaluationMetricsUseCase.ProbabilisticEvaluationSample(
                    homeProb = pred.homeWinProb,
                    drawProb = pred.drawProb,
                    awayProb = pred.awayWinProb,
                    actualOutcome = actual
                )
            )
        }
        val baselineResult = calculateEvaluationMetricsUseCase(baselinePredictions)
        val baselineBrierScore = calculateEvaluationMetricsUseCase.calculateBrierScore(baselineProbSamples)

        // 2. Chạy mô hình Candidate A (Decision Margin) với cấu hình đã đóng băng
        val candidateAStrategy = DrawStrategyConfig(
            strategy = DrawModelingStrategy.DECISION_MARGIN,
            marginConfig = frozenCandidateAConfig
        )
        val candidateAPredictions = ArrayList<Pair<String, String>>(totalSamples)
        val candidateAProbSamples = ArrayList<CalculateEvaluationMetricsUseCase.ProbabilisticEvaluationSample>(totalSamples)

        for ((ctx, actual) in testContexts) {
            val pred = predictMatchOutcomeUseCase(context = ctx, drawStrategyConfig = candidateAStrategy)
            candidateAPredictions.add(Pair(pred.predictedOutcome, actual))
            candidateAProbSamples.add(
                CalculateEvaluationMetricsUseCase.ProbabilisticEvaluationSample(
                    homeProb = pred.homeWinProb,
                    drawProb = pred.drawProb,
                    awayProb = pred.awayWinProb,
                    actualOutcome = actual
                )
            )
        }
        val candidateAResult = calculateEvaluationMetricsUseCase(candidateAPredictions)
        val candidateABrierScore = calculateEvaluationMetricsUseCase.calculateBrierScore(candidateAProbSamples)

        // 3. Chạy mô hình Candidate B (Dynamic Draw Prior) với cấu hình đã đóng băng
        val candidateBStrategy = DrawStrategyConfig(
            strategy = DrawModelingStrategy.DYNAMIC_DRAW_PRIOR,
            dynamicPriorConfig = frozenCandidateBConfig
        )
        val candidateBPredictions = ArrayList<Pair<String, String>>(totalSamples)
        val candidateBProbSamples = ArrayList<CalculateEvaluationMetricsUseCase.ProbabilisticEvaluationSample>(totalSamples)

        for ((ctx, actual) in testContexts) {
            val pred = predictMatchOutcomeUseCase(context = ctx, drawStrategyConfig = candidateBStrategy)
            candidateBPredictions.add(Pair(pred.predictedOutcome, actual))
            candidateBProbSamples.add(
                CalculateEvaluationMetricsUseCase.ProbabilisticEvaluationSample(
                    homeProb = pred.homeWinProb,
                    drawProb = pred.drawProb,
                    awayProb = pred.awayWinProb,
                    actualOutcome = actual
                )
            )
        }
        val candidateBResult = calculateEvaluationMetricsUseCase(candidateBPredictions)
        val candidateBBrierScore = calculateEvaluationMetricsUseCase.calculateBrierScore(candidateBProbSamples)

        return IndependentTestEvaluationResult(
            totalSamples = totalSamples,
            baselineResult = baselineResult,
            baselineBrierScore = baselineBrierScore,
            candidateAResult = candidateAResult,
            candidateABrierScore = candidateABrierScore,
            candidateAFrozenConfig = frozenCandidateAConfig,
            candidateBResult = candidateBResult,
            candidateBBrierScore = candidateBBrierScore,
            candidateBFrozenConfig = frozenCandidateBConfig
        )
    }

    /**
     * Tự động xây dựng bối cảnh thời gian chống rò rỉ và đánh giá trên tập Independent Test (30% sau cùng).
     */
    operator fun invoke(
        matches: List<Match>,
        matchOddsMap: Map<Long, OddsRecordItem> = emptyMap(),
        teamEloMap: Map<Int, Double> = emptyMap(),
        validationRatio: Double = 0.70,
        frozenCandidateAConfig: DrawMarginConfig,
        frozenCandidateBConfig: DynamicDrawPriorConfig
    ): IndependentTestEvaluationResult {
        val sortedMatches = matches
            .filter { it.startTimeDate.isNotBlank() && it.isEnded && it.homeScore != null && it.awayScore != null }
            .sortedWith(compareBy({ it.startTimeDate }, { it.id }))

        val allContextsWithActual = buildChronologicalContexts(sortedMatches, matchOddsMap, teamEloMap)
        val split = TemporalDatasetSplitter.splitChronological(allContextsWithActual, validationRatio = validationRatio)

        return invoke(
            testContexts = split.independentTest,
            frozenCandidateAConfig = frozenCandidateAConfig,
            frozenCandidateBConfig = frozenCandidateBConfig
        )
    }

    fun buildChronologicalContexts(
        matches: List<Match>,
        matchOddsMap: Map<Long, OddsRecordItem>,
        teamEloMap: Map<Int, Double>
    ): List<Pair<MatchPredictionContext, String>> {
        val n = matches.size
        val result = ArrayList<Pair<MatchPredictionContext, String>>(n)
        val teamHistory = HashMap<Int, MutableList<Match>>()
        val h2hHistory = HashMap<Long, MutableList<Match>>()
        val currentEloMap = HashMap<Int, Double>(teamEloMap)

        var i = 0
        while (i < n) {
            val currentTimestamp = matches[i].startTimeDate
            var j = i
            while (j < n && matches[j].startTimeDate == currentTimestamp) {
                j++
            }

            for (k in i until j) {
                val target = matches[k]
                val homeScore = target.homeScore ?: continue
                val awayScore = target.awayScore ?: continue
                val homeTeamId = target.homeTeam.id
                val awayTeamId = target.awayTeam.id

                val homeRecent = getRecentMatches(teamHistory[homeTeamId], 5)
                val awayRecent = getRecentMatches(teamHistory[awayTeamId], 5)
                val h2h = getH2HMatches(h2hHistory[packTeamPair(homeTeamId, awayTeamId)])
                val homeElo = currentEloMap[homeTeamId] ?: 1500.0
                val awayElo = currentEloMap[awayTeamId] ?: 1500.0

                val context = MatchPredictionContext(
                    matchId = target.id,
                    homeTeamId = homeTeamId,
                    awayTeamId = awayTeamId,
                    matchStartTimeDate = target.startTimeDate,
                    homeElo = homeElo,
                    awayElo = awayElo,
                    homeRecentMatches = homeRecent,
                    awayRecentMatches = awayRecent,
                    h2hMatches = h2h,
                    latestOdds = matchOddsMap[target.id]
                )

                val actualOutcome = when {
                    homeScore > awayScore -> CalculateEvaluationMetricsUseCase.LABEL_HOME_WIN
                    homeScore == awayScore -> CalculateEvaluationMetricsUseCase.LABEL_DRAW
                    else -> CalculateEvaluationMetricsUseCase.LABEL_AWAY_WIN
                }

                result.add(Pair(context, actualOutcome))
            }

            for (k in i until j) {
                val target = matches[k]
                val homeScore = target.homeScore ?: continue
                val awayScore = target.awayScore ?: continue
                val homeTeamId = target.homeTeam.id
                val awayTeamId = target.awayTeam.id

                val homeElo = currentEloMap[homeTeamId] ?: 1500.0
                val awayElo = currentEloMap[awayTeamId] ?: 1500.0
                val actualScoreHome = when {
                    homeScore > awayScore -> 1.0
                    homeScore == awayScore -> 0.5
                    else -> 0.0
                }

                val matchRating = eloRatingCalculator.calculateMatch(homeElo, awayElo, actualScoreHome, kFactor = 32.0)
                currentEloMap[homeTeamId] = matchRating.newRatingA
                currentEloMap[awayTeamId] = matchRating.newRatingB

                teamHistory.getOrPut(homeTeamId) { ArrayList() }.add(target)
                teamHistory.getOrPut(awayTeamId) { ArrayList() }.add(target)
                h2hHistory.getOrPut(packTeamPair(homeTeamId, awayTeamId)) { ArrayList() }.add(target)
            }

            i = j
        }
        return result
    }

    private fun packTeamPair(idA: Int, idB: Int): Long {
        val minId = if (idA < idB) idA else idB
        val maxId = if (idA < idB) idB else idA
        return (minId.toLong() shl 32) or (maxId.toLong() and 0xFFFFFFFFL)
    }

    private fun getRecentMatches(history: List<Match>?, count: Int): List<Match> {
        if (history.isNullOrEmpty()) return emptyList()
        val size = history.size
        val start = if (size > count) size - count else 0
        val result = ArrayList<Match>(size - start)
        for (idx in size - 1 downTo start) {
            result.add(history[idx])
        }
        return result
    }

    private fun getH2HMatches(history: List<Match>?): List<Match> {
        if (history.isNullOrEmpty()) return emptyList()
        val size = history.size
        val result = ArrayList<Match>(size)
        for (idx in size - 1 downTo 0) {
            result.add(history[idx])
        }
        return result
    }
}
