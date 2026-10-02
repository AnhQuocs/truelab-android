package dev.anhquocs.truelab.feature.backtest.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusMedium
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.s10
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.s18
import dev.anhquocs.truelab.core.ui.utils.semiBold
import dev.anhquocs.truelab.feature.backtest.presentation.components.BacktestEmptyCard
import dev.anhquocs.truelab.feature.backtest.presentation.components.BacktestErrorCard
import dev.anhquocs.truelab.feature.backtest.presentation.components.BacktestOverviewCard
import dev.anhquocs.truelab.feature.backtest.presentation.components.ClassMetricsBreakdownCard
import dev.anhquocs.truelab.feature.backtest.presentation.components.ConfusionMatrixHeatmapCard
import dev.anhquocs.truelab.feature.backtest.presentation.components.HistoricalTimelineCard
import dev.anhquocs.truelab.feature.backtest.presentation.model.BacktestUiState
import dev.anhquocs.truelab.feature.backtest.presentation.viewmodel.BacktestViewModel

@Composable
fun BacktestVisualizerScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BacktestViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BacktestTopBar(
                onNavigateBack = onNavigateBack,
                onRefresh = { viewModel.runBacktest() },
                isRunning = uiState is BacktestUiState.Running
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is BacktestUiState.Loading,
                is BacktestUiState.Running -> {
                    BacktestLoadingContent()
                }

                is BacktestUiState.Empty -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Dimen.PaddingM),
                        contentAlignment = Alignment.Center
                    ) {
                        BacktestEmptyCard(
                            message = state.message,
                            onRetry = { viewModel.runBacktest() }
                        )
                    }
                }

                is BacktestUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Dimen.PaddingM),
                        contentAlignment = Alignment.Center
                    ) {
                        BacktestErrorCard(
                            message = state.message,
                            onRetry = { viewModel.runBacktest() }
                        )
                    }
                }

                is BacktestUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(Dimen.PaddingM),
                        verticalArrangement = Arrangement.spacedBy(Dimen.PaddingM)
                    ) {
                        // 1. Overall Performance Card
                        BacktestOverviewCard(overview = state.overview)

                        // 2. 3x3 Confusion Matrix Heatmap
                        ConfusionMatrixHeatmapCard(matrix = state.confusionMatrix)

                        // 3. Per-Class Metrics Breakdown
                        ClassMetricsBreakdownCard(classMetrics = state.classMetrics)

                        // 4. Historical Predictions Timeline
                        HistoricalTimelineCard(
                            allMatchesCount = state.allMatches.size,
                            correctCount = state.overview.correctMatches,
                            incorrectCount = state.overview.totalMatches - state.overview.correctMatches,
                            filteredMatches = state.filteredMatches,
                            selectedFilter = state.selectedFilter,
                            onFilterSelected = { viewModel.onFilterSelected(it) }
                        )

                        // 5. Data Leakage & Methodology Note Banner
                        MethodologyNoteBanner()

                        Spacer(modifier = Modifier.height(Dimen.PaddingL))
                    }
                }
            }
        }
    }
}

@Composable
private fun BacktestTopBar(
    onNavigateBack: () -> Unit,
    onRefresh: () -> Unit,
    isRunning: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = Dimen.PaddingS, vertical = Dimen.PaddingS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingXS)
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Column {
                Text(
                    text = stringResource(R.string.backtest_screen_title),
                    style = MaterialTheme.typography.s18.bold(),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.backtest_screen_subtitle),
                    style = MaterialTheme.typography.s12,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        IconButton(
            onClick = onRefresh,
            enabled = !isRunning
        ) {
            if (isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Dimen.SizeM),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.backtest_run_btn),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun BacktestLoadingContent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(Dimen.SizeButtonM),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(Dimen.PaddingM))
            Text(
                text = stringResource(R.string.backtest_running_status),
                style = MaterialTheme.typography.s14.semiBold(),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun MethodologyNoteBanner(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMedium))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(Dimen.PaddingM),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingS)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Dimen.SizeSM)
        )
        Text(
            text = stringResource(R.string.backtest_zero_leakage_note),
            style = MaterialTheme.typography.s10,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
