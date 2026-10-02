package dev.anhquocs.truelab.feature.prediction.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.ui.components.CompetitionLogo
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusPill
import dev.anhquocs.truelab.core.ui.theme.SpacingXS
import dev.anhquocs.truelab.core.ui.theme.SpacingXXS
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.medium
import dev.anhquocs.truelab.core.ui.utils.s11
import dev.anhquocs.truelab.core.ui.utils.s13

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
 * Modern dark-themed section header for competition groups.
 */
@Composable
fun CompetitionSectionHeader(
    group: CompetitionMatchGroup,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = Dimen.PaddingS, bottom = SpacingXXS),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompetitionLogo(
                logoUrl = group.leagueLogo,
                leagueName = group.leagueName,
                size = Dimen.SizeSM
            )
            Spacer(modifier = Modifier.width(SpacingXS))
            Text(
                text = group.leagueName,
                style = MaterialTheme.typography.s13.bold(),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(Dimen.PaddingS))

        Surface(
            shape = RoundedCornerShape(RadiusPill),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Text(
                text = "${group.matches.size}",
                style = MaterialTheme.typography.s11.medium(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Dimen.PaddingS, vertical = Dimen.PaddingXXS)
            )
        }
    }
}
