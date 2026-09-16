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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.text.style.TextAlign
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusMedium
import dev.anhquocs.truelab.core.ui.theme.SpacingL
import dev.anhquocs.truelab.core.ui.theme.SpacingM
import dev.anhquocs.truelab.core.ui.theme.SpacingS
import dev.anhquocs.truelab.core.ui.theme.SpacingXL
import dev.anhquocs.truelab.core.ui.theme.SpacingXS
import dev.anhquocs.truelab.navigation.TrueLabMainLayout

data class MatchSample(
    val id: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeScore: Int?,
    val awayScore: Int?,
    val status: String,
    val matchTime: String
)

@Composable
fun MatchesScreen(
    modifier: Modifier = Modifier,
    onMatchClick: (String) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterIndex by remember { mutableIntStateOf(0) }

    val filterLabels = listOf(
        stringResource(R.string.matches_filter_all),
        stringResource(R.string.matches_filter_live),
        stringResource(R.string.matches_filter_finished),
        stringResource(R.string.matches_filter_upcoming)
    )

    val sampleMatches = listOf(
        MatchSample("1", "Arsenal", "Chelsea", 2, 1, "FT", "20:00"),
        MatchSample("2", "Man City", "Liverpool", 1, 1, "78'", "22:30"),
        MatchSample("3", "Real Madrid", "Barcelona", null, null, "Upcoming", "Tomorrow 02:00"),
        MatchSample("4", "Bayern Munich", "Dortmund", 3, 2, "FT", "Yesterday"),
        MatchSample("5", "Inter Milan", "AC Milan", null, null, "Upcoming", "Sun 01:45")
    )

    TrueLabMainLayout(
        modifier = modifier,
        header = {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = SpacingL)) {
                Text(
                    text = stringResource(R.string.matches_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { contentModifier ->
        LazyColumn(
            modifier = contentModifier
                .fillMaxSize()
                .padding(horizontal = SpacingL),
            verticalArrangement = Arrangement.spacedBy(SpacingM),
            contentPadding = PaddingValues(bottom = SpacingXL)
        ) {
            item {
                Spacer(modifier = Modifier.height(SpacingM))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.matches_search_hint)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(RadiusMedium),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(SpacingS),
                    contentPadding = PaddingValues(vertical = SpacingXS)
                ) {
                    items(filterLabels.size) { index ->
                        FilterChip(
                            selected = selectedFilterIndex == index,
                            onClick = { selectedFilterIndex = index },
                            label = { Text(filterLabels[index]) }
                        )
                    }
                }
            }

            items(sampleMatches) { match ->
                MatchCard(
                    match = match,
                    onClick = { onMatchClick(match.id) }
                )
            }
        }
    }
}

@Composable
private fun MatchCard(
    match: MatchSample,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusLarge),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingL),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Home Team
            Text(
                text = match.homeTeam,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start
            )

            // Score / Status Center
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = SpacingS)
            ) {
                if (match.homeScore != null && match.awayScore != null) {
                    Text(
                        text = "${match.homeScore} - ${match.awayScore}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "VS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                SuggestionChip(
                    onClick = {},
                    label = {
                        Text(
                            text = match.status,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            }

            // Away Team
            Text(
                text = match.awayTeam,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
        }
    }
}
