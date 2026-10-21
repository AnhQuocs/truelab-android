package dev.anhquocs.truelab.feature.analytics.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.ui.components.TeamLogo
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusMedium
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.medium
import dev.anhquocs.truelab.core.ui.utils.s10
import dev.anhquocs.truelab.core.ui.utils.s11
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s13
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.s16
import dev.anhquocs.truelab.core.ui.utils.semiBold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsMatchPickerBottomSheet(
    matches: List<Match>,
    targetTeamName: String? = null,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedMatch: Match?,
    onSelectMatch: (Match) -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimen.PaddingM)
                .padding(bottom = Dimen.PaddingXL)
        ) {
            Text(
                text = stringResource(R.string.analytics_select_match),
                style = MaterialTheme.typography.s16.bold(),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = if (targetTeamName != null) Dimen.PaddingXXS else Dimen.PaddingS)
            )

            if (targetTeamName != null) {
                Text(
                    text = stringResource(R.string.analytics_matches_of_team, targetTeamName),
                    style = MaterialTheme.typography.s12.medium(),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = Dimen.PaddingS)
                )
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(R.string.analytics_search_match_hint),
                        style = MaterialTheme.typography.s14
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(RadiusMedium),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(Dimen.PaddingM))

            if (matches.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimen.PaddingXL),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.analytics_no_matches_found),
                        style = MaterialTheme.typography.s13,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(Dimen.PaddingXS),
                    contentPadding = PaddingValues(vertical = Dimen.PaddingXXS)
                ) {
                    items(
                        items = matches,
                        key = { it.id }
                    ) { match ->
                        val isSelected = match.id == selectedMatch?.id
                        MatchPickerItem(
                            match = match,
                            isSelected = isSelected,
                            onClick = {
                                onSelectMatch(match)
                                onDismissRequest()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchPickerItem(
    match: Match,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMedium))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimen.PaddingM, vertical = Dimen.PaddingS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // League and date line
            if (!match.leagueName.isNullOrBlank() || match.startTimeDate.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = Dimen.PaddingXXS),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = match.leagueName ?: "",
                        style = MaterialTheme.typography.s10.medium(),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = match.startTimeDate,
                        style = MaterialTheme.typography.s10,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Teams Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home Team
                TeamLogo(
                    logoUrl = match.homeTeam.logo,
                    teamName = match.homeTeam.name,
                    size = Dimen.SizeS
                )
                Spacer(modifier = Modifier.width(Dimen.PaddingXS))
                Text(
                    text = match.homeTeam.name,
                    style = MaterialTheme.typography.s12.semiBold(),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // VS / Score
                Text(
                    text = if (match.homeScore != null && match.awayScore != null) {
                        "${match.homeScore} - ${match.awayScore}"
                    } else {
                        "VS"
                    },
                    style = MaterialTheme.typography.s10.bold(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Dimen.PaddingS)
                )

                // Away Team
                Text(
                    text = match.awayTeam.name,
                    style = MaterialTheme.typography.s12.semiBold(),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
                Spacer(modifier = Modifier.width(Dimen.PaddingXS))
                TeamLogo(
                    logoUrl = match.awayTeam.logo,
                    teamName = match.awayTeam.name,
                    size = Dimen.SizeS
                )
            }
        }

        if (isSelected) {
            Spacer(modifier = Modifier.width(Dimen.PaddingS))
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Dimen.SizeS)
            )
        }
    }
}
