package dev.anhquocs.truelab.feature.benchmark.presentation.viewmodel

import dev.anhquocs.truelab.core.domain.benchmark.usecase.RunAlgorithmBenchmarkUseCase
import dev.anhquocs.truelab.core.ui.utils.UiText
import dev.anhquocs.truelab.feature.benchmark.presentation.model.BenchmarkUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class BenchmarkViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var useCase: RunAlgorithmBenchmarkUseCase
    private lateinit var viewModel: BenchmarkViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        useCase = RunAlgorithmBenchmarkUseCase()
        viewModel = BenchmarkViewModel(
            runAlgorithmBenchmarkUseCase = useCase,
            defaultDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle with default size 10000`() {
        val state = viewModel.uiState.value
        assertTrue(state is BenchmarkUiState.Idle)
        assertEquals(10_000, state.selectedDatasetSize)
    }

    @Test
    fun `onSelectDatasetSize updates selectedDatasetSize in Idle state`() {
        viewModel.onSelectDatasetSize(1_000)
        assertEquals(1_000, viewModel.uiState.value.selectedDatasetSize)
        assertTrue(viewModel.uiState.value is BenchmarkUiState.Idle)

        viewModel.onSelectDatasetSize(50_000)
        assertEquals(50_000, viewModel.uiState.value.selectedDatasetSize)
        assertTrue(viewModel.uiState.value is BenchmarkUiState.Idle)
    }

    @Test
    fun `runBenchmark transitions from Idle to Running to Success`() = runTest(testDispatcher) {
        viewModel.onSelectDatasetSize(1_000)
        viewModel.runBenchmark()

        // Ngay sau khi gọi runBenchmark, state là Running
        val runningState = viewModel.uiState.value
        assertTrue(runningState is BenchmarkUiState.Running)
        assertEquals(1_000, runningState.selectedDatasetSize)

        advanceUntilIdle()

        // Sau khi coroutine hoàn thành, state chuyển sang Success
        val successState = viewModel.uiState.value
        assertTrue(successState is BenchmarkUiState.Success)
        val success = successState as BenchmarkUiState.Success
        assertEquals(1_000, success.selectedDatasetSize)
        assertEquals("Linear Search", success.searchResult.algorithmA.algorithmName)
        assertEquals("Binary Search", success.searchResult.algorithmB.algorithmName)
        assertEquals("Quick Sort", success.sortResult.algorithmA.algorithmName)
        assertEquals("Merge Sort", success.sortResult.algorithmB.algorithmName)
        assertTrue(success.searchResult.speedupFactor >= 1.0)
        assertTrue(success.sortResult.speedupFactor >= 1.0)
        assertTrue(success.timestamp > 0)
    }

    @Test
    fun `runBenchmark preserves selectedDatasetSize across rerun`() = runTest(testDispatcher) {
        viewModel.onSelectDatasetSize(50_000)
        viewModel.runBenchmark()

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is BenchmarkUiState.Success)
        assertEquals(50_000, viewModel.uiState.value.selectedDatasetSize)

        // Chọn lại size 10_000 và chạy lại
        viewModel.onSelectDatasetSize(10_000)
        assertTrue(viewModel.uiState.value is BenchmarkUiState.Idle)
        assertEquals(10_000, viewModel.uiState.value.selectedDatasetSize)

        viewModel.runBenchmark()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is BenchmarkUiState.Success)
        assertEquals(10_000, viewModel.uiState.value.selectedDatasetSize)
    }

    @Test
    fun `onSelectDatasetSize and runBenchmark are ignored when currently in Running state`() = runTest(testDispatcher) {
        viewModel.onSelectDatasetSize(1_000)
        viewModel.runBenchmark()

        assertTrue(viewModel.uiState.value is BenchmarkUiState.Running)

        // Cố gắng đổi size khi đang chạy -> không có tác dụng
        viewModel.onSelectDatasetSize(50_000)
        assertEquals(1_000, viewModel.uiState.value.selectedDatasetSize)
        assertTrue(viewModel.uiState.value is BenchmarkUiState.Running)

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is BenchmarkUiState.Success)
        assertEquals(1_000, viewModel.uiState.value.selectedDatasetSize)
    }
}
