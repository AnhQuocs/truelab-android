package dev.anhquocs.truelab.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import dev.anhquocs.truelab.core.ui.theme.TrueLabShapes

private val DarkColorScheme = darkColorScheme(
    primary = RoyalIndigoDarkPrimary,
    onPrimary = RoyalIndigoDarkOnPrimary,
    primaryContainer = RoyalIndigoDarkPrimaryContainer,
    onPrimaryContainer = RoyalIndigoDarkOnPrimaryContainer,
    secondary = VividCyanDarkSecondary,
    onSecondary = VividCyanDarkOnSecondary,
    secondaryContainer = VividCyanDarkSecondaryContainer,
    onSecondaryContainer = VividCyanDarkOnSecondaryContainer,
    tertiary = SoftAmberDarkTertiary,
    onTertiary = SoftAmberDarkOnTertiary,
    tertiaryContainer = SoftAmberDarkTertiaryContainer,
    onTertiaryContainer = SoftAmberDarkOnTertiaryContainer,
    background = SlateDarkBackground,
    onBackground = SlateDarkOnBackground,
    surface = SlateDarkSurface,
    onSurface = SlateDarkOnSurface,
    surfaceVariant = SlateDarkSurfaceVariant,
    onSurfaceVariant = SlateDarkOnSurfaceVariant,
    outline = SlateDarkOutline,
    error = StatusError,
    onError = StatusOnError
)

private val LightColorScheme = lightColorScheme(
    primary = RoyalIndigoLightPrimary,
    onPrimary = RoyalIndigoLightOnPrimary,
    primaryContainer = RoyalIndigoLightPrimaryContainer,
    onPrimaryContainer = RoyalIndigoLightOnPrimaryContainer,
    secondary = VividCyanLightSecondary,
    onSecondary = VividCyanLightOnSecondary,
    secondaryContainer = VividCyanLightSecondaryContainer,
    onSecondaryContainer = VividCyanLightOnSecondaryContainer,
    tertiary = SoftAmberLightTertiary,
    onTertiary = SoftAmberLightOnTertiary,
    tertiaryContainer = SoftAmberLightTertiaryContainer,
    onTertiaryContainer = SoftAmberLightOnTertiaryContainer,
    background = SlateLightBackground,
    onBackground = SlateLightOnBackground,
    surface = SlateLightSurface,
    onSurface = SlateLightOnSurface,
    surfaceVariant = SlateLightSurfaceVariant,
    onSurfaceVariant = SlateLightOnSurfaceVariant,
    outline = SlateLightOutline,
    error = StatusError,
    onError = StatusOnError
)

@Composable
fun TrueLabTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set false to preserve TrueLab Brand Identity
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = TrueLabShapes,
        typography = Typography,
        content = content
    )
}
