package dev.anhquocs.truelab.feature.prediction.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusPill
import dev.anhquocs.truelab.core.ui.theme.SpacingXS
import dev.anhquocs.truelab.core.ui.theme.SpacingXXS
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.s11
import dev.anhquocs.truelab.core.ui.utils.s12

/**
 * Live minute badge displaying "🔴 LIVE 65'" or "🔴 LIVE" with an infinite blinking apostrophe (').
 *
 * Adheres to TrueScore-aligned behavior:
 * - Number/text is completely static.
 * - Only the trailing apostrophe (') animates alpha between 0.0f and 1.0f every 600ms.
 * - Fallbacks gracefully to "🔴 LIVE" when minutes is null or blank.
 */
@Composable
fun LiveMinuteBadge(
    minutes: String?,
    modifier: Modifier = Modifier
) {
    val cleanMinutes = minutes?.trim()?.takeIf { it.isNotEmpty() }
    val isMinutesAvailable = cleanMinutes != null

    // Blinking transition specifically for the apostrophe mark (')
    val infiniteTransition = rememberInfiniteTransition(label = "live_minute_blinking")
    val apostropheAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "apostrophe_alpha"
    )

    val liveRed = MaterialTheme.colorScheme.error
    val liveContainer = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(RadiusPill))
            .background(liveContainer)
            .padding(horizontal = Dimen.PaddingS, vertical = Dimen.PaddingXXS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Red Pulsing Indicator Dot
        Box(
            modifier = Modifier
                .size(Dimen.SizeDot)
                .clip(CircleShape)
                .background(liveRed)
        )
        Spacer(modifier = Modifier.width(SpacingXXS))

        Text(
            text = "LIVE",
            style = MaterialTheme.typography.s11.bold(),
            color = liveRed
        )

        if (isMinutesAvailable) {
            Spacer(modifier = Modifier.width(SpacingXS))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = cleanMinutes ?: "",
                    style = MaterialTheme.typography.s12.bold(),
                    color = liveRed
                )
                Text(
                    text = "'",
                    style = MaterialTheme.typography.s12.bold(),
                    color = liveRed,
                    modifier = Modifier.alpha(apostropheAlpha)
                )
            }
        }
    }
}
