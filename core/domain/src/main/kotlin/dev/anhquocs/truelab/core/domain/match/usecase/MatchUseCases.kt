package dev.anhquocs.truelab.core.domain.match.usecase

import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.domain.match.repository.MatchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class MatchUseCases(
    val getMatchesUseCase: GetMatchesUseCase,
    val getMatchDetailUseCase: GetMatchDetailUseCase
)

class GetMatchesUseCase(
    private val repository: MatchRepository
) {
    operator fun invoke(
        date: String,
        statusFilter: MatchStatus? = null,
        searchQuery: String? = null
    ): Flow<List<Match>> {
        return repository.getMatches(date).map { matches ->
            matches
                .filter { match ->
                    statusFilter == null || match.status == statusFilter
                }
                .filter { match ->
                    searchQuery.isNullOrBlank() ||
                            match.homeTeam.name.contains(searchQuery, ignoreCase = true) ||
                            match.awayTeam.name.contains(searchQuery, ignoreCase = true)
                }
                .sortedBy { it.startTimeDate }
        }
    }
}

class GetMatchDetailUseCase(
    private val repository: MatchRepository
) {
    operator fun invoke(matchId: Long): Flow<Match?> = repository.getMatchDetail(matchId)
}
