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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.team.model.TeamDetail
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusMedium
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.s10
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.s16
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                // Team A Home Split Box
                SplitBox(
                    teamName = teamA.name,
                    roleLabel = stringResource(R.string.h2h_home_record_label),
                    record = comparison.teamAHomeRecord,
                    winRate = comparison.teamAHomeWinRate,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                // Team B Away Split Box
                SplitBox(
                    teamName = teamB.name,
                    roleLabel = stringResource(R.string.h2h_away_record_label),
                    record = comparison.teamBAwayRecord,
                    winRate = comparison.teamBAwayWinRate,
                    accentColor = Color(0xFFE57373),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SplitBox(
    teamName: String,
    roleLabel: String,
    record: String,
    winRate: String,
    accentColor: Color,
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
                style = MaterialTheme.typography.s12.semiBold(),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                textAlign = TextAlign.Center
            )

            Text(
                text = roleLabel,
                style = MaterialTheme.typography.s10,
                color = accentColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Dimen.PaddingS))

            Text(
                text = record,
                style = MaterialTheme.typography.s16.bold(),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "$winRate Win",
                style = MaterialTheme.typography.s12.bold(),
                color = accentColor
            )
        }
    }
}
