package dev.anhquocs.truelab.core.domain.team.model

/**
 * Thống kê thành tích thi đấu của một đội bóng trong một tập trận đấu (Sân nhà, Sân khách, hoặc Tổng hợp).
 *
 * @property played Số trận đấu đã thi đấu hợp lệ (đã kết thúc và có tỷ số).
 * @property won Số trận thắng.
 * @property draw Số trận hòa.
 * @property loss Số trận thua.
 * @property goalsFor Tổng số bàn thắng ghi được.
 * @property goalsAgainst Tổng số bàn thua phải nhận.
 * @property goalDiff Hiệu số bàn thắng thua (goalsFor - goalsAgainst).
 * @property points Tổng số điểm tích lũy (won * 3 + draw).
 * @property winRate Tỷ lệ chiến thắng trong đoạn [0.0, 1.0] (won / played; nếu played == 0 thì 0.0).
 */
data class TeamPerformanceSplit(
    val played: Int,
    val won: Int,
    val draw: Int,
    val loss: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val goalDiff: Int,
    val points: Int,
    val winRate: Double
) {
    companion object {
        val EMPTY = TeamPerformanceSplit(
            played = 0,
            won = 0,
            draw = 0,
            loss = 0,
            goalsFor = 0,
            goalsAgainst = 0,
            goalDiff = 0,
            points = 0,
            winRate = 0.0
        )
    }
}

/**
 * Đóng gói toàn diện bộ thống kê phân tách Sân nhà (Home Split), Sân khách (Away Split)
 * và Tổng hợp (Total Split) của một đội bóng.
 *
 * @property teamId ID của đội bóng.
 * @property homeSplit Thành tích thi đấu trên sân nhà.
 * @property awaySplit Thành tích thi đấu trên sân khách.
 * @property totalSplit Tổng hợp toàn bộ thành tích thi đấu.
 */
data class TeamHomeAwaySplits(
    val teamId: Int,
    val homeSplit: TeamPerformanceSplit,
    val awaySplit: TeamPerformanceSplit,
    val totalSplit: TeamPerformanceSplit
)
