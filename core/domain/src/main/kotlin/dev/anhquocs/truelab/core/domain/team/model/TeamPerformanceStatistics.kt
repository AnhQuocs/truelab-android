package dev.anhquocs.truelab.core.domain.team.model

import dev.anhquocs.truelab.core.algorithm.statistics.DescriptiveStatistics

/**
 * Đóng gói toàn bộ các chỉ số thống kê hiệu suất thi đấu của một đội bóng (Phase 3 Statistics).
 *
 * @property teamId ID của đội bóng.
 * @property matchesCount Số trận đấu thực tế hợp lệ được đưa vào tính toán.
 * @property goalsScoredStats Thống kê mô tả số bàn thắng ghi được (Mean, Median, Min, Max, Range, Variance, StdDev, Skewness).
 * @property goalsConcededStats Thống kê mô tả số bàn thua phải nhận.
 * @property totalGoalsStats Thống kê mô tả tổng số bàn thắng trong trận đấu.
 * @property goalDiffStats Thống kê mô tả hiệu số bàn thắng (bàn thắng - bàn thua).
 */
data class TeamPerformanceStatistics(
    val teamId: Int,
    val matchesCount: Int,
    val goalsScoredStats: DescriptiveStatistics,
    val goalsConcededStats: DescriptiveStatistics,
    val totalGoalsStats: DescriptiveStatistics,
    val goalDiffStats: DescriptiveStatistics
)
