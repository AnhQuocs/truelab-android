package dev.anhquocs.truelab.core.data.odds.repository

import dev.anhquocs.truelab.core.data.local.mapper.RoomMappers.toDomain
import dev.anhquocs.truelab.core.data.local.mapper.RoomMappers.toEntity
import dev.anhquocs.truelab.core.data.odds.local.dao.OddsDao
import dev.anhquocs.truelab.core.data.odds.remote.api.OddsApi
import dev.anhquocs.truelab.core.domain.odds.model.MatchOdds
import dev.anhquocs.truelab.core.domain.odds.model.OddsRecordItem
import dev.anhquocs.truelab.core.domain.odds.repository.OddsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OddsRepositoryImpl @Inject constructor(
    private val oddsDao: OddsDao,
    private val oddsApi: OddsApi
) : OddsRepository {

    private val fetchMutex = Mutex()

    override fun getMatchOdds(matchId: Long): Flow<MatchOdds> {
        return oddsDao.getOddsForMatch(matchId).map { list ->
            MatchOdds(
                matchId = matchId,
                oddsList = list.map { it.toDomain() }
            )
        }
    }

    override fun getOddsHistory(
        matchId: Long,
        companyId: Int?,
        oddsType: String?
    ): Flow<List<OddsRecordItem>> {
        return oddsDao.getOddsHistory(matchId, companyId, oddsType).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getLatestEuropeanOddsMap(): Map<Long, OddsRecordItem> {
        return oddsDao.getLatestPreMatchEuropeanOddsForAllMatches()
            .first()
            .distinctBy { it.matchId }
            .associate { it.matchId to it.toDomain() }
    }

    override suspend fun fetchAndCacheOddsForMatch(matchId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val existingRecords = oddsDao.getOddsListForMatch(matchId)
        val hasValidPreMatchOdds = existingRecords.any { entity ->
            val type = entity.oddsType.lowercase().trim()
            val phase = (entity.marketPhase ?: "").lowercase().trim()
            type == "eu" && phase !in listOf("rolling_ball", "in_play", "live", "running") &&
                entity.homeWin != null && entity.draw != null && entity.awayWin != null &&
                entity.homeWin > 1.0 && entity.draw > 1.0 && entity.awayWin > 1.0
        }

        if (hasValidPreMatchOdds) {
            return@withContext Result.success(Unit)
        }

        fetchMutex.withLock {
            val recheck = oddsDao.getOddsListForMatch(matchId)
            if (recheck.isNotEmpty()) {
                return@withContext Result.success(Unit)
            }
            try {
                val response = oddsApi.getOdds(matchId)
                val records = response.data ?: emptyList()

                if (records.isNotEmpty()) {
                    val entities = records.map { it.toEntity(matchId) }
                    oddsDao.insertOdds(entities)
                }
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
