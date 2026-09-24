package dev.anhquocs.truelab.feature.match.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusMedium
import dev.anhquocs.truelab.core.ui.theme.RadiusPill
import dev.anhquocs.truelab.core.ui.theme.RadiusSmall
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.medium
import dev.anhquocs.truelab.core.ui.utils.s10
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s13
import dev.anhquocs.truelab.core.ui.utils.s15
import dev.anhquocs.truelab.core.ui.utils.s18
import dev.anhquocs.truelab.core.ui.utils.semiBold
import dev.anhquocs.truelab.feature.match.presentation.model.MatchDataRecord

@Composable
fun MatchDataCard(
    record: MatchDataRecord,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
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
            // 1. Top Meta Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(Dimen.PaddingXSPlus))
                    Text(
                        text = record.league,
                        style = MaterialTheme.typography.s12.semiBold(),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = " • ${record.date}",
                        style = MaterialTheme.typography.s12,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ResultBadge(result = record.actualResult)
            }

            Spacer(modifier = Modifier.height(Dimen.PaddingM))

            // 2. Teams Matchup Showcase
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home Team
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TeamAvatar(name = record.homeTeam)
                    Spacer(modifier = Modifier.width(Dimen.PaddingS))
                    Text(
                        text = record.homeTeam,
                        style = MaterialTheme.typography.s15.bold(),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }

                // Center Score Box
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(RadiusMedium))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                        .padding(horizontal = Dimen.PaddingM, vertical = Dimen.PaddingXSPlus),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${record.homeScore} - ${record.awayScore}",
                        style = MaterialTheme.typography.s18.bold(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Away Team
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = record.awayTeam,
                        style = MaterialTheme.typography.s15.bold(),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        textAlign = TextAlign.End
                    )
                    Spacer(modifier = Modifier.width(Dimen.PaddingS))
                    TeamAvatar(name = record.awayTeam)
                }
            }

            Spacer(modifier = Modifier.height(Dimen.PaddingM))

            // 3. Visual Odds Market Bar
            OddsMarketBar(
                homeOdds = record.avgHomeOdds,
                drawOdds = record.avgDrawOdds,
                awayOdds = record.avgAwayOdds
            )

            Spacer(modifier = Modifier.height(Dimen.PaddingS))

            // 4. Algorithm Features & Insights Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingXSPlus),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FeaturePill(
                    icon = Icons.Default.Bolt,
                    label = "Elo +${record.eloDiff}",
                    tint = Color(0xFFF59E0B)
                )
                FeaturePill(
                    icon = Icons.Default.SportsSoccer,
                    label = "${record.totalGoals} Bàn",
                    tint = Color(0xFF3B82F6)
                )
                Spacer(modifier = Modifier.weight(1f))
                FeaturePill(
                    icon = Icons.Default.AutoGraph,
                    label = record.predictedProb,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun TeamAvatar(name: String) {
    val initials = name.take(2).uppercase()
    Box(
        modifier = Modifier
            .size(Dimen.SizeL)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.s12.bold(),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun OddsMarketBar(
    homeOdds: Double,
    drawOdds: Double,
    awayOdds: Double
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMedium))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(vertical = Dimen.PaddingXSPlus, horizontal = Dimen.PaddingS),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OddsCell(label = "1 (Home)", value = homeOdds)
        Box(modifier = Modifier.width(1.dp).height(14.dp).background(MaterialTheme.colorScheme.outlineVariant))
        OddsCell(label = "X (Draw)", value = drawOdds)
        Box(modifier = Modifier.width(1.dp).height(14.dp).background(MaterialTheme.colorScheme.outlineVariant))
        OddsCell(label = "2 (Away)", value = awayOdds)
    }
}

@Composable
private fun OddsCell(label: String, value: Double) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingXS)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.s10,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "%.2f".format(value),
            style = MaterialTheme.typography.s12.bold(),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun FeaturePill(
    icon: ImageVector,
    label: String,
    tint: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(RadiusPill))
            .background(tint.copy(alpha = 0.12f))
            .padding(horizontal = Dimen.PaddingS, vertical = Dimen.PaddingXXS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingXXS)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.s10.bold(),
            color = tint
        )
    }
}

@Composable
private fun ResultBadge(result: String) {
    val (label, bgColor, textColor) = when (result) {
        "HOME_WIN" -> Triple("Chủ nhà Thắng", Color(0xFF10B981).copy(alpha = 0.15f), Color(0xFF059669))
        "DRAW" -> Triple("Hòa", Color(0xFFF59E0B).copy(alpha = 0.15f), Color(0xFFD97706))
        "AWAY_WIN" -> Triple("Đội khách Thắng", Color(0xFF3B82F6).copy(alpha = 0.15f), Color(0xFF2563EB))
        else -> Triple("Sắp diễn ra", Color(0xFF6B7280).copy(alpha = 0.15f), Color(0xFF4B5563))
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(RadiusPill))
            .background(bgColor)
            .padding(horizontal = Dimen.PaddingS, vertical = Dimen.PaddingXXS)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.s10.bold(),
            color = textColor
        )
    }
}
