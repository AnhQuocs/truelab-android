package dev.anhquocs.truelab.core.data.match.repository

import dev.anhquocs.truelab.core.data.match.remote.api.MatchApi
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.domain.match.model.TeamSummary
import dev.anhquocs.truelab.core.domain.match.repository.MatchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class MatchRepositoryImpl @Inject constructor(
    private val matchApi: MatchApi
) : MatchRepository {

    override fun getMatches(date: String): Flow<List<Match>> = flow {
        try {
            val response = matchApi.getMatches(date = date)
            val matches = response.data.data.map { record ->
                Match(
                    id = record.id,
                    homeTeam = TeamSummary(id = record.homeTeam.id, name = record.homeTeam.name, logo = record.homeTeam.logo),
                    awayTeam = TeamSummary(id = record.awayTeam.id, name = record.awayTeam.name, logo = record.awayTeam.logo),
                    homeScore = record.homeScore,
                    awayScore = record.awayScore,
                    startTimeDate = record.startTimeDate,
                    status = MatchStatus.fromCode(record.status)
                )
            }
            emit(matches)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getMatchDetail(matchId: Long): Flow<Match?> = flow {
        emit(null)
    }
}
