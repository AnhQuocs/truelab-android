package dev.anhquocs.truelab.feature.prediction.presentation.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.ui.components.CompetitionLogo
import dev.anhquocs.truelab.core.ui.components.TeamLogo
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusSmall
import dev.anhquocs.truelab.core.ui.theme.SpacingS
import dev.anhquocs.truelab.core.ui.theme.SpacingXS
import dev.anhquocs.truelab.core.ui.theme.SpacingXXS
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.medium
import dev.anhquocs.truelab.core.ui.utils.s11
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s13
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.s18
import dev.anhquocs.truelab.core.ui.utils.semiBold

/**
 * Clean Matchup Overview Card with teams, logos, kickoff time, and subtle pre-match baseline notice.
 */
@Composable
fun MatchupOverviewCard(
    match: Match,
    leagueName: String?,
    leagueLogo: String?,
    isLive: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimen.PaddingM),
            verticalArrangement = Arrangement.spacedBy(SpacingS)
        ) {
            // Competition & Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    CompetitionLogo(logoUrl = leagueLogo, leagueName = leagueName, size = 18.dp)
                    Spacer(modifier = Modifier.width(SpacingXS))
                    Text(
                        text = leagueName ?: (if (match.leagueId != null) "League #${match.leagueId}" else stringResource(R.string.prediction_default_competition)),
                        style = MaterialTheme.typography.s12.medium(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isLive) {
                    LiveMinuteBadge(minutes = match.minutes)
                } else {
                    Text(
                        text = dev.anhquocs.truelab.core.ui.utils.DateTimeFormatterUtils.formatToVietnamDateTime(match.startTimeDate),
                        style = MaterialTheme.typography.s12.semiBold(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Teams & Score/VS (Symmetrical 2-column layout)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SpacingXS),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home Team (50% left)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TeamLogo(
                        logoUrl = match.homeTeam.logo,
                        teamName = match.homeTeam.name,
                        size = 38.dp
                    )
                    Spacer(modifier = Modifier.height(SpacingXS))
                    Text(
                        text = match.homeTeam.name,
                        style = MaterialTheme.typography.s13.semiBold(),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Middle: Score or VS
                Box(
                    modifier = Modifier.padding(horizontal = SpacingS),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLive || match.status == MatchStatus.ENDED) {
                        Text(
                            text = "${match.homeScore ?: 0} : ${match.awayScore ?: 0}",
                            style = MaterialTheme.typography.s18.bold(),
                            color = if (isLive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Text(
                            text = "VS",
                            style = MaterialTheme.typography.s12.bold(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                // Away Team (50% right)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TeamLogo(
                        logoUrl = match.awayTeam.logo,
                        teamName = match.awayTeam.name,
                        size = 38.dp
                    )
                    Spacer(modifier = Modifier.height(SpacingXS))
                    Text(
                        text = match.awayTeam.name,
                        style = MaterialTheme.typography.s13.semiBold(),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Subtle Pre-match informational notice
            if (isLive) {
                Surface(
                    shape = RoundedCornerShape(RadiusSmall),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Dimen.PaddingS, vertical = SpacingXS),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(SpacingXS))
                        Text(
                            text = stringResource(R.string.prediction_live_baseline_desc),
                            style = MaterialTheme.typography.s11,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
