package dev.anhquocs.truelab.feature.match.presentation.viewmodel

import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.league.model.League
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchSortCriteria
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.domain.match.model.TeamSummary
import dev.anhquocs.truelab.core.domain.match.usecase.SearchMatchesUseCase
import dev.anhquocs.truelab.core.domain.match.usecase.SortMatchesUseCase
import dev.anhquocs.truelab.core.ui.utils.UiText
import dev.anhquocs.truelab.feature.match.presentation.model.MatchDetailUiState
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
class MatchesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeMatchRepository: FakeMatchRepository
    private lateinit var fakeLeagueRepository: FakeLeagueRepository
    private lateinit var searchMatchesUseCase: SearchMatchesUseCase
    private lateinit var sortMatchesUseCase: SortMatchesUseCase
    private lateinit var viewModel: MatchesViewModel

    private val arsenal = TeamSummary(id = 1, name = "Arsenal")
    private val chelsea = TeamSummary(id = 2, name = "Chelsea")
    private val liverpool = TeamSummary(id = 3, name = "Liverpool")
    private val manCity = TeamSummary(id = 4, name = "Man City")

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
        awayTeam = manCity,
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
        searchMatchesUseCase = SearchMatchesUseCase()
        sortMatchesUseCase = SortMatchesUseCase()
        viewModel = MatchesViewModel(
            matchRepository = fakeMatchRepository,
            leagueRepository = fakeLeagueRepository,
            searchMatchesUseCase = searchMatchesUseCase,
            sortMatchesUseCase = sortMatchesUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun test1_initialStateIsLoading() {
        assertEquals(MatchesUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun test2_repositoryEmitsMatches_updatesUiStateToSuccess() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect {} }

        fakeMatchRepository.emit(sampleMatches)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Success state, but was $state", state is MatchesUiState.Success)
        val success = state as MatchesUiState.Success
        assertEquals(3, success.matches.size)
        assertEquals(3, success.rawMatchesCount)
    }

    @Test
    fun test3_emptyRepository_updatesUiStateToEmpty() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect {} }

        fakeMatchRepository.emit(emptyList())
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Empty state, but was $state", state is MatchesUiState.Empty)
        val empty = state as MatchesUiState.Empty
        assertEquals(UiText.StringResource(R.string.matches_empty_no_data), empty.message)
    }

    @Test
    fun test4_searchQuery_filtersMatchesCorrectly() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect {} }

        fakeMatchRepository.emit(sampleMatches)
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("Arsenal")
        advanceUntilIdle()

        val state = viewModel.uiState.value as MatchesUiState.Success
        assertEquals(2, state.matches.size)
        assertTrue(state.matches.any { it.id == "101" })
        assertTrue(state.matches.any { it.id == "103" })
    }

    @Test
    fun test5_sortCriteria_ordersMatchesByTotalGoals() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect {} }

        fakeMatchRepository.emit(sampleMatches)
        advanceUntilIdle()

        viewModel.onSortCriteriaChanged(MatchSortCriteria.TOTAL_GOALS_DESC)
        advanceUntilIdle()

        val state = viewModel.uiState.value as MatchesUiState.Success
        assertEquals("102", state.matches[0].id)
        assertEquals("101", state.matches[1].id)
        assertEquals("103", state.matches[2].id)
    }

    @Test
    fun test6_statusFilter_ALL_includesAllMatches() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect {} }

        fakeMatchRepository.emit(sampleMatches)
        viewModel.onStatusFilterChanged(MatchStatusFilter.ALL)
        advanceUntilIdle()

        val state = viewModel.uiState.value as MatchesUiState.Success
        assertEquals(3, state.matches.size)
    }

    @Test
    fun test7_statusFilter_ENDED_includesOnlyEndedMatches() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect {} }

        fakeMatchRepository.emit(sampleMatches)
        viewModel.onStatusFilterChanged(MatchStatusFilter.ENDED)
        advanceUntilIdle()

        val state = viewModel.uiState.value as MatchesUiState.Success
        assertEquals(2, state.matches.size)
        assertTrue(state.matches.all { it.actualResult != "SCHEDULED" })
    }

    @Test
    fun test8_statusFilter_SCHEDULED_includesOnlyScheduledMatches() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect {} }

        fakeMatchRepository.emit(sampleMatches)
        viewModel.onStatusFilterChanged(MatchStatusFilter.SCHEDULED)
        advanceUntilIdle()

        val state = viewModel.uiState.value as MatchesUiState.Success
        assertEquals(1, state.matches.size)
        assertEquals("103", state.matches[0].id)
        assertEquals("SCHEDULED", state.matches[0].actualResult)
    }

    @Test
    fun test9_repositoryError_updatesUiStateToError() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect {} }

        fakeMatchRepository.errorToThrow = RuntimeException("Database query failed")
        fakeMatchRepository.emit(sampleMatches)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Error state, but was $state", state is MatchesUiState.Error)
        val error = state as MatchesUiState.Error
        assertTrue(error.message is UiText.DynamicString)
        assertEquals("Database query failed", (error.message as UiText.DynamicString).value)
    }

    @Test
    fun test10_noMutationOfOriginalDataset() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val originalList = ArrayList(sampleMatches)
        fakeMatchRepository.emit(originalList)
        advanceUntilIdle()

        viewModel.onSortCriteriaChanged(MatchSortCriteria.TOTAL_GOALS_DESC)
        viewModel.onSearchQueryChanged("Liverpool")
        advanceUntilIdle()

        assertEquals(3, originalList.size)
        assertEquals(match1, originalList[0])
        assertEquals(match2, originalList[1])
        assertEquals(match3, originalList[2])
    }

    @Test
    fun test11_searchQueryWithNoResults_updatesUiStateToEmpty() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect {} }

        fakeMatchRepository.emit(sampleMatches)
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("Real Madrid")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Empty state when search matches nothing", state is MatchesUiState.Empty)
        val empty = state as MatchesUiState.Empty
        assertEquals(UiText.StringResource(R.string.matches_empty_search, "Real Madrid"), empty.message)
    }

    @Test
    fun test12_onMatchClicked_loadsAndEmitsSuccessWithCorrectDetails() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.matchDetailState.collect {} }

        fakeMatchRepository.emit(sampleMatches)
        advanceUntilIdle()

        viewModel.onMatchClicked(101L)
        advanceUntilIdle()

        val state = viewModel.matchDetailState.value
        assertTrue("Expected Success state for match detail", state is MatchDetailUiState.Success)
        val successState = state as MatchDetailUiState.Success
        assertEquals(101L, successState.match.id)
        assertEquals("Arsenal", successState.match.homeTeam.name)
        assertEquals("Chelsea", successState.match.awayTeam.name)
        assertEquals(2, successState.match.homeScore)
        assertEquals(1, successState.match.awayScore)
        assertEquals(MatchStatus.ENDED, successState.match.status)
        assertEquals(3, successState.match.totalGoals)
        assertTrue(successState.match.isHomeWin)
        assertEquals(101L, fakeMatchRepository.lastRequestedMatchId)
    }

    @Test
    fun test13_onMatchClicked_nonExistentMatch_emitsEmpty() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.matchDetailState.collect {} }

        fakeMatchRepository.emit(sampleMatches)
        advanceUntilIdle()

        viewModel.onMatchClicked(999L)
        advanceUntilIdle()

        val state = viewModel.matchDetailState.value
        assertTrue("Expected Empty state when match not found", state is MatchDetailUiState.Empty)
        assertEquals(UiText.StringResource(R.string.match_detail_empty), (state as MatchDetailUiState.Empty).message)
        assertEquals(999L, fakeMatchRepository.lastRequestedMatchId)
    }

    @Test
    fun test14_onMatchClicked_repositoryError_emitsError() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.matchDetailState.collect {} }

        fakeMatchRepository.emit(sampleMatches)
        fakeMatchRepository.errorToThrow = RuntimeException("Database error")
        advanceUntilIdle()

        viewModel.onMatchClicked(101L)
        advanceUntilIdle()

        val state = viewModel.matchDetailState.value
        assertTrue("Expected Error state when repository throws", state is MatchDetailUiState.Error)
        val error = state as MatchDetailUiState.Error
        assertTrue(error.message is UiText.DynamicString)
        assertEquals("Database error", (error.message as UiText.DynamicString).value)
    }

    @Test
    fun test15_onDismissMatchDetail_resetsStateToIdle() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.matchDetailState.collect {} }

        fakeMatchRepository.emit(sampleMatches)
        advanceUntilIdle()

        viewModel.onMatchClicked(101L)
        advanceUntilIdle()
        assertTrue(viewModel.matchDetailState.value is MatchDetailUiState.Success)

        viewModel.onDismissMatchDetail()
        advanceUntilIdle()

        val state = viewModel.matchDetailState.value
        assertTrue("Expected Idle state after dismiss", state is MatchDetailUiState.Idle)
    }
}
