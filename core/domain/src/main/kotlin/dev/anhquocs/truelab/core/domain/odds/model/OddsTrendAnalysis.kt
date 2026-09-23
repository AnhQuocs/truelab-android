package dev.anhquocs.truelab.core.domain.odds.model

/**
 * Các loại tỷ lệ cược mục tiêu có thể phân tích xu hướng biến động.
 */
enum class TargetOddsField {
    HOME_WIN,
    DRAW,
    AWAY_WIN,
    OVER,
    UNDER,
    HANDICAP
}

/**
 * Đóng gói kết quả phân tích xu hướng biến động tỷ lệ cược (Odds Time Series Analysis).
 *
 * @property matchId ID trận đấu.
 * @property targetField Loại tỷ lệ cược được phân tích.
 * @property windowSize Kích thước cửa sổ trượt SMA.
 * @property timestamps Danh sách mốc thời gian [changeTime] tương ứng với các điểm cược hợp lệ (đã sắp xếp tăng dần).
 * @property rawOddsSeries Chuỗi tỷ lệ cược gốc hợp lệ theo thứ tự thời gian.
 * @property smaSeries Chuỗi giá trị trung bình động (Simple Moving Average - Phase 4) tương ứng.
 * @property currentOdds Tỷ lệ cược mới nhất (ở cuối chuỗi thời gian), null nếu không có dữ liệu.
 * @property openingOdds Tỷ lệ cược mở kèo (ở đầu chuỗi thời gian), null nếu không có dữ liệu.
 * @property volatility Độ biến động tỷ lệ cược (Sample Standard Deviation - Phase 3), 0.0 nếu N < 2.
 * @property hasSufficientData True nếu số điểm dữ liệu đủ để tính toán ít nhất 1 giá trị SMA (N >= windowSize).
 */
data class OddsTrendAnalysis(
    val matchId: Long,
    val targetField: TargetOddsField,
    val windowSize: Int,
    val timestamps: List<Long>,
    val rawOddsSeries: List<Double>,
    val smaSeries: List<Double>,
    val currentOdds: Double?,
    val openingOdds: Double?,
    val volatility: Double,
    val hasSufficientData: Boolean
)
