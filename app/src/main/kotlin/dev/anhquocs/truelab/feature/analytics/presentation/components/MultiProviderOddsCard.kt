package dev.anhquocs.truelab.feature.analytics.presentation.components

import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.odds.model.MatchOdds
import dev.anhquocs.truelab.core.domain.odds.model.OddsRecordItem
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusMedium
import dev.anhquocs.truelab.core.ui.theme.RadiusPill
import dev.anhquocs.truelab.core.ui.theme.RadiusSmall
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.medium
import dev.anhquocs.truelab.core.ui.utils.s10
import dev.anhquocs.truelab.core.ui.utils.s11
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s13
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.semiBold
import java.util.Locale

private const val DEFAULT_VISIBLE_PROVIDERS = 5

@Composable
fun MultiProviderOddsCard(
    matchOdds: MatchOdds?,
    matchTitle: String,
    avgHomeOdds: Double?,
    avgDrawOdds: Double?,
    avgAwayOdds: Double?,
    oddsSpread: Double?,
    modifier: Modifier = Modifier,
    isOddsLoading: Boolean = false
) {
    // Deduplicate and filter out completely empty odds rows
    val distinctOddsList = remember(matchOdds) {
        matchOdds?.oddsList
            ?.filter { item ->
                val hw = item.homeWin
                val d = item.draw
                val aw = item.awayWin
                (hw != null && hw > 0.0) ||
                    (d != null && d > 0.0) ||
                    (aw != null && aw > 0.0)
            }
            ?.groupBy { it.companyName }
            ?.map { (_, records) ->
                records.maxByOrNull {
                    (if (it.homeWin != null) 1 else 0) +
                        (if (it.draw != null) 1 else 0) +
                        (if (it.awayWin != null) 1 else 0)
                } ?: records.first()
            }
            ?.sortedBy { it.companyName }
            ?: emptyList()
    }

    var isExpanded by remember { mutableStateOf(false) }
    val hasOdds = distinctOddsList.isNotEmpty()
    val displayList = if (isExpanded || distinctOddsList.size <= DEFAULT_VISIBLE_PROVIDERS) {
        distinctOddsList
    } else {
        distinctOddsList.take(DEFAULT_VISIBLE_PROVIDERS)
    }

    val avgHomeStr = avgHomeOdds?.let { String.format(Locale.US, "%.2f", it) } ?: "—"
    val avgDrawStr = avgDrawOdds?.let { String.format(Locale.US, "%.2f", it) } ?: "—"
    val avgAwayStr = avgAwayOdds?.let { String.format(Locale.US, "%.2f", it) } ?: "—"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusLarge),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimen.Elevation1)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimen.PaddingM)
                .animateContentSize()
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(RadiusPill))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = Dimen.PaddingS, vertical = Dimen.PaddingXXS)
                ) {
                    Text(
                        text = stringResource(R.string.analytics_provider_count, distinctOddsList.size),
                        style = MaterialTheme.typography.s10.bold(),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimen.PaddingM))

            if (!hasOdds) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(RadiusMedium))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .padding(vertical = Dimen.PaddingL, horizontal = Dimen.PaddingM),
                    contentAlignment = Alignment.Center
                ) {
                    if (isOddsLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingS)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.analytics_loading_odds),
                                style = MaterialTheme.typography.s12,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.analytics_no_odds),
                            style = MaterialTheme.typography.s12,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Pinned Average Odds Summary Banner at TOP
                AvgOddsSummaryBanner(
                    avgHomeStr = avgHomeStr,
                    avgDrawStr = avgDrawStr,
                    avgAwayStr = avgAwayStr,
                    oddsSpread = oddsSpread
                )

                Spacer(modifier = Modifier.height(Dimen.PaddingM))

                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(RadiusSmall))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(vertical = Dimen.PaddingXS, horizontal = Dimen.PaddingS),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
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

                // Odds Rows
                displayList.forEachIndexed { index, item ->
                    OddsTableRow(
                        provider = item.companyName,
                        home = item.homeWin,
                        draw = item.draw,
                        away = item.awayWin,
                        isAlternate = index % 2 == 1
                    )
                }

                // Expand / Collapse Action Button
                if (distinctOddsList.size > DEFAULT_VISIBLE_PROVIDERS) {
                    Spacer(modifier = Modifier.height(Dimen.PaddingXS))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(RadiusPill))
                            .clickable { isExpanded = !isExpanded }
                            .padding(vertical = Dimen.PaddingS),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingXXS)
                        ) {
                            Text(
                                text = if (isExpanded) {
                                    stringResource(R.string.analytics_show_less_providers)
                                } else {
                                    stringResource(R.string.analytics_show_all_providers, distinctOddsList.size)
                                },
                                style = MaterialTheme.typography.s12.bold(),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AvgOddsSummaryBanner(
    avgHomeStr: String,
    avgDrawStr: String,
    avgAwayStr: String,
    oddsSpread: Double?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusSmall))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
            .padding(horizontal = Dimen.PaddingM, vertical = Dimen.PaddingS)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.analytics_avg_odds, avgHomeStr, avgDrawStr, avgAwayStr),
                    style = MaterialTheme.typography.s12.semiBold(),
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (oddsSpread != null) {
                Text(
                    text = "Δ biên: ${String.format(Locale.US, "%.3f", oddsSpread)}",
                    style = MaterialTheme.typography.s10.medium(),
                    color = Color(0xFF10B981)
                )
            }
        }
    }
}

@Composable
private fun OddsTableRow(
    provider: String,
    home: Double?,
    draw: Double?,
    away: Double?,
    isAlternate: Boolean = false
) {
    val homeText = home?.let { String.format(Locale.US, "%.2f", it) } ?: "—"
    val drawText = draw?.let { String.format(Locale.US, "%.2f", it) } ?: "—"
    val awayText = away?.let { String.format(Locale.US, "%.2f", it) } ?: "—"

    val rowBg = if (isAlternate) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(rowBg)
            .padding(vertical = Dimen.PaddingXS, horizontal = Dimen.PaddingS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = provider,
            style = MaterialTheme.typography.s12.medium(),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
