package dev.anhquocs.truelab.feature.match.presentation.viewmodel.fakes

import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.repository.MatchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

/**
 * Fake implementation of MatchRepository for deterministic ViewModel unit tests.
 */
class FakeMatchRepository : MatchRepository {
    private val matchesFlow = MutableStateFlow<List<Match>>(emptyList())
    var errorToThrow: Throwable? = null
    var lastRequestedMatchId: Long? = null

    fun emit(matches: List<Match>) {
        matchesFlow.value = matches
    }

    override fun getMatches(date: String): Flow<List<Match>> = flow {
        errorToThrow?.let { throw it }
        matchesFlow.collect { emit(it) }
    }

    override fun getMatchDetail(matchId: Long): Flow<Match?> = flow {
        lastRequestedMatchId = matchId
        errorToThrow?.let { throw it }
        emit(matchesFlow.value.find { it.id == matchId })
    }

    override fun getRecentMatchesForTeam(teamId: Int, limit: Int): Flow<List<Match>> = flow {
        errorToThrow?.let { throw it }
        emit(matchesFlow.value.filter { it.homeTeam.id == teamId || it.awayTeam.id == teamId }.take(limit))
    }

    override fun getMatchesByLeagueAndSeason(leagueId: Int, season: String): Flow<List<Match>> = flow {
        errorToThrow?.let { throw it }
        emit(matchesFlow.value.filter { it.leagueId == leagueId && it.season == season })
    }
}
