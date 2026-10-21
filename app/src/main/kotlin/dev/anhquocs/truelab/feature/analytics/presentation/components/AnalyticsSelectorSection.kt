package dev.anhquocs.truelab.feature.analytics.presentation.components

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.team.model.TeamDetail
import dev.anhquocs.truelab.core.ui.components.TeamLogo
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusPill
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.medium
import dev.anhquocs.truelab.core.ui.utils.s10
import dev.anhquocs.truelab.core.ui.utils.s11
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s13
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.semiBold

@Composable
fun AnalyticsSelectorSection(
    selectedTeam: TeamDetail?,
    selectedMatch: Match?,
    onOpenTeamPicker: () -> Unit,
    onOpenMatchPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusLarge),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimen.Elevation1)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(Dimen.PaddingM)) {
            // Team Selector Tile
            TeamSelectorTile(
                selectedTeam = selectedTeam,
                onClick = onOpenTeamPicker
            )

            Spacer(modifier = Modifier.height(Dimen.PaddingS))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            Spacer(modifier = Modifier.height(Dimen.PaddingS))

            // Match Selector Tile
            MatchSelectorTile(
                selectedMatch = selectedMatch,
                onClick = onOpenMatchPicker
            )
        }
    }
}

@Composable
private fun TeamSelectorTile(
    selectedTeam: TeamDetail?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimen.PaddingS))
            .clickable(onClick = onClick)
            .padding(vertical = Dimen.PaddingXS, horizontal = Dimen.PaddingXXS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TeamLogo(
            logoUrl = selectedTeam?.logo,
            teamName = selectedTeam?.name ?: "",
            size = 36.dp
        )

        Spacer(modifier = Modifier.width(Dimen.PaddingM))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.analytics_target_team_title),
                style = MaterialTheme.typography.s10.bold(),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = selectedTeam?.name ?: stringResource(R.string.analytics_select_team),
                style = MaterialTheme.typography.s14.semiBold(),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val leagueName = selectedTeam?.leagueName
            if (!leagueName.isNullOrBlank()) {
                Text(
                    text = leagueName,
                    style = MaterialTheme.typography.s11,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(Dimen.PaddingS))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(RadiusPill))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                .padding(horizontal = Dimen.PaddingS, vertical = Dimen.PaddingXXS)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.analytics_change_selection),
                    style = MaterialTheme.typography.s10.bold(),
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun MatchSelectorTile(
    selectedMatch: Match?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimen.PaddingS))
            .clickable(onClick = onClick)
            .padding(vertical = Dimen.PaddingXS, horizontal = Dimen.PaddingXXS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectedMatch != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingXXS)
            ) {
                TeamLogo(
                    logoUrl = selectedMatch.homeTeam.logo,
                    teamName = selectedMatch.homeTeam.name,
                    size = 28.dp
                )
                TeamLogo(
                    logoUrl = selectedMatch.awayTeam.logo,
                    teamName = selectedMatch.awayTeam.name,
                    size = 28.dp
                )
            }
        }

        Spacer(modifier = Modifier.width(Dimen.PaddingM))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.analytics_target_match_title),
                style = MaterialTheme.typography.s10.bold(),
                color = MaterialTheme.colorScheme.secondary
            )
            val matchTitle = if (selectedMatch != null) {
                "${selectedMatch.homeTeam.name} vs ${selectedMatch.awayTeam.name}"
            } else {
                stringResource(R.string.analytics_select_match)
            }
            Text(
                text = matchTitle,
                style = MaterialTheme.typography.s13.semiBold(),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (selectedMatch?.leagueName != null || selectedMatch?.startTimeDate?.isNotBlank() == true) {
                val sub = listOfNotNull(
                    selectedMatch.leagueName?.takeIf { it.isNotBlank() },
                    selectedMatch.startTimeDate.takeIf { it.isNotBlank() }
                ).joinToString(" • ")
                Text(
                    text = sub,
                    style = MaterialTheme.typography.s11,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(Dimen.PaddingS))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(RadiusPill))
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                .padding(horizontal = Dimen.PaddingS, vertical = Dimen.PaddingXXS)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.analytics_change_selection),
                    style = MaterialTheme.typography.s10.bold(),
                    color = MaterialTheme.colorScheme.secondary
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
