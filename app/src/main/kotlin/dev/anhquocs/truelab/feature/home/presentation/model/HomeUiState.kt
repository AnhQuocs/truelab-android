package dev.anhquocs.truelab.feature.home.presentation.model

import dev.anhquocs.truelab.core.domain.metadata.model.DatasetMetadata
import dev.anhquocs.truelab.core.ui.utils.UiText

/**
 * Unidirectional UI state for HomeScreen and DatasetOverviewCard.
 */
sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Success(
        val metadata: DatasetMetadata,
        val formattedLastSync: String
    ) : HomeUiState

    data class Error(val message: UiText) : HomeUiState
}
