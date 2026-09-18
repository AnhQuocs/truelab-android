package dev.anhquocs.truelab.core.domain.team.repository

import dev.anhquocs.truelab.core.domain.team.model.SeasonRanking
import dev.anhquocs.truelab.core.domain.team.model.TeamDetail
import kotlinx.coroutines.flow.Flow

interface TeamRepository {
    fun getTeamDetail(teamId: Int): Flow<TeamDetail?>
    fun getSeasonRanking(matchId: Long): Flow<List<SeasonRanking>>
}
