package dev.anhquocs.truelab.core.domain.team.usecase

import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.team.model.HeadToHeadComparisonSummary
import dev.anhquocs.truelab.core.domain.team.model.TeamDetail

/**
 * UseCase thuần túy (Pure Kotlin/JVM) tổng hợp toàn bộ các chỉ số đối đầu trực tiếp,
 * phong độ thi đấu, hiệu suất sân nhà/khách và thống kê mô tả bàn thắng giữa 2 đội bóng.
 *
 * @param calculateTeamFormUseCase UseCase tính toán điểm số phong độ (Phase 5).
 * @param calculateHomeAwaySplitsUseCase UseCase tính toán phân tách Sân nhà / Sân khách.
 * @param getTeamStatisticsUseCase UseCase tính toán thống kê mô tả bàn thắng (Phase 3).
 */
class GetHeadToHeadComparisonUseCase(
    private val calculateTeamFormUseCase: CalculateTeamFormUseCase = CalculateTeamFormUseCase(),
    private val calculateHomeAwaySplitsUseCase: CalculateHomeAwaySplitsUseCase = CalculateHomeAwaySplitsUseCase(),
    private val getTeamStatisticsUseCase: GetTeamStatisticsUseCase = GetTeamStatisticsUseCase()
) {

    /**
     * Thực thi tổng hợp báo cáo đối sánh giữa [teamA] và [teamB].
     *
     * @param teamA Thông tin chi tiết đội A.
     * @param teamB Thông tin chi tiết đội B.
     * @param h2hMatches Danh sách các trận đối đầu trực tiếp trong lịch sử giữa 2 đội.
     * @param teamARecentMatches Danh sách các trận gần nhất của đội A (để tính Form, Splits, Stats).
     * @param teamBRecentMatches Danh sách các trận gần nhất của đội B (để tính Form, Splits, Stats).
     * @return [HeadToHeadComparisonSummary] chứa toàn bộ số liệu đối sánh hai chiều.
     */
    operator fun invoke(
        teamA: TeamDetail,
        teamB: TeamDetail,
        h2hMatches: List<Match>,
        teamARecentMatches: List<Match> = emptyList(),
        teamBRecentMatches: List<Match> = emptyList()
    ): HeadToHeadComparisonSummary {
        // 1. Lọc các trận đối đầu hợp lệ (đã kết thúc và có tỷ số đầy đủ)
        val validH2h = h2hMatches.filter {
            it.isEnded && it.homeScore != null && it.awayScore != null &&
                ((it.homeTeam.id == teamA.id && it.awayTeam.id == teamB.id) ||
                    (it.homeTeam.id == teamB.id && it.awayTeam.id == teamA.id))
        }

        val totalMatches = validH2h.size
        var teamAWins = 0
        var draws = 0
        var teamBWins = 0
        var teamAGoals = 0
        var teamBGoals = 0

        // 2. Tính toán số trận thắng - hòa - thua và tổng bàn thắng tương ứng
        for (m in validH2h) {
            val hScore = m.homeScore ?: 0
            val aScore = m.awayScore ?: 0

            if (m.homeTeam.id == teamA.id) {
                // Team A là đội chủ nhà, Team B là đội khách
                teamAGoals += hScore
                teamBGoals += aScore
                when {
                    m.isHomeWin -> teamAWins++
                    m.isAwayWin -> teamBWins++
                    m.isDraw -> draws++
                }
            } else {
                // Team B là đội chủ nhà, Team A là đội khách
                teamBGoals += hScore
                teamAGoals += aScore
                when {
                    m.isHomeWin -> teamBWins++
                    m.isAwayWin -> teamAWins++
                    m.isDraw -> draws++
                }
            }
        }

        val teamAWinPct = if (totalMatches > 0) teamAWins.toDouble() / totalMatches else 0.0
        val drawPct = if (totalMatches > 0) draws.toDouble() / totalMatches else 0.0
        val teamBWinPct = if (totalMatches > 0) teamBWins.toDouble() / totalMatches else 0.0

        // 3. Tính toán phong độ 5 trận gần nhất
        val teamAForm = if (teamARecentMatches.isNotEmpty()) {
            calculateTeamFormUseCase(teamA.id, teamARecentMatches)
        } else null

        val teamBForm = if (teamBRecentMatches.isNotEmpty()) {
            calculateTeamFormUseCase(teamB.id, teamBRecentMatches)
        } else null

        // 4. Tính toán phân tách Sân nhà / Sân khách
        val teamASplits = calculateHomeAwaySplitsUseCase(teamA.id, teamARecentMatches)
        val teamBSplits = calculateHomeAwaySplitsUseCase(teamB.id, teamBRecentMatches)

        // 5. Tính toán phân phối thống kê bàn thắng
        val teamAStats = if (teamARecentMatches.isNotEmpty()) {
            getTeamStatisticsUseCase(teamA.id, teamARecentMatches)
        } else null

        val teamBStats = if (teamBRecentMatches.isNotEmpty()) {
            getTeamStatisticsUseCase(teamB.id, teamBRecentMatches)
        } else null

        // 6. Lấy 5 trận đã kết thúc gần nhất của từng đội, sắp xếp tăng dần theo thời gian (cũ nhất -> mới nhất) cho timeline
        val validRecentA = teamARecentMatches
            .filter { it.isEnded && it.homeScore != null && it.awayScore != null && (it.homeTeam.id == teamA.id || it.awayTeam.id == teamA.id) }
            .sortedByDescending { it.startTimeDate }
            .take(5)
            .sortedBy { it.startTimeDate }

        val validRecentB = teamBRecentMatches
            .filter { it.isEnded && it.homeScore != null && it.awayScore != null && (it.homeTeam.id == teamB.id || it.awayTeam.id == teamB.id) }
            .sortedByDescending { it.startTimeDate }
            .take(5)
            .sortedBy { it.startTimeDate }

        return HeadToHeadComparisonSummary(
            teamA = teamA,
            teamB = teamB,
            totalMatches = totalMatches,
            teamAWins = teamAWins,
            draws = draws,
            teamBWins = teamBWins,
            teamAGoals = teamAGoals,
            teamBGoals = teamBGoals,
            teamAWinPercent = teamAWinPct,
            drawPercent = drawPct,
            teamBWinPercent = teamBWinPct,
            teamAForm = teamAForm,
            teamBForm = teamBForm,
            teamAHomeAwaySplits = teamASplits,
            teamBHomeAwaySplits = teamBSplits,
            teamAStats = teamAStats,
            teamBStats = teamBStats,
            teamARecentMatches = validRecentA,
            teamBRecentMatches = validRecentB,
            h2hMatches = validH2h.sortedByDescending { it.startTimeDate }
        )
    }
}
