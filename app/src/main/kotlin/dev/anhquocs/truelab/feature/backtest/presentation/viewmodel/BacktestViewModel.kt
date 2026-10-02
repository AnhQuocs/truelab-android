package dev.anhquocs.truelab.feature.backtest.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.evaluation.model.DailyBacktestProgressEvent
import dev.anhquocs.truelab.core.domain.evaluation.model.EvaluationPhase
import dev.anhquocs.truelab.core.domain.evaluation.usecase.RunDailyBacktestUseCase
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.repository.MatchRepository
import dev.anhquocs.truelab.core.domain.team.repository.TeamRepository
import dev.anhquocs.truelab.core.ui.utils.DateTimeFormatterUtils
import dev.anhquocs.truelab.core.ui.utils.UiText
import dev.anhquocs.truelab.feature.backtest.presentation.mapper.BacktestUiMapper
import dev.anhquocs.truelab.feature.backtest.presentation.model.BacktestFilter
import dev.anhquocs.truelab.feature.backtest.presentation.model.BacktestMatchUiRecord
import dev.anhquocs.truelab.feature.backtest.presentation.model.BacktestUiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * ViewModel điều phối quy trình Daily Backtest & Evaluation Visualizer trên các trận FT của ngày được chọn.
 *
 * Quản lý vòng đời trạng thái (Idle -> Running -> Success / Empty / Error) và hỗ trợ hủy tác vụ (Cancellation).
 */
@HiltViewModel
class BacktestViewModel(
    private val matchRepository: MatchRepository,
    private val teamRepository: TeamRepository,
    private val runDailyBacktestUseCase: RunDailyBacktestUseCase,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    @Inject
    constructor(
        matchRepository: MatchRepository,
        teamRepository: TeamRepository,
        runDailyBacktestUseCase: RunDailyBacktestUseCase
    ) : this(matchRepository, teamRepository, runDailyBacktestUseCase, Dispatchers.Default)

    private val _selectedDate = MutableStateFlow(LocalDate.now(DateTimeFormatterUtils.VIETNAM_ZONE_ID).toString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _uiState = MutableStateFlow<BacktestUiState>(BacktestUiState.Idle(_selectedDate.value, 0))
    val uiState: StateFlow<BacktestUiState> = _uiState.asStateFlow()

    private var currentFtMatches: List<Match> = emptyList()
    private var backtestJob: Job? = null

    init {
        viewModelScope.launch(defaultDispatcher) {
            _selectedDate.collect { date ->
                loadDayFtMatches(date)
            }
        }
    }

    /**
     * Tải danh sách các trận đấu đã kết thúc (FT) trong ngày được chọn.
     */
    private suspend fun loadDayFtMatches(date: String) {
        val parsedDate = try {
            LocalDate.parse(date)
        } catch (_: Exception) {
            LocalDate.now(DateTimeFormatterUtils.VIETNAM_ZONE_ID)
        }

        val startUtc = parsedDate.atStartOfDay(DateTimeFormatterUtils.VIETNAM_ZONE_ID)
            .withZoneSameInstant(ZoneOffset.UTC)
        val endUtc = parsedDate.plusDays(1).atStartOfDay(DateTimeFormatterUtils.VIETNAM_ZONE_ID)
            .withZoneSameInstant(ZoneOffset.UTC)

        val startDateUtc = startUtc.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val endDateUtc = endUtc.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        val todayVietnam = LocalDate.now(DateTimeFormatterUtils.VIETNAM_ZONE_ID)
        val isPastDate = parsedDate.isBefore(todayVietnam)
        val isFutureDate = parsedDate.isAfter(todayVietnam)

        try {
            val matches = matchRepository.getPredictableMatchesFiltered(
                startDateUtc = startDateUtc,
                endDateUtc = endDateUtc,
                isPastDate = isPastDate,
                isFutureDate = isFutureDate,
                datePrefix = date,
                leagueId = null,
                statusFilter = dev.anhquocs.truelab.core.domain.match.model.PredictionStatusFilter.FINISHED,
                searchQuery = null,
                limit = 200
            ).first()

            val ftList = matches.filter {
                it.isEnded && it.homeScore != null && it.awayScore != null && it.startTimeDate.isNotBlank()
            }

            currentFtMatches = ftList

            if (ftList.isEmpty()) {
                _uiState.value = BacktestUiState.Empty(
                    selectedDate = date,
                    message = UiText.StringResource(R.string.backtest_empty_desc)
                )
            } else {
                _uiState.value = BacktestUiState.Idle(
                    selectedDate = date,
                    availableFtMatchesCount = ftList.size
                )
            }
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

    /**
     * Khởi chạy quy trình Daily Backtest trên các trận FT của ngày đang chọn.
     */
    fun runBacktest() {
        val date = _selectedDate.value
        val targets = currentFtMatches
        if (targets.isEmpty()) {
            _uiState.value = BacktestUiState.Empty(
                selectedDate = date,
                message = UiText.StringResource(R.string.backtest_empty_desc)
            )
            return
        }

        backtestJob?.cancel()
        backtestJob = viewModelScope.launch(defaultDispatcher) {
            try {
                val allMatches = matchRepository.getAllMatches().first()
                val allTeams = teamRepository.getTeams().first()
                val teamEloMap = allTeams.associate { it.id to (it.eloRating ?: 1500.0) }

                _uiState.value = BacktestUiState.Running(
                    selectedDate = date,
                    completedMatches = 0,
                    totalMatches = targets.size,
                    progressPercent = 0f,
                    currentMatchName = "${targets.first().homeTeam.name} vs ${targets.first().awayTeam.name}",
                    currentPhase = EvaluationPhase.FETCHING_ODDS
                )

                runDailyBacktestUseCase(
                    evaluationDate = date,
                    targetMatches = targets,
                    allMatches = allMatches,
                    initialEloMap = teamEloMap
                ).collect { event ->
                    when (event) {
                        is DailyBacktestProgressEvent.Progress -> {
                            val pct = if (event.totalCount > 0) {
                                event.completedCount.toFloat() / event.totalCount
                            } else {
                                0f
                            }
                            _uiState.value = BacktestUiState.Running(
                                selectedDate = date,
                                completedMatches = event.completedCount,
                                totalMatches = event.totalCount,
                                progressPercent = pct,
                                currentMatchName = event.currentMatchName,
                                currentPhase = event.currentPhase
                            )
                        }

                        is DailyBacktestProgressEvent.Completed -> {
                            val result = event.result
                            val overviewUi = BacktestUiMapper.toOverviewUiRecord(result)
                            val oddsCoverageUi = BacktestUiMapper.toOddsCoverageUiRecord(result.oddsCoverage)
                            val confusionMatrixUi = BacktestUiMapper.toConfusionMatrixUiRecord(result.evaluationResult.confusionMatrix)
                            val classMetricsUi = BacktestUiMapper.toClassMetricUiRecords(result.evaluationResult)
                            val matchUiRecords = BacktestUiMapper.toMatchUiRecords(result.records)

                            _uiState.value = BacktestUiState.Success(
                                selectedDate = date,
                                overview = overviewUi,
                                oddsCoverage = oddsCoverageUi,
                                confusionMatrix = confusionMatrixUi,
                                classMetrics = classMetricsUi,
                                allMatches = matchUiRecords,
                                filteredMatches = matchUiRecords,
                                selectedFilter = BacktestFilter.ALL
                            )
                        }
                    }
                }
            } catch (e: CancellationException) {
                // Khi hủy, đưa trạng thái về Idle an toàn
                _uiState.value = BacktestUiState.Idle(
                    selectedDate = date,
                    availableFtMatchesCount = targets.size
                )
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
     * Hủy bỏ tiến trình đánh giá đang thực thi.
     */
    fun cancelBacktest() {
        backtestJob?.cancel()
        backtestJob = null
        _uiState.value = BacktestUiState.Idle(
            selectedDate = _selectedDate.value,
            availableFtMatchesCount = currentFtMatches.size
        )
    }

    fun onDateSelected(date: String) {
        if (_selectedDate.value != date) {
            cancelBacktest()
            _selectedDate.value = date
        }
    }

    fun onPreviousDay() {
        try {
            val current = LocalDate.parse(_selectedDate.value)
            onDateSelected(current.minusDays(1).toString())
        } catch (_: Exception) {}
    }

    fun onNextDay() {
        try {
            val current = LocalDate.parse(_selectedDate.value)
            onDateSelected(current.plusDays(1).toString())
        } catch (_: Exception) {}
    }

    fun onToday() {
        onDateSelected(LocalDate.now(DateTimeFormatterUtils.VIETNAM_ZONE_ID).toString())
    }

    /**
     * Lọc danh sách trận đấu đã đánh giá theo bộ lọc.
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
