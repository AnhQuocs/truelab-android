package dev.anhquocs.truelab.feature.analytics.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.odds.model.OddsTrendAnalysis
import dev.anhquocs.truelab.core.domain.odds.model.TargetOddsField
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusPill
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.medium
import dev.anhquocs.truelab.core.ui.utils.s10
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s13
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.semiBold
import java.util.Locale

@Composable
fun OddsTrendCard(
    oddsTrend: OddsTrendAnalysis?,
    selectedTargetField: TargetOddsField,
    selectedWindowSize: Int,
    onTargetFieldSelected: (TargetOddsField) -> Unit,
    onWindowSizeChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasData = oddsTrend != null && oddsTrend.rawOddsSeries.isNotEmpty()
    val latestSma = oddsTrend?.smaSeries?.lastOrNull()
    val opening = oddsTrend?.openingOdds
    val current = oddsTrend?.currentOdds

    val smaStr = if (latestSma != null) {
        String.format(Locale.US, "%.2f", latestSma)
    } else {
        "—"
    }

    val openingStr = opening?.let { String.format(Locale.US, "%.2f", it) } ?: "—"
    val currentStr = current?.let { String.format(Locale.US, "%.2f", it) } ?: "—"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusLarge),
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
                horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingXSPlus)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ShowChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Dimen.SizeS)
                )
                Text(
                    text = "3. " + stringResource(R.string.analytics_trend_title),
                    style = MaterialTheme.typography.s14.semiBold(),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(Dimen.PaddingS))

            // Control Chips Row: Target Odds Field Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingXS)
            ) {
                TargetOddsField.values().filter {
                    it == TargetOddsField.HOME_WIN || it == TargetOddsField.DRAW || it == TargetOddsField.AWAY_WIN
                }.forEach { field ->
                    val isSelected = field == selectedTargetField
                    val label = when (field) {
                        TargetOddsField.HOME_WIN -> stringResource(R.string.analytics_target_home)
                        TargetOddsField.DRAW -> stringResource(R.string.analytics_target_draw)
                        TargetOddsField.AWAY_WIN -> stringResource(R.string.analytics_target_away)
                        else -> field.name
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { onTargetFieldSelected(field) },
                        label = { Text(text = label, style = MaterialTheme.typography.s10) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            // Window Size Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingXS)
            ) {
                listOf(2, 3, 5).forEach { window ->
                    val isSelected = window == selectedWindowSize
                    FilterChip(
                        selected = isSelected,
                        onClick = { onWindowSizeChanged(window) },
                        label = {
                            Text(
                                text = stringResource(R.string.analytics_window_size, window),
                                style = MaterialTheme.typography.s10
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimen.PaddingS))

            if (!hasData) {
                Text(
                    text = stringResource(R.string.analytics_no_odds),
                    style = MaterialTheme.typography.s12,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(vertical = Dimen.PaddingS)
                )
            } else {
                // Line Chart Visualizer
                OddsTrendLineChart(
                    rawOdds = oddsTrend!!.rawOddsSeries,
                    smaOdds = oddsTrend.smaSeries,
                    windowSize = selectedWindowSize,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(Dimen.PaddingM))

                // Moving Average Metric
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Moving Average (${stringResource(R.string.analytics_window_size, selectedWindowSize)})",
                            style = MaterialTheme.typography.s10,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "SMA: $smaStr",
                            style = MaterialTheme.typography.s13.bold(),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(RadiusPill))
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                            .padding(horizontal = Dimen.PaddingS, vertical = Dimen.PaddingXXS)
                    ) {
                        Text(
                            text = stringResource(R.string.analytics_volatility_val, oddsTrend.volatility),
                            style = MaterialTheme.typography.s10.bold(),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Dimen.PaddingS))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(Dimen.PaddingS))

                // Odds Shift Snapshot
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.analytics_odds_shift, openingStr, currentStr),
                            style = MaterialTheme.typography.s12.medium(),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "N = ${oddsTrend.rawOddsSeries.size} điểm dữ liệu",
                            style = MaterialTheme.typography.s10,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val shiftDiff = if (opening != null && current != null) current - opening else 0.0
                    val (icon, tint) = when {
                        shiftDiff > 0.01 -> Icons.AutoMirrored.Filled.TrendingUp to Color(0xFF10B981)
                        shiftDiff < -0.01 -> Icons.AutoMirrored.Filled.TrendingDown to Color(0xFFEF4444)
                        else -> Icons.AutoMirrored.Filled.TrendingFlat to MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(Dimen.SizeM)
                    )
                }
            }
        }
    }
}
