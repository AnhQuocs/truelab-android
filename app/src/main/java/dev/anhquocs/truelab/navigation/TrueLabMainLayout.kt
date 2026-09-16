package dev.anhquocs.truelab.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
    val statusBarHeightPx = WindowInsets.statusBars.getTop(density).toFloat()
    val contentHeaderHeightPx = with(density) { headerHeight.toPx() }
    val fullHeaderHeightPx = statusBarHeightPx + contentHeaderHeightPx

    // Quy tắc Threshold: SCROLL_THRESHOLD = headerHeight (dp) + 20f
    val scrollThresholdPx = headerHeight.value + 20f

    val bottomNavController = LocalBottomNavVisibility.current
    val barsState = rememberCollapsingBarsState(scrollThreshold = scrollThresholdPx)

    val headerOffset = animateToolbarOffset(barsState.barsVisible, fullHeaderHeightPx)

    LaunchedEffect(barsState.barsVisible) {
        bottomNavController.visible = barsState.barsVisible
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Content container
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(barsState.nestedScrollConnection)
                .offset {
                    val visibleHeaderHeight = (fullHeaderHeightPx + headerOffset).coerceAtLeast(0f)
                    IntOffset(0, visibleHeaderHeight.toInt())
                }
        ) {
            content(Modifier)
        }

        // Animated Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, headerOffset.toInt()) }
                .background(MaterialTheme.colorScheme.background) // Nền che nội dung cuộn bên dưới
        ) {
            // Spacer này chiếm chiều cao bằng đúng Status Bar, giúp Status Bar "có màu"
            Spacer(modifier = Modifier.fillMaxWidth().height(with(density) { statusBarHeightPx.toDp() }))
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                header()
            }
        }
    }
}
