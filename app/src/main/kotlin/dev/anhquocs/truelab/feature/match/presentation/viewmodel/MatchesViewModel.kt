package dev.anhquocs.truelab.feature.match.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.league.model.League
import dev.anhquocs.truelab.core.domain.league.repository.LeagueRepository
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchSortCriteria
import dev.anhquocs.truelab.core.domain.match.repository.MatchRepository
import dev.anhquocs.truelab.core.domain.match.usecase.SearchMatchesUseCase
import dev.anhquocs.truelab.core.domain.match.usecase.SortMatchesUseCase
import dev.anhquocs.truelab.core.ui.utils.UiText
import dev.anhquocs.truelab.feature.match.presentation.mapper.toUiRecord
import dev.anhquocs.truelab.feature.match.presentation.model.MatchDetailUiState
import dev.anhquocs.truelab.feature.match.presentation.model.MatchStatusFilter
import dev.anhquocs.truelab.feature.match.presentation.model.MatchesUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel managing Unidirectional Data Flow for MatchesScreen with progressive pagination.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class MatchesViewModel @Inject constructor(
    private val matchRepository: MatchRepository,
    private val leagueRepository: LeagueRepository,
    private val searchMatchesUseCase: SearchMatchesUseCase,
    private val sortMatchesUseCase: SortMatchesUseCase
) : ViewModel() {

    companion object {
        const val PAGE_SIZE = 20
        const val SEARCH_DEBOUNCE_MS = 300L
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortCriteria = MutableStateFlow(MatchSortCriteria.START_TIME_DESC)
    val sortCriteria: StateFlow<MatchSortCriteria> = _sortCriteria.asStateFlow()

    private val _statusFilter = MutableStateFlow(MatchStatusFilter.ALL)
    val statusFilter: StateFlow<MatchStatusFilter> = _statusFilter.asStateFlow()

    private val _selectedLeagueId = MutableStateFlow<Int?>(null)
    val selectedLeagueId: StateFlow<Int?> = _selectedLeagueId.asStateFlow()

    private val _selectedSeason = MutableStateFlow<String?>(null)
    val selectedSeason: StateFlow<String?> = _selectedSeason.asStateFlow()

    private val _selectedDate = MutableStateFlow("")
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _page = MutableStateFlow(0)
    private val _isLoadingMore = MutableStateFlow(false)

    private val _matchDetailState = MutableStateFlow<MatchDetailUiState>(MatchDetailUiState.Idle)
    val matchDetailState: StateFlow<MatchDetailUiState> = _matchDetailState.asStateFlow()

    private var matchDetailJob: Job? = null

    val leagues: StateFlow<List<League>> = leagueRepository.getLeagues()
        .map { list ->
            list.filter { league ->
                league.name.isNotBlank() && !league.name.matches(Regex("^[a-z0-9]{15,}$"))
            }
        }
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _debouncedQuery: Flow<String> = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            flowOf(query)
        } else {
            flow {
                delay(SEARCH_DEBOUNCE_MS)
                emit(query)
            }
        }
    }.distinctUntilChanged()

    private data class MatchFilterParams(
        val query: String,
        val leagueId: Int?,
        val status: MatchStatusFilter,
        val sort: MatchSortCriteria,
        val page: Int
    )

    private val _filterParams: Flow<MatchFilterParams> = combine(
        _debouncedQuery,
        _selectedLeagueId,
        _statusFilter,
        _sortCriteria,
        _page
    ) { query, leagueId, status, sort, page ->
        MatchFilterParams(query, leagueId, status, sort, page)
    }

    private val _matchesFlow: Flow<List<Match>> = _filterParams.flatMapLatest { params ->
        val statusStr = when (params.status) {
            MatchStatusFilter.ALL -> "ALL"
            MatchStatusFilter.ENDED -> "ENDED"
            MatchStatusFilter.SCHEDULED -> "SCHEDULED"
        }
        val sortStr = when (params.sort) {
            MatchSortCriteria.START_TIME_DESC -> "START_TIME_DESC"
            MatchSortCriteria.TOTAL_GOALS_DESC -> "TOTAL_GOALS_DESC"
            MatchSortCriteria.ID_ASC -> "ID_ASC"
            else -> "START_TIME_DESC"
        }
        matchRepository.getMatchesPaged(
            query = params.query.takeIf { it.isNotBlank() },
            leagueId = params.leagueId,
            statusFilter = statusStr,
            sortBy = sortStr,
            limit = (params.page + 1) * PAGE_SIZE,
            offset = 0
        )
    }.onEach {
        if (_isLoadingMore.value) {
            _isLoadingMore.value = false
        }
    }

    val uiState: StateFlow<MatchesUiState> = combine(
        _matchesFlow,
        _filterParams,
        leagues,
        _isLoadingMore
    ) { matches, params, leaguesList, isLoadingMore ->
        if (matches.isEmpty()) {
            if (params.query.isNotBlank()) {
                MatchesUiState.Empty(
                    message = UiText.StringResource(R.string.matches_empty_search, params.query),
                    leagues = leaguesList,
                    selectedLeagueId = params.leagueId,
                    selectedSeason = _selectedSeason.value
                )
            } else if (params.leagueId != null || params.status != MatchStatusFilter.ALL) {
                MatchesUiState.Empty(
                    message = UiText.StringResource(R.string.matches_empty_filter),
                    leagues = leaguesList,
                    selectedLeagueId = params.leagueId,
                    selectedSeason = _selectedSeason.value
                )
            } else {
                MatchesUiState.Empty(
                    message = UiText.StringResource(R.string.matches_empty_no_data),
                    leagues = leaguesList,
                    selectedLeagueId = params.leagueId,
                    selectedSeason = _selectedSeason.value
                )
            }
        } else {
            val canLoadMore = matches.size >= (params.page + 1) * PAGE_SIZE
            MatchesUiState.Success(
                matches = matches.map { it.toUiRecord() },
                rawMatchesCount = matches.size,
                searchQuery = params.query,
                selectedSort = params.sort,
                selectedStatusFilter = params.status,
                leagues = leaguesList,
                selectedLeagueId = params.leagueId,
                selectedSeason = _selectedSeason.value,
                isLoadingMore = isLoadingMore,
                canLoadMore = canLoadMore
            )
        }
    }.catch { error ->
        val errorUiText = error.message?.let { UiText.DynamicString(it) }
            ?: UiText.StringResource(R.string.matches_error_default)
        emit(MatchesUiState.Error(errorUiText))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MatchesUiState.Loading
    )

    fun loadNextPage() {
        if (!_isLoadingMore.value) {
            val currentState = uiState.value
            if (currentState is MatchesUiState.Success && currentState.canLoadMore) {
                _isLoadingMore.value = true
                _page.value += 1
            }
        }
    }

    fun onLeagueSelected(leagueId: Int?) {
        _selectedLeagueId.value = leagueId
        _page.value = 0
    }

    fun onSeasonSelected(season: String?) {
        _selectedSeason.value = season
        _page.value = 0
    }

    fun onClearFilters() {
        _selectedLeagueId.value = null
        _selectedSeason.value = null
        _statusFilter.value = MatchStatusFilter.ALL
        _searchQuery.value = ""
        _page.value = 0
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _page.value = 0
    }

    fun onSortCriteriaChanged(criteria: MatchSortCriteria) {
        _sortCriteria.value = criteria
        _page.value = 0
    }

    fun onStatusFilterChanged(filter: MatchStatusFilter) {
        _statusFilter.value = filter
        _page.value = 0
    }

    fun onDateChanged(date: String) {
        _selectedDate.value = date
        _page.value = 0
    }

    fun onMatchClicked(matchId: Long) {
        matchDetailJob?.cancel()
        _matchDetailState.value = MatchDetailUiState.Loading
        matchDetailJob = viewModelScope.launch {
            matchRepository.getMatchDetail(matchId)
                .catch { error ->
                    val errorUiText = error.message?.let { UiText.DynamicString(it) }
                        ?: UiText.StringResource(R.string.match_detail_error_default)
                    _matchDetailState.value = MatchDetailUiState.Error(errorUiText)
                }
                .collect { match ->
                    _matchDetailState.value = if (match != null) {
                        MatchDetailUiState.Success(match)
                    } else {
                        MatchDetailUiState.Empty(UiText.StringResource(R.string.match_detail_empty))
                    }
                }
        }
    }

    fun onDismissMatchDetail() {
        matchDetailJob?.cancel()
        _matchDetailState.value = MatchDetailUiState.Idle
    }
}
