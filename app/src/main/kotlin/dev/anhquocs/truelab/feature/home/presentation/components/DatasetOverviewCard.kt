package dev.anhquocs.truelab.feature.home.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusMedium
import dev.anhquocs.truelab.core.ui.theme.SpacingXXS
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.s18
import dev.anhquocs.truelab.core.ui.utils.semiBold
import dev.anhquocs.truelab.feature.home.presentation.model.HomeUiState
import java.util.Locale

@Composable
fun DatasetOverviewCard(
    uiState: HomeUiState,
    modifier: Modifier = Modifier
) {
    val (matchesText, teamsText, oddsText, lastUpdatedText) = when (uiState) {
        is HomeUiState.Loading -> {
            Quadruple("—", "—", "—", stringResource(R.string.home_dataset_last_updated, "—"))
        }
        is HomeUiState.Error -> {
            Quadruple("0", "0", "0", stringResource(R.string.home_dataset_last_updated, "—"))
        }
        is HomeUiState.Success -> {
            val meta = uiState.metadata
            val m = if (meta.totalMatches > 0) String.format(Locale.getDefault(), "%,d", meta.totalMatches) else "0"
            val t = if (meta.totalTeams > 0) String.format(Locale.getDefault(), "%,d", meta.totalTeams) else "0"
            val o = if (meta.totalOddsRecords > 0) String.format(Locale.getDefault(), "%,d", meta.totalOddsRecords) else "0"
            val updated = stringResource(R.string.home_dataset_last_updated, uiState.formattedLastSync)
            Quadruple(m, t, o, updated)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusMedium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimen.PaddingXXS)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimen.PaddingM)
        ) {
            Text(
                text = stringResource(R.string.home_dataset_section_title),
                style = MaterialTheme.typography.s14.semiBold(),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(Dimen.PaddingM))

            // 4 Metrics Grid Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatMetricItem(
                    label = stringResource(R.string.home_stat_matches),
                    value = matchesText,
                    modifier = Modifier.weight(1f)
                )
                StatMetricItem(
                    label = stringResource(R.string.home_stat_teams),
                    value = teamsText,
                    modifier = Modifier.weight(1f)
                )
                StatMetricItem(
                    label = stringResource(R.string.home_stat_providers),
                    value = oddsText,
                    modifier = Modifier.weight(1f)
                )
                StatMetricItem(
                    label = stringResource(R.string.home_stat_algorithms),
                    value = "11",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(Dimen.PaddingM))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            Spacer(modifier = Modifier.height(Dimen.PaddingS))

            Text(
                text = lastUpdatedText,
                style = MaterialTheme.typography.s12,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun StatMetricItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.s18.bold(),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(SpacingXXS))
        Text(
            text = label,
            style = MaterialTheme.typography.s12,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
