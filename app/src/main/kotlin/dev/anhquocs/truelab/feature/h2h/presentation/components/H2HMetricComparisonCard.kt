package dev.anhquocs.truelab.feature.h2h.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.team.model.TeamDetail
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusSmall
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.s10
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.semiBold
import dev.anhquocs.truelab.feature.h2h.presentation.model.H2HComparisonRecord

@Composable
fun H2HMetricComparisonCard(
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
                text = stringResource(R.string.h2h_metrics_comparison_title),
                style = MaterialTheme.typography.s14.semiBold(),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(Dimen.PaddingM))

            // Elo Rating Row
            MetricComparisonRow(
                title = stringResource(R.string.h2h_elo_rating_label),
                valueA = comparison.teamAElo,
                valueB = comparison.teamBElo,
                badge = comparison.eloDiffFormatted
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = Dimen.PaddingS),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )

            // Form Score Row
            MetricComparisonRow(
                title = stringResource(R.string.h2h_form_score_label),
                valueA = comparison.teamAFormScore,
                valueB = comparison.teamBFormScore
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = Dimen.PaddingS),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )

            // 5-Match Streak Row
            MetricComparisonRow(
                title = stringResource(R.string.h2h_streak_label),
                valueA = comparison.teamAStreak,
                valueB = comparison.teamBStreak
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = Dimen.PaddingS),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )

            // Mean Goals per match Row
            MetricComparisonRow(
                title = stringResource(R.string.h2h_mean_goals_label),
                valueA = comparison.teamAMeanGoals,
                valueB = comparison.teamBMeanGoals
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = Dimen.PaddingS),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )

            // Goal Differential Row
            MetricComparisonRow(
                title = stringResource(R.string.h2h_goal_diff_label),
                valueA = comparison.teamAGoalDiff,
                valueB = comparison.teamBGoalDiff
            )
        }
    }
}

@Composable
private fun MetricComparisonRow(
    title: String,
    valueA: String,
    valueB: String,
    badge: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = valueA,
            style = MaterialTheme.typography.s14.bold(),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(2f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.s12.semiBold(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (badge != null) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.s10.bold(),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(RadiusSmall))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = Dimen.PaddingXS, vertical = 2.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Text(
            text = valueB,
            style = MaterialTheme.typography.s14.bold(),
            color = Color(0xFFE57373),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}
