package dev.anhquocs.truelab.feature.analytics.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.team.model.TeamDetail
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusPill
import dev.anhquocs.truelab.core.ui.theme.SpacingS
import dev.anhquocs.truelab.core.ui.theme.SpacingXS
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.s10
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s13
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.s16
import dev.anhquocs.truelab.core.ui.utils.s20
import dev.anhquocs.truelab.feature.analytics.presentation.components.DescriptiveStatsCard
import dev.anhquocs.truelab.feature.analytics.presentation.components.MultiProviderOddsCard
import dev.anhquocs.truelab.feature.analytics.presentation.components.OddsTrendCard
import dev.anhquocs.truelab.feature.analytics.presentation.model.AnalyticsUiState
import dev.anhquocs.truelab.feature.analytics.presentation.viewmodel.AnalyticsViewModel
import dev.anhquocs.truelab.navigation.TrueLabMainLayout

private val TOP_BAR_HEIGHT = 75.dp

@Composable
fun AnalyticsScreen(
    modifier: Modifier = Modifier,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TrueLabMainLayout(
        headerHeight = TOP_BAR_HEIGHT,
        header = {
            AnalyticsHeader(modifier = Modifier.fillMaxWidth())
        },
        modifier = modifier
    ) { contentModifier ->
        when (val state = uiState) {
            is AnalyticsUiState.Loading -> {
                Box(
                    modifier = contentModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Dimen.SizeXL),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            is AnalyticsUiState.Error -> {
                Box(
                    modifier = contentModifier
                        .fillMaxSize()
                        .padding(Dimen.PaddingM),
                    contentAlignment = Alignment.Center
                ) {
                    AnalyticsFeedbackCard(
                        icon = Icons.Default.ErrorOutline,
                        iconTint = MaterialTheme.colorScheme.error,
                        title = stringResource(R.string.analytics_error_default),
                        message = state.message.asString(),
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        titleColor = MaterialTheme.colorScheme.error,
                        messageColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            is AnalyticsUiState.Empty -> {
                Box(
                    modifier = contentModifier
                        .fillMaxSize()
                        .padding(Dimen.PaddingM),
                    contentAlignment = Alignment.Center
                ) {
                    AnalyticsFeedbackCard(
                        icon = Icons.Default.Info,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = stringResource(R.string.analytics_title),
                        message = state.message.asString(),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        titleColor = MaterialTheme.colorScheme.primary,
                        messageColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            is AnalyticsUiState.Success -> {
                LazyColumn(
                    modifier = contentModifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Dimen.PaddingM,
                        end = Dimen.PaddingM,
                        top = Dimen.PaddingS,
                        bottom = Dimen.PaddingXXL
                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimen.PaddingM)
                ) {
                    if (state.availableTeams.isNotEmpty()) {
                        item {
                            TeamSelectorSection(
                                teams = state.availableTeams,
                                selectedTeam = state.selectedTeam,
                                onTeamSelected = { viewModel.onTeamSelected(it.id) }
                            )
                        }
                    }

                    if (state.availableMatches.isNotEmpty()) {
                        item {
                            MatchSelectorSection(
                                matches = state.availableMatches,
                                selectedMatch = state.selectedMatch,
                                onMatchSelected = { viewModel.onMatchSelected(it.id) }
                            )
                        }
                    }

                    // Module 1: Descriptive Statistics
                    item {
                        DescriptiveStatsCard(
                            teamStats = state.teamStats
                        )
                    }

                    // Module 2: Multi-Provider Odds Matrix
                    item {
                        val matchTitle = state.selectedMatch?.let {
                            "${it.homeTeam.name} vs ${it.awayTeam.name}"
                        } ?: stringResource(R.string.analytics_no_odds)

                        MultiProviderOddsCard(
                            matchOdds = state.matchOdds,
                            matchTitle = matchTitle,
                            avgHomeOdds = state.avgHomeOdds,
                            avgDrawOdds = state.avgDrawOdds,
                            avgAwayOdds = state.avgAwayOdds,
                            oddsSpread = state.oddsSpread
                        )
                    }

                    // Module 3: Odds Trend & Moving Average
                    item {
                        OddsTrendCard(
                            oddsTrend = state.oddsTrend,
                            selectedTargetField = state.selectedTargetField,
                            selectedWindowSize = state.selectedWindowSize,
                            onTargetFieldSelected = viewModel::onTargetFieldSelected,
                            onWindowSizeChanged = viewModel::onWindowSizeChanged
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamSelectorSection(
    teams: List<TeamDetail>,
    selectedTeam: TeamDetail?,
    onTeamSelected: (TeamDetail) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.analytics_select_team),
            style = MaterialTheme.typography.s13.bold(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = SpacingXS)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(SpacingS)
        ) {
            teams.forEach { team ->
                val isSelected = team.id == selectedTeam?.id
                FilterChip(
                    selected = isSelected,
                    onClick = { onTeamSelected(team) },
                    label = {
                        Text(
                            text = team.name,
                            style = MaterialTheme.typography.s12
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

@Composable
private fun MatchSelectorSection(
    matches: List<Match>,
    selectedMatch: Match?,
    onMatchSelected: (Match) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.analytics_select_match),
            style = MaterialTheme.typography.s13.bold(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = SpacingXS)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(SpacingS)
        ) {
            matches.forEach { match ->
                val isSelected = match.id == selectedMatch?.id
                val label = "${match.homeTeam.name} vs ${match.awayTeam.name}"
                FilterChip(
                    selected = isSelected,
                    onClick = { onMatchSelected(match) },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.s12
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
        }
    }
}

@Composable
private fun AnalyticsHeader(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = Dimen.PaddingM),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = Dimen.PaddingS),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.analytics_title),
                style = MaterialTheme.typography.s20.bold(),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.analytics_subtitle),
                style = MaterialTheme.typography.s14,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(RadiusPill))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = Dimen.PaddingM, vertical = SpacingXS)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingXXS)
            ) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(Dimen.SizeS)
                )
                Text(
                    text = "EDA v2.4",
                    style = MaterialTheme.typography.s10.bold(),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun AnalyticsFeedbackCard(
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
                .padding(Dimen.PaddingL),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SpacingS)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.s16.bold(),
                color = titleColor
            )
            Text(
                text = message,
                style = MaterialTheme.typography.s14,
                color = messageColor,
                textAlign = TextAlign.Center
            )
        }
    }
}
