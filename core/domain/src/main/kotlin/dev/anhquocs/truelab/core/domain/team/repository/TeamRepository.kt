package dev.anhquocs.truelab.core.domain.team.repository

import dev.anhquocs.truelab.core.domain.team.model.SeasonRanking
import dev.anhquocs.truelab.core.domain.team.model.TeamDetail
import kotlinx.coroutines.flow.Flow

interface TeamRepository {
    fun getTeamDetail(teamId: Int): Flow<TeamDetail?>
    fun getTeams(limit: Int = 50): Flow<List<TeamDetail>>
    fun searchTeams(query: String, limit: Int = 50): Flow<List<TeamDetail>>
    fun getSeasonRanking(matchId: Long): Flow<List<SeasonRanking>>
    fun getSeasonRankings(): Flow<List<SeasonRanking>>
    fun getTeamsPaged(
        query: String? = null,
        sortBy: String = "POPULAR",
        limit: Int = 20,
        offset: Int = 0
    ): Flow<List<TeamDetail>> = searchTeams(query ?: "", limit)
}
