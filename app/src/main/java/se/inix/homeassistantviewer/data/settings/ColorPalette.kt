package se.inix.homeassistantviewer.data.settings

/**
 * Hand-crafted colour palettes the user can pick between in Settings.
 *
 * Orthogonal to [ThemeMode]: every palette has both a Light and Dark
 * variant, so changing palette does not flip light↔dark and vice-versa.
 *
 * - [DYNAMIC] — defer to the OS (Material You on Android 12+, falls back
 *   to [OCEAN] below that). This is what older builds shipped with as
 *   the implicit default, so it stays the default for new installs.
 * - [OCEAN] — the previously-baked cyan/teal/lavender palette (calm, cool).
 * - [AURORA] — deep indigo, lilac and magenta (electric, modern).
 * - [SUNSET] — warm amber/coral with a cool teal accent (cozy, modern).
 */
enum class ColorPalette { DYNAMIC, OCEAN, AURORA, SUNSET }
