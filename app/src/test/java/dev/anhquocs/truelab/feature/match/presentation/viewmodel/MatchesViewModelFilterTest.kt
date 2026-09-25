package dev.anhquocs.truelab.feature.match.presentation.viewmodel

import dev.anhquocs.truelab.core.domain.league.model.League
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.domain.match.model.TeamSummary
import dev.anhquocs.truelab.core.domain.match.usecase.SearchMatchesUseCase
import dev.anhquocs.truelab.core.domain.match.usecase.SortMatchesUseCase
import dev.anhquocs.truelab.feature.match.presentation.model.MatchStatusFilter
import dev.anhquocs.truelab.feature.match.presentation.model.MatchesUiState
import dev.anhquocs.truelab.feature.match.presentation.viewmodel.fakes.FakeLeagueRepository
import dev.anhquocs.truelab.feature.match.presentation.viewmodel.fakes.FakeMatchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MatchesViewModelFilterTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeMatchRepository: FakeMatchRepository
    private lateinit var fakeLeagueRepository: FakeLeagueRepository
    private lateinit var viewModel: MatchesViewModel

    private val arsenal = TeamSummary(id = 1, name = "Arsenal")
    private val chelsea = TeamSummary(id = 2, name = "Chelsea")
    private val liverpool = TeamSummary(id = 3, name = "Liverpool")

    private val premierLeague = League(id = 1, name = "Premier League")
    private val laLiga = League(id = 2, name = "La Liga")

    private val match1 = Match(
        id = 101L,
        homeTeam = arsenal,
        awayTeam = chelsea,
        homeScore = 2,
        awayScore = 1,
        startTimeDate = "2026-03-01T15:00:00",
        status = MatchStatus.ENDED,
        leagueId = 1,
        season = "2025-2026"
    )

    private val match2 = Match(
        id = 102L,
        homeTeam = liverpool,
        awayTeam = arsenal,
        homeScore = 3,
        awayScore = 3,
        startTimeDate = "2026-03-05T20:00:00",
        status = MatchStatus.ENDED,
        leagueId = 1,
        season = "2025-2026"
    )

    private val match3 = Match(
        id = 103L,
        homeTeam = arsenal,
        awayTeam = liverpool,
        homeScore = null,
        awayScore = null,
        startTimeDate = "2026-03-10T17:30:00",
        status = MatchStatus.SCHEDULED,
        leagueId = 2,
        season = "2025-2026"
    )

    private val sampleMatches = listOf(match1, match2, match3)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeMatchRepository = FakeMatchRepository()
        fakeLeagueRepository = FakeLeagueRepository()
        viewModel = MatchesViewModel(
            matchRepository = fakeMatchRepository,
            leagueRepository = fakeLeagueRepository,
            searchMatchesUseCase = SearchMatchesUseCase(),
            sortMatchesUseCase = SortMatchesUseCase()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun test1_leaguesEmitted_availableInViewModelLeaguesStateFlow() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.leagues.collect {} }

        fakeLeagueRepository.emit(listOf(premierLeague, laLiga))
        advanceUntilIdle()

        val list = viewModel.leagues.value
        assertEquals(2, list.size)
        assertEquals("Premier League", list[0].name)
        assertEquals("La Liga", list[1].name)
    }

    @Test
    fun test2_onLeagueSelected_filtersMatchesBySelectedLeague() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect {} }
        backgroundScope.launch { viewModel.leagues.collect {} }

        fakeMatchRepository.emit(sampleMatches)
        fakeLeagueRepository.emit(listOf(premierLeague, laLiga))
        advanceUntilIdle()

        viewModel.onLeagueSelected(1)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Success state, was $state", state is MatchesUiState.Success)
        val success = state as MatchesUiState.Success
        assertEquals(2, success.matches.size)
        assertTrue(success.matches.all { it.id == "101" || it.id == "102" })
        assertEquals(1, viewModel.selectedLeagueId.value)
    }

    @Test
    fun test3_onLeagueSelected_resetToNull_restoresAllMatches() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect {} }

        fakeMatchRepository.emit(sampleMatches)
        advanceUntilIdle()

        viewModel.onLeagueSelected(2)
        advanceUntilIdle()
        val filteredState = viewModel.uiState.value as MatchesUiState.Success
        assertEquals(1, filteredState.matches.size)
        assertEquals("103", filteredState.matches[0].id)

        viewModel.onLeagueSelected(null)
        advanceUntilIdle()

        val allState = viewModel.uiState.value as MatchesUiState.Success
        assertEquals(3, allState.matches.size)
        assertNull(viewModel.selectedLeagueId.value)
    }

    @Test
    fun test4_onClearFilters_resetsLeagueStatusAndSearch() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect {} }

        fakeMatchRepository.emit(sampleMatches)
        advanceUntilIdle()

        viewModel.onLeagueSelected(1)
        viewModel.onStatusFilterChanged(MatchStatusFilter.ENDED)
        viewModel.onSearchQueryChanged("Arsenal")
        advanceUntilIdle()

        viewModel.onClearFilters()
        advanceUntilIdle()

        assertNull(viewModel.selectedLeagueId.value)
        assertNull(viewModel.selectedSeason.value)
        assertEquals(MatchStatusFilter.ALL, viewModel.statusFilter.value)
        assertEquals("", viewModel.searchQuery.value)
    }

    @Test
    fun test5_leagueAndSeasonFilter_invokesGetMatchesByLeagueAndSeason() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect {} }

        fakeMatchRepository.emit(sampleMatches)
        advanceUntilIdle()

        viewModel.onLeagueSelected(1)
        viewModel.onSeasonSelected("2025-2026")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is MatchesUiState.Success)
        val success = state as MatchesUiState.Success
        assertEquals(2, success.matches.size)
    }
}
