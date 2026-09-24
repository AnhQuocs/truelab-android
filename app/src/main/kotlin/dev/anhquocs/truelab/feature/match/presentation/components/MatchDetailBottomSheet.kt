package dev.anhquocs.truelab.feature.match.presentation.components

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
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.ui.theme.ButtonHeightMedium
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusMedium
import dev.anhquocs.truelab.core.ui.theme.SpacingL
import dev.anhquocs.truelab.core.ui.theme.SpacingM
import dev.anhquocs.truelab.core.ui.theme.SpacingS
import dev.anhquocs.truelab.core.ui.theme.SpacingXS
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.medium
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.s16
import dev.anhquocs.truelab.core.ui.utils.s20
import dev.anhquocs.truelab.core.ui.utils.s24
import dev.anhquocs.truelab.core.ui.utils.semiBold
import dev.anhquocs.truelab.feature.match.presentation.model.MatchDetailUiState

/**
 * Bottom sheet displaying detailed information for a selected match.
 *
 * Provides entry point CTA to trigger match outcome prediction.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailBottomSheet(
    state: MatchDetailUiState,
    onDismiss: () -> Unit,
    onPredictClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state is MatchDetailUiState.Idle) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = RadiusLarge, topEnd = RadiusLarge),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpacingM)
                .padding(bottom = SpacingL)
        ) {
            // Header: Title and Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.match_detail_title),
                    style = MaterialTheme.typography.s16.bold(),
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.common_close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(SpacingS))

            when (state) {
                is MatchDetailUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                is MatchDetailUiState.Empty -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = SpacingL),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(SpacingS))
                        Text(
                            text = state.message.asString(),
                            style = MaterialTheme.typography.s14.medium(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                is MatchDetailUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = SpacingL),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(SpacingS))
                        Text(
                            text = state.message.asString(),
                            style = MaterialTheme.typography.s14.medium(),
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                is MatchDetailUiState.Success -> {
                    MatchDetailContent(
                        match = state.match,
                        onPredictClick = { onPredictClick(state.match.id) }
                    )
                }

                is MatchDetailUiState.Idle -> Unit
            }
        }
    }
}

@Composable
private fun MatchDetailContent(
    match: Match,
    onPredictClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SpacingM)
    ) {
        // Teams & Score Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(RadiusLarge),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(SpacingM),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Home Team
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsSoccer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(SpacingXS))
                    Text(
                        text = match.homeTeam.name,
                        style = MaterialTheme.typography.s14.semiBold(),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }

                // Score or VS
                Column(
                    modifier = Modifier.padding(horizontal = SpacingS),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (match.isEnded) {
                        Text(
                            text = "${match.homeScore ?: 0} - ${match.awayScore ?: 0}",
                            style = MaterialTheme.typography.s24.bold(),
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = "VS",
                            style = MaterialTheme.typography.s20.bold(),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = if (match.isEnded) {
                            stringResource(R.string.matches_filter_ended)
                        } else {
                            stringResource(R.string.matches_filter_scheduled)
                        },
                        style = MaterialTheme.typography.s12.medium(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Away Team
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsSoccer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(SpacingXS))
                    Text(
                        text = match.awayTeam.name,
                        style = MaterialTheme.typography.s14.semiBold(),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
        }

        // Match Info Summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(RadiusMedium),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(SpacingM),
                verticalArrangement = Arrangement.spacedBy(SpacingS)
            ) {
                DetailInfoRow(
                    label = stringResource(R.string.match_detail_date_label),
                    value = match.startTimeDate
                )
                DetailInfoRow(
                    label = stringResource(R.string.match_detail_status_label),
                    value = when (match.status) {
                        MatchStatus.ENDED -> stringResource(R.string.matches_filter_ended)
                        MatchStatus.SCHEDULED -> stringResource(R.string.matches_filter_scheduled)
                        MatchStatus.IN_PROGRESS -> "In Progress"
                        MatchStatus.CANCELLED -> "Cancelled"
                        MatchStatus.UNKNOWN -> "Unknown"
                    }
                )
                if (match.isEnded) {
                    DetailInfoRow(
                        label = stringResource(R.string.match_detail_total_goals),
                        value = "${match.totalGoals}"
                    )
                    val resultText = when {
                        match.isHomeWin -> stringResource(R.string.match_detail_home_win)
                        match.isAwayWin -> stringResource(R.string.match_detail_away_win)
                        match.isDraw -> stringResource(R.string.match_detail_draw)
                        else -> "—"
                    }
                    DetailInfoRow(
                        label = stringResource(R.string.match_detail_result_label),
                        value = resultText
                    )
                }
            }
        }

        // CTA: Predict Match
        Button(
            onClick = onPredictClick,
            shape = RoundedCornerShape(RadiusLarge),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(ButtonHeightMedium)
        ) {
            Icon(
                imageVector = Icons.Default.AutoGraph,
                contentDescription = null,
                modifier = Modifier.size(Dimen.SizeS)
            )
            Spacer(modifier = Modifier.width(SpacingS))
            Text(
                text = stringResource(R.string.match_detail_cta_predict),
                style = MaterialTheme.typography.s14.bold()
            )
        }
    }
}

@Composable
private fun DetailInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.s12,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.s12.semiBold(),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
