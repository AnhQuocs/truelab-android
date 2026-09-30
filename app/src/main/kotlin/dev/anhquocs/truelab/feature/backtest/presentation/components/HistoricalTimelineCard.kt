package dev.anhquocs.truelab.feature.backtest.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusMedium
import dev.anhquocs.truelab.core.ui.theme.RadiusPill
import dev.anhquocs.truelab.core.ui.theme.RadiusSmall
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.s10
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.s16
import dev.anhquocs.truelab.core.ui.utils.semiBold
import dev.anhquocs.truelab.feature.backtest.presentation.model.BacktestFilter
import dev.anhquocs.truelab.feature.backtest.presentation.model.BacktestMatchUiRecord

@Composable
fun HistoricalTimelineCard(
    allMatchesCount: Int,
    correctCount: Int,
    incorrectCount: Int,
    filteredMatches: List<BacktestMatchUiRecord>,
    selectedFilter: BacktestFilter,
    onFilterSelected: (BacktestFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusMedium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimen.PaddingM)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingS)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(RadiusMedium))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(Dimen.PaddingXSPlus),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Dimen.SizeSM)
                    )
                }

                Text(
                    text = stringResource(R.string.backtest_timeline_title),
                    style = MaterialTheme.typography.s16.bold(),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(Dimen.PaddingM))

            // Filter Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingS)
            ) {
                FilterTab(
                    label = stringResource(R.string.backtest_filter_all, allMatchesCount),
                    isSelected = selectedFilter == BacktestFilter.ALL,
                    onClick = { onFilterSelected(BacktestFilter.ALL) },
                    modifier = Modifier.weight(1f)
                )

                FilterTab(
                    label = stringResource(R.string.backtest_filter_correct, correctCount),
                    isSelected = selectedFilter == BacktestFilter.CORRECT_ONLY,
                    onClick = { onFilterSelected(BacktestFilter.CORRECT_ONLY) },
                    modifier = Modifier.weight(1f)
                )

                FilterTab(
                    label = stringResource(R.string.backtest_filter_incorrect, incorrectCount),
                    isSelected = selectedFilter == BacktestFilter.INCORRECT_ONLY,
                    onClick = { onFilterSelected(BacktestFilter.INCORRECT_ONLY) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(Dimen.PaddingM))

            // Matches List
            if (filteredMatches.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimen.PaddingL),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.h2h_no_teams_match),
                        style = MaterialTheme.typography.s12,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                var visibleCount by remember(selectedFilter, filteredMatches.size) { mutableStateOf(50) }
                val displayedMatches = filteredMatches.take(visibleCount)

                Column(
                    verticalArrangement = Arrangement.spacedBy(Dimen.PaddingS)
                ) {
                    displayedMatches.forEachIndexed { index, match ->
                        BacktestMatchItem(match = match)
                        if (index < displayedMatches.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                        }
                    }

                    if (filteredMatches.size > visibleCount) {
                        val remaining = filteredMatches.size - visibleCount
                        val nextBatch = if (remaining > 50) 50 else remaining
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = Dimen.PaddingS),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.TextButton(
                                onClick = { visibleCount += 50 }
                            ) {
                                Text(
                                    text = "Xem thêm $nextBatch trận tiếp theo (còn $remaining trận)",
                                    style = MaterialTheme.typography.s12.bold(),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(RadiusPill))
            .background(containerColor)
            .clickable { onClick() }
            .padding(vertical = Dimen.PaddingS, horizontal = Dimen.PaddingXS),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.s12.semiBold(),
            color = textColor
        )
    }
}

@Composable
private fun BacktestMatchItem(
    match: BacktestMatchUiRecord,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimen.PaddingXS)
    ) {
        // Date & Status Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = match.formattedDate,
                style = MaterialTheme.typography.s10,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            StatusBadge(isCorrect = match.isCorrect)
        }

        Spacer(modifier = Modifier.height(Dimen.PaddingXS))

        // Teams & Confidence
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${match.homeTeamName} vs ${match.awayTeamName}",
                style = MaterialTheme.typography.s14.semiBold(),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = stringResource(R.string.backtest_confidence_format, match.confidencePct),
                style = MaterialTheme.typography.s10.semiBold(),
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(Dimen.PaddingXS))

        // Outcome Details (Pred vs Actual)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingM)
        ) {
            Text(
                text = stringResource(R.string.backtest_predicted_label, stringResource(match.predictedOutcomeRes)),
                style = MaterialTheme.typography.s12,
                color = if (match.isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )

            Text(
                text = stringResource(R.string.backtest_actual_label, stringResource(match.actualOutcomeRes)),
                style = MaterialTheme.typography.s12,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(Dimen.PaddingXS))

        // Probabilities Bar [PH % - PD % - PA %]
        ProbabilitiesBar(
            homePct = match.homeWinPct,
            drawPct = match.drawPct,
            awayPct = match.awayWinPct
        )
    }
}

@Composable
private fun StatusBadge(
    isCorrect: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isCorrect) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
    val contentColor = if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val text = if (isCorrect) stringResource(R.string.backtest_match_correct_badge) else stringResource(R.string.backtest_match_incorrect_badge)
    val icon = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Close

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(RadiusSmall))
            .background(bgColor)
            .padding(horizontal = Dimen.PaddingS, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingXXS)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.s10.bold(),
            color = contentColor
        )
    }
}

@Composable
private fun ProbabilitiesBar(
    homePct: Int,
    drawPct: Int,
    awayPct: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        if (homePct > 0) {
            Box(
                modifier = Modifier
                    .weight(homePct.toFloat())
                    .height(6.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        if (drawPct > 0) {
            Box(
                modifier = Modifier
                    .weight(drawPct.toFloat())
                    .height(6.dp)
                    .background(MaterialTheme.colorScheme.tertiary)
            )
        }
        if (awayPct > 0) {
            Box(
                modifier = Modifier
                    .weight(awayPct.toFloat())
                    .height(6.dp)
                    .background(MaterialTheme.colorScheme.secondary)
            )
        }
    }
}
