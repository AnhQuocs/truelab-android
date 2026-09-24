package dev.anhquocs.truelab.feature.match.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anhquocs.truelab.core.domain.match.model.MatchSortCriteria
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.domain.match.repository.MatchRepository
import dev.anhquocs.truelab.core.domain.match.usecase.SearchMatchesUseCase
import dev.anhquocs.truelab.core.domain.match.usecase.SortMatchesUseCase
import dev.anhquocs.truelab.feature.match.presentation.mapper.toUiRecord
import dev.anhquocs.truelab.feature.match.presentation.model.MatchStatusFilter
import dev.anhquocs.truelab.feature.match.presentation.model.MatchesUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel managing Unidirectional Data Flow for MatchesScreen.
 *
 * Pipeline:
 * Raw Matches -> Status Filter -> SearchMatchesUseCase -> SortMatchesUseCase -> MatchUiMapper -> UI State
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MatchesViewModel @Inject constructor(
    private val matchRepository: MatchRepository,
    private val searchMatchesUseCase: SearchMatchesUseCase,
    private val sortMatchesUseCase: SortMatchesUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortCriteria = MutableStateFlow(MatchSortCriteria.START_TIME_DESC)
    val sortCriteria: StateFlow<MatchSortCriteria> = _sortCriteria.asStateFlow()

    private val _statusFilter = MutableStateFlow(MatchStatusFilter.ALL)
    val statusFilter: StateFlow<MatchStatusFilter> = _statusFilter.asStateFlow()

    private val _selectedDate = MutableStateFlow("")
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    val uiState: StateFlow<MatchesUiState> = combine(
        _selectedDate.flatMapLatest { date -> matchRepository.getMatches(date) },
        _searchQuery,
        _sortCriteria,
        _statusFilter
    ) { rawMatches, query, sort, filter ->
        if (rawMatches.isEmpty()) {
            MatchesUiState.Empty("Không có trận đấu nào trong cơ sở dữ liệu")
        } else {
            // Pipeline Step 1: Status Filter
            val statusFiltered = when (filter) {
                MatchStatusFilter.ALL -> rawMatches
                MatchStatusFilter.ENDED -> rawMatches.filter { it.isEnded }
                MatchStatusFilter.SCHEDULED -> rawMatches.filter { it.status == MatchStatus.SCHEDULED }
            }

            if (statusFiltered.isEmpty()) {
                MatchesUiState.Empty("Không tìm thấy trận đấu nào phù hợp với bộ lọc")
            } else {
                // Pipeline Step 2: Search via SearchMatchesUseCase (LinearSearch O(n))
                val searched = searchMatchesUseCase(statusFiltered, query)

                if (searched.isEmpty()) {
                    MatchesUiState.Empty("Không tìm thấy trận đấu nào khớp với từ khóa \"$query\"")
                } else {
                    // Pipeline Step 3: Sort via SortMatchesUseCase (MergeSort O(n log n))
                    val sorted = sortMatchesUseCase(searched, sort)

                    // Pipeline Step 4: Map to UI Presentation Model
                    MatchesUiState.Success(
                        matches = sorted.map { it.toUiRecord() },
                        rawMatchesCount = rawMatches.size,
                        searchQuery = query,
                        selectedSort = sort,
                        selectedStatusFilter = filter
                    )
                }
            }
        }
    }.catch { error ->
        emit(MatchesUiState.Error(error.message ?: "Đã xảy ra lỗi khi tải dữ liệu trận đấu"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MatchesUiState.Loading
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onSortCriteriaChanged(criteria: MatchSortCriteria) {
        _sortCriteria.value = criteria
    }

    fun onStatusFilterChanged(filter: MatchStatusFilter) {
        _statusFilter.value = filter
    }

    fun onDateChanged(date: String) {
        _selectedDate.value = date
    }
}
