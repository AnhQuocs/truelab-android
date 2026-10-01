package dev.anhquocs.truelab.feature.prediction.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusMedium
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
import dev.anhquocs.truelab.core.ui.utils.s16
import dev.anhquocs.truelab.core.ui.utils.s18
import dev.anhquocs.truelab.core.ui.utils.semiBold

/**
 * Modern sports analytics Match Card for the Prediction Screen.
 *
 * Visual features:
 * - Clear League hierarchy (top left) & Status badge (top right).
 * - Home vs Away teams with team logo avatars & score/VS.
 * - Subtle selected state with 2dp border, slight tint, and "Đã chọn" indicator.
 * - Interactive "Dự đoán trận này →" CTA button on selected card.
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
                else -> {
                    val kickoffZoned = try {
                        dev.anhquocs.truelab.core.ui.utils.DateTimeFormatterUtils.parseToVietnamZonedDateTime(match.startTimeDate)
                    } catch (_: Exception) {
                        null
                    }
                    if (kickoffZoned != null && (kickoffZoned.isBefore(now) || kickoffZoned.isEqual(now))) {
                        DisplayMatchStatus.STARTED
                    } else {
                        DisplayMatchStatus.UPCOMING
                    }
                }
            }
        }
    }
}

/**
 * Modern sports analytics Match Card for the Prediction Screen.
 *
 * Visual features:
 * - Clear League hierarchy (top left) & Status badge (top right).
 * - Home vs Away teams with team logo avatars & score/VS.
 * - Subtle selected state with 2dp border, slight tint, and "Đã chọn" indicator.
 * - Interactive "Dự đoán trận này →" CTA button on selected card.
 */
@Composable
fun PredictableMatchCard(
    match: Match,
    isSelected: Boolean,
    onClick: () -> Unit,
    onPredictClick: () -> Unit = onClick,
    modifier: Modifier = Modifier,
    showLeagueHeader: Boolean = false,
    leagueName: String? = null,
    leagueLogo: String? = null,
    selectedDate: String? = null
) {
    val effectiveLeagueName = match.leagueName ?: leagueName
    val effectiveLeagueLogo = match.leagueLogo ?: leagueLogo

    val displayStatus = resolveDisplayStatus(match, selectedDate)
    val isLive = displayStatus == DisplayMatchStatus.LIVE
    val isUpcoming = displayStatus == DisplayMatchStatus.UPCOMING
    val isStarted = displayStatus == DisplayMatchStatus.STARTED
    val isEnded = displayStatus == DisplayMatchStatus.ENDED

    val cardBorder = if (isSelected) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    }

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusLarge))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(RadiusLarge),
        border = cardBorder,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimen.PaddingM),
            verticalArrangement = Arrangement.spacedBy(Dimen.PaddingS)
        ) {
            // Status & Optional League Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (showLeagueHeader) Arrangement.SpaceBetween else Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showLeagueHeader) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        dev.anhquocs.truelab.core.ui.components.CompetitionLogo(
                            logoUrl = effectiveLeagueLogo,
                            leagueName = effectiveLeagueName,
                            size = 18.dp
                        )
                        Spacer(modifier = Modifier.width(SpacingXS))
                        Text(
                            text = effectiveLeagueName ?: (if (match.leagueId != null) "League #${match.leagueId}" else stringResource(R.string.prediction_default_competition)),
                            style = MaterialTheme.typography.s12.medium(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(Dimen.PaddingS))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    when {
                        isLive -> {
                            LiveMinuteBadge(minutes = match.minutes)
                        }
                        isUpcoming -> {
                            Surface(
                                shape = RoundedCornerShape(RadiusPill),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = formatKickoffTime(match.startTimeDate),
                                    style = MaterialTheme.typography.s11.bold(),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = Dimen.PaddingS, vertical = Dimen.PaddingXXS)
                                )
                            }
                        }
                        isStarted -> {
                            Surface(
                                shape = RoundedCornerShape(RadiusPill),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = stringResource(R.string.prediction_status_started),
                                    style = MaterialTheme.typography.s11.semiBold(),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = Dimen.PaddingS, vertical = Dimen.PaddingXXS)
                                )
                            }
                        }
                        isEnded -> {
                            Surface(
                                shape = RoundedCornerShape(RadiusPill),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ) {
                                Text(
                                    text = "FT",
                                    style = MaterialTheme.typography.s11.medium(),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = Dimen.PaddingS, vertical = Dimen.PaddingXXS)
                                )
                            }
                        }
                        else -> {
                            Text(
                                text = match.status.name,
                                style = MaterialTheme.typography.s11,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isSelected) {
                        Spacer(modifier = Modifier.width(SpacingXS))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = stringResource(R.string.prediction_selected_badge),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Teams and Score / Matchup Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SpacingXXS),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home Team (Left)
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = match.homeTeam.name,
                        style = MaterialTheme.typography.s14.semiBold(),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(SpacingXS))
                    dev.anhquocs.truelab.core.ui.components.TeamLogo(
                        logoUrl = match.homeTeam.logo,
                        teamName = match.homeTeam.name,
                        size = 32.dp
                    )
                }

                // Middle: Score or VS
                Box(
                    modifier = Modifier
                        .padding(horizontal = Dimen.PaddingM),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLive || isEnded || (isStarted && match.homeScore != null && match.awayScore != null)) {
                        Text(
                            text = "${match.homeScore ?: 0} : ${match.awayScore ?: 0}",
                            style = MaterialTheme.typography.s18.bold(),
                            color = if (isLive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Text(
                            text = "VS",
                            style = MaterialTheme.typography.s13.bold(),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Away Team (Right)
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    dev.anhquocs.truelab.core.ui.components.TeamLogo(
                        logoUrl = match.awayTeam.logo,
                        teamName = match.awayTeam.name,
                        size = 32.dp
                    )
                    Spacer(modifier = Modifier.width(SpacingXS))
                    Text(
                        text = match.awayTeam.name,
                        style = MaterialTheme.typography.s14.semiBold(),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Start,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
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
                        shape = RoundedCornerShape(RadiusPill),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
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
private fun formatKickoffTime(startTimeDate: String): String {
    return dev.anhquocs.truelab.core.ui.utils.DateTimeFormatterUtils.formatToVietnamTime(startTimeDate)
}
