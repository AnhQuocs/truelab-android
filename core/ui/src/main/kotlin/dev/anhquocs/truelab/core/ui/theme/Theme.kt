package dev.anhquocs.truelab.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldDarkPrimary,
    onPrimary = EmeraldDarkOnPrimary,
    primaryContainer = EmeraldDarkPrimaryContainer,
    onPrimaryContainer = EmeraldDarkOnPrimaryContainer,
    secondary = TechBlueDarkSecondary,
    onSecondary = TechBlueDarkOnSecondary,
    secondaryContainer = TechBlueDarkSecondaryContainer,
    onSecondaryContainer = TechBlueDarkOnSecondaryContainer,
    tertiary = TrophyGoldDarkTertiary,
    onTertiary = TrophyGoldDarkOnTertiary,
    tertiaryContainer = TrophyGoldDarkTertiaryContainer,
    onTertiaryContainer = TrophyGoldDarkOnTertiaryContainer,
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
    primary = EmeraldLightPrimary,
    onPrimary = EmeraldLightOnPrimary,
    primaryContainer = EmeraldLightPrimaryContainer,
    onPrimaryContainer = EmeraldLightOnPrimaryContainer,
    secondary = TechBlueLightSecondary,
    onSecondary = TechBlueLightOnSecondary,
    secondaryContainer = TechBlueLightSecondaryContainer,
    onSecondaryContainer = TechBlueLightOnSecondaryContainer,
    tertiary = TrophyGoldLightTertiary,
    onTertiary = TrophyGoldLightOnTertiary,
    tertiaryContainer = TrophyGoldLightTertiaryContainer,
    onTertiaryContainer = TrophyGoldLightOnTertiaryContainer,
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
    dynamicColor: Boolean = false, // Default false to preserve TrueLab Emerald Brand Identity
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
        content = content
    )
}
