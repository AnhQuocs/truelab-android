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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
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
import dev.anhquocs.truelab.core.domain.prediction.model.PredictionResult
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusPill
import dev.anhquocs.truelab.core.ui.theme.SpacingS
import dev.anhquocs.truelab.core.ui.theme.SpacingXS
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.medium
import dev.anhquocs.truelab.core.ui.utils.s11
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s22
import dev.anhquocs.truelab.core.ui.utils.semiBold
import kotlin.math.roundToInt

/**
 * Hero Prediction Probability — flat, no card/border wrappers.
 * Three large probability numbers dominate via typography hierarchy.
 * Non-dominant values are dimmed (alpha) rather than hidden.
 */
@Composable
fun PredictionHeroProbabilityCard(
    predictionResult: PredictionResult,
    modifier: Modifier = Modifier
) {
    val homePct = (predictionResult.homeWinProb * 100).roundToInt()
    val drawPct = (predictionResult.drawProb * 100).roundToInt()
    val awayPct = (predictionResult.awayWinProb * 100).roundToInt()
    val confPct = (predictionResult.confidenceScore * 100).roundToInt()

    val isHomeMax = predictionResult.homeWinProb >= predictionResult.drawProb &&
            predictionResult.homeWinProb >= predictionResult.awayWinProb
    val isDrawMax = predictionResult.drawProb > predictionResult.homeWinProb &&
            predictionResult.drawProb >= predictionResult.awayWinProb
    val isAwayMax = predictionResult.awayWinProb > predictionResult.homeWinProb &&
            predictionResult.awayWinProb > predictionResult.drawProb

    val (outcomeLabel, outcomeColor) = when (predictionResult.predictedOutcome) {
        "HOME_WIN" -> stringResource(R.string.prediction_outcome_home) to MaterialTheme.colorScheme.primary
        "AWAY_WIN" -> stringResource(R.string.prediction_outcome_away) to MaterialTheme.colorScheme.secondary
        else -> stringResource(R.string.prediction_outcome_draw) to MaterialTheme.colorScheme.tertiary
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SpacingS)
    ) {
        // Three large probability numbers — no background, no border
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SpacingXS)
        ) {
            FlatProbColumn(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.prediction_home_win),
                percent = homePct,
                isHighest = isHomeMax,
                accentColor = MaterialTheme.colorScheme.primary
            )
            FlatProbColumn(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.prediction_draw),
                percent = drawPct,
                isHighest = isDrawMax,
                accentColor = MaterialTheme.colorScheme.tertiary
            )
            FlatProbColumn(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.prediction_away_win),
                percent = awayPct,
                isHighest = isAwayMax,
                accentColor = MaterialTheme.colorScheme.secondary
            )
        }

        // Thin segmented probability bar — 3dp, accent-only colors
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(RadiusPill))
        ) {
            Box(
                modifier = Modifier
                    .weight(predictionResult.homeWinProb.toFloat().coerceAtLeast(0.02f))
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Box(
                modifier = Modifier
                    .weight(predictionResult.drawProb.toFloat().coerceAtLeast(0.02f))
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f))
            )
            Box(
                modifier = Modifier
                    .weight(predictionResult.awayWinProb.toFloat().coerceAtLeast(0.02f))
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.secondary)
            )
        }

        // Confidence + predicted outcome — compact inline row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.prediction_confidence, confPct.toDouble()),
                    style = MaterialTheme.typography.s11.medium(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "→ $outcomeLabel",
                style = MaterialTheme.typography.s12.bold(),
                color = outcomeColor
            )
        }
    }
}

@Composable
private fun FlatProbColumn(
    modifier: Modifier = Modifier,
    label: String,
    percent: Int,
    isHighest: Boolean,
    accentColor: Color
) {
    Column(
        modifier = modifier.padding(vertical = Dimen.PaddingXS),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = "$percent%",
            style = MaterialTheme.typography.s22.bold(),
            color = if (isHighest) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            style = MaterialTheme.typography.s11.medium(),
            color = if (isHighest) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
