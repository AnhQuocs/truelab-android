package dev.anhquocs.truelab.feature.benchmark.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.benchmark.usecase.RunAlgorithmBenchmarkUseCase
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.repository.MatchRepository
import dev.anhquocs.truelab.core.ui.utils.UiText
import dev.anhquocs.truelab.feature.benchmark.presentation.model.BenchmarkUiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * ViewModel quản lý logic đo lường hiệu năng thuật toán (Benchmark).
 *
 * Thực thi các phép đo lường CPU-bound trên [defaultDispatcher] (mặc định là [Dispatchers.Default])
 * để đảm bảo không làm nghẽn giao diện người dùng.
 */
@HiltViewModel
class BenchmarkViewModel @Inject constructor(
    private val runAlgorithmBenchmarkUseCase: RunAlgorithmBenchmarkUseCase,
    private val matchRepository: MatchRepository
) : ViewModel() {

    private var defaultDispatcher: CoroutineDispatcher = Dispatchers.Default

    // Secondary constructor phục vụ Unit Testing
    constructor(
        runAlgorithmBenchmarkUseCase: RunAlgorithmBenchmarkUseCase,
        defaultDispatcher: CoroutineDispatcher
    ) : this(runAlgorithmBenchmarkUseCase, NoOpMatchRepository) {
        this.defaultDispatcher = defaultDispatcher
    }

    private val _uiState = MutableStateFlow<BenchmarkUiState>(BenchmarkUiState.Idle())
    val uiState: StateFlow<BenchmarkUiState> = _uiState.asStateFlow()
    private var matches: List<Match> = emptyList()

    init {
        viewModelScope.launch {
            try {
                matches = withContext(defaultDispatcher) {
                    matchRepository.getAllMatches().first()
                }
                updateIdle { it.copy(availableMatches = matches.size) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // The benchmark remains usable with the synthetic fallback in tests.
            }
        }
    }

    fun onSelectDatasetSize(size: Int) {
        val currentState = _uiState.value
        if (currentState is BenchmarkUiState.Running) return
        _uiState.value = currentState.toIdle().copy(selectedDatasetSize = size)
    }

    fun onSearchQueryChanged(query: String) {
        if (_uiState.value is BenchmarkUiState.Running) return
        _uiState.value = _uiState.value.toIdle().copy(searchQuery = query)
    }

    fun onSearchLimitChanged(delta: Int) {
        if (_uiState.value is BenchmarkUiState.Running) return
        val nextLimit = (_uiState.value.searchLimit + delta).coerceAtLeast(1)
        _uiState.value = _uiState.value.toIdle().copy(searchLimit = nextLimit)
    }

    fun runBenchmark() {
        val currentState = _uiState.value
        if (currentState is BenchmarkUiState.Running) return

        val currentSize = currentState.selectedDatasetSize
        val currentQuery = currentState.searchQuery
        val currentLimit = currentState.searchLimit
        val availableMatches = currentState.availableMatches
        _uiState.value = BenchmarkUiState.Running(
            selectedDatasetSize = currentSize,
            searchQuery = currentQuery,
            searchLimit = currentLimit,
            availableMatches = availableMatches
        )

        viewModelScope.launch {
            try {
                val suiteResult = withContext(defaultDispatcher) {
                    runAlgorithmBenchmarkUseCase(
                        datasetSizes = listOf(currentSize),
                        matches = matches.takeIf { it.isNotEmpty() },
                        searchQuery = currentQuery,
                        searchLimit = currentLimit
                    )
                }
                val searchComparison = suiteResult.searchBenchmarks.firstOrNull()
                val sortComparison = suiteResult.sortBenchmarks.firstOrNull()

                if (searchComparison != null && sortComparison != null) {
                    _uiState.value = BenchmarkUiState.Success(
                        selectedDatasetSize = currentSize,
                        searchResult = searchComparison,
                        sortResult = sortComparison,
                        timestamp = suiteResult.timestamp,
                        searchQuery = currentQuery,
                        searchLimit = currentLimit,
                        availableMatches = availableMatches
                    )
                } else {
                    _uiState.value = BenchmarkUiState.Error(
                        selectedDatasetSize = currentSize,
                        message = UiText.StringResource(R.string.benchmark_error_default),
                        searchQuery = currentQuery,
                        searchLimit = currentLimit,
                        availableMatches = availableMatches
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = BenchmarkUiState.Error(
                    selectedDatasetSize = currentSize,
                    message = if (e.message.isNullOrBlank()) {
                        UiText.StringResource(R.string.benchmark_error_default)
                    } else {
                        UiText.DynamicString(e.message ?: "")
                    },
                    searchQuery = currentQuery,
                    searchLimit = currentLimit,
                    availableMatches = availableMatches
                )
            }
        }
    }

    private fun updateIdle(transform: (BenchmarkUiState.Idle) -> BenchmarkUiState.Idle) {
        val state = _uiState.value as? BenchmarkUiState.Idle ?: return
        _uiState.value = transform(state)
    }

    private fun BenchmarkUiState.toIdle(): BenchmarkUiState.Idle = BenchmarkUiState.Idle(
        selectedDatasetSize = selectedDatasetSize,
        searchQuery = searchQuery,
        searchLimit = searchLimit,
        availableMatches = availableMatches
    )

    private object NoOpMatchRepository : MatchRepository {
        override fun getMatches(date: String) = kotlinx.coroutines.flow.flowOf(emptyList<Match>())
        override fun getMatchDetail(matchId: Long) = kotlinx.coroutines.flow.flowOf(null)
        override fun getRecentMatchesForTeam(teamId: Int, limit: Int) = kotlinx.coroutines.flow.flowOf(emptyList<Match>())
        override fun getMatchesByLeagueAndSeason(leagueId: Int, season: String) = kotlinx.coroutines.flow.flowOf(emptyList<Match>())
        override fun getH2HMatches(teamAId: Int, teamBId: Int) = kotlinx.coroutines.flow.flowOf(emptyList<Match>())
        override fun getAllMatches() = kotlinx.coroutines.flow.flowOf(emptyList<Match>())
        override fun getPredictableMatches(limit: Int) = kotlinx.coroutines.flow.flowOf(emptyList<Match>())
        override fun searchMatches(query: String, limit: Int) = kotlinx.coroutines.flow.flowOf(emptyList<Match>())
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
        ) = kotlinx.coroutines.flow.flowOf(emptyList<Match>())
        override fun getMatchesPaged(
            query: String?,
            leagueId: Int?,
            statusFilter: String,
            sortBy: String,
            limit: Int,
            offset: Int
        ) = kotlinx.coroutines.flow.flowOf(emptyList<Match>())
        override suspend fun refreshMatchesForDate(date: String) = Result.success(Unit)
    }
}
