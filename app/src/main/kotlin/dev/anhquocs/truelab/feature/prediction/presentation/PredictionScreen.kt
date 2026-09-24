package dev.anhquocs.truelab.feature.prediction.presentation

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusPill
import dev.anhquocs.truelab.core.ui.theme.SpacingS
import dev.anhquocs.truelab.core.ui.theme.SpacingXS
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.medium
import dev.anhquocs.truelab.core.ui.utils.s10
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s13
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.s16
import dev.anhquocs.truelab.core.ui.utils.s18
import dev.anhquocs.truelab.core.ui.utils.s20
import dev.anhquocs.truelab.core.ui.utils.semiBold
import dev.anhquocs.truelab.feature.prediction.presentation.components.PredictionFactorsCard
import dev.anhquocs.truelab.feature.prediction.presentation.components.ProbabilityResultsCard
import dev.anhquocs.truelab.feature.prediction.presentation.model.PredictionUiState
import dev.anhquocs.truelab.feature.prediction.presentation.viewmodel.PredictionViewModel

@Composable
fun PredictionScreen(
    modifier: Modifier = Modifier,
    viewModel: PredictionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            PredictionTopBar(onNavigateBack = onNavigateBack)
        },
        modifier = modifier
    ) { paddingValues ->
        when (val state = uiState) {
            is PredictionUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Dimen.SizeXL),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            is PredictionUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(Dimen.PaddingM),
                    contentAlignment = Alignment.Center
                ) {
                    PredictionFeedbackCard(
                        icon = Icons.Default.ErrorOutline,
                        iconTint = MaterialTheme.colorScheme.error,
                        title = stringResource(R.string.prediction_error_default),
                        message = state.message.asString(),
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        titleColor = MaterialTheme.colorScheme.error,
                        messageColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            is PredictionUiState.Empty -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(Dimen.PaddingM),
                    contentAlignment = Alignment.Center
                ) {
                    PredictionFeedbackCard(
                        icon = Icons.Default.Info,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = stringResource(R.string.prediction_title),
                        message = state.message.asString(),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        titleColor = MaterialTheme.colorScheme.primary,
                        messageColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            is PredictionUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(
                        start = Dimen.PaddingM,
                        end = Dimen.PaddingM,
                        top = Dimen.PaddingS,
                        bottom = Dimen.PaddingXXL
                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimen.PaddingM)
                ) {
                    // Match Selector
                    if (state.availableMatches.isNotEmpty()) {
                        item {
                            PredictionMatchSelector(
                                matches = state.availableMatches,
                                selectedMatch = state.selectedMatch,
                                onMatchSelected = { viewModel.onSelectMatch(it.id) }
                            )
                        }
                    }

                    // Selected Match Header Card
                    item {
                        SelectedMatchHeaderCard(match = state.selectedMatch)
                    }

                    // Win / Draw / Loss Probabilities Card
                    item {
                        ProbabilityResultsCard(
                            predictionResult = state.predictionResult,
                            homeWinPercent = state.homeWinPercent,
                            drawPercent = state.drawPercent,
                            awayWinPercent = state.awayWinPercent
                        )
                    }

                    // Prediction Factors Card
                    item {
                        PredictionFactorsCard(
                            match = state.selectedMatch,
                            predictionResult = state.predictionResult,
                            homeElo = state.homeElo,
                            awayElo = state.awayElo
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PredictionTopBar(
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimen.TopbarHeight)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimen.PaddingS, vertical = Dimen.PaddingS),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = "Back",
                    modifier = Modifier.size(Dimen.SizeML),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(Dimen.PaddingXS))
            Column {
                Text(
                    text = stringResource(R.string.prediction_title),
                    style = MaterialTheme.typography.s18.bold(),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.prediction_subtitle),
                    style = MaterialTheme.typography.s12,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PredictionMatchSelector(
    matches: List<Match>,
    selectedMatch: Match,
    onMatchSelected: (Match) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.prediction_select_match),
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
            matches.forEach { match ->
                val isSelected = match.id == selectedMatch.id
                val label = "${match.homeTeam.name} vs ${match.awayTeam.name}"
                FilterChip(
                    selected = isSelected,
                    onClick = { onMatchSelected(match) },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.s12
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
private fun SelectedMatchHeaderCard(
    match: Match
) {
    Card(
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
                .padding(Dimen.PaddingM),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.prediction_selected_match),
                style = MaterialTheme.typography.s10.medium(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(SpacingXS))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = match.homeTeam.name,
                    style = MaterialTheme.typography.s16.bold(),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(RadiusPill))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = Dimen.PaddingS, vertical = Dimen.PaddingXXS)
                ) {
                    Text(
                        text = "VS",
                        style = MaterialTheme.typography.s10.bold(),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    text = match.awayTeam.name,
                    style = MaterialTheme.typography.s16.bold(),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun PredictionFeedbackCard(
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
                textAlign = TextAlign.Center
            )
        }
    }
}
