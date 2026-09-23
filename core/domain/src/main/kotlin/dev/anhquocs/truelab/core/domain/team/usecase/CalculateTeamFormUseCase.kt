package dev.anhquocs.truelab.core.domain.team.usecase

import dev.anhquocs.truelab.core.algorithm.evaluation.FormEvaluator
import dev.anhquocs.truelab.core.algorithm.evaluation.FormScore
import dev.anhquocs.truelab.core.algorithm.evaluation.LinearDecayFormEvaluator
import dev.anhquocs.truelab.core.algorithm.evaluation.MatchOutcome
import dev.anhquocs.truelab.core.domain.match.model.Match

/**
 * UseCase tính toán phong độ thi đấu của một đội bóng dựa trên thuật toán Linear Time-Decay (Phase 5).
 *
 * UseCase này là pure computation, độc lập với Repository, nhận danh sách trận đấu và trả về [FormScore].
 *
 * @param formEvaluator Thuật toán đánh giá phong độ thi đấu (mặc định là [LinearDecayFormEvaluator]).
 */
class CalculateTeamFormUseCase(
    private val formEvaluator: FormEvaluator = LinearDecayFormEvaluator()
) {

    /**
     * Tính toán phong độ thi đấu từ danh sách trận đấu của đội bóng.
     *
     * @param teamId ID của đội bóng cần tính phong độ.
     * @param matches Danh sách trận đấu lịch sử.
     * @param windowSize Kích thước cửa sổ đánh giá (mặc định 5, phải > 0).
     * @param currentMatchId ID của trận đấu hiện tại cần loại trừ (chống Data Leakage nếu gọi trước trận).
     * @return [FormScore] chứa điểm phong độ có time-decay [0.0, 100.0], rawScore, W-D-L counts.
     * @throws IllegalArgumentException nếu [windowSize] <= 0.
     */
    operator fun invoke(
        teamId: Int,
        matches: List<Match>,
        windowSize: Int = 5,
        currentMatchId: Long? = null
    ): FormScore {
        require(windowSize > 0) {
            "windowSize must be greater than 0, but was $windowSize"
        }

        // 1. Lọc các trận đấu hợp lệ:
        // - Trận đấu đã kết thúc (isEnded == true)
        // - Có tỷ số hợp lệ (homeScore != null && awayScore != null)
        // - Không phải là trận đấu hiện tại (id != currentMatchId)
        // - Đội bóng cần tính phải tham gia trận đấu (là home hoặc away)
        val validMatches = matches.filter { match ->
            match.isEnded &&
                match.homeScore != null &&
                match.awayScore != null &&
                (currentMatchId == null || match.id != currentMatchId) &&
                (match.homeTeam.id == teamId || match.awayTeam.id == teamId)
        }

        if (validMatches.isEmpty()) {
            return formEvaluator.evaluate(emptyList(), windowSize)
        }

        // 2. Sắp xếp thứ tự thời gian tăng dần (oldest -> newest) để trận mới nhất ở cuối danh sách
        val sortedMatches = validMatches.sortedBy { it.startTimeDate }

        // 3. Ánh xạ Match -> MatchOutcome theo góc nhìn của teamId
        val outcomes = sortedMatches.mapNotNull { it.toOutcomeForTeam(teamId) }

        if (outcomes.isEmpty()) {
            return formEvaluator.evaluate(emptyList(), windowSize)
        }

        // 4. Gọi FormEvaluator của Phase 5
        return formEvaluator.evaluate(outcomes, windowSize)
    }

    internal fun Match.toOutcomeForTeam(teamId: Int): MatchOutcome? {
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
