package dev.anhquocs.truelab.core.domain.evaluation.usecase

import dev.anhquocs.truelab.core.algorithm.rating.EloRatingCalculator
import dev.anhquocs.truelab.core.algorithm.rating.RatingCalculator
import dev.anhquocs.truelab.core.domain.evaluation.model.CandidateAGridEvaluationRecord
import dev.anhquocs.truelab.core.domain.evaluation.model.CandidateAOptimizationConfig
import dev.anhquocs.truelab.core.domain.evaluation.model.CandidateAOptimizationResult
import dev.anhquocs.truelab.core.domain.evaluation.model.DrawMarginParameterGrid
import dev.anhquocs.truelab.core.domain.evaluation.model.TemporalDatasetSplitter
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.odds.model.OddsRecordItem
import dev.anhquocs.truelab.core.domain.prediction.model.DrawMarginConfig
import dev.anhquocs.truelab.core.domain.prediction.model.DrawModelingStrategy
import dev.anhquocs.truelab.core.domain.prediction.model.DrawStrategyConfig
import dev.anhquocs.truelab.core.domain.prediction.model.MatchPredictionContext
import dev.anhquocs.truelab.core.domain.prediction.usecase.PredictMatchOutcomeUseCase

/**
 * UseCase thuần túy (Pure Kotlin/JVM) thực thi quy trình tối ưu hóa tham số cho Candidate A (Decision Margin)
 * bằng phương pháp quét lưới (Grid Search) trên tập Calibration / Validation.
 *
 * Đảm bảo các nguyên tắc bất biến:
 * 1. Không gian tìm kiếm gồm đúng 42 cấu hình từ [DrawMarginParameterGrid] (δ ∈ [0.02, 0.08], θ ∈ [0.250, 0.275]).
 * 2. Phân tách tập dữ liệu thời gian nghiêm ngặt (Earliest 70% Calibration/Validation, 30% Test), tuyệt đối không tối ưu trên Test Set.
 * 3. Đánh giá đối chứng Baseline trên cùng một tập dữ liệu.
 * 4. Ràng buộc bảo vệ Home/Away: Home F1 và Away F1 không được suy giảm quá [CandidateAOptimizationConfig.f1Tolerance] (0.03).
 * 5. Chọn lọc cấu hình tối ưu theo hàm mục tiêu Macro F1 trên các cấu hình đạt chuẩn (Eligible).
 */
class OptimizeCandidateADrawUseCase(
    private val predictMatchOutcomeUseCase: PredictMatchOutcomeUseCase = PredictMatchOutcomeUseCase(),
    private val calculateEvaluationMetricsUseCase: CalculateEvaluationMetricsUseCase = CalculateEvaluationMetricsUseCase(),
    private val eloRatingCalculator: RatingCalculator = EloRatingCalculator()
) {

    /**
     * Tối ưu hóa trên tập các bối cảnh dự đoán kèm kết quả thực tế đã được chuẩn bị sẵn.
     *
     * @param validationContexts Danh sách các cặp (MatchPredictionContext, actualOutcomeLabel).
     * @param optimizationConfig Cấu hình kiểm soát quá trình Grid Search.
     * @return [CandidateAOptimizationResult] chứa đầy đủ kết quả quét lưới và cấu hình tối ưu.
     */
    fun optimizeOnContexts(
        validationContexts: List<Pair<MatchPredictionContext, String>>,
        optimizationConfig: CandidateAOptimizationConfig = CandidateAOptimizationConfig()
    ): CandidateAOptimizationResult {
        val totalSamples = validationContexts.size
        val grid = DrawMarginParameterGrid.generateCandidateAGrid()

        // 1. Đánh giá Baseline trên tập Calibration / Validation
        val baselinePairs = validationContexts.map { (ctx, actual) ->
            val result = predictMatchOutcomeUseCase(
                context = ctx,
                drawStrategyConfig = DrawStrategyConfig(strategy = DrawModelingStrategy.BASELINE)
            )
            Pair(result.predictedOutcome, actual)
        }
        val baselineMetrics = calculateEvaluationMetricsUseCase(baselinePairs)
        val baselineHomeF1 = baselineMetrics.homeMetrics.f1Score
        val baselineAwayF1 = baselineMetrics.awayMetrics.f1Score
        val baselineMacroF1 = baselineMetrics.macroF1

        if (totalSamples == 0) {
            return CandidateAOptimizationResult(
                baselineResult = baselineMetrics,
                totalConfigurations = grid.size,
                eligibleConfigurationsCount = 0,
                bestConfiguration = null,
                allRecords = emptyList()
            )
        }

        // 2. Thực thi Grid Search trên 42 cấu hình của Candidate A
        val records = ArrayList<CandidateAGridEvaluationRecord>(grid.size)

        for (config in grid) {
            val strategyConfig = DrawStrategyConfig(
                strategy = DrawModelingStrategy.DECISION_MARGIN,
                marginConfig = config
            )

            val candidatePairs = validationContexts.map { (ctx, actual) ->
                val result = predictMatchOutcomeUseCase(
                    context = ctx,
                    drawStrategyConfig = strategyConfig
                )
                Pair(result.predictedOutcome, actual)
            }

            val eval = calculateEvaluationMetricsUseCase(candidatePairs)
            val homeF1 = eval.homeMetrics.f1Score
            val awayF1 = eval.awayMetrics.f1Score
            val macroF1 = eval.macroF1
            val drawF1 = eval.drawMetrics.f1Score

            // Kiểm tra ràng buộc bảo toàn năng lực phân loại Home/Away
            val homeDelta = homeF1 - baselineHomeF1
            val awayDelta = awayF1 - baselineAwayF1
            val macroDelta = macroF1 - baselineMacroF1

            val isHomeEligible = homeF1 >= (baselineHomeF1 - optimizationConfig.f1Tolerance)
            val isAwayEligible = awayF1 >= (baselineAwayF1 - optimizationConfig.f1Tolerance)
            val eligible = isHomeEligible && isAwayEligible

            records.add(
                CandidateAGridEvaluationRecord(
                    config = config,
                    sampleCount = totalSamples,
                    accuracy = eval.accuracy,
                    macroPrecision = eval.macroPrecision,
                    macroRecall = eval.macroRecall,
                    macroF1 = macroF1,
                    homeF1 = homeF1,
                    drawF1 = drawF1,
                    awayF1 = awayF1,
                    drawPrecision = eval.drawMetrics.precision,
                    drawRecall = eval.drawMetrics.recall,
                    eligible = eligible,
                    homeF1Delta = homeDelta,
                    awayF1Delta = awayDelta,
                    macroF1Delta = macroDelta,
                    confusionMatrix = eval.confusionMatrix
                )
            )
        }

        // 3. Sắp xếp kết quả theo thứ tự ưu tiên tất định
        val sortedRecords = records.sortedWith(
            compareByDescending<CandidateAGridEvaluationRecord> { it.eligible }
                .thenByDescending { it.macroF1 }
                .thenByDescending { it.drawF1 }
                .thenByDescending { it.macroRecall }
                .thenByDescending { it.accuracy }
                .thenByDescending { it.config.deltaMargin }
                .thenBy { it.config.thetaMinProb }
        )

        val bestConfig = sortedRecords.firstOrNull { it.eligible }
        val eligibleCount = sortedRecords.count { it.eligible }

        return CandidateAOptimizationResult(
            baselineResult = baselineMetrics,
            totalConfigurations = grid.size,
            eligibleConfigurationsCount = eligibleCount,
            bestConfiguration = bestConfig,
            allRecords = sortedRecords
        )
    }

    /**
     * Tối ưu hóa từ danh sách các trận đấu thô, tự động xây dựng bối cảnh thời gian chống rò rỉ và phân tách tập dữ liệu.
     *
     * @param matches Danh sách toàn bộ trận đấu lịch sử.
     * @param matchOddsMap Bản đồ tỷ lệ cược pre-match (matchId -> OddsRecordItem).
     * @param teamEloMap Bản đồ điểm Elo ban đầu của các đội.
     * @param optimizationConfig Cấu hình kiểm soát quá trình Grid Search.
     * @return [CandidateAOptimizationResult] trên tập Calibration/Validation.
     */
    operator fun invoke(
        matches: List<Match>,
        matchOddsMap: Map<Long, OddsRecordItem> = emptyMap(),
        teamEloMap: Map<Int, Double> = emptyMap(),
        optimizationConfig: CandidateAOptimizationConfig = CandidateAOptimizationConfig()
    ): CandidateAOptimizationResult {
        val sortedMatches = matches
            .filter { it.startTimeDate.isNotBlank() && it.isEnded && it.homeScore != null && it.awayScore != null }
            .sortedWith(compareBy({ it.startTimeDate }, { it.id }))

        if (sortedMatches.isEmpty()) {
            return optimizeOnContexts(emptyList(), optimizationConfig)
        }

        val allContextsWithActual = buildChronologicalContexts(
            matches = sortedMatches,
            matchOddsMap = matchOddsMap,
            teamEloMap = teamEloMap
        )

        val splitResult = TemporalDatasetSplitter.splitChronological(
            items = allContextsWithActual,
            validationRatio = optimizationConfig.validationRatio
        )

        return optimizeOnContexts(
            validationContexts = splitResult.calibrationValidation,
            optimizationConfig = optimizationConfig
        )
    }

    private fun buildChronologicalContexts(
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
