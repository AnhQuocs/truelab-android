package dev.anhquocs.truelab.feature.prediction.presentation.components

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.prediction.model.PredictionResult
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusPill
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.s10
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.semiBold

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
        "$sign$diff pts"
    } else {
        "—"
    }

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
                .padding(Dimen.PaddingM),
            verticalArrangement = Arrangement.spacedBy(Dimen.PaddingS)
        ) {
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
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = Dimen.PaddingS, vertical = Dimen.PaddingXXS)
                ) {
                    Text(
                        text = stringResource(R.string.prediction_algorithm_badge, predictionResult.algorithmName),
                        style = MaterialTheme.typography.s10.bold(),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimen.PaddingXXS))

            Text(
                text = stringResource(
                    R.string.prediction_elo_diff,
                    match.homeTeam.name,
                    homeEloStr,
                    match.awayTeam.name,
                    awayEloStr,
                    eloDiffStr
                ),
                style = MaterialTheme.typography.s12,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
