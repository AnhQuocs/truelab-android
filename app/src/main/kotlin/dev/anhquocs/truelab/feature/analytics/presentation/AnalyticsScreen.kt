package dev.anhquocs.truelab.feature.analytics.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timeline
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
import dev.anhquocs.truelab.core.ui.theme.RadiusPill
import dev.anhquocs.truelab.core.ui.theme.SpacingXS
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.s10
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.s20
import dev.anhquocs.truelab.feature.analytics.presentation.components.DescriptiveStatsCard
import dev.anhquocs.truelab.feature.analytics.presentation.components.MultiProviderOddsCard
import dev.anhquocs.truelab.feature.analytics.presentation.components.OddsTrendCard
import dev.anhquocs.truelab.navigation.TrueLabMainLayout

private val TOP_BAR_HEIGHT = 75.dp

@Composable
fun AnalyticsScreen(
    modifier: Modifier = Modifier
) {
    TrueLabMainLayout(
        headerHeight = TOP_BAR_HEIGHT,
        header = {
            AnalyticsHeader(modifier = Modifier.fillMaxWidth())
        },
        modifier = modifier
    ) { contentModifier ->
        LazyColumn(
            modifier = contentModifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Dimen.PaddingM,
                end = Dimen.PaddingM,
                top = Dimen.PaddingM,
                bottom = Dimen.PaddingXXL
            ),
            verticalArrangement = Arrangement.spacedBy(Dimen.PaddingL)
        ) {
            // Module 1: Descriptive Statistics
            item {
                DescriptiveStatsCard()
            }

            // Module 2: Multi-Provider Odds Matrix
            item {
                MultiProviderOddsCard()
            }

            // Module 3: Odds Trend & Moving Average
            item {
                OddsTrendCard()
            }
        }
    }
}

@Composable
private fun AnalyticsHeader(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = Dimen.PaddingM),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f).padding(end = Dimen.PaddingS),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.analytics_title),
                style = MaterialTheme.typography.s20.bold(),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.analytics_subtitle),
                style = MaterialTheme.typography.s14,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(RadiusPill))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = Dimen.PaddingM, vertical = SpacingXS)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingXXS)
            ) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(Dimen.SizeS)
                )
                Text(
                    text = "EDA v2.4",
                    style = MaterialTheme.typography.s10.bold(),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
