package dev.anhquocs.truelab.feature.analytics.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.repository.MatchRepository
import dev.anhquocs.truelab.core.domain.odds.model.TargetOddsField
import dev.anhquocs.truelab.core.domain.odds.repository.OddsRepository
import dev.anhquocs.truelab.core.domain.odds.usecase.AnalyzeOddsTrendUseCase
import dev.anhquocs.truelab.core.domain.team.model.TeamDetail
import dev.anhquocs.truelab.core.domain.team.repository.TeamRepository
import dev.anhquocs.truelab.core.domain.team.usecase.GetTeamStatisticsUseCase
import dev.anhquocs.truelab.core.ui.utils.UiText
import dev.anhquocs.truelab.feature.analytics.presentation.model.AnalyticsUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
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

    private val _selectionFlow: Flow<UserSelection> = combine(
        _selectedTeamId,
        _selectedMatchId,
        _selectedTargetField,
        _selectedWindowSize
    ) { teamId, matchId, targetField, windowSize ->
        UserSelection(teamId, matchId, targetField, windowSize)
    }

    val uiState: StateFlow<AnalyticsUiState> = combine(
        teamRepository.getTeams(),
        matchRepository.getMatches(""),
        _selectionFlow
    ) { teams, matches, selection ->
        DataSelectionBundle(
            teams = teams,
            matches = matches,
            userTeamId = selection.teamId,
            userMatchId = selection.matchId,
            targetField = selection.targetField,
            windowSize = selection.windowSize
        )
    }.flatMapLatest { bundle ->
        val teams = bundle.teams
        val matches = bundle.matches

        if (teams.isEmpty() && matches.isEmpty()) {
            flowOf(AnalyticsUiState.Empty(UiText.StringResource(R.string.analytics_empty_no_data)))
        } else {
            val selectedTeam = bundle.userTeamId?.let { id -> teams.find { it.id == id } }
                ?: teams.firstOrNull()
            val selectedMatch = bundle.userMatchId?.let { id -> matches.find { it.id == id } }
                ?: matches.firstOrNull()

            // 1. Descriptive Statistics via Pure Domain UseCase
            val teamStats = selectedTeam?.let { team ->
                getTeamStatisticsUseCase(teamId = team.id, matches = matches)
            }

            val matchId = selectedMatch?.id
            if (matchId != null) {
                combine(
                    oddsRepository.getMatchOdds(matchId),
                    oddsRepository.getOddsHistory(matchId, null, null)
                ) { matchOdds, oddsHistory ->
                    // 2. Odds Time-Series & Trend via Pure Domain UseCase
                    val oddsTrend = analyzeOddsTrendUseCase(
                        matchId = matchId,
                        oddsHistory = oddsHistory,
                        targetField = bundle.targetField,
                        windowSize = bundle.windowSize
                    )

                    // 3. Multi-Provider Summary Metrics computed strictly from real MatchOdds
                    val validHomeOdds = matchOdds.oddsList.mapNotNull { it.homeWin }.filter { it > 0.0 }
                    val validDrawOdds = matchOdds.oddsList.mapNotNull { it.draw }.filter { it > 0.0 }
                    val validAwayOdds = matchOdds.oddsList.mapNotNull { it.awayWin }.filter { it > 0.0 }

                    val avgHome = if (validHomeOdds.isNotEmpty()) validHomeOdds.average() else null
                    val avgDraw = if (validDrawOdds.isNotEmpty()) validDrawOdds.average() else null
                    val avgAway = if (validAwayOdds.isNotEmpty()) validAwayOdds.average() else null

                    val spread = if (validHomeOdds.size >= 2) {
                        (validHomeOdds.maxOrNull() ?: 0.0) - (validHomeOdds.minOrNull() ?: 0.0)
                    } else {
                        null
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
                        oddsSpread = spread
                    )
                }
            } else {
                flowOf(
                    AnalyticsUiState.Success(
                        selectedTeam = selectedTeam,
                        availableTeams = teams,
                        teamStats = teamStats,
                        selectedMatch = null,
                        availableMatches = matches,
                        matchOdds = null,
                        oddsTrend = null,
                        selectedTargetField = bundle.targetField,
                        selectedWindowSize = bundle.windowSize
                    )
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

    fun onTeamSelected(teamId: Int) {
        _selectedTeamId.value = teamId
    }

    fun onMatchSelected(matchId: Long) {
        _selectedMatchId.value = matchId
    }

    fun onTargetFieldSelected(targetField: TargetOddsField) {
        _selectedTargetField.value = targetField
    }

    fun onWindowSizeChanged(windowSize: Int) {
        if (windowSize > 0) {
            _selectedWindowSize.value = windowSize
        }
    }

    private data class UserSelection(
        val teamId: Int?,
        val matchId: Long?,
        val targetField: TargetOddsField,
        val windowSize: Int
    )

    private data class DataSelectionBundle(
        val teams: List<TeamDetail>,
        val matches: List<Match>,
        val userTeamId: Int?,
        val userMatchId: Long?,
        val targetField: TargetOddsField,
        val windowSize: Int
    )
}
