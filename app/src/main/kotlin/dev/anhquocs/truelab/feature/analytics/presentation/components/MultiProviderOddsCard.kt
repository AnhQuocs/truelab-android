package dev.anhquocs.truelab.feature.analytics.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusPill
import dev.anhquocs.truelab.core.ui.theme.RadiusSmall
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.medium
import dev.anhquocs.truelab.core.ui.utils.s10
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.semiBold

@Composable
fun MultiProviderOddsCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusLarge),
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
                verticalAlignment = Alignment.Top, // Đảm bảo căn trên cùng, đề phòng lỗi lệch do xuống dòng
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = Dimen.PaddingS)) { // Cấp weight cho Tiêu đề để không lấn át Box bên phải
                    Text(
                        text = "2. " + stringResource(R.string.analytics_odds_comparison),
                        style = MaterialTheme.typography.s14.semiBold(),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Mẫu trận: Arsenal vs Chelsea (#M-38012)",
                        style = MaterialTheme.typography.s12,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(RadiusPill))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = Dimen.PaddingS, vertical = Dimen.PaddingXXS)
                ) {
                    Text(
                        text = "4 Nhà cung cấp",
                        style = MaterialTheme.typography.s10.bold(),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1 // Chống rớt dòng dọc
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimen.PaddingM))

            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(RadiusSmall))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(vertical = Dimen.PaddingXS, horizontal = Dimen.PaddingS),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Nhà cung cấp", style = MaterialTheme.typography.s10.bold(), modifier = Modifier.weight(1.3f))
                Text(text = "1 (Home)", style = MaterialTheme.typography.s10.bold(), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text(text = "X (Draw)", style = MaterialTheme.typography.s10.bold(), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text(text = "2 (Away)", style = MaterialTheme.typography.s10.bold(), modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }

            Spacer(modifier = Modifier.height(Dimen.PaddingXXS))

            OddsTableRow(provider = "Bet365", home = 1.85, draw = 3.60, away = 4.20)
            OddsTableRow(provider = "Pinnacle", home = 1.88, draw = 3.65, away = 4.10)
            OddsTableRow(provider = "Bwin", home = 1.83, draw = 3.50, away = 4.30)
            OddsTableRow(provider = "William Hill", home = 1.85, draw = 3.55, away = 4.25)

            Spacer(modifier = Modifier.height(Dimen.PaddingS))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(Dimen.PaddingS))

            // Variance & Spread Metric Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Odds TB: 1.85 | 3.58 | 4.21",
                    style = MaterialTheme.typography.s12.semiBold(),
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Độ biến động: 0.038 (Thấp)",
                    style = MaterialTheme.typography.s10.medium(),
                    color = Color(0xFF10B981)
                )
            }
        }
    }
}

@Composable
private fun OddsTableRow(
    provider: String,
    home: Double,
    draw: Double,
    away: Double
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimen.PaddingXS, horizontal = Dimen.PaddingS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = provider,
            style = MaterialTheme.typography.s12.medium(),
            modifier = Modifier.weight(1.3f)
        )
        Text(
            text = "%.2f".format(home),
            style = MaterialTheme.typography.s12.bold(),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "%.2f".format(draw),
            style = MaterialTheme.typography.s12,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Text(
            text = "%.2f".format(away),
            style = MaterialTheme.typography.s12.bold(),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}
