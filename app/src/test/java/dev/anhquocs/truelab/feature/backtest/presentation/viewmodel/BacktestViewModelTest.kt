package dev.anhquocs.truelab.feature.backtest.presentation.viewmodel

import dev.anhquocs.truelab.core.domain.evaluation.usecase.BacktestPredictionUseCase
import dev.anhquocs.truelab.core.domain.evaluation.usecase.CalculateEvaluationMetricsUseCase
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.domain.match.model.TeamSummary
import dev.anhquocs.truelab.core.domain.prediction.usecase.PredictMatchOutcomeUseCase
import dev.anhquocs.truelab.core.domain.team.model.SeasonRanking
import dev.anhquocs.truelab.core.domain.odds.model.MatchOdds
import dev.anhquocs.truelab.core.domain.odds.model.OddsRecordItem
import dev.anhquocs.truelab.core.domain.odds.repository.OddsRepository
import dev.anhquocs.truelab.core.domain.team.model.TeamDetail
import dev.anhquocs.truelab.core.domain.team.repository.TeamRepository
import dev.anhquocs.truelab.feature.backtest.presentation.model.BacktestFilter
import dev.anhquocs.truelab.feature.backtest.presentation.model.BacktestUiState
import dev.anhquocs.truelab.feature.match.presentation.viewmodel.fakes.FakeMatchRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BacktestViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeMatchRepository: FakeMatchRepository
    private lateinit var fakeTeamRepository: FakeBacktestTeamRepository
    private lateinit var fakeOddsRepository: FakeBacktestOddsRepository
    private lateinit var backtestPredictionUseCase: BacktestPredictionUseCase

    private val arsenal = TeamDetail(id = 1, name = "Arsenal", eloRating = 1900.0)
    private val chelsea = TeamDetail(id = 2, name = "Chelsea", eloRating = 1800.0)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeMatchRepository = FakeMatchRepository()
        fakeTeamRepository = FakeBacktestTeamRepository()
        fakeOddsRepository = FakeBacktestOddsRepository()
        fakeTeamRepository.setTeams(listOf(arsenal, chelsea))

        backtestPredictionUseCase = BacktestPredictionUseCase(
            predictMatchOutcomeUseCase = PredictMatchOutcomeUseCase(),
            calculateEvaluationMetricsUseCase = CalculateEvaluationMetricsUseCase()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_withHistoricalMatches_executesBacktestAndEmitsSuccess() = runTest(testDispatcher) {
        val matches = listOf(
            Match(
                id = 101L,
                homeTeam = TeamSummary(1, "Arsenal"),
                awayTeam = TeamSummary(2, "Chelsea"),
                homeScore = 2,
                awayScore = 0,
                startTimeDate = "2026-01-01T15:00:00",
                status = MatchStatus.ENDED
            ),
            Match(
                id = 102L,
                homeTeam = TeamSummary(2, "Chelsea"),
                awayTeam = TeamSummary(1, "Arsenal"),
                homeScore = 1,
                awayScore = 1,
                startTimeDate = "2026-02-01T15:00:00",
                status = MatchStatus.ENDED
            )
        )
        fakeMatchRepository.emit(matches)

        val viewModel = BacktestViewModel(
            matchRepository = fakeMatchRepository,
            teamRepository = fakeTeamRepository,
            oddsRepository = fakeOddsRepository,
            backtestPredictionUseCase = backtestPredictionUseCase,
            defaultDispatcher = testDispatcher
        )

        backgroundScope.launch {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Success state but was $state", state is BacktestUiState.Success)
        val success = state as BacktestUiState.Success
        assertEquals(2, success.overview.totalMatches)
        assertEquals(2, success.allMatches.size)
        assertEquals(BacktestFilter.ALL, success.selectedFilter)
    }

    @Test
    fun init_withEmptyMatches_emitsEmptyState() = runTest(testDispatcher) {
        fakeMatchRepository.emit(emptyList())

        val viewModel = BacktestViewModel(
            matchRepository = fakeMatchRepository,
            teamRepository = fakeTeamRepository,
            oddsRepository = fakeOddsRepository,
            backtestPredictionUseCase = backtestPredictionUseCase,
            defaultDispatcher = testDispatcher
        )

        backgroundScope.launch {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Empty state but was $state", state is BacktestUiState.Empty)
    }

    @Test
    fun onFilterSelected_filtersMatchesAccordingly() = runTest(testDispatcher) {
        val matches = listOf(
            Match(
                id = 101L,
                homeTeam = TeamSummary(1, "Arsenal"),
                awayTeam = TeamSummary(2, "Chelsea"),
                homeScore = 2,
                awayScore = 0,
                startTimeDate = "2026-01-01T15:00:00",
                status = MatchStatus.ENDED
            ),
            Match(
                id = 102L,
                homeTeam = TeamSummary(2, "Chelsea"),
                awayTeam = TeamSummary(1, "Arsenal"),
                homeScore = 0,
                awayScore = 3,
                startTimeDate = "2026-02-01T15:00:00",
                status = MatchStatus.ENDED
            )
        )
        fakeMatchRepository.emit(matches)

        val viewModel = BacktestViewModel(
            matchRepository = fakeMatchRepository,
            teamRepository = fakeTeamRepository,
            oddsRepository = fakeOddsRepository,
            backtestPredictionUseCase = backtestPredictionUseCase,
            defaultDispatcher = testDispatcher
        )

        backgroundScope.launch {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is BacktestUiState.Success)

        // Filter CORRECT_ONLY
        viewModel.onFilterSelected(BacktestFilter.CORRECT_ONLY)
        val correctState = viewModel.uiState.value as BacktestUiState.Success
        assertEquals(BacktestFilter.CORRECT_ONLY, correctState.selectedFilter)
        assertTrue(correctState.filteredMatches.all { it.isCorrect })

        // Filter INCORRECT_ONLY
        viewModel.onFilterSelected(BacktestFilter.INCORRECT_ONLY)
        val incorrectState = viewModel.uiState.value as BacktestUiState.Success
        assertEquals(BacktestFilter.INCORRECT_ONLY, incorrectState.selectedFilter)
        assertTrue(incorrectState.filteredMatches.all { !it.isCorrect })

        // Filter ALL
        viewModel.onFilterSelected(BacktestFilter.ALL)
        val allState = viewModel.uiState.value as BacktestUiState.Success
        assertEquals(BacktestFilter.ALL, allState.selectedFilter)
        assertEquals(2, allState.filteredMatches.size)
    }

    @Test
    fun repositoryError_emitsErrorState() = runTest(testDispatcher) {
        fakeMatchRepository.errorToThrow = RuntimeException("Database error loading matches")

        val viewModel = BacktestViewModel(
            matchRepository = fakeMatchRepository,
            teamRepository = fakeTeamRepository,
            oddsRepository = fakeOddsRepository,
            backtestPredictionUseCase = backtestPredictionUseCase,
            defaultDispatcher = testDispatcher
        )

        backgroundScope.launch {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Error state but was $state", state is BacktestUiState.Error)
    }
}

private class FakeBacktestTeamRepository : TeamRepository {
    private val teamsFlow = MutableStateFlow<List<TeamDetail>>(emptyList())

    fun setTeams(teams: List<TeamDetail>) {
        teamsFlow.value = teams
    }

    override fun getTeams(limit: Int): Flow<List<TeamDetail>> = kotlinx.coroutines.flow.flow {
        teamsFlow.collect { list -> emit(list.take(limit)) }
    }

    override fun searchTeams(query: String, limit: Int): Flow<List<TeamDetail>> = kotlinx.coroutines.flow.flow {
        teamsFlow.collect { list ->
            val filtered = if (query.isBlank()) list else list.filter { it.name.contains(query, ignoreCase = true) }
            emit(filtered.take(limit))
        }
    }

    override fun getTeamDetail(teamId: Int): Flow<TeamDetail?> = flow {
        emit(teamsFlow.value.firstOrNull { it.id == teamId })
    }

    override fun getSeasonRanking(matchId: Long): Flow<List<SeasonRanking>> = flowOf(emptyList())

    override fun getSeasonRankings(): Flow<List<SeasonRanking>> = flowOf(emptyList())
}

private class FakeBacktestOddsRepository : OddsRepository {
    var oddsMapToReturn: Map<Long, OddsRecordItem> = emptyMap()

    override fun getMatchOdds(matchId: Long): Flow<MatchOdds> = flowOf(MatchOdds(matchId = matchId, oddsList = emptyList()))

    override fun getOddsHistory(matchId: Long, companyId: Int?, oddsType: String?): Flow<List<OddsRecordItem>> = flowOf(emptyList())

    override suspend fun getLatestEuropeanOddsMap(): Map<Long, OddsRecordItem> = oddsMapToReturn

    override suspend fun fetchAndCacheOddsForMatch(matchId: Long): Result<Unit> = Result.success(Unit)
}
