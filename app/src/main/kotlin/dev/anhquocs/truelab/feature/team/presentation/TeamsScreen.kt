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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusMedium
import dev.anhquocs.truelab.core.ui.theme.RadiusPill
import dev.anhquocs.truelab.core.ui.theme.RadiusSmall
import dev.anhquocs.truelab.core.ui.theme.SpacingXS
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.medium
import dev.anhquocs.truelab.core.ui.utils.s10
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s13
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.s15
import dev.anhquocs.truelab.core.ui.utils.s16
import dev.anhquocs.truelab.core.ui.utils.s18
import dev.anhquocs.truelab.core.ui.utils.semiBold
import dev.anhquocs.truelab.feature.team.presentation.components.TeamAnalyticsCard
import dev.anhquocs.truelab.navigation.TrueLabMainLayout

private val TOP_BAR_HEIGHT = 120.dp

data class TeamAnalyticsRecord(
    val id: String,
    val name: String,
    val league: String,
    val eloRating: Int,
    val rank: Int,
    val played: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val form: List<Char>,
    val formScore: Int, // max 15 (W=3, D=1, L=0)
    val homeWinRate: Double,
    val homeRecord: String,
    val awayWinRate: Double,
    val awayRecord: String,
    val h2hHighlight: String
)

@Composable
fun TeamsScreen(
    modifier: Modifier = Modifier,
    onTeamClick: (String) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }

    val sampleTeams = remember {
        listOf(
            TeamAnalyticsRecord(
                id = "1",
                name = "Manchester City",
                league = "Premier League • England",
                eloRating = 1980,
                rank = 1,
                played = 28,
                wins = 20,
                draws = 5,
                losses = 3,
                form = listOf('W', 'W', 'W', 'D', 'W'),
                formScore = 13,
                homeWinRate = 85.7,
                homeRecord = "12W - 1D - 1L",
                awayWinRate = 57.1,
                awayRecord = "8W - 4D - 2L",
                h2hHighlight = "75% Thắng vs Top 6"
            ),
            TeamAnalyticsRecord(
                id = "2",
                name = "Real Madrid",
                league = "La Liga • Spain",
                eloRating = 1960,
                rank = 2,
                played = 27,
                wins = 21,
                draws = 4,
                losses = 2,
                form = listOf('W', 'W', 'W', 'W', 'D'),
                formScore = 13,
                homeWinRate = 84.6,
                homeRecord = "11W - 2D - 0L",
                awayWinRate = 71.4,
                awayRecord = "10W - 2D - 2L",
                h2hHighlight = "80% Thắng El Clasico gần nhất"
            ),
            TeamAnalyticsRecord(
                id = "3",
                name = "Arsenal",
                league = "Premier League • England",
                eloRating = 1945,
                rank = 3,
                played = 28,
                wins = 19,
                draws = 6,
                losses = 3,
                form = listOf('W', 'W', 'D', 'W', 'L'),
                formScore = 10,
                homeWinRate = 78.6,
                homeRecord = "11W - 2D - 1L",
                awayWinRate = 57.1,
                awayRecord = "8W - 4D - 2L",
                h2hHighlight = "66.7% Thắng Derby London"
            ),
            TeamAnalyticsRecord(
                id = "4",
                name = "Liverpool",
                league = "Premier League • England",
                eloRating = 1920,
                rank = 4,
                played = 28,
                wins = 18,
                draws = 7,
                losses = 3,
                form = listOf('D', 'W', 'W', 'W', 'W'),
                formScore = 13,
                homeWinRate = 85.7,
                homeRecord = "12W - 2D - 0L",
                awayWinRate = 42.9,
                awayRecord = "6W - 5D - 3L",
                h2hHighlight = "Bất bại sân nhà (14 trận)"
            ),
            TeamAnalyticsRecord(
                id = "5",
                name = "Bayern Munich",
                league = "Bundesliga • Germany",
                eloRating = 1910,
                rank = 5,
                played = 26,
                wins = 17,
                draws = 4,
                losses = 5,
                form = listOf('L', 'W', 'W', 'D', 'W'),
                formScore = 10,
                homeWinRate = 76.9,
                homeRecord = "10W - 2D - 1L",
                awayWinRate = 53.8,
                awayRecord = "7W - 2D - 4L",
                h2hHighlight = "60% Thắng vs Top 4"
            ),
            TeamAnalyticsRecord(
                id = "6",
                name = "Inter Milan",
                league = "Serie A • Italy",
                eloRating = 1890,
                rank = 6,
                played = 27,
                wins = 18,
                draws = 5,
                losses = 4,
                form = listOf('W', 'W', 'W', 'L', 'W'),
                formScore = 12,
                homeWinRate = 78.6,
                homeRecord = "11W - 2D - 1L",
                awayWinRate = 53.8,
                awayRecord = "7W - 3D - 3L",
                h2hHighlight = "70% Thắng Derby Milano"
            )
        )
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
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it }
                )
            }

            itemsIndexed(sampleTeams) { _, team ->
                TeamAnalyticsCard(
                    team = team,
                    onClick = { onTeamClick(team.id) }
                )
            }
        }
    }
}

@Composable
private fun TeamsHeader() {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Dimen.PaddingS)) {
        Text(
            text = stringResource(R.string.teams_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(SpacingXS))
        Text(
            text = stringResource(R.string.teams_subtitle),
            style = MaterialTheme.typography.s13.medium(),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
private fun TeamAnalyticsCard(
    team: TeamAnalyticsRecord,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusLarge),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimen.PaddingM)
        ) {
            // 1. Team Header Row: Rank, Avatar, Name, League, Elo Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rank Badge
                Box(
                    modifier = Modifier
                        .size(Dimen.SizeM)
                        .clip(RoundedCornerShape(RadiusSmall))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${team.rank}",
                        style = MaterialTheme.typography.s12.bold(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(Dimen.PaddingS))

                // Team Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = team.name,
                        style = MaterialTheme.typography.s16.bold(),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = team.league,
                        style = MaterialTheme.typography.s12,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Elo Rating Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(RadiusPill))
                        .background(Color(0xFFF59E0B).copy(alpha = 0.15f))
                        .padding(horizontal = Dimen.PaddingS, vertical = Dimen.PaddingXXS)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingXXS)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${team.eloRating} Elo",
                            style = MaterialTheme.typography.s12.bold(),
                            color = Color(0xFFD97706)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimen.PaddingM))

            // 2. Form Score & 5-Match Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.teams_form_score_label),
                        style = MaterialTheme.typography.s10,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${team.formScore}/15 pts (${(team.formScore * 100) / 15}%)",
                        style = MaterialTheme.typography.s13.bold(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Form W/D/L Badges
                Row(horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingXS)) {
                    team.form.forEach { res ->
                        FormPillBadge(res = res)
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimen.PaddingS))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(Dimen.PaddingS))

            // 3. Home / Away Splits & H2H Highlights
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home Stats
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingXXS)) {
                        Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                        Text(text = "Sân nhà", style = MaterialTheme.typography.s10, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = "${team.homeWinRate}% (${team.homeRecord})",
                        style = MaterialTheme.typography.s12.semiBold(),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Away Stats
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingXXS)) {
                        Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(12.dp))
                        Text(text = "Sân khách", style = MaterialTheme.typography.s10, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = "${team.awayWinRate}% (${team.awayRecord})",
                        style = MaterialTheme.typography.s12.semiBold(),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // H2H
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "H2H Highlight", style = MaterialTheme.typography.s10, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = team.h2hHighlight,
                        style = MaterialTheme.typography.s12.bold(),
                        color = Color(0xFF10B981)
                    )
                }
            }
        }
    }
}

@Composable
private fun FormPillBadge(res: Char) {
    val (bgColor, textColor) = when (res) {
        'W' -> Color(0xFF10B981) to Color.White
        'D' -> Color(0xFFF59E0B) to Color.White
        else -> Color(0xFFEF4444) to Color.White
    }

    Box(
        modifier = Modifier
            .size(Dimen.SizeS2)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = res.toString(),
            color = textColor,
            style = MaterialTheme.typography.s10.bold()
        )
    }
}
