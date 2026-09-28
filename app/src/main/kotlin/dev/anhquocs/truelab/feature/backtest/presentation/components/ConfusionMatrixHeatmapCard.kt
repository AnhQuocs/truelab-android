package dev.anhquocs.truelab.feature.backtest.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusMedium
import dev.anhquocs.truelab.core.ui.theme.RadiusSmall
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.s10
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.s16
import dev.anhquocs.truelab.core.ui.utils.semiBold
import dev.anhquocs.truelab.feature.backtest.presentation.model.ConfusionMatrixCellUi
import dev.anhquocs.truelab.feature.backtest.presentation.model.ConfusionMatrixUiRecord

@Composable
fun ConfusionMatrixHeatmapCard(
    matrix: ConfusionMatrixUiRecord,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusMedium),
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingS)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(RadiusMedium))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(Dimen.PaddingXSPlus),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Dimen.SizeSM)
                    )
                }

                Column {
                    Text(
                        text = stringResource(R.string.backtest_confusion_matrix_title),
                        style = MaterialTheme.typography.s16.bold(),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.backtest_matrix_predicted_label) + " (Col) vs " + stringResource(R.string.backtest_matrix_actual_label) + " (Row)",
                        style = MaterialTheme.typography.s10,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimen.PaddingM))

            // 3x3 Heatmap Grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimen.PaddingXS)
            ) {
                // Column Labels (Predicted)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingXS)
                ) {
                    // Top-left blank label corner
                    Box(modifier = Modifier.weight(1.1f)) {
                        Text(
                            text = "Act \\ Pred",
                            style = MaterialTheme.typography.s10.semiBold(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = Dimen.PaddingXS)
                        )
                    }

                    HeaderCell(label = stringResource(R.string.backtest_matrix_home), modifier = Modifier.weight(1f))
                    HeaderCell(label = stringResource(R.string.backtest_matrix_draw), modifier = Modifier.weight(1f))
                    HeaderCell(label = stringResource(R.string.backtest_matrix_away), modifier = Modifier.weight(1f))
                }

                // Row 1: Actual Home
                MatrixRow(
                    rowLabel = stringResource(R.string.backtest_matrix_home),
                    cell1 = matrix.homeAsHome,
                    cell2 = matrix.homeAsDraw,
                    cell3 = matrix.homeAsAway
                )

                // Row 2: Actual Draw
                MatrixRow(
                    rowLabel = stringResource(R.string.backtest_matrix_draw),
                    cell1 = matrix.drawAsHome,
                    cell2 = matrix.drawAsDraw,
                    cell3 = matrix.drawAsAway
                )

                // Row 3: Actual Away
                MatrixRow(
                    rowLabel = stringResource(R.string.backtest_matrix_away),
                    cell1 = matrix.awayAsHome,
                    cell2 = matrix.awayAsDraw,
                    cell3 = matrix.awayAsAway
                )
            }
        }
    }
}

@Composable
private fun HeaderCell(
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(vertical = Dimen.PaddingXXS),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.s12.semiBold(),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MatrixRow(
    rowLabel: String,
    cell1: ConfusionMatrixCellUi,
    cell2: ConfusionMatrixCellUi,
    cell3: ConfusionMatrixCellUi,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingXS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Row header
        Box(
            modifier = Modifier.weight(1.1f),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = rowLabel,
                style = MaterialTheme.typography.s12.semiBold(),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = Dimen.PaddingXS)
            )
        }

        MatrixHeatmapCell(cell = cell1, modifier = Modifier.weight(1f))
        MatrixHeatmapCell(cell = cell2, modifier = Modifier.weight(1f))
        MatrixHeatmapCell(cell = cell3, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MatrixHeatmapCell(
    cell: ConfusionMatrixCellUi,
    modifier: Modifier = Modifier
) {
    val baseColor = if (cell.isDiagonal) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }

    val alpha = if (cell.count > 0) {
        0.12f + (cell.intensityLevel * 0.45f).coerceIn(0f, 0.45f)
    } else {
        0.04f
    }

    val containerColor = baseColor.copy(alpha = alpha)
    val borderColor = if (cell.isDiagonal && cell.count > 0) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    } else {
        Color.Transparent
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(RadiusSmall))
            .background(containerColor)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(RadiusSmall))
            .padding(vertical = Dimen.PaddingS, horizontal = Dimen.PaddingXS),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = cell.count.toString(),
                style = MaterialTheme.typography.s14.bold(),
                color = if (cell.isDiagonal && cell.count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = cell.formattedPercentage,
                style = MaterialTheme.typography.s10,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
