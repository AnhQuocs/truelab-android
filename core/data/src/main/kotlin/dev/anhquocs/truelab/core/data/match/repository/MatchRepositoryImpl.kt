package dev.anhquocs.truelab.core.data.match.repository

import dev.anhquocs.truelab.core.data.local.mapper.RoomMappers.toDomain
import dev.anhquocs.truelab.core.data.match.local.dao.MatchDao
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.repository.MatchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MatchRepositoryImpl @Inject constructor(
    private val matchDao: MatchDao
) : MatchRepository {

    override fun getMatches(date: String): Flow<List<Match>> {
        return matchDao.getMatchesByDate(date).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getMatchDetail(matchId: Long): Flow<Match?> {
        return matchDao.getMatchById(matchId).map { it?.toDomain() }
    }

    override fun getRecentMatchesForTeam(teamId: Int, limit: Int): Flow<List<Match>> {
        return matchDao.getRecentMatchesForTeam(teamId, limit).map { list ->
            list.map { it.toDomain() }
        }
    }
}
