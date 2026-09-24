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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.odds.model.MatchOdds
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusPill
import dev.anhquocs.truelab.core.ui.theme.RadiusSmall
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.medium
import dev.anhquocs.truelab.core.ui.utils.s10
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.semiBold
import java.util.Locale

@Composable
fun MultiProviderOddsCard(
    matchOdds: MatchOdds?,
    matchTitle: String,
    avgHomeOdds: Double?,
    avgDrawOdds: Double?,
    avgAwayOdds: Double?,
    oddsSpread: Double?,
    modifier: Modifier = Modifier
) {
    val oddsList = matchOdds?.oddsList ?: emptyList()
    val hasOdds = oddsList.isNotEmpty()

    val avgHomeStr = avgHomeOdds?.let { String.format(Locale.US, "%.2f", it) } ?: "—"
    val avgDrawStr = avgDrawOdds?.let { String.format(Locale.US, "%.2f", it) } ?: "—"
    val avgAwayStr = avgAwayOdds?.let { String.format(Locale.US, "%.2f", it) } ?: "—"

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
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = Dimen.PaddingS)) {
                    Text(
                        text = "2. " + stringResource(R.string.analytics_odds_comparison),
                        style = MaterialTheme.typography.s14.semiBold(),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = matchTitle,
                        style = MaterialTheme.typography.s12,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(RadiusPill))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = Dimen.PaddingS, vertical = Dimen.PaddingXXS)
                ) {
                    Text(
                        text = stringResource(R.string.analytics_provider_count, oddsList.size),
                        style = MaterialTheme.typography.s10.bold(),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimen.PaddingM))

            if (!hasOdds) {
                Text(
                    text = stringResource(R.string.analytics_no_odds),
                    style = MaterialTheme.typography.s12,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(vertical = Dimen.PaddingS)
                )
            } else {
                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(RadiusSmall))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(vertical = Dimen.PaddingXS, horizontal = Dimen.PaddingS),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.analytics_provider_header),
                        style = MaterialTheme.typography.s10.bold(),
                        modifier = Modifier.weight(1.3f)
                    )
                    Text(
                        text = "1 (Home)",
                        style = MaterialTheme.typography.s10.bold(),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "X (Draw)",
                        style = MaterialTheme.typography.s10.bold(),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "2 (Away)",
                        style = MaterialTheme.typography.s10.bold(),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                }

                Spacer(modifier = Modifier.height(Dimen.PaddingXXS))

                oddsList.forEach { item ->
                    OddsTableRow(
                        provider = item.companyName,
                        home = item.homeWin,
                        draw = item.draw,
                        away = item.awayWin
                    )
                }

                Spacer(modifier = Modifier.height(Dimen.PaddingS))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(Dimen.PaddingS))

                // Variance & Spread Metric Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.analytics_avg_odds, avgHomeStr, avgDrawStr, avgAwayStr),
                        style = MaterialTheme.typography.s12.semiBold(),
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (oddsSpread != null) {
                        Text(
                            text = "Độ lệch biên: ${String.format(Locale.US, "%.3f", oddsSpread)}",
                            style = MaterialTheme.typography.s10.medium(),
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OddsTableRow(
    provider: String,
    home: Double?,
    draw: Double?,
    away: Double?
) {
    val homeText = home?.let { String.format(Locale.US, "%.2f", it) } ?: "—"
    val drawText = draw?.let { String.format(Locale.US, "%.2f", it) } ?: "—"
    val awayText = away?.let { String.format(Locale.US, "%.2f", it) } ?: "—"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimen.PaddingXS, horizontal = Dimen.PaddingS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = provider,
            style = MaterialTheme.typography.s12.medium(),
            modifier = Modifier.weight(1.3f)
        )
        Text(
            text = homeText,
            style = MaterialTheme.typography.s12.bold(),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = drawText,
            style = MaterialTheme.typography.s12,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Text(
            text = awayText,
            style = MaterialTheme.typography.s12.bold(),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}
