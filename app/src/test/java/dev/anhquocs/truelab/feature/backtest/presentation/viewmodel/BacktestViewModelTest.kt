package dev.anhquocs.truelab.feature.backtest.presentation.viewmodel

import dev.anhquocs.truelab.core.domain.evaluation.usecase.CalculateEvaluationMetricsUseCase
import dev.anhquocs.truelab.core.domain.evaluation.usecase.RunDailyBacktestUseCase
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.domain.match.model.TeamSummary
import dev.anhquocs.truelab.core.domain.odds.model.MatchOdds
import dev.anhquocs.truelab.core.domain.odds.model.OddsRecordItem
import dev.anhquocs.truelab.core.domain.odds.repository.OddsRepository
import dev.anhquocs.truelab.core.domain.prediction.usecase.PredictMatchOutcomeUseCase
import dev.anhquocs.truelab.core.domain.team.model.SeasonRanking
import dev.anhquocs.truelab.core.domain.team.model.TeamDetail
import dev.anhquocs.truelab.core.domain.team.repository.TeamRepository
import dev.anhquocs.truelab.core.domain.team.usecase.CalculateDynamicEloUseCase
import dev.anhquocs.truelab.feature.backtest.presentation.model.BacktestFilter
import dev.anhquocs.truelab.feature.backtest.presentation.model.BacktestUiState
import dev.anhquocs.truelab.feature.match.presentation.viewmodel.fakes.FakeMatchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BacktestViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeMatchRepository: FakeMatchRepository
    private lateinit var fakeTeamRepository: FakeBacktestTeamRepository
    private lateinit var fakeOddsRepository: FakeBacktestOddsRepository
    private lateinit var runDailyBacktestUseCase: RunDailyBacktestUseCase

    private val arsenal = TeamDetail(id = 1, name = "Arsenal", eloRating = 1900.0)
    private val chelsea = TeamDetail(id = 2, name = "Chelsea", eloRating = 1800.0)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeMatchRepository = FakeMatchRepository()
        fakeTeamRepository = FakeBacktestTeamRepository()
        fakeOddsRepository = FakeBacktestOddsRepository()
        fakeTeamRepository.setTeams(listOf(arsenal, chelsea))

        runDailyBacktestUseCase = RunDailyBacktestUseCase(
            oddsRepository = fakeOddsRepository,
            predictMatchOutcomeUseCase = PredictMatchOutcomeUseCase(),
            calculateDynamicEloUseCase = CalculateDynamicEloUseCase(),
            calculateEvaluationMetricsUseCase = CalculateEvaluationMetricsUseCase(),
            maxConcurrency = 3
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_withDayFtMatches_emitsIdleStateWithCount() = runTest(testDispatcher) {
        val matches = listOf(
            Match(
                id = 101L,
                homeTeam = TeamSummary(1, "Arsenal"),
                awayTeam = TeamSummary(2, "Chelsea"),
                homeScore = 2,
                awayScore = 0,
                startTimeDate = "2026-10-02T15:00:00",
                status = MatchStatus.ENDED
            )
        )
        fakeMatchRepository.emit(matches)

        val viewModel = BacktestViewModel(
            matchRepository = fakeMatchRepository,
            teamRepository = fakeTeamRepository,
            runDailyBacktestUseCase = runDailyBacktestUseCase,
            defaultDispatcher = testDispatcher
        )

        backgroundScope.launch {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Idle state but was $state", state is BacktestUiState.Idle)
        assertEquals(1, (state as BacktestUiState.Idle).availableFtMatchesCount)
    }

    @Test
    fun runBacktest_executesEvaluationAndEmitsSuccess() = runTest(testDispatcher) {
        val matches = listOf(
            Match(
                id = 101L,
                homeTeam = TeamSummary(1, "Arsenal"),
                awayTeam = TeamSummary(2, "Chelsea"),
                homeScore = 2,
                awayScore = 0,
                startTimeDate = "2026-10-02T15:00:00",
                status = MatchStatus.ENDED
            )
        )
        fakeMatchRepository.emit(matches)

        val viewModel = BacktestViewModel(
            matchRepository = fakeMatchRepository,
            teamRepository = fakeTeamRepository,
            runDailyBacktestUseCase = runDailyBacktestUseCase,
            defaultDispatcher = testDispatcher
        )

        backgroundScope.launch {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.runBacktest()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Success state but was $state", state is BacktestUiState.Success)
        val success = state as BacktestUiState.Success
        assertEquals(1, success.overview.totalMatches)
        assertEquals(1, success.allMatches.size)
    }

    @Test
    fun onFilterSelected_updatesFilteredMatches() = runTest(testDispatcher) {
        val matches = listOf(
            Match(
                id = 101L,
                homeTeam = TeamSummary(1, "Arsenal"),
                awayTeam = TeamSummary(2, "Chelsea"),
                homeScore = 2,
                awayScore = 0,
                startTimeDate = "2026-10-02T15:00:00",
                status = MatchStatus.ENDED
            )
        )
        fakeMatchRepository.emit(matches)

        val viewModel = BacktestViewModel(
            matchRepository = fakeMatchRepository,
            teamRepository = fakeTeamRepository,
            runDailyBacktestUseCase = runDailyBacktestUseCase,
            defaultDispatcher = testDispatcher
        )

        backgroundScope.launch {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.runBacktest()
        advanceUntilIdle()

        viewModel.onFilterSelected(BacktestFilter.CORRECT_ONLY)
        val state = viewModel.uiState.value as BacktestUiState.Success
        assertEquals(BacktestFilter.CORRECT_ONLY, state.selectedFilter)
    }

    @Test
    fun cancelBacktest_resetsStateToIdle() = runTest(testDispatcher) {
        val matches = listOf(
            Match(
                id = 101L,
                homeTeam = TeamSummary(1, "Arsenal"),
                awayTeam = TeamSummary(2, "Chelsea"),
                homeScore = 2,
                awayScore = 0,
                startTimeDate = "2026-10-02T15:00:00",
                status = MatchStatus.ENDED
            )
        )
        fakeMatchRepository.emit(matches)

        val viewModel = BacktestViewModel(
            matchRepository = fakeMatchRepository,
            teamRepository = fakeTeamRepository,
            runDailyBacktestUseCase = runDailyBacktestUseCase,
            defaultDispatcher = testDispatcher
        )

        backgroundScope.launch {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.cancelBacktest()
        val state = viewModel.uiState.value
        assertTrue(state is BacktestUiState.Idle)
    }

    private class FakeBacktestTeamRepository : TeamRepository {
        private val teamsFlow = MutableStateFlow<List<TeamDetail>>(emptyList())

        fun setTeams(teams: List<TeamDetail>) {
            teamsFlow.value = teams
        }

        override fun getTeamDetail(teamId: Int): Flow<TeamDetail?> = flowOf(teamsFlow.value.find { it.id == teamId })

        override fun getTeams(limit: Int): Flow<List<TeamDetail>> = flowOf(teamsFlow.value.take(limit))

        override fun searchTeams(query: String, limit: Int): Flow<List<TeamDetail>> = flowOf(emptyList())

        override fun getSeasonRanking(matchId: Long): Flow<List<SeasonRanking>> = flowOf(emptyList())

        override fun getSeasonRankings(): Flow<List<SeasonRanking>> = flowOf(emptyList())
    }

    private class FakeBacktestOddsRepository : OddsRepository {
        override fun getMatchOdds(matchId: Long): Flow<MatchOdds> = flowOf(MatchOdds(matchId = matchId, oddsList = emptyList()))

        override fun getOddsHistory(matchId: Long, companyId: Int?, oddsType: String?): Flow<List<OddsRecordItem>> = flowOf(emptyList())

        override suspend fun getLatestEuropeanOddsMap(): Map<Long, OddsRecordItem> = emptyMap()

        override suspend fun fetchAndCacheOddsForMatch(matchId: Long): Result<Unit> = Result.success(Unit)
    }
}
