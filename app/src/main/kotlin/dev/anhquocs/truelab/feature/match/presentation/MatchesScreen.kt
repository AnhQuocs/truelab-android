package dev.anhquocs.truelab.feature.match.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.match.model.MatchSortCriteria
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusMedium
import dev.anhquocs.truelab.core.ui.theme.SpacingS
import dev.anhquocs.truelab.core.ui.theme.SpacingXS
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.medium
import dev.anhquocs.truelab.core.ui.utils.s10
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.s20
import dev.anhquocs.truelab.core.ui.utils.semiBold
import dev.anhquocs.truelab.feature.match.presentation.components.MatchDataCard
import dev.anhquocs.truelab.feature.match.presentation.components.MatchDetailBottomSheet
import dev.anhquocs.truelab.feature.match.presentation.model.MatchDetailUiState
import dev.anhquocs.truelab.feature.match.presentation.model.MatchStatusFilter
import dev.anhquocs.truelab.feature.match.presentation.model.MatchesUiState
import dev.anhquocs.truelab.feature.match.presentation.viewmodel.MatchesViewModel
import dev.anhquocs.truelab.navigation.TrueLabMainLayout

private val TOP_BAR_HEIGHT = 75.dp

@Composable
fun MatchesScreen(
    modifier: Modifier = Modifier,
    viewModel: MatchesViewModel = hiltViewModel(),
    onMatchClick: (String) -> Unit = {},
    onNavigateToPrediction: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val matchDetailState by viewModel.matchDetailState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currentSort by viewModel.sortCriteria.collectAsStateWithLifecycle()
    val currentFilter by viewModel.statusFilter.collectAsStateWithLifecycle()

    val filterOptions = listOf(
        MatchStatusFilter.ALL to stringResource(R.string.matches_filter_all),
        MatchStatusFilter.ENDED to stringResource(R.string.matches_filter_ended),
        MatchStatusFilter.SCHEDULED to stringResource(R.string.matches_filter_scheduled)
    )

    val sortOptions = listOf(
        MatchSortCriteria.START_TIME_DESC to stringResource(R.string.matches_sort_date),
        MatchSortCriteria.TOTAL_GOALS_DESC to stringResource(R.string.matches_sort_goals),
        MatchSortCriteria.ID_ASC to stringResource(R.string.matches_sort_id)
    )

    TrueLabMainLayout(
        modifier = modifier,
        headerHeight = TOP_BAR_HEIGHT,
        header = { MatchesHeader() }
    ) { contentModifier ->
        LazyColumn(
            modifier = contentModifier
                .fillMaxSize()
                .padding(horizontal = Dimen.PaddingM),
            verticalArrangement = Arrangement.spacedBy(Dimen.PaddingM),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Search Input Field
            item {
                Spacer(modifier = Modifier.height(Dimen.PaddingS))
                DatasetSearchField(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.onSearchQueryChanged(it) }
                )
            }

            // Status Filter Chips
            item {
                DatasetFilterChips(
                    filterOptions = filterOptions,
                    selectedFilter = currentFilter,
                    onSelectFilter = { viewModel.onStatusFilterChanged(it) }
                )
            }

            // Sort Section Chips
            item {
                DatasetSortSection(
                    sortOptions = sortOptions,
                    selectedSort = currentSort,
                    onSelectSort = { viewModel.onSortCriteriaChanged(it) }
                )
            }

            // Content based on UiState
            when (val state = uiState) {
                is MatchesUiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                is MatchesUiState.Empty -> {
                    item {
                        MatchesFeedbackCard(
                            icon = Icons.Default.Info,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = stringResource(R.string.matches_empty_title),
                            message = state.message.asString(),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            titleColor = MaterialTheme.colorScheme.onSurface,
                            messageColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is MatchesUiState.Error -> {
                    item {
                        MatchesFeedbackCard(
                            icon = Icons.Default.ErrorOutline,
                            iconTint = MaterialTheme.colorScheme.error,
                            title = stringResource(R.string.matches_error_title),
                            message = state.message.asString(),
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                            titleColor = MaterialTheme.colorScheme.error,
                            messageColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                is MatchesUiState.Success -> {
                    items(state.matches, key = { it.id }) { record ->
                        MatchDataCard(
                            record = record,
                            onClick = {
                                onMatchClick(record.id)
                                viewModel.onMatchClicked(record.id.toLongOrNull() ?: 0L)
                            }
                        )
                    }
                }
            }
        }
    }

    if (matchDetailState !is MatchDetailUiState.Idle) {
        MatchDetailBottomSheet(
            state = matchDetailState,
            onDismiss = { viewModel.onDismissMatchDetail() },
            onPredictClick = { matchId ->
                viewModel.onDismissMatchDetail()
                onNavigateToPrediction(matchId)
            }
        )
    }
}

@Composable
private fun MatchesHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimen.PaddingM),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.matches_title),
            style = MaterialTheme.typography.s20.bold(),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(SpacingXS))
        Text(
            text = stringResource(R.string.matches_subtitle),
            style = MaterialTheme.typography.s14,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(SpacingS))
    }
}

@Composable
private fun DatasetSearchField(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
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
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun DatasetFilterChips(
    filterOptions: List<Pair<MatchStatusFilter, String>>,
    selectedFilter: MatchStatusFilter,
    onSelectFilter: (MatchStatusFilter) -> Unit
) {
    LazyRow(
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
private fun DatasetSortSection(
    sortOptions: List<Pair<MatchSortCriteria, String>>,
    selectedSort: MatchSortCriteria,
    onSelectSort: (MatchSortCriteria) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
private fun MatchesFeedbackCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    message: String,
    containerColor: Color,
    titleColor: Color,
    messageColor: Color
) {
    Card(
        modifier = Modifier
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
                modifier = Modifier.size(40.dp)
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
