package dev.anhquocs.truelab.feature.benchmark.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.benchmark.usecase.RunAlgorithmBenchmarkUseCase
import dev.anhquocs.truelab.core.ui.utils.UiText
import dev.anhquocs.truelab.feature.benchmark.presentation.model.BenchmarkUiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val runAlgorithmBenchmarkUseCase: RunAlgorithmBenchmarkUseCase
) : ViewModel() {

    private var defaultDispatcher: CoroutineDispatcher = Dispatchers.Default

    // Secondary constructor phục vụ Unit Testing
    constructor(
        runAlgorithmBenchmarkUseCase: RunAlgorithmBenchmarkUseCase,
        defaultDispatcher: CoroutineDispatcher
    ) : this(runAlgorithmBenchmarkUseCase) {
        this.defaultDispatcher = defaultDispatcher
    }

    private val _uiState = MutableStateFlow<BenchmarkUiState>(BenchmarkUiState.Idle())
    val uiState: StateFlow<BenchmarkUiState> = _uiState.asStateFlow()

    fun onSelectDatasetSize(size: Int) {
        val currentState = _uiState.value
        if (currentState is BenchmarkUiState.Running) return
        _uiState.value = BenchmarkUiState.Idle(selectedDatasetSize = size)
    }

    fun runBenchmark() {
        val currentState = _uiState.value
        if (currentState is BenchmarkUiState.Running) return

        val currentSize = currentState.selectedDatasetSize
        _uiState.value = BenchmarkUiState.Running(selectedDatasetSize = currentSize)

        viewModelScope.launch {
            try {
                val suiteResult = withContext(defaultDispatcher) {
                    runAlgorithmBenchmarkUseCase(datasetSizes = listOf(currentSize))
                }
                val searchComparison = suiteResult.searchBenchmarks.firstOrNull()
                val sortComparison = suiteResult.sortBenchmarks.firstOrNull()

                if (searchComparison != null && sortComparison != null) {
                    _uiState.value = BenchmarkUiState.Success(
                        selectedDatasetSize = currentSize,
                        searchResult = searchComparison,
                        sortResult = sortComparison,
                        timestamp = suiteResult.timestamp
                    )
                } else {
                    _uiState.value = BenchmarkUiState.Error(
                        selectedDatasetSize = currentSize,
                        message = UiText.StringResource(R.string.benchmark_error_default)
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
                    }
                )
            }
        }
    }
}
