package dev.anhquocs.truelab.feature.analytics.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.repository.MatchRepository
import dev.anhquocs.truelab.core.domain.odds.model.MatchOdds
import dev.anhquocs.truelab.core.domain.odds.model.OddsRecordItem
import dev.anhquocs.truelab.core.domain.odds.model.OddsTrendAnalysis
import dev.anhquocs.truelab.core.domain.odds.model.TargetOddsField
import dev.anhquocs.truelab.core.domain.odds.repository.OddsRepository
import dev.anhquocs.truelab.core.domain.odds.usecase.AnalyzeOddsTrendUseCase
import dev.anhquocs.truelab.core.domain.team.model.TeamDetail
import dev.anhquocs.truelab.core.domain.team.repository.TeamRepository
import dev.anhquocs.truelab.core.domain.team.usecase.GetTeamStatisticsUseCase
import dev.anhquocs.truelab.core.ui.utils.UiText
import dev.anhquocs.truelab.feature.analytics.presentation.model.AnalyticsUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val teamRepository: TeamRepository,
    private val matchRepository: MatchRepository,
    private val oddsRepository: OddsRepository,
    private val getTeamStatisticsUseCase: GetTeamStatisticsUseCase,
    private val analyzeOddsTrendUseCase: AnalyzeOddsTrendUseCase
) : ViewModel() {

    private val _selectedTeamId = MutableStateFlow<Int?>(null)
    private val _selectedMatchId = MutableStateFlow<Long?>(null)
    private val _selectedTargetField = MutableStateFlow(TargetOddsField.HOME_WIN)
    private val _selectedWindowSize = MutableStateFlow(3)
    private val _isOddsLoading = MutableStateFlow(false)

    // Dynamic Team Search
    private val _teamSearchQuery = MutableStateFlow("")
    val teamSearchQuery: StateFlow<String> = _teamSearchQuery.asStateFlow()

    val teamSearchResults: StateFlow<List<TeamDetail>> = _teamSearchQuery
        .debounce(300L)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) {
                teamRepository.getTeams(limit = MAX_TEAMS_SEARCH)
            } else {
                teamRepository.searchTeams(query = query.trim(), limit = MAX_SEARCH_RESULTS)
            }
        }
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val defaultTeamsFlow = teamRepository.getTeams(limit = MAX_TEAMS_SELECTION)
    private val defaultMatchesFlow = matchRepository.getPredictableMatches(limit = MAX_MATCHES_SELECTION)

    private val selectedTeamFlow: Flow<TeamDetail?> = combine(
        _selectedTeamId,
        defaultTeamsFlow
    ) { selectedId, defaultTeams ->
        selectedId to defaultTeams
    }.flatMapLatest { (selectedId, defaultTeams) ->
        if (selectedId != null) {
            teamRepository.getTeamDetail(selectedId).map { detail ->
                detail ?: defaultTeams.find { it.id == selectedId }
            }
        } else {
            flowOf(defaultTeams.firstOrNull())
        }
    }

    // Dynamic Match Search (scoped to selected team)
    private val _matchSearchQuery = MutableStateFlow("")
    val matchSearchQuery: StateFlow<String> = _matchSearchQuery.asStateFlow()

    val matchSearchResults: StateFlow<List<Match>> = combine(
        selectedTeamFlow,
        _matchSearchQuery.debounce(300L).distinctUntilChanged()
    ) { selectedTeam, query ->
        selectedTeam to query
    }.flatMapLatest { (selectedTeam, query) ->
        val teamId = selectedTeam?.id
        if (query.isNotBlank()) {
            // Cho phép tìm kiếm toàn cục mọi trận đấu khi người dùng gõ từ khóa
            matchRepository.searchMatches(query = query.trim(), limit = MAX_SEARCH_RESULTS)
        } else if (teamId != null) {
            matchRepository.getRecentMatchesForTeam(teamId, limit = MAX_MATCHES_SEARCH)
        } else {
            matchRepository.getPredictableMatches(limit = MAX_MATCHES_SEARCH)
        }
    }.catch { emit(emptyList()) }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private val selectedMatchFlow: Flow<Match?> = combine(
        _selectedMatchId,
        defaultMatchesFlow
    ) { selectedId, defaultMatches ->
        selectedId to defaultMatches
    }.flatMapLatest { (selectedId, defaultMatches) ->
        if (selectedId != null) {
            matchRepository.getMatchDetail(selectedId).map { detail ->
                detail ?: defaultMatches.find { it.id == selectedId }
            }
        } else {
            flowOf(defaultMatches.firstOrNull())
        }
    }

    init {
        viewModelScope.launch {
            selectedMatchFlow
                .mapNotNull { it?.id }
                .distinctUntilChanged()
                .collectLatest { matchId ->
                    _isOddsLoading.value = true
                    try {
                        oddsRepository.fetchAndCacheOddsForMatch(matchId)
                    } catch (_: Exception) {
                        // Handled gracefully; local room flow will reflect state
                    } finally {
                        _isOddsLoading.value = false
                    }
                }
        }
    }

    val uiState: StateFlow<AnalyticsUiState> = combine(
        combine(defaultTeamsFlow, defaultMatchesFlow, selectedTeamFlow) { teams, matches, selectedTeam ->
            Triple(teams, matches, selectedTeam)
        },
        combine(selectedMatchFlow, _selectedTargetField, _selectedWindowSize) { selectedMatch, targetField, windowSize ->
            Triple(selectedMatch, targetField, windowSize)
        }
    ) { (teams, matches, selectedTeam), (selectedMatch, targetField, windowSize) ->
        DataSelectionBundle(
            teams = teams,
            matches = matches,
            selectedTeam = selectedTeam,
            selectedMatch = selectedMatch,
            targetField = targetField,
            windowSize = windowSize
        )
    }.flatMapLatest { bundle ->
        val teams = bundle.teams
        val matches = bundle.matches
        val selectedTeam = bundle.selectedTeam
        val selectedMatch = bundle.selectedMatch

        if (teams.isEmpty() && matches.isEmpty() && selectedTeam == null && selectedMatch == null) {
            flowOf(AnalyticsUiState.Empty(UiText.StringResource(R.string.analytics_empty_no_data)))
        } else {
            val teamMatchesFlow = if (selectedTeam != null) {
                matchRepository.getRecentMatchesForTeam(selectedTeam.id, limit = MAX_TEAM_HISTORY_MATCHES)
            } else {
                flowOf(emptyList())
            }

            val matchId = selectedMatch?.id
            val oddsFlow = if (matchId != null) {
                combine(
                    oddsRepository.getMatchOdds(matchId),
                    oddsRepository.getOddsHistory(matchId, null, null)
                ) { matchOdds, oddsHistory ->
                    matchOdds to oddsHistory
                }
            } else {
                flowOf(null to emptyList<OddsRecordItem>())
            }

            combine(teamMatchesFlow, oddsFlow, _isOddsLoading) { teamMatches, (matchOdds, oddsHistory), isOddsLoading ->
                // 1. Descriptive Statistics via Pure Domain UseCase
                val teamStats = selectedTeam?.let { team ->
                    getTeamStatisticsUseCase(teamId = team.id, matches = teamMatches)
                }

                // 2. Odds Time-Series & Trend and Multi-Provider Summary Metrics
                val (oddsTrend, avgHome, avgDraw, avgAway, spread) = if (matchId != null && matchOdds != null) {
                    val trend = analyzeOddsTrendUseCase(
                        matchId = matchId,
                        oddsHistory = oddsHistory,
                        targetField = bundle.targetField,
                        windowSize = bundle.windowSize
                    )

                    val validHomeOdds = matchOdds.oddsList.mapNotNull { it.homeWin }.filter { it > 0.0 }
                    val validDrawOdds = matchOdds.oddsList.mapNotNull { it.draw }.filter { it > 0.0 }
                    val validAwayOdds = matchOdds.oddsList.mapNotNull { it.awayWin }.filter { it > 0.0 }

                    val avgH = if (validHomeOdds.isNotEmpty()) validHomeOdds.average() else null
                    val avgD = if (validDrawOdds.isNotEmpty()) validDrawOdds.average() else null
                    val avgA = if (validAwayOdds.isNotEmpty()) validAwayOdds.average() else null

                    val sp = if (validHomeOdds.size >= 2) {
                        (validHomeOdds.maxOrNull() ?: 0.0) - (validHomeOdds.minOrNull() ?: 0.0)
                    } else {
                        null
                    }

                    OddsAnalysisResult(trend, avgH, avgD, avgA, sp)
                } else {
                    OddsAnalysisResult(null, null, null, null, null)
                }

                AnalyticsUiState.Success(
                    selectedTeam = selectedTeam,
                    availableTeams = teams,
                    teamStats = teamStats,
                    selectedMatch = selectedMatch,
                    availableMatches = matches,
                    matchOdds = matchOdds,
                    oddsTrend = oddsTrend,
                    selectedTargetField = bundle.targetField,
                    selectedWindowSize = bundle.windowSize,
                    avgHomeOdds = avgHome,
                    avgDrawOdds = avgDraw,
                    avgAwayOdds = avgAway,
                    oddsSpread = spread,
                    isOddsLoading = isOddsLoading
                )
            }
        }
    }.catch { e ->
        emit(
            AnalyticsUiState.Error(
                if (e.message.isNullOrBlank()) {
                    UiText.StringResource(R.string.analytics_error_default)
                } else {
                    UiText.DynamicString(e.message ?: "")
                }
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AnalyticsUiState.Loading
    )

    fun onTeamSearchQueryChanged(query: String) {
        _teamSearchQuery.value = query
    }

    fun onMatchSearchQueryChanged(query: String) {
        _matchSearchQuery.value = query
    }

    fun onTeamSelected(teamId: Int) {
        _selectedTeamId.value = teamId
        viewModelScope.launch {
            matchRepository.getRecentMatchesForTeam(teamId, limit = 1)
                .firstOrNull()
                ?.firstOrNull()
                ?.let { latestMatch ->
                    _selectedMatchId.value = latestMatch.id
                }
        }
    }

    fun onMatchSelected(matchId: Long) {
        _selectedMatchId.value = matchId
        viewModelScope.launch {
            matchRepository.getMatchDetail(matchId)
                .firstOrNull()
                ?.let { match ->
                    val currentTeamId = _selectedTeamId.value
                    val isInMatch = currentTeamId == match.homeTeam.id || currentTeamId == match.awayTeam.id
                    if (!isInMatch) {
                        _selectedTeamId.value = match.homeTeam.id
                    }
                }
        }
    }

    fun onTargetFieldSelected(targetField: TargetOddsField) {
        _selectedTargetField.value = targetField
    }

    fun onWindowSizeChanged(windowSize: Int) {
        if (windowSize > 0) {
            _selectedWindowSize.value = windowSize
        }
    }

    private data class OddsAnalysisResult(
        val oddsTrend: OddsTrendAnalysis?,
        val avgHomeOdds: Double?,
        val avgDrawOdds: Double?,
        val avgAwayOdds: Double?,
        val oddsSpread: Double?
    )

    private data class DataSelectionBundle(
        val teams: List<TeamDetail>,
        val matches: List<Match>,
        val selectedTeam: TeamDetail?,
        val selectedMatch: Match?,
        val targetField: TargetOddsField,
        val windowSize: Int
    )

    companion object {
        private const val MAX_TEAMS_SELECTION = 50
        private const val MAX_MATCHES_SELECTION = 50
        private const val MAX_TEAMS_SEARCH = 100
        private const val MAX_MATCHES_SEARCH = 100
        private const val MAX_SEARCH_RESULTS = 50
        private const val MAX_TEAM_HISTORY_MATCHES = 50
    }
}
