package dev.anhquocs.truelab.feature.h2h.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.match.model.TeamSummary
import dev.anhquocs.truelab.core.domain.match.repository.MatchRepository
import dev.anhquocs.truelab.core.domain.team.model.TeamDetail
import dev.anhquocs.truelab.core.domain.team.repository.TeamRepository
import dev.anhquocs.truelab.core.domain.team.usecase.GetHeadToHeadComparisonUseCase
import dev.anhquocs.truelab.core.ui.utils.UiText
import dev.anhquocs.truelab.feature.h2h.presentation.mapper.H2HUiMapper
import dev.anhquocs.truelab.feature.h2h.presentation.model.H2HComparisonUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class H2HComparisonViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val teamRepository: TeamRepository,
    private val matchRepository: MatchRepository,
    private val getHeadToHeadComparisonUseCase: GetHeadToHeadComparisonUseCase
) : ViewModel() {

    private val navTeamAId: Int? = savedStateHandle.get<String>("teamAId")?.toIntOrNull()
        ?: savedStateHandle.get<Int>("teamAId")
    private val navTeamBId: Int? = savedStateHandle.get<String>("teamBId")?.toIntOrNull()
        ?: savedStateHandle.get<Int>("teamBId")

    private val _selectedTeamAId = MutableStateFlow<Int?>(navTeamAId)
    val selectedTeamAId: StateFlow<Int?> = _selectedTeamAId.asStateFlow()

    private val _selectedTeamBId = MutableStateFlow<Int?>(navTeamBId)
    val selectedTeamBId: StateFlow<Int?> = _selectedTeamBId.asStateFlow()

    val uiState: StateFlow<H2HComparisonUiState> = combine(
        teamRepository.getTeams().catch { emit(emptyList()) },
        _selectedTeamAId,
        _selectedTeamBId
    ) { teams, teamAId, teamBId ->
        val availableSummaries = teams.map { TeamSummary(it.id, it.name, it.logo) }
        Triple(availableSummaries, teamAId, teamBId)
    }.flatMapLatest { (availableTeams, teamAId, teamBId) ->
        when {
            teamAId == null || teamBId == null -> {
                flowOf(
                    H2HComparisonUiState.TeamSelectionRequired(
                        availableTeams = availableTeams,
                        selectedTeamAId = teamAId,
                        selectedTeamBId = teamBId,
                        message = UiText.StringResource(R.string.h2h_select_both_teams_prompt)
                    )
                )
            }
            teamAId == teamBId -> {
                flowOf(
                    H2HComparisonUiState.TeamSelectionRequired(
                        availableTeams = availableTeams,
                        selectedTeamAId = teamAId,
                        selectedTeamBId = null,
                        message = UiText.StringResource(R.string.h2h_error_same_team)
                    )
                )
            }
            else -> {
                combine(
                    teamRepository.getTeamDetail(teamAId),
                    teamRepository.getTeamDetail(teamBId),
                    matchRepository.getH2HMatches(teamAId, teamBId),
                    matchRepository.getRecentMatchesForTeam(teamAId, 10),
                    matchRepository.getRecentMatchesForTeam(teamBId, 10)
                ) { teamADetail, teamBDetail, h2hMatches, teamARecent, teamBRecent ->
                    val resolvedTeamA = teamADetail ?: TeamDetail(
                        id = teamAId,
                        name = availableTeams.find { it.id == teamAId }?.name ?: "Team #$teamAId"
                    )
                    val resolvedTeamB = teamBDetail ?: TeamDetail(
                        id = teamBId,
                        name = availableTeams.find { it.id == teamBId }?.name ?: "Team #$teamBId"
                    )

                    val summary = getHeadToHeadComparisonUseCase(
                        teamA = resolvedTeamA,
                        teamB = resolvedTeamB,
                        h2hMatches = h2hMatches,
                        teamARecentMatches = teamARecent,
                        teamBRecentMatches = teamBRecent
                    )

                    H2HComparisonUiState.Success(
                        availableTeams = availableTeams,
                        teamA = resolvedTeamA,
                        teamB = resolvedTeamB,
                        comparison = H2HUiMapper.toUiRecord(summary),
                        matchHistory = H2HUiMapper.toMatchDataRecords(summary)
                    ) as H2HComparisonUiState
                }.catch { e ->
                    emit(
                        H2HComparisonUiState.Error(
                            message = UiText.DynamicString(e.localizedMessage ?: "Unknown error loading H2H"),
                            availableTeams = availableTeams
                        )
                    )
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = H2HComparisonUiState.Loading(
            selectedTeamAId = navTeamAId,
            selectedTeamBId = navTeamBId
        )
    )

    fun selectTeamA(teamId: Int?) {
        if (_selectedTeamBId.value == teamId && teamId != null) {
            _selectedTeamBId.value = null
        }
        _selectedTeamAId.value = teamId
    }

    fun selectTeamB(teamId: Int?) {
        if (_selectedTeamAId.value == teamId && teamId != null) {
            _selectedTeamAId.value = null
        }
        _selectedTeamBId.value = teamId
    }

    fun swapTeams() {
        val currentA = _selectedTeamAId.value
        val currentB = _selectedTeamBId.value
        _selectedTeamAId.value = currentB
        _selectedTeamBId.value = currentA
    }

    fun clearSelection() {
        _selectedTeamAId.value = null
        _selectedTeamBId.value = null
    }
}
