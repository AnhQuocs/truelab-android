package dev.anhquocs.truelab.feature.team.presentation.viewmodel

import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.domain.match.model.TeamSummary
import dev.anhquocs.truelab.core.domain.match.repository.MatchRepository
import dev.anhquocs.truelab.core.domain.team.model.SeasonRanking
import dev.anhquocs.truelab.core.domain.team.model.StandingsSortCriteria
import dev.anhquocs.truelab.core.domain.team.model.TeamDetail
import dev.anhquocs.truelab.core.domain.team.repository.TeamRepository
import dev.anhquocs.truelab.core.domain.team.usecase.CalculateHomeAwaySplitsUseCase
import dev.anhquocs.truelab.core.domain.team.usecase.CalculateTeamFormUseCase
import dev.anhquocs.truelab.core.domain.team.usecase.SearchTeamsUseCase
import dev.anhquocs.truelab.core.domain.team.usecase.SortSeasonRankingUseCase
import dev.anhquocs.truelab.core.ui.utils.UiText
import dev.anhquocs.truelab.feature.team.presentation.model.TeamsUiState
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TeamsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeTeamRepository: FakeTeamRepository
    private lateinit var fakeMatchRepository: FakeMatchRepository
    private lateinit var searchTeamsUseCase: SearchTeamsUseCase
    private lateinit var sortSeasonRankingUseCase: SortSeasonRankingUseCase
    private lateinit var calculateTeamFormUseCase: CalculateTeamFormUseCase
    private lateinit var calculateHomeAwaySplitsUseCase: CalculateHomeAwaySplitsUseCase
    private lateinit var viewModel: TeamsViewModel

    private val manCityDetail = TeamDetail(id = 1, name = "Manchester City", leagueName = "Premier League", eloRating = 1980.0)
    private val arsenalDetail = TeamDetail(id = 2, name = "Arsenal", leagueName = "Premier League", eloRating = 1945.0)
    private val liverpoolDetail = TeamDetail(id = 3, name = "Liverpool", leagueName = "Premier League", eloRating = 1920.0)
    private val chelseaDetail = TeamDetail(id = 4, name = "Chelsea", leagueName = "Premier League", eloRating = 1800.0)

    private val manCityRanking = SeasonRanking(teamId = 1, position = 1, won = 20, draw = 5, loss = 3, goalDiff = 45, recently = listOf("W", "W", "W", "D", "W"))
    private val arsenalRanking = SeasonRanking(teamId = 2, position = 2, won = 19, draw = 6, loss = 3, goalDiff = 38, recently = listOf("W", "W", "D", "W", "L"))
    private val liverpoolRanking = SeasonRanking(teamId = 3, position = 3, won = 18, draw = 7, loss = 3, goalDiff = 35, recently = listOf("D", "W", "W", "W", "W"))

    private val match1 = Match(
        id = 101L,
        homeTeam = TeamSummary(id = 1, name = "Manchester City"),
        awayTeam = TeamSummary(id = 2, name = "Arsenal"),
        homeScore = 2,
        awayScore = 1,
        startTimeDate = "2026-03-01T15:00:00",
        status = MatchStatus.ENDED
    )

    private val match2 = Match(
        id = 102L,
        homeTeam = TeamSummary(id = 3, name = "Liverpool"),
        awayTeam = TeamSummary(id = 1, name = "Manchester City"),
        homeScore = 0,
        awayScore = 2,
        startTimeDate = "2026-03-08T17:30:00",
        status = MatchStatus.ENDED
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeTeamRepository = FakeTeamRepository()
        fakeMatchRepository = FakeMatchRepository()
        searchTeamsUseCase = SearchTeamsUseCase()
        sortSeasonRankingUseCase = SortSeasonRankingUseCase()
        calculateTeamFormUseCase = CalculateTeamFormUseCase()
        calculateHomeAwaySplitsUseCase = CalculateHomeAwaySplitsUseCase()

        fakeTeamRepository.setTeams(listOf(manCityDetail, arsenalDetail, liverpoolDetail))
        fakeTeamRepository.setRankings(listOf(manCityRanking, arsenalRanking, liverpoolRanking))
        fakeMatchRepository.setMatches(listOf(match1, match2))

        viewModel = TeamsViewModel(
            teamRepository = fakeTeamRepository,
            matchRepository = fakeMatchRepository,
            searchTeamsUseCase = searchTeamsUseCase,
            sortSeasonRankingUseCase = sortSeasonRankingUseCase,
            calculateTeamFormUseCase = calculateTeamFormUseCase,
            calculateHomeAwaySplitsUseCase = calculateHomeAwaySplitsUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() {
        assertEquals(TeamsUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `uiState emits Success when repository emits teams and rankings`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Success state but was $state", state is TeamsUiState.Success)
        val successState = state as TeamsUiState.Success
        assertEquals(3, successState.teams.size)
        assertEquals(3, successState.rawTeamsCount)
        assertEquals("Manchester City", successState.teams[0].name)
        assertEquals(1980, successState.teams[0].eloRating)
        assertEquals(1, successState.teams[0].rank)
        assertEquals(StandingsSortCriteria.POSITION_ASC, successState.standingsSort)

        collectJob.cancel()
    }

    @Test
    fun `uiState emits Empty when teams list is empty`() = runTest {
        fakeTeamRepository.setTeams(emptyList())
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Empty state but was $state", state is TeamsUiState.Empty)
        val emptyState = state as TeamsUiState.Empty
        assertTrue(emptyState.message is UiText.StringResource)
        assertEquals(R.string.teams_empty_no_data, (emptyState.message as UiText.StringResource).resId)

        collectJob.cancel()
    }

    @Test
    fun `search query filters teams using SearchTeamsUseCase`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("Arsenal")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Success state but was $state", state is TeamsUiState.Success)
        val successState = state as TeamsUiState.Success
        assertEquals(1, successState.teams.size)
        assertEquals("Arsenal", successState.teams[0].name)
        assertEquals("Arsenal", successState.searchQuery)

        collectJob.cancel()
    }

    @Test
    fun `search query with no match emits Empty with query`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("Barcelona")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Empty state but was $state", state is TeamsUiState.Empty)
        val emptyState = state as TeamsUiState.Empty
        assertTrue(emptyState.message is UiText.StringResource)
        val stringResource = emptyState.message as UiText.StringResource
        assertEquals(R.string.teams_empty_search, stringResource.resId)
        assertEquals("Barcelona", stringResource.args[0])

        collectJob.cancel()
    }

    @Test
    fun `changing standings sort to POINTS_DESC sorts by points via SortSeasonRankingUseCase`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSortChanged(StandingsSortCriteria.POINTS_DESC)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is TeamsUiState.Success)
        val successState = state as TeamsUiState.Success
        assertEquals(StandingsSortCriteria.POINTS_DESC, successState.standingsSort)
        assertEquals("Manchester City", successState.teams[0].name) // 20*3+5 = 65 pts
        assertEquals("Arsenal", successState.teams[1].name)         // 19*3+6 = 63 pts
        assertEquals("Liverpool", successState.teams[2].name)       // 18*3+7 = 61 pts

        collectJob.cancel()
    }

    @Test
    fun `changing standings sort to GOAL_DIFF_DESC sorts by goal difference`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSortChanged(StandingsSortCriteria.GOAL_DIFF_DESC)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is TeamsUiState.Success)
        val successState = state as TeamsUiState.Success
        assertEquals(StandingsSortCriteria.GOAL_DIFF_DESC, successState.standingsSort)
        assertEquals("Manchester City", successState.teams[0].name) // 45
        assertEquals("Arsenal", successState.teams[1].name)         // 38
        assertEquals("Liverpool", successState.teams[2].name)       // 35

        collectJob.cancel()
    }

    @Test
    fun `changing standings sort to WINS_DESC sorts by wins`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSortChanged(StandingsSortCriteria.WINS_DESC)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is TeamsUiState.Success)
        val successState = state as TeamsUiState.Success
        assertEquals(StandingsSortCriteria.WINS_DESC, successState.standingsSort)
        assertEquals(20, successState.teams[0].wins)
        assertEquals(19, successState.teams[1].wins)
        assertEquals(18, successState.teams[2].wins)

        collectJob.cancel()
    }

    @Test
    fun `changing standings sort to LOSSES_ASC sorts by fewest losses`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSortChanged(StandingsSortCriteria.LOSSES_ASC)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is TeamsUiState.Success)
        val successState = state as TeamsUiState.Success
        assertEquals(StandingsSortCriteria.LOSSES_ASC, successState.standingsSort)
        assertEquals(3, successState.teams[0].losses)

        collectJob.cancel()
    }

    @Test
    fun `formScore is computed via CalculateTeamFormUseCase with windowSize 5`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is TeamsUiState.Success)
        val successState = state as TeamsUiState.Success
        val manCity = successState.teams.first { it.id == "1" }
        // Man City played 2 matches: Win vs Arsenal (2-1), Win vs Liverpool (2-0) -> 100.0 form score
        assertEquals(100, manCity.formScore)

        collectJob.cancel()
    }

    @Test
    fun `teams without rankings are handled safely`() = runTest {
        // Add Chelsea which has no ranking record
        fakeTeamRepository.setTeams(listOf(manCityDetail, chelseaDetail))
        fakeTeamRepository.setRankings(listOf(manCityRanking))

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is TeamsUiState.Success)
        val successState = state as TeamsUiState.Success
        assertEquals(2, successState.teams.size)
        val chelseaRecord = successState.teams.first { it.id == "4" }
        assertEquals("Chelsea", chelseaRecord.name)
        assertEquals(0, chelseaRecord.rank)
        assertEquals(1800, chelseaRecord.eloRating)

        collectJob.cancel()
    }

    @Test
    fun `home and away splits are computed via CalculateHomeAwaySplitsUseCase and mapped accurately`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is TeamsUiState.Success)
        val successState = state as TeamsUiState.Success

        // Man City: 1 home win (2-1 vs Arsenal) -> 1W-0D-0L, 100% win rate; 1 away win (2-0 vs Liverpool) -> 1W-0D-0L, 100% win rate
        val manCity = successState.teams.first { it.id == "1" }
        assertEquals(100.0, manCity.homeWinRate)
        assertEquals("1W-0D-0L", manCity.homeRecord)
        assertEquals(100.0, manCity.awayWinRate)
        assertEquals("1W-0D-0L", manCity.awayRecord)

        // Arsenal: 0 home matches -> 0W-0D-0L, 0% win rate; 1 away loss (1-2 vs Man City) -> 0W-0D-1L, 0% win rate
        val arsenal = successState.teams.first { it.id == "2" }
        assertEquals(0.0, arsenal.homeWinRate)
        assertEquals("0W-0D-0L", arsenal.homeRecord)
        assertEquals(0.0, arsenal.awayWinRate)
        assertEquals("0W-0D-1L", arsenal.awayRecord)

        // Liverpool: 1 home loss (0-2 vs Man City) -> 0W-0D-1L, 0% win rate; 0 away matches -> 0W-0D-0L, 0% win rate
        val liverpool = successState.teams.first { it.id == "3" }
        assertEquals(0.0, liverpool.homeWinRate)
        assertEquals("0W-0D-1L", liverpool.homeRecord)
        assertEquals(0.0, liverpool.awayWinRate)
        assertEquals("0W-0D-0L", liverpool.awayRecord)

        collectJob.cancel()
    }

    @Test
    fun `repository error emits Error state with UiText`() = runTest {
        fakeTeamRepository.shouldThrowError = true

        val errorViewModel = TeamsViewModel(
            teamRepository = fakeTeamRepository,
            matchRepository = fakeMatchRepository,
            searchTeamsUseCase = searchTeamsUseCase,
            sortSeasonRankingUseCase = sortSeasonRankingUseCase,
            calculateTeamFormUseCase = calculateTeamFormUseCase,
            calculateHomeAwaySplitsUseCase = calculateHomeAwaySplitsUseCase
        )

        val collectJob = launch { errorViewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = errorViewModel.uiState.value
        assertTrue("Expected Error state but was $state", state is TeamsUiState.Error)
        val errorState = state as TeamsUiState.Error
        assertTrue(errorState.message is UiText.DynamicString)
        assertEquals("Database connection failed", (errorState.message as UiText.DynamicString).value)

        collectJob.cancel()
    }

    // --- Fake Repositories ---

    private class FakeTeamRepository : TeamRepository {
        private val teamsFlow = MutableStateFlow<List<TeamDetail>>(emptyList())
        private val rankingsFlow = MutableStateFlow<List<SeasonRanking>>(emptyList())
        var shouldThrowError = false

        fun setTeams(teams: List<TeamDetail>) {
            teamsFlow.value = teams
        }

        fun setRankings(rankings: List<SeasonRanking>) {
            rankingsFlow.value = rankings
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
            rankingsFlow.collect { emit(it) }
        }

        override fun getSeasonRankings(): Flow<List<SeasonRanking>> = flow {
            if (shouldThrowError) throw RuntimeException("Database connection failed")
            rankingsFlow.collect { emit(it) }
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
}
