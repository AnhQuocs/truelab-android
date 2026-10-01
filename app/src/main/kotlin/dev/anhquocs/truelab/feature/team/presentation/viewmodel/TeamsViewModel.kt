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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TeamsViewModel @Inject constructor(
    private val teamRepository: TeamRepository,
    private val matchRepository: MatchRepository,
    private val searchTeamsUseCase: SearchTeamsUseCase,
    private val sortSeasonRankingUseCase: SortSeasonRankingUseCase,
    private val calculateTeamFormUseCase: CalculateTeamFormUseCase,
    private val calculateHomeAwaySplitsUseCase: CalculateHomeAwaySplitsUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _standingsSort = MutableStateFlow(StandingsSortCriteria.POSITION_ASC)

    private val _teamsFlow: Flow<List<TeamDetail>> = _searchQuery.flatMapLatest { query ->
        teamRepository.searchTeams(query = query, limit = 50)
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

    val uiState: StateFlow<TeamsUiState> = combine(
        _teamsWithRecentDataFlow,
        teamRepository.getSeasonRankings(),
        _searchQuery,
        _standingsSort
    ) { teamHolders: List<TeamDataHolder>, rankings: List<SeasonRanking>, query: String, sort: StandingsSortCriteria ->
        if (teamHolders.isEmpty()) {
            if (query.isNotBlank()) {
                TeamsUiState.Empty(UiText.StringResource(R.string.teams_empty_search, query))
            } else {
                TeamsUiState.Empty(UiText.StringResource(R.string.teams_empty_no_data))
            }
        } else {
            // 1. Sắp xếp SeasonRanking bằng Pure Domain UseCase (MergeSort composite tie-breakers)
            val sortedRankings = sortSeasonRankingUseCase(rankings, sort)

            // 2. Ánh xạ danh sách teams đã được bounded từ DAO sang UI records kèm phong độ và Home/Away splits
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
                TeamsUiState.Success(
                    teams = records,
                    rawTeamsCount = teamHolders.size,
                    searchQuery = query,
                    standingsSort = sort
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
    }

    fun onSortChanged(newSort: StandingsSortCriteria) {
        _standingsSort.value = newSort
    }

    private data class TeamDataHolder(
        val team: TeamDetail,
        val recentMatches: List<Match>,
        val formScore: FormScore,
        val splits: TeamHomeAwaySplits
    )
}

