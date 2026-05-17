package se.inix.homeassistantviewer.ui.theme.palettes

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Monochromatic graphite canvas with vibrant ember-orange as the sole
 * saturated accent. Tuned for a "stilren" / refined feel — surfaces and
 * `secondary` stay pure neutral grey so the orange does the entire job
 * of carrying brand and status:
 *
 *   - `primary` is a saturated orange (#FF8E45). M3 switches draw their
 *     ON-track from `primary`, so it visually pops against the neutral
 *     graphite `surface` (#0E0E0E) without competing with warm browns.
 *   - `secondary` and its container are pure slate greys, not peach —
 *     this avoids the muddy "warm-on-warm" look that creeps in when
 *     dark orange (which is just brown) meets a warm surface.
 *   - `surfaceVariant` is a neutral graphite (#2A2A2D), so the M3
 *     `Switch` OFF-track reads as visibly dim, not as "a different
 *     orange".
 *   - `tertiary` is a cool blue (#8CCFFF) so the "warning" status
 *     colour on dashboard banners doesn't collide with the warm
 *     `primary`.
 *   - `errorContainer` keeps the standard red, so destructive states
 *     remain readable even though the rest of the palette is
 *     restrained.
 */
internal object EmberPalette : AppPalette {

    override val light = lightColorScheme(
        primary = Color(0xFFA8430A),
        onPrimary = Color(0xFFFFFFFF),
        // Deeper peach than the error pink (#FFDAD6) so the "healthy" and
        // "error" status banners stay visually separable in light mode.
        primaryContainer = Color(0xFFFFCDA9),
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
        primary = Color(0xFFFF8E45),
        onPrimary = Color(0xFF4D1F00),
        // Slightly redder than the previous #6A2D00 so the container
        // reads as "ember", not "milk-chocolate brown".
        primaryContainer = Color(0xFF7A2F08),
        onPrimaryContainer = Color(0xFFFFDCC4),
        // Neutral slate, not peach — keeps the palette monochromatic
        // outside of `primary` and avoids the warm-on-warm mud.
        secondary = Color(0xFFD4D4D8),
        onSecondary = Color(0xFF2A2A2D),
        secondaryContainer = Color(0xFF3A3A3D),
        onSecondaryContainer = Color(0xFFE8E8EA),
        tertiary = Color(0xFF8CCFFF),
        onTertiary = Color(0xFF003348),
        tertiaryContainer = Color(0xFF004A6B),
        onTertiaryContainer = Color(0xFFC9E6FF),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF0E0E0E),
        onBackground = Color(0xFFECECEE),
        surface = Color(0xFF0E0E0E),
        onSurface = Color(0xFFECECEE),
        surfaceVariant = Color(0xFF2A2A2D),
        onSurfaceVariant = Color(0xFFC8C8CC),
        outline = Color(0xFF6E6E72),
    )
}
