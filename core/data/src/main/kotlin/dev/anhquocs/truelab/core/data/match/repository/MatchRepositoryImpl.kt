package dev.anhquocs.truelab.core.data.match.repository

import dev.anhquocs.truelab.core.data.crawler.DataSyncEngine
import dev.anhquocs.truelab.core.data.crawler.model.SyncResult
import dev.anhquocs.truelab.core.data.local.mapper.RoomMappers.toDomain
import dev.anhquocs.truelab.core.data.match.local.dao.MatchDao
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.PredictionStatusFilter
import dev.anhquocs.truelab.core.domain.match.repository.MatchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MatchRepositoryImpl @Inject constructor(
    private val matchDao: MatchDao,
    private val dataSyncEngine: DataSyncEngine? = null
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

    override fun getMatchesByLeagueAndSeason(leagueId: Int, season: String): Flow<List<Match>> {
        return matchDao.getMatchesByLeagueAndSeason(leagueId, season).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getH2HMatches(teamAId: Int, teamBId: Int): Flow<List<Match>> {
        return matchDao.getH2HMatches(teamAId, teamBId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getAllMatches(): Flow<List<Match>> {
        return matchDao.getAllMatches().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getPredictableMatches(limit: Int): Flow<List<Match>> {
        return matchDao.getPredictableMatches(limit).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun searchMatches(query: String, limit: Int): Flow<List<Match>> {
        return matchDao.searchMatches(query, limit).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getPredictableMatchesFiltered(
        startDateUtc: String?,
        endDateUtc: String?,
        isPastDate: Boolean,
        isFutureDate: Boolean,
        datePrefix: String?,
        leagueId: Int?,
        statusFilter: PredictionStatusFilter,
        searchQuery: String?,
        limit: Int
    ): Flow<List<Match>> {
        val statusString = statusFilter.name
        return matchDao.getPredictableMatchesFiltered(
            startDateUtc = startDateUtc,
            endDateUtc = endDateUtc,
            isPastDate = isPastDate,
            isFutureDate = isFutureDate,
            leagueId = leagueId,
            statusFilter = statusString,
            searchQuery = searchQuery?.trim()?.takeIf { it.isNotBlank() },
            limit = limit
        ).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun refreshMatchesForDate(date: String): Result<Unit> {
        val syncEngine = dataSyncEngine ?: return Result.success(Unit)
        return try {
            val syncResult = syncEngine.syncFullPipelineForDate(
                date = date,
                forceRefresh = true
            )
            when (syncResult) {
                is SyncResult.Success -> Result.success(Unit)
                is SyncResult.Failure -> Result.failure(syncResult.error)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
