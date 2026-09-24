package dev.anhquocs.truelab.core.domain.team.usecase

import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.team.model.TeamHomeAwaySplits
import dev.anhquocs.truelab.core.domain.team.model.TeamPerformanceSplit

/**
 * UseCase thuần túy (Pure Kotlin/JVM) tính toán phân tích hiệu suất thi đấu của một đội bóng
 * phân tách theo 3 nhóm: Sân nhà (Home), Sân khách (Away) và Tổng hợp (Total).
 *
 * Nguyên tắc nghiệp vụ:
 * - Chỉ tính các trận đấu đã kết thúc ([Match.isEnded] == true) và có tỷ số hợp lệ ([Match.homeScore] != null && [Match.awayScore] != null).
 * - Bỏ qua các trận đấu mà đội bóng không tham gia (không phải đội nhà cũng không phải đội khách).
 * - Phân loại Home: [Match.homeTeam.id] == teamId.
 * - Phân loại Away: [Match.awayTeam.id] == teamId.
 * - Công thức điểm số: points = won * 3 + draw.
 * - Hiệu số bàn thắng: goalDiff = goalsFor - goalsAgainst.
 * - Tỷ lệ thắng: winRate = if (played == 0) 0.0 else won.toDouble() / played.toDouble().
 */
class CalculateHomeAwaySplitsUseCase {

    /**
     * Thực thi tính toán [TeamHomeAwaySplits] cho một đội bóng từ danh sách trận đấu.
     *
     * @param teamId ID của đội bóng cần phân tích.
     * @param matches Danh sách các trận đấu đầu vào.
     * @return [TeamHomeAwaySplits] chứa thống kê Sân nhà, Sân khách và Tổng hợp.
     */
    operator fun invoke(
        teamId: Int,
        matches: List<Match>
    ): TeamHomeAwaySplits {
        // Lọc các trận đấu hợp lệ mà đội bóng có tham gia
        val validMatches = matches.filter {
            it.isEnded && it.homeScore != null && it.awayScore != null &&
                (it.homeTeam.id == teamId || it.awayTeam.id == teamId)
        }

        if (validMatches.isEmpty()) {
            return TeamHomeAwaySplits(
                teamId = teamId,
                homeSplit = TeamPerformanceSplit.EMPTY,
                awaySplit = TeamPerformanceSplit.EMPTY,
                totalSplit = TeamPerformanceSplit.EMPTY
            )
        }

        val homeMatches = validMatches.filter { it.homeTeam.id == teamId }
        val awayMatches = validMatches.filter { it.awayTeam.id == teamId }

        val homeSplit = computeSplit(teamId, homeMatches, isHome = true)
        val awaySplit = computeSplit(teamId, awayMatches, isHome = false)

        val totalPlayed = homeSplit.played + awaySplit.played
        val totalWon = homeSplit.won + awaySplit.won
        val totalDraw = homeSplit.draw + awaySplit.draw
        val totalLoss = homeSplit.loss + awaySplit.loss
        val totalGoalsFor = homeSplit.goalsFor + awaySplit.goalsFor
        val totalGoalsAgainst = homeSplit.goalsAgainst + awaySplit.goalsAgainst
        val totalGoalDiff = totalGoalsFor - totalGoalsAgainst
        val totalPoints = totalWon * 3 + totalDraw
        val totalWinRate = if (totalPlayed == 0) 0.0 else totalWon.toDouble() / totalPlayed.toDouble()

        val totalSplit = TeamPerformanceSplit(
            played = totalPlayed,
            won = totalWon,
            draw = totalDraw,
            loss = totalLoss,
            goalsFor = totalGoalsFor,
            goalsAgainst = totalGoalsAgainst,
            goalDiff = totalGoalDiff,
            points = totalPoints,
            winRate = totalWinRate
        )

        return TeamHomeAwaySplits(
            teamId = teamId,
            homeSplit = homeSplit,
            awaySplit = awaySplit,
            totalSplit = totalSplit
        )
    }

    private fun computeSplit(
        teamId: Int,
        matches: List<Match>,
        isHome: Boolean
    ): TeamPerformanceSplit {
        if (matches.isEmpty()) {
            return TeamPerformanceSplit.EMPTY
        }

        var won = 0
        var draw = 0
        var loss = 0
        var goalsFor = 0
        var goalsAgainst = 0

        for (match in matches) {
            val homeScore = match.homeScore ?: continue
            val awayScore = match.awayScore ?: continue

            val (gf, ga) = if (isHome) {
                homeScore to awayScore
            } else {
                awayScore to homeScore
            }

            goalsFor += gf
            goalsAgainst += ga

            when {
                gf > ga -> won++
                gf == ga -> draw++
                else -> loss++
            }
        }

        val played = matches.size
        val goalDiff = goalsFor - goalsAgainst
        val points = won * 3 + draw
        val winRate = if (played == 0) 0.0 else won.toDouble() / played.toDouble()

        return TeamPerformanceSplit(
            played = played,
            won = won,
            draw = draw,
            loss = loss,
            goalsFor = goalsFor,
            goalsAgainst = goalsAgainst,
            goalDiff = goalDiff,
            points = points,
            winRate = winRate
        )
    }
}
