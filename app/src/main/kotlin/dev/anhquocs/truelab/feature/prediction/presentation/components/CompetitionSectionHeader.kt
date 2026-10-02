package dev.anhquocs.truelab.feature.prediction.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.ui.components.CompetitionLogo
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.SpacingXS
import dev.anhquocs.truelab.core.ui.theme.SpacingXXS
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.medium
import dev.anhquocs.truelab.core.ui.utils.s11
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.semiBold

/**
 * UI presentation model representing a competition grouping of matches.
 */
data class CompetitionMatchGroup(
    val leagueId: Int?,
    val leagueName: String,
    val leagueLogo: String?,
    val matches: List<Match>
)

/**
 * Minimalist competition header:
 * [logo]  League Name
 *         Round / Season  •  1 Live
 *
 * Direct logo without background/container/frame, no pin/favorite icons.
 */
@Composable
fun CompetitionSectionHeader(
    group: CompetitionMatchGroup,
    modifier: Modifier = Modifier
) {
    val liveCount = group.matches.count { it.status == MatchStatus.IN_PROGRESS }
    val roundOrSeason = group.matches.firstNotNullOfOrNull { it.season }?.takeIf { it.isNotBlank() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = Dimen.PaddingSM, bottom = SpacingXS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompetitionLogo(
            logoUrl = group.leagueLogo,
            leagueName = group.leagueName,
            size = Dimen.SizeM
        )
        Spacer(modifier = Modifier.width(SpacingXS + SpacingXXS))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = group.leagueName,
                style = MaterialTheme.typography.s14.bold(),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val hasSubtitle = roundOrSeason != null || liveCount > 0
            if (hasSubtitle) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SpacingXXS)
                ) {
                    if (roundOrSeason != null) {
                        Text(
                            text = roundOrSeason,
                            style = MaterialTheme.typography.s11.medium(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (roundOrSeason != null && liveCount > 0) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.s11.medium(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (liveCount > 0) {
                        Text(
                            text = "$liveCount Live",
                            style = MaterialTheme.typography.s11.semiBold(),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
