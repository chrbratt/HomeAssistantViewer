package se.inix.homeassistantviewer.ui.theme.palettes

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Warm & cozy. Amber-orange primary with coral secondary; a cool teal
 * tertiary keeps "warning" visually distinct from "healthy" so status
 * reading isn't ambiguous in a warm palette. `error` stays standard
 * crimson; the warm primary is yellow-leaning enough that red/orange
 * confusion is avoided.
 *
 * Dark mode uses a deep coffee-brown (#1A1311) — modern, not the typical
 * navy — so the warm accents read as glowing instead of muddy.
 */
internal object SunsetPalette : AppPalette {

    override val light = lightColorScheme(
        primary = Color(0xFFA2461A),
        onPrimary = Color(0xFFFFFFFF),
        // Deeper peach than the standard light errorContainer pink
        // (#FFDAD6) so the "healthy" and "error" banners stay separable.
        primaryContainer = Color(0xFFFFCDAA),
        onPrimaryContainer = Color(0xFF360F00),
        secondary = Color(0xFF8F4A3F),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFFDAD2),
        onSecondaryContainer = Color(0xFF3A0905),
        tertiary = Color(0xFF006A66),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFA0EDE8),
        onTertiaryContainer = Color(0xFF00201F),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFFFBF8),
        onBackground = Color(0xFF221A17),
        surface = Color(0xFFFFFBF8),
        onSurface = Color(0xFF221A17),
        surfaceVariant = Color(0xFFF3DDD7),
        onSurfaceVariant = Color(0xFF52443F),
    )

    override val dark = darkColorScheme(
        primary = Color(0xFFFFB68F),
        onPrimary = Color(0xFF552100),
        primaryContainer = Color(0xFF7A3500),
        onPrimaryContainer = Color(0xFFFFDBC9),
        secondary = Color(0xFFFFB4AA),
        onSecondary = Color(0xFF561E12),
        secondaryContainer = Color(0xFF733427),
        onSecondaryContainer = Color(0xFFFFDAD2),
        tertiary = Color(0xFF7AD0CC),
        onTertiary = Color(0xFF003735),
        tertiaryContainer = Color(0xFF1F4F4C),
        onTertiaryContainer = Color(0xFFA0EDE8),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF1A1311),
        onBackground = Color(0xFFEDE0DA),
        surface = Color(0xFF1A1311),
        onSurface = Color(0xFFEDE0DA),
        surfaceVariant = Color(0xFF534340),
        onSurfaceVariant = Color(0xFFD8C2BD),
    )
}
