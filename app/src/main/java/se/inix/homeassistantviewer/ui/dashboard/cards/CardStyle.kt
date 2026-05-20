package se.inix.homeassistantviewer.ui.dashboard.cards

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import se.inix.homeassistantviewer.data.settings.Density

/**
 * Visual spacing tokens used by every dashboard card and the grid that
 * holds them. Two presets are exposed (see [Comfortable] / [Compact])
 * and bound at the top of the composition by `MainActivity` via
 * [LocalCardSpacing].
 *
 * Existing call sites use [CardStyle] which delegates to the current
 * [LocalCardSpacing] value — that way switching density only requires
 * the user to flip a setting; no card knows about the enum at all.
 */
internal data class CardSpacing(
    val padding: Dp,
    /** Vertical gap inside [DashboardCardShell] and horizontal gap in card rows. */
    val spacing: Dp,
    /** Tighter gap for chips, cover buttons, and header trailing icons. */
    val tightSpacing: Dp,
    val iconBoxSize: Dp,
    val iconSize: Dp,
    val gridContentPadding: Dp,
    val gridItemSpacing: Dp,
    val sliderHeight: Dp,
    val switchScale: Float,
    val switchRowHeight: Dp,
    val actionIconButtonSize: Dp,
    val actionIconSize: Dp,
    val actionGap: Dp,
    val controlIconButtonSize: Dp,
    val controlIconSize: Dp,
    val primaryControlIconButtonSize: Dp,
    val primaryControlIconSize: Dp,
) {
    companion object {
        val Comfortable = CardSpacing(
            padding = 12.dp,
            spacing = 6.dp,
            tightSpacing = 4.dp,
            iconBoxSize = 32.dp,
            iconSize = 18.dp,
            gridContentPadding = 8.dp,
            gridItemSpacing = 8.dp,
            sliderHeight = 48.dp,
            switchScale = 1f,
            switchRowHeight = 48.dp,
            actionIconButtonSize = 28.dp,
            actionIconSize = 16.dp,
            actionGap = 2.dp,
            controlIconButtonSize = 32.dp,
            controlIconSize = 20.dp,
            primaryControlIconButtonSize = 40.dp,
            primaryControlIconSize = 28.dp,
        )

        val Compact = CardSpacing(
            padding = 6.dp,
            spacing = 2.dp,
            tightSpacing = 2.dp,
            iconBoxSize = 26.dp,
            iconSize = 14.dp,
            gridContentPadding = 4.dp,
            gridItemSpacing = 2.dp,
            sliderHeight = 32.dp,
            switchScale = 0.88f,
            switchRowHeight = 36.dp,
            actionIconButtonSize = 24.dp,
            actionIconSize = 14.dp,
            actionGap = 1.dp,
            controlIconButtonSize = 28.dp,
            controlIconSize = 16.dp,
            primaryControlIconButtonSize = 34.dp,
            primaryControlIconSize = 22.dp,
        )

        fun forDensity(density: Density): CardSpacing = when (density) {
            Density.COMFORTABLE -> Comfortable
            Density.COMPACT -> Compact
        }
    }
}

/**
 * The active [CardSpacing] for this branch of the composition tree.
 * Defaults to [CardSpacing.Comfortable] so previews and lower-level
 * tests don't need to wire the provider explicitly.
 */
internal val LocalCardSpacing = compositionLocalOf { CardSpacing.Comfortable }

/**
 * Backward-compatible accessor used by every card. Reads
 * [LocalCardSpacing] at composition time, so call sites stay unchanged
 * even though the values are now dynamic.
 */
internal object CardStyle {
    val Padding: Dp
        @Composable get() = LocalCardSpacing.current.padding
    val Spacing: Dp
        @Composable get() = LocalCardSpacing.current.spacing
    val TightSpacing: Dp
        @Composable get() = LocalCardSpacing.current.tightSpacing
    val IconBoxSize: Dp
        @Composable get() = LocalCardSpacing.current.iconBoxSize
    val IconSize: Dp
        @Composable get() = LocalCardSpacing.current.iconSize
    val SliderHeight: Dp
        @Composable get() = LocalCardSpacing.current.sliderHeight
    val SwitchScale: Float
        @Composable get() = LocalCardSpacing.current.switchScale
    val SwitchRowHeight: Dp
        @Composable get() = LocalCardSpacing.current.switchRowHeight
    val ActionIconButtonSize: Dp
        @Composable get() = LocalCardSpacing.current.actionIconButtonSize
    val ActionIconSize: Dp
        @Composable get() = LocalCardSpacing.current.actionIconSize
    val ActionGap: Dp
        @Composable get() = LocalCardSpacing.current.actionGap
    val ControlIconButtonSize: Dp
        @Composable get() = LocalCardSpacing.current.controlIconButtonSize
    val ControlIconSize: Dp
        @Composable get() = LocalCardSpacing.current.controlIconSize
    val PrimaryControlIconButtonSize: Dp
        @Composable get() = LocalCardSpacing.current.primaryControlIconButtonSize
    val PrimaryControlIconSize: Dp
        @Composable get() = LocalCardSpacing.current.primaryControlIconSize
}
