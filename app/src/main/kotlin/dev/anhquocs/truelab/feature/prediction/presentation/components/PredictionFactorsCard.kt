package dev.anhquocs.truelab.feature.prediction.presentation.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.domain.prediction.model.PredictionResult
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusPill
import dev.anhquocs.truelab.core.ui.theme.RadiusSmall
import dev.anhquocs.truelab.core.ui.theme.SpacingS
import dev.anhquocs.truelab.core.ui.theme.SpacingXS
import dev.anhquocs.truelab.core.ui.theme.SpacingXXS
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.medium
import dev.anhquocs.truelab.core.ui.utils.s10
import dev.anhquocs.truelab.core.ui.utils.s11
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s13
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.semiBold

/**
 * Visual card breaking down key prediction factors, Elo comparison, and algorithm information.
 */
@Composable
fun PredictionFactorsCard(
    match: Match,
    predictionResult: PredictionResult,
    homeElo: Double?,
    awayElo: Double?,
    modifier: Modifier = Modifier
) {
    val homeEloStr = homeElo?.toInt()?.toString() ?: "—"
    val awayEloStr = awayElo?.toInt()?.toString() ?: "—"
    val eloDiffStr = if (homeElo != null && awayElo != null) {
        val diff = homeElo.toInt() - awayElo.toInt()
        val sign = if (diff > 0) "+" else ""
        "$sign$diff"
    } else {
        "—"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusLarge),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimen.Elevation0)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimen.PaddingM),
            verticalArrangement = Arrangement.spacedBy(SpacingS)
        ) {
            // Header Row: Title & Engine Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.prediction_factors_title),
                    style = MaterialTheme.typography.s14.semiBold(),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(RadiusPill))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(horizontal = Dimen.PaddingS, vertical = Dimen.PaddingXXS)
                ) {
                    Text(
                        text = stringResource(R.string.prediction_algorithm_badge, predictionResult.algorithmName),
                        style = MaterialTheme.typography.s10.bold(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Pre-match Baseline Notice for LIVE matches (Small subtle badge, not large solid block)
            if (match.status == MatchStatus.IN_PROGRESS) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(RadiusSmall))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = Dimen.PaddingS, vertical = SpacingXS),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(Dimen.SizeXS),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(SpacingXS))
                    Text(
                        text = stringResource(R.string.prediction_prematch_baseline_notice),
                        style = MaterialTheme.typography.s11,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

            // Elo Rating Comparison Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(RadiusSmall))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                    .padding(Dimen.PaddingS),
                verticalArrangement = Arrangement.spacedBy(SpacingXXS)
            ) {
                Text(
                    text = "Elo Rating",
                    style = MaterialTheme.typography.s12.bold(),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${match.homeTeam.name}: $homeEloStr",
                        style = MaterialTheme.typography.s12.medium(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Diff: $eloDiffStr pts",
                        style = MaterialTheme.typography.s12.bold(),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${match.awayTeam.name}: $awayEloStr",
                        style = MaterialTheme.typography.s12.medium(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Analytical Weights Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FactorWeightChip(label = "Elo", weight = "30%")
                FactorWeightChip(label = "EU Odds", weight = "20%")
                FactorWeightChip(label = "Form", weight = "20%")
                FactorWeightChip(label = "H2H", weight = "15%")
                FactorWeightChip(label = "Goals", weight = "15%")
            }
        }
    }
}

@Composable
private fun FactorWeightChip(
    label: String,
    weight: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(RadiusSmall))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(horizontal = SpacingXS, vertical = SpacingXXS)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.s10.medium(),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = weight,
            style = MaterialTheme.typography.s11.bold(),
            color = MaterialTheme.colorScheme.primary
        )
    }
}
