package dev.anhquocs.truelab.feature.match.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.league.model.League
import dev.anhquocs.truelab.core.domain.match.model.MatchSortCriteria
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusMedium
import dev.anhquocs.truelab.core.ui.theme.SpacingXS
import dev.anhquocs.truelab.core.ui.utils.medium
import dev.anhquocs.truelab.core.ui.utils.s10
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.semiBold
import dev.anhquocs.truelab.feature.match.presentation.model.MatchStatusFilter

@Composable
fun DatasetSearchField(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = {
            Text(
                text = stringResource(R.string.matches_search_hint),
                style = MaterialTheme.typography.s14,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(RadiusLarge),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun LeagueFilterChips(
    leagues: List<League>,
    selectedLeagueId: Int?,
    onSelectLeague: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingS),
        contentPadding = PaddingValues(vertical = Dimen.PaddingXXS)
    ) {
        item {
            FilterChip(
                selected = selectedLeagueId == null,
                onClick = { onSelectLeague(null) },
                shape = RoundedCornerShape(RadiusMedium),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                label = {
                    Text(
                        text = stringResource(R.string.matches_filter_all_leagues),
                        style = MaterialTheme.typography.s12.semiBold()
                    )
                }
            )
        }
        items(leagues, key = { it.id }) { league ->
            FilterChip(
                selected = selectedLeagueId == league.id,
                onClick = { onSelectLeague(league.id) },
                shape = RoundedCornerShape(RadiusMedium),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                label = {
                    Text(
                        text = league.name,
                        style = MaterialTheme.typography.s12.semiBold()
                    )
                }
            )
        }
    }
}

@Composable
fun DatasetFilterChips(
    filterOptions: List<Pair<MatchStatusFilter, String>>,
    selectedFilter: MatchStatusFilter,
    onSelectFilter: (MatchStatusFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingS),
        contentPadding = PaddingValues(vertical = Dimen.PaddingXXS)
    ) {
        items(filterOptions) { (filter, label) ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onSelectFilter(filter) },
                shape = RoundedCornerShape(RadiusMedium),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.s12.semiBold()
                    )
                }
            )
        }
    }
}

@Composable
fun DatasetSortSection(
    sortOptions: List<Pair<MatchSortCriteria, String>>,
    selectedSort: MatchSortCriteria,
    onSelectSort: (MatchSortCriteria) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingS)
    ) {
        Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = "Sort",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Dimen.SizeS)
        )
        Text(
            text = stringResource(R.string.matches_sort_label),
            style = MaterialTheme.typography.s12.semiBold(),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingXS)
        ) {
            items(sortOptions) { (criteria, label) ->
                FilterChip(
                    selected = selectedSort == criteria,
                    onClick = { onSelectSort(criteria) },
                    shape = RoundedCornerShape(RadiusMedium),
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.s10.medium()
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun MatchesFeedbackCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    message: String,
    containerColor: Color,
    titleColor: Color,
    messageColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimen.PaddingL),
        shape = RoundedCornerShape(RadiusLarge),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimen.PaddingXL),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(Dimen.SizeXLPlus)
            )
            Spacer(modifier = Modifier.height(Dimen.PaddingS))
            Text(
                text = title,
                style = MaterialTheme.typography.s14.semiBold(),
                color = titleColor
            )
            Spacer(modifier = Modifier.height(SpacingXS))
            Text(
                text = message,
                style = MaterialTheme.typography.s12,
                color = messageColor,
                textAlign = TextAlign.Center
            )
        }
    }
}
