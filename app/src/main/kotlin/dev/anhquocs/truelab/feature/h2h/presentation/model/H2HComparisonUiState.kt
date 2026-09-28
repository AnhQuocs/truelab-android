package dev.anhquocs.truelab.feature.h2h.presentation.model

import dev.anhquocs.truelab.core.domain.match.model.TeamSummary
import dev.anhquocs.truelab.core.domain.team.model.TeamDetail
import dev.anhquocs.truelab.core.ui.utils.UiText
import dev.anhquocs.truelab.feature.match.presentation.model.MatchDataRecord

sealed interface H2HComparisonUiState {
    data class Loading(
        val availableTeams: List<TeamSummary> = emptyList(),
        val selectedTeamAId: Int? = null,
        val selectedTeamBId: Int? = null
    ) : H2HComparisonUiState

    data class TeamSelectionRequired(
        val availableTeams: List<TeamSummary>,
        val selectedTeamAId: Int? = null,
        val selectedTeamBId: Int? = null,
        val message: UiText
    ) : H2HComparisonUiState

    data class Success(
        val availableTeams: List<TeamSummary>,
        val teamA: TeamDetail,
        val teamB: TeamDetail,
        val comparison: H2HComparisonRecord,
        val matchHistory: List<MatchDataRecord>
    ) : H2HComparisonUiState

    data class Error(
        val message: UiText,
        val availableTeams: List<TeamSummary> = emptyList()
    ) : H2HComparisonUiState
}
