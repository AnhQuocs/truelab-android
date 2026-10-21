package dev.anhquocs.truelab.core.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.anhquocs.truelab.core.ui.theme.Dimen

/**
 * Reusable Team Logo component utilizing Coil Image Loading.
 * High-performance: uses AsyncImage without subcomposition overhead for smooth 60/120fps scrolling.
 */
@Composable
fun TeamLogo(
    logoUrl: String?,
    teamName: String,
    modifier: Modifier = Modifier,
    size: Dp = Dimen.SizeMD
) {
    val fallbackPainter = rememberVectorPainter(Icons.Default.SportsSoccer)
    if (!logoUrl.isNullOrBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(logoUrl)
                .crossfade(true)
                .build(),
            contentDescription = teamName,
            contentScale = ContentScale.Fit,
            modifier = modifier.size(size),
            placeholder = fallbackPainter,
            error = fallbackPainter
        )
    } else {
        Icon(
            painter = fallbackPainter,
            contentDescription = teamName,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = modifier.size(size)
        )
    }
}

/**
 * Reusable Competition/League Logo component.
 * High-performance: displays logo image directly via AsyncImage without frame or subcomposition.
 */
@Composable
fun CompetitionLogo(
    logoUrl: String?,
    leagueName: String?,
    modifier: Modifier = Modifier,
    size: Dp = Dimen.SizeSM
) {
    val fallbackPainter = rememberVectorPainter(Icons.Default.SportsSoccer)
    if (!logoUrl.isNullOrBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(logoUrl)
                .crossfade(true)
                .build(),
            contentDescription = leagueName,
            contentScale = ContentScale.Fit,
            modifier = modifier.size(size),
            placeholder = fallbackPainter,
            error = fallbackPainter
        )
    } else {
        Icon(
            painter = fallbackPainter,
            contentDescription = leagueName,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.size(size)
        )
    }
}
