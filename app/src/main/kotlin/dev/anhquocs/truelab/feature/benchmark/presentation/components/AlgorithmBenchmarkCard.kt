package dev.anhquocs.truelab.feature.benchmark.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.benchmark.model.ComparisonBenchmarkResult
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusMedium
import dev.anhquocs.truelab.core.ui.theme.RadiusPill
import dev.anhquocs.truelab.core.ui.theme.SpacingL
import dev.anhquocs.truelab.core.ui.theme.SpacingM
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.s16
import dev.anhquocs.truelab.core.ui.utils.semiBold
import java.util.Locale

/**
 * Thẻ hiển thị so sánh hiệu năng giữa hai thuật toán cùng nhóm (Tìm kiếm hoặc Sắp xếp).
 *
 * @param title Tiêu đề nhóm thuật toán.
 * @param comparison Kết quả so sánh từ Domain (null nếu chưa chạy benchmark).
 * @param algo1Name Tên thuật toán 1.
 * @param algo1Complexity Độ phức tạp lý thuyết thuật toán 1 (metadata của Presentation).
 * @param algo2Name Tên thuật toán 2.
 * @param algo2Complexity Độ phức tạp lý thuyết thuật toán 2 (metadata của Presentation).
 * @param isRunning Đang trong quá trình chạy đo lường hay không.
 */
@Composable
fun AlgorithmBenchmarkCard(
    title: String,
    comparison: ComparisonBenchmarkResult?,
    algo1Name: String,
    algo1Complexity: String,
    algo2Name: String,
    algo2Complexity: String,
    isRunning: Boolean,
    modifier: Modifier = Modifier
) {
    val time1 = when {
        isRunning -> "..."
        comparison != null -> String.format(Locale.US, "%.3f ms", comparison.algorithmA.executionTimeMs)
        else -> "—"
    }

    val time2 = when {
        isRunning -> "..."
        comparison != null -> String.format(Locale.US, "%.3f ms", comparison.algorithmB.executionTimeMs)
        else -> "—"
    }

    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusLarge)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingL),
            verticalArrangement = Arrangement.spacedBy(SpacingM)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.s16.bold(),
                color = MaterialTheme.colorScheme.onSurface
            )

            AlgoResultRow(name = algo1Name, time = time1, complexity = algo1Complexity)
            AlgoResultRow(name = algo2Name, time = time2, complexity = algo2Complexity)

            if (comparison != null && !isRunning) {
                SpeedupBanner(comparison = comparison)
            }
        }
    }
}

@Composable
private fun AlgoResultRow(
    name: String,
    time: String,
    complexity: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusMedium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingM),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.s14.semiBold()
                )
                Text(
                    text = time,
                    style = MaterialTheme.typography.s16.bold(),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            SuggestionChip(
                onClick = {},
                label = {
                    Text(
                        text = complexity,
                        style = MaterialTheme.typography.s12.bold()
                    )
                }
            )
        }
    }
}

@Composable
private fun SpeedupBanner(
    comparison: ComparisonBenchmarkResult
) {
    val isFaster = comparison.fasterAlgorithm != "Equal"
    val text = if (isFaster) {
        stringResource(
            R.string.benchmark_speedup_format,
            comparison.fasterAlgorithm,
            comparison.speedupFactor
        )
    } else {
        stringResource(R.string.benchmark_equal_speed)
    }

    val bannerBg = if (isFaster) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isFaster) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusPill))
            .background(bannerBg)
            .padding(horizontal = Dimen.PaddingM, vertical = Dimen.PaddingS),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingXS)
        ) {
            if (isFaster) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(Dimen.SizeS)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.s12.bold(),
                color = textColor
            )
        }
    }
}
