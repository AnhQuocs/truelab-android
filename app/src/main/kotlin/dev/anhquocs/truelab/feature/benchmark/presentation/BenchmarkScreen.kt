package dev.anhquocs.truelab.feature.benchmark.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.ui.theme.ButtonHeightMedium
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusMedium
import dev.anhquocs.truelab.core.ui.theme.SpacingL
import dev.anhquocs.truelab.core.ui.theme.SpacingM
import dev.anhquocs.truelab.core.ui.theme.SpacingS
import dev.anhquocs.truelab.core.ui.theme.SpacingXL
import dev.anhquocs.truelab.core.ui.theme.SpacingXS
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.s20
import dev.anhquocs.truelab.core.ui.utils.semiBold
import dev.anhquocs.truelab.feature.benchmark.presentation.components.AlgorithmBenchmarkCard
import dev.anhquocs.truelab.feature.benchmark.presentation.components.DatasetSizeSelector
import dev.anhquocs.truelab.feature.benchmark.presentation.model.BenchmarkUiState
import dev.anhquocs.truelab.feature.benchmark.presentation.viewmodel.BenchmarkViewModel
import dev.anhquocs.truelab.navigation.TrueLabMainLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BenchmarkScreen(
    modifier: Modifier = Modifier,
    viewModel: BenchmarkViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TrueLabMainLayout(
        modifier = modifier,
        header = {
            BenchmarkHeader(onNavigateBack = onNavigateBack)
        }
    ) { contentModifier ->
        BenchmarkContent(
            uiState = uiState,
            onSelectDatasetSize = viewModel::onSelectDatasetSize,
            onSearchQueryChanged = viewModel::onSearchQueryChanged,
            onSearchLimitChanged = viewModel::onSearchLimitChanged,
            onRunBenchmark = viewModel::runBenchmark,
            modifier = contentModifier
        )
    }
}

@Composable
private fun BenchmarkHeader(
    onNavigateBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimen.PaddingS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(Dimen.PaddingS))
        Text(
            text = stringResource(R.string.benchmark_title),
            style = MaterialTheme.typography.s20.bold(),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun BenchmarkContent(
    uiState: BenchmarkUiState,
    onSelectDatasetSize: (Int) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSearchLimitChanged: (Int) -> Unit,
    onRunBenchmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRunning = uiState is BenchmarkUiState.Running
    val successState = uiState as? BenchmarkUiState.Success
    val errorState = uiState as? BenchmarkUiState.Error

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = SpacingL),
        verticalArrangement = Arrangement.Top,
        contentPadding = PaddingValues(bottom = SpacingXL)
    ) {
        // 1. Dataset Size Selector
        item {
            Spacer(modifier = Modifier.height(SpacingS))
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChanged,
                enabled = !isRunning,
                singleLine = true,
                label = { Text(stringResource(R.string.benchmark_search_query_label)) },
                placeholder = { Text(stringResource(R.string.benchmark_search_query_hint)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(SpacingS))
            Text(
                text = stringResource(R.string.benchmark_search_limit_label),
                style = MaterialTheme.typography.s14,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = { onSearchLimitChanged(-1) },
                    enabled = !isRunning && uiState.searchLimit > 1,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = stringResource(R.string.benchmark_decrease_limit)
                    )
                }
                Text(
                    text = uiState.searchLimit.toString(),
                    style = MaterialTheme.typography.s20.bold(),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = SpacingM),
                    textAlign = TextAlign.Center
                )
                IconButton(
                    onClick = { onSearchLimitChanged(1) },
                    enabled = !isRunning,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.benchmark_increase_limit)
                    )
                }
            }
            Spacer(modifier = Modifier.height(SpacingS))
            DatasetSizeSelector(
                sizes = BenchmarkUiState.AVAILABLE_SIZES,
                selectedSize = uiState.selectedDatasetSize,
                enabled = !isRunning,
                onSelectSize = onSelectDatasetSize
            )
            Text(
                text = stringResource(R.string.benchmark_available_matches, uiState.availableMatches),
                style = MaterialTheme.typography.s12,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 2. Searching Benchmark Card (Linear vs Binary Search)
        item {
            Spacer(modifier = Modifier.height(SpacingM))
            AlgorithmBenchmarkCard(
                title = stringResource(R.string.benchmark_search_title),
                comparison = successState?.searchResult,
                algo1Name = "Linear Search",
                algo1Complexity = "O(N)",
                algo2Name = "Binary Search",
                algo2Complexity = "O(log N)",
                isRunning = isRunning
            )
        }
        if (successState != null && successState.searchResult.algorithmA.matchedItems != null) {
            item {
                Spacer(modifier = Modifier.height(SpacingS))
                Text(
                    text = stringResource(
                        R.string.benchmark_search_result_summary,
                        successState.searchQuery,
                        successState.searchResult.algorithmA.matchedItems ?: 0,
                        successState.searchLimit
                    ),
                    style = MaterialTheme.typography.s12,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 3. Sorting Benchmark Card (QuickSort vs MergeSort)
        item {
            Spacer(modifier = Modifier.height(SpacingM))
            Text(
                text = stringResource(R.string.benchmark_sort_key_description),
                style = MaterialTheme.typography.s12,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(SpacingS))
            AlgorithmBenchmarkCard(
                title = stringResource(R.string.benchmark_sort_title),
                comparison = successState?.sortResult,
                algo1Name = "QuickSort (In-Place)",
                algo1Complexity = "O(N log N)",
                algo2Name = "MergeSort",
                algo2Complexity = "O(N log N)",
                isRunning = isRunning
            )
        }

        // 4. Status / Error Message
        if (errorState != null) {
            item {
                Spacer(modifier = Modifier.height(SpacingM))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(RadiusMedium))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(Dimen.PaddingM)
                ) {
                    Text(
                        text = errorState.message.asString(),
                        style = MaterialTheme.typography.s12,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // 5. Execution Timestamp / Idle Hint
        item {
            Spacer(modifier = Modifier.height(SpacingM))
            val statusText = when {
                isRunning -> stringResource(R.string.benchmark_running_status, uiState.selectedDatasetSize)
                successState != null -> {
                    val formatted = SimpleDateFormat("dd/MM/yyyy • HH:mm:ss", Locale.getDefault())
                        .format(Date(successState.timestamp))
                    stringResource(R.string.benchmark_last_run, formatted)
                }
                else -> stringResource(R.string.benchmark_idle_hint)
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.s12,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 6. Run Benchmark Button
        item {
            Spacer(modifier = Modifier.height(SpacingM))
            Button(
                onClick = onRunBenchmark,
                enabled = !isRunning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ButtonHeightMedium),
                shape = RoundedCornerShape(RadiusMedium)
            ) {
                if (isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Dimen.SizeS2),
                        strokeWidth = Dimen.PaddingXXS,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(SpacingS))
                    Text(
                        text = stringResource(R.string.benchmark_running_status, uiState.selectedDatasetSize),
                        style = MaterialTheme.typography.s14.semiBold()
                    )
                } else {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.padding(horizontal = SpacingXS))
                    Text(
                        text = stringResource(R.string.benchmark_run_btn),
                        style = MaterialTheme.typography.s14.semiBold()
                    )
                }
            }
        }
    }
}
