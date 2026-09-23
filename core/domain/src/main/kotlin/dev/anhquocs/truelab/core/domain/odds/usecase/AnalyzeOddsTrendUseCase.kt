package dev.anhquocs.truelab.core.domain.odds.usecase

import dev.anhquocs.truelab.core.algorithm.statistics.DescriptiveStatisticsCalculator
import dev.anhquocs.truelab.core.algorithm.statistics.StatisticsCalculator
import dev.anhquocs.truelab.core.algorithm.trend.MovingAverageCalculator
import dev.anhquocs.truelab.core.algorithm.trend.SimpleMovingAverageCalculator
import dev.anhquocs.truelab.core.domain.odds.model.OddsRecordItem
import dev.anhquocs.truelab.core.domain.odds.model.OddsTrendAnalysis
import dev.anhquocs.truelab.core.domain.odds.model.TargetOddsField

/**
 * UseCase phân tích chuỗi biến động tỷ lệ cược (Odds Time Series Analysis) dựa trên thuật toán làm mịn
 * Simple Moving Average - SMA (Phase 4) và thống kê mô tả độ biến động Volatility (Phase 3).
 *
 * UseCase này là pure computation, độc lập hoàn toàn với Repository và Data/Room persistence.
 *
 * @param movingAverageCalculator Thuật toán làm mịn trung bình động (mặc định [SimpleMovingAverageCalculator]).
 * @param statisticsCalculator Thuật toán thống kê mô tả (mặc định [DescriptiveStatisticsCalculator]).
 */
class AnalyzeOddsTrendUseCase(
    private val movingAverageCalculator: MovingAverageCalculator = SimpleMovingAverageCalculator(),
    private val statisticsCalculator: StatisticsCalculator = DescriptiveStatisticsCalculator()
) {

    /**
     * Phân tích xu hướng biến động của một loại kèo mục tiêu từ lịch sử tỷ lệ cược.
     *
     * @param matchId ID của trận đấu cần phân tích.
     * @param oddsHistory Lịch sử các bản ghi tỷ lệ cược.
     * @param targetField Loại kèo mục tiêu cần phân tích (mặc định [TargetOddsField.HOME_WIN]).
     * @param windowSize Kích thước cửa sổ trượt SMA (mặc định 3, phải > 0).
     * @return [OddsTrendAnalysis] chứa chuỗi thời gian, chuỗi SMA, độ biến động và trạng thái dữ liệu.
     * @throws IllegalArgumentException nếu [windowSize] <= 0.
     */
    operator fun invoke(
        matchId: Long,
        oddsHistory: List<OddsRecordItem>,
        targetField: TargetOddsField = TargetOddsField.HOME_WIN,
        windowSize: Int = 3
    ): OddsTrendAnalysis {
        require(windowSize > 0) {
            "windowSize must be greater than 0, but was $windowSize"
        }

        // 1. Sắp xếp theo thứ tự thời gian tăng dần (changeTime ASC)
        val sortedHistory = oddsHistory.sortedBy { it.changeTime }

        // 2. Trích xuất dãy số thực hợp lệ theo targetField (lọc null, <= 0.0, non-finite)
        val validPairs = sortedHistory.mapNotNull { item ->
            val value = when (targetField) {
                TargetOddsField.HOME_WIN -> item.homeWin
                TargetOddsField.DRAW -> item.draw
                TargetOddsField.AWAY_WIN -> item.awayWin
                TargetOddsField.OVER -> item.over
                TargetOddsField.UNDER -> item.under
                TargetOddsField.HANDICAP -> item.handicap
            }
            if (value != null && value.isFinite() && value > 0.0) {
                Pair(item.changeTime, value)
            } else {
                null
            }
        }

        val timestamps = validPairs.map { it.first }
        val rawSeries = validPairs.map { it.second }

        // 3. Tính toán chuỗi SMA (Phase 4 MovingAverageCalculator)
        val smaSeries = if (rawSeries.size >= windowSize) {
            movingAverageCalculator.calculate(rawSeries, windowSize)
        } else {
            emptyList()
        }

        // 4. Tính toán Volatility (Sample Standard Deviation - Phase 3 StatisticsCalculator)
        val volatility = if (rawSeries.size >= 2) {
            val stdDev = statisticsCalculator.sampleStandardDeviation(rawSeries)
            if (stdDev.isNaN() || !stdDev.isFinite()) 0.0 else stdDev
        } else {
            0.0
        }

        val openingOdds = rawSeries.firstOrNull()
        val currentOdds = rawSeries.lastOrNull()

        return OddsTrendAnalysis(
            matchId = matchId,
            targetField = targetField,
            windowSize = windowSize,
            timestamps = timestamps,
            rawOddsSeries = rawSeries,
            smaSeries = smaSeries,
            currentOdds = currentOdds,
            openingOdds = openingOdds,
            volatility = volatility,
            hasSufficientData = smaSeries.isNotEmpty()
        )
    }
}
