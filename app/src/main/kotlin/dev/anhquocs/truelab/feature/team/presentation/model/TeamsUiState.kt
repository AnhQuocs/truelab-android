package dev.anhquocs.truelab.feature.team.presentation.model

import dev.anhquocs.truelab.core.domain.team.model.StandingsSortCriteria
import dev.anhquocs.truelab.core.ui.utils.UiText

sealed interface TeamsUiState {
    data object Loading : TeamsUiState

    data class Success(
        val teams: List<TeamAnalyticsRecord>,
        val rawTeamsCount: Int,
        val searchQuery: String,
        val standingsSort: StandingsSortCriteria
    ) : TeamsUiState

    data class Empty(val message: UiText) : TeamsUiState

    data class Error(val message: UiText) : TeamsUiState
}
