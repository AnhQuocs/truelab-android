package dev.anhquocs.truelab.feature.team.presentation

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusMedium
import dev.anhquocs.truelab.core.ui.theme.SpacingL
import dev.anhquocs.truelab.core.ui.theme.SpacingM
import dev.anhquocs.truelab.core.ui.theme.SpacingXL
import dev.anhquocs.truelab.core.ui.theme.SpacingXXS
import dev.anhquocs.truelab.navigation.TrueLabMainLayout

data class TeamLeaderboardItem(
    val id: String,
    val name: String,
    val eloRating: Int,
    val played: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val form: List<Char> // 'W', 'D', 'L'
)

@Composable
fun TeamsScreen(
    modifier: Modifier = Modifier,
    onTeamClick: (String) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }

    val sampleTeams = listOf(
        TeamLeaderboardItem("1", "Manchester City", 1980, 28, 20, 5, 3, listOf('W', 'W', 'W', 'D', 'W')),
        TeamLeaderboardItem("2", "Arsenal", 1945, 28, 19, 6, 3, listOf('W', 'W', 'D', 'W', 'L')),
        TeamLeaderboardItem("3", "Liverpool", 1920, 28, 18, 7, 3, listOf('D', 'W', 'W', 'W', 'W')),
        TeamLeaderboardItem("4", "Real Madrid", 1960, 27, 21, 4, 2, listOf('W', 'W', 'W', 'W', 'D')),
        TeamLeaderboardItem("5", "Bayern Munich", 1910, 26, 17, 4, 5, listOf('L', 'W', 'W', 'D', 'W')),
        TeamLeaderboardItem("6", "Paris Saint-Germain", 1885, 26, 16, 6, 4, listOf('W', 'D', 'W', 'W', 'D')),
        TeamLeaderboardItem("7", "Inter Milan", 1890, 27, 18, 5, 4, listOf('W', 'W', 'W', 'L', 'W'))
    )

    TrueLabMainLayout(
        modifier = modifier,
        header = {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = SpacingL)) {
                Text(
                    text = stringResource(R.string.teams_title),
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
                    placeholder = { Text(stringResource(R.string.teams_search_hint)) },
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

            itemsIndexed(sampleTeams) { index, team ->
                TeamCard(
                    rank = index + 1,
                    team = team,
                    onClick = { onTeamClick(team.id) }
                )
            }
        }
    }
}

@Composable
private fun TeamCard(
    rank: Int,
    team: TeamLeaderboardItem,
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
            Text(
                text = "$rank",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(SpacingXL)
            )

            // Team Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = team.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(SpacingXXS))
                Text(
                    text = "${stringResource(R.string.teams_elo_label)}: ${team.eloRating}  •  ${team.wins}W - ${team.draws}D - ${team.losses}L",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            // Form Badges
            Row(horizontalArrangement = Arrangement.spacedBy(SpacingXXS)) {
                team.form.takeLast(5).forEach { result ->
                    FormBadge(result = result)
                }
            }
        }
    }
}

@Composable
private fun FormBadge(result: Char) {
    val (bgColor, textColor) = when (result) {
        'W' -> Color(0xFF10B981) to Color.White
        'D' -> Color(0xFFF59E0B) to Color.White
        else -> Color(0xFFEF4444) to Color.White
    }

    Box(
        modifier = Modifier
            .size(SpacingL)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = result.toString(),
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
