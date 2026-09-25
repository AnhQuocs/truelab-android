package dev.anhquocs.truelab.feature.team.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.match.model.TeamSummary
import dev.anhquocs.truelab.core.domain.match.repository.MatchRepository
import dev.anhquocs.truelab.core.domain.team.model.StandingsSortCriteria
import dev.anhquocs.truelab.core.domain.team.repository.TeamRepository
import dev.anhquocs.truelab.core.domain.team.usecase.CalculateHomeAwaySplitsUseCase
import dev.anhquocs.truelab.core.domain.team.usecase.CalculateTeamFormUseCase
import dev.anhquocs.truelab.core.domain.team.usecase.SearchTeamsUseCase
import dev.anhquocs.truelab.core.domain.team.usecase.SortSeasonRankingUseCase
import dev.anhquocs.truelab.core.ui.utils.UiText
import dev.anhquocs.truelab.feature.team.presentation.mapper.TeamUiMapper
import dev.anhquocs.truelab.feature.team.presentation.model.TeamsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

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

    val uiState: StateFlow<TeamsUiState> = combine(
        teamRepository.getTeams(),
        teamRepository.getSeasonRankings(),
        matchRepository.getMatches(""),
        _searchQuery,
        _standingsSort
    ) { teams, rankings, allMatches, query, sort ->
        if (teams.isEmpty()) {
            TeamsUiState.Empty(UiText.StringResource(R.string.teams_empty_no_data))
        } else {
            // 1. Sắp xếp SeasonRanking bằng Pure Domain UseCase (MergeSort composite tie-breakers)
            val sortedRankings = sortSeasonRankingUseCase(rankings, sort)
            val rankingMap = sortedRankings.associateBy { it.teamId }

            // 2. Tìm kiếm đội bóng bằng Pure Domain UseCase (LinearSearch)
            val teamSummaries = teams.map { TeamSummary(it.id, it.name, it.logo) }
            val matchedSummaries = searchTeamsUseCase.searchByName(teamSummaries, query)
            val matchedIds = matchedSummaries.map { it.id }.toSet()

            // 3. Lọc danh sách đội theo kết quả tìm kiếm
            val filteredTeams = teams.filter { it.id in matchedIds }

            // 4. Ánh xạ sang UI records có thứ tự theo Standings hoặc giữ nguyên nếu unranked
            val records = if (rankings.isNotEmpty()) {
                val matchedRankings = sortedRankings.filter { it.teamId in matchedIds }
                val teamsById = filteredTeams.associateBy { it.id }

                val rankedRecords = matchedRankings.mapNotNull { ranking ->
                    teamsById[ranking.teamId]?.let { team ->
                        val formScore = calculateTeamFormUseCase(team.id, allMatches, windowSize = 5)
                        val splits = calculateHomeAwaySplitsUseCase(team.id, allMatches)
                        TeamUiMapper.toRecord(team, ranking, formScore, splits)
                    }
                }

                val rankedIds = matchedRankings.map { it.teamId }.toSet()
                val unrankedRecords = filteredTeams.filter { it.id !in rankedIds }.map { team ->
                    val formScore = calculateTeamFormUseCase(team.id, allMatches, windowSize = 5)
                    val splits = calculateHomeAwaySplitsUseCase(team.id, allMatches)
                    TeamUiMapper.toRecord(team, null, formScore, splits)
                }

                rankedRecords + unrankedRecords
            } else {
                filteredTeams.map { team ->
                    val formScore = calculateTeamFormUseCase(team.id, allMatches, windowSize = 5)
                    val splits = calculateHomeAwaySplitsUseCase(team.id, allMatches)
                    TeamUiMapper.toRecord(team, null, formScore, splits)
                }
            }

            if (records.isEmpty()) {
                TeamsUiState.Empty(UiText.StringResource(R.string.teams_empty_search, query))
            } else {
                TeamsUiState.Success(
                    teams = records,
                    rawTeamsCount = teams.size,
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
}
