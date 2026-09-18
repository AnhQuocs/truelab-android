package dev.anhquocs.truelab.core.domain.odds.repository

import dev.anhquocs.truelab.core.domain.odds.model.MatchOdds
import dev.anhquocs.truelab.core.domain.odds.model.OddsRecordItem
import kotlinx.coroutines.flow.Flow

interface OddsRepository {
    fun getMatchOdds(matchId: Long): Flow<MatchOdds>
    fun getOddsHistory(matchId: Long, companyId: Int?, oddsType: String?): Flow<List<OddsRecordItem>>
}
