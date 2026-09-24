package dev.anhquocs.truelab.core.domain.match.repository

import dev.anhquocs.truelab.core.domain.match.model.Match
import kotlinx.coroutines.flow.Flow

interface MatchRepository {
    fun getMatches(date: String): Flow<List<Match>>
    fun getMatchDetail(matchId: Long): Flow<Match?>
    fun getRecentMatchesForTeam(teamId: Int, limit: Int = 5): Flow<List<Match>>
    fun getMatchesByLeagueAndSeason(leagueId: Int, season: String): Flow<List<Match>>
}
