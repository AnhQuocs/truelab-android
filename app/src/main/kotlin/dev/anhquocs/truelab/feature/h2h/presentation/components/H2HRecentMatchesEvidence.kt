package dev.anhquocs.truelab.feature.h2h.presentation.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.ui.components.TeamLogo
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusExtraSmall
import dev.anhquocs.truelab.core.ui.theme.RadiusSmall
import dev.anhquocs.truelab.core.ui.theme.SpacingXS
import dev.anhquocs.truelab.core.ui.theme.SpacingXXS
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.medium
import dev.anhquocs.truelab.core.ui.utils.normal
import dev.anhquocs.truelab.core.ui.utils.s10
import dev.anhquocs.truelab.core.ui.utils.semiBold
import dev.anhquocs.truelab.feature.h2h.presentation.model.TeamRecentMatchItem

/**
 * Hiển thị huy hiệu và danh sách bằng chứng 5 trận gần nhất phục vụ giải thích điểm Form.
 * Sắp xếp theo thứ tự thời gian tăng dần: CŨ NHẤT (trái/trên) -> MỚI NHẤT (phải/dưới).
 */
@Composable
fun RecentMatchesFormSection(
    teamARecentMatches: List<TeamRecentMatchItem>,
    teamBRecentMatches: List<TeamRecentMatchItem>,
    modifier: Modifier = Modifier
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = SpacingXS),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Team A Form Badges (Left area)
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (teamARecentMatches.isNotEmpty()) {
                    OutcomeBadgeRow(matches = teamARecentMatches)
                } else {
                    Text(
                        text = stringResource(R.string.h2h_no_recent_matches),
                        style = MaterialTheme.typography.s10.normal(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Center Label & Toggle Button (Center area without greedy weight)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(RadiusSmall))
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = Dimen.PaddingXSPlus, vertical = Dimen.PaddingXXS)
            ) {
                Text(
                    text = stringResource(R.string.h2h_recent_5_matches_title),
                    style = MaterialTheme.typography.s10.semiBold(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isExpanded) {
                            stringResource(R.string.h2h_hide_recent_matches)
                        } else {
                            stringResource(R.string.h2h_view_recent_matches)
                        },
                        style = MaterialTheme.typography.s10.medium(),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(Dimen.SizeXS),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Team B Form Badges (Right area)
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (teamBRecentMatches.isNotEmpty()) {
                    OutcomeBadgeRow(matches = teamBRecentMatches)
                } else {
                    Text(
                        text = stringResource(R.string.h2h_no_recent_matches),
                        style = MaterialTheme.typography.s10.normal(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Expanded match-by-match evidence cards (oldest at top -> newest at bottom)
        if (isExpanded) {
            Spacer(modifier = Modifier.height(Dimen.PaddingS))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingS)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(SpacingXS)
                ) {
                    teamARecentMatches.forEach { matchItem ->
                        RecentMatchEvidenceItem(matchItem)
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(SpacingXS)
                ) {
                    teamBRecentMatches.forEach { matchItem ->
                        RecentMatchEvidenceItem(matchItem)
                    }
                }
            }
            Spacer(modifier = Modifier.height(Dimen.PaddingSM))
        }
    }
}

@Composable
fun OutcomeBadgeRow(
    matches: List<TeamRecentMatchItem>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(SpacingXXS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        matches.forEach { match ->
            OutcomeBadge(result = match.result)
        }
    }
}

@Composable
fun OutcomeBadge(
    result: String,
    modifier: Modifier = Modifier
) {
    val bgColor = when (result.uppercase()) {
        "W" -> Color(0xFF2E7D32)
        "D" -> Color(0xFFF57C00)
        "L" -> Color(0xFFD32F2F)
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Box(
        modifier = modifier
            .size(Dimen.SizeS2)
            .clip(RoundedCornerShape(RadiusExtraSmall))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = result,
            style = MaterialTheme.typography.s10.bold(),
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RecentMatchEvidenceItem(
    matchItem: TeamRecentMatchItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusSmall),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimen.PaddingXSPlus, vertical = SpacingXS)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    OutcomeBadge(result = matchItem.result)
                    Spacer(modifier = Modifier.width(SpacingXS))
                    TeamLogo(
                        logoUrl = matchItem.opponentLogo,
                        teamName = matchItem.opponentName,
                        size = Dimen.SizeS
                    )
                    Spacer(modifier = Modifier.width(SpacingXS))
                    Text(
                        text = "${if (matchItem.isHome) "vs" else "@"} ${matchItem.opponentName}",
                        style = MaterialTheme.typography.s10.medium(),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = matchItem.score,
                    style = MaterialTheme.typography.s10.bold(),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = matchItem.date,
                style = MaterialTheme.typography.s10.normal(),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
        }
    }
}
