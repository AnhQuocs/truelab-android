package dev.anhquocs.truelab.feature.match.presentation.model

import dev.anhquocs.truelab.core.domain.match.model.MatchSortCriteria
import dev.anhquocs.truelab.core.ui.utils.UiText

/**
 * Filter by match lifecycle status.
 */
enum class MatchStatusFilter {
    ALL,
    ENDED,
    SCHEDULED
}

/**
 * Unidirectional UI state for MatchesScreen.
 */
sealed interface MatchesUiState {
    data object Loading : MatchesUiState

    data class Success(
        val matches: List<MatchDataRecord>,
        val rawMatchesCount: Int,
        val searchQuery: String,
        val selectedSort: MatchSortCriteria,
        val selectedStatusFilter: MatchStatusFilter
    ) : MatchesUiState

    data class Empty(val message: UiText) : MatchesUiState

    data class Error(val message: UiText) : MatchesUiState
}
