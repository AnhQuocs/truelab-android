package dev.anhquocs.truelab.feature.match.presentation.viewmodel.fakes

import dev.anhquocs.truelab.core.domain.league.model.League
import dev.anhquocs.truelab.core.domain.league.model.Season
import dev.anhquocs.truelab.core.domain.league.repository.LeagueRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/**
 * Fake implementation of LeagueRepository for deterministic ViewModel unit tests.
 */
class FakeLeagueRepository : LeagueRepository {
    private val leaguesFlow = MutableStateFlow<List<League>>(emptyList())

    fun emit(leagues: List<League>) {
        leaguesFlow.value = leagues
    }

    override fun getLeagues(): Flow<List<League>> = leaguesFlow

    override fun getLeagueDetail(leagueId: Int): Flow<League?> = flow {
        emit(leaguesFlow.value.find { it.id == leagueId })
    }

    override fun getSeasons(leagueId: Int): Flow<List<Season>> = flowOf(emptyList())
}
