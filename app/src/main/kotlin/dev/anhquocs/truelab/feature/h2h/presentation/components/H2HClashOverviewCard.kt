package dev.anhquocs.truelab.feature.h2h.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.team.model.TeamDetail
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusSmall
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.s10
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.s20
import dev.anhquocs.truelab.core.ui.utils.semiBold
import dev.anhquocs.truelab.feature.h2h.presentation.model.H2HComparisonRecord

@Composable
fun H2HClashOverviewCard(
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.h2h_clash_summary_title),
                    style = MaterialTheme.typography.s14.semiBold(),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = stringResource(R.string.h2h_total_clashes, comparison.totalMatches),
                    style = MaterialTheme.typography.s12.semiBold(),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(Dimen.PaddingM))

            if (comparison.totalMatches == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimen.PaddingM),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.h2h_no_past_clashes),
                        style = MaterialTheme.typography.s12,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Segmented Progress Bar
                val aWeight = if (comparison.teamAWinPercent > 0) comparison.teamAWinPercent else 0.001f
                val dWeight = if (comparison.drawPercent > 0) comparison.drawPercent else 0.001f
                val bWeight = if (comparison.teamBWinPercent > 0) comparison.teamBWinPercent else 0.001f

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimen.Height.ProgressBarThick)
                        .clip(RoundedCornerShape(RadiusSmall))
                ) {
                    Box(
                        modifier = Modifier
                            .weight(aWeight)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Box(
                        modifier = Modifier
                            .weight(dWeight)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    Box(
                        modifier = Modifier
                            .weight(bWeight)
                            .fillMaxHeight()
                            .background(Color(0xFFE57373))
                    )
                }

                Spacer(modifier = Modifier.height(Dimen.PaddingS))

                // Stats breakdown row (Team A wins | Draws | Team B wins)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "${comparison.teamAWins} ${stringResource(R.string.h2h_team_a_wins_label)}",
                            style = MaterialTheme.typography.s12.bold(),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = comparison.teamAWinPercentFormatted,
                            style = MaterialTheme.typography.s10,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${comparison.draws} ${stringResource(R.string.h2h_draws_label)}",
                            style = MaterialTheme.typography.s12.bold(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = comparison.drawPercentFormatted,
                            style = MaterialTheme.typography.s10,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${comparison.teamBWins} ${stringResource(R.string.h2h_team_b_wins_label)}",
                            style = MaterialTheme.typography.s12.bold(),
                            color = Color(0xFFE57373)
                        )
                        Text(
                            text = comparison.teamBWinPercentFormatted,
                            style = MaterialTheme.typography.s10,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Dimen.PaddingM))

                // Goals direct comparison
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(RadiusSmall))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(horizontal = Dimen.PaddingM, vertical = Dimen.PaddingS),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${comparison.teamAGoals}",
                        style = MaterialTheme.typography.s20.bold(),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = stringResource(R.string.h2h_total_goals_scored),
                        style = MaterialTheme.typography.s12.semiBold(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "${comparison.teamBGoals}",
                        style = MaterialTheme.typography.s20.bold(),
                        color = Color(0xFFE57373)
                    )
                }
            }
        }
    }
}
