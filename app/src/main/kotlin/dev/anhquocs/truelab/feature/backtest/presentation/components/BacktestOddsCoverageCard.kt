package dev.anhquocs.truelab.feature.backtest.presentation.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusMedium
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.medium
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.s16
import dev.anhquocs.truelab.core.ui.utils.s20
import dev.anhquocs.truelab.core.ui.utils.semiBold
import dev.anhquocs.truelab.feature.backtest.presentation.model.OddsCoverageUiRecord

@Composable
fun BacktestOddsCoverageCard(
    oddsCoverage: OddsCoverageUiRecord,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusLarge),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimen.Elevation1)
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingS)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(RadiusMedium))
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f))
                            .padding(Dimen.PaddingXSPlus),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueryStats,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(Dimen.SizeSM)
                        )
                    }

                    Text(
                        text = stringResource(R.string.backtest_odds_coverage_title),
                        style = MaterialTheme.typography.s16.bold(),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = oddsCoverage.formattedCoverage,
                    style = MaterialTheme.typography.s20.bold(),
                    color = if (oddsCoverage.coveragePercent >= 70.0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    }
                )
            }

            Spacer(modifier = Modifier.height(Dimen.PaddingM))

            // Breakdown Rows
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimen.PaddingXS)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingS)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(
                            R.string.backtest_odds_usable_count,
                            oddsCoverage.matchesWithOdds,
                            oddsCoverage.totalMatches,
                            oddsCoverage.formattedCoverage
                        ),
                        style = MaterialTheme.typography.s12.medium(),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (oddsCoverage.matchesWithoutOdds > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingS)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(
                                R.string.backtest_odds_missing_count,
                                oddsCoverage.matchesWithoutOdds
                            ),
                            style = MaterialTheme.typography.s12.medium(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
