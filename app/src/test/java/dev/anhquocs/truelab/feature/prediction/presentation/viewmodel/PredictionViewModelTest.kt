package dev.anhquocs.truelab.feature.prediction.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.league.model.League
import dev.anhquocs.truelab.core.domain.league.model.Season
import dev.anhquocs.truelab.core.domain.league.repository.LeagueRepository
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.domain.match.model.PredictionStatusFilter
import dev.anhquocs.truelab.core.domain.match.model.TeamSummary
import dev.anhquocs.truelab.core.domain.match.repository.MatchRepository
import dev.anhquocs.truelab.core.domain.odds.model.MatchOdds
import dev.anhquocs.truelab.core.domain.odds.model.OddsRecordItem
import dev.anhquocs.truelab.core.domain.odds.repository.OddsRepository
import dev.anhquocs.truelab.core.domain.prediction.usecase.PredictMatchOutcomeUseCase
import dev.anhquocs.truelab.core.domain.team.model.SeasonRanking
import dev.anhquocs.truelab.core.domain.team.model.TeamDetail
import dev.anhquocs.truelab.core.domain.team.repository.TeamRepository
import dev.anhquocs.truelab.core.ui.utils.UiText
import dev.anhquocs.truelab.feature.prediction.presentation.model.PredictionUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class PredictionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeMatchRepository: FakeMatchRepository
    private lateinit var fakeTeamRepository: FakeTeamRepository
    private lateinit var fakeOddsRepository: FakeOddsRepository
    private lateinit var fakeLeagueRepository: FakeLeagueRepository
    private lateinit var predictMatchOutcomeUseCase: PredictMatchOutcomeUseCase
    private lateinit var viewModel: PredictionViewModel

    private val today = LocalDate.now().toString()

    private val manCity = TeamDetail(id = 1, name = "Manchester City", leagueName = "Premier League", eloRating = 1980.0)
    private val arsenal = TeamDetail(id = 2, name = "Arsenal", leagueName = "Premier League", eloRating = 1945.0)
    private val liverpool = TeamDetail(id = 3, name = "Liverpool", leagueName = "Premier League", eloRating = 1920.0)
    private val vietnam = TeamDetail(id = 4, name = "Vietnam", leagueName = "FIFA ASEAN Cup", eloRating = 1500.0)
    private val pakistan = TeamDetail(id = 5, name = "Pakistan", leagueName = "FIFA ASEAN Cup", eloRating = 1300.0)

    private val match1 = Match(
        id = 101L,
        homeTeam = TeamSummary(id = 1, name = "Manchester City"),
        awayTeam = TeamSummary(id = 2, name = "Arsenal"),
        homeScore = 1,
        awayScore = 0,
        startTimeDate = "${today}T15:00:00",
        status = MatchStatus.IN_PROGRESS,
        leagueId = 10,
        leagueName = "Premier League",
        minutes = "65"
    )

    private val match2 = Match(
        id = 102L,
        homeTeam = TeamSummary(id = 2, name = "Arsenal"),
        awayTeam = TeamSummary(id = 3, name = "Liverpool"),
        homeScore = null,
        awayScore = null,
        startTimeDate = "${today}T17:30:00",
        status = MatchStatus.SCHEDULED,
        leagueId = 10,
        leagueName = "Premier League"
    )

    private val match3 = Match(
        id = 103L,
        homeTeam = TeamSummary(id = 4, name = "Vietnam"),
        awayTeam = TeamSummary(id = 5, name = "Pakistan"),
        homeScore = 2,
        awayScore = 0,
        startTimeDate = "${today}T12:00:00",
        status = MatchStatus.ENDED,
        leagueId = 20,
        leagueName = "FIFA ASEAN Cup"
    )

    private val match1Odds = MatchOdds(
        matchId = 101L,
        oddsList = listOf(
            OddsRecordItem(companyId = 1, companyName = "Provider A", oddsType = "eu", homeWin = 1.85, draw = 3.40, awayWin = 4.20, marketPhase = "immediate")
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeMatchRepository = FakeMatchRepository()
        fakeTeamRepository = FakeTeamRepository()
        fakeOddsRepository = FakeOddsRepository()
        fakeLeagueRepository = FakeLeagueRepository()
        predictMatchOutcomeUseCase = PredictMatchOutcomeUseCase()

        fakeMatchRepository.setMatches(listOf(match1, match2, match3))
        fakeTeamRepository.setTeams(listOf(manCity, arsenal, liverpool, vietnam, pakistan))
        fakeOddsRepository.setMatchOdds(101L, match1Odds)
        fakeLeagueRepository.setLeagues(listOf(
            League(id = 10, name = "Premier League", country = "England"),
            League(id = 20, name = "FIFA ASEAN Cup", country = "Asia")
        ))

        viewModel = PredictionViewModel(
            savedStateHandle = SavedStateHandle(),
            matchRepository = fakeMatchRepository,
            teamRepository = fakeTeamRepository,
            oddsRepository = fakeOddsRepository,
            leagueRepository = fakeLeagueRepository,
            predictMatchOutcomeUseCase = predictMatchOutcomeUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading and auto selects LIVE match`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Success state but was $state", state is PredictionUiState.Success)
        val success = state as PredictionUiState.Success

        assertEquals(101L, success.selectedMatch.id)
        assertEquals("65", success.selectedMatch.minutes)
        assertEquals(MatchStatus.IN_PROGRESS, success.selectedMatch.status)
        assertEquals(1980.0, success.homeElo)
        assertEquals(1945.0, success.awayElo)

        val totalPct = success.homeWinPercent + success.drawPercent + success.awayWinPercent
        assertTrue(totalPct in 99..101)
        collectJob.cancel()
    }

    @Test
    fun `date navigation updates selected date and fetches predictable matches`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onNextDay()
        advanceUntilIdle()
        val nextDay = LocalDate.parse(today).plusDays(1).toString()
        assertEquals(nextDay, viewModel.selectedDate.value)

        viewModel.onPreviousDay()
        advanceUntilIdle()
        assertEquals(today, viewModel.selectedDate.value)

        viewModel.onToday()
        advanceUntilIdle()
        assertEquals(today, viewModel.selectedDate.value)

        collectJob.cancel()
    }

    @Test
    fun `status filter changes filter parameter and resets selection`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onStatusFilterSelected(PredictionStatusFilter.UPCOMING)
        advanceUntilIdle()

        assertEquals(PredictionStatusFilter.UPCOMING, viewModel.selectedStatusFilter.value)
        val success = viewModel.uiState.value as PredictionUiState.Success
        assertEquals(102L, success.selectedMatch.id)

        collectJob.cancel()
    }

    @Test
    fun `league selection filters match list`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onLeagueSelected(10)
        advanceUntilIdle()
        assertEquals(10, viewModel.selectedLeagueId.value)

        viewModel.onLeagueSelected(999) // Non-existent league
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is PredictionUiState.Empty)

        collectJob.cancel()
    }

    @Test
    fun `reset filters restores defaults`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onLeagueSelected(999)
        viewModel.onSearchQueryChanged("test")
        viewModel.onStatusFilterSelected(PredictionStatusFilter.LIVE)
        advanceUntilIdle()

        viewModel.onResetFilters()
        advanceUntilIdle()

        assertEquals(today, viewModel.selectedDate.value)
        assertEquals(null, viewModel.selectedLeagueId.value)
        assertEquals(PredictionStatusFilter.LIVE_AND_UPCOMING, viewModel.selectedStatusFilter.value)
        assertEquals("", viewModel.searchQuery.value)

        collectJob.cancel()
    }

    @Test
    fun `pull to refresh triggers matchRepository refreshMatchesForDate`() = runTest {
        var refreshedDate: String? = null
        fakeMatchRepository.onRefreshCallback = { date -> refreshedDate = date }

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(today, refreshedDate)
        assertEquals(false, viewModel.isRefreshing.value)
    }

    @Test
    fun `pull to refresh uses selectedDate when date is changed`() = runTest {
        var refreshedDate: String? = null
        fakeMatchRepository.onRefreshCallback = { date -> refreshedDate = date }

        val futureDate = "2026-10-05"
        viewModel.onDateSelected(futureDate)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(futureDate, refreshedDate)
        assertEquals(false, viewModel.isRefreshing.value)
    }

    @Test
    fun `pull to refresh failure preserves existing data and emits refreshErrorEvent`() = runTest {
        fakeMatchRepository.refreshResult = Result.failure(RuntimeException("Network error"))

        var emittedError: UiText? = null
        val errorJob = launch {
            viewModel.refreshErrorEvent.collect { emittedError = it }
        }

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(false, viewModel.isRefreshing.value)
        assertNotNull(emittedError)
        val state = viewModel.uiState.value
        assertTrue("Data should be preserved on refresh failure", state is PredictionUiState.Success)
        assertEquals(101L, (state as PredictionUiState.Success).selectedMatch.id)

        errorJob.cancel()
        collectJob.cancel()
    }

    @Test
    fun `when Room emits updated match snapshot after refresh, selectedMatch is updated`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val initialState = viewModel.uiState.value as PredictionUiState.Success
        assertEquals(101L, initialState.selectedMatch.id)
        assertEquals("65", initialState.selectedMatch.minutes)
        assertEquals(1, initialState.selectedMatch.homeScore)

        // Simulate refresh updating the match in Room with new minutes and score
        val updatedMatch1 = match1.copy(
            minutes = "75",
            homeScore = 2,
            awayScore = 0
        )
        fakeMatchRepository.setMatches(listOf(updatedMatch1, match2))
        advanceUntilIdle()

        val updatedState = viewModel.uiState.value as PredictionUiState.Success
        assertEquals("75", updatedState.selectedMatch.minutes)
        assertEquals(2, updatedState.selectedMatch.homeScore)
        assertEquals(0, updatedState.selectedMatch.awayScore)

        collectJob.cancel()
    }

    @Test
    fun `uiState emits Empty when matches list is empty`() = runTest {
        fakeMatchRepository.setMatches(emptyList())

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Empty state but was $state", state is PredictionUiState.Empty)
        val empty = state as PredictionUiState.Empty
        assertTrue(empty.message is UiText.StringResource)
        assertEquals(R.string.prediction_empty_no_matches, (empty.message as UiText.StringResource).resId)

        collectJob.cancel()
    }

    @Test
    fun `search query filters available matches without reloading full dataset`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("Liverpool")
        advanceUntilIdle()

        val stateLiverpool = viewModel.uiState.value as PredictionUiState.Success
        assertEquals(1, stateLiverpool.availableMatches.size)
        assertEquals(102L, stateLiverpool.availableMatches[0].id)

        collectJob.cancel()
    }

    @Test
    fun `search query matches competition name properly`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        // Show ALL status filter so ended FIFA match is included
        viewModel.onStatusFilterSelected(PredictionStatusFilter.ALL)
        viewModel.onSearchQueryChanged("fifa")
        advanceUntilIdle()

        val state = viewModel.uiState.value as PredictionUiState.Success
        assertEquals(1, state.availableMatches.size)
        assertEquals(103L, state.availableMatches[0].id)
        assertEquals("Vietnam", state.availableMatches[0].homeTeam.name)

        collectJob.cancel()
    }

    @Test
    fun `finished status filter only returns ended matches for selected date`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onStatusFilterSelected(PredictionStatusFilter.FINISHED)
        advanceUntilIdle()

        val state = viewModel.uiState.value as PredictionUiState.Success
        assertEquals(1, state.availableMatches.size)
        assertEquals(103L, state.availableMatches[0].id)
        assertEquals(MatchStatus.ENDED, state.availableMatches[0].status)

        collectJob.cancel()
    }

    @Test
    fun `combined filter date, competition, finished status, and search query works together`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onLeagueSelected(20) // FIFA ASEAN Cup
        viewModel.onStatusFilterSelected(PredictionStatusFilter.FINISHED)
        viewModel.onSearchQueryChanged("vietnam")
        advanceUntilIdle()

        val state = viewModel.uiState.value as PredictionUiState.Success
        assertEquals(1, state.availableMatches.size)
        assertEquals(103L, state.availableMatches[0].id)

        // Change search to something not matching Vietnam in FIFA
        viewModel.onSearchQueryChanged("Arsenal")
        advanceUntilIdle()

        assertTrue("Expected Empty state for non-matching search", viewModel.uiState.value is PredictionUiState.Empty)

        collectJob.cancel()
    }

    @Test
    fun `prediction evidence in uiState is properly populated with team elo and odds details`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value as PredictionUiState.Success
        val evidence = state.predictionResult.evidence
        assertNotNull(evidence)
        assertEquals("1980.0", evidence!!.elo.details["homeElo"])
        assertEquals("1945.0", evidence.elo.details["awayElo"])
        assertEquals("+35.0", evidence.elo.details["diff"])
        assertEquals(6, evidence.signals.size)

        collectJob.cancel()
    }

    @Test
    fun `date navigation automatically triggers date-scoped sync on matchRepository`() = runTest {
        var syncedDate: String? = null
        fakeMatchRepository.onRefreshCallback = { date -> syncedDate = date }

        viewModel.onNextDay()
        advanceUntilIdle()

        val nextDay = LocalDate.parse(today).plusDays(1).toString()
        assertEquals(nextDay, syncedDate)
    }

    @Test
    fun `past date resolves to ended status without LIVE or UPCOMING indicators`() {
        val pastMatch = Match(
            id = 201L,
            homeTeam = TeamSummary(id = 1, name = "Manchester City"),
            awayTeam = TeamSummary(id = 2, name = "Arsenal"),
            homeScore = 2,
            awayScore = 1,
            startTimeDate = "2024-05-10T15:00:00Z",
            status = MatchStatus.IN_PROGRESS, // Stale status in DB
            leagueId = 10,
            minutes = "75"
        )
        val pastDate = "2024-05-10"
        val status = dev.anhquocs.truelab.feature.prediction.presentation.components.resolveDisplayStatus(pastMatch, pastDate)
        assertEquals(dev.anhquocs.truelab.feature.prediction.presentation.components.DisplayMatchStatus.ENDED, status)
    }

    @Test
    fun `future date resolves to upcoming status without LIVE or ENDED indicators`() {
        val futureMatch = Match(
            id = 202L,
            homeTeam = TeamSummary(id = 1, name = "Manchester City"),
            awayTeam = TeamSummary(id = 2, name = "Arsenal"),
            homeScore = 0,
            awayScore = 0,
            startTimeDate = "2030-05-10T15:00:00Z",
            status = MatchStatus.IN_PROGRESS, // Stale status in DB
            leagueId = 10
        )
        val futureDate = "2030-05-10"
        val status = dev.anhquocs.truelab.feature.prediction.presentation.components.resolveDisplayStatus(futureMatch, futureDate)
        assertEquals(dev.anhquocs.truelab.feature.prediction.presentation.components.DisplayMatchStatus.UPCOMING, status)
    }

    @Test
    fun `today date preserves live and upcoming statuses`() {
        val liveMatch = match1.copy(status = MatchStatus.IN_PROGRESS)
        val upcomingMatch = match2.copy(status = MatchStatus.SCHEDULED)

        val liveStatus = dev.anhquocs.truelab.feature.prediction.presentation.components.resolveDisplayStatus(liveMatch, today)
        val upcomingStatus = dev.anhquocs.truelab.feature.prediction.presentation.components.resolveDisplayStatus(upcomingMatch, today)

        assertEquals(dev.anhquocs.truelab.feature.prediction.presentation.components.DisplayMatchStatus.LIVE, liveStatus)
        assertEquals(dev.anhquocs.truelab.feature.prediction.presentation.components.DisplayMatchStatus.UPCOMING, upcomingStatus)
    }

    @Test
    fun `today kickoff time comparison strictly respects server status`() {
        // Mock current time: 2026-10-01 08:50:00 Asia/Ho_Chi_Minh
        val testZone = dev.anhquocs.truelab.core.ui.utils.DateTimeFormatterUtils.VIETNAM_ZONE_ID
        val now = java.time.ZonedDateTime.of(2026, 10, 1, 8, 50, 0, 0, testZone)
        val selectedDate = "2026-10-01"

        // Match A: 07:00 GMT+7 pending (kickoff passed) -> UPCOMING (Never LIVE)
        val matchA = match1.copy(startTimeDate = "2026-10-01T07:00:00+07:00", status = MatchStatus.SCHEDULED)
        val statusA = dev.anhquocs.truelab.feature.prediction.presentation.components.resolveDisplayStatus(matchA, selectedDate, now)
        assertEquals(dev.anhquocs.truelab.feature.prediction.presentation.components.DisplayMatchStatus.UPCOMING, statusA)

        // Match B: 07:30 GMT+7 pending -> UPCOMING
        val matchB = match1.copy(startTimeDate = "2026-10-01T07:30:00+07:00", status = MatchStatus.SCHEDULED)
        val statusB = dev.anhquocs.truelab.feature.prediction.presentation.components.resolveDisplayStatus(matchB, selectedDate, now)
        assertEquals(dev.anhquocs.truelab.feature.prediction.presentation.components.DisplayMatchStatus.UPCOMING, statusB)

        // Match C: 08:30 GMT+7 server confirmed IN_PROGRESS -> LIVE
        val matchC = match1.copy(startTimeDate = "2026-10-01T08:30:00+07:00", status = MatchStatus.IN_PROGRESS, minutes = "20")
        val statusC = dev.anhquocs.truelab.feature.prediction.presentation.components.resolveDisplayStatus(matchC, selectedDate, now)
        assertEquals(dev.anhquocs.truelab.feature.prediction.presentation.components.DisplayMatchStatus.LIVE, statusC)

        // Match D: 09:00 GMT+7 pending -> UPCOMING
        val matchD = match1.copy(startTimeDate = "2026-10-01T09:00:00+07:00", status = MatchStatus.SCHEDULED)
        val statusD = dev.anhquocs.truelab.feature.prediction.presentation.components.resolveDisplayStatus(matchD, selectedDate, now)
        assertEquals(dev.anhquocs.truelab.feature.prediction.presentation.components.DisplayMatchStatus.UPCOMING, statusD)

        // Match E: 06:00 GMT+7 server confirmed ENDED -> ENDED
        val matchE = match1.copy(startTimeDate = "2026-10-01T06:00:00+07:00", status = MatchStatus.ENDED, homeScore = 2, awayScore = 1)
        val statusE = dev.anhquocs.truelab.feature.prediction.presentation.components.resolveDisplayStatus(matchE, selectedDate, now)
        assertEquals(dev.anhquocs.truelab.feature.prediction.presentation.components.DisplayMatchStatus.ENDED, statusE)

        // Match F: 12:00 GMT+7 pending -> UPCOMING
        val matchF = match1.copy(startTimeDate = "2026-10-01T12:00:00+07:00", status = MatchStatus.SCHEDULED)
        val statusF = dev.anhquocs.truelab.feature.prediction.presentation.components.resolveDisplayStatus(matchF, selectedDate, now)
        assertEquals(dev.anhquocs.truelab.feature.prediction.presentation.components.DisplayMatchStatus.UPCOMING, statusF)

        // Match G: 08:50 GMT+7 (exact now) pending -> UPCOMING
        val matchG = match1.copy(startTimeDate = "2026-10-01T08:50:00+07:00", status = MatchStatus.SCHEDULED)
        val statusG = dev.anhquocs.truelab.feature.prediction.presentation.components.resolveDisplayStatus(matchG, selectedDate, now)
        assertEquals(dev.anhquocs.truelab.feature.prediction.presentation.components.DisplayMatchStatus.UPCOMING, statusG)

        // Match H: 01:45 GMT+7 pending (e.g. at 10:30am) -> UPCOMING (Never LIVE)
        val now1030 = java.time.ZonedDateTime.of(2026, 10, 1, 10, 30, 0, 0, testZone)
        val matchH = match1.copy(startTimeDate = "2026-10-01T01:45:00+07:00", status = MatchStatus.SCHEDULED)
        val statusH = dev.anhquocs.truelab.feature.prediction.presentation.components.resolveDisplayStatus(matchH, selectedDate, now1030)
        assertEquals(dev.anhquocs.truelab.feature.prediction.presentation.components.DisplayMatchStatus.UPCOMING, statusH)
    }

    @Test
    fun `past date is never LIVE and never UPCOMING`() {
        val pastDate = "2026-09-30"
        val now = java.time.ZonedDateTime.of(2026, 10, 1, 8, 50, 0, 0, dev.anhquocs.truelab.core.ui.utils.DateTimeFormatterUtils.VIETNAM_ZONE_ID)

        val pastLiveMatch = match1.copy(startTimeDate = "2026-09-30T20:00:00+07:00", status = MatchStatus.IN_PROGRESS)
        val pastPendingMatch = match1.copy(startTimeDate = "2026-09-30T20:00:00+07:00", status = MatchStatus.SCHEDULED)

        val status1 = dev.anhquocs.truelab.feature.prediction.presentation.components.resolveDisplayStatus(pastLiveMatch, pastDate, now)
        val status2 = dev.anhquocs.truelab.feature.prediction.presentation.components.resolveDisplayStatus(pastPendingMatch, pastDate, now)

        assertEquals(dev.anhquocs.truelab.feature.prediction.presentation.components.DisplayMatchStatus.ENDED, status1)
        assertEquals(dev.anhquocs.truelab.feature.prediction.presentation.components.DisplayMatchStatus.ENDED, status2)
    }

    @Test
    fun `future date is never LIVE and only UPCOMING`() {
        val futureDate = "2026-10-02"
        val now = java.time.ZonedDateTime.of(2026, 10, 1, 8, 50, 0, 0, dev.anhquocs.truelab.core.ui.utils.DateTimeFormatterUtils.VIETNAM_ZONE_ID)

        val futureLiveMatch = match1.copy(startTimeDate = "2026-10-02T09:00:00+07:00", status = MatchStatus.IN_PROGRESS)
        val futureEndedMatch = match1.copy(startTimeDate = "2026-10-02T09:00:00+07:00", status = MatchStatus.ENDED)
        val futurePendingMatch = match1.copy(startTimeDate = "2026-10-02T09:00:00+07:00", status = MatchStatus.SCHEDULED)

        val status1 = dev.anhquocs.truelab.feature.prediction.presentation.components.resolveDisplayStatus(futureLiveMatch, futureDate, now)
        val status2 = dev.anhquocs.truelab.feature.prediction.presentation.components.resolveDisplayStatus(futureEndedMatch, futureDate, now)
        val status3 = dev.anhquocs.truelab.feature.prediction.presentation.components.resolveDisplayStatus(futurePendingMatch, futureDate, now)

        assertEquals(dev.anhquocs.truelab.feature.prediction.presentation.components.DisplayMatchStatus.UPCOMING, status1)
        assertEquals(dev.anhquocs.truelab.feature.prediction.presentation.components.DisplayMatchStatus.UPCOMING, status2)
        assertEquals(dev.anhquocs.truelab.feature.prediction.presentation.components.DisplayMatchStatus.UPCOMING, status3)
    }

    @Test
    fun `timezone conversion from UTC to Vietnam UTC+7 parses accurately`() {
        val utcIso = "2026-10-01T01:50:00Z" // 01:50 UTC = 08:50 Vietnam UTC+7
        val zoned = dev.anhquocs.truelab.core.ui.utils.DateTimeFormatterUtils.parseToVietnamZonedDateTime(utcIso)
        assertNotNull(zoned)
        assertEquals(2026, zoned.year)
        assertEquals(10, zoned.monthValue)
        assertEquals(1, zoned.dayOfMonth)
        assertEquals(8, zoned.hour)
        assertEquals(50, zoned.minute)
    }

    @Test
    fun `today sorting puts LIVE first, UPCOMING in kickoff ASC second, ENDED last`() {
        val now = java.time.ZonedDateTime.of(2026, 10, 1, 8, 50, 0, 0, dev.anhquocs.truelab.core.ui.utils.DateTimeFormatterUtils.VIETNAM_ZONE_ID)
        val selectedDate = "2026-10-01"

        val m1 = match1.copy(id = 1L, startTimeDate = "2026-10-01T10:00:00+07:00", status = MatchStatus.SCHEDULED) // UPCOMING 10:00
        val m2 = match1.copy(id = 2L, startTimeDate = "2026-10-01T08:30:00+07:00", status = MatchStatus.IN_PROGRESS, minutes = "20") // LIVE 08:30
        val m3 = match1.copy(id = 3L, startTimeDate = "2026-10-01T07:00:00+07:00", status = MatchStatus.SCHEDULED) // UPCOMING 07:00
        val m4 = match1.copy(id = 4L, startTimeDate = "2026-10-01T09:00:00+07:00", status = MatchStatus.SCHEDULED) // UPCOMING 09:00
        val m5 = match1.copy(id = 5L, startTimeDate = "2026-10-01T06:00:00+07:00", status = MatchStatus.ENDED) // ENDED 06:00

        val unsorted = listOf(m1, m2, m3, m4, m5)
        val sorted = PredictionViewModel.sortMatchesForDisplay(unsorted, selectedDate, now)

        assertEquals(listOf(2L, 3L, 4L, 1L, 5L), sorted.map { it.id })
    }

    @Test
    fun `on-demand odds hydration is triggered when match is selected`() = runTest {
        viewModel = PredictionViewModel(
            savedStateHandle = SavedStateHandle(),
            matchRepository = fakeMatchRepository,
            teamRepository = fakeTeamRepository,
            oddsRepository = fakeOddsRepository,
            leagueRepository = fakeLeagueRepository,
            predictMatchOutcomeUseCase = predictMatchOutcomeUseCase
        )

        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        // autoSelectMatch picks match1 (101L)
        assertTrue(fakeOddsRepository.fetchedMatchIds.contains(101L))

        // When selecting match2 (102L)
        viewModel.onSelectMatch(102L)
        advanceUntilIdle()

        assertTrue(fakeOddsRepository.fetchedMatchIds.contains(102L))
        job.cancel()
    }

    @Test
    fun `prediction with valid pre-match EU odds includes odds signal evidence with 20 percent weight`() = runTest {
        viewModel = PredictionViewModel(
            savedStateHandle = SavedStateHandle(),
            matchRepository = fakeMatchRepository,
            teamRepository = fakeTeamRepository,
            oddsRepository = fakeOddsRepository,
            leagueRepository = fakeLeagueRepository,
            predictMatchOutcomeUseCase = predictMatchOutcomeUseCase
        )

        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is PredictionUiState.Success)
        val success = state as PredictionUiState.Success

        val oddsEvidence = success.predictionResult.evidence?.signals?.find { it.name == "EU Odds" }
        assertNotNull(oddsEvidence)
        assertTrue(oddsEvidence!!.isAvailable)
        assertEquals(0.20, oddsEvidence.rawWeight, 0.001)

        job.cancel()
    }

    @Test
    fun `prediction with AH and OU in matchOdds only consumes EU for scoring`() = runTest {
        val multiMarketOdds = MatchOdds(
            matchId = 101L,
            oddsList = listOf(
                OddsRecordItem(companyId = 1, companyName = "Bet365", oddsType = "eu", homeWin = 1.85, draw = 3.40, awayWin = 4.20, marketPhase = "immediate"),
                OddsRecordItem(companyId = 1, companyName = "Bet365", oddsType = "asia", handicap = -0.5, homeWin = 1.90, awayWin = 1.95, marketPhase = "immediate"),
                OddsRecordItem(companyId = 1, companyName = "Bet365", oddsType = "bs", over = 1.85, under = 1.95, marketPhase = "immediate")
            )
        )
        fakeOddsRepository.setMatchOdds(101L, multiMarketOdds)

        viewModel = PredictionViewModel(
            savedStateHandle = SavedStateHandle(),
            matchRepository = fakeMatchRepository,
            teamRepository = fakeTeamRepository,
            oddsRepository = fakeOddsRepository,
            leagueRepository = fakeLeagueRepository,
            predictMatchOutcomeUseCase = predictMatchOutcomeUseCase
        )

        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is PredictionUiState.Success)
        val success = state as PredictionUiState.Success

        // Scoring must strictly match the EU implied probability
        val oddsEvidence = success.predictionResult.evidence?.signals?.find { it.name == "EU Odds" }
        assertNotNull(oddsEvidence)
        assertTrue(oddsEvidence!!.isAvailable)

        job.cancel()
    }

    // --- Fakes ---

    private class FakeMatchRepository : MatchRepository {
        private val matchesFlow = MutableStateFlow<List<Match>>(emptyList())
        var shouldThrowError = false
        var onRefreshCallback: ((String) -> Unit)? = null

        fun setMatches(matches: List<Match>) {
            matchesFlow.value = matches
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
        ): Flow<List<Match>> = flow {
            if (shouldThrowError) throw RuntimeException("Database query failed")
            matchesFlow.collect { matches ->
                val filtered = matches.filter { match ->
                    val dateMatch = datePrefix == null || match.startTimeDate.startsWith(datePrefix) ||
                        ((startDateUtc == null || match.startTimeDate >= startDateUtc) && (endDateUtc == null || match.startTimeDate < endDateUtc))
                    val leagueMatch = leagueId == null || match.leagueId == leagueId
                    val statusMatch = when {
                        statusFilter == PredictionStatusFilter.FINISHED -> match.status == MatchStatus.ENDED
                        isPastDate -> true
                        isFutureDate -> true
                        statusFilter == PredictionStatusFilter.LIVE_AND_UPCOMING -> match.status in listOf(MatchStatus.IN_PROGRESS, MatchStatus.SCHEDULED)
                        statusFilter == PredictionStatusFilter.LIVE -> match.status == MatchStatus.IN_PROGRESS
                        statusFilter == PredictionStatusFilter.UPCOMING -> match.status == MatchStatus.SCHEDULED
                        statusFilter == PredictionStatusFilter.ALL -> true
                        else -> true
                    }
                    val searchMatch = searchQuery.isNullOrBlank() ||
                        match.homeTeam.name.contains(searchQuery, ignoreCase = true) ||
                        match.awayTeam.name.contains(searchQuery, ignoreCase = true) ||
                        (match.leagueName != null && match.leagueName!!.contains(searchQuery, ignoreCase = true))
                    dateMatch && leagueMatch && statusMatch && searchMatch
                }.take(limit)
                emit(filtered)
            }
        }

        var refreshResult: Result<Unit> = Result.success(Unit)

        override suspend fun refreshMatchesForDate(date: String): Result<Unit> {
            onRefreshCallback?.invoke(date)
            return refreshResult
        }

        override fun getMatches(date: String): Flow<List<Match>> = flow { emit(matchesFlow.value) }
        override fun getMatchDetail(matchId: Long): Flow<Match?> = flow { emit(matchesFlow.value.find { it.id == matchId }) }
        override fun getRecentMatchesForTeam(teamId: Int, limit: Int): Flow<List<Match>> = flow { emit(emptyList()) }
        override fun getMatchesByLeagueAndSeason(leagueId: Int, season: String): Flow<List<Match>> = flow { emit(emptyList()) }
        override fun getH2HMatches(teamAId: Int, teamBId: Int): Flow<List<Match>> = flow { emit(emptyList()) }
        override fun getAllMatches(): Flow<List<Match>> = flow { emit(matchesFlow.value) }
        override fun getPredictableMatches(limit: Int): Flow<List<Match>> = flow { emit(matchesFlow.value.take(limit)) }
        override fun searchMatches(query: String, limit: Int): Flow<List<Match>> = flow {
            matchesFlow.collect { matches ->
                emit(matches.filter {
                    it.homeTeam.name.contains(query, ignoreCase = true) || it.awayTeam.name.contains(query, ignoreCase = true)
                }.take(limit))
            }
        }
    }

    private class FakeTeamRepository : TeamRepository {
        private val teamsFlow = MutableStateFlow<List<TeamDetail>>(emptyList())
        fun setTeams(teams: List<TeamDetail>) { teamsFlow.value = teams }
        override fun getTeamDetail(teamId: Int): Flow<TeamDetail?> = flow { emit(teamsFlow.value.find { it.id == teamId }) }
        override fun getTeams(limit: Int): Flow<List<TeamDetail>> = flow { teamsFlow.collect { emit(it.take(limit)) } }
        override fun searchTeams(query: String, limit: Int): Flow<List<TeamDetail>> = flow {
            teamsFlow.collect { list ->
                val filtered = if (query.isBlank()) list else list.filter { it.name.contains(query, ignoreCase = true) }
                emit(filtered.take(limit))
            }
        }
        override fun getSeasonRanking(matchId: Long): Flow<List<SeasonRanking>> = flow { emit(emptyList()) }
        override fun getSeasonRankings(): Flow<List<SeasonRanking>> = flow { emit(emptyList()) }
    }

    private class FakeOddsRepository : OddsRepository {
        private val matchOddsMap = mutableMapOf<Long, MatchOdds>()
        val fetchedMatchIds = mutableListOf<Long>()

        fun setMatchOdds(matchId: Long, matchOdds: MatchOdds) { matchOddsMap[matchId] = matchOdds }
        override fun getMatchOdds(matchId: Long): Flow<MatchOdds> = flow { emit(matchOddsMap[matchId] ?: MatchOdds(matchId, emptyList())) }
        override fun getOddsHistory(matchId: Long, companyId: Int?, oddsType: String?): Flow<List<OddsRecordItem>> = flow { emit(emptyList()) }
        override suspend fun getLatestEuropeanOddsMap(): Map<Long, OddsRecordItem> = emptyMap()
        override suspend fun fetchAndCacheOddsForMatch(matchId: Long): Result<Unit> {
            fetchedMatchIds.add(matchId)
            return Result.success(Unit)
        }
    }

    private class FakeLeagueRepository : LeagueRepository {
        private val leaguesFlow = MutableStateFlow<List<League>>(emptyList())
        fun setLeagues(leagues: List<League>) { leaguesFlow.value = leagues }
        override fun getLeagues(): Flow<List<League>> = flow { leaguesFlow.collect { emit(it) } }
        override fun getLeagueDetail(leagueId: Int): Flow<League?> = flow { emit(leaguesFlow.value.find { it.id == leagueId }) }
        override fun getSeasons(leagueId: Int): Flow<List<Season>> = flow { emit(emptyList()) }
    }
}
