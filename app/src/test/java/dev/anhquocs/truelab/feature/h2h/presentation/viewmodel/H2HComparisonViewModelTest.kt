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
}

private class FakeH2HTeamRepository : TeamRepository {
    private val teamsFlow = MutableStateFlow<List<TeamDetail>>(emptyList())

    fun setTeams(teams: List<TeamDetail>) {
        teamsFlow.value = teams
    }

    override fun getTeams(): Flow<List<TeamDetail>> = teamsFlow

    override fun getTeamDetail(teamId: Int): Flow<TeamDetail?> = flow {
        emit(teamsFlow.value.firstOrNull { it.id == teamId })
    }

    override fun getSeasonRanking(matchId: Long): Flow<List<SeasonRanking>> = flowOf(emptyList())

    override fun getSeasonRankings(): Flow<List<SeasonRanking>> = flowOf(emptyList())
}

private class FakeH2HMatchRepository : MatchRepository {
    private val recentMatchesMap = mutableMapOf<Int, List<Match>>()
    private val h2hMatchesMap = mutableMapOf<Pair<Int, Int>, List<Match>>()

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

    override fun getAllMatches(): Flow<List<Match>> = flowOf(emptyList())
}
