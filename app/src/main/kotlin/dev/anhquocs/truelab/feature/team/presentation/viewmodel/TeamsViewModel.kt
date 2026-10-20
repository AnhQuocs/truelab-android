package dev.anhquocs.truelab.feature.team.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.algorithm.evaluation.FormScore
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.repository.MatchRepository
import dev.anhquocs.truelab.core.domain.team.model.SeasonRanking
import dev.anhquocs.truelab.core.domain.team.model.StandingsSortCriteria
import dev.anhquocs.truelab.core.domain.team.model.TeamDetail
import dev.anhquocs.truelab.core.domain.team.model.TeamHomeAwaySplits
import dev.anhquocs.truelab.core.domain.team.repository.TeamRepository
import dev.anhquocs.truelab.core.domain.team.usecase.CalculateHomeAwaySplitsUseCase
import dev.anhquocs.truelab.core.domain.team.usecase.CalculateTeamFormUseCase
import dev.anhquocs.truelab.core.domain.team.usecase.SearchTeamsUseCase
import dev.anhquocs.truelab.core.domain.team.usecase.SortSeasonRankingUseCase
import dev.anhquocs.truelab.core.ui.utils.UiText
import dev.anhquocs.truelab.feature.team.presentation.mapper.TeamUiMapper
import dev.anhquocs.truelab.feature.team.presentation.model.TeamsUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class TeamsViewModel @Inject constructor(
    private val teamRepository: TeamRepository,
    private val matchRepository: MatchRepository,
    private val searchTeamsUseCase: SearchTeamsUseCase,
    private val sortSeasonRankingUseCase: SortSeasonRankingUseCase,
    private val calculateTeamFormUseCase: CalculateTeamFormUseCase,
    private val calculateHomeAwaySplitsUseCase: CalculateHomeAwaySplitsUseCase
) : ViewModel() {

    companion object {
        const val PAGE_SIZE = 20
        const val SEARCH_DEBOUNCE_MS = 300L
    }

    private val _searchQuery = MutableStateFlow("")
    private val _standingsSort = MutableStateFlow(StandingsSortCriteria.POSITION_ASC)
    private val _sortBy = MutableStateFlow("POPULAR")
    private val _page = MutableStateFlow(0)
    private val _isLoadingMore = MutableStateFlow(false)

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

    private val _teamsFlow: Flow<List<TeamDetail>> = combine(
        _debouncedQuery,
        _sortBy,
        _page
    ) { query, sort, page ->
        Triple(query, sort, (page + 1) * PAGE_SIZE)
    }.flatMapLatest { (query, sort, limit) ->
        teamRepository.getTeamsPaged(
            query = query,
            sortBy = sort,
            limit = limit,
            offset = 0
        )
    }.onEach {
        if (_isLoadingMore.value) {
            _isLoadingMore.value = false
        }
    }

    private val _teamsWithRecentDataFlow: Flow<List<TeamDataHolder>> = _teamsFlow.flatMapLatest { teams ->
        if (teams.isEmpty()) {
            flowOf(emptyList())
        } else {
            val teamFlows = teams.map { team ->
                matchRepository.getRecentMatchesForTeam(team.id, limit = 5).map { matches ->
                    val formScore = calculateTeamFormUseCase(team.id, matches, windowSize = 5)
                    val splits = calculateHomeAwaySplitsUseCase(team.id, matches)
                    TeamDataHolder(
                        team = team,
                        recentMatches = matches,
                        formScore = formScore,
                        splits = splits
                    )
                }
            }
            combine(teamFlows) { it.toList() }
        }
    }

    private data class FilterParams(
        val query: String,
        val sort: StandingsSortCriteria,
        val sortBy: String,
        val page: Int,
        val isLoadingMore: Boolean
    )

    private val _filterParams: Flow<FilterParams> = combine(
        _debouncedQuery,
        _standingsSort,
        _sortBy,
        _page,
        _isLoadingMore
    ) { query, sort, sortBy, page, isLoadingMore ->
        FilterParams(query, sort, sortBy, page, isLoadingMore)
    }

    val uiState: StateFlow<TeamsUiState> = combine(
        _teamsWithRecentDataFlow,
        teamRepository.getSeasonRankings(),
        _filterParams
    ) { teamHolders: List<TeamDataHolder>,
        rankings: List<SeasonRanking>,
        filter: FilterParams ->

        val query = filter.query
        val sort = filter.sort
        val sortBy = filter.sortBy
        val page = filter.page
        val isLoadingMore = filter.isLoadingMore

        if (teamHolders.isEmpty()) {
            if (query.isNotBlank()) {
                TeamsUiState.Empty(UiText.StringResource(R.string.teams_empty_search, query))
            } else {
                TeamsUiState.Empty(UiText.StringResource(R.string.teams_empty_no_data))
            }
        } else {
            val sortedRankings = sortSeasonRankingUseCase(rankings, sort)

            val records = if (rankings.isNotEmpty()) {
                val holdersById = teamHolders.associateBy { it.team.id }
                val matchedRankings = sortedRankings.filter { it.teamId in holdersById.keys }

                val rankedRecords = matchedRankings.mapNotNull { ranking ->
                    holdersById[ranking.teamId]?.let { holder ->
                        TeamUiMapper.toRecord(
                            team = holder.team,
                            ranking = ranking,
                            formScore = holder.formScore,
                            splits = holder.splits,
                            recentMatches = holder.recentMatches
                        )
                    }
                }

                val rankedIds = matchedRankings.map { it.teamId }.toSet()
                val unrankedRecords = teamHolders.filter { it.team.id !in rankedIds }.map { holder ->
                    TeamUiMapper.toRecord(
                        team = holder.team,
                        ranking = null,
                        formScore = holder.formScore,
                        splits = holder.splits,
                        recentMatches = holder.recentMatches
                    )
                }

                rankedRecords + unrankedRecords
            } else {
                teamHolders.map { holder ->
                    TeamUiMapper.toRecord(
                        team = holder.team,
                        ranking = null,
                        formScore = holder.formScore,
                        splits = holder.splits,
                        recentMatches = holder.recentMatches
                    )
                }
            }

            if (records.isEmpty()) {
                TeamsUiState.Empty(UiText.StringResource(R.string.teams_empty_search, query))
            } else {
                val canLoadMore = teamHolders.size >= (page + 1) * PAGE_SIZE
                TeamsUiState.Success(
                    teams = records,
                    rawTeamsCount = teamHolders.size,
                    searchQuery = query,
                    standingsSort = sort,
                    sortBy = sortBy,
                    isLoadingMore = isLoadingMore,
                    canLoadMore = canLoadMore
                )
            }
        }
    }.catch { e ->
        emit(
            TeamsUiState.Error(
                if (e.message.isNullOrBlank()) {
                    UiText.StringResource(R.string.teams_error_default)
                } else {
                    UiText.DynamicString(e.message ?: "")
                }
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TeamsUiState.Loading
    )

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
        _page.value = 0
    }

    fun onSortChanged(newSort: StandingsSortCriteria) {
        _standingsSort.value = newSort
        when (newSort) {
            StandingsSortCriteria.POSITION_ASC, StandingsSortCriteria.POINTS_DESC -> _sortBy.value = "POPULAR"
            StandingsSortCriteria.WINS_DESC -> _sortBy.value = "FORM"
            else -> _sortBy.value = "NAME"
        }
        _page.value = 0
    }

    fun onSortByChanged(newSortBy: String) {
        _sortBy.value = newSortBy
        _page.value = 0
    }

    fun loadNextPage() {
        val currentState = uiState.value
        if (currentState is TeamsUiState.Success && currentState.canLoadMore && !currentState.isLoadingMore) {
            _isLoadingMore.value = true
            _page.value = _page.value + 1
        }
    }

    private data class TeamDataHolder(
        val team: TeamDetail,
        val recentMatches: List<Match>,
        val formScore: FormScore,
        val splits: TeamHomeAwaySplits
    )
}
