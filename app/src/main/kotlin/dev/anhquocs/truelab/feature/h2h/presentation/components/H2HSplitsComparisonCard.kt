package dev.anhquocs.truelab.feature.h2h.presentation.components

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.team.model.TeamDetail
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusMedium
import dev.anhquocs.truelab.core.ui.theme.SpacingXXS
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.s10
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.semiBold
import dev.anhquocs.truelab.feature.h2h.presentation.model.H2HComparisonRecord

@Composable
fun H2HSplitsComparisonCard(
    teamA: TeamDetail,
    teamB: TeamDetail,
    comparison: H2HComparisonRecord,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimen.Elevation2)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimen.PaddingM)
        ) {
            Text(
                text = stringResource(R.string.h2h_splits_comparison_title),
                style = MaterialTheme.typography.s14.semiBold(),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(Dimen.PaddingM))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingM)
            ) {
                // Team A Splits Box
                TeamSplitsBox(
                    teamName = teamA.name,
                    homeRecord = comparison.teamAHomeRecord,
                    homeWinRate = comparison.teamAHomeWinRate,
                    awayRecord = comparison.teamAAwayRecord,
                    awayWinRate = comparison.teamAAwayWinRate,
                    teamColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                // Team B Splits Box
                TeamSplitsBox(
                    teamName = teamB.name,
                    homeRecord = comparison.teamBHomeRecord,
                    homeWinRate = comparison.teamBHomeWinRate,
                    awayRecord = comparison.teamBAwayRecord,
                    awayWinRate = comparison.teamBAwayWinRate,
                    teamColor = Color(0xFFE57373),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TeamSplitsBox(
    teamName: String,
    homeRecord: String,
    homeWinRate: String,
    awayRecord: String,
    awayWinRate: String,
    teamColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(RadiusMedium))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(Dimen.PaddingM),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = teamName,
                style = MaterialTheme.typography.s14.semiBold(),
                color = teamColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Dimen.PaddingS))

            // Home Subsection
            Text(
                text = stringResource(R.string.h2h_home_record_label),
                style = MaterialTheme.typography.s10.semiBold(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(SpacingXXS))
            Text(
                text = homeRecord,
                style = MaterialTheme.typography.s12.bold(),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = "$homeWinRate Win",
                style = MaterialTheme.typography.s10.semiBold(),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = Dimen.PaddingS),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )

            // Away Subsection
            Text(
                text = stringResource(R.string.h2h_away_record_label),
                style = MaterialTheme.typography.s10.semiBold(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(SpacingXXS))
            Text(
                text = awayRecord,
                style = MaterialTheme.typography.s12.bold(),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = "$awayWinRate Win",
                style = MaterialTheme.typography.s10.semiBold(),
                color = Color(0xFFE57373),
                textAlign = TextAlign.Center
            )
        }
    }
}
