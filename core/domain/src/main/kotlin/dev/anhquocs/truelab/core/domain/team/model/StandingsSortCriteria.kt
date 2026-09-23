package dev.anhquocs.truelab.core.domain.team.model

/**
 * Tiêu chí sắp xếp bảng xếp hạng mùa giải (Standings / Season Ranking).
 */
enum class StandingsSortCriteria {
    POSITION_ASC,        // Thứ hạng chính thức tăng dần (1, 2, 3...)
    POINTS_DESC,         // Tổng điểm giảm dần (kèm composite tie-breakers: Điểm -> Hiệu số -> Thắng -> Thứ hạng)
    GOAL_DIFF_DESC,      // Hiệu số bàn thắng giảm dần
    WINS_DESC,           // Số trận thắng nhiều nhất trước
    LOSSES_ASC           // Số trận thua ít nhất trước
}
