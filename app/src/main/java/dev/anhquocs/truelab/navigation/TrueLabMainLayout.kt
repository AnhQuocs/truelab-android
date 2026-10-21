package dev.anhquocs.truelab.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Composable
fun TrueLabMainLayout(
    modifier: Modifier = Modifier,
    headerHeight: Dp = 60.dp,
    header: @Composable () -> Unit,
    content: @Composable ColumnScope.(Modifier) -> Unit
) {
    val density = LocalDensity.current
    val fallbackHeaderHeightPx = with(density) { headerHeight.roundToPx() }
    var measuredHeaderHeightPx by remember { mutableIntStateOf(0) }
    val contentHeaderHeightPx =
        measuredHeaderHeightPx.takeIf { it > 0 } ?: fallbackHeaderHeightPx

    // Quy tắc Threshold: SCROLL_THRESHOLD = headerHeight (dp) + 20f
    val scrollThresholdPx = contentHeaderHeightPx.toFloat() + with(density) { 20.dp.toPx() }

    val bottomNavController = LocalBottomNavVisibility.current
    val barsState = rememberCollapsingBarsState(scrollThreshold = scrollThresholdPx)

    val headerOffset = animateToolbarOffset(barsState.barsVisible, contentHeaderHeightPx.toFloat())

    LaunchedEffect(barsState.barsVisible) {
        bottomNavController.visible = barsState.barsVisible
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .displayCutoutPadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Content container
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(barsState.nestedScrollConnection)
                .offset {
                    val visibleHeaderHeight = (contentHeaderHeightPx + headerOffset).coerceAtLeast(0f)
                    IntOffset(0, visibleHeaderHeight.toInt())
                }
        ) {
            content(Modifier)
        }

        // Animated Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { measuredHeaderHeightPx = it.height }
                .offset { IntOffset(0, headerOffset.toInt()) }
                .background(MaterialTheme.colorScheme.background) // Nền che nội dung cuộn bên dưới
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                header()
            }
        }
    }
}
