package dev.anhquocs.truelab.feature.backtest.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.evaluation.model.PredictionBacktestResult
import dev.anhquocs.truelab.core.domain.evaluation.usecase.BacktestPredictionUseCase
import dev.anhquocs.truelab.core.domain.match.repository.MatchRepository
import dev.anhquocs.truelab.core.domain.team.repository.TeamRepository
import dev.anhquocs.truelab.core.ui.utils.UiText
import dev.anhquocs.truelab.feature.backtest.presentation.mapper.BacktestUiMapper
import dev.anhquocs.truelab.feature.backtest.presentation.model.BacktestFilter
import dev.anhquocs.truelab.feature.backtest.presentation.model.BacktestMatchUiRecord
import dev.anhquocs.truelab.feature.backtest.presentation.model.BacktestUiState
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
 * ViewModel orchestrating the execution of historical prediction backtesting and evaluation visualization.
 *
 * Runs heavy data synthesis and backtesting on [defaultDispatcher] to guarantee that the UI thread never stutters.
 */
@HiltViewModel
class BacktestViewModel @Inject constructor(
    private val matchRepository: MatchRepository,
    private val teamRepository: TeamRepository,
    private val backtestPredictionUseCase: BacktestPredictionUseCase
) : ViewModel() {

    private var defaultDispatcher: CoroutineDispatcher = Dispatchers.Default

    // Secondary constructor for Unit Testing
    constructor(
        matchRepository: MatchRepository,
        teamRepository: TeamRepository,
        backtestPredictionUseCase: BacktestPredictionUseCase,
        defaultDispatcher: CoroutineDispatcher
    ) : this(matchRepository, teamRepository, backtestPredictionUseCase) {
        this.defaultDispatcher = defaultDispatcher
    }

    private val _uiState = MutableStateFlow<BacktestUiState>(BacktestUiState.Loading)
    val uiState: StateFlow<BacktestUiState> = _uiState.asStateFlow()

    init {
        runBacktest()
    }

    /**
     * Executes the backtest evaluation on historical matches stored in Room.
     */
    fun runBacktest() {
        val currentState = _uiState.value
        if (currentState is BacktestUiState.Running) return

        _uiState.value = BacktestUiState.Running

        viewModelScope.launch {
            try {
                // Fetch matches and teams from repositories
                val allMatches = matchRepository.getAllMatches().first()
                val allTeams = teamRepository.getTeams().first()

                if (allMatches.isEmpty()) {
                    _uiState.value = BacktestUiState.Empty(
                        message = UiText.StringResource(R.string.backtest_empty_desc)
                    )
                    return@launch
                }

                val teamEloMap = allTeams.associate { it.id to (it.eloRating ?: 1500.0) }

                // Execute heavy backtest calculation on background dispatcher
                val backtestResult: PredictionBacktestResult = withContext(defaultDispatcher) {
                    backtestPredictionUseCase(
                        matches = allMatches,
                        matchOddsMap = emptyMap(),
                        teamEloMap = teamEloMap
                    )
                }

                if (backtestResult.totalMatches == 0) {
                    _uiState.value = BacktestUiState.Empty(
                        message = UiText.StringResource(R.string.backtest_empty_desc)
                    )
                    return@launch
                }

                val overviewUi = BacktestUiMapper.toOverviewUiRecord(backtestResult)
                val confusionMatrixUi = BacktestUiMapper.toConfusionMatrixUiRecord(backtestResult.evaluationResult.confusionMatrix)
                val classMetricsUi = BacktestUiMapper.toClassMetricUiRecords(backtestResult.evaluationResult)
                val matchUiRecords = BacktestUiMapper.toMatchUiRecords(backtestResult.records)

                _uiState.value = BacktestUiState.Success(
                    overview = overviewUi,
                    confusionMatrix = confusionMatrixUi,
                    classMetrics = classMetricsUi,
                    allMatches = matchUiRecords,
                    filteredMatches = matchUiRecords,
                    selectedFilter = BacktestFilter.ALL
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = BacktestUiState.Error(
                    message = if (e.message.isNullOrBlank()) {
                        UiText.StringResource(R.string.backtest_error_default)
                    } else {
                        UiText.DynamicString(e.message ?: "")
                    }
                )
            }
        }
    }

    /**
     * Filters the backtested match records in the timeline.
     */
    fun onFilterSelected(filter: BacktestFilter) {
        val currentState = _uiState.value
        if (currentState is BacktestUiState.Success) {
            val filtered = filterMatches(currentState.allMatches, filter)
            _uiState.value = currentState.copy(
                selectedFilter = filter,
                filteredMatches = filtered
            )
        }
    }

    private fun filterMatches(
        matches: List<BacktestMatchUiRecord>,
        filter: BacktestFilter
    ): List<BacktestMatchUiRecord> {
        return when (filter) {
            BacktestFilter.ALL -> matches
            BacktestFilter.CORRECT_ONLY -> matches.filter { it.isCorrect }
            BacktestFilter.INCORRECT_ONLY -> matches.filter { !it.isCorrect }
        }
    }
}
