package dev.anhquocs.truelab.core.domain.team.model

import dev.anhquocs.truelab.core.algorithm.evaluation.FormScore
import dev.anhquocs.truelab.core.domain.match.model.Match

/**
 * Domain entity đóng gói toàn bộ dữ liệu đối sánh trực tiếp giữa hai đội bóng.
 *
 * @property teamA Thông tin chi tiết đội A.
 * @property teamB Thông tin chi tiết đội B.
 * @property totalMatches Tổng số trận đối đầu trực tiếp đã kết thúc.
 * @property teamAWins Số trận thắng của đội A.
 * @property draws Số trận hòa giữa 2 đội.
 * @property teamBWins Số trận thắng của đội B.
 * @property teamAGoals Tổng số bàn thắng đội A ghi được trong các trận đối đầu.
 * @property teamBGoals Tổng số bàn thắng đội B ghi được trong các trận đối đầu.
 * @property teamAWinPercent Tỷ lệ thắng của đội A trong đối đầu (0.0 đến 1.0).
 * @property drawPercent Tỷ lệ hòa trong đối đầu (0.0 đến 1.0).
 * @property teamBWinPercent Tỷ lệ thắng của đội B trong đối đầu (0.0 đến 1.0).
 * @property teamAForm Điểm số và chuỗi phong độ của đội A (5 trận gần nhất).
 * @property teamBForm Điểm số và chuỗi phong độ của đội B (5 trận gần nhất).
 * @property teamAHomeAwaySplits Hiệu suất thi đấu Sân nhà / Sân khách / Tổng hợp của đội A.
 * @property teamBHomeAwaySplits Hiệu suất thi đấu Sân nhà / Sân khách / Tổng hợp của đội B.
 * @property teamAStats Thống kê mô tả bàn thắng của đội A.
 * @property teamBStats Thống kê mô tả bàn thắng của đội B.
 * @property h2hMatches Danh sách các trận đấu đối đầu trực tiếp theo thứ tự thời gian mới nhất.
 */
data class HeadToHeadComparisonSummary(
    val teamA: TeamDetail,
    val teamB: TeamDetail,
    val totalMatches: Int,
    val teamAWins: Int,
    val draws: Int,
    val teamBWins: Int,
    val teamAGoals: Int,
    val teamBGoals: Int,
    val teamAWinPercent: Double,
    val drawPercent: Double,
    val teamBWinPercent: Double,
    val teamAForm: FormScore?,
    val teamBForm: FormScore?,
    val teamAHomeAwaySplits: TeamHomeAwaySplits,
    val teamBHomeAwaySplits: TeamHomeAwaySplits,
    val teamAStats: TeamPerformanceStatistics?,
    val teamBStats: TeamPerformanceStatistics?,
    val h2hMatches: List<Match>
)
