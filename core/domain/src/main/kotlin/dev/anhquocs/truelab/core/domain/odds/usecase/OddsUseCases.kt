package dev.anhquocs.truelab.core.domain.odds.usecase

import dev.anhquocs.truelab.core.domain.odds.model.MatchOdds
import dev.anhquocs.truelab.core.domain.odds.model.OddsRecordItem
import dev.anhquocs.truelab.core.domain.odds.repository.OddsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class OddsUseCases(
    val getMatchOddsUseCase: GetMatchOddsUseCase,
    val getOddsHistoryUseCase: GetOddsHistoryUseCase
)

class GetMatchOddsUseCase(
    private val repository: OddsRepository
) {
    operator fun invoke(matchId: Long): Flow<MatchOdds> = repository.getMatchOdds(matchId)
}

class GetOddsHistoryUseCase(
    private val repository: OddsRepository
) {
    operator fun invoke(
        matchId: Long,
        companyId: Int? = null,
        oddsType: String? = null
    ): Flow<List<OddsRecordItem>> {
        return repository.getOddsHistory(matchId, companyId, oddsType).map { history ->
            history.sortedBy { it.changeTime }
        }
    }
}
