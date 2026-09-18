package dev.anhquocs.truelab.core.data.team.repository

import dev.anhquocs.truelab.core.data.ranking.remote.api.RankingApi
import dev.anhquocs.truelab.core.domain.team.model.SeasonRanking
import dev.anhquocs.truelab.core.domain.team.model.TeamDetail
import dev.anhquocs.truelab.core.domain.team.repository.TeamRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class TeamRepositoryImpl @Inject constructor(
    private val rankingApi: RankingApi
) : TeamRepository {

    override fun getTeamDetail(teamId: Int): Flow<TeamDetail?> = flow {
        emit(TeamDetail(id = teamId, name = "Team $teamId"))
    }

    override fun getSeasonRanking(matchId: Long): Flow<List<SeasonRanking>> = flow {
        try {
            val response = rankingApi.getSeasonRanking(matchId)
            val rankings = response.data.map { rank ->
                SeasonRanking(
                    teamId = rank.teamId,
                    position = rank.position,
                    won = rank.won,
                    draw = rank.draw,
                    loss = rank.loss,
                    goalDiff = rank.goalDiff,
                    recently = rank.recently ?: emptyList()
                )
            }
            emit(rankings)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
}
