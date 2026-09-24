package dev.anhquocs.truelab.feature.analytics.presentation.viewmodel

import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.domain.match.model.TeamSummary
import dev.anhquocs.truelab.core.domain.match.repository.MatchRepository
import dev.anhquocs.truelab.core.domain.odds.model.MatchOdds
import dev.anhquocs.truelab.core.domain.odds.model.OddsRecordItem
import dev.anhquocs.truelab.core.domain.odds.model.TargetOddsField
import dev.anhquocs.truelab.core.domain.odds.repository.OddsRepository
import dev.anhquocs.truelab.core.domain.odds.usecase.AnalyzeOddsTrendUseCase
import dev.anhquocs.truelab.core.domain.team.model.SeasonRanking
import dev.anhquocs.truelab.core.domain.team.model.TeamDetail
import dev.anhquocs.truelab.core.domain.team.repository.TeamRepository
import dev.anhquocs.truelab.core.domain.team.usecase.GetTeamStatisticsUseCase
import dev.anhquocs.truelab.core.ui.utils.UiText
import dev.anhquocs.truelab.feature.analytics.presentation.model.AnalyticsUiState
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeTeamRepository: FakeTeamRepository
    private lateinit var fakeMatchRepository: FakeMatchRepository
    private lateinit var fakeOddsRepository: FakeOddsRepository
    private lateinit var getTeamStatisticsUseCase: GetTeamStatisticsUseCase
    private lateinit var analyzeOddsTrendUseCase: AnalyzeOddsTrendUseCase
    private lateinit var viewModel: AnalyticsViewModel

    private val manCity = TeamDetail(id = 1, name = "Manchester City", leagueName = "Premier League", eloRating = 1980.0)
    private val arsenal = TeamDetail(id = 2, name = "Arsenal", leagueName = "Premier League", eloRating = 1945.0)

    private val match1 = Match(
        id = 101L,
        homeTeam = TeamSummary(id = 1, name = "Manchester City"),
        awayTeam = TeamSummary(id = 2, name = "Arsenal"),
        homeScore = 3,
        awayScore = 1,
        startTimeDate = "2026-03-01T15:00:00",
        status = MatchStatus.ENDED
    )

    private val match2 = Match(
        id = 102L,
        homeTeam = TeamSummary(id = 1, name = "Manchester City"),
        awayTeam = TeamSummary(id = 3, name = "Chelsea"),
        homeScore = 2,
        awayScore = 0,
        startTimeDate = "2026-03-08T17:30:00",
        status = MatchStatus.ENDED
    )

    private val match1Odds = MatchOdds(
        matchId = 101L,
        oddsList = listOf(
            createOddsItem(companyId = 1, companyName = "Provider A", homeWin = 1.80, draw = 3.50, awayWin = 4.20),
            createOddsItem(companyId = 2, companyName = "Provider B", homeWin = 1.90, draw = 3.40, awayWin = 4.00)
        )
    )

    private val match1History = listOf(
        createOddsItem(companyId = 1, companyName = "Provider A", homeWin = 2.00, draw = 3.20, awayWin = 3.80, changeTime = 1000L),
        createOddsItem(companyId = 1, companyName = "Provider A", homeWin = 1.90, draw = 3.30, awayWin = 4.00, changeTime = 2000L),
        createOddsItem(companyId = 1, companyName = "Provider A", homeWin = 1.80, draw = 3.50, awayWin = 4.20, changeTime = 3000L)
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeTeamRepository = FakeTeamRepository()
        fakeMatchRepository = FakeMatchRepository()
        fakeOddsRepository = FakeOddsRepository()
        getTeamStatisticsUseCase = GetTeamStatisticsUseCase()
        analyzeOddsTrendUseCase = AnalyzeOddsTrendUseCase()

        fakeTeamRepository.setTeams(listOf(manCity, arsenal))
        fakeMatchRepository.setMatches(listOf(match1, match2))
        fakeOddsRepository.setMatchOdds(101L, match1Odds)
        fakeOddsRepository.setOddsHistory(101L, match1History)

        viewModel = AnalyticsViewModel(
            teamRepository = fakeTeamRepository,
            matchRepository = fakeMatchRepository,
            oddsRepository = fakeOddsRepository,
            getTeamStatisticsUseCase = getTeamStatisticsUseCase,
            analyzeOddsTrendUseCase = analyzeOddsTrendUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() {
        assertEquals(AnalyticsUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `uiState emits Success with descriptive stats and odds analysis when repositories have data`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Success state but was $state", state is AnalyticsUiState.Success)
        val success = state as AnalyticsUiState.Success

        assertEquals("Manchester City", success.selectedTeam?.name)
        assertEquals(101L, success.selectedMatch?.id)

        // Verify team descriptive stats from GetTeamStatisticsUseCase
        assertNotNull(success.teamStats)
        assertEquals(2, success.teamStats?.matchesCount)
        // Man City: scored 3, 2 -> mean = 2.5
        assertEquals(2.5, success.teamStats!!.goalsScoredStats.mean, 0.001)

        // Verify Multi-provider calculated metrics
        assertEquals(1.85, success.avgHomeOdds!!, 0.001) // (1.80 + 1.90) / 2
        assertEquals(3.45, success.avgDrawOdds!!, 0.001) // (3.50 + 3.40) / 2
        assertEquals(4.10, success.avgAwayOdds!!, 0.001) // (4.20 + 4.00) / 2
        assertEquals(0.10, success.oddsSpread!!, 0.001)  // 1.90 - 1.80

        // Verify Odds trend from AnalyzeOddsTrendUseCase
        assertNotNull(success.oddsTrend)
        assertEquals(3, success.oddsTrend?.rawOddsSeries?.size)
        assertEquals(2.00, success.oddsTrend?.openingOdds!!, 0.001)
        assertEquals(1.80, success.oddsTrend?.currentOdds!!, 0.001)
        // SMA(3) for [2.00, 1.90, 1.80] = 1.90
        assertEquals(1.90, success.oddsTrend?.smaSeries?.last()!!, 0.001)

        collectJob.cancel()
    }

    @Test
    fun `uiState emits Empty when team and match repositories are empty`() = runTest {
        fakeTeamRepository.setTeams(emptyList())
        fakeMatchRepository.setMatches(emptyList())

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Empty state but was $state", state is AnalyticsUiState.Empty)
        val empty = state as AnalyticsUiState.Empty
        assertTrue(empty.message is UiText.StringResource)
        assertEquals(R.string.analytics_empty_no_data, (empty.message as UiText.StringResource).resId)

        collectJob.cancel()
    }

    @Test
    fun `repository error emits Error state with UiText`() = runTest {
        fakeTeamRepository.shouldThrowError = true

        val errorViewModel = AnalyticsViewModel(
            teamRepository = fakeTeamRepository,
            matchRepository = fakeMatchRepository,
            oddsRepository = fakeOddsRepository,
            getTeamStatisticsUseCase = getTeamStatisticsUseCase,
            analyzeOddsTrendUseCase = analyzeOddsTrendUseCase
        )

        val collectJob = launch { errorViewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = errorViewModel.uiState.value
        assertTrue("Expected Error state but was $state", state is AnalyticsUiState.Error)
        val error = state as AnalyticsUiState.Error
        assertTrue(error.message is UiText.DynamicString)
        assertEquals("Database connection failed", (error.message as UiText.DynamicString).value)

        collectJob.cancel()
    }

    @Test
    fun `selecting different team updates team descriptive statistics via GetTeamStatisticsUseCase`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onTeamSelected(2) // Arsenal
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AnalyticsUiState.Success)
        val success = state as AnalyticsUiState.Success
        assertEquals("Arsenal", success.selectedTeam?.name)
        // Arsenal has 1 match (match 1: scored 1)
        assertEquals(1, success.teamStats?.matchesCount)
        assertEquals(1.0, success.teamStats!!.goalsScoredStats.mean, 0.001)

        collectJob.cancel()
    }

    @Test
    fun `selecting different match updates match odds and odds trend`() = runTest {
        val match2Odds = MatchOdds(
            matchId = 102L,
            oddsList = listOf(
                createOddsItem(companyId = 3, companyName = "Provider C", homeWin = 1.50, draw = 4.00, awayWin = 6.00)
            )
        )
        fakeOddsRepository.setMatchOdds(102L, match2Odds)
        fakeOddsRepository.setOddsHistory(102L, emptyList())

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onMatchSelected(102L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AnalyticsUiState.Success)
        val success = state as AnalyticsUiState.Success
        assertEquals(102L, success.selectedMatch?.id)
        assertEquals(1.50, success.avgHomeOdds!!, 0.001)
        assertNull(success.oddsSpread) // Only 1 provider -> spread is null

        collectJob.cancel()
    }

    @Test
    fun `changing TargetOddsField recalculates trend for selected target`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onTargetFieldSelected(TargetOddsField.AWAY_WIN)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AnalyticsUiState.Success)
        val success = state as AnalyticsUiState.Success
        assertEquals(TargetOddsField.AWAY_WIN, success.selectedTargetField)
        assertEquals(TargetOddsField.AWAY_WIN, success.oddsTrend?.targetField)
        // Away odds history: [3.80, 4.00, 4.20]
        assertEquals(3.80, success.oddsTrend?.openingOdds!!, 0.001)
        assertEquals(4.20, success.oddsTrend?.currentOdds!!, 0.001)
        // SMA(3) for [3.80, 4.00, 4.20] = 4.00
        assertEquals(4.00, success.oddsTrend?.smaSeries?.last()!!, 0.001)

        collectJob.cancel()
    }

    @Test
    fun `changing window size recalculates SMA with new window`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onWindowSizeChanged(2)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AnalyticsUiState.Success)
        val success = state as AnalyticsUiState.Success
        assertEquals(2, success.selectedWindowSize)
        assertEquals(2, success.oddsTrend?.windowSize)
        // SMA(2) for [2.00, 1.90, 1.80] -> SMA results: (2.00+1.90)/2=1.95, (1.90+1.80)/2=1.85
        assertEquals(2, success.oddsTrend?.smaSeries?.size)
        assertEquals(1.85, success.oddsTrend?.smaSeries?.last()!!, 0.001)

        collectJob.cancel()
    }

    @Test
    fun `insufficient odds history marks hasSufficientData as false with empty smaSeries`() = runTest {
        // Only 2 points, but window size is 3
        val shortHistory = listOf(
            createOddsItem(companyId = 1, companyName = "Provider A", homeWin = 2.00, changeTime = 1000L),
            createOddsItem(companyId = 1, companyName = "Provider A", homeWin = 1.90, changeTime = 2000L)
        )
        fakeOddsRepository.setOddsHistory(101L, shortHistory)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AnalyticsUiState.Success)
        val success = state as AnalyticsUiState.Success
        assertEquals(false, success.oddsTrend?.hasSufficientData)
        assertTrue(success.oddsTrend?.smaSeries?.isEmpty() == true)
        assertEquals(2.00, success.oddsTrend?.openingOdds!!, 0.001)
        assertEquals(1.90, success.oddsTrend?.currentOdds!!, 0.001)

        collectJob.cancel()
    }

    @Test
    fun `no mock static data regression - state relies purely on repository and usecase calculations`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value as AnalyticsUiState.Success
        // Ensure no legacy hardcoded values like 75000, 2.84, 1.42, 2.01, 0.42
        assertTrue(state.teamStats?.matchesCount != 75000)
        assertTrue(state.teamStats?.totalGoalsStats?.mean != 2.84)
        assertTrue(state.teamStats?.totalGoalsStats?.sampleStandardDeviation != 1.42)

        collectJob.cancel()
    }

    // --- Fake Repositories & Helpers ---

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

    private class FakeTeamRepository : TeamRepository {
        private val teamsFlow = MutableStateFlow<List<TeamDetail>>(emptyList())
        var shouldThrowError = false

        fun setTeams(teams: List<TeamDetail>) {
            teamsFlow.value = teams
        }

        override fun getTeamDetail(teamId: Int): Flow<TeamDetail?> = flow {
            if (shouldThrowError) throw RuntimeException("Database connection failed")
            emit(teamsFlow.value.find { it.id == teamId })
        }

        override fun getTeams(): Flow<List<TeamDetail>> = flow {
            if (shouldThrowError) throw RuntimeException("Database connection failed")
            teamsFlow.collect { emit(it) }
        }

        override fun getSeasonRanking(matchId: Long): Flow<List<SeasonRanking>> = flow {
            if (shouldThrowError) throw RuntimeException("Database connection failed")
            emit(emptyList())
        }

        override fun getSeasonRankings(): Flow<List<SeasonRanking>> = flow {
            if (shouldThrowError) throw RuntimeException("Database connection failed")
            emit(emptyList())
        }
    }

    private class FakeMatchRepository : MatchRepository {
        private val matchesFlow = MutableStateFlow<List<Match>>(emptyList())
        var shouldThrowError = false

        fun setMatches(matches: List<Match>) {
            matchesFlow.value = matches
        }

        override fun getMatches(date: String): Flow<List<Match>> = flow {
            if (shouldThrowError) throw RuntimeException("Database connection failed")
            matchesFlow.collect { emit(it) }
        }

        override fun getMatchDetail(matchId: Long): Flow<Match?> = flow {
            if (shouldThrowError) throw RuntimeException("Database connection failed")
            emit(matchesFlow.value.find { it.id == matchId })
        }

        override fun getRecentMatchesForTeam(teamId: Int, limit: Int): Flow<List<Match>> = flow {
            if (shouldThrowError) throw RuntimeException("Database connection failed")
            matchesFlow.collect { matches ->
                emit(matches.filter { it.homeTeam.id == teamId || it.awayTeam.id == teamId }.take(limit))
            }
        }

        override fun getMatchesByLeagueAndSeason(leagueId: Int, season: String): Flow<List<Match>> = flow {
            if (shouldThrowError) throw RuntimeException("Database connection failed")
            matchesFlow.collect { matches ->
                emit(matches.filter { it.leagueId == leagueId && it.season == season })
            }
        }
    }

    private class FakeOddsRepository : OddsRepository {
        private val matchOddsMap = mutableMapOf<Long, MatchOdds>()
        private val historyMap = mutableMapOf<Long, List<OddsRecordItem>>()
        var shouldThrowError = false

        fun setMatchOdds(matchId: Long, matchOdds: MatchOdds) {
            matchOddsMap[matchId] = matchOdds
        }

        fun setOddsHistory(matchId: Long, history: List<OddsRecordItem>) {
            historyMap[matchId] = history
        }

        override fun getMatchOdds(matchId: Long): Flow<MatchOdds> = flow {
            if (shouldThrowError) throw RuntimeException("Database connection failed")
            emit(matchOddsMap[matchId] ?: MatchOdds(matchId, emptyList()))
        }

        override fun getOddsHistory(matchId: Long, companyId: Int?, oddsType: String?): Flow<List<OddsRecordItem>> = flow {
            if (shouldThrowError) throw RuntimeException("Database connection failed")
            emit(historyMap[matchId] ?: emptyList())
        }
    }
}
