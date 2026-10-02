package dev.anhquocs.truelab.feature.prediction.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.league.model.League
import dev.anhquocs.truelab.core.domain.league.repository.LeagueRepository
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.domain.match.model.PredictionStatusFilter
import dev.anhquocs.truelab.core.domain.match.repository.MatchRepository
import dev.anhquocs.truelab.core.domain.odds.repository.OddsRepository
import dev.anhquocs.truelab.core.domain.odds.selector.PreMatchOddsSelector
import dev.anhquocs.truelab.core.domain.prediction.model.MatchPredictionContext
import dev.anhquocs.truelab.core.domain.prediction.usecase.PredictMatchOutcomeUseCase
import dev.anhquocs.truelab.core.domain.team.repository.TeamRepository
import dev.anhquocs.truelab.core.ui.utils.UiText
import dev.anhquocs.truelab.feature.prediction.presentation.model.PredictionUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.roundToInt
import dev.anhquocs.truelab.feature.prediction.presentation.components.DisplayMatchStatus
import dev.anhquocs.truelab.feature.prediction.presentation.components.resolveDisplayStatus

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PredictionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val matchRepository: MatchRepository,
    private val teamRepository: TeamRepository,
    private val oddsRepository: OddsRepository,
    private val leagueRepository: LeagueRepository,
    private val predictMatchOutcomeUseCase: PredictMatchOutcomeUseCase,
    private val calculateDynamicEloUseCase: dev.anhquocs.truelab.core.domain.team.usecase.CalculateDynamicEloUseCase = dev.anhquocs.truelab.core.domain.team.usecase.CalculateDynamicEloUseCase()
) : ViewModel() {

    private val navMatchId: Long? = savedStateHandle.get<String>("matchId")?.toLongOrNull()
        ?: savedStateHandle.get<Long>("matchId")

    private val _selectedMatchId = MutableStateFlow<Long?>(navMatchId)
    val selectedMatchId: StateFlow<Long?> = _selectedMatchId.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now().toString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _selectedLeagueId = MutableStateFlow<Int?>(null)
    val selectedLeagueId: StateFlow<Int?> = _selectedLeagueId.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow(PredictionStatusFilter.LIVE_AND_UPCOMING)
    val selectedStatusFilter: StateFlow<PredictionStatusFilter> = _selectedStatusFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _refreshErrorEvent = kotlinx.coroutines.flow.MutableSharedFlow<UiText>()
    val refreshErrorEvent: kotlinx.coroutines.flow.SharedFlow<UiText> = _refreshErrorEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            _selectedDate.collect { date ->
                syncDateIfNeeded(date)
            }
        }
    }

    val leagues: StateFlow<List<League>> = leagueRepository.getLeagues()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val matchesFlow = combine(
        _selectedDate,
        _selectedLeagueId,
        _selectedStatusFilter,
        _searchQuery
    ) { date, leagueId, statusFilter, query ->
        FilterParams(date = date, leagueId = leagueId, statusFilter = statusFilter, query = query)
    }.flatMapLatest { params ->
        val parsedDate = try {
            LocalDate.parse(params.date)
        } catch (_: Exception) {
            LocalDate.now(dev.anhquocs.truelab.core.ui.utils.DateTimeFormatterUtils.VIETNAM_ZONE_ID)
        }
        val todayVietnam = LocalDate.now(dev.anhquocs.truelab.core.ui.utils.DateTimeFormatterUtils.VIETNAM_ZONE_ID)
        val isPastDate = parsedDate.isBefore(todayVietnam)
        val isFutureDate = parsedDate.isAfter(todayVietnam)

        val startUtc = parsedDate.atStartOfDay(dev.anhquocs.truelab.core.ui.utils.DateTimeFormatterUtils.VIETNAM_ZONE_ID)
            .withZoneSameInstant(java.time.ZoneOffset.UTC)
        val endUtc = parsedDate.plusDays(1).atStartOfDay(dev.anhquocs.truelab.core.ui.utils.DateTimeFormatterUtils.VIETNAM_ZONE_ID)
            .withZoneSameInstant(java.time.ZoneOffset.UTC)

        val startDateUtc = startUtc.format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val endDateUtc = endUtc.format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        matchRepository.getPredictableMatchesFiltered(
            startDateUtc = startDateUtc,
            endDateUtc = endDateUtc,
            isPastDate = isPastDate,
            isFutureDate = isFutureDate,
            datePrefix = params.date,
            leagueId = params.leagueId,
            statusFilter = params.statusFilter,
            searchQuery = params.query.takeIf { it.isNotBlank() },
            limit = 50
        ).map { list ->
            sortMatchesForDisplay(list, params.date)
        }
    }

    val availableMatches: StateFlow<List<Match>> = matchesFlow
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val uiState: StateFlow<PredictionUiState> = combine(
        matchesFlow,
        _selectedMatchId,
        _selectedDate
    ) { matches, userMatchId, selectedDate ->
        Triple(matches, userMatchId, selectedDate)
    }.flatMapLatest { (matches, userMatchId, selectedDate) ->
        if (matches.isEmpty()) {
            flowOf(PredictionUiState.Empty(UiText.StringResource(R.string.prediction_empty_no_matches)))
        } else {
            val selectedMatch = autoSelectMatch(matches, userMatchId, selectedDate)

            // Trigger on-demand odds hydration for selected match in background
            viewModelScope.launch(Dispatchers.IO) {
                oddsRepository.fetchAndCacheOddsForMatch(selectedMatch.id)
            }

            val homeTeamId = selectedMatch.homeTeam.id
            val awayTeamId = selectedMatch.awayTeam.id

            combine(
                teamRepository.getTeamDetail(homeTeamId),
                teamRepository.getTeamDetail(awayTeamId),
                matchRepository.getAllMatches(),
                oddsRepository.getMatchOdds(selectedMatch.id)
            ) { homeTeamDetail, awayTeamDetail, allMatches, matchOdds ->
                val targetTime = selectedMatch.startTimeDate

                // 1. Calculate dynamic chronological Elo up to target match time (Zero Temporal Leakage)
                val dynamicEloMap = calculateDynamicEloUseCase(allMatches, targetTime)
                val homeElo = dynamicEloMap[homeTeamId] ?: homeTeamDetail?.eloRating ?: 1500.0
                val awayElo = dynamicEloMap[awayTeamId] ?: awayTeamDetail?.eloRating ?: 1500.0

                // 2. Strict Pre-Match Filter for Historical, Recent Form & H2H (strictly before target match)
                val homeHistory = allMatches
                    .filter { it.isEnded && it.id != selectedMatch.id && (it.homeTeam.id == homeTeamId || it.awayTeam.id == homeTeamId) && (targetTime.isBlank() || it.startTimeDate < targetTime) }
                    .sortedByDescending { it.startTimeDate }

                val awayHistory = allMatches
                    .filter { it.isEnded && it.id != selectedMatch.id && (it.homeTeam.id == awayTeamId || it.awayTeam.id == awayTeamId) && (targetTime.isBlank() || it.startTimeDate < targetTime) }
                    .sortedByDescending { it.startTimeDate }

                val h2hMatches = allMatches
                    .filter { it.isEnded && it.id != selectedMatch.id && ((it.homeTeam.id == homeTeamId && it.awayTeam.id == awayTeamId) || (it.homeTeam.id == awayTeamId && it.awayTeam.id == homeTeamId)) && (targetTime.isBlank() || it.startTimeDate < targetTime) }
                    .sortedByDescending { it.startTimeDate }

                // 3. Select valid Pre-Match European 1X2 Odds
                val preMatchOdds = PreMatchOddsSelector.selectPreMatchEuropeanOdds(
                    oddsList = matchOdds.oddsList,
                    kickoffTime = selectedMatch.startTimeDate
                )

                val context = MatchPredictionContext(
                    matchId = selectedMatch.id,
                    homeTeamId = homeTeamId,
                    awayTeamId = awayTeamId,
                    homeElo = homeElo,
                    awayElo = awayElo,
                    homeRecentMatches = homeHistory,
                    awayRecentMatches = awayHistory,
                    h2hMatches = h2hMatches,
                    latestOdds = preMatchOdds
                )

                val result = predictMatchOutcomeUseCase(context)

                val homeWinPct = (result.homeWinProb * 100).roundToInt()
                val drawPct = (result.drawProb * 100).roundToInt()
                val awayWinPct = (result.awayWinProb * 100).roundToInt()
                val confPct = (result.confidenceScore * 100).roundToInt()

                PredictionUiState.Success(
                    selectedMatch = selectedMatch,
                    availableMatches = matches,
                    predictionResult = result,
                    homeElo = homeElo,
                    awayElo = awayElo,
                    homeWinPercent = homeWinPct,
                    drawPercent = drawPct,
                    awayWinPercent = awayWinPct,
                    confidencePercent = confPct
                )
            }
        }
    }.catch { e ->
        emit(
            PredictionUiState.Error(
                if (e.message.isNullOrBlank()) {
                    UiText.StringResource(R.string.prediction_error_default)
                } else {
                    UiText.DynamicString(e.message ?: "")
                }
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PredictionUiState.Loading
    )

    private fun autoSelectMatch(matches: List<Match>, userMatchId: Long?, selectedDate: String?): Match {
        if (userMatchId != null) {
            val userMatch = matches.find { it.id == userMatchId }
            if (userMatch != null) return userMatch
        }
        return matches.firstOrNull { resolveDisplayStatus(it, selectedDate) == DisplayMatchStatus.LIVE }
            ?: matches.firstOrNull { resolveDisplayStatus(it, selectedDate) == DisplayMatchStatus.UPCOMING }
            ?: matches.firstOrNull { resolveDisplayStatus(it, selectedDate) == DisplayMatchStatus.STARTED }
            ?: matches.first()
    }

    fun onSelectMatch(matchId: Long) {
        _selectedMatchId.value = matchId
    }

    fun onDateSelected(date: String) {
        _selectedDate.value = date
        _selectedMatchId.value = null
    }

    fun onPreviousDay() {
        try {
            val current = LocalDate.parse(_selectedDate.value)
            _selectedDate.value = current.minusDays(1).toString()
            _selectedMatchId.value = null
        } catch (_: Exception) {}
    }

    fun onNextDay() {
        try {
            val current = LocalDate.parse(_selectedDate.value)
            _selectedDate.value = current.plusDays(1).toString()
            _selectedMatchId.value = null
        } catch (_: Exception) {}
    }

    fun onToday() {
        _selectedDate.value = LocalDate.now().toString()
        _selectedMatchId.value = null
    }

    fun onLeagueSelected(leagueId: Int?) {
        _selectedLeagueId.value = leagueId
        _selectedMatchId.value = null
    }

    fun onStatusFilterSelected(filter: PredictionStatusFilter) {
        _selectedStatusFilter.value = filter
        _selectedMatchId.value = null
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onResetFilters() {
        _selectedDate.value = LocalDate.now().toString()
        _selectedLeagueId.value = null
        _selectedStatusFilter.value = PredictionStatusFilter.LIVE_AND_UPCOMING
        _searchQuery.value = ""
        _selectedMatchId.value = null
    }

    private var syncJob: kotlinx.coroutines.Job? = null

    fun refresh() {
        if (_isRefreshing.value) return
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val result = matchRepository.refreshMatchesForDate(_selectedDate.value)
                if (result.isFailure) {
                    _refreshErrorEvent.emit(UiText.StringResource(R.string.prediction_refresh_failed))
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                val message = if (!e.message.isNullOrBlank()) {
                    UiText.DynamicString(e.message!!)
                } else {
                    UiText.StringResource(R.string.prediction_refresh_failed)
                }
                _refreshErrorEvent.emit(message)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun syncDateIfNeeded(date: String) {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            try {
                matchRepository.refreshMatchesForDate(date)
            } catch (_: kotlinx.coroutines.CancellationException) {
                // Ignore cancellation
            } catch (_: Exception) {
                // Ignore silent background sync errors
            }
        }
    }

    private data class FilterParams(
        val date: String,
        val leagueId: Int?,
        val statusFilter: PredictionStatusFilter,
        val query: String
    )

    companion object {
        fun sortMatchesForDisplay(
            matches: List<Match>,
            selectedDate: String?,
            now: java.time.ZonedDateTime = java.time.ZonedDateTime.now(dev.anhquocs.truelab.core.ui.utils.DateTimeFormatterUtils.VIETNAM_ZONE_ID)
        ): List<Match> {
            return matches.sortedWith(
                compareBy<Match> { match ->
                    val displayStatus = resolveDisplayStatus(match, selectedDate, now)
                    when (displayStatus) {
                        DisplayMatchStatus.LIVE -> 0
                        DisplayMatchStatus.STARTED -> 1
                        DisplayMatchStatus.UPCOMING -> 2
                        DisplayMatchStatus.ENDED -> 3
                    }
                }.thenBy { it.startTimeDate }
            )
        }
    }
}
