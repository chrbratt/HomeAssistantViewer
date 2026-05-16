package se.inix.homeassistantviewer.ui.dashboard.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import se.inix.homeassistantviewer.ui.dashboard.DashboardItem
import se.inix.homeassistantviewer.ui.dashboard.EntityAction
import se.inix.homeassistantviewer.ui.dashboard.cards.EntityCard
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyStaggeredGridState

/**
 * Reorderable staggered grid of [DashboardItem]s. Dividers span a full row
 * (forcing the cards below to start on a fresh row); entities take one lane.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardGrid(
    items: List<DashboardItem>,
    columns: Int,
    onAction: (EntityAction) -> Unit,
    onSaveOrder: (List<DashboardItem>) -> Unit,
    onRequestRemove: (DashboardItem) -> Unit,
    onOpenDetail: (connectionId: String, entityId: String) -> Unit
) {
    val lazyGridState = rememberLazyStaggeredGridState()
    var localItems by remember { mutableStateOf(items) }

    val reorderState = rememberReorderableLazyStaggeredGridState(lazyGridState) { from, to ->
        localItems = localItems.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
    }

    // Pull external updates in unless the user is actively dragging.
    LaunchedEffect(items) {
        if (!reorderState.isAnyItemDragging) localItems = items
    }

    val haptic = LocalHapticFeedback.current

    LazyVerticalStaggeredGrid(
        state = lazyGridState,
        columns = StaggeredGridCells.Fixed(columns),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            count = localItems.size,
            key = { index -> localItems[index].key },
            span = { index -> dashboardSpan(localItems, index) }
        ) { index ->
            val item = localItems[index]
            ReorderableItem(state = reorderState, key = item.key) { isDragging ->
                val elevation by animateDpAsState(
                    if (isDragging) 10.dp else 0.dp,
                    label = "dragElevation"
                )
                val scale by animateFloatAsState(
                    if (isDragging) 1.04f else 1f,
                    label = "dragScale"
                )

                val dragModifier = Modifier
                    .longPressDraggableHandle(
                        onDragStarted = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                        onDragStopped = { onSaveOrder(localItems) }
                    )
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .shadow(elevation, shape = MaterialTheme.shapes.large)

                when (item) {
                    is DashboardItem.Divider -> RowBreakDivider(
                        modifier = dragModifier,
                        onRemove = { onRequestRemove(item) }
                    )
                    is DashboardItem.Entity -> EntityCard(
                        item = item,
                        onAction = onAction,
                        onRequestRemove = { onRequestRemove(item) },
                        onOpenDetail = onOpenDetail,
                        modifier = dragModifier
                    )
                }
            }
        }
    }
}
