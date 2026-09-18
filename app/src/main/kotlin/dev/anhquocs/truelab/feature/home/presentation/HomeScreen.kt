package dev.anhquocs.truelab.feature.home.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.SpacingL
import dev.anhquocs.truelab.core.ui.theme.SpacingXS
import dev.anhquocs.truelab.core.ui.theme.SpacingXXL
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.s20
import dev.anhquocs.truelab.feature.home.presentation.components.AlgorithmProgressCard
import dev.anhquocs.truelab.feature.home.presentation.components.DatasetOverviewCard
import dev.anhquocs.truelab.feature.home.presentation.components.QuickActionsGrid
import dev.anhquocs.truelab.feature.home.presentation.components.RecentPredictionsCard
import dev.anhquocs.truelab.navigation.TrueLabMainLayout

private val TOP_BAR_HEIGHT = 70.dp

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigate: (String) -> Unit = {},
    onSetting: () -> Unit
) {
    TrueLabMainLayout(
        modifier = modifier,
        headerHeight = TOP_BAR_HEIGHT,
        header = { HomeHeader(onSetting) }
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

            Spacer(modifier = Modifier.height(SpacingXXL))
        }
    }
}

@Composable
private fun HomeHeader(onSetting: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimen.PaddingS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = Dimen.PaddingS)
        ) {
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.s20.bold(),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(SpacingXS))
            Text(
                text = stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.s14,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Setting",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(end = Dimen.PaddingSM)
                .clip(RoundedCornerShape(8.dp))
                .size(Dimen.SizeML)
                .clickable { onSetting() }
        )
    }
}
