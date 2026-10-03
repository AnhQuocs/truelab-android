package dev.anhquocs.truelab.core.domain.evaluation.usecase

import dev.anhquocs.truelab.core.algorithm.rating.EloRatingCalculator
import dev.anhquocs.truelab.core.algorithm.rating.RatingCalculator
import dev.anhquocs.truelab.core.domain.evaluation.model.BacktestMatchRecord
import dev.anhquocs.truelab.core.domain.evaluation.model.PredictionBacktestResult
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.odds.model.OddsRecordItem
import dev.anhquocs.truelab.core.domain.prediction.model.MatchPredictionContext
import dev.anhquocs.truelab.core.domain.prediction.usecase.PredictMatchOutcomeUseCase

/**
 * UseCase thuần túy (Pure Kotlin/JVM) thực thi quy trình Backtest đánh giá hiệu năng mô hình dự đoán bóng đá
 * trên tập dữ liệu trận đấu lịch sử.
 *
 * Sử dụng thuật toán Bộ tích lũy Trạng thái Tuần tự Thời gian (Chronological State Accumulator) O(N log N),
 * đảm bảo nguyên tắc Temporal Data Leakage Prevention 100%:
 * Đối với mỗi trận đấu T_i cần đánh giá:
 * - Chỉ sử dụng các trận đấu diễn ra nghiêm ngặt TRƯỚC thời điểm của T_i (startTimeDate < T_i.startTimeDate).
 * - Điểm Elo được tái hiện tuần tự (Chronological Elo Replay): dự đoán bằng Elo trước trận, cập nhật Elo sau trận.
 * - Tuyệt đối không đưa chính trận T_i hoặc các trận đấu cùng/sau thời điểm vào bối cảnh phong độ (Recent Form) hay đối đầu (H2H).
 */
class BacktestPredictionUseCase(
    private val predictMatchOutcomeUseCase: PredictMatchOutcomeUseCase = PredictMatchOutcomeUseCase(),
    private val calculateEvaluationMetricsUseCase: CalculateEvaluationMetricsUseCase = CalculateEvaluationMetricsUseCase(),
    private val eloRatingCalculator: RatingCalculator = EloRatingCalculator()
) {

    /**
     * Thực thi quy trình Backtest trên danh sách trận đấu.
     *
     * @param matches Tập dữ liệu trận đấu (bao gồm cả trận lịch sử và trận cần đánh giá).
     * @param matchOddsMap Bản đồ tỷ lệ cược đã ghi nhận cho từng trận đấu (matchId -> OddsRecordItem).
     * @param teamEloMap Bản đồ điểm Elo ban đầu / hiện tại của các đội bóng (teamId -> EloRating).
     * @return [PredictionBacktestResult] chứa ma trận nhầm lẫn, các chỉ số đánh giá và bản ghi chi tiết từng trận.
     */
    operator fun invoke(
        matches: List<Match>,
        matchOddsMap: Map<Long, OddsRecordItem> = emptyMap(),
        teamEloMap: Map<Int, Double> = emptyMap()
    ): PredictionBacktestResult {
        // 1. Sắp xếp toàn bộ trận đấu theo thứ tự thời gian tăng dần và ID để đảm bảo tính xác định (Deterministic)
        val sortedMatches = matches
            .filter { it.startTimeDate.isNotBlank() }
            .sortedWith(compareBy({ it.startTimeDate }, { it.id }))

        // 2. Lọc các trận đấu hợp lệ đủ điều kiện đánh giá (đã kết thúc và có tỷ số đầy đủ)
        val eligibleTargets = sortedMatches.filter {
            it.isEnded && it.homeScore != null && it.awayScore != null
        }

        if (eligibleTargets.isEmpty()) {
            return PredictionBacktestResult(
                evaluationResult = calculateEvaluationMetricsUseCase(emptyList()),
                records = emptyList(),
                totalMatches = 0,
                correctMatches = 0
            )
        }

        val n = eligibleTargets.size
        val records = ArrayList<BacktestMatchRecord>(n)
        val teamHistory = HashMap<Int, MutableList<Match>>()
        val h2hHistory = HashMap<Long, MutableList<Match>>()
        val currentEloMap = HashMap<Int, Double>(teamEloMap)

        var i = 0
        while (i < n) {
            val currentTimestamp = eligibleTargets[i].startTimeDate
            var j = i
            while (j < n && eligibleTargets[j].startTimeDate == currentTimestamp) {
                j++
            }

            // A. Dự đoán cho toàn bộ các trận trong nhóm mốc thời gian hiện tại dựa trên trạng thái trước mốc đó
            for (k in i until j) {
                val target = eligibleTargets[k]
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

                val prediction = predictMatchOutcomeUseCase(context)

                val actualOutcome = when {
                    homeScore > awayScore -> CalculateEvaluationMetricsUseCase.LABEL_HOME_WIN
                    homeScore == awayScore -> CalculateEvaluationMetricsUseCase.LABEL_DRAW
                    else -> CalculateEvaluationMetricsUseCase.LABEL_AWAY_WIN
                }

                val isCorrect = prediction.predictedOutcome == actualOutcome

                records.add(
                    BacktestMatchRecord(
                        matchId = target.id,
                        matchDate = target.startTimeDate,
                        homeTeamName = target.homeTeam.name,
                        awayTeamName = target.awayTeam.name,
                        predictedOutcome = prediction.predictedOutcome,
                        actualOutcome = actualOutcome,
                        homeWinProb = prediction.homeWinProb,
                        drawProb = prediction.drawProb,
                        awayWinProb = prediction.awayWinProb,
                        confidenceScore = prediction.confidenceScore,
                        isCorrect = isCorrect
                    )
                )
            }

            // B. Sau khi dự đoán xong nhóm mốc thời gian, cập nhật các trận này vào lịch sử tra cứu & Elo tích lũy
            for (k in i until j) {
                val target = eligibleTargets[k]
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

        // 4. Tổng hợp các cặp (predicted, actual) để tính toán toàn bộ Evaluation Metrics
        val pairs = records.map { Pair(it.predictedOutcome, it.actualOutcome) }
        val evaluationResult = calculateEvaluationMetricsUseCase(pairs)
        val correctCount = records.count { it.isCorrect }

        return PredictionBacktestResult(
            evaluationResult = evaluationResult,
            records = records,
            totalMatches = records.size,
            correctMatches = correctCount
        )
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
