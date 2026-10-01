package dev.anhquocs.truelab.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import dev.anhquocs.truelab.core.ui.utils.medium
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s14

/**
 * Reusable Team Logo component utilizing Coil Image Loading with robust fallback mechanisms.
 */
@Composable
fun TeamLogo(
    logoUrl: String?,
    teamName: String,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    val initial = teamName.take(2).uppercase().ifBlank { "?" }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        if (!logoUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(logoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = teamName,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                loading = {
                    Box(
                        modifier = Modifier
                            .size(size)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            style = if (size >= 32.dp) MaterialTheme.typography.s14.medium() else MaterialTheme.typography.s12.medium(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                error = {
                    Box(
                        modifier = Modifier
                            .size(size)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            style = if (size >= 32.dp) MaterialTheme.typography.s14.medium() else MaterialTheme.typography.s12.medium(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        } else {
            Text(
                text = initial,
                style = if (size >= 32.dp) MaterialTheme.typography.s14.medium() else MaterialTheme.typography.s12.medium(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Reusable Competition/League Logo component.
 */
@Composable
fun CompetitionLogo(
    logoUrl: String?,
    leagueName: String?,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        if (!logoUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(logoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = leagueName,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(4.dp)),
                loading = {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = leagueName,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(size * 0.75f)
                    )
                },
                error = {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = leagueName,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(size * 0.75f)
                    )
                }
            )
        } else {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = leagueName,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size * 0.75f)
            )
        }
    }
}
