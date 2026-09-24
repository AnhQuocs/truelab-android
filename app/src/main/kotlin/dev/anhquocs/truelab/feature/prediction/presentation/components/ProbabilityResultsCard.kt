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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
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
import dev.anhquocs.truelab.core.domain.prediction.model.PredictionResult
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusPill
import dev.anhquocs.truelab.core.ui.theme.SpacingS
import dev.anhquocs.truelab.core.ui.theme.SpacingXS
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.medium
import dev.anhquocs.truelab.core.ui.utils.s10
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s13
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.semiBold

@Composable
fun ProbabilityResultsCard(
    predictionResult: PredictionResult,
    homeWinPercent: Int,
    drawPercent: Int,
    awayWinPercent: Int,
    modifier: Modifier = Modifier
) {
    val outcomeLabel = when (predictionResult.predictedOutcome) {
        "HOME_WIN" -> stringResource(R.string.prediction_outcome_home)
        "AWAY_WIN" -> stringResource(R.string.prediction_outcome_away)
        else -> stringResource(R.string.prediction_outcome_draw)
    }

    val outcomeColor = when (predictionResult.predictedOutcome) {
        "HOME_WIN" -> MaterialTheme.colorScheme.primary
        "AWAY_WIN" -> MaterialTheme.colorScheme.secondary
        else -> Color(0xFFF59E0B)
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
            verticalArrangement = Arrangement.spacedBy(Dimen.PaddingM)
        ) {
            // Header with Predicted Outcome Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.prediction_probabilities_title),
                        style = MaterialTheme.typography.s14.semiBold(),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.prediction_confidence, predictionResult.confidenceScore * 100),
                        style = MaterialTheme.typography.s12,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(RadiusPill))
                        .background(outcomeColor.copy(alpha = 0.15f))
                        .padding(horizontal = Dimen.PaddingS, vertical = Dimen.PaddingXXS)
                ) {
                    Text(
                        text = outcomeLabel,
                        style = MaterialTheme.typography.s10.bold(),
                        color = outcomeColor
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // 3 Probability Bars
            ProbabilityBarItem(
                label = stringResource(R.string.prediction_home_win),
                percentage = homeWinPercent,
                color = MaterialTheme.colorScheme.primary
            )

            ProbabilityBarItem(
                label = stringResource(R.string.prediction_draw),
                percentage = drawPercent,
                color = Color(0xFFF59E0B)
            )

            ProbabilityBarItem(
                label = stringResource(R.string.prediction_away_win),
                percentage = awayWinPercent,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun ProbabilityBarItem(
    label: String,
    percentage: Int,
    color: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.s13.medium(),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.s13.bold(),
                color = color
            )
        }
        Spacer(modifier = Modifier.height(SpacingXS))
        LinearProgressIndicator(
            progress = { percentage / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(SpacingS)
                .clip(RoundedCornerShape(RadiusPill)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            drawStopIndicator = {}
        )
    }
}
