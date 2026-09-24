package dev.anhquocs.truelab.core.domain.evaluation.usecase

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
 * Đảm bảo nguyên tắc Temporal Data Leakage Prevention 100%:
 * Đối với mỗi trận đấu T_i cần đánh giá:
 * - Chỉ sử dụng các trận đấu diễn ra nghiêm ngặt TRƯỚC thời điểm của T_i (startTimeDate < T_i.startTimeDate).
 * - Tuyệt đối không đưa chính trận T_i hoặc các trận đấu trong tương lai vào bối cảnh phong độ (Recent Form) hay đối đầu (H2H).
 */
class BacktestPredictionUseCase(
    private val predictMatchOutcomeUseCase: PredictMatchOutcomeUseCase = PredictMatchOutcomeUseCase(),
    private val calculateEvaluationMetricsUseCase: CalculateEvaluationMetricsUseCase = CalculateEvaluationMetricsUseCase()
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

        val records = mutableListOf<BacktestMatchRecord>()

        // 3. Thực hiện dự đoán cho từng trận đấu với ngữ cảnh lịch sử nghiêm ngặt
        for (target in eligibleTargets) {
            val homeScore = target.homeScore ?: continue
            val awayScore = target.awayScore ?: continue

            // Lọc tập trận lịch sử diễn ra trước thời điểm target (chống Temporal Leakage)
            val pastMatches = sortedMatches.filter { m ->
                isStrictlyBefore(m, target) && m.isEnded && m.homeScore != null && m.awayScore != null && m.id != target.id
            }

            val homeRecent = pastMatches
                .filter { m -> m.homeTeam.id == target.homeTeam.id || m.awayTeam.id == target.homeTeam.id }
                .sortedWith(compareByDescending<Match> { it.startTimeDate }.thenByDescending { it.id })
                .take(5)

            val awayRecent = pastMatches
                .filter { m -> m.homeTeam.id == target.awayTeam.id || m.awayTeam.id == target.awayTeam.id }
                .sortedWith(compareByDescending<Match> { it.startTimeDate }.thenByDescending { it.id })
                .take(5)

            val h2h = pastMatches
                .filter { m ->
                    (m.homeTeam.id == target.homeTeam.id && m.awayTeam.id == target.awayTeam.id) ||
                        (m.homeTeam.id == target.awayTeam.id && m.awayTeam.id == target.homeTeam.id)
                }
                .sortedWith(compareByDescending<Match> { it.startTimeDate }.thenByDescending { it.id })

            val context = MatchPredictionContext(
                matchId = target.id,
                homeTeamId = target.homeTeam.id,
                awayTeamId = target.awayTeam.id,
                homeElo = teamEloMap[target.homeTeam.id],
                awayElo = teamEloMap[target.awayTeam.id],
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

    private fun isStrictlyBefore(matchA: Match, matchB: Match): Boolean {
        return matchA.startTimeDate < matchB.startTimeDate
    }
}
