package se.inix.homeassistantviewer.data.settings

/**
 * How crisp body text reads against the surface behind it.
 *
 * A pure Material 3 scheme pins on-colours to fixed tones, which on a
 * high-brightness screen can make text look "too sharp" — the dark ink
 * almost vibrates against a near-white surface (and vice-versa in dark
 * mode). This preference lets the user dial the body-text contrast down a
 * notch for comfort without touching the palette itself.
 *
 * The enum stays free of `dp`/`Color` values so the data layer doesn't
 * depend on Compose — the UI layer maps each level to a softening amount
 * (see `ui.theme.SchemeAdjustments`).
 *
 * - [CRISP] — no softening; the raw, maximum-contrast palette text.
 * - [BALANCED] — default; a subtle softening that takes the harsh edge
 *   off without dropping below WCAG AA.
 * - [SOFT] — the most relaxed level, for bright screens or long reading,
 *   still kept above WCAG AA for body text.
 */
enum class TextContrast { CRISP, BALANCED, SOFT }
