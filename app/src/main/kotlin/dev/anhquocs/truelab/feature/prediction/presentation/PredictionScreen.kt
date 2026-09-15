package dev.anhquocs.truelab.feature.prediction.presentation

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusMedium
import dev.anhquocs.truelab.core.ui.theme.SpacingL
import dev.anhquocs.truelab.core.ui.theme.SpacingM
import dev.anhquocs.truelab.core.ui.theme.SpacingS
import dev.anhquocs.truelab.core.ui.theme.SpacingXL
import dev.anhquocs.truelab.core.ui.theme.SpacingXS

@Composable
fun PredictionScreen(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = SpacingL),
        verticalArrangement = Arrangement.spacedBy(SpacingM),
        contentPadding = PaddingValues(bottom = SpacingXL)
    ) {
        item {
            Spacer(modifier = Modifier.height(SpacingS))
            Text(
                text = stringResource(R.string.prediction_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            MatchSelectionCard()
        }

        item {
            ProbabilityResultsCard()
        }

        item {
            ModelWeightsCard()
        }

        item {
            Button(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dev.anhquocs.truelab.core.ui.theme.ButtonHeightMedium),
                shape = RoundedCornerShape(RadiusMedium)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoGraph,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.padding(horizontal = SpacingXS))
                Text(
                    text = stringResource(R.string.prediction_calculate_btn),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun MatchSelectionCard() {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusLarge)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingL),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.prediction_select_match),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(SpacingS))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Man City",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "VS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Liverpool",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ProbabilityResultsCard() {
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
                .padding(SpacingL),
            verticalArrangement = Arrangement.spacedBy(SpacingM)
        ) {
            Text(
                text = "Win/Draw/Loss Probabilities",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ProbabilityBarItem(
                label = stringResource(R.string.prediction_home_win),
                percentage = 54,
                color = MaterialTheme.colorScheme.primary
            )
            ProbabilityBarItem(
                label = stringResource(R.string.prediction_draw),
                percentage = 24,
                color = Color(0xFFF59E0B)
            )
            ProbabilityBarItem(
                label = stringResource(R.string.prediction_away_win),
                percentage = 22,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun ProbabilityBarItem(
    label: String,
    percentage: Int,
    color: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(SpacingXS))
        LinearProgressIndicator(
            progress = { percentage / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(SpacingS),
            color = color,
            trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            drawStopIndicator = {}
        )
    }
}

@Composable
private fun ModelWeightsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusLarge),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingL)
        ) {
            Text(
                text = stringResource(R.string.prediction_weighted_model),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(SpacingS))
            Text(
                text = "• Elo Diff: +60 pts (Weight: 40%)\n• Form Index: 0.82 vs 0.74 (Weight: 35%)\n• Home Advantage Boost: +15% (Weight: 25%)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
