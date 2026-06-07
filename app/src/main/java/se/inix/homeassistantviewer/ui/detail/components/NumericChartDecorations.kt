package se.inix.homeassistantviewer.ui.detail.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.decoration.HorizontalLine
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent

/**
 * Zero reference line for numeric charts.
 *
 * Only drawn when the plotted data dips below zero (e.g. power import/export,
 * sub-zero temperature), where "where is zero?" is genuinely ambiguous. For
 * purely non-negative series the line would sit on the bottom edge and add
 * visual noise, so it is omitted.
 */
@Composable
internal fun rememberZeroLineDecoration(includesNegative: Boolean): HorizontalLine? {
    if (!includesNegative) return null
    val lineColor = MaterialTheme.colorScheme.outline
    val line = rememberLineComponent(
        fill = Fill(lineColor),
        thickness = 1.dp
    )
    return remember(line) { HorizontalLine(y = { 0.0 }, line = line) }
}
