package dev.anhquocs.truelab.feature.match.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.match.model.MatchSortCriteria
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.SpacingS
import dev.anhquocs.truelab.core.ui.theme.SpacingXS
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.s20
import dev.anhquocs.truelab.feature.match.presentation.components.DatasetFilterChips
import dev.anhquocs.truelab.feature.match.presentation.components.DatasetSearchField
import dev.anhquocs.truelab.feature.match.presentation.components.DatasetSortSection
import dev.anhquocs.truelab.feature.match.presentation.components.LeagueFilterChips
import dev.anhquocs.truelab.feature.match.presentation.components.MatchDataCard
import dev.anhquocs.truelab.feature.match.presentation.components.MatchDetailBottomSheet
import dev.anhquocs.truelab.feature.match.presentation.components.MatchesFeedbackCard
import dev.anhquocs.truelab.feature.match.presentation.model.MatchDetailUiState
import dev.anhquocs.truelab.feature.match.presentation.model.MatchStatusFilter
import dev.anhquocs.truelab.feature.match.presentation.model.MatchesUiState
import dev.anhquocs.truelab.feature.match.presentation.viewmodel.MatchesViewModel
import dev.anhquocs.truelab.feature.prediction.presentation.components.PredictionBottomSheet
import dev.anhquocs.truelab.feature.prediction.presentation.model.PredictionUiState
import dev.anhquocs.truelab.feature.prediction.presentation.viewmodel.PredictionViewModel
import dev.anhquocs.truelab.navigation.TrueLabMainLayout

private val TOP_BAR_HEIGHT = 75.dp

@Composable
fun MatchesScreen(
    modifier: Modifier = Modifier,
    viewModel: MatchesViewModel = hiltViewModel(),
    onMatchClick: (String) -> Unit = {},
    onNavigateToPrediction: (Long) -> Unit = {}
) {
    val predictionViewModel: PredictionViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val matchDetailState by viewModel.matchDetailState.collectAsStateWithLifecycle()
    val predictionState by predictionViewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currentSort by viewModel.sortCriteria.collectAsStateWithLifecycle()
    val currentFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val leagues by viewModel.leagues.collectAsStateWithLifecycle()
    val selectedLeagueId by viewModel.selectedLeagueId.collectAsStateWithLifecycle()

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

    val lazyListState = rememberLazyListState()

    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisible = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()
            val totalItems = lazyListState.layoutInfo.totalItemsCount
            lastVisible != null && totalItems > 0 && lastVisible.index >= totalItems - 3
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            viewModel.loadNextPage()
        }
    }

    TrueLabMainLayout(
        modifier = modifier,
        headerHeight = TOP_BAR_HEIGHT,
        header = { MatchesHeader() }
    ) { contentModifier ->
        LazyColumn(
            state = lazyListState,
            modifier = contentModifier
                .fillMaxSize()
                .padding(horizontal = Dimen.PaddingM),
            verticalArrangement = Arrangement.spacedBy(Dimen.PaddingM),
            contentPadding = PaddingValues(bottom = Dimen.PaddingUltra)
        ) {
            // Search Input Field
            item {
                Spacer(modifier = Modifier.height(Dimen.PaddingS))
                DatasetSearchField(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.onSearchQueryChanged(it) }
                )
            }

            // League Filter Chips
            if (leagues.isNotEmpty()) {
                item {
                    LeagueFilterChips(
                        leagues = leagues,
                        selectedLeagueId = selectedLeagueId,
                        onSelectLeague = { viewModel.onLeagueSelected(it) }
                    )
                }
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
                                .height(Dimen.SizeUltra),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(Dimen.SizeXL)
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

                    if (state.isLoadingMore) {
                        item(key = "loading_more_indicator") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = Dimen.PaddingM),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(Dimen.SizeL)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (matchDetailState !is MatchDetailUiState.Idle) {
        MatchDetailBottomSheet(
            state = matchDetailState,
            isPredicting = predictionState is PredictionUiState.Predicting,
            onDismiss = { viewModel.onDismissMatchDetail() },
            onPredictClick = { matchId ->
                val match = (matchDetailState as? MatchDetailUiState.Success)?.match
                if (match != null && match.id == matchId) {
                    predictionViewModel.predictMatch(match)
                }
            }
        )
    }

    if (predictionState is PredictionUiState.Success) {
        val successState = predictionState as PredictionUiState.Success
        LaunchedEffect(successState.selectedMatch.id) {
            viewModel.onDismissMatchDetail()
        }
        PredictionBottomSheet(
            selectedMatch = successState.selectedMatch,
            predictionResult = successState.predictionResult,
            leagueName = successState.selectedMatch.leagueName,
            leagueLogo = successState.selectedMatch.leagueLogo,
            selectedDate = successState.selectedMatch.startTimeDate.take(10),
            onDismissRequest = {
                predictionViewModel.onSelectMatch(successState.selectedMatch.id)
                viewModel.onDismissMatchDetail()
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
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(SpacingS))
    }
}
