package se.inix.homeassistantviewer.data.settings

/**
 * Which Home Assistant timestamp a dashboard card shows as a subtle
 * "time since last" counter.
 *
 * Used in two roles:
 *  - as the **global** default ([NONE] = off) applied to every card, and
 *  - as a **per-entity override** where `null` means "inherit the global
 *    choice" and a non-null value wins over the global one.
 *
 * - [NONE] — show nothing.
 * - [LAST_UPDATED] — `last_updated` (state *or* attributes changed).
 * - [LAST_REPORTED] — `last_reported` (entity last phoned home, even with an
 *   identical value); falls back to `last_updated` when HA doesn't provide it.
 */
enum class CardTimestamp { NONE, LAST_UPDATED, LAST_REPORTED }

/**
 * Resolves the effective card timestamp: a per-entity [override] wins, and
 * only when it's absent (`null` = inherit) does the [global] default apply.
 * This is the single place encoding "global must not override an individual
 * choice".
 */
fun resolveCardTimestamp(override: CardTimestamp?, global: CardTimestamp): CardTimestamp =
    override ?: global
