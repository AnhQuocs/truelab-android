package dev.anhquocs.truelab.core.data.odds.repository

import dev.anhquocs.truelab.core.data.odds.remote.api.OddsApi
import dev.anhquocs.truelab.core.domain.odds.model.MatchOdds
import dev.anhquocs.truelab.core.domain.odds.model.OddsRecordItem
import dev.anhquocs.truelab.core.domain.odds.repository.OddsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class OddsRepositoryImpl @Inject constructor(
    private val oddsApi: OddsApi
) : OddsRepository {

    override fun getMatchOdds(matchId: Long): Flow<MatchOdds> = flow {
        try {
            val response = oddsApi.getOdds(matchId)
            val items = response.data.map { record ->
                OddsRecordItem(
                    companyId = record.companyId,
                    companyName = record.company?.name ?: "Unknown Provider",
                    oddsType = record.oddsType,
                    handicap = record.handicap,
                    over = record.over,
                    under = record.under,
                    homeWin = record.homeWin,
                    draw = record.draw,
                    awayWin = record.awayWin,
                    changeTime = record.changeTime,
                    marketPhase = record.marketPhase
                )
            }
            emit(MatchOdds(matchId = matchId, oddsList = items))
        } catch (e: Exception) {
            emit(MatchOdds(matchId = matchId, oddsList = emptyList()))
        }
    }

    override fun getOddsHistory(
        matchId: Long,
        companyId: Int?,
        oddsType: String?
    ): Flow<List<OddsRecordItem>> = flow {
        try {
            val response = oddsApi.getOddsHistory(matchId, companyId, oddsType)
            val items = response.data.data.map { record ->
                OddsRecordItem(
                    companyId = record.companyId,
                    companyName = record.company?.name ?: "Unknown Provider",
                    oddsType = record.oddsType,
                    handicap = record.handicap,
                    over = record.over,
                    under = record.under,
                    homeWin = record.homeWin,
                    draw = record.draw,
                    awayWin = record.awayWin,
                    changeTime = record.changeTime,
                    marketPhase = record.marketPhase
                )
            }
            emit(items)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
}
