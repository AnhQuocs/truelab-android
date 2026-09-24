package dev.anhquocs.truelab.feature.team.presentation.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusPill
import dev.anhquocs.truelab.core.ui.theme.RadiusSmall
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.s10
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s13
import dev.anhquocs.truelab.core.ui.utils.s16
import dev.anhquocs.truelab.core.ui.utils.semiBold
import dev.anhquocs.truelab.feature.team.presentation.model.TeamAnalyticsRecord

@Composable
fun TeamAnalyticsCard(
    team: TeamAnalyticsRecord,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
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
            // 1. Team Header Row: Rank, Avatar, Name, League, Elo Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rank Badge
                Box(
                    modifier = Modifier
                        .size(Dimen.SizeM)
                        .clip(RoundedCornerShape(RadiusSmall))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${team.rank}",
                        style = MaterialTheme.typography.s12.bold(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(Dimen.PaddingS))

                // Team Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = team.name,
                        style = MaterialTheme.typography.s16.bold(),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = team.league,
                        style = MaterialTheme.typography.s12,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Elo Rating Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(RadiusPill))
                        .background(Color(0xFFF59E0B).copy(alpha = 0.15f))
                        .padding(horizontal = Dimen.PaddingS, vertical = Dimen.PaddingXXS)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingXXS)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${team.eloRating} Elo",
                            style = MaterialTheme.typography.s12.bold(),
                            color = Color(0xFFD97706)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimen.PaddingM))

            // 2. Form Score & 5-Match Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.teams_form_score_label),
                        style = MaterialTheme.typography.s10,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${team.formScore}/15 pts (${(team.formScore * 100) / 15}%)",
                        style = MaterialTheme.typography.s13.bold(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Form W/D/L Badges
                Row(horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingXS)) {
                    team.form.forEach { res ->
                        FormPillBadge(res = res)
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimen.PaddingS))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(Dimen.PaddingS))

            // 3. Home / Away Splits & H2H Highlights
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top // Căn trên thay vì Center để các block không bị lệch
            ) {
                // Home Stats
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingXXS)) {
                        Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                        Text(text = stringResource(R.string.teams_home_stats), style = MaterialTheme.typography.s10, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(Dimen.PaddingXXS))
                    Text(
                        text = team.homeWinRate?.let { "$it%" } ?: "—",
                        style = MaterialTheme.typography.s12.semiBold(),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = team.homeRecord,
                        style = MaterialTheme.typography.s10,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Away Stats
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingXXS)) {
                        Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(12.dp))
                        Text(text = stringResource(R.string.teams_away_stats), style = MaterialTheme.typography.s10, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(Dimen.PaddingXXS))
                    Text(
                        text = team.awayWinRate?.let { "$it%" } ?: "—",
                        style = MaterialTheme.typography.s12.semiBold(),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = team.awayRecord,
                        style = MaterialTheme.typography.s10,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // H2H
                Column(
                    modifier = Modifier.weight(1.5f), // Cho H2H nhiều không gian hơn vì text dài
                    horizontalAlignment = Alignment.End
                ) {
                    Text(text = stringResource(R.string.teams_h2h_highlight), style = MaterialTheme.typography.s10, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(Dimen.PaddingXXS))
                    Text(
                        text = team.h2hHighlight,
                        style = MaterialTheme.typography.s12.bold(),
                        color = Color(0xFF10B981),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
private fun FormPillBadge(res: Char) {
    val (bgColor, textColor) = when (res) {
        'W' -> Color(0xFF10B981) to Color.White
        'D' -> Color(0xFFF59E0B) to Color.White
        else -> Color(0xFFEF4444) to Color.White
    }

    Box(
        modifier = Modifier
            .size(Dimen.SizeS2)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = res.toString(),
            color = textColor,
            style = MaterialTheme.typography.s10.bold()
        )
    }
}
