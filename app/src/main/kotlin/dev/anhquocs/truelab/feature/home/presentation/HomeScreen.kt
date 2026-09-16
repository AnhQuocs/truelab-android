package dev.anhquocs.truelab.feature.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusMedium
import dev.anhquocs.truelab.core.ui.theme.SpacingL
import dev.anhquocs.truelab.core.ui.theme.SpacingM
import dev.anhquocs.truelab.core.ui.theme.SpacingXL
import dev.anhquocs.truelab.core.ui.theme.SpacingXS
import dev.anhquocs.truelab.feature.home.presentation.components.AlgorithmProgressCard
import dev.anhquocs.truelab.feature.home.presentation.components.DatasetOverviewCard
import dev.anhquocs.truelab.feature.home.presentation.components.QuickActionsGrid
import dev.anhquocs.truelab.feature.home.presentation.components.RecentPredictionsCard
import dev.anhquocs.truelab.navigation.TrueLabMainLayout

private val TOP_BAR_HEIGHT = 70.dp

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigate: (String) -> Unit = {}
) {
    TrueLabMainLayout(
        modifier = modifier,
        headerHeight = TOP_BAR_HEIGHT,
        header = { HomeHeader() }
    ) { contentModifier ->
        val scrollState = rememberScrollState()

        Column(
            modifier = contentModifier
                .fillMaxSize()
                .padding(horizontal = Dimen.PaddingM)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(SpacingL))
            DatasetOverviewCard()

            Spacer(modifier = Modifier.height(SpacingL))

            Text(
                text = stringResource(R.string.home_quick_actions),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(SpacingL))

            QuickActionsGrid(onNavigate = onNavigate)

            Spacer(modifier = Modifier.height(SpacingL))

            AlgorithmProgressCard()

            Spacer(modifier = Modifier.height(SpacingL))

            RecentPredictionsCard()

            Spacer(modifier = Modifier.height(100.dp)) // Extra padding for bottom nav
        }
    }
}

@Composable
private fun HomeHeader() {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Dimen.PaddingS)) {
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(SpacingXS))
        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DatasetOverviewCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusLarge),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingL)
        ) {
            Text(
                text = stringResource(R.string.home_dataset_card_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(SpacingM))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(label = stringResource(R.string.home_stat_matches), value = "380")
                StatItem(label = stringResource(R.string.home_stat_teams), value = "20")
                StatItem(label = stringResource(R.string.home_stat_providers), value = "3")
                StatItem(label = stringResource(R.string.home_stat_algorithms), value = "4")
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(SpacingXXS))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickActionsGrid(
    onNavigate: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SpacingM)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SpacingM)
        ) {
            ActionTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.SportsSoccer,
                title = stringResource(R.string.nav_matches),
                onClick = { onNavigate("matches") }
            )
            ActionTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Group,
                title = stringResource(R.string.nav_teams),
                onClick = { onNavigate("teams") }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SpacingM)
        ) {
            ActionTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Analytics,
                title = stringResource(R.string.nav_analytics),
                onClick = { onNavigate("analytics") }
            )
            ActionTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Calculate,
                title = stringResource(R.string.nav_prediction),
                onClick = { onNavigate("prediction") }
            )
        }
        ActionTile(
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Default.Speed,
            title = stringResource(R.string.nav_benchmark),
            onClick = { onNavigate("benchmark") }
        )
    }
}

@Composable
private fun ActionTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(RadiusMedium),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingL),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(SpacingXL)
            )
            Spacer(modifier = Modifier.width(SpacingM))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private val SpacingXXS = dev.anhquocs.truelab.core.ui.theme.SpacingXXS
