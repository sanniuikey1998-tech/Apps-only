package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val SpatialDarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = TextPrimary,
    primaryContainer = ElectricBlueGlow,
    onPrimaryContainer = ElectricBlueBright,
    secondary = LuminousViolet,
    onSecondary = TextPrimary,
    secondaryContainer = LuminousVioletGlow,
    onSecondaryContainer = LuminousVioletBright,
    tertiary = CyanIce,
    onTertiary = SpaceObsidian,
    background = SpaceObsidian,
    onBackground = TextPrimary,
    surface = SpaceDeep,
    onSurface = TextPrimary,
    surfaceVariant = SpaceSlate,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorderHighlight,
    outlineVariant = GlassBorderSubtle,
    error = RoseAlert,
    onError = TextPrimary,
    errorContainer = RoseContainer,
    onErrorContainer = RoseAlert
)

val DeckLinkShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun DeckLinkTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SpatialDarkColorScheme,
        typography = DeckLinkTypography,
        shapes = DeckLinkShapes,
        content = content
    )
}
