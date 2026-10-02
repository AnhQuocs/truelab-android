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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextAlign
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusMedium
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.medium
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.s16
import dev.anhquocs.truelab.core.ui.utils.s18
import dev.anhquocs.truelab.core.ui.utils.semiBold

@Composable
fun BacktestIdleCard(
    availableFtMatchesCount: Int,
    onStartEvaluation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusLarge),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimen.Elevation2)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimen.PaddingL),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(RadiusMedium))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(Dimen.PaddingM),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Assessment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Dimen.SizeL)
                )

            }

            Spacer(modifier = Modifier.height(Dimen.PaddingM))

            Text(
                text = stringResource(R.string.backtest_idle_title),
                style = MaterialTheme.typography.s18.bold(),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Dimen.PaddingS))

            Text(
                text = stringResource(R.string.backtest_idle_desc),
                style = MaterialTheme.typography.s14.medium(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Dimen.PaddingM))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingXS)
            ) {
                Icon(
                    imageVector = Icons.Default.SportsSoccer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Dimen.SizeXS)
                )
                Text(
                    text = stringResource(R.string.backtest_ft_matches_count, availableFtMatchesCount),
                    style = MaterialTheme.typography.s14.semiBold(),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(Dimen.PaddingL))

            Button(
                onClick = onStartEvaluation,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(RadiusMedium),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(Dimen.SizeSM)
                )
                Spacer(modifier = Modifier.width(Dimen.PaddingXS))
                Text(
                    text = stringResource(R.string.backtest_start_btn),
                    style = MaterialTheme.typography.s16.bold()
                )
            }
        }
    }
}
