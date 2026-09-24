package dev.anhquocs.truelab.core.data.league.repository

import dev.anhquocs.truelab.core.data.league.local.dao.LeagueDao
import dev.anhquocs.truelab.core.data.league.local.dao.SeasonDao
import dev.anhquocs.truelab.core.data.local.mapper.RoomMappers.toDomain
import dev.anhquocs.truelab.core.domain.league.model.League
import dev.anhquocs.truelab.core.domain.league.model.Season
import dev.anhquocs.truelab.core.domain.league.repository.LeagueRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LeagueRepositoryImpl @Inject constructor(
    private val leagueDao: LeagueDao,
    private val seasonDao: SeasonDao
) : LeagueRepository {

    override fun getLeagues(): Flow<List<League>> {
        return leagueDao.getLeagues().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getLeagueDetail(leagueId: Int): Flow<League?> {
        return leagueDao.getLeagueById(leagueId).map { it?.toDomain() }
    }

    override fun getSeasons(leagueId: Int): Flow<List<Season>> {
        return seasonDao.getSeasonsByLeague(leagueId).map { list ->
            list.map { it.toDomain() }
        }
    }
}
