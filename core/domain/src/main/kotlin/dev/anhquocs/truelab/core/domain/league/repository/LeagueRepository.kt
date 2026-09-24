package dev.anhquocs.truelab.core.domain.league.repository

import dev.anhquocs.truelab.core.domain.league.model.League
import dev.anhquocs.truelab.core.domain.league.model.Season
import kotlinx.coroutines.flow.Flow

interface LeagueRepository {
    fun getLeagues(): Flow<List<League>>
    fun getLeagueDetail(leagueId: Int): Flow<League?>
    fun getSeasons(leagueId: Int): Flow<List<Season>>
}
