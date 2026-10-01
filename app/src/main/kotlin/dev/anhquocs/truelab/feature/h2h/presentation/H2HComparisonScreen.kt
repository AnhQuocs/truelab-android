package dev.anhquocs.truelab.feature.h2h.presentation

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.match.model.TeamSummary
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusMedium
import dev.anhquocs.truelab.core.ui.theme.TopBarHeight
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.s16
import dev.anhquocs.truelab.core.ui.utils.s20
import dev.anhquocs.truelab.core.ui.utils.semiBold
import dev.anhquocs.truelab.feature.h2h.presentation.components.H2HClashOverviewCard
import dev.anhquocs.truelab.feature.h2h.presentation.components.H2HMatchHistoryCard
import dev.anhquocs.truelab.feature.h2h.presentation.components.H2HMetricComparisonCard
import dev.anhquocs.truelab.feature.h2h.presentation.components.H2HSplitsComparisonCard
import dev.anhquocs.truelab.feature.h2h.presentation.components.TeamPickerBottomSheet
import dev.anhquocs.truelab.feature.h2h.presentation.components.TeamSelectorCard
import dev.anhquocs.truelab.feature.h2h.presentation.model.H2HComparisonUiState
import dev.anhquocs.truelab.feature.h2h.presentation.viewmodel.H2HComparisonViewModel
import dev.anhquocs.truelab.navigation.TrueLabMainLayout

@Composable
fun H2HComparisonScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: H2HComparisonViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTeamAId by viewModel.selectedTeamAId.collectAsStateWithLifecycle()
    val selectedTeamBId by viewModel.selectedTeamBId.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val selectedTeamADetail by viewModel.selectedTeamADetail.collectAsStateWithLifecycle()
    val selectedTeamBDetail by viewModel.selectedTeamBDetail.collectAsStateWithLifecycle()

    var showTeamAPicker by remember { mutableStateOf(false) }
    var showTeamBPicker by remember { mutableStateOf(false) }

    val teamA = when (val state = uiState) {
        is H2HComparisonUiState.Success -> TeamSummary(state.teamA.id, state.teamA.name, state.teamA.logo)
        else -> selectedTeamADetail?.let { TeamSummary(it.id, it.name, it.logo) }
    }
    val teamB = when (val state = uiState) {
        is H2HComparisonUiState.Success -> TeamSummary(state.teamB.id, state.teamB.name, state.teamB.logo)
        else -> selectedTeamBDetail?.let { TeamSummary(it.id, it.name, it.logo) }
    }

    TrueLabMainLayout(
        modifier = modifier,
        headerHeight = TopBarHeight,
        header = {
            H2HHeader(
                onNavigateBack = onNavigateBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TopBarHeight)
            )
        }
    ) { contentModifier ->
        LazyColumn(
            modifier = contentModifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Dimen.PaddingM,
                end = Dimen.PaddingM,
                top = Dimen.PaddingM,
                bottom = Dimen.PaddingXXL
            ),
            verticalArrangement = Arrangement.spacedBy(Dimen.PaddingM)
        ) {
            // Team Selector Card
            item {
                TeamSelectorCard(
                    teamA = teamA,
                    teamB = teamB,
                    onPickTeamA = { showTeamAPicker = true },
                    onPickTeamB = { showTeamBPicker = true },
                    onSwap = { viewModel.swapTeams() }
                )
            }

            when (val state = uiState) {
                is H2HComparisonUiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                is H2HComparisonUiState.TeamSelectionRequired -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(RadiusLarge),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Dimen.PaddingXL),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(Dimen.PaddingM))

                                Text(
                                    text = state.message.asString(),
                                    style = MaterialTheme.typography.s14.semiBold(),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(Dimen.PaddingXS))

                                Text(
                                    text = stringResource(R.string.h2h_selection_guide_desc),
                                    style = MaterialTheme.typography.s12,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                is H2HComparisonUiState.Success -> {
                    // Clash Overview Card
                    item {
                        H2HClashOverviewCard(
                            teamA = state.teamA,
                            teamB = state.teamB,
                            comparison = state.comparison
                        )
                    }

                    // Comparative Metrics Card
                    item {
                        H2HMetricComparisonCard(
                            teamA = state.teamA,
                            teamB = state.teamB,
                            comparison = state.comparison
                        )
                    }

                    // Home/Away Splits Comparison
                    item {
                        H2HSplitsComparisonCard(
                            teamA = state.teamA,
                            teamB = state.teamB,
                            comparison = state.comparison
                        )
                    }

                    // Match History
                    item {
                        H2HMatchHistoryCard(
                            matchHistory = state.matchHistory
                        )
                    }
                }

                is H2HComparisonUiState.Error -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(RadiusMedium),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = state.message.asString(),
                                style = MaterialTheme.typography.s14,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(Dimen.PaddingM)
                            )
                        }
                    }
                }
            }
        }

        // Team Pickers Modal Bottom Sheets
        if (showTeamAPicker) {
            TeamPickerBottomSheet(
                teams = searchResults,
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
                excludedTeamId = selectedTeamBId,
                title = stringResource(R.string.h2h_select_team_a),
                onSelectTeam = { selected -> viewModel.selectTeamA(selected.id) },
                onDismissRequest = {
                    showTeamAPicker = false
                    viewModel.onSearchQueryChanged("")
                }
            )
        }

        if (showTeamBPicker) {
            TeamPickerBottomSheet(
                teams = searchResults,
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
                excludedTeamId = selectedTeamAId,
                title = stringResource(R.string.h2h_select_team_b),
                onSelectTeam = { selected -> viewModel.selectTeamB(selected.id) },
                onDismissRequest = {
                    showTeamBPicker = false
                    viewModel.onSearchQueryChanged("")
                }
            )
        }
    }
}

@Composable
private fun H2HHeader(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = Dimen.PaddingM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(modifier = Modifier.padding(start = Dimen.PaddingXS)) {
            Text(
                text = stringResource(R.string.h2h_screen_title),
                style = MaterialTheme.typography.s16.bold(),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.h2h_screen_subtitle),
                style = MaterialTheme.typography.s12,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
