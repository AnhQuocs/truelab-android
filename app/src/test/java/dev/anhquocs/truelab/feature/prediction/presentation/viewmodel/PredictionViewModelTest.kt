package dev.anhquocs.truelab.feature.prediction.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
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

@OptIn(ExperimentalCoroutinesApi::class)
class PredictionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeMatchRepository: FakeMatchRepository
    private lateinit var fakeTeamRepository: FakeTeamRepository
    private lateinit var fakeOddsRepository: FakeOddsRepository
    private lateinit var predictMatchOutcomeUseCase: PredictMatchOutcomeUseCase
    private lateinit var viewModel: PredictionViewModel

    private val manCity = TeamDetail(id = 1, name = "Manchester City", leagueName = "Premier League", eloRating = 1980.0)
    private val arsenal = TeamDetail(id = 2, name = "Arsenal", leagueName = "Premier League", eloRating = 1945.0)
    private val liverpool = TeamDetail(id = 3, name = "Liverpool", leagueName = "Premier League", eloRating = 1920.0)

    private val match1 = Match(
        id = 101L,
        homeTeam = TeamSummary(id = 1, name = "Manchester City"),
        awayTeam = TeamSummary(id = 2, name = "Arsenal"),
        homeScore = null,
        awayScore = null,
        startTimeDate = "2026-04-01T15:00:00",
        status = MatchStatus.SCHEDULED
    )

    private val match2 = Match(
        id = 102L,
        homeTeam = TeamSummary(id = 2, name = "Arsenal"),
        awayTeam = TeamSummary(id = 3, name = "Liverpool"),
        homeScore = null,
        awayScore = null,
        startTimeDate = "2026-04-08T17:30:00",
        status = MatchStatus.SCHEDULED
    )

    private val recentMatchManCity = Match(
        id = 91L,
        homeTeam = TeamSummary(id = 1, name = "Manchester City"),
        awayTeam = TeamSummary(id = 4, name = "Chelsea"),
        homeScore = 3,
        awayScore = 0,
        startTimeDate = "2026-03-20T15:00:00",
        status = MatchStatus.ENDED
    )

    private val recentMatchArsenal = Match(
        id = 92L,
        homeTeam = TeamSummary(id = 2, name = "Arsenal"),
        awayTeam = TeamSummary(id = 5, name = "Tottenham"),
        homeScore = 2,
        awayScore = 1,
        startTimeDate = "2026-03-21T15:00:00",
        status = MatchStatus.ENDED
    )

    private val match1Odds = MatchOdds(
        matchId = 101L,
        oddsList = listOf(
            createOddsItem(companyId = 1, companyName = "Provider A", homeWin = 1.85, draw = 3.40, awayWin = 4.20)
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeMatchRepository = FakeMatchRepository()
        fakeTeamRepository = FakeTeamRepository()
        fakeOddsRepository = FakeOddsRepository()
        predictMatchOutcomeUseCase = PredictMatchOutcomeUseCase()

        fakeMatchRepository.setMatches(listOf(match1, match2))
        fakeTeamRepository.setTeams(listOf(manCity, arsenal, liverpool))
        fakeMatchRepository.setRecentMatches(1, listOf(recentMatchManCity))
        fakeMatchRepository.setRecentMatches(2, listOf(recentMatchArsenal))
        fakeOddsRepository.setMatchOdds(101L, match1Odds)

        viewModel = PredictionViewModel(
            savedStateHandle = SavedStateHandle(),
            matchRepository = fakeMatchRepository,
            teamRepository = fakeTeamRepository,
            oddsRepository = fakeOddsRepository,
            predictMatchOutcomeUseCase = predictMatchOutcomeUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() {
        assertEquals(PredictionUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `uiState emits Success with computed prediction result when data is available`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Success state but was $state", state is PredictionUiState.Success)
        val success = state as PredictionUiState.Success

        assertEquals(101L, success.selectedMatch.id)
        assertEquals("Manchester City", success.selectedMatch.homeTeam.name)
        assertEquals("Arsenal", success.selectedMatch.awayTeam.name)
        assertEquals(1980.0, success.homeElo)
        assertEquals(1945.0, success.awayElo)

        // Verify computed probabilities sum to ~100%
        val totalPct = success.homeWinPercent + success.drawPercent + success.awayWinPercent
        assertTrue("Total probability percentage should be around 100%, but was $totalPct", totalPct in 99..101)

        assertNotNull(success.predictionResult)
        assertEquals("Weighted Scoring", success.predictionResult.algorithmName)
        assertTrue(success.confidencePercent > 0)

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
    fun `repository error emits Error state with UiText`() = runTest {
        fakeMatchRepository.shouldThrowError = true

        val errorViewModel = PredictionViewModel(
            savedStateHandle = SavedStateHandle(),
            matchRepository = fakeMatchRepository,
            teamRepository = fakeTeamRepository,
            oddsRepository = fakeOddsRepository,
            predictMatchOutcomeUseCase = predictMatchOutcomeUseCase
        )

        val collectJob = launch { errorViewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = errorViewModel.uiState.value
        assertTrue("Expected Error state but was $state", state is PredictionUiState.Error)
        val error = state as PredictionUiState.Error
        assertTrue(error.message is UiText.DynamicString)
        assertEquals("Database match query failed", (error.message as UiText.DynamicString).value)

        collectJob.cancel()
    }

    @Test
    fun `selecting different match recalculates prediction context and probabilities`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSelectMatch(102L) // Arsenal vs Liverpool
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is PredictionUiState.Success)
        val success = state as PredictionUiState.Success
        assertEquals(102L, success.selectedMatch.id)
        assertEquals("Arsenal", success.selectedMatch.homeTeam.name)
        assertEquals("Liverpool", success.selectedMatch.awayTeam.name)
        assertEquals(1945.0, success.homeElo)
        assertEquals(1920.0, success.awayElo)

        collectJob.cancel()
    }

    @Test
    fun `initial matchId from SavedStateHandle selects that match initially`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("matchId" to "102"))
        val customViewModel = PredictionViewModel(
            savedStateHandle = savedStateHandle,
            matchRepository = fakeMatchRepository,
            teamRepository = fakeTeamRepository,
            oddsRepository = fakeOddsRepository,
            predictMatchOutcomeUseCase = predictMatchOutcomeUseCase
        )

        val collectJob = launch { customViewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = customViewModel.uiState.value
        assertTrue(state is PredictionUiState.Success)
        val success = state as PredictionUiState.Success
        assertEquals(102L, success.selectedMatch.id)

        collectJob.cancel()
    }

    @Test
    fun `missing Elo or odds falls back safely without crash`() = runTest {
        fakeTeamRepository.setTeams(emptyList()) // No elo
        fakeOddsRepository.setMatchOdds(101L, MatchOdds(101L, emptyList())) // No odds

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is PredictionUiState.Success)
        val success = state as PredictionUiState.Success
        assertEquals(null, success.homeElo)
        assertEquals(null, success.awayElo)
        assertTrue(success.homeWinPercent > 0)

        collectJob.cancel()
    }

    @Test
    fun `no mock static data regression - probabilities are strictly computed`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value as PredictionUiState.Success
        // Ensure probabilities are not hardcoded 54, 24, 22
        val notAllStatic = (state.homeWinPercent != 54) || (state.drawPercent != 24) || (state.awayWinPercent != 22)
        assertTrue("Probabilities should be computed, not mock 54/24/22", notAllStatic)

        collectJob.cancel()
    }

    // --- Helpers and Fakes ---

    private fun createOddsItem(
        companyId: Int = 1,
        companyName: String = "Provider",
        oddsType: String = "1x2",
        handicap: Double? = null,
        over: Double? = null,
        under: Double? = null,
        homeWin: Double? = null,
        draw: Double? = null,
        awayWin: Double? = null,
        changeTime: Long = 0L,
        marketPhase: String? = null
    ) = OddsRecordItem(
        companyId = companyId,
        companyName = companyName,
        oddsType = oddsType,
        handicap = handicap,
        over = over,
        under = under,
        homeWin = homeWin,
        draw = draw,
        awayWin = awayWin,
        changeTime = changeTime,
        marketPhase = marketPhase
    )

    private class FakeMatchRepository : MatchRepository {
        private val matchesFlow = MutableStateFlow<List<Match>>(emptyList())
        private val recentMatchesMap = mutableMapOf<Int, List<Match>>()
        var shouldThrowError = false

        fun setMatches(matches: List<Match>) {
            matchesFlow.value = matches
        }

        fun setRecentMatches(teamId: Int, matches: List<Match>) {
            recentMatchesMap[teamId] = matches
        }

        override fun getMatches(date: String): Flow<List<Match>> = flow {
            if (shouldThrowError) throw RuntimeException("Database match query failed")
            matchesFlow.collect { emit(it) }
        }

        override fun getMatchDetail(matchId: Long): Flow<Match?> = flow {
            if (shouldThrowError) throw RuntimeException("Database match query failed")
            emit(matchesFlow.value.find { it.id == matchId })
        }

        override fun getRecentMatchesForTeam(teamId: Int, limit: Int): Flow<List<Match>> = flow {
            if (shouldThrowError) throw RuntimeException("Database match query failed")
            emit(recentMatchesMap[teamId] ?: emptyList())
        }

        override fun getMatchesByLeagueAndSeason(leagueId: Int, season: String): Flow<List<Match>> = flow {
            if (shouldThrowError) throw RuntimeException("Database match query failed")
            matchesFlow.collect { matches ->
                emit(matches.filter { it.leagueId == leagueId && it.season == season })
            }
        }
    }

    private class FakeTeamRepository : TeamRepository {
        private val teamsFlow = MutableStateFlow<List<TeamDetail>>(emptyList())
        var shouldThrowError = false

        fun setTeams(teams: List<TeamDetail>) {
            teamsFlow.value = teams
        }

        override fun getTeamDetail(teamId: Int): Flow<TeamDetail?> = flow {
            if (shouldThrowError) throw RuntimeException("Database team query failed")
            emit(teamsFlow.value.find { it.id == teamId })
        }

        override fun getTeams(): Flow<List<TeamDetail>> = flow {
            if (shouldThrowError) throw RuntimeException("Database team query failed")
            teamsFlow.collect { emit(it) }
        }

        override fun getSeasonRanking(matchId: Long): Flow<List<SeasonRanking>> = flow {
            if (shouldThrowError) throw RuntimeException("Database ranking query failed")
            emit(emptyList())
        }

        override fun getSeasonRankings(): Flow<List<SeasonRanking>> = flow {
            if (shouldThrowError) throw RuntimeException("Database ranking query failed")
            emit(emptyList())
        }
    }

    private class FakeOddsRepository : OddsRepository {
        private val matchOddsMap = mutableMapOf<Long, MatchOdds>()
        var shouldThrowError = false

        fun setMatchOdds(matchId: Long, matchOdds: MatchOdds) {
            matchOddsMap[matchId] = matchOdds
        }

        override fun getMatchOdds(matchId: Long): Flow<MatchOdds> = flow {
            if (shouldThrowError) throw RuntimeException("Database odds query failed")
            emit(matchOddsMap[matchId] ?: MatchOdds(matchId, emptyList()))
        }

        override fun getOddsHistory(matchId: Long, companyId: Int?, oddsType: String?): Flow<List<OddsRecordItem>> = flow {
            if (shouldThrowError) throw RuntimeException("Database history query failed")
            emit(emptyList())
        }
    }
}
