package se.inix.homeassistantviewer.ui.theme.palettes

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Warm honey-amber sibling of [EmberPalette]. Where Ember keeps a strict
 * graphite surface and uses orange as the only accent, Amber leans
 * deliberately into warmth — the surfaces themselves are tinted brown,
 * the secondary is a soft peach, and the primary is shifted toward
 * honey instead of pure orange.
 *
 *   - `primary` (#FFA862) is amber-yellow, so it harmonises with the
 *     warm `surface` and `surfaceVariant` instead of fighting them. The
 *     M3 `Switch` still pops against the dark brown OFF-track because
 *     the saturation gap is large.
 *   - `tertiary` stays cool blue so "warning" status banners remain
 *     visually distinct from the warm `primary`.
 *   - `errorContainer` keeps the standard red, so destructive states
 *     remain readable in an otherwise warm palette.
 *
 * Pick Amber when you want a cosy "fireplace" feel; pick Ember when
 * you want a clean monochromatic look with a single accent.
 */
internal object AmberPalette : AppPalette {

    override val light = lightColorScheme(
        primary = Color(0xFFA8540A),
        onPrimary = Color(0xFFFFFFFF),
        // Warm peach, deep enough that it stays separable from the
        // standard light errorContainer pink (#FFDAD6).
        primaryContainer = Color(0xFFFFCEAA),
        onPrimaryContainer = Color(0xFF381300),
        secondary = Color(0xFF8A5028),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFFDCC4),
        onSecondaryContainer = Color(0xFF2E1500),
        tertiary = Color(0xFF00638B),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFC5E7FF),
        onTertiaryContainer = Color(0xFF001E2E),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFFFBF7),
        onBackground = Color(0xFF1E1611),
        surface = Color(0xFFFFFBF7),
        onSurface = Color(0xFF1E1611),
        surfaceVariant = Color(0xFFF2E0D2),
        onSurfaceVariant = Color(0xFF4E443C),
        outline = Color(0xFF80746A),
    )

    override val dark = darkColorScheme(
        primary = Color(0xFFFFA862),
        onPrimary = Color(0xFF4D2500),
        primaryContainer = Color(0xFF7A3F00),
        onPrimaryContainer = Color(0xFFFFDDC2),
        secondary = Color(0xFFFFCBA5),
        onSecondary = Color(0xFF4F2A00),
        secondaryContainer = Color(0xFF6E3F1A),
        onSecondaryContainer = Color(0xFFFFDDC2),
        tertiary = Color(0xFF8CCFFF),
        onTertiary = Color(0xFF003348),
        tertiaryContainer = Color(0xFF004A6B),
        onTertiaryContainer = Color(0xFFC9E6FF),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF14100C),
        onBackground = Color(0xFFECE0D5),
        surface = Color(0xFF14100C),
        onSurface = Color(0xFFECE0D5),
        surfaceVariant = Color(0xFF3D3127),
        onSurfaceVariant = Color(0xFFE0CDBE),
        outline = Color(0xFF998878),
    )
}
