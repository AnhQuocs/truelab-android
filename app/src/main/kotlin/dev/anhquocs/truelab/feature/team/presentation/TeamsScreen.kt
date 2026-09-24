package dev.anhquocs.truelab.feature.team.presentation

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.team.model.StandingsSortCriteria
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.SpacingS
import dev.anhquocs.truelab.core.ui.theme.SpacingXS
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.s13
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.s16
import dev.anhquocs.truelab.core.ui.utils.s20
import dev.anhquocs.truelab.feature.team.presentation.components.TeamAnalyticsCard
import dev.anhquocs.truelab.feature.team.presentation.model.TeamsUiState
import dev.anhquocs.truelab.feature.team.presentation.viewmodel.TeamsViewModel
import dev.anhquocs.truelab.navigation.TrueLabMainLayout

private val TOP_BAR_HEIGHT = 75.dp

@Composable
fun TeamsScreen(
    modifier: Modifier = Modifier,
    viewModel: TeamsViewModel = hiltViewModel(),
    onTeamClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchInput by remember { mutableStateOf("") }

    val currentSort = when (val state = uiState) {
        is TeamsUiState.Success -> state.standingsSort
        else -> StandingsSortCriteria.POSITION_ASC
    }

    TrueLabMainLayout(
        modifier = modifier,
        headerHeight = TOP_BAR_HEIGHT,
        header = { TeamsHeader() }
    ) { contentModifier ->
        LazyColumn(
            modifier = contentModifier
                .fillMaxSize()
                .padding(horizontal = Dimen.PaddingM),
            verticalArrangement = Arrangement.spacedBy(Dimen.PaddingM),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(Dimen.PaddingS))
                TeamsSearchField(
                    searchQuery = searchInput,
                    onSearchQueryChange = { query ->
                        searchInput = query
                        viewModel.onSearchQueryChanged(query)
                    }
                )
            }

            item {
                StandingsSortSection(
                    selectedSort = currentSort,
                    onSelectSort = { viewModel.onSortChanged(it) }
                )
            }

            when (val state = uiState) {
                is TeamsUiState.Loading -> {
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

                is TeamsUiState.Empty -> {
                    item {
                        TeamsFeedbackCard(
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

                is TeamsUiState.Error -> {
                    item {
                        TeamsFeedbackCard(
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

                is TeamsUiState.Success -> {
                    items(
                        items = state.teams,
                        key = { it.id }
                    ) { team ->
                        TeamAnalyticsCard(
                            team = team,
                            onClick = { onTeamClick(team.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamsHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimen.PaddingM),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.teams_title),
            style = MaterialTheme.typography.s20.bold(),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(SpacingXS))
        Text(
            text = stringResource(R.string.teams_subtitle),
            style = MaterialTheme.typography.s14,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(SpacingS))
    }
}

@Composable
private fun TeamsSearchField(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = {
            Text(
                text = stringResource(R.string.teams_search_hint),
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
private fun StandingsSortSection(
    selectedSort: StandingsSortCriteria,
    onSelectSort: (StandingsSortCriteria) -> Unit
) {
    val sortOptions = listOf(
        StandingsSortCriteria.POSITION_ASC to stringResource(R.string.teams_sort_position),
        StandingsSortCriteria.POINTS_DESC to stringResource(R.string.teams_sort_points),
        StandingsSortCriteria.GOAL_DIFF_DESC to stringResource(R.string.teams_sort_goal_diff),
        StandingsSortCriteria.WINS_DESC to stringResource(R.string.teams_sort_wins),
        StandingsSortCriteria.LOSSES_ASC to stringResource(R.string.teams_sort_losses)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.teams_sort_label),
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
            sortOptions.forEach { (sort, label) ->
                val isSelected = selectedSort == sort
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectSort(sort) },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.s13
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
private fun TeamsFeedbackCard(
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
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
