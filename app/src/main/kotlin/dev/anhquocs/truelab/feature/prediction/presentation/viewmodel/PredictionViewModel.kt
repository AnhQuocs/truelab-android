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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
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
    private val currentMatches = MutableStateFlow<List<Match>>(emptyList())
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

    private val _isSyncing = MutableStateFlow(true)
    val isBackgroundSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

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
        .onEach { currentMatches.value = it }
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val isMatchListLoading: StateFlow<Boolean> = combine(
        _isSyncing,
        availableMatches
    ) { isSyncing, matches ->
        if (!isSyncing) {
            false
        } else {
            // While syncing in background: ready if we have matches and all matches have resolved competition metadata
            matches.isEmpty() || matches.any { it.leagueId == null && it.leagueName.isNullOrBlank() }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true
    )

    private val selectionState: StateFlow<PredictionUiState> = combine(
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
            flowOf(PredictionUiState.Selected(selectedMatch, matches))
        }
    }.onEach { state ->
        if (state is PredictionUiState.Selected) {
            currentMatches.value = state.availableMatches
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

    private val _predictionState = MutableStateFlow<PredictionUiState?>(null)

    val uiState: StateFlow<PredictionUiState> = combine(
        selectionState,
        _predictionState
    ) { selection, prediction ->
        val selectedId = when (selection) {
            is PredictionUiState.Selected -> selection.selectedMatch.id
            is PredictionUiState.Predicting -> selection.selectedMatch.id
            is PredictionUiState.Success -> selection.selectedMatch.id
            else -> null
        }
        if (prediction != null && (selectedId == prediction.selectedMatchId() || prediction is PredictionUiState.Predicting || prediction is PredictionUiState.Success)) {
            prediction
        } else {
            selection
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PredictionUiState.Loading
    )

    private var predictionJob: Job? = null
    private var historicalSnapshot: List<Match>? = null
    private val historicalContextCache = mutableMapOf<Long, HistoricalContext>()

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
        predictionJob?.cancel()
        _predictionState.value = null
        _selectedMatchId.value = matchId
    }

    fun predictSelectedMatch() {
        val requestedMatchId = _selectedMatchId.value
        predictionJob?.cancel()
        predictionJob = viewModelScope.launch {
            val matches = currentMatches.value
            if (matches.isEmpty()) return@launch
            val selectedMatch = requestedMatchId?.let { id -> matches.find { it.id == id } }
                ?: autoSelectMatch(matches, null, _selectedDate.value)
            runPrediction(selectedMatch, matches)
        }
    }

    fun predictMatch(match: Match) {
        predictionJob?.cancel()
        _selectedMatchId.value = match.id
        val matches = (currentMatches.value + match).distinctBy { it.id }
        currentMatches.value = matches
        predictionJob = viewModelScope.launch {
            runPrediction(match, matches)
        }
    }

    private suspend fun runPrediction(selectedMatch: Match, matches: List<Match>) {
        _predictionState.value = PredictionUiState.Predicting(
            selectedMatch = selectedMatch,
            availableMatches = matches
        )
        try {
            oddsRepository.fetchAndCacheOddsForMatch(selectedMatch.id)
            val prediction = calculatePrediction(selectedMatch)
            if (_selectedMatchId.value != null && _selectedMatchId.value != selectedMatch.id) {
                return
            }
            _predictionState.value = prediction.copy(availableMatches = matches)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            if (_selectedMatchId.value == selectedMatch.id) {
                _predictionState.value = PredictionUiState.Error(
                    message = if (e.message.isNullOrBlank()) {
                        UiText.StringResource(R.string.prediction_error_default)
                    } else {
                        UiText.DynamicString(e.message!!)
                    },
                    selectedMatchId = selectedMatch.id
                )
            }
        }
    }

    private suspend fun calculatePrediction(selectedMatch: Match): PredictionUiState.Success {
        val snapshot = historicalSnapshot ?: matchRepository.getAllMatches().first().also {
            historicalSnapshot = it
        }
        val historicalContext = historicalContextCache.getOrPut(selectedMatch.id) {
            buildHistoricalContext(selectedMatch, snapshot)
        }
        val matchOdds = oddsRepository.getMatchOdds(selectedMatch.id).first()
        val preMatchOdds = PreMatchOddsSelector.selectPreMatchEuropeanOdds(
            oddsList = matchOdds.oddsList,
            kickoffTime = selectedMatch.startTimeDate
        )
        val result = predictMatchOutcomeUseCase(
            MatchPredictionContext(
                matchId = selectedMatch.id,
                homeTeamId = selectedMatch.homeTeam.id,
                awayTeamId = selectedMatch.awayTeam.id,
                matchStartTimeDate = selectedMatch.startTimeDate,
                homeElo = historicalContext.homeElo,
                awayElo = historicalContext.awayElo,
                homeRecentMatches = historicalContext.homeHistory,
                awayRecentMatches = historicalContext.awayHistory,
                h2hMatches = historicalContext.h2hMatches,
                latestOdds = preMatchOdds
            )
        )
        return PredictionUiState.Success(
            selectedMatch = selectedMatch,
            availableMatches = availableMatches.value,
            predictionResult = result,
            homeElo = historicalContext.homeElo,
            awayElo = historicalContext.awayElo,
            homeWinPercent = (result.homeWinProb * 100).roundToInt(),
            drawPercent = (result.drawProb * 100).roundToInt(),
            awayWinPercent = (result.awayWinProb * 100).roundToInt(),
            confidencePercent = (result.confidenceScore * 100).roundToInt()
        )
    }

    private suspend fun buildHistoricalContext(
        selectedMatch: Match,
        allMatches: List<Match>
    ): HistoricalContext {
        val homeTeamId = selectedMatch.homeTeam.id
        val awayTeamId = selectedMatch.awayTeam.id
        val targetTime = selectedMatch.startTimeDate
        val endedMatches = allMatches
            .filter { it.isEnded && it.homeScore != null && it.awayScore != null }
            .sortedWith(compareBy({ it.startTimeDate }, { it.id }))
        val dynamicEloMap = calculateDynamicEloUseCase(endedMatches, targetTime)
        val homeDetail = teamRepository.getTeamDetail(homeTeamId).first()
        val awayDetail = teamRepository.getTeamDetail(awayTeamId).first()
        val beforeTarget: (Match) -> Boolean = {
            targetTime.isBlank() || it.startTimeDate < targetTime
        }
        return HistoricalContext(
            homeElo = dynamicEloMap[homeTeamId] ?: homeDetail?.eloRating ?: 1500.0,
            awayElo = dynamicEloMap[awayTeamId] ?: awayDetail?.eloRating ?: 1500.0,
            homeHistory = endedMatches.filter {
                it.id != selectedMatch.id &&
                    (it.homeTeam.id == homeTeamId || it.awayTeam.id == homeTeamId) &&
                    beforeTarget(it)
            }.sortedByDescending { it.startTimeDate },
            awayHistory = endedMatches.filter {
                it.id != selectedMatch.id &&
                    (it.homeTeam.id == awayTeamId || it.awayTeam.id == awayTeamId) &&
                    beforeTarget(it)
            }.sortedByDescending { it.startTimeDate },
            h2hMatches = endedMatches.filter {
                it.id != selectedMatch.id &&
                    ((it.homeTeam.id == homeTeamId && it.awayTeam.id == awayTeamId) ||
                        (it.homeTeam.id == awayTeamId && it.awayTeam.id == homeTeamId)) &&
                    beforeTarget(it)
            }.sortedByDescending { it.startTimeDate }
        )
    }

    private data class HistoricalContext(
        val homeElo: Double,
        val awayElo: Double,
        val homeHistory: List<Match>,
        val awayHistory: List<Match>,
        val h2hMatches: List<Match>
    )

    fun onDateSelected(date: String) {
        predictionJob?.cancel()
        _predictionState.value = null
        _selectedDate.value = date
        _selectedMatchId.value = null
    }

    fun onPreviousDay() {
        try {
            predictionJob?.cancel()
            _predictionState.value = null
            val current = LocalDate.parse(_selectedDate.value)
            _selectedDate.value = current.minusDays(1).toString()
            _selectedMatchId.value = null
        } catch (_: Exception) {}
    }

    fun onNextDay() {
        try {
            predictionJob?.cancel()
            _predictionState.value = null
            val current = LocalDate.parse(_selectedDate.value)
            _selectedDate.value = current.plusDays(1).toString()
            _selectedMatchId.value = null
        } catch (_: Exception) {}
    }

    fun onToday() {
        predictionJob?.cancel()
        _predictionState.value = null
        _selectedDate.value = LocalDate.now().toString()
        _selectedMatchId.value = null
    }

    fun onLeagueSelected(leagueId: Int?) {
        predictionJob?.cancel()
        _predictionState.value = null
        _selectedLeagueId.value = leagueId
        _selectedMatchId.value = null
    }

    fun onStatusFilterSelected(filter: PredictionStatusFilter) {
        predictionJob?.cancel()
        _predictionState.value = null
        _selectedStatusFilter.value = filter
        _selectedMatchId.value = null
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onResetFilters() {
        predictionJob?.cancel()
        _predictionState.value = null
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
                if (result.isSuccess) {
                    invalidateHistoricalCache()
                }
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
            _isSyncing.value = true
            try {
                matchRepository.refreshMatchesForDate(date)
                invalidateHistoricalCache()
            } catch (_: kotlinx.coroutines.CancellationException) {
                // Ignore cancellation
            } catch (_: Exception) {
                // Ignore silent background sync errors
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private fun invalidateHistoricalCache() {
        historicalSnapshot = null
        historicalContextCache.clear()
    }

    private data class FilterParams(
        val date: String,
        val leagueId: Int?,
        val statusFilter: PredictionStatusFilter,
        val query: String
    )

    private fun PredictionUiState.selectedMatchId(): Long? = when (this) {
        is PredictionUiState.Selected -> selectedMatch.id
        is PredictionUiState.Predicting -> selectedMatch.id
        is PredictionUiState.Success -> selectedMatch.id
        is PredictionUiState.Error -> selectedMatchId
        else -> null
    }

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
