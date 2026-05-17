package se.inix.homeassistantviewer.ui.theme.palettes

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Warm near-black canvas with golden / citrine yellow accents. Same
 * design intent as [EmberPalette] — make Material 3 switch on/off state
 * unambiguous — but tuned around yellow instead of orange:
 *
 *   - `primary` is a bright golden yellow (#FFD23F). On the warm dark
 *     `surface` (#14120D) it reads as a glowing "ON" indicator and is
 *     easy to spot at a glance for users scanning a wall of switches.
 *   - `surfaceVariant` is a muted dark olive — the M3 `Switch` uses it
 *     for the OFF-track, so OFF looks distinctly dim and not just
 *     "another yellow".
 *   - `tertiary` is a lime-leaning green (#B6CD60), kept channel-distinct
 *     from `primary` so "warning" status stays visually separate from
 *     "healthy" in dashboard banners.
 *   - `errorContainer` keeps the standard red, so destructive states
 *     remain readable even though the rest of the palette is warm.
 */
internal object CitrinePalette : AppPalette {

    override val light = lightColorScheme(
        primary = Color(0xFF6E5A00),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFE08C),
        onPrimaryContainer = Color(0xFF221A00),
        secondary = Color(0xFF6D5D2F),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFF8E2A6),
        onSecondaryContainer = Color(0xFF221A00),
        tertiary = Color(0xFF4E6400),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFCFEB89),
        onTertiaryContainer = Color(0xFF131F00),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFFFBEF),
        onBackground = Color(0xFF1F1B0A),
        surface = Color(0xFFFFFBEF),
        onSurface = Color(0xFF1F1B0A),
        surfaceVariant = Color(0xFFECE2C8),
        onSurfaceVariant = Color(0xFF4B4634),
        outline = Color(0xFF7C7660),
    )

    override val dark = darkColorScheme(
        primary = Color(0xFFFFD23F),
        onPrimary = Color(0xFF3D2F00),
        primaryContainer = Color(0xFF574500),
        onPrimaryContainer = Color(0xFFFFE08C),
        secondary = Color(0xFFFFCD61),
        onSecondary = Color(0xFF402E00),
        secondaryContainer = Color(0xFF5C4400),
        onSecondaryContainer = Color(0xFFFFE08C),
        tertiary = Color(0xFFB6CD60),
        onTertiary = Color(0xFF243200),
        tertiaryContainer = Color(0xFF384B00),
        onTertiaryContainer = Color(0xFFD2E876),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF14120D),
        onBackground = Color(0xFFECE2C8),
        surface = Color(0xFF14120D),
        onSurface = Color(0xFFECE2C8),
        surfaceVariant = Color(0xFF3D3624),
        onSurfaceVariant = Color(0xFFDDD0AC),
        outline = Color(0xFF978C72),
    )
}
