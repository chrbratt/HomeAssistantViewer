package se.inix.homeassistantviewer.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import se.inix.homeassistantviewer.data.settings.TextContrast

/**
 * Pure colour-scheme transforms shared by every palette. Kept here, away
 * from the palette objects, so the rule lives in exactly one place:
 *
 *  - [withTonalSurfaces] derives the Material 3 "surface container" family
 *    (and a few neighbours) from a palette's own `surface`/`onSurface`,
 *    instead of letting the unspecified roles fall back to the default M3
 *    purple-tinted baseline — which clashed with the hand-crafted hues.
 *  - [withTextContrast] softens body-text colours toward their background
 *    by a small, bounded amount so high-brightness screens don't make the
 *    text look like it's cutting into the surface.
 *
 * Both are deliberately free of Android logging so they stay testable as
 * plain functions.
 */

/** Linear sRGB blend of [this] toward [other] by [fraction] (0..1). */
private fun Color.mix(other: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = red + (other.red - red) * f,
        green = green + (other.green - green) * f,
        blue = blue + (other.blue - blue) * f,
        alpha = 1f,
    )
}

/**
 * Fills the surface-container roles that the hand-crafted palettes leave
 * unset. Higher-elevation containers are nudged toward [ColorScheme.onSurface]
 * (lighter in dark mode, darker in light mode) and the lowest container is
 * nudged toward the mode's extreme, matching how M3 lays out its tonal
 * surface ladder. The dynamic (Material You) scheme already defines these,
 * so callers should not run this on it.
 */
internal fun ColorScheme.withTonalSurfaces(dark: Boolean): ColorScheme {
    val ink = onSurface
    val extreme = if (dark) Color.Black else Color.White
    fun elevated(fraction: Float) = surface.mix(ink, fraction)
    return copy(
        surfaceContainerLowest = surface.mix(extreme, 0.05f),
        surfaceContainerLow = elevated(0.04f),
        surfaceContainer = elevated(0.06f),
        surfaceContainerHigh = elevated(0.09f),
        surfaceContainerHighest = elevated(0.12f),
        surfaceBright = if (dark) elevated(0.12f) else surface,
        surfaceDim = if (dark) surface else elevated(0.07f),
        surfaceTint = primary,
        outlineVariant = onSurfaceVariant.mix(surface, 0.65f),
        inverseSurface = ink,
        inverseOnSurface = surface,
    )
}

/** Softening amount per [TextContrast] level (fraction toward the surface). */
private val TextContrast.softening: Float
    get() = when (this) {
        TextContrast.CRISP -> 0f
        TextContrast.BALANCED -> 0.07f
        TextContrast.SOFT -> 0.16f
    }

/**
 * Eases the primary body-text colours toward the surface behind them so
 * text reads comfortably on a bright screen. Container/accent on-colours
 * are intentionally left untouched: those sit on small, saturated chips
 * and buttons where full contrast aids legibility. `onSurfaceVariant` is
 * already a lower-emphasis tone, so it is softened less to stay readable.
 */
internal fun ColorScheme.withTextContrast(level: TextContrast): ColorScheme {
    val f = level.softening
    if (f <= 0f) return this
    return copy(
        onSurface = onSurface.mix(surface, f),
        onBackground = onBackground.mix(background, f),
        onSurfaceVariant = onSurfaceVariant.mix(surface, f * 0.6f),
    )
}
