package dev.anhquocs.truelab.feature.prediction.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.ui.components.TeamLogo
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusMedium
import dev.anhquocs.truelab.core.ui.theme.RadiusPill
import dev.anhquocs.truelab.core.ui.theme.SpacingS
import dev.anhquocs.truelab.core.ui.theme.SpacingXS
import dev.anhquocs.truelab.core.ui.theme.SpacingXXS
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.medium
import dev.anhquocs.truelab.core.ui.utils.s11
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s13
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.semiBold

/**
 * Display status modes for Match Cards in Prediction Screen.
 */
enum class DisplayMatchStatus {
    LIVE,
    STARTED,
    UPCOMING,
    ENDED
}

fun resolveDisplayStatus(
    match: Match,
    selectedDate: String?,
    now: java.time.ZonedDateTime = java.time.ZonedDateTime.now(dev.anhquocs.truelab.core.ui.utils.DateTimeFormatterUtils.VIETNAM_ZONE_ID)
): DisplayMatchStatus {
    val todayVietnam = now.toLocalDate()

    val selectedDateObj = if (!selectedDate.isNullOrBlank()) {
        try {
            java.time.LocalDate.parse(selectedDate)
        } catch (_: Exception) {
            todayVietnam
        }
    } else {
        todayVietnam
    }

    return when {
        selectedDateObj.isBefore(todayVietnam) -> DisplayMatchStatus.ENDED
        selectedDateObj.isAfter(todayVietnam) -> DisplayMatchStatus.UPCOMING
        else -> {
            when (match.status) {
                MatchStatus.IN_PROGRESS -> DisplayMatchStatus.LIVE
                MatchStatus.ENDED -> DisplayMatchStatus.ENDED
                MatchStatus.CANCELLED -> DisplayMatchStatus.ENDED
                MatchStatus.SCHEDULED, MatchStatus.UNKNOWN -> DisplayMatchStatus.UPCOMING
            }
        }
    }
}

/**
 * Minimalist Scoreboard Match Card for Prediction Screen.
 *
 * 3-Column Scoreboard Layout:
 * [Live / Kickoff / FT]  [Logo] Home Team    [Score / VS]
 *                        [Logo] Away Team    [Score / VS]
 */
@Composable
fun PredictableMatchCard(
    match: Match,
    isSelected: Boolean,
    onClick: () -> Unit,
    onPredictClick: () -> Unit = onClick,
    modifier: Modifier = Modifier,
    selectedDate: String? = null
) {
    val displayStatus = resolveDisplayStatus(match, selectedDate)
    val isLive = displayStatus == DisplayMatchStatus.LIVE
    val isUpcoming = displayStatus == DisplayMatchStatus.UPCOMING
    val isStarted = displayStatus == DisplayMatchStatus.STARTED
    val isEnded = displayStatus == DisplayMatchStatus.ENDED

    val cardBorder = if (isSelected) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    }

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMedium))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(RadiusMedium),
        border = cardBorder,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimen.Elevation0)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimen.PaddingM, vertical = Dimen.PaddingSM),
            verticalArrangement = Arrangement.spacedBy(SpacingXS)
        ) {
            // Main Scoreboard Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Match Status / Kickoff Time
                Box(
                    modifier = Modifier.width(Dimen.Width.StatusColumn),
                    contentAlignment = Alignment.CenterStart
                ) {
                    when {
                        isLive -> {
                            LiveMinuteText(minutes = match.minutes)
                        }

                        isUpcoming -> {
                            Text(
                                text = formatKickoffTime(match.startTimeDate),
                                style = MaterialTheme.typography.s12.semiBold(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        isEnded -> {
                            Text(
                                text = "FT",
                                style = MaterialTheme.typography.s13.bold(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        else -> {
                            Text(
                                text = match.status.name,
                                style = MaterialTheme.typography.s11,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(SpacingXS))

                // Middle Column: Team rows (Logo + Name)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(SpacingS)
                ) {
                    // Home Team Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TeamLogo(
                            logoUrl = match.homeTeam.logo,
                            teamName = match.homeTeam.name,
                            size = Dimen.SizeMD
                        )
                        Spacer(modifier = Modifier.width(SpacingS))
                        Text(
                            text = match.homeTeam.name,
                            style = MaterialTheme.typography.s14.semiBold(),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Away Team Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TeamLogo(
                            logoUrl = match.awayTeam.logo,
                            teamName = match.awayTeam.name,
                            size = Dimen.SizeMD
                        )
                        Spacer(modifier = Modifier.width(SpacingS))
                        Text(
                            text = match.awayTeam.name,
                            style = MaterialTheme.typography.s14.semiBold(),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(Dimen.PaddingS))

                // Right Column: Fixed Score / VS Column
                val hasScore = isLive || isEnded || (isStarted && match.homeScore != null && match.awayScore != null)
                if (hasScore) {
                    Column(
                        modifier = Modifier.width(Dimen.Width.ScoreColumn),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(SpacingS)
                    ) {
                        Box(
                            modifier = Modifier.height(Dimen.SizeMD),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = "${match.homeScore ?: 0}",
                                style = MaterialTheme.typography.s14.bold(),
                                color = if (isLive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.End
                            )
                        }
                        Box(
                            modifier = Modifier.height(Dimen.SizeMD),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = "${match.awayScore ?: 0}",
                                style = MaterialTheme.typography.s14.bold(),
                                color = if (isLive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .width(Dimen.Width.ScoreColumn)
                            .height(Dimen.SizeMD * 2 + SpacingS),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = "VS",
                            style = MaterialTheme.typography.s12.bold(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            // Action / CTA Row for Selected Match
            if (isSelected) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    modifier = Modifier.padding(top = SpacingXXS)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onPredictClick,
                        shape = RoundedCornerShape(RadiusMedium),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.height(Dimen.Height.ActionPill)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = null,
                            modifier = Modifier.size(Dimen.SizeXS)
                        )
                        Spacer(modifier = Modifier.width(SpacingXXS))
                        Text(
                            text = stringResource(R.string.prediction_cta_predict),
                            style = MaterialTheme.typography.s12.bold()
                        )
                    }
                }
            }
        }
    }
}

/**
 * Formats startTimeDate string into Vietnam timezone ("HH:mm").
 */
private fun formatKickoffTime(startTimeDate: String?): String {
    if (startTimeDate.isNullOrBlank()) return ""
    return dev.anhquocs.truelab.core.ui.utils.DateTimeFormatterUtils.formatToVietnamTime(startTimeDate)
}

/**
 * Live minute text with animated blinking apostrophe (').
 */
@Composable
private fun LiveMinuteText(
    minutes: String?,
    modifier: Modifier = Modifier
) {
    val cleanMinutes = minutes?.trim()?.removeSuffix("'")?.takeIf { it.isNotEmpty() }
    val isMinutesAvailable = cleanMinutes != null

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

    if (isMinutesAvailable) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = cleanMinutes ?: "",
                style = MaterialTheme.typography.s13.bold(),
                color = liveRed
            )
            Text(
                text = "'",
                style = MaterialTheme.typography.s13.bold(),
                color = liveRed,
                modifier = Modifier.alpha(apostropheAlpha)
            )
        }
    } else {
        Text(
            text = "LIVE",
            style = MaterialTheme.typography.s13.bold(),
            color = liveRed,
            modifier = modifier
        )
    }
}
