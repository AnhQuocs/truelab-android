package dev.anhquocs.truelab.core.domain.match.model

/**
 * Tiêu chí sắp xếp danh sách các trận đấu.
 */
enum class MatchSortCriteria {
    START_TIME_ASC,      // Thời gian thi đấu tăng dần (sớm nhất trước)
    START_TIME_DESC,     // Thời gian thi đấu giảm dần (mới nhất trước)
    TOTAL_GOALS_DESC,    // Tổng số bàn thắng nhiều nhất trước
    GOAL_DIFF_DESC,      // Hiệu số bàn thắng có dấu giảm dần (+3 > +1 > 0 > -2)
    ID_ASC               // Theo ID tăng dần (chuẩn bị dữ liệu cho BinarySearch)
}
