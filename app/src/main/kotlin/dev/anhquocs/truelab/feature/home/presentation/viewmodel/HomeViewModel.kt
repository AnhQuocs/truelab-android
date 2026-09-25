package dev.anhquocs.truelab.feature.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.metadata.model.DatasetMetadata
import dev.anhquocs.truelab.core.domain.metadata.repository.DatasetMetadataRepository
import dev.anhquocs.truelab.core.ui.utils.UiText
import dev.anhquocs.truelab.feature.home.presentation.model.HomeUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val datasetMetadataRepository: DatasetMetadataRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = datasetMetadataRepository.getMetadata()
        .map<DatasetMetadata?, HomeUiState> { metadata ->
            if (metadata != null) {
                val formattedDate = formatTimestamp(metadata.lastSyncTimestamp)
                HomeUiState.Success(metadata = metadata, formattedLastSync = formattedDate)
            } else {
                val defaultMetadata = DatasetMetadata(
                    lastSyncTimestamp = 0L,
                    totalMatches = 0,
                    totalTeams = 0,
                    totalOddsRecords = 0,
                    totalLeagues = 0,
                    totalSeasons = 0,
                    schemaVersion = DatasetMetadata.CURRENT_SCHEMA_VERSION
                )
                HomeUiState.Success(
                    metadata = defaultMetadata,
                    formattedLastSync = "—"
                )
            }
        }.catch { error ->
            val errorMsg = error.message?.takeIf { it.isNotBlank() }
            val uiText = if (errorMsg != null) {
                UiText.DynamicString(errorMsg)
            } else {
                UiText.StringResource(R.string.matches_error_default)
            }
            emit(HomeUiState.Error(uiText))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState.Loading
        )

    private fun formatTimestamp(timestamp: Long): String {
        if (timestamp <= 0L) return "—"
        val sdf = SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
