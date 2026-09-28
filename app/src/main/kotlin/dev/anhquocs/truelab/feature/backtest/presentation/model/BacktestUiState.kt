package dev.anhquocs.truelab.feature.backtest.presentation.model

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
 * State definitions for the Prediction Backtest & Evaluation Visualizer.
 */
sealed interface BacktestUiState {
    data object Loading : BacktestUiState
    data object Running : BacktestUiState

    data class Success(
        val overview: BacktestOverviewUiRecord,
        val confusionMatrix: ConfusionMatrixUiRecord,
        val classMetrics: List<ClassMetricUiRecord>,
        val allMatches: List<BacktestMatchUiRecord>,
        val filteredMatches: List<BacktestMatchUiRecord>,
        val selectedFilter: BacktestFilter = BacktestFilter.ALL
    ) : BacktestUiState

    data class Empty(
        val message: UiText
    ) : BacktestUiState

    data class Error(
        val message: UiText
    ) : BacktestUiState
}
