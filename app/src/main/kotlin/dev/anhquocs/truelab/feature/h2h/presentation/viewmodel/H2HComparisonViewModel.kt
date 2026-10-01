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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class H2HComparisonViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val teamRepository: TeamRepository,
    private val matchRepository: MatchRepository,
    private val getHeadToHeadComparisonUseCase: GetHeadToHeadComparisonUseCase,
    private val calculateDynamicEloUseCase: dev.anhquocs.truelab.core.domain.team.usecase.CalculateDynamicEloUseCase = dev.anhquocs.truelab.core.domain.team.usecase.CalculateDynamicEloUseCase()
) : ViewModel() {

    private val navTeamAId: Int? = savedStateHandle.get<String>("teamAId")?.toIntOrNull()
        ?: savedStateHandle.get<Int>("teamAId")
    private val navTeamBId: Int? = savedStateHandle.get<String>("teamBId")?.toIntOrNull()
        ?: savedStateHandle.get<Int>("teamBId")

    private val _selectedTeamAId = MutableStateFlow<Int?>(navTeamAId)
    val selectedTeamAId: StateFlow<Int?> = _selectedTeamAId.asStateFlow()

    private val _selectedTeamBId = MutableStateFlow<Int?>(navTeamBId)
    val selectedTeamBId: StateFlow<Int?> = _selectedTeamBId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<TeamSummary>> = _searchQuery
        .debounce(300L)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                teamRepository.getTeams(limit = 100)
            } else {
                teamRepository.searchTeams(query = query.trim(), limit = 50)
            }
        }
        .map { list -> list.map { TeamSummary(it.id, it.name, it.logo) } }
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val selectedTeamADetail: StateFlow<TeamDetail?> = _selectedTeamAId
        .flatMapLatest { id ->
            if (id == null) flowOf(null) else teamRepository.getTeamDetail(id)
        }
        .catch { emit(null) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val selectedTeamBDetail: StateFlow<TeamDetail?> = _selectedTeamBId
        .flatMapLatest { id ->
            if (id == null) flowOf(null) else teamRepository.getTeamDetail(id)
        }
        .catch { emit(null) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val uiState: StateFlow<H2HComparisonUiState> = combine(
        _selectedTeamAId,
        _selectedTeamBId
    ) { teamAId, teamBId ->
        Pair(teamAId, teamBId)
    }.flatMapLatest { (teamAId, teamBId) ->
        when {
            teamAId == null || teamBId == null -> {
                flowOf(
                    H2HComparisonUiState.TeamSelectionRequired(
                        selectedTeamAId = teamAId,
                        selectedTeamBId = teamBId,
                        message = UiText.StringResource(R.string.h2h_select_both_teams_prompt)
                    )
                )
            }
            teamAId == teamBId -> {
                flowOf(
                    H2HComparisonUiState.TeamSelectionRequired(
                        selectedTeamAId = teamAId,
                        selectedTeamBId = null,
                        message = UiText.StringResource(R.string.h2h_error_same_team)
                    )
                )
            }
            else -> {
                combine(
                    combine(
                        teamRepository.getTeamDetail(teamAId),
                        teamRepository.getTeamDetail(teamBId),
                        matchRepository.getAllMatches()
                    ) { teamADetail, teamBDetail, allMatches ->
                        Triple(teamADetail, teamBDetail, allMatches)
                    },
                    matchRepository.getH2HMatches(teamAId, teamBId),
                    matchRepository.getRecentMatchesForTeam(teamAId, 50),
                    matchRepository.getRecentMatchesForTeam(teamBId, 50)
                ) { (teamADetail, teamBDetail, allMatches), h2hMatches, teamARecent, teamBRecent ->
                    val dynamicEloMap = calculateDynamicEloUseCase(allMatches)
                    val dynamicEloA = dynamicEloMap[teamAId] ?: teamADetail?.eloRating ?: 1500.0
                    val dynamicEloB = dynamicEloMap[teamBId] ?: teamBDetail?.eloRating ?: 1500.0

                    val resolvedTeamA = (teamADetail ?: TeamDetail(
                        id = teamAId,
                        name = "Team #$teamAId"
                    )).copy(eloRating = dynamicEloA)

                    val resolvedTeamB = (teamBDetail ?: TeamDetail(
                        id = teamBId,
                        name = "Team #$teamBId"
                    )).copy(eloRating = dynamicEloB)

                    val summary = getHeadToHeadComparisonUseCase(
                        teamA = resolvedTeamA,
                        teamB = resolvedTeamB,
                        h2hMatches = h2hMatches,
                        teamARecentMatches = teamARecent,
                        teamBRecentMatches = teamBRecent
                    )

                    H2HComparisonUiState.Success(
                        teamA = resolvedTeamA,
                        teamB = resolvedTeamB,
                        comparison = H2HUiMapper.toUiRecord(summary),
                        matchHistory = H2HUiMapper.toMatchDataRecords(summary)
                    ) as H2HComparisonUiState
                }.catch { e ->
                    emit(
                        H2HComparisonUiState.Error(
                            message = UiText.DynamicString(e.localizedMessage ?: "Unknown error loading H2H")
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

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

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
