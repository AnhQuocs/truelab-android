package dev.anhquocs.truelab.feature.backtest.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.evaluation.model.EvaluationPhase
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusMedium
import dev.anhquocs.truelab.core.ui.theme.RadiusSmall
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.medium
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.s16
import dev.anhquocs.truelab.core.ui.utils.s18
import dev.anhquocs.truelab.core.ui.utils.semiBold
import kotlin.math.roundToInt

@Composable
fun BacktestProcessingCard(
    completedMatches: Int,
    totalMatches: Int,
    progressPercent: Float,
    currentMatchName: String,
    currentPhase: EvaluationPhase,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progressPercent.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "backtest_progress"
    )

    val progressPctInt = (animatedProgress * 100).roundToInt()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusLarge),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimen.Elevation2)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimen.PaddingL),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Processing Indicator & Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingS)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(RadiusMedium))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(Dimen.PaddingXSPlus),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(Dimen.SizeSM)
                        )
                    }

                    Text(
                        text = stringResource(R.string.backtest_screen_title),
                        style = MaterialTheme.typography.s16.bold(),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = stringResource(
                        R.string.backtest_analyzing_progress,
                        completedMatches,
                        totalMatches,
                        progressPctInt
                    ),
                    style = MaterialTheme.typography.s12.semiBold(),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(Dimen.PaddingL))

            // Animated Linear Progress Bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(RadiusSmall)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(Dimen.PaddingM))

            // Current Match Title
            Text(
                text = currentMatchName.ifBlank { "..." },
                style = MaterialTheme.typography.s18.bold(),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(Dimen.PaddingM))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Spacer(modifier = Modifier.height(Dimen.PaddingM))

            // Phase Indicators (Checklist)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimen.PaddingS)
            ) {
                PhaseRowItem(
                    label = stringResource(R.string.backtest_phase_odds),
                    isCurrent = currentPhase == EvaluationPhase.FETCHING_ODDS,
                    isDone = currentPhase.ordinal > EvaluationPhase.FETCHING_ODDS.ordinal
                )

                PhaseRowItem(
                    label = stringResource(R.string.backtest_phase_context),
                    isCurrent = currentPhase == EvaluationPhase.PREPARING_CONTEXT,
                    isDone = currentPhase.ordinal > EvaluationPhase.PREPARING_CONTEXT.ordinal
                )

                PhaseRowItem(
                    label = stringResource(R.string.backtest_phase_predict),
                    isCurrent = currentPhase == EvaluationPhase.PREDICTING,
                    isDone = currentPhase.ordinal > EvaluationPhase.PREDICTING.ordinal
                )

                PhaseRowItem(
                    label = stringResource(R.string.backtest_phase_eval),
                    isCurrent = currentPhase == EvaluationPhase.EVALUATING,
                    isDone = false
                )
            }

            Spacer(modifier = Modifier.height(Dimen.PaddingL))

            // Cancel Button
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(RadiusMedium),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(Dimen.SizeXS)
                )
                Spacer(modifier = Modifier.width(Dimen.PaddingXS))
                Text(
                    text = stringResource(R.string.backtest_cancel_btn),
                    style = MaterialTheme.typography.s14.semiBold()
                )
            }
        }
    }
}

@Composable
private fun PhaseRowItem(
    label: String,
    isCurrent: Boolean,
    isDone: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingS)
    ) {
        when {
            isDone -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.s12.medium(),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            isCurrent -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.s12.bold(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.s12.medium(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}
