package dev.anhquocs.truelab.core.domain.evaluation.usecase

import dev.anhquocs.truelab.core.domain.evaluation.model.BacktestMatchRecord
import dev.anhquocs.truelab.core.domain.evaluation.model.DailyBacktestProgressEvent
import dev.anhquocs.truelab.core.domain.evaluation.model.DailyBacktestResult
import dev.anhquocs.truelab.core.domain.evaluation.model.EvaluationPhase
import dev.anhquocs.truelab.core.domain.evaluation.model.OddsCoverageStats
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.odds.repository.OddsRepository
import dev.anhquocs.truelab.core.domain.odds.selector.PreMatchOddsSelector
import dev.anhquocs.truelab.core.domain.prediction.model.DrawStrategyConfig
import dev.anhquocs.truelab.core.domain.prediction.model.MatchPredictionContext
import dev.anhquocs.truelab.core.domain.prediction.usecase.PredictMatchOutcomeUseCase
import dev.anhquocs.truelab.core.domain.team.usecase.CalculateDynamicEloUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger

/**
 * UseCase Orchestration thực thi quy trình Daily Backtest Evaluation trên các trận FT của một ngày cụ thể.
 *
 * Đảm bảo các nguyên tắc cốt lõi:
 * 1. Tái sử dụng 100% [PredictMatchOutcomeUseCase] hiện tại.
 * 2. On-Demand Odds Ingestion & không ngộ nhận độ phủ Odds (Odds Coverage Tracking).
 * 3. 100% Zero Temporal Data Leakage: mọi dữ liệu Form, Elo, H2H, Odds đều diễn ra trước giờ bóng lăn.
 * 4. Đối soát kết quả FT thực tế sau khi tính toán xong dự đoán.
 * 5. Giới hạn đồng thời (Bounded Concurrency qua [Semaphore]) tránh nghẽn I/O & rate limit API.
 */
class RunDailyBacktestUseCase(
    private val oddsRepository: OddsRepository,
    private val predictMatchOutcomeUseCase: PredictMatchOutcomeUseCase = PredictMatchOutcomeUseCase(),
    private val calculateDynamicEloUseCase: CalculateDynamicEloUseCase = CalculateDynamicEloUseCase(),
    private val calculateEvaluationMetricsUseCase: CalculateEvaluationMetricsUseCase = CalculateEvaluationMetricsUseCase(),
    private val maxConcurrency: Int = 3
) {

    /**
     * Thực thi quy trình Daily Backtest và phát ra luồng sự kiện tiến độ thời gian thực.
     *
     * @param evaluationDate Chuỗi ngày đánh giá (ISO yyyy-MM-dd).
     * @param targetMatches Danh sách các trận FT cần đánh giá trong ngày.
     * @param allMatches Toàn bộ danh sách trận trong database dùng để xây dựng bối cảnh lịch sử và Elo.
     * @param initialEloMap Bản đồ Elo khởi tạo cho các đội (mặc định 1500.0).
     * @param drawStrategyConfig Cấu hình chiến lược Draw Modeling (mặc định BASELINE).
     * @return [Flow] phát ra các [DailyBacktestProgressEvent].
     */
    operator fun invoke(
        evaluationDate: String,
        targetMatches: List<Match>,
        allMatches: List<Match>,
        initialEloMap: Map<Int, Double> = emptyMap(),
        drawStrategyConfig: DrawStrategyConfig = DrawStrategyConfig.DEFAULT
    ): Flow<DailyBacktestProgressEvent> = flow {
        // 1. Lọc và sắp xếp các trận FT hợp lệ
        val validTargets = targetMatches
            .filter { it.isEnded && it.homeScore != null && it.awayScore != null && it.startTimeDate.isNotBlank() }
            .sortedWith(compareBy({ it.startTimeDate }, { it.id }))

        val totalMatches = validTargets.size
        if (totalMatches == 0) {
            emit(
                DailyBacktestProgressEvent.Completed(
                    result = DailyBacktestResult(
                        evaluationDate = evaluationDate,
                        totalMatches = 0,
                        correctMatches = 0,
                        accuracy = 0.0,
                        oddsCoverage = OddsCoverageStats(
                            totalMatches = 0,
                            matchesWithUsableOdds = 0,
                            matchesWithoutOdds = 0,
                            coveragePercentage = 0.0
                        ),
                        evaluationResult = calculateEvaluationMetricsUseCase(emptyList()),
                        records = emptyList()
                    )
                )
            )
            return@flow
        }

        // 2. Tiền xử lý danh sách trận lịch sử đã kết thúc để tối ưu hiệu năng lọc
        val historicalEndedMatches = allMatches.filter {
            it.isEnded && it.homeScore != null && it.awayScore != null && it.startTimeDate.isNotBlank()
        }

        val evaluatedRecords = ArrayList<BacktestMatchRecord>(totalMatches)
        val completedCount = AtomicInteger(0)
        val semaphore = Semaphore(maxConcurrency)

        for (target in validTargets) {
            val matchName = "${target.homeTeam.name} vs ${target.awayTeam.name}"

            // Giai đoạn 1: Nạp Odds on-demand
            emit(
                DailyBacktestProgressEvent.Progress(
                    completedCount = completedCount.get(),
                    totalCount = totalMatches,
                    currentMatchName = matchName,
                    currentPhase = EvaluationPhase.FETCHING_ODDS
                )
            )

            val preMatchOdds = semaphore.withPermit {
                try {
                    oddsRepository.fetchAndCacheOddsForMatch(target.id)
                } catch (_: Exception) {
                    // Tiếp tục xử lý ngay cả khi API lỗi (missing signal fallback)
                }
                val matchOdds = oddsRepository.getMatchOdds(target.id).firstOrNull()
                PreMatchOddsSelector.selectPreMatchEuropeanOdds(
                    oddsList = matchOdds?.oddsList ?: emptyList(),
                    kickoffTime = target.startTimeDate
                )
            }

            // Giai đoạn 2: Chuẩn bị bối cảnh lịch sử trước kickoff (Zero Leakage)
            emit(
                DailyBacktestProgressEvent.Progress(
                    completedCount = completedCount.get(),
                    totalCount = totalMatches,
                    currentMatchName = matchName,
                    currentPhase = EvaluationPhase.PREPARING_CONTEXT
                )
            )

            val homeTeamId = target.homeTeam.id
            val awayTeamId = target.awayTeam.id
            val targetTime = target.startTimeDate

            // Tính Dynamic Elo trước mốc kickoff
            val dynamicEloMap = calculateDynamicEloUseCase(
                allMatches = historicalEndedMatches,
                beforeTimestamp = targetTime,
                initialEloMap = initialEloMap
            )
            val homeElo = dynamicEloMap[homeTeamId] ?: initialEloMap[homeTeamId] ?: 1500.0
            val awayElo = dynamicEloMap[awayTeamId] ?: initialEloMap[awayTeamId] ?: 1500.0

            // Lọc các trận diễn ra nghiêm ngặt trước target match
            val homeHistory = historicalEndedMatches
                .filter { it.id != target.id && (it.homeTeam.id == homeTeamId || it.awayTeam.id == homeTeamId) && it.startTimeDate < targetTime }
                .sortedByDescending { it.startTimeDate }

            val awayHistory = historicalEndedMatches
                .filter { it.id != target.id && (it.homeTeam.id == awayTeamId || it.awayTeam.id == awayTeamId) && it.startTimeDate < targetTime }
                .sortedByDescending { it.startTimeDate }

            val h2hHistory = historicalEndedMatches
                .filter {
                    it.id != target.id &&
                        ((it.homeTeam.id == homeTeamId && it.awayTeam.id == awayTeamId) || (it.homeTeam.id == awayTeamId && it.awayTeam.id == homeTeamId)) &&
                        it.startTimeDate < targetTime
                }
                .sortedByDescending { it.startTimeDate }

            val context = MatchPredictionContext(
                matchId = target.id,
                homeTeamId = homeTeamId,
                awayTeamId = awayTeamId,
                matchStartTimeDate = target.startTimeDate,
                homeElo = homeElo,
                awayElo = awayElo,
                homeRecentMatches = homeHistory,
                awayRecentMatches = awayHistory,
                h2hMatches = h2hHistory,
                latestOdds = preMatchOdds
            )

            // Giai đoạn 3: Thực thi Prediction Pipeline 6 Signals
            emit(
                DailyBacktestProgressEvent.Progress(
                    completedCount = completedCount.get(),
                    totalCount = totalMatches,
                    currentMatchName = matchName,
                    currentPhase = EvaluationPhase.PREDICTING
                )
            )

            val predictionResult = predictMatchOutcomeUseCase(
                context = context,
                drawStrategyConfig = drawStrategyConfig
            )

            // Giai đoạn 4: Đối soát với kết quả FT thực tế (Strictly Post-Prediction Evaluation)
            emit(
                DailyBacktestProgressEvent.Progress(
                    completedCount = completedCount.get(),
                    totalCount = totalMatches,
                    currentMatchName = matchName,
                    currentPhase = EvaluationPhase.EVALUATING
                )
            )

            val homeScore = target.homeScore ?: 0
            val awayScore = target.awayScore ?: 0
            val actualOutcome = when {
                homeScore > awayScore -> CalculateEvaluationMetricsUseCase.LABEL_HOME_WIN
                homeScore == awayScore -> CalculateEvaluationMetricsUseCase.LABEL_DRAW
                else -> CalculateEvaluationMetricsUseCase.LABEL_AWAY_WIN
            }

            val isCorrect = predictionResult.predictedOutcome == actualOutcome
            val record = BacktestMatchRecord(
                matchId = target.id,
                matchDate = target.startTimeDate,
                homeTeamName = target.homeTeam.name,
                awayTeamName = target.awayTeam.name,
                predictedOutcome = predictionResult.predictedOutcome,
                actualOutcome = actualOutcome,
                homeWinProb = predictionResult.homeWinProb,
                drawProb = predictionResult.drawProb,
                awayWinProb = predictionResult.awayWinProb,
                confidenceScore = predictionResult.confidenceScore,
                isCorrect = isCorrect,
                homeScore = homeScore,
                awayScore = awayScore,
                hasUsableOdds = preMatchOdds != null
            )
            evaluatedRecords.add(record)

            val currentDone = completedCount.incrementAndGet()
            emit(
                DailyBacktestProgressEvent.Progress(
                    completedCount = currentDone,
                    totalCount = totalMatches,
                    currentMatchName = matchName,
                    currentPhase = EvaluationPhase.EVALUATING
                )
            )
        }

        // 3. Tính toán các chỉ số tổng hợp
        val predictionPairs = evaluatedRecords.map { it.predictedOutcome to it.actualOutcome }
        val evaluationMetrics = calculateEvaluationMetricsUseCase(predictionPairs)
        val correctCount = evaluatedRecords.count { it.isCorrect }
        val accuracy = if (totalMatches > 0) correctCount.toDouble() / totalMatches else 0.0

        val matchesWithOdds = evaluatedRecords.count { it.hasUsableOdds }
        val matchesWithoutOdds = totalMatches - matchesWithOdds
        val coveragePercentage = if (totalMatches > 0) (matchesWithOdds.toDouble() / totalMatches) * 100.0 else 0.0

        val finalResult = DailyBacktestResult(
            evaluationDate = evaluationDate,
            totalMatches = totalMatches,
            correctMatches = correctCount,
            accuracy = accuracy,
            oddsCoverage = OddsCoverageStats(
                totalMatches = totalMatches,
                matchesWithUsableOdds = matchesWithOdds,
                matchesWithoutOdds = matchesWithoutOdds,
                coveragePercentage = coveragePercentage
            ),
            evaluationResult = evaluationMetrics,
            records = evaluatedRecords
        )

        emit(DailyBacktestProgressEvent.Completed(finalResult))
    }
}
