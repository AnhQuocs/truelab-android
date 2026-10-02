package dev.anhquocs.truelab.feature.backtest.presentation.model

import dev.anhquocs.truelab.core.domain.evaluation.model.EvaluationPhase
import dev.anhquocs.truelab.core.ui.utils.UiText

/**
 * Filter options for the backtested matches timeline.
 */
enum class BacktestFilter {
    ALL,
    CORRECT_ONLY,
    INCORRECT_ONLY
}

/**
 * State definitions for the Daily Prediction Backtest & Evaluation Visualizer.
 */
sealed interface BacktestUiState {
    /** Trạng thái chờ khởi chạy đánh giá trên tập trận FT của ngày đã chọn */
    data class Idle(
        val selectedDate: String,
        val availableFtMatchesCount: Int
    ) : BacktestUiState

    /** Trạng thái đang thực thi phân tích batch với tiến độ thời gian thực */
    data class Running(
        val selectedDate: String,
        val completedMatches: Int,
        val totalMatches: Int,
        val progressPercent: Float,
        val currentMatchName: String,
        val currentPhase: EvaluationPhase
    ) : BacktestUiState

    /** Đánh giá hoàn tất thành công */
    data class Success(
        val selectedDate: String,
        val overview: BacktestOverviewUiRecord,
        val oddsCoverage: OddsCoverageUiRecord,
        val confusionMatrix: ConfusionMatrixUiRecord,
        val classMetrics: List<ClassMetricUiRecord>,
        val allMatches: List<BacktestMatchUiRecord>,
        val filteredMatches: List<BacktestMatchUiRecord>,
        val selectedFilter: BacktestFilter = BacktestFilter.ALL
    ) : BacktestUiState

    /** Ngày được chọn không có trận FT nào để đánh giá */
    data class Empty(
        val selectedDate: String,
        val message: UiText
    ) : BacktestUiState

    /** Lỗi trong quá trình xử lý */
    data class Error(
        val message: UiText
    ) : BacktestUiState
}
