package dev.anhquocs.truelab.feature.match.presentation

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusMedium
import dev.anhquocs.truelab.core.ui.theme.SpacingXS
import dev.anhquocs.truelab.core.ui.utils.medium
import dev.anhquocs.truelab.core.ui.utils.s10
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s13
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.semiBold
import dev.anhquocs.truelab.feature.match.presentation.components.MatchDataCard
import dev.anhquocs.truelab.navigation.TrueLabMainLayout

private val TOP_BAR_HEIGHT = 75.dp

data class MatchDataRecord(
    val id: String,
    val league: String,
    val date: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeScore: Int,
    val awayScore: Int,
    val actualResult: String, // "HOME_WIN", "DRAW", "AWAY_WIN"
    val avgHomeOdds: Double,
    val avgDrawOdds: Double,
    val avgAwayOdds: Double,
    val providerCount: Int,
    val eloDiff: Int,
    val totalGoals: Int,
    val isNormalized: Boolean,
    val predictedProb: String
)

@Composable
fun MatchesScreen(
    modifier: Modifier = Modifier,
    onMatchClick: (String) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    var selectedSortIndex by remember { mutableIntStateOf(0) }

    val filterLabels = listOf(
        stringResource(R.string.matches_filter_all),
        stringResource(R.string.matches_filter_with_odds),
        stringResource(R.string.matches_filter_cleaned),
        stringResource(R.string.matches_filter_backtested)
    )

    val sortLabels = listOf(
        stringResource(R.string.matches_sort_date),
        stringResource(R.string.matches_sort_goals),
        stringResource(R.string.matches_sort_id)
    )

    val sampleRecords = remember {
        listOf(
            MatchDataRecord(
                id = "M-38012",
                league = "Premier League",
                date = "10 Th03, 2024",
                homeTeam = "Arsenal",
                awayTeam = "Chelsea",
                homeScore = 2,
                awayScore = 1,
                actualResult = "HOME_WIN",
                avgHomeOdds = 1.85,
                avgDrawOdds = 3.60,
                avgAwayOdds = 4.20,
                providerCount = 3,
                eloDiff = 85,
                totalGoals = 3,
                isNormalized = true,
                predictedProb = "HW 54%"
            ),
            MatchDataRecord(
                id = "M-38013",
                league = "Premier League",
                date = "11 Th03, 2024",
                homeTeam = "Man City",
                awayTeam = "Liverpool",
                homeScore = 1,
                awayScore = 1,
                actualResult = "DRAW",
                avgHomeOdds = 2.10,
                avgDrawOdds = 3.45,
                avgAwayOdds = 3.30,
                providerCount = 3,
                eloDiff = 60,
                totalGoals = 2,
                isNormalized = true,
                predictedProb = "DR 32%"
            ),
            MatchDataRecord(
                id = "M-38014",
                league = "La Liga",
                date = "12 Th03, 2024",
                homeTeam = "Real Madrid",
                awayTeam = "Barcelona",
                homeScore = 3,
                awayScore = 2,
                actualResult = "HOME_WIN",
                avgHomeOdds = 1.95,
                avgDrawOdds = 3.50,
                avgAwayOdds = 3.80,
                providerCount = 3,
                eloDiff = 40,
                totalGoals = 5,
                isNormalized = true,
                predictedProb = "HW 49%"
            ),
            MatchDataRecord(
                id = "M-38015",
                league = "Bundesliga",
                date = "13 Th03, 2024",
                homeTeam = "Bayern Munich",
                awayTeam = "Dortmund",
                homeScore = 2,
                awayScore = 2,
                actualResult = "DRAW",
                avgHomeOdds = 1.65,
                avgDrawOdds = 4.10,
                avgAwayOdds = 5.00,
                providerCount = 3,
                eloDiff = 120,
                totalGoals = 4,
                isNormalized = true,
                predictedProb = "HW 61%"
            ),
            MatchDataRecord(
                id = "M-38016",
                league = "Serie A",
                date = "14 Th03, 2024",
                homeTeam = "Inter Milan",
                awayTeam = "AC Milan",
                homeScore = 0,
                awayScore = 1,
                actualResult = "AWAY_WIN",
                avgHomeOdds = 2.05,
                avgDrawOdds = 3.30,
                avgAwayOdds = 3.70,
                providerCount = 3,
                eloDiff = 30,
                totalGoals = 1,
                isNormalized = true,
                predictedProb = "HW 45%"
            )
        )
    }

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
            item {
                Spacer(modifier = Modifier.height(Dimen.PaddingS))
                DatasetSearchField(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it }
                )
            }

            item {
                DatasetFilterChips(
                    filterLabels = filterLabels,
                    selectedIndex = selectedFilterIndex,
                    onSelectIndex = { selectedFilterIndex = it }
                )
            }

            item {
                DatasetSortSection(
                    sortLabels = sortLabels,
                    selectedSortIndex = selectedSortIndex,
                    onSelectSortIndex = { selectedSortIndex = it }
                )
            }

            items(sampleRecords) { record ->
                MatchDataCard(
                    record = record,
                    onClick = { onMatchClick(record.id) }
                )
            }
        }
    }
}

@Composable
private fun MatchesHeader() {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Dimen.PaddingS)) {
        Text(
            text = stringResource(R.string.matches_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(SpacingXS))
        Text(
            text = stringResource(R.string.matches_subtitle),
            style = MaterialTheme.typography.s13.medium(),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(SpacingXS))
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
    filterLabels: List<String>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingS),
        contentPadding = PaddingValues(vertical = Dimen.PaddingXXS)
    ) {
        items(filterLabels.size) { index ->
            FilterChip(
                selected = selectedIndex == index,
                onClick = { onSelectIndex(index) },
                shape = RoundedCornerShape(RadiusMedium),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                label = {
                    Text(
                        text = filterLabels[index],
                        style = MaterialTheme.typography.s12.semiBold()
                    )
                }
            )
        }
    }
}

@Composable
private fun DatasetSortSection(
    sortLabels: List<String>,
    selectedSortIndex: Int,
    onSelectSortIndex: (Int) -> Unit
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
            items(sortLabels.size) { index ->
                FilterChip(
                    selected = selectedSortIndex == index,
                    onClick = { onSelectSortIndex(index) },
                    shape = RoundedCornerShape(RadiusMedium),
                    label = {
                        Text(
                            text = sortLabels[index],
                            style = MaterialTheme.typography.s10.medium()
                        )
                    }
                )
            }
        }
    }
}
