package dev.anhquocs.truelab.feature.backtest.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import dev.anhquocs.truelab.core.ui.theme.RadiusPill
import dev.anhquocs.truelab.core.ui.utils.DateTimeFormatterUtils
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.medium
import dev.anhquocs.truelab.core.ui.utils.s10
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.s16
import dev.anhquocs.truelab.core.ui.utils.s18
import dev.anhquocs.truelab.core.ui.utils.semiBold
import dev.anhquocs.truelab.feature.backtest.presentation.components.BacktestEmptyCard
import dev.anhquocs.truelab.feature.backtest.presentation.components.BacktestErrorCard
import dev.anhquocs.truelab.feature.backtest.presentation.components.BacktestIdleCard
import dev.anhquocs.truelab.feature.backtest.presentation.components.BacktestOddsCoverageCard
import dev.anhquocs.truelab.feature.backtest.presentation.components.BacktestOverviewCard
import dev.anhquocs.truelab.feature.backtest.presentation.components.BacktestProcessingCard
import dev.anhquocs.truelab.feature.backtest.presentation.components.ClassMetricsBreakdownCard
import dev.anhquocs.truelab.feature.backtest.presentation.components.ConfusionMatrixHeatmapCard
import dev.anhquocs.truelab.feature.backtest.presentation.components.HistoricalTimelineCard
import dev.anhquocs.truelab.feature.backtest.presentation.model.BacktestUiState
import dev.anhquocs.truelab.feature.backtest.presentation.viewmodel.BacktestViewModel
import java.time.LocalDate

@Composable
fun BacktestVisualizerScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BacktestViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Date Selector Navigation Bar
            DateNavigationBar(
                selectedDate = selectedDate,
                onPreviousDay = { viewModel.onPreviousDay() },
                onNextDay = { viewModel.onNextDay() },
                onToday = { viewModel.onToday() },
                enabled = uiState !is BacktestUiState.Running
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Dimen.PaddingM, vertical = Dimen.PaddingS)
            ) {
                when (val state = uiState) {
                    is BacktestUiState.Idle -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            BacktestIdleCard(
                                availableFtMatchesCount = state.availableFtMatchesCount,
                                onStartEvaluation = { viewModel.runBacktest() }
                            )
                        }
                    }

                    is BacktestUiState.Running -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            BacktestProcessingCard(
                                completedMatches = state.completedMatches,
                                totalMatches = state.totalMatches,
                                progressPercent = state.progressPercent,
                                currentMatchName = state.currentMatchName,
                                currentPhase = state.currentPhase,
                                onCancel = { viewModel.cancelBacktest() }
                            )
                        }
                    }

                    is BacktestUiState.Empty -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
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
                            modifier = Modifier.fillMaxSize(),
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
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(Dimen.PaddingM)
                        ) {
                            // 1. Overall Performance Card
                            BacktestOverviewCard(overview = state.overview)

                            // 2. Odds Coverage Card
                            BacktestOddsCoverageCard(oddsCoverage = state.oddsCoverage)

                            // 3. 3x3 Confusion Matrix Heatmap
                            ConfusionMatrixHeatmapCard(matrix = state.confusionMatrix)

                            // 4. Per-Class Metrics Breakdown
                            ClassMetricsBreakdownCard(classMetrics = state.classMetrics)

                            // 5. Daily Predictions Timeline
                            HistoricalTimelineCard(
                                allMatchesCount = state.allMatches.size,
                                correctCount = state.overview.correctMatches,
                                incorrectCount = state.overview.totalMatches - state.overview.correctMatches,
                                filteredMatches = state.filteredMatches,
                                selectedFilter = state.selectedFilter,
                                onFilterSelected = { viewModel.onFilterSelected(it) }
                            )

                            // 6. Data Leakage & Methodology Note Banner
                            MethodologyNoteBanner()

                            // 7. Re-evaluate Button
                            Button(
                                onClick = { viewModel.runBacktest() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(RadiusMedium),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(Dimen.SizeSM)
                                )
                                Spacer(modifier = Modifier.width(Dimen.PaddingXS))
                                Text(
                                    text = stringResource(R.string.backtest_btn_re_evaluate),
                                    style = MaterialTheme.typography.s14.bold()
                                )
                            }

                            Spacer(modifier = Modifier.height(Dimen.PaddingL))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateNavigationBar(
    selectedDate: String,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now(DateTimeFormatterUtils.VIETNAM_ZONE_ID).toString()
    val isToday = selectedDate == today

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = Dimen.PaddingM, vertical = Dimen.PaddingXS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onPreviousDay,
            enabled = enabled
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingXS)
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Dimen.SizeXS)
            )
            Text(
                text = selectedDate,
                style = MaterialTheme.typography.s14.bold(),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingXXS)
        ) {
            if (!isToday) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(RadiusPill))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .clickable(enabled = enabled) { onToday() }
                        .padding(horizontal = Dimen.PaddingS, vertical = Dimen.PaddingXXS)
                ) {
                    Text(
                        text = "Hôm nay",
                        style = MaterialTheme.typography.s10.semiBold(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(
                onClick = onNextDay,
                enabled = enabled
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
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
