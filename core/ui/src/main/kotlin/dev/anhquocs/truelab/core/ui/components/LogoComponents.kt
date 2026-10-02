package dev.anhquocs.truelab.core.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import dev.anhquocs.truelab.core.ui.theme.Dimen

/**
 * Reusable Team Logo component utilizing Coil Image Loading.
 * Minimalist design: renders direct team crest without bulky containers.
 */
@Composable
fun TeamLogo(
    logoUrl: String?,
    teamName: String,
    modifier: Modifier = Modifier,
    size: Dp = Dimen.SizeMD
) {
    if (!logoUrl.isNullOrBlank()) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(logoUrl)
                .crossfade(true)
                .build(),
            contentDescription = teamName,
            contentScale = ContentScale.Fit,
            modifier = modifier.size(size),
            loading = {
                Icon(
                    imageVector = Icons.Default.SportsSoccer,
                    contentDescription = teamName,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(size)
                )
            },
            error = {
                Icon(
                    imageVector = Icons.Default.SportsSoccer,
                    contentDescription = teamName,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(size)
                )
            }
        )
    } else {
        Icon(
            imageVector = Icons.Default.SportsSoccer,
            contentDescription = teamName,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = modifier.size(size)
        )
    }
}

/**
 * Reusable Competition/League Logo component.
 * Minimalist design: displays logo image directly without container background or frame.
 */
@Composable
fun CompetitionLogo(
    logoUrl: String?,
    leagueName: String?,
    modifier: Modifier = Modifier,
    size: Dp = Dimen.SizeSM
) {
    if (!logoUrl.isNullOrBlank()) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(logoUrl)
                .crossfade(true)
                .build(),
            contentDescription = leagueName,
            contentScale = ContentScale.Fit,
            modifier = modifier.size(size),
            loading = {
                Icon(
                    imageVector = Icons.Default.SportsSoccer,
                    contentDescription = leagueName,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(size)
                )
            },
            error = {
                Icon(
                    imageVector = Icons.Default.SportsSoccer,
                    contentDescription = leagueName,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(size)
                )
            }
        )
    } else {
        Icon(
            imageVector = Icons.Default.SportsSoccer,
            contentDescription = leagueName,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.size(size)
        )
    }
}
