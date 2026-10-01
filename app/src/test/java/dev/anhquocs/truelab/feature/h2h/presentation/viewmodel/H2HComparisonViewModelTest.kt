package dev.anhquocs.truelab.feature.h2h.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.domain.match.model.TeamSummary
import dev.anhquocs.truelab.core.domain.match.repository.MatchRepository
import dev.anhquocs.truelab.core.domain.team.model.SeasonRanking
import dev.anhquocs.truelab.core.domain.team.model.TeamDetail
import dev.anhquocs.truelab.core.domain.team.repository.TeamRepository
import dev.anhquocs.truelab.core.domain.team.usecase.CalculateHomeAwaySplitsUseCase
import dev.anhquocs.truelab.core.domain.team.usecase.CalculateTeamFormUseCase
import dev.anhquocs.truelab.core.domain.team.usecase.GetHeadToHeadComparisonUseCase
import dev.anhquocs.truelab.core.domain.team.usecase.GetTeamStatisticsUseCase
import dev.anhquocs.truelab.feature.h2h.presentation.model.H2HComparisonUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
class H2HComparisonViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeTeamRepository: FakeH2HTeamRepository
    private lateinit var fakeMatchRepository: FakeH2HMatchRepository
    private lateinit var getHeadToHeadComparisonUseCase: GetHeadToHeadComparisonUseCase
    private lateinit var viewModel: H2HComparisonViewModel

    private val arsenal = TeamDetail(id = 1, name = "Arsenal", eloRating = 1950.0)
    private val chelsea = TeamDetail(id = 2, name = "Chelsea", eloRating = 1820.0)
    private val liverpool = TeamDetail(id = 3, name = "Liverpool", eloRating = 1910.0)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeTeamRepository = FakeH2HTeamRepository()
        fakeMatchRepository = FakeH2HMatchRepository()

        fakeTeamRepository.setTeams(listOf(arsenal, chelsea, liverpool))

        val calculateTeamFormUseCase = CalculateTeamFormUseCase()
        val calculateHomeAwaySplitsUseCase = CalculateHomeAwaySplitsUseCase()
        val getTeamStatisticsUseCase = GetTeamStatisticsUseCase()

        getHeadToHeadComparisonUseCase = GetHeadToHeadComparisonUseCase(
            calculateTeamFormUseCase = calculateTeamFormUseCase,
            calculateHomeAwaySplitsUseCase = calculateHomeAwaySplitsUseCase,
            getTeamStatisticsUseCase = getTeamStatisticsUseCase
        )

        viewModel = H2HComparisonViewModel(
            savedStateHandle = SavedStateHandle(),
            teamRepository = fakeTeamRepository,
            matchRepository = fakeMatchRepository,
            getHeadToHeadComparisonUseCase = getHeadToHeadComparisonUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_requiresSelection() = runTest(testDispatcher) {
        backgroundScope.launch {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        assertNull(viewModel.selectedTeamAId.value)
        assertNull(viewModel.selectedTeamBId.value)
        assertTrue(viewModel.uiState.value is H2HComparisonUiState.TeamSelectionRequired)
    }

    @Test
    fun selectTeamAOnly_remainsInSelectionRequired() = runTest(testDispatcher) {
        backgroundScope.launch {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.selectTeamA(1)
        advanceUntilIdle()

        assertEquals(1, viewModel.selectedTeamAId.value)
        assertNull(viewModel.selectedTeamBId.value)
        assertTrue(viewModel.uiState.value is H2HComparisonUiState.TeamSelectionRequired)
    }

    @Test
    fun selectBothDifferentTeams_loadsAndCalculatesComparisonSuccessfully() = runTest(testDispatcher) {
        val h2hMatch = Match(
            id = 101L,
            homeTeam = TeamSummary(1, "Arsenal"),
            awayTeam = TeamSummary(2, "Chelsea"),
            homeScore = 2,
            awayScore = 1,
            startTimeDate = "2026-03-01T15:00:00",
            status = MatchStatus.ENDED
        )
        fakeMatchRepository.setH2HMatches(1, 2, listOf(h2hMatch))
        fakeMatchRepository.setRecentMatches(1, listOf(h2hMatch))
        fakeMatchRepository.setRecentMatches(2, listOf(h2hMatch))

        backgroundScope.launch {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.selectTeamA(1)
        viewModel.selectTeamB(2)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Success state but was $state", state is H2HComparisonUiState.Success)
        val success = state as H2HComparisonUiState.Success
        assertEquals("Arsenal", success.teamA.name)
        assertEquals("Chelsea", success.teamB.name)
        assertEquals(1, success.comparison.totalMatches)
        assertEquals(1, success.comparison.teamAWins)
        assertEquals(0, success.comparison.teamBWins)
    }

    @Test
    fun swapTeams_swapsAAndBAndRecalculates() = runTest(testDispatcher) {
        val h2hMatch = Match(
            id = 101L,
            homeTeam = TeamSummary(1, "Arsenal"),
            awayTeam = TeamSummary(2, "Chelsea"),
            homeScore = 2,
            awayScore = 1,
            startTimeDate = "2026-03-01T15:00:00",
            status = MatchStatus.ENDED
        )
        fakeMatchRepository.setH2HMatches(1, 2, listOf(h2hMatch))
        fakeMatchRepository.setRecentMatches(1, listOf(h2hMatch))
        fakeMatchRepository.setRecentMatches(2, listOf(h2hMatch))

        backgroundScope.launch {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.selectTeamA(1)
        viewModel.selectTeamB(2)
        advanceUntilIdle()

        viewModel.swapTeams()
        advanceUntilIdle()

        assertEquals(2, viewModel.selectedTeamAId.value)
        assertEquals(1, viewModel.selectedTeamBId.value)

        val state = viewModel.uiState.value
        assertTrue("Expected Success state but was $state", state is H2HComparisonUiState.Success)
        val success = state as H2HComparisonUiState.Success
        assertEquals("Chelsea", success.teamA.name)
        assertEquals("Arsenal", success.teamB.name)
        assertEquals(0, success.comparison.teamAWins)
        assertEquals(1, success.comparison.teamBWins)
    }

    @Test
    fun clearSelection_resetsToTeamSelectionRequired() = runTest(testDispatcher) {
        backgroundScope.launch {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.selectTeamA(1)
        viewModel.selectTeamB(2)
        advanceUntilIdle()

        viewModel.clearSelection()
        advanceUntilIdle()

        assertNull(viewModel.selectedTeamAId.value)
        assertNull(viewModel.selectedTeamBId.value)
        assertTrue(viewModel.uiState.value is H2HComparisonUiState.TeamSelectionRequired)
    }

    @Test
    fun searchQuery_blank_emitsDefaultTeamsFromGetTeams() = runTest(testDispatcher) {
        backgroundScope.launch {
            viewModel.searchResults.collect {}
        }
        advanceUntilIdle()

        assertEquals(3, viewModel.searchResults.value.size)
        assertEquals("Arsenal", viewModel.searchResults.value[0].name)
        assertEquals("Chelsea", viewModel.searchResults.value[1].name)
        assertEquals("Liverpool", viewModel.searchResults.value[2].name)
    }

    @Test
    fun searchQuery_withArsenal_callsSearchTeamsAndReturnsArsenal() = runTest(testDispatcher) {
        backgroundScope.launch {
            viewModel.searchResults.collect {}
        }
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("arsenal")
        testScheduler.advanceTimeBy(350)
        advanceUntilIdle()

        assertEquals("arsenal", viewModel.searchQuery.value)
        val results = viewModel.searchResults.value
        assertEquals(1, results.size)
        assertEquals("Arsenal", results[0].name)
    }

    @Test
    fun searchQuery_withVietnam_returnsVietnamOutsideFirst100() = runTest(testDispatcher) {
        val vietnam = TeamDetail(id = 127052, name = "Vietnam", eloRating = 1500.0)
        fakeTeamRepository.setTeams(listOf(arsenal, chelsea, liverpool, vietnam))

        backgroundScope.launch {
            viewModel.searchResults.collect {}
        }
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("vie")
        testScheduler.advanceTimeBy(350)
        advanceUntilIdle()

        val results = viewModel.searchResults.value
        assertEquals(1, results.size)
        assertEquals("Vietnam", results[0].name)
        assertEquals(127052, results[0].id)
    }

    @Test
    fun searchQuery_withPakistan_returnsPakistan() = runTest(testDispatcher) {
        val pakistan = TeamDetail(id = 137359, name = "Pakistan", eloRating = 1500.0)
        fakeTeamRepository.setTeams(listOf(arsenal, chelsea, liverpool, pakistan))

        backgroundScope.launch {
            viewModel.searchResults.collect {}
        }
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("pak")
        testScheduler.advanceTimeBy(350)
        advanceUntilIdle()

        val results = viewModel.searchResults.value
        assertEquals(1, results.size)
        assertEquals("Pakistan", results[0].name)
        assertEquals(137359, results[0].id)
    }

    @Test
    fun selectTeam_outsideFirst100_resolvesTeamDetailAndSuccess() = runTest(testDispatcher) {
        val vietnam = TeamDetail(id = 127052, name = "Vietnam", eloRating = 1500.0)
        val pakistan = TeamDetail(id = 137359, name = "Pakistan", eloRating = 1500.0)
        fakeTeamRepository.setTeams(listOf(arsenal, chelsea, liverpool, vietnam, pakistan))

        val h2hMatch = Match(
            id = 908567L,
            homeTeam = TeamSummary(127052, "Vietnam"),
            awayTeam = TeamSummary(137359, "Pakistan"),
            homeScore = 0,
            awayScore = 0,
            startTimeDate = "2026-10-02T16:00:00",
            status = MatchStatus.SCHEDULED
        )
        fakeMatchRepository.setH2HMatches(127052, 137359, listOf(h2hMatch))
        fakeMatchRepository.setRecentMatches(127052, listOf(h2hMatch))
        fakeMatchRepository.setRecentMatches(137359, listOf(h2hMatch))

        backgroundScope.launch { viewModel.uiState.collect {} }
        backgroundScope.launch { viewModel.selectedTeamADetail.collect {} }
        backgroundScope.launch { viewModel.selectedTeamBDetail.collect {} }
        advanceUntilIdle()

        viewModel.selectTeamA(127052)
        viewModel.selectTeamB(137359)
        advanceUntilIdle()

        assertEquals("Vietnam", viewModel.selectedTeamADetail.value?.name)
        assertEquals("Pakistan", viewModel.selectedTeamBDetail.value?.name)

        val state = viewModel.uiState.value
        assertTrue("Expected Success state but was $state", state is H2HComparisonUiState.Success)
        val success = state as H2HComparisonUiState.Success
        assertEquals("Vietnam", success.teamA.name)
        assertEquals("Pakistan", success.teamB.name)
    }
    @Test
    fun dynamicElo_replaysHistoricalMatches_updatesTeamEloRatings() = runTest(testDispatcher) {
        val vietnam = TeamDetail(id = 127052, name = "Vietnam", eloRating = 1500.0)
        val thailand = TeamDetail(id = 127053, name = "Thailand", eloRating = 1500.0)
        fakeTeamRepository.setTeams(listOf(arsenal, chelsea, liverpool, vietnam, thailand))

        val match1 = Match(
            id = 501L,
            homeTeam = TeamSummary(127052, "Vietnam"),
            awayTeam = TeamSummary(127053, "Thailand"),
            homeScore = 2,
            awayScore = 0,
            startTimeDate = "2026-05-01T15:00:00",
            status = MatchStatus.ENDED
        )
        fakeMatchRepository.setAllMatches(listOf(match1))
        fakeMatchRepository.setH2HMatches(127052, 127053, listOf(match1))
        fakeMatchRepository.setRecentMatches(127052, listOf(match1))
        fakeMatchRepository.setRecentMatches(127053, listOf(match1))

        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.selectTeamA(127052)
        viewModel.selectTeamB(127053)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Success state but was $state", state is H2HComparisonUiState.Success)
        val success = state as H2HComparisonUiState.Success
        assertTrue("Vietnam Elo must increase above 1500.0", success.teamA.eloRating > 1500.0)
        assertTrue("Thailand Elo must decrease below 1500.0", success.teamB.eloRating < 1500.0)
        assertTrue("Formatted Elo A must not be 1500.0", success.comparison.teamAElo != "1500.0")
        assertTrue("Formatted Elo B must not be 1500.0", success.comparison.teamBElo != "1500.0")
    }

    @Test
    fun homeAwaySplits_calculatedIndependentlyForBothTeams() = runTest(testDispatcher) {
        val teamAMatchHome1 = Match(
            id = 201L,
            homeTeam = TeamSummary(1, "Arsenal"),
            awayTeam = TeamSummary(99, "OtherTeamX"),
            homeScore = 2,
            awayScore = 1,
            startTimeDate = "2026-03-01T15:00:00",
            status = MatchStatus.ENDED
        )
        val teamAMatchHome2 = Match(
            id = 202L,
            homeTeam = TeamSummary(1, "Arsenal"),
            awayTeam = TeamSummary(98, "OtherTeamY"),
            homeScore = 1,
            awayScore = 1,
            startTimeDate = "2026-03-05T15:00:00",
            status = MatchStatus.ENDED
        )
        val teamAMatchAway1 = Match(
            id = 203L,
            homeTeam = TeamSummary(97, "OtherTeamZ"),
            awayTeam = TeamSummary(1, "Arsenal"),
            homeScore = 1,
            awayScore = 2,
            startTimeDate = "2026-03-10T15:00:00",
            status = MatchStatus.ENDED
        )
        val teamAMatchAway2 = Match(
            id = 204L,
            homeTeam = TeamSummary(96, "OtherTeamW"),
            awayTeam = TeamSummary(1, "Arsenal"),
            homeScore = 3,
            awayScore = 0,
            startTimeDate = "2026-03-15T15:00:00",
            status = MatchStatus.ENDED
        )
        fakeMatchRepository.setRecentMatches(1, listOf(teamAMatchHome1, teamAMatchHome2, teamAMatchAway1, teamAMatchAway2))

        val teamBMatchHome1 = Match(
            id = 301L,
            homeTeam = TeamSummary(2, "Chelsea"),
            awayTeam = TeamSummary(95, "OtherTeamP"),
            homeScore = 3,
            awayScore = 0,
            startTimeDate = "2026-03-02T15:00:00",
            status = MatchStatus.ENDED
        )
        val teamBMatchAway1 = Match(
            id = 302L,
            homeTeam = TeamSummary(94, "OtherTeamQ"),
            awayTeam = TeamSummary(2, "Chelsea"),
            homeScore = 0,
            awayScore = 1,
            startTimeDate = "2026-03-06T15:00:00",
            status = MatchStatus.ENDED
        )
        val teamBMatchAway2 = Match(
            id = 303L,
            homeTeam = TeamSummary(93, "OtherTeamR"),
            awayTeam = TeamSummary(2, "Chelsea"),
            homeScore = 2,
            awayScore = 0,
            startTimeDate = "2026-03-12T15:00:00",
            status = MatchStatus.ENDED
        )
        fakeMatchRepository.setRecentMatches(2, listOf(teamBMatchHome1, teamBMatchAway1, teamBMatchAway2))

        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.selectTeamA(1)
        viewModel.selectTeamB(2)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Success state but was $state", state is H2HComparisonUiState.Success)
        val success = state as H2HComparisonUiState.Success

        // Team A (Arsenal)
        assertEquals("1W - 1D - 0L", success.comparison.teamAHomeRecord)
        assertEquals("50%", success.comparison.teamAHomeWinRate)
        assertEquals("1W - 0D - 1L", success.comparison.teamAAwayRecord)
        assertEquals("50%", success.comparison.teamAAwayWinRate)

        // Team B (Chelsea)
        assertEquals("1W - 0D - 0L", success.comparison.teamBHomeRecord)
        assertEquals("100%", success.comparison.teamBHomeWinRate)
        assertEquals("1W - 0D - 1L", success.comparison.teamBAwayRecord)
        assertEquals("50%", success.comparison.teamBAwayWinRate)
    }

    @Test
    fun h2hClashes_isolatedFromGeneralTeamHistory() = runTest(testDispatcher) {
        val generalMatchA = Match(
            id = 401L,
            homeTeam = TeamSummary(1, "Arsenal"),
            awayTeam = TeamSummary(99, "OtherTeamX"),
            homeScore = 3,
            awayScore = 0,
            startTimeDate = "2026-03-01T15:00:00",
            status = MatchStatus.ENDED
        )
        val h2hClash = Match(
            id = 402L,
            homeTeam = TeamSummary(1, "Arsenal"),
            awayTeam = TeamSummary(2, "Chelsea"),
            homeScore = 2,
            awayScore = 1,
            startTimeDate = "2026-03-10T15:00:00",
            status = MatchStatus.ENDED
        )

        fakeMatchRepository.setRecentMatches(1, listOf(generalMatchA, h2hClash))
        fakeMatchRepository.setRecentMatches(2, listOf(h2hClash))
        fakeMatchRepository.setH2HMatches(1, 2, listOf(h2hClash))

        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.selectTeamA(1)
        viewModel.selectTeamB(2)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Success state but was $state", state is H2HComparisonUiState.Success)
        val success = state as H2HComparisonUiState.Success

        // Direct H2H must only reflect the 1 clash between Arsenal & Chelsea
        assertEquals(1, success.comparison.totalMatches)
        assertEquals(1, success.comparison.teamAWins)
        assertEquals(0, success.comparison.teamBWins)
        assertEquals(0, success.comparison.draws)
    }

    @Test
    fun edgeCases_emptyMatchesAndNoHistory_handlesGracefully() = runTest(testDispatcher) {
        val teamX = TeamDetail(id = 1001, name = "EmptyTeamX", eloRating = 1500.0)
        val teamY = TeamDetail(id = 1002, name = "EmptyTeamY", eloRating = 1500.0)
        fakeTeamRepository.setTeams(listOf(teamX, teamY))

        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.selectTeamA(1001)
        viewModel.selectTeamB(1002)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Success state but was $state", state is H2HComparisonUiState.Success)
        val success = state as H2HComparisonUiState.Success

        assertEquals(0, success.comparison.totalMatches)
        assertEquals("1500.0", success.comparison.teamAElo)
        assertEquals("1500.0", success.comparison.teamBElo)
        assertEquals("0W - 0D - 0L", success.comparison.teamAHomeRecord)
        assertEquals("0%", success.comparison.teamAHomeWinRate)
        assertEquals("0W - 0D - 0L", success.comparison.teamAAwayRecord)
        assertEquals("0%", success.comparison.teamAAwayWinRate)
        assertTrue(success.comparison.teamARecentMatches.isEmpty())
        assertTrue(success.comparison.teamBRecentMatches.isEmpty())
    }

    @Test
    fun recentMatches_forBothTeams_populatedCorrectlyAndIndependentOfH2H() = runTest(testDispatcher) {
        val teamAMatches = (1..5).map { i ->
            Match(
                id = 1000L + i,
                homeTeam = TeamSummary(1, "Arsenal"),
                awayTeam = TeamSummary(10 + i, "OpponentA$i"),
                homeScore = if (i == 1) 3 else 1, // i=1 (newest): W (3-0), others: D (1-1)
                awayScore = if (i == 1) 0 else 1,
                startTimeDate = "2026-04-0$i" + "T15:00:00",
                status = MatchStatus.ENDED
            )
        }

        val teamBMatches = (1..5).map { i ->
            Match(
                id = 2000L + i,
                homeTeam = TeamSummary(20 + i, "OpponentB$i"),
                awayTeam = TeamSummary(2, "Chelsea"),
                homeScore = 2,
                awayScore = if (i == 5) 3 else 0, // i=5 (newest date): Away Win (2-3)
                startTimeDate = "2026-04-0$i" + "T17:00:00",
                status = MatchStatus.ENDED
            )
        }

        fakeMatchRepository.setRecentMatches(1, teamAMatches)
        fakeMatchRepository.setRecentMatches(2, teamBMatches)
        fakeMatchRepository.setH2HMatches(1, 2, emptyList())

        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.selectTeamA(1)
        viewModel.selectTeamB(2)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Success state but was $state", state is H2HComparisonUiState.Success)
        val success = state as H2HComparisonUiState.Success

        // Both teams should have 5 recent matches in UI
        assertEquals(5, success.comparison.teamARecentMatches.size)
        assertEquals(5, success.comparison.teamBRecentMatches.size)

        // Oldest match should be at index 0 for Team A (2026-04-01), newest at index 4 (2026-04-05)
        assertEquals("2026-04-01", success.comparison.teamARecentMatches[0].date)
        assertEquals("OpponentA1", success.comparison.teamARecentMatches[0].opponentName)
        assertEquals("2026-04-05", success.comparison.teamARecentMatches[4].date)
        assertEquals("OpponentA5", success.comparison.teamARecentMatches[4].opponentName)

        // Oldest match should be at index 0 for Team B (2026-04-01), newest at index 4 (2026-04-05)
        assertEquals("2026-04-01", success.comparison.teamBRecentMatches[0].date)
        assertEquals("OpponentB1", success.comparison.teamBRecentMatches[0].opponentName)
        assertEquals("2026-04-05", success.comparison.teamBRecentMatches[4].date)
        assertEquals("OpponentB5", success.comparison.teamBRecentMatches[4].opponentName)
        assertEquals("W", success.comparison.teamBRecentMatches[4].result) // Chelsea won 3-2 away

        // H2H match count remains 0 (independent)
        assertEquals(0, success.comparison.totalMatches)
    }

    @Test
    fun recentMatches_fewerThan5Matches_displaysActualCount() = runTest(testDispatcher) {
        val teamAMatches = (1..3).map { i ->
            Match(
                id = 3000L + i,
                homeTeam = TeamSummary(1, "Arsenal"),
                awayTeam = TeamSummary(30 + i, "Opponent$i"),
                homeScore = 2,
                awayScore = 0,
                startTimeDate = "2026-03-0$i" + "T15:00:00",
                status = MatchStatus.ENDED
            )
        }

        fakeMatchRepository.setRecentMatches(1, teamAMatches)
        fakeMatchRepository.setRecentMatches(2, emptyList())

        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.selectTeamA(1)
        viewModel.selectTeamB(2)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Success state but was $state", state is H2HComparisonUiState.Success)
        val success = state as H2HComparisonUiState.Success

        assertEquals(3, success.comparison.teamARecentMatches.size)
        assertEquals(0, success.comparison.teamBRecentMatches.size)
    }
}

private class FakeH2HTeamRepository : TeamRepository {
    private val teamsFlow = MutableStateFlow<List<TeamDetail>>(emptyList())

    fun setTeams(teams: List<TeamDetail>) {
        teamsFlow.value = teams
    }

    override fun getTeams(limit: Int): Flow<List<TeamDetail>> = teamsFlow.map { list ->
        list.take(limit)
    }

    override fun searchTeams(query: String, limit: Int): Flow<List<TeamDetail>> = teamsFlow.map { list ->
        val filtered = if (query.isBlank()) list else list.filter { it.name.contains(query, ignoreCase = true) }
        filtered.take(limit)
    }

    override fun getTeamDetail(teamId: Int): Flow<TeamDetail?> = teamsFlow.map { list ->
        list.firstOrNull { it.id == teamId }
    }

    override fun getSeasonRanking(matchId: Long): Flow<List<SeasonRanking>> = flowOf(emptyList())

    override fun getSeasonRankings(): Flow<List<SeasonRanking>> = flowOf(emptyList())
}

private class FakeH2HMatchRepository : MatchRepository {
    private val allMatchesFlow = MutableStateFlow<List<Match>>(emptyList())
    private val recentMatchesMap = mutableMapOf<Int, List<Match>>()
    private val h2hMatchesMap = mutableMapOf<Pair<Int, Int>, List<Match>>()

    fun setAllMatches(matches: List<Match>) {
        allMatchesFlow.value = matches
    }

    fun setRecentMatches(teamId: Int, matches: List<Match>) {
        recentMatchesMap[teamId] = matches
    }

    fun setH2HMatches(teamAId: Int, teamBId: Int, matches: List<Match>) {
        h2hMatchesMap[Pair(teamAId, teamBId)] = matches
        h2hMatchesMap[Pair(teamBId, teamAId)] = matches
    }

    override fun getH2HMatches(teamAId: Int, teamBId: Int): Flow<List<Match>> = flow {
        emit(h2hMatchesMap[Pair(teamAId, teamBId)] ?: emptyList())
    }

    override fun getRecentMatchesForTeam(teamId: Int, limit: Int): Flow<List<Match>> = flow {
        val matches = recentMatchesMap[teamId] ?: emptyList()
        emit(matches.take(limit))
    }

    override fun getMatches(date: String): Flow<List<Match>> = flowOf(emptyList())

    override fun getMatchDetail(matchId: Long): Flow<Match?> = flowOf(null)

    override fun getMatchesByLeagueAndSeason(leagueId: Int, season: String): Flow<List<Match>> = flowOf(emptyList())

    override fun getAllMatches(): Flow<List<Match>> = allMatchesFlow

    override fun getPredictableMatches(limit: Int): Flow<List<Match>> = flowOf(emptyList())

    override fun searchMatches(query: String, limit: Int): Flow<List<Match>> = flowOf(emptyList())

    override fun getPredictableMatchesFiltered(
        startDateUtc: String?,
        endDateUtc: String?,
        isPastDate: Boolean,
        isFutureDate: Boolean,
        datePrefix: String?,
        leagueId: Int?,
        statusFilter: dev.anhquocs.truelab.core.domain.match.model.PredictionStatusFilter,
        searchQuery: String?,
        limit: Int
    ): Flow<List<Match>> = flowOf(emptyList())

    override suspend fun refreshMatchesForDate(date: String): Result<Unit> = Result.success(Unit)
}
