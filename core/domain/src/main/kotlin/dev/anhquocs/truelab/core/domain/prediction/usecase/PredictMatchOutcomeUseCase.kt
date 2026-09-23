package dev.anhquocs.truelab.core.domain.prediction.usecase

import dev.anhquocs.truelab.core.algorithm.evaluation.FormEvaluator
import dev.anhquocs.truelab.core.algorithm.evaluation.FormScore
import dev.anhquocs.truelab.core.algorithm.evaluation.LinearDecayFormEvaluator
import dev.anhquocs.truelab.core.algorithm.evaluation.MatchOutcome
import dev.anhquocs.truelab.core.algorithm.prediction.DefaultWeightedScorer
import dev.anhquocs.truelab.core.algorithm.prediction.Signal3Way
import dev.anhquocs.truelab.core.algorithm.prediction.WeightedScorer
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.prediction.model.MatchPredictionContext
import dev.anhquocs.truelab.core.domain.prediction.model.PredictionResult
import dev.anhquocs.truelab.core.domain.prediction.model.PredictionWeightConfig
import dev.anhquocs.truelab.core.domain.prediction.transformer.EloSignalTransformer
import dev.anhquocs.truelab.core.domain.prediction.transformer.FormSignalTransformer
import dev.anhquocs.truelab.core.domain.prediction.transformer.GoalsSignalTransformer
import dev.anhquocs.truelab.core.domain.prediction.transformer.H2hSignalTransformer
import dev.anhquocs.truelab.core.domain.prediction.transformer.HomeAdvantageSignalTransformer
import dev.anhquocs.truelab.core.domain.prediction.transformer.OddsSignalTransformer

/**
 * UseCase Orchestration điều phối toàn bộ quy trình thu thập đặc trưng và tính toán dự đoán kết quả trận đấu.
 *
 * Quy trình xử lý:
 * Match / Data Context
 *   -> Trích xuất & chuẩn bị đặc trưng nghiệp vụ (loại bỏ leakage)
 *   -> 6 Signal Transformers (Form, Elo, Goals, Odds, H2H, HomeAdvantage)
 *   -> List<Signal3Way>
 *   -> WeightedScorer.predictOutcome(signals)
 *   -> PredictionResult
 *
 * @param weightedScorer Thuật toán tính điểm và dự đoán theo mô hình trọng số (Phase 7).
 * @param formEvaluator Thuật toán đánh giá phong độ thi đấu (Phase 5).
 * @param defaultConfig Cấu hình trọng số và tham số mặc định theo FR-14.
 */
class PredictMatchOutcomeUseCase(
    private val weightedScorer: WeightedScorer = DefaultWeightedScorer(),
    private val formEvaluator: FormEvaluator = LinearDecayFormEvaluator(),
    private val defaultConfig: PredictionWeightConfig = PredictionWeightConfig.DEFAULT
) {

    /**
     * Thực hiện điều phối và tính toán kết quả dự đoán từ [MatchPredictionContext].
     *
     * @param context Ngữ cảnh chứa toàn bộ dữ liệu trận đấu và thống kê liên quan.
     * @param configOverride Cấu hình trọng số tùy chỉnh (tùy chọn, mặc định sử dụng [defaultConfig]).
     * @return [PredictionResult] chứa phân phối xác suất 3 chiều, kết quả dự đoán và độ tin cậy.
     * @throws IllegalStateException nếu tất cả các tín hiệu đều có trọng số 0.0 hoặc không thể tính toán.
     */
    operator fun invoke(
        context: MatchPredictionContext,
        configOverride: PredictionWeightConfig? = null
    ): PredictionResult {
        val config = configOverride ?: defaultConfig

        // 1. Chuẩn bị tín hiệu Form (Phong độ 5 trận gần nhất)
        val homeForm = resolveFormScore(
            explicitForm = context.homeFormScore,
            recentMatches = context.homeRecentMatches,
            teamId = context.homeTeamId,
            currentMatchId = context.matchId
        )
        val awayForm = resolveFormScore(
            explicitForm = context.awayFormScore,
            recentMatches = context.awayRecentMatches,
            teamId = context.awayTeamId,
            currentMatchId = context.matchId
        )
        val formSignal = FormSignalTransformer.transform(homeForm, awayForm, config)

        // 2. Chuẩn bị tín hiệu Elo Rating
        val eloSignal = EloSignalTransformer.transform(context.homeElo, context.awayElo, config)

        // 3. Chuẩn bị tín hiệu Goals (Hiệu suất bàn thắng kỳ vọng)
        val homeScored = context.homeMeanScored ?: calculateMeanScored(
            context.homeRecentMatches,
            context.homeTeamId,
            context.matchId
        )
        val homeConceded = context.homeMeanConceded ?: calculateMeanConceded(
            context.homeRecentMatches,
            context.homeTeamId,
            context.matchId
        )
        val awayScored = context.awayMeanScored ?: calculateMeanScored(
            context.awayRecentMatches,
            context.awayTeamId,
            context.matchId
        )
        val awayConceded = context.awayMeanConceded ?: calculateMeanConceded(
            context.awayRecentMatches,
            context.awayTeamId,
            context.matchId
        )

        val goalsSignal = GoalsSignalTransformer.transform(
            homeMeanScored = homeScored,
            homeMeanConceded = homeConceded,
            awayMeanScored = awayScored,
            awayMeanConceded = awayConceded,
            config = config
        )

        // 4. Chuẩn bị tín hiệu Odds Nhà cung cấp
        val oddsSignal = OddsSignalTransformer.transform(context.latestOdds, config)

        // 5. Chuẩn bị tín hiệu Head-to-Head (H2H)
        val (hw, d, aw) = resolveH2hCounts(context)
        val h2hSignal = H2hSignalTransformer.transform(hw, d, aw, config)

        // 6. Chuẩn bị tín hiệu Ưu thế sân nhà (Home Advantage)
        val homeAdvSignal = HomeAdvantageSignalTransformer.transform(context.isNeutralVenue, config)

        // 7. Tập hợp danh sách 6 tín hiệu Signal3Way
        val signals: List<Signal3Way> = listOf(
            formSignal,
            eloSignal,
            goalsSignal,
            oddsSignal,
            h2hSignal,
            homeAdvSignal
        )

        // 8. Kiểm tra tính hợp lệ của tổng trọng số
        val totalWeight = signals.sumOf { it.weight }
        if (!totalWeight.isFinite() || totalWeight <= 0.0) {
            throw IllegalStateException("All prediction signals have zero total weight or are invalid. Cannot compute prediction.")
        }

        // 9. Gọi thuật toán Phase 7 WeightedScorer
        val probabilities = weightedScorer.predictOutcome(signals)

        // 10. Map kết quả sang PredictionResult Domain Entity
        return PredictionResult(
            matchId = context.matchId,
            algorithmName = "Weighted Scoring",
            homeWinProb = probabilities.homeWinProb,
            drawProb = probabilities.drawProb,
            awayWinProb = probabilities.awayWinProb,
            predictedOutcome = probabilities.predictedOutcome.name,
            confidenceScore = probabilities.confidenceScore
        )
    }

    private fun resolveFormScore(
        explicitForm: FormScore?,
        recentMatches: List<Match>,
        teamId: Int,
        currentMatchId: Long
    ): FormScore? {
        if (explicitForm != null) return explicitForm

        // Lọc các trận đã kết thúc trong lịch sử, loại trừ chính trận đấu đang được dự đoán (chống Data Leakage)
        val outcomes = recentMatches
            .filter { it.isEnded && it.id != currentMatchId }
            .mapNotNull { it.toOutcomeForTeam(teamId) }
            .take(5)

        return if (outcomes.isNotEmpty()) {
            formEvaluator.evaluate(outcomes, windowSize = 5)
        } else {
            null
        }
    }

    private fun calculateMeanScored(matches: List<Match>, teamId: Int, currentMatchId: Long): Double? {
        val ended = matches.filter { it.isEnded && it.id != currentMatchId }
        val goals = ended.mapNotNull { m ->
            when (teamId) {
                m.homeTeam.id -> m.homeScore
                m.awayTeam.id -> m.awayScore
                else -> null
            }
        }
        return if (goals.isNotEmpty()) goals.average() else null
    }

    private fun calculateMeanConceded(matches: List<Match>, teamId: Int, currentMatchId: Long): Double? {
        val ended = matches.filter { it.isEnded && it.id != currentMatchId }
        val goals = ended.mapNotNull { m ->
            when (teamId) {
                m.homeTeam.id -> m.awayScore
                m.awayTeam.id -> m.homeScore
                else -> null
            }
        }
        return if (goals.isNotEmpty()) goals.average() else null
    }

    private fun resolveH2hCounts(context: MatchPredictionContext): Triple<Int, Int, Int> {
        if (context.homeWins != null && context.draws != null && context.awayWins != null) {
            return Triple(context.homeWins, context.draws, context.awayWins)
        }

        var hw = 0
        var d = 0
        var aw = 0

        val endedH2h = context.h2hMatches.filter { it.isEnded && it.id != context.matchId }
        for (m in endedH2h) {
            val hs = m.homeScore ?: continue
            val as_ = m.awayScore ?: continue
            when {
                hs == as_ -> d++
                m.homeTeam.id == context.homeTeamId -> if (hs > as_) hw++ else aw++
                m.homeTeam.id == context.awayTeamId -> if (as_ > hs) hw++ else aw++
            }
        }

        return Triple(hw, d, aw)
    }

    private fun Match.toOutcomeForTeam(teamId: Int): MatchOutcome? {
        if (!isEnded) return null
        val hs = homeScore ?: return null
        val as_ = awayScore ?: return null
        return when {
            hs == as_ -> MatchOutcome.DRAW
            homeTeam.id == teamId -> if (hs > as_) MatchOutcome.WIN else MatchOutcome.LOSS
            awayTeam.id == teamId -> if (as_ > hs) MatchOutcome.WIN else MatchOutcome.LOSS
            else -> null
        }
    }
}
