package se.inix.homeassistantviewer.ui.theme.palettes

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Electric & modern. Deep indigo and lilac with magenta highlights — a
 * dark-leaning palette that still has personality. Status visibility is
 * preserved because:
 *   - `primaryContainer` (indigo) reads clearly as "healthy",
 *   - `tertiaryContainer` (magenta) is unambiguous as "warning",
 *   - `errorContainer` keeps the standard red.
 *
 * On a near-black surface (#131129) the saturated accents pop without
 * fighting each other.
 */
internal object AuroraPalette : AppPalette {

    override val light = lightColorScheme(
        primary = Color(0xFF4A45C7),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE4DFFF),
        onPrimaryContainer = Color(0xFF09005A),
        secondary = Color(0xFF5E5C71),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE5DEF9),
        onSecondaryContainer = Color(0xFF1A1A2C),
        tertiary = Color(0xFF94406F),
        onTertiary = Color(0xFFFFFFFF),
        // Pulled toward magenta so the "warning" container stays clearly
        // distinct from the standard light errorContainer pink (#FFDAD6).
        tertiaryContainer = Color(0xFFFFB7DA),
        onTertiaryContainer = Color(0xFF3D0027),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFCFBFF),
        onBackground = Color(0xFF1B1B25),
        surface = Color(0xFFFCFBFF),
        onSurface = Color(0xFF1B1B25),
        surfaceVariant = Color(0xFFE3E1F0),
        onSurfaceVariant = Color(0xFF46465A),
    )

    override val dark = darkColorScheme(
        primary = Color(0xFFB4ACFF),
        onPrimary = Color(0xFF1F1A60),
        primaryContainer = Color(0xFF3A35A0),
        onPrimaryContainer = Color(0xFFE4DFFF),
        secondary = Color(0xFFC9C2DC),
        onSecondary = Color(0xFF312E40),
        secondaryContainer = Color(0xFF484458),
        onSecondaryContainer = Color(0xFFE5DEF9),
        tertiary = Color(0xFFFFB1D5),
        onTertiary = Color(0xFF5A1947),
        tertiaryContainer = Color(0xFF7A2D62),
        onTertiaryContainer = Color(0xFFFFD8E8),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF131129),
        onBackground = Color(0xFFE5E1F6),
        surface = Color(0xFF131129),
        onSurface = Color(0xFFE5E1F6),
        surfaceVariant = Color(0xFF464559),
        onSurfaceVariant = Color(0xFFC8C5DD),
    )
}
