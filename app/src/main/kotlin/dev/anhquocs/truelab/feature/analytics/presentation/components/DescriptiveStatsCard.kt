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
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusMedium
import dev.anhquocs.truelab.core.ui.theme.RadiusPill
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.s10
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.s18
import dev.anhquocs.truelab.core.ui.utils.semiBold

@Composable
fun DescriptiveStatsCard(
    modifier: Modifier = Modifier
) {
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
                verticalAlignment = Alignment.Top, // Đảm bảo căn trên để badge không bị kéo lệch
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = Dimen.PaddingS), // Cho title chiếm weight để badge không bị bóp méo
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
                        maxLines = 2 // Cho phép rớt dòng tối đa 2 dòng nếu màn hình quá nhỏ
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(RadiusPill))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = Dimen.PaddingS, vertical = Dimen.PaddingXXS)
                ) {
                    Text(
                        text = "N = 75,000 trận",
                        style = MaterialTheme.typography.s10.bold(),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1 // Chống rớt dòng tạo hình con giun
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
                    value = "2.84",
                    subtext = stringResource(R.string.analytics_goals_match),
                    modifier = Modifier.weight(1f)
                )
                StatGridItem(
                    label = stringResource(R.string.analytics_median_goals),
                    value = "3.00",
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
                    value = "±1.42",
                    subtext = "σ phân tán",
                    modifier = Modifier.weight(1f)
                )
                StatGridItem(
                    label = stringResource(R.string.analytics_variance),
                    value = "2.01",
                    subtext = "σ² biến thiên",
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
                    value = "0 - 8",
                    subtext = "Bàn thắng tối đa",
                    modifier = Modifier.weight(1f)
                )
                StatGridItem(
                    label = stringResource(R.string.analytics_skewness),
                    value = "+0.42",
                    subtext = "Lệch phải nhẹ",
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
