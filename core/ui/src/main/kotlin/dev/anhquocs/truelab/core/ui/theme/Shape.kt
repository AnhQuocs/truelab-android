package dev.anhquocs.truelab.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val RadiusExtraSmall = 4.dp
val RadiusSmall      = 8.dp
val RadiusMedium     = 12.dp
val RadiusLarge      = 16.dp
val RadiusExtraLarge = 24.dp
val RadiusPill       = 100.dp

val TrueLabShapes = Shapes(
    extraSmall = RoundedCornerShape(RadiusExtraSmall),
    small = RoundedCornerShape(RadiusSmall),
    medium = RoundedCornerShape(RadiusMedium),
    large = RoundedCornerShape(RadiusLarge),
    extraLarge = RoundedCornerShape(RadiusExtraLarge)
)
