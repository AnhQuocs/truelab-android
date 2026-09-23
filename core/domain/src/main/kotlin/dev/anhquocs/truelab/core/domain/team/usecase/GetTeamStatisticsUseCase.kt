package dev.anhquocs.truelab.core.domain.team.usecase

import dev.anhquocs.truelab.core.algorithm.statistics.DescriptiveStatisticsCalculator
import dev.anhquocs.truelab.core.algorithm.statistics.StatisticsCalculator
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.team.model.TeamPerformanceStatistics

/**
 * UseCase tổng hợp toàn diện các chỉ số phân phối thống kê mô tả (Mean, Median, Min, Max, Range,
 * Variance, StdDev, Skewness) cho một đội bóng từ lịch sử các trận đã đấu (Phase 3 Statistics).
 *
 * UseCase này là pure computation, độc lập hoàn toàn với Repository và Data/Room persistence.
 *
 * @param statisticsCalculator Thuật toán thống kê mô tả (mặc định [DescriptiveStatisticsCalculator]).
 */
class GetTeamStatisticsUseCase(
    private val statisticsCalculator: StatisticsCalculator = DescriptiveStatisticsCalculator()
) {

    /**
     * Tính toán các chỉ số thống kê hiệu suất thi đấu cho đội bóng.
     *
     * @param teamId ID của đội bóng cần thống kê.
     * @param matches Danh sách các trận đấu lịch sử.
     * @param currentMatchId ID của trận đấu hiện tại cần loại trừ (chống Data Leakage nếu gọi trước trận).
     * @return [TeamPerformanceStatistics] chứa thống kê mô tả chi tiết của bàn thắng ghi được, bàn thua,
     *         tổng bàn thắng và hiệu số bàn thắng.
     */
    operator fun invoke(
        teamId: Int,
        matches: List<Match>,
        currentMatchId: Long? = null
    ): TeamPerformanceStatistics {
        // 1. Lọc các trận đấu hợp lệ của teamId:
        // - Đã kết thúc (isEnded == true)
        // - Tỷ số hợp lệ (homeScore != null && awayScore != null)
        // - Loại trừ trận hiện tại (id != currentMatchId)
        // - Đội bóng có thực sự tham gia trận đấu (là home hoặc away)
        val validMatches = matches.filter { match ->
            match.isEnded &&
                match.homeScore != null &&
                match.awayScore != null &&
                (currentMatchId == null || match.id != currentMatchId) &&
                (match.homeTeam.id == teamId || match.awayTeam.id == teamId)
        }

        if (validMatches.isEmpty()) {
            val emptyStats = statisticsCalculator.summarize(emptyList())
            return TeamPerformanceStatistics(
                teamId = teamId,
                matchesCount = 0,
                goalsScoredStats = emptyStats,
                goalsConcededStats = emptyStats,
                totalGoalsStats = emptyStats,
                goalDiffStats = emptyStats
            )
        }

        // 2. Trích xuất 4 chuỗi số liệu thống kê:
        val goalsScored = ArrayList<Double>(validMatches.size)
        val goalsConceded = ArrayList<Double>(validMatches.size)
        val totalGoals = ArrayList<Double>(validMatches.size)
        val goalDiffs = ArrayList<Double>(validMatches.size)

        for (m in validMatches) {
            val isHome = m.homeTeam.id == teamId
            val hs = m.homeScore!!.toDouble()
            val as_ = m.awayScore!!.toDouble()

            val scored = if (isHome) hs else as_
            val conceded = if (isHome) as_ else hs

            goalsScored.add(scored)
            goalsConceded.add(conceded)
            totalGoals.add(hs + as_)
            goalDiffs.add(scored - conceded)
        }

        // 3. Ủy thác tính toán toàn diện cho StatisticsCalculator (Phase 3)
        return TeamPerformanceStatistics(
            teamId = teamId,
            matchesCount = validMatches.size,
            goalsScoredStats = statisticsCalculator.summarize(goalsScored),
            goalsConcededStats = statisticsCalculator.summarize(goalsConceded),
            totalGoalsStats = statisticsCalculator.summarize(totalGoals),
            goalDiffStats = statisticsCalculator.summarize(goalDiffs)
        )
    }
}
