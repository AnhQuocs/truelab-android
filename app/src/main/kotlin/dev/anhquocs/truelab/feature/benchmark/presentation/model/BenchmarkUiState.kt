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

    data class Idle(
        override val selectedDatasetSize: Int = DEFAULT_DATASET_SIZE
    ) : BenchmarkUiState

    data class Running(
        override val selectedDatasetSize: Int
    ) : BenchmarkUiState

    data class Success(
        override val selectedDatasetSize: Int,
        val searchResult: ComparisonBenchmarkResult,
        val sortResult: ComparisonBenchmarkResult,
        val timestamp: Long
    ) : BenchmarkUiState

    data class Error(
        override val selectedDatasetSize: Int,
        val message: UiText
    ) : BenchmarkUiState

    companion object {
        const val DEFAULT_DATASET_SIZE = 10_000
        val AVAILABLE_SIZES = listOf(1_000, 10_000, 50_000)
    }
}
