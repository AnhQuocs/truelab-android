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
