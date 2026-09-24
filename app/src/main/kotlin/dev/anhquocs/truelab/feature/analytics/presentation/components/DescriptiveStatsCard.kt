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
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.team.model.TeamPerformanceStatistics
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusMedium
import dev.anhquocs.truelab.core.ui.theme.RadiusPill
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.s10
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.s18
import dev.anhquocs.truelab.core.ui.utils.semiBold
import java.util.Locale

@Composable
fun DescriptiveStatsCard(
    teamStats: TeamPerformanceStatistics?,
    modifier: Modifier = Modifier
) {
    val stats = teamStats?.totalGoalsStats
    val hasData = teamStats != null && teamStats.matchesCount > 0

    val meanStr = if (hasData) String.format(Locale.US, "%.2f", stats!!.mean) else "—"
    val medianStr = if (hasData) String.format(Locale.US, "%.2f", stats!!.median) else "—"
    val stdDevStr = if (hasData) String.format(Locale.US, "±%.2f", stats!!.sampleStandardDeviation) else "—"
    val varianceStr = if (hasData) String.format(Locale.US, "%.2f", stats!!.sampleVariance) else "—"
    val minMaxStr = if (hasData) "${stats!!.min.toInt()} - ${stats.max.toInt()}" else "—"
    val skewnessStr = if (hasData) String.format(Locale.US, "%+.2f", stats!!.skewness) else "—"

    val skewnessSubtext = if (hasData) {
        when {
            stats!!.skewness > 0.1 -> "Lệch phải (Right-skewed)"
            stats.skewness < -0.1 -> "Lệch trái (Left-skewed)"
            else -> "Đối xứng (Symmetric)"
        }
    } else {
        "Hệ số bất đối xứng"
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = Dimen.PaddingS),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingXSPlus)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoGraph,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Dimen.SizeS)
                    )
                    Text(
                        text = "1. " + stringResource(R.string.analytics_descriptive_stats),
                        style = MaterialTheme.typography.s14.semiBold(),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(RadiusPill))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = Dimen.PaddingS, vertical = Dimen.PaddingXXS)
                ) {
                    Text(
                        text = stringResource(R.string.analytics_matches_count, teamStats?.matchesCount ?: 0),
                        style = MaterialTheme.typography.s10.bold(),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimen.PaddingM))

            // 2x3 Grid of Descriptive Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingS)
            ) {
                StatGridItem(
                    label = stringResource(R.string.analytics_mean_goals),
                    value = meanStr,
                    subtext = stringResource(R.string.analytics_goals_match),
                    modifier = Modifier.weight(1f)
                )
                StatGridItem(
                    label = stringResource(R.string.analytics_median_goals),
                    value = medianStr,
                    subtext = stringResource(R.string.analytics_goal_median_desc),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(Dimen.PaddingS))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingS)
            ) {
                StatGridItem(
                    label = stringResource(R.string.analytics_std_dev),
                    value = stdDevStr,
                    subtext = "σ phân tán (StdDev)",
                    modifier = Modifier.weight(1f)
                )
                StatGridItem(
                    label = stringResource(R.string.analytics_variance),
                    value = varianceStr,
                    subtext = "σ² biến thiên (Variance)",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(Dimen.PaddingS))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingS)
            ) {
                StatGridItem(
                    label = stringResource(R.string.analytics_min_max_goals),
                    value = minMaxStr,
                    subtext = "Khoảng Min/Max",
                    modifier = Modifier.weight(1f)
                )
                StatGridItem(
                    label = stringResource(R.string.analytics_skewness),
                    value = skewnessStr,
                    subtext = skewnessSubtext,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatGridItem(
    label: String,
    value: String,
    subtext: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(RadiusMedium))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(Dimen.PaddingS)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.s10,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Dimen.PaddingXXS))
            Text(
                text = value,
                style = MaterialTheme.typography.s18.bold(),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.s10,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
