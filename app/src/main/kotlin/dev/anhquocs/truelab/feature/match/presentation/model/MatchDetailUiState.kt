package dev.anhquocs.truelab.feature.match.presentation.model

import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.ui.utils.UiText

/**
 * UI State for Match Detail presentation.
 */
sealed interface MatchDetailUiState {
    data object Idle : MatchDetailUiState
    data object Loading : MatchDetailUiState
    data class Success(val match: Match) : MatchDetailUiState
    data class Empty(val message: UiText) : MatchDetailUiState
    data class Error(val message: UiText) : MatchDetailUiState
}
