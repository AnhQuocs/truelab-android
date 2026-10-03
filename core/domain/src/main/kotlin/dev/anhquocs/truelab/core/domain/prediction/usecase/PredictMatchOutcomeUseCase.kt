package dev.anhquocs.truelab.core.domain.prediction.usecase

import dev.anhquocs.truelab.core.algorithm.prediction.DefaultWeightedScorer
import dev.anhquocs.truelab.core.algorithm.prediction.Signal3Way
import dev.anhquocs.truelab.core.algorithm.prediction.WeightedScorer
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.odds.selector.PreMatchOddsSelector
import dev.anhquocs.truelab.core.domain.prediction.model.MatchPredictionContext
import dev.anhquocs.truelab.core.domain.prediction.model.PredictionEvidence
import dev.anhquocs.truelab.core.domain.prediction.model.PredictionResult
import dev.anhquocs.truelab.core.domain.prediction.model.PredictionWeightConfig
import dev.anhquocs.truelab.core.domain.prediction.model.SignalEvidence
import dev.anhquocs.truelab.core.domain.prediction.model.SixthSignalMode
import dev.anhquocs.truelab.core.domain.prediction.transformer.EloSignalTransformer
import dev.anhquocs.truelab.core.domain.prediction.transformer.FormSignalTransformer
import dev.anhquocs.truelab.core.domain.prediction.transformer.GoalsSignalTransformer
import dev.anhquocs.truelab.core.domain.prediction.transformer.H2hSignalTransformer
import dev.anhquocs.truelab.core.domain.prediction.transformer.HomeAdvantageSignalTransformer
import dev.anhquocs.truelab.core.domain.prediction.transformer.OddsSignalTransformer
import dev.anhquocs.truelab.core.domain.prediction.transformer.RestAdvantageSignalTransformer
import dev.anhquocs.truelab.core.domain.team.usecase.CalculateTeamFormUseCase
import java.util.Locale

/**
 * UseCase điều phối toàn bộ luồng dự đoán kết quả trận đấu (Prediction Orchestration).
 *
 * Tích hợp 6 tín hiệu chuẩn hóa:
 * 1. Form (25%)
 * 2. Elo Rating (20%)
 * 3. Goals Expected (15%)
 * 4. Odds 1X2 (20%)
 * 5. Head-to-Head (10%)
 * 6. Rest Advantage (10% - Mặc định trong Production) hoặc Home Advantage (Baseline Benchmark Mode)
 */
class PredictMatchOutcomeUseCase(
    private val calculateTeamFormUseCase: CalculateTeamFormUseCase = CalculateTeamFormUseCase(),
    private val weightedScorer: WeightedScorer = DefaultWeightedScorer()
) {

    operator fun invoke(
        context: MatchPredictionContext,
        config: PredictionWeightConfig = PredictionWeightConfig.DEFAULT
    ): PredictionResult {
        // 1. Chuẩn bị tín hiệu Phong độ (Form)
        val homeForm = context.homeFormScore ?: calculateTeamFormUseCase(
            teamId = context.homeTeamId,
            matches = context.homeRecentMatches,
            currentMatchId = context.matchId
        )
        val awayForm = context.awayFormScore ?: calculateTeamFormUseCase(
            teamId = context.awayTeamId,
            matches = context.awayRecentMatches,
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

        // 6. Chuẩn bị tín hiệu thứ 6: Rest Advantage (Production Default) hoặc Home Advantage (Baseline Benchmark Mode)
        val homePrev = context.homeRecentMatches.firstOrNull { it.isEnded && it.id != context.matchId }
        val awayPrev = context.awayRecentMatches.firstOrNull { it.isEnded && it.id != context.matchId }

        val sixthSignal: Signal3Way = when (config.sixthSignalMode) {
            SixthSignalMode.REST_ADVANTAGE -> {
                RestAdvantageSignalTransformer.transform(
                    homePreviousMatch = homePrev,
                    awayPreviousMatch = awayPrev,
                    targetKickoffTime = context.matchStartTimeDate,
                    config = config
                )
            }
            SixthSignalMode.HOME_ADVANTAGE -> {
                HomeAdvantageSignalTransformer.transform(
                    isNeutralVenue = false,
                    config = config
                )
            }
        }

        // 7. Tập hợp danh sách đúng 6 tín hiệu Signal3Way
        val signals: List<Signal3Way> = listOf(
            formSignal,
            eloSignal,
            goalsSignal,
            oddsSignal,
            h2hSignal,
            sixthSignal
        )

        // 8. Kiểm tra tính hợp lệ của tổng trọng số
        val totalWeight = signals.sumOf { it.weight }
        if (!totalWeight.isFinite() || totalWeight <= 0.0) {
            throw IllegalStateException("All prediction signals have zero total weight or are invalid. Cannot compute prediction.")
        }

        // 9. Gọi thuật toán Phase 7 WeightedScorer
        val probabilities = weightedScorer.predictOutcome(signals)

        // 10. Tạo PredictionEvidence lưu lại toàn bộ đầu vào và trọng số thực tế
        val hElo = context.homeElo ?: 1500.0
        val aElo = context.awayElo ?: 1500.0
        val eloEff = eloSignal.weight / totalWeight
        val homeHistoryCount = context.homeRecentMatches.filter { it.isEnded && it.id != context.matchId }.size
        val awayHistoryCount = context.awayRecentMatches.filter { it.isEnded && it.id != context.matchId }.size
        val eloAvailable = (hElo != 1500.0 || aElo != 1500.0 || homeHistoryCount > 0 || awayHistoryCount > 0)
        val eloEvidence = SignalEvidence(
            name = "Elo",
            homeProb = eloSignal.homeProb,
            drawProb = eloSignal.drawProb,
            awayProb = eloSignal.awayProb,
            rawWeight = config.eloWeight,
            effectiveWeight = eloEff,
            contributionHome = eloEff * eloSignal.homeProb,
            contributionDraw = eloEff * eloSignal.drawProb,
            contributionAway = eloEff * eloSignal.awayProb,
            isAvailable = eloAvailable,
            details = mapOf(
                "homeElo" to String.format(Locale.US, "%.1f", hElo),
                "awayElo" to String.format(Locale.US, "%.1f", aElo),
                "diff" to String.format(Locale.US, "%+.1f", hElo - aElo)
            )
        )

        val formEff = formSignal.weight / totalWeight
        val formEvidence = SignalEvidence(
            name = "Form",
            homeProb = formSignal.homeProb,
            drawProb = formSignal.drawProb,
            awayProb = formSignal.awayProb,
            rawWeight = config.formWeight,
            effectiveWeight = formEff,
            contributionHome = formEff * formSignal.homeProb,
            contributionDraw = formEff * formSignal.drawProb,
            contributionAway = formEff * formSignal.awayProb,
            isAvailable = (homeHistoryCount > 0 || awayHistoryCount > 0),
            details = mapOf(
                "homeFormScore" to String.format(Locale.US, "%.2f", homeForm.score),
                "awayFormScore" to String.format(Locale.US, "%.2f", awayForm.score),
                "homeHistoryCount" to homeHistoryCount.toString(),
                "awayHistoryCount" to awayHistoryCount.toString()
            )
        )

        val oddsEff = oddsSignal.weight / totalWeight
        val oddsAvailable = (oddsSignal.weight > 0.0 && context.latestOdds != null)
        val oddsDetails = if (context.latestOdds != null) {
            val odds = context.latestOdds
            buildMap {
                put("bookmaker", odds.companyName)
                put("marketPhase", odds.marketPhase ?: "standard")
                odds.homeWin?.let { put("homeOdds", String.format(Locale.US, "%.2f", it)) }
                odds.draw?.let { put("drawOdds", String.format(Locale.US, "%.2f", it)) }
                odds.awayWin?.let { put("awayOdds", String.format(Locale.US, "%.2f", it)) }
                if (odds.changeTime > 0L) {
                    put("changeTime", odds.changeTime.toString())
                }
            }
        } else {
            emptyMap()
        }
        val oddsEvidence = SignalEvidence(
            name = "Odds",
            homeProb = oddsSignal.homeProb,
            drawProb = oddsSignal.drawProb,
            awayProb = oddsSignal.awayProb,
            rawWeight = config.oddsWeight,
            effectiveWeight = oddsEff,
            contributionHome = oddsEff * oddsSignal.homeProb,
            contributionDraw = oddsEff * oddsSignal.drawProb,
            contributionAway = oddsEff * oddsSignal.awayProb,
            isAvailable = oddsAvailable,
            details = oddsDetails
        )

        val goalsEff = goalsSignal.weight / totalWeight
        val goalsEvidence = SignalEvidence(
            name = "Goals",
            homeProb = goalsSignal.homeProb,
            drawProb = goalsSignal.drawProb,
            awayProb = goalsSignal.awayProb,
            rawWeight = config.goalsWeight,
            effectiveWeight = goalsEff,
            contributionHome = goalsEff * goalsSignal.homeProb,
            contributionDraw = goalsEff * goalsSignal.drawProb,
            contributionAway = goalsEff * goalsSignal.awayProb,
            isAvailable = (homeHistoryCount > 0 || awayHistoryCount > 0),
            details = mapOf(
                "homeMeanScored" to String.format(Locale.US, "%.2f", homeScored),
                "homeMeanConceded" to String.format(Locale.US, "%.2f", homeConceded),
                "awayMeanScored" to String.format(Locale.US, "%.2f", awayScored),
                "awayMeanConceded" to String.format(Locale.US, "%.2f", awayConceded)
            )
        )

        val h2hEff = h2hSignal.weight / totalWeight
        val totalH2hCount = hw + d + aw
        val h2hEvidence = SignalEvidence(
            name = "H2H",
            homeProb = h2hSignal.homeProb,
            drawProb = h2hSignal.drawProb,
            awayProb = h2hSignal.awayProb,
            rawWeight = config.h2hWeight,
            effectiveWeight = h2hEff,
            contributionHome = h2hEff * h2hSignal.homeProb,
            contributionDraw = h2hEff * h2hSignal.drawProb,
            contributionAway = h2hEff * h2hSignal.awayProb,
            isAvailable = (totalH2hCount > 0),
            details = mapOf(
                "homeWins" to hw.toString(),
                "draws" to d.toString(),
                "awayWins" to aw.toString(),
                "totalMatches" to totalH2hCount.toString()
            )
        )

        val sixthEff = sixthSignal.weight / totalWeight
        val restAdvEvidence = if (config.sixthSignalMode == SixthSignalMode.REST_ADVANTAGE) {
            val targetEpoch = PreMatchOddsSelector.parseKickoffEpochSeconds(context.matchStartTimeDate)
            val homePrevEpoch = PreMatchOddsSelector.parseKickoffEpochSeconds(homePrev?.startTimeDate)
            val awayPrevEpoch = PreMatchOddsSelector.parseKickoffEpochSeconds(awayPrev?.startTimeDate)

            val restIsAvailable = targetEpoch != null && homePrevEpoch != null && awayPrevEpoch != null &&
                homePrevEpoch < targetEpoch && awayPrevEpoch < targetEpoch

            val homeRestDays = if (targetEpoch != null && homePrevEpoch != null && homePrevEpoch < targetEpoch) {
                (targetEpoch - homePrevEpoch) / 86400.0
            } else null

            val awayRestDays = if (targetEpoch != null && awayPrevEpoch != null && awayPrevEpoch < targetEpoch) {
                (targetEpoch - awayPrevEpoch) / 86400.0
            } else null

            val deltaRestDays = if (homeRestDays != null && awayRestDays != null) {
                homeRestDays - awayRestDays
            } else null

            val detailsMap = mutableMapOf<String, String>()
            if (homeRestDays != null) detailsMap["homeRestDays"] = String.format(Locale.US, "%.1f", homeRestDays)
            if (awayRestDays != null) detailsMap["awayRestDays"] = String.format(Locale.US, "%.1f", awayRestDays)
            if (deltaRestDays != null) detailsMap["deltaRestDays"] = String.format(Locale.US, "%+.1f", deltaRestDays)
            if (!restIsAvailable) detailsMap["fallbackReason"] = "MISSING_PREVIOUS_MATCH"

            SignalEvidence(
                name = "Rest Advantage",
                homeProb = sixthSignal.homeProb,
                drawProb = sixthSignal.drawProb,
                awayProb = sixthSignal.awayProb,
                rawWeight = config.restAdvantageWeight,
                effectiveWeight = sixthEff,
                contributionHome = sixthEff * sixthSignal.homeProb,
                contributionDraw = sixthEff * sixthSignal.drawProb,
                contributionAway = sixthEff * sixthSignal.awayProb,
                isAvailable = restIsAvailable,
                details = detailsMap
            )
        } else {
            SignalEvidence(
                name = "Home Advantage",
                homeProb = sixthSignal.homeProb,
                drawProb = sixthSignal.drawProb,
                awayProb = sixthSignal.awayProb,
                rawWeight = config.homeAdvantageWeight,
                effectiveWeight = sixthEff,
                contributionHome = sixthEff * sixthSignal.homeProb,
                contributionDraw = sixthEff * sixthSignal.drawProb,
                contributionAway = sixthEff * sixthSignal.awayProb,
                isAvailable = true,
                details = mapOf("mode" to "BASELINE_HOME_ADVANTAGE")
            )
        }

        val predictionEvidence = PredictionEvidence(
            elo = eloEvidence,
            form = formEvidence,
            odds = oddsEvidence,
            goals = goalsEvidence,
            h2h = h2hEvidence,
            restAdvantage = restAdvEvidence,
            totalWeight = totalWeight
        )

        // 11. Map kết quả sang PredictionResult Domain Entity
        return PredictionResult(
            matchId = context.matchId,
            algorithmName = "Weighted Scoring",
            homeWinProb = probabilities.homeWinProb,
            drawProb = probabilities.drawProb,
            awayWinProb = probabilities.awayWinProb,
            predictedOutcome = probabilities.predictedOutcome.name,
            confidenceScore = probabilities.confidenceScore,
            evidence = predictionEvidence
        )
    }

    private fun calculateMeanScored(
        recentMatches: List<Match>,
        teamId: Int,
        currentMatchId: Long
    ): Double {
        val validMatches = recentMatches.filter { it.isEnded && it.id != currentMatchId }
        if (validMatches.isEmpty()) return 1.35
        val goals = validMatches.mapNotNull { m ->
            when {
                m.homeTeam.id == teamId -> m.homeScore?.toDouble()
                m.awayTeam.id == teamId -> m.awayScore?.toDouble()
                else -> null
            }
        }
        return if (goals.isEmpty()) 1.35 else goals.average()
    }

    private fun calculateMeanConceded(
        recentMatches: List<Match>,
        teamId: Int,
        currentMatchId: Long
    ): Double {
        val validMatches = recentMatches.filter { it.isEnded && it.id != currentMatchId }
        if (validMatches.isEmpty()) return 1.35
        val conceded = validMatches.mapNotNull { m ->
            when {
                m.homeTeam.id == teamId -> m.awayScore?.toDouble()
                m.awayTeam.id == teamId -> m.homeScore?.toDouble()
                else -> null
            }
        }
        return if (conceded.isEmpty()) 1.35 else conceded.average()
    }

    private fun resolveH2hCounts(context: MatchPredictionContext): Triple<Int, Int, Int> {
        val hasManualCounts = (context.homeWins != null && context.draws != null && context.awayWins != null)
        if (hasManualCounts) {
            return Triple(context.homeWins!!, context.draws!!, context.awayWins!!)
        }

        val validH2h = context.h2hMatches.filter { it.isEnded && it.id != context.matchId }
        var hw = 0
        var d = 0
        var aw = 0

        for (m in validH2h) {
            val hScore = m.homeScore
            val aScore = m.awayScore
            if (hScore == null || aScore == null) continue

            if (hScore == aScore) {
                d++
            } else if (hScore > aScore) {
                if (m.homeTeam.id == context.homeTeamId) hw++ else aw++
            } else {
                if (m.homeTeam.id == context.homeTeamId) aw++ else hw++
            }
        }
        return Triple(hw, d, aw)
    }
}
