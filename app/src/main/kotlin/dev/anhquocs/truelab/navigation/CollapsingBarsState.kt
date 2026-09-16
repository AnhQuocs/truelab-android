package dev.anhquocs.truelab.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

private const val SCROLL_THRESHOLD = 80f
private const val ANIMATION_DURATION_MS = 300

@Stable
class CollapsingBarsState {
    var barsVisible by mutableStateOf(true)
        private set

    private var accumulatedDelta = 0f
    private var lastDirection = 0 // -1 down, 1 up

    fun show() {
        barsVisible = true
        accumulatedDelta = 0f
        lastDirection = 0
    }

    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val delta = available.y
            if (delta == 0f) return Offset.Zero

            val direction = if (delta > 0f) 1 else -1

            if (direction != lastDirection) {
                accumulatedDelta = 0f
                lastDirection = direction
            }

            accumulatedDelta += delta

            if (accumulatedDelta < -SCROLL_THRESHOLD && barsVisible) {
                barsVisible = false
                accumulatedDelta = 0f
            } else if (accumulatedDelta > SCROLL_THRESHOLD && !barsVisible) {
                barsVisible = true
                accumulatedDelta = 0f
            }

            return Offset.Zero
        }
    }
}

@Composable
fun rememberCollapsingBarsState(): CollapsingBarsState {
    return remember { CollapsingBarsState() }
}

@Stable
class BottomNavVisibilityState {
    var visible by mutableStateOf(true)
}

val LocalBottomNavVisibility = compositionLocalOf { BottomNavVisibilityState() }

@Composable
fun animateToolbarOffset(
    barsVisible: Boolean,
    collapseHeightPx: Float,
): Float {
    val offset by animateFloatAsState(
        // Nếu hiện: offset = 0
        // Nếu ẩn: offset = âm chiều cao của nó (để trượt lên trên)
        targetValue = if (barsVisible) 0f else -collapseHeightPx,

        // Thời gian trượt (thường là 300ms cho mượt)
        animationSpec = tween(durationMillis = 300),
        label = "toolbarOffset",
    )
    return offset // Trả về con số pixel đang thay đổi theo thời gian
}