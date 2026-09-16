package dev.anhquocs.truelab.feature.analytics.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusMedium
import dev.anhquocs.truelab.core.ui.theme.SpacingL
import dev.anhquocs.truelab.core.ui.theme.SpacingM
import dev.anhquocs.truelab.core.ui.theme.SpacingS
import dev.anhquocs.truelab.core.ui.theme.SpacingXL
import dev.anhquocs.truelab.core.ui.theme.SpacingXS
import dev.anhquocs.truelab.navigation.TrueLabMainLayout

@Composable
fun AnalyticsScreen(
    modifier: Modifier = Modifier
) {
    TrueLabMainLayout(
        modifier = modifier,
        header = {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = SpacingL)) {
                Text(
                    text = stringResource(R.string.analytics_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { contentModifier ->
        LazyColumn(
            modifier = contentModifier
                .fillMaxSize()
                .padding(horizontal = SpacingL),
            verticalArrangement = Arrangement.spacedBy(SpacingM),
            contentPadding = PaddingValues(bottom = SpacingXL)
        ) {
            item {
                Spacer(modifier = Modifier.height(SpacingS))
                DescriptiveStatsSection()
            }

            item {
                Spacer(modifier = Modifier.height(SpacingXS))
                Text(
                    text = stringResource(R.string.analytics_odds_comparison),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                OddsComparisonCard()
            }
        }
    }
}

@Composable
private fun DescriptiveStatsSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SpacingS)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SpacingS)
        ) {
            StatMetricCard(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.analytics_mean_goals),
                value = "2.84",
                subtext = "Goals/match"
            )
            StatMetricCard(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.analytics_median_goals),
                value = "3.00",
                subtext = "Goal median"
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SpacingS)
        ) {
            StatMetricCard(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.analytics_std_dev),
                value = "±1.42",
                subtext = "Variance: 2.01"
            )
            StatMetricCard(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.analytics_odds_variance),
                value = "0.038",
                subtext = "Spread index"
            )
        }
    }
}

@Composable
private fun StatMetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    subtext: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(RadiusMedium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingM)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(SpacingXS))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(SpacingXS))
            Text(
                text = subtext,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun OddsComparisonCard() {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusLarge)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingL)
        ) {
            Text(
                text = "Arsenal vs Chelsea",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(SpacingM))

            // Table Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Provider",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1.2f)
                )
                Text(
                    text = "1 (Home)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "X (Draw)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "2 (Away)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
            }

            Spacer(modifier = Modifier.height(SpacingS))
            HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(SpacingS))

            OddsRow(provider = "Bet365", home = "1.85", draw = "3.60", away = "4.20")
            OddsRow(provider = "Pinnacle", home = "1.88", draw = "3.65", away = "4.10")
            OddsRow(provider = "Bwin", home = "1.83", draw = "3.50", away = "4.30")
        }
    }
}

@Composable
private fun OddsRow(
    provider: String,
    home: String,
    draw: String,
    away: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = SpacingXS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = provider,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1.2f)
        )
        Text(
            text = home,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = draw,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Text(
            text = away,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}
