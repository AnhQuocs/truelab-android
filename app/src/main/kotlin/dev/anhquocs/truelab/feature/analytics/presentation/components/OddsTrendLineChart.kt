package dev.anhquocs.truelab.feature.analytics.presentation.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusMedium
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.medium
import dev.anhquocs.truelab.core.ui.utils.s10
import java.util.Locale

@Composable
fun OddsTrendLineChart(
    rawOdds: List<Double>,
    smaOdds: List<Double>,
    windowSize: Int,
    modifier: Modifier = Modifier
) {
    if (rawOdds.isEmpty()) return

    val rawColor = MaterialTheme.colorScheme.tertiary
    val smaColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface

    // Determine min and max across both series with padding
    val allValues = rawOdds + smaOdds
    val rawMin = allValues.minOrNull() ?: 1.0
    val rawMax = allValues.maxOrNull() ?: 3.0
    val range = (rawMax - rawMin).coerceAtLeast(0.20)
    val yMin = (rawMin - range * 0.15).coerceAtLeast(1.0)
    val yMax = rawMax + range * 0.15

    val yMid = (yMin + yMax) / 2.0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMedium))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(Dimen.PaddingS)
    ) {
        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Dimen.PaddingXS),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(rawColor)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Raw Odds",
                style = MaterialTheme.typography.s10.medium(),
                color = onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(Dimen.PaddingM))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(smaColor)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "SMA($windowSize)",
                style = MaterialTheme.typography.s10.bold(),
                color = smaColor
            )
        }

        // Chart with Y-Axis Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            // Y-Axis Labels
            Column(
                modifier = Modifier
                    .height(130.dp)
                    .padding(end = Dimen.PaddingXS),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = String.format(Locale.US, "%.2f", yMax),
                    style = MaterialTheme.typography.s10,
                    color = onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = String.format(Locale.US, "%.2f", yMid),
                    style = MaterialTheme.typography.s10,
                    color = onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = String.format(Locale.US, "%.2f", yMin),
                    style = MaterialTheme.typography.s10,
                    color = onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // Canvas drawing area
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp)
            ) {
                val width = size.width
                val height = size.height
                val n = rawOdds.size

                val yRange = (yMax - yMin).coerceAtLeast(0.01)

                fun getY(value: Double): Float {
                    val normalized = ((value - yMin) / yRange).toFloat()
                    return height - (normalized * height).coerceIn(0f, height)
                }

                fun getX(index: Int): Float {
                    return if (n <= 1) width / 2f else (index.toFloat() / (n - 1)) * width
                }

                // 1. Draw horizontal gridlines (Top, Mid, Bottom)
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, 0f),
                    end = Offset(width, 0f),
                    strokeWidth = 1f,
                    pathEffect = dashEffect
                )
                drawLine(
                    color = gridColor,
                    start = Offset(0f, height / 2f),
                    end = Offset(width, height / 2f),
                    strokeWidth = 1f,
                    pathEffect = dashEffect
                )
                drawLine(
                    color = gridColor,
                    start = Offset(0f, height),
                    end = Offset(width, height),
                    strokeWidth = 1f,
                    pathEffect = dashEffect
                )

                if (n == 1) {
                    val cx = width / 2f
                    val cy = getY(rawOdds[0])
                    drawCircle(color = rawColor, radius = 5.dp.toPx(), center = Offset(cx, cy))
                    return@Canvas
                }

                // 2. Draw Raw Odds Series (dashed / thinner line with dots)
                val rawPath = Path().apply {
                    moveTo(getX(0), getY(rawOdds[0]))
                    for (i in 1 until n) {
                        lineTo(getX(i), getY(rawOdds[i]))
                    }
                }

                drawPath(
                    path = rawPath,
                    color = rawColor.copy(alpha = 0.75f),
                    style = Stroke(
                        width = 1.8.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                    )
                )

                // Draw dots on raw points
                for (i in 0 until n) {
                    drawCircle(
                        color = rawColor,
                        radius = 2.5.dp.toPx(),
                        center = Offset(getX(i), getY(rawOdds[i]))
                    )
                }

                // 3. Draw SMA Series (Solid prominent line with gradient fill)
                if (smaOdds.isNotEmpty()) {
                    val smaStartIndex = (windowSize - 1).coerceAtMost(n - 1)
                    val smaPath = Path()
                    val smaFillPath = Path()

                    val startX = getX(smaStartIndex)
                    val startY = getY(smaOdds[0])

                    smaPath.moveTo(startX, startY)
                    smaFillPath.moveTo(startX, height)
                    smaFillPath.lineTo(startX, startY)

                    for (k in 1 until smaOdds.size) {
                        val pointIndex = (smaStartIndex + k).coerceAtMost(n - 1)
                        val px = getX(pointIndex)
                        val py = getY(smaOdds[k])
                        smaPath.lineTo(px, py)
                        smaFillPath.lineTo(px, py)
                    }

                    val lastIndex = (smaStartIndex + smaOdds.size - 1).coerceAtMost(n - 1)
                    smaFillPath.lineTo(getX(lastIndex), height)
                    smaFillPath.close()

                    // Gradient under SMA curve
                    drawPath(
                        path = smaFillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                smaColor.copy(alpha = 0.25f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = height
                        )
                    )

                    // SMA Stroke
                    drawPath(
                        path = smaPath,
                        color = smaColor,
                        style = Stroke(
                            width = 2.8.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Draw marker dot on latest SMA point
                    val latestX = getX(lastIndex)
                    val latestY = getY(smaOdds.last())
                    drawCircle(
                        color = surfaceColor,
                        radius = 4.dp.toPx(),
                        center = Offset(latestX, latestY)
                    )
                    drawCircle(
                        color = smaColor,
                        radius = 3.dp.toPx(),
                        center = Offset(latestX, latestY)
                    )
                }
            }
        }

        // X-Axis Timeline Indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimen.PaddingXXS, start = 36.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "T-0 (Opening)",
                style = MaterialTheme.typography.s10,
                color = onSurfaceVariant.copy(alpha = 0.6f)
            )
            Text(
                text = "T-${rawOdds.size - 1} (Current)",
                style = MaterialTheme.typography.s10,
                color = onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
