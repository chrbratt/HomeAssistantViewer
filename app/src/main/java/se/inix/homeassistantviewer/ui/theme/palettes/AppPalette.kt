package se.inix.homeassistantviewer.ui.theme.palettes

import androidx.compose.material3.ColorScheme

/**
 * A complete colour palette comprises a Light and a Dark Material 3 scheme.
 * Both must be defined so the user's theme-mode (Light / Dark / System)
 * works orthogonally to their palette choice.
 *
 * Implementations are simple `object`s so the schemes are constructed
 * exactly once and reused for every recomposition.
 */
internal interface AppPalette {
    val light: ColorScheme
    val dark: ColorScheme
}
