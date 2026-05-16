package se.inix.homeassistantviewer.ui.theme.palettes

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * The palette this app shipped with — calm cyan / teal / lavender. Kept
 * around as the canonical "neutral & professional" option and as the
 * fallback when the user picks `DYNAMIC` on a device that doesn't support
 * Material You.
 */
internal object OceanPalette : AppPalette {

    override val light = lightColorScheme(
        primary = Color(0xFF006782),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFBCE9FF),
        onPrimaryContainer = Color(0xFF001F29),
        secondary = Color(0xFF4C626C),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFCFE6F2),
        onSecondaryContainer = Color(0xFF071E27),
        tertiary = Color(0xFF5A5B7E),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFE0E0FF),
        onTertiaryContainer = Color(0xFF161837),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFBFCFE),
        onBackground = Color(0xFF191C1E),
        surface = Color(0xFFFBFCFE),
        onSurface = Color(0xFF191C1E),
        surfaceVariant = Color(0xFFDBE4E9),
        onSurfaceVariant = Color(0xFF40484C),
    )

    override val dark = darkColorScheme(
        primary = Color(0xFF62D2FF),
        onPrimary = Color(0xFF003544),
        primaryContainer = Color(0xFF004D62),
        onPrimaryContainer = Color(0xFFBCE9FF),
        secondary = Color(0xFFB3CAD5),
        onSecondary = Color(0xFF1E333C),
        secondaryContainer = Color(0xFF354A53),
        onSecondaryContainer = Color(0xFFCFE6F2),
        tertiary = Color(0xFFC3C3EA),
        onTertiary = Color(0xFF2C2D4D),
        tertiaryContainer = Color(0xFF424465),
        onTertiaryContainer = Color(0xFFE0E0FF),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF191C1E),
        onBackground = Color(0xFFE1E2E4),
        surface = Color(0xFF191C1E),
        onSurface = Color(0xFFE1E2E4),
        surfaceVariant = Color(0xFF40484C),
        onSurfaceVariant = Color(0xFFDBE4E9),
    )
}
