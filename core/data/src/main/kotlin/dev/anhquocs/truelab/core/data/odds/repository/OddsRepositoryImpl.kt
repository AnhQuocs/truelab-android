package dev.anhquocs.truelab.core.data.odds.repository

import dev.anhquocs.truelab.core.data.local.mapper.RoomMappers.toDomain
import dev.anhquocs.truelab.core.data.odds.local.dao.OddsDao
import dev.anhquocs.truelab.core.domain.odds.model.MatchOdds
import dev.anhquocs.truelab.core.domain.odds.model.OddsRecordItem
import dev.anhquocs.truelab.core.domain.odds.repository.OddsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OddsRepositoryImpl @Inject constructor(
    private val oddsDao: OddsDao
) : OddsRepository {

    override fun getMatchOdds(matchId: Long): Flow<MatchOdds> {
        return oddsDao.getLatestOddsForMatch(matchId).map { list ->
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
}
