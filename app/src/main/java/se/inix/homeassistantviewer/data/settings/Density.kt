package se.inix.homeassistantviewer.data.settings

/**
 * Visual density of dashboard cards and the grid that holds them.
 *
 * Persisted alongside the other layout preferences. UI consumers do not
 * read this enum directly — instead they read `LocalCardSpacing`, which
 * MainActivity binds based on the selected value here. Keeping the enum
 * free of `dp` values means the data layer doesn't depend on Compose.
 *
 * - [COMFORTABLE] — the previously-baked spacing (default; existing
 *   users see no change after the upgrade).
 * - [COMPACT] — tighter paddings and grid spacing for users who want
 *   to fit more cards on screen.
 */
enum class Density { COMFORTABLE, COMPACT }
