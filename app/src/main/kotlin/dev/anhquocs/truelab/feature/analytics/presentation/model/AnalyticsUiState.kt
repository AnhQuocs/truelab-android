package dev.anhquocs.truelab.feature.analytics.presentation.model

import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.odds.model.MatchOdds
import dev.anhquocs.truelab.core.domain.odds.model.OddsTrendAnalysis
import dev.anhquocs.truelab.core.domain.odds.model.TargetOddsField
import dev.anhquocs.truelab.core.domain.team.model.TeamDetail
import dev.anhquocs.truelab.core.domain.team.model.TeamPerformanceStatistics
import dev.anhquocs.truelab.core.ui.utils.UiText

sealed interface AnalyticsUiState {
    data object Loading : AnalyticsUiState

    data class Success(
        val selectedTeam: TeamDetail?,
        val availableTeams: List<TeamDetail>,
        val teamStats: TeamPerformanceStatistics?,
        val selectedMatch: Match?,
        val availableMatches: List<Match>,
        val matchOdds: MatchOdds?,
        val oddsTrend: OddsTrendAnalysis?,
        val selectedTargetField: TargetOddsField = TargetOddsField.HOME_WIN,
        val selectedWindowSize: Int = 3,
        val avgHomeOdds: Double? = null,
        val avgDrawOdds: Double? = null,
        val avgAwayOdds: Double? = null,
        val oddsSpread: Double? = null
    ) : AnalyticsUiState

    data class Empty(val message: UiText) : AnalyticsUiState

    data class Error(val message: UiText) : AnalyticsUiState
}
