package dev.anhquocs.truelab.feature.home.presentation.viewmodel

import dev.anhquocs.truelab.core.domain.metadata.model.DatasetMetadata
import dev.anhquocs.truelab.core.domain.metadata.repository.DatasetMetadataRepository
import dev.anhquocs.truelab.core.ui.utils.UiText
import dev.anhquocs.truelab.feature.home.presentation.model.HomeUiState
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
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeDatasetMetadataRepository
    private lateinit var viewModel: HomeViewModel

    private val sampleMetadata = DatasetMetadata(
        lastSyncTimestamp = 1773600000000L, // 2026-03-15
        totalMatches = 380,
        totalTeams = 20,
        totalOddsRecords = 1140,
        totalLeagues = 1,
        totalSeasons = 1,
        schemaVersion = 2
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeDatasetMetadataRepository()
        viewModel = HomeViewModel(datasetMetadataRepository = fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun test1_initialStateIsLoading() {
        assertEquals(HomeUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun test2_metadataFlowEmitsData_mapsToSuccessState() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect {} }

        fakeRepository.emit(sampleMetadata)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Success state, but was $state", state is HomeUiState.Success)
        val success = state as HomeUiState.Success
        assertEquals(380, success.metadata.totalMatches)
        assertEquals(20, success.metadata.totalTeams)
        assertEquals(1140, success.metadata.totalOddsRecords)
        assertTrue(success.formattedLastSync.isNotBlank())
        assertTrue(success.formattedLastSync.contains("2026"))
    }

    @Test
    fun test3_nullMetadata_fallsBackToDefaultSuccessState() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect {} }

        fakeRepository.emit(null)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Success fallback, but was $state", state is HomeUiState.Success)
        val success = state as HomeUiState.Success
        assertEquals(0, success.metadata.totalMatches)
        assertEquals(0, success.metadata.totalTeams)
        assertEquals(0, success.metadata.totalOddsRecords)
        assertEquals("—", success.formattedLastSync)
    }

    @Test
    fun test4_repositoryError_updatesUiStateToError() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect {} }

        fakeRepository.errorToThrow = RuntimeException("Failed to read metadata")
        fakeRepository.emit(sampleMetadata)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Error state, but was $state", state is HomeUiState.Error)
        val error = state as HomeUiState.Error
        assertTrue(error.message is UiText.DynamicString)
        assertEquals("Failed to read metadata", (error.message as UiText.DynamicString).value)
    }

    private class FakeDatasetMetadataRepository : DatasetMetadataRepository {
        private val metadataFlow = MutableStateFlow<DatasetMetadata?>(null)
        var errorToThrow: Throwable? = null

        fun emit(metadata: DatasetMetadata?) {
            metadataFlow.value = metadata
        }

        override fun getMetadata(key: String): Flow<DatasetMetadata?> = flow {
            errorToThrow?.let { throw it }
            metadataFlow.collect { emit(it) }
        }

        override suspend fun refreshSnapshot(timestamp: Long): Result<DatasetMetadata> {
            return errorToThrow?.let { Result.failure(it) }
                ?: Result.success(metadataFlow.value ?: DatasetMetadata(
                    lastSyncTimestamp = timestamp,
                    totalMatches = 0,
                    totalTeams = 0,
                    totalOddsRecords = 0,
                    totalLeagues = 0,
                    totalSeasons = 0
                ))
        }
    }
}
