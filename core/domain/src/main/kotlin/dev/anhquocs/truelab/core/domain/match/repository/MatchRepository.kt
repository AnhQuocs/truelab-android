package dev.anhquocs.truelab.core.domain.match.repository

import dev.anhquocs.truelab.core.domain.match.model.Match
import kotlinx.coroutines.flow.Flow

interface MatchRepository {
    fun getMatches(date: String): Flow<List<Match>>
    fun getMatchDetail(matchId: Long): Flow<Match?>
    fun getRecentMatchesForTeam(teamId: Int, limit: Int = 5): Flow<List<Match>>
    fun getMatchesByLeagueAndSeason(leagueId: Int, season: String): Flow<List<Match>>
    fun getH2HMatches(teamAId: Int, teamBId: Int): Flow<List<Match>>
    fun getAllMatches(): Flow<List<Match>>
    fun getPredictableMatches(limit: Int = 50): Flow<List<Match>>
    fun searchMatches(query: String, limit: Int = 30): Flow<List<Match>>
    fun getPredictableMatchesFiltered(
        startDateUtc: String? = null,
        endDateUtc: String? = null,
        isPastDate: Boolean = false,
        isFutureDate: Boolean = false,
        datePrefix: String? = null,
        leagueId: Int? = null,
        statusFilter: dev.anhquocs.truelab.core.domain.match.model.PredictionStatusFilter = dev.anhquocs.truelab.core.domain.match.model.PredictionStatusFilter.ALL,
        searchQuery: String? = null,
        limit: Int = 50
    ): Flow<List<Match>>
    suspend fun refreshMatchesForDate(date: String): Result<Unit>
}
