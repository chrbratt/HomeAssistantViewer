package se.inix.homeassistantviewer.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import se.inix.homeassistantviewer.data.settings.ColorPalette
import se.inix.homeassistantviewer.ui.theme.palettes.AmberPalette
import se.inix.homeassistantviewer.ui.theme.palettes.AppPalette
import se.inix.homeassistantviewer.ui.theme.palettes.AuroraPalette
import se.inix.homeassistantviewer.ui.theme.palettes.CitrinePalette
import se.inix.homeassistantviewer.ui.theme.palettes.EmberPalette
import se.inix.homeassistantviewer.ui.theme.palettes.OceanPalette
import se.inix.homeassistantviewer.ui.theme.palettes.SunsetPalette

/**
 * Resolves a Material 3 [ColorScheme] from the user's preferences.
 *
 *  - [ColorPalette.DYNAMIC] uses Material You on Android 12+, otherwise
 *    falls back to [OceanPalette] so the app never goes naked-grey on
 *    older devices.
 *  - The hand-crafted palettes ignore the device API level — they look
 *    the same on Android 10 and Android 15.
 */
@Composable
fun HomeAssistantStugaTheme(
    darkTheme: Boolean,
    palette: ColorPalette = ColorPalette.DYNAMIC,
    content: @Composable () -> Unit
) {
    val colorScheme = resolveColorScheme(palette, darkTheme)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
private fun resolveColorScheme(palette: ColorPalette, darkTheme: Boolean): ColorScheme {
    if (palette == ColorPalette.DYNAMIC && supportsDynamicColor()) {
        val context = LocalContext.current
        return if (darkTheme) dynamicDarkColorScheme(context)
        else dynamicLightColorScheme(context)
    }
    val handCrafted: AppPalette = when (palette) {
        ColorPalette.AURORA -> AuroraPalette
        ColorPalette.SUNSET -> SunsetPalette
        ColorPalette.EMBER -> EmberPalette
        ColorPalette.AMBER -> AmberPalette
        ColorPalette.CITRINE -> CitrinePalette
        // DYNAMIC on a pre-S device falls through here, intentionally.
        ColorPalette.OCEAN, ColorPalette.DYNAMIC -> OceanPalette
    }
    return if (darkTheme) handCrafted.dark else handCrafted.light
}

private fun supportsDynamicColor(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
