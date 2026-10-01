package dev.anhquocs.truelab.feature.prediction.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.prediction.model.PredictionResult
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.s12

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictionBottomSheet(
    selectedMatch: Match,
    predictionResult: PredictionResult?,
    leagueName: String?,
    leagueLogo: String? = null,
    selectedDate: String? = null,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val displayStatus = resolveDisplayStatus(selectedMatch, selectedDate)
    val isLive = displayStatus == DisplayMatchStatus.LIVE

    val effectiveLeagueName = selectedMatch.leagueName ?: leagueName
    val effectiveLeagueLogo = selectedMatch.leagueLogo ?: leagueLogo

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimen.PaddingM),
            verticalArrangement = Arrangement.spacedBy(Dimen.PaddingS)
        ) {
            // 1. Sheet label
            item {
                Text(
                    text = stringResource(R.string.prediction_sheet_title),
                    style = MaterialTheme.typography.s12.bold(),
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.2.sp
                )
            }

            // 2. Matchup Overview Card
            item {
                MatchupOverviewCard(
                    match = selectedMatch,
                    leagueName = effectiveLeagueName,
                    leagueLogo = effectiveLeagueLogo,
                    isLive = isLive
                )
            }

            // 3. Hero Prediction Probability Summary
            if (predictionResult != null) {
                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    )
                    PredictionHeroProbabilityCard(predictionResult = predictionResult)
                }

                // 4. Evidence & Signals Section
                val evidence = predictionResult.evidence
                if (evidence != null) {
                    item {
                        PredictionEvidenceSection(
                            evidence = evidence,
                            selectedMatch = selectedMatch
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
