package dev.anhquocs.truelab.feature.benchmark.presentation.model

import dev.anhquocs.truelab.core.domain.benchmark.model.ComparisonBenchmarkResult
import dev.anhquocs.truelab.core.ui.utils.UiText

/**
 * Trạng thái giao diện cho màn hình đo lường hiệu năng thuật toán (BenchmarkScreen).
 *
 * Tuân thủ mô hình Unidirectional Data Flow (UDF):
 * - [Idle]: Trạng thái khởi tạo hoặc sau khi đổi kích thước dataset mà chưa bấm Run.
 * - [Running]: Trạng thái đang thực thi tính toán đo lường trên background thread.
 * - [Success]: Trạng thái hoàn thành thành công với kết quả đo lường thực tế từ Domain.
 * - [Error]: Trạng thái lỗi khi có ngoại lệ phát sinh trong quá trình đo lường.
 */
sealed interface BenchmarkUiState {
    val selectedDatasetSize: Int
    val searchQuery: String
    val searchLimit: Int
    val availableMatches: Int

    data class Idle(
        override val selectedDatasetSize: Int = DEFAULT_DATASET_SIZE,
        override val searchQuery: String = "",
        override val searchLimit: Int = DEFAULT_SEARCH_LIMIT,
        override val availableMatches: Int = 0
    ) : BenchmarkUiState

    data class Running(
        override val selectedDatasetSize: Int,
        override val searchQuery: String,
        override val searchLimit: Int,
        override val availableMatches: Int
    ) : BenchmarkUiState

    data class Success(
        override val selectedDatasetSize: Int,
        val searchResult: ComparisonBenchmarkResult,
        val sortResult: ComparisonBenchmarkResult,
        val timestamp: Long,
        override val searchQuery: String,
        override val searchLimit: Int,
        override val availableMatches: Int
    ) : BenchmarkUiState

    data class Error(
        override val selectedDatasetSize: Int,
        val message: UiText,
        override val searchQuery: String,
        override val searchLimit: Int,
        override val availableMatches: Int
    ) : BenchmarkUiState

    companion object {
        const val DEFAULT_DATASET_SIZE = 10_000
        const val DEFAULT_SEARCH_LIMIT = 5
        val AVAILABLE_SIZES = listOf(10_000, 30_000, 50_000, 75_000)
    }
}
