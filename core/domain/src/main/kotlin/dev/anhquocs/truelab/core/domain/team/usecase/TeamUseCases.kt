package dev.anhquocs.truelab.core.domain.team.usecase

import dev.anhquocs.truelab.core.domain.team.model.SeasonRanking
import dev.anhquocs.truelab.core.domain.team.model.TeamDetail
import dev.anhquocs.truelab.core.domain.team.repository.TeamRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class TeamUseCases(
    val getTeamDetailUseCase: GetTeamDetailUseCase,
    val getSeasonRankingUseCase: GetSeasonRankingUseCase
)

class GetTeamDetailUseCase(
    private val repository: TeamRepository
) {
    operator fun invoke(teamId: Int): Flow<TeamDetail?> = repository.getTeamDetail(teamId)
}

class GetSeasonRankingUseCase(
    private val repository: TeamRepository
) {
    operator fun invoke(matchId: Long): Flow<List<SeasonRanking>> {
        return repository.getSeasonRanking(matchId).map { rankings ->
            rankings.sortedBy { it.position }
        }
    }
}
