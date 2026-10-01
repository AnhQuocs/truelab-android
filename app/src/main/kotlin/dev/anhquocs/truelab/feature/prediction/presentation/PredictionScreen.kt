package dev.anhquocs.truelab.feature.prediction.presentation

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusMedium
import dev.anhquocs.truelab.core.ui.theme.SpacingS
import dev.anhquocs.truelab.core.ui.theme.SpacingXS
import dev.anhquocs.truelab.core.ui.theme.SpacingXXS
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.s16
import dev.anhquocs.truelab.core.ui.utils.s18
import dev.anhquocs.truelab.core.ui.utils.semiBold
import dev.anhquocs.truelab.feature.prediction.presentation.components.PredictableMatchCard
import dev.anhquocs.truelab.feature.prediction.presentation.components.PredictionBottomSheet
import dev.anhquocs.truelab.feature.prediction.presentation.components.PredictionEmptyCard
import dev.anhquocs.truelab.feature.prediction.presentation.components.PredictionFeedbackCard
import dev.anhquocs.truelab.feature.prediction.presentation.components.PredictionFilterBar
import dev.anhquocs.truelab.feature.prediction.presentation.components.PredictionHeader
import dev.anhquocs.truelab.feature.prediction.presentation.components.PREDICTION_HEADER_HEIGHT
import dev.anhquocs.truelab.feature.prediction.presentation.model.PredictionUiState
import dev.anhquocs.truelab.feature.prediction.presentation.viewmodel.PredictionViewModel
import dev.anhquocs.truelab.navigation.TrueLabMainLayout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictionScreen(
    modifier: Modifier = Modifier,
    viewModel: PredictionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val availableMatches by viewModel.availableMatches.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedLeagueId by viewModel.selectedLeagueId.collectAsStateWithLifecycle()
    val selectedStatusFilter by viewModel.selectedStatusFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val leagues by viewModel.leagues.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val selectedMatchId by viewModel.selectedMatchId.collectAsStateWithLifecycle()

    var showPredictionSheet by remember { mutableStateOf(false) }

    val pullToRefreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.refreshErrorEvent.collect { errorUiText ->
            val message = errorUiText.asString(context)
            snackbarHostState.showSnackbar(message)
        }
    }

    val currentSelectedId = (uiState as? PredictionUiState.Success)?.selectedMatch?.id ?: selectedMatchId
    val leagueNameMap = remember(leagues) { leagues.associate { it.id to it.name } }

    val groupedMatches = remember(availableMatches, leagues, selectedDate) {
        availableMatches
            .groupBy { match ->
                match.leagueId?.toString() ?: match.leagueName ?: "default_group"
            }
            .map { (_, matchesInGroup) ->
                val leagueId = matchesInGroup.firstNotNullOfOrNull { it.leagueId }
                val leagueName = matchesInGroup.firstNotNullOfOrNull { it.leagueName }
                    ?: leagueId?.let { id -> leagueNameMap[id] }
                    ?: "Giải đấu"
                val leagueLogo = matchesInGroup.firstNotNullOfOrNull { it.leagueLogo }
                    ?: leagueId?.let { id -> leagues.find { it.id == id }?.logo }

                val sortedMatches = matchesInGroup.sortedWith(
                    compareBy<dev.anhquocs.truelab.core.domain.match.model.Match> { match ->
                        when (dev.anhquocs.truelab.feature.prediction.presentation.components.resolveDisplayStatus(match, selectedDate)) {
                            dev.anhquocs.truelab.feature.prediction.presentation.components.DisplayMatchStatus.LIVE -> 0
                            dev.anhquocs.truelab.feature.prediction.presentation.components.DisplayMatchStatus.STARTED -> 1
                            dev.anhquocs.truelab.feature.prediction.presentation.components.DisplayMatchStatus.UPCOMING -> 2
                            dev.anhquocs.truelab.feature.prediction.presentation.components.DisplayMatchStatus.ENDED -> 3
                        }
                    }.thenBy { it.startTimeDate }
                )

                dev.anhquocs.truelab.feature.prediction.presentation.components.CompetitionMatchGroup(
                    leagueId = leagueId,
                    leagueName = leagueName,
                    leagueLogo = leagueLogo,
                    matches = sortedMatches
                )
            }
    }

    TrueLabMainLayout(
        modifier = modifier,
        headerHeight = PREDICTION_HEADER_HEIGHT,
        header = {
            PredictionHeader(
                onNavigateBack = onNavigateBack,
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh() }
            )
        }
    ) { contentModifier ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            state = pullToRefreshState,
            modifier = contentModifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Dimen.PaddingM,
                    end = Dimen.PaddingM,
                    top = Dimen.PaddingS,
                    bottom = Dimen.PaddingUltra
                ),
                verticalArrangement = Arrangement.spacedBy(Dimen.PaddingS)
            ) {
                // Section 1: Filters (Date, Competition, Status, Search)
                item {
                    PredictionFilterBar(
                        selectedDate = selectedDate,
                        onDateChanged = { viewModel.onDateSelected(it) },
                        leagues = leagues,
                        selectedLeagueId = selectedLeagueId,
                        onLeagueSelected = { viewModel.onLeagueSelected(it) },
                        selectedStatusFilter = selectedStatusFilter,
                        onStatusFilterSelected = { viewModel.onStatusFilterSelected(it) },
                        searchQuery = searchQuery,
                        onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) }
                    )
                }

                // Section 2: Match Candidates Grouped by Competition
                if (groupedMatches.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.prediction_select_match),
                            style = MaterialTheme.typography.s14.semiBold(),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = SpacingXXS)
                        )
                    }

                    groupedMatches.forEach { group ->
                        item(key = "header_${group.leagueId ?: group.leagueName}") {
                            dev.anhquocs.truelab.feature.prediction.presentation.components.CompetitionSectionHeader(group = group)
                        }

                        items(
                            items = group.matches,
                            key = { it.id }
                        ) { match ->
                            PredictableMatchCard(
                                match = match,
                                isSelected = match.id == currentSelectedId,
                                showLeagueHeader = false,
                                leagueName = group.leagueName,
                                leagueLogo = group.leagueLogo,
                                selectedDate = selectedDate,
                                onClick = {
                                    viewModel.onSelectMatch(match.id)
                                },
                                onPredictClick = {
                                    viewModel.onSelectMatch(match.id)
                                    showPredictionSheet = true
                                }
                            )
                        }
                    }
                }

                // Section 3: Status / Feedback (Loading, Empty, Error)
                when (val state = uiState) {
                    is PredictionUiState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = Dimen.PaddingXXL),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(Dimen.SizeXL),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    is PredictionUiState.Empty -> {
                        item {
                            PredictionEmptyCard(
                                message = stringResource(R.string.prediction_empty_filter_desc),
                                onResetFilters = { viewModel.onResetFilters() }
                            )
                        }
                    }

                    is PredictionUiState.Error -> {
                        item {
                            PredictionFeedbackCard(
                                icon = Icons.Default.ErrorOutline,
                                iconTint = MaterialTheme.colorScheme.error,
                                title = stringResource(R.string.prediction_error_default),
                                message = state.message.asString(),
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                                titleColor = MaterialTheme.colorScheme.error,
                                messageColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    is PredictionUiState.Success -> {
                        // Prediction results are displayed inside PredictionBottomSheet
                    }
                }
            }

            androidx.compose.material3.SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = Dimen.PaddingUltra)
            )
        }
    }

    // ModalBottomSheet for Prediction Result & Detailed Evidence
    if (showPredictionSheet && uiState is PredictionUiState.Success) {
        val successState = uiState as PredictionUiState.Success
        PredictionBottomSheet(
            selectedMatch = successState.selectedMatch,
            predictionResult = successState.predictionResult,
            leagueName = successState.selectedMatch.leagueName ?: successState.selectedMatch.leagueId?.let { leagueNameMap[it] },
            leagueLogo = successState.selectedMatch.leagueLogo ?: leagues.find { it.id == successState.selectedMatch.leagueId }?.logo,
            selectedDate = selectedDate,
            onDismissRequest = { showPredictionSheet = false }
        )
    }
}
