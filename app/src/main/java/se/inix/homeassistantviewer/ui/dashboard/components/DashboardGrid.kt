package se.inix.homeassistantviewer.ui.dashboard.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import se.inix.homeassistantviewer.data.model.ComparisonEntity
import se.inix.homeassistantviewer.data.model.isComparableDomain
import se.inix.homeassistantviewer.ui.dashboard.DashboardItem
import se.inix.homeassistantviewer.ui.dashboard.EntityAction
import se.inix.homeassistantviewer.ui.dashboard.cards.ComparisonSelectionUi
import se.inix.homeassistantviewer.ui.dashboard.cards.EntityCard
import se.inix.homeassistantviewer.ui.dashboard.cards.LocalCardSpacing
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyStaggeredGridState

/**
 * Reorderable staggered grid of [DashboardItem]s. Dividers span a full row
 * (forcing the cards below to start on a fresh row); entities take one lane.
 */
@Composable
fun DashboardGrid(
    items: List<DashboardItem>,
    columns: Int,
    comparisonSelection: Set<ComparisonEntity>,
    onAction: (EntityAction) -> Unit,
    onSaveOrder: (List<DashboardItem>) -> Unit,
    onRequestRemove: (DashboardItem) -> Unit,
    onRequestRename: (DashboardItem) -> Unit,
    onOpenDetail: (connectionId: String, entityId: String) -> Unit,
    onToggleComparison: (connectionId: String, entityId: String) -> Unit
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
    val spacing = LocalCardSpacing.current
    val comparisonModeActive = comparisonSelection.isNotEmpty()

    LazyVerticalStaggeredGrid(
        state = lazyGridState,
        columns = StaggeredGridCells.Fixed(columns),
        contentPadding = PaddingValues(spacing.gridContentPadding),
        horizontalArrangement = Arrangement.spacedBy(spacing.gridItemSpacing),
        verticalItemSpacing = spacing.gridItemSpacing,
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

                val dragHandleModifier = Modifier.longPressDraggableHandle(
                    onDragStarted = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDragStopped = { onSaveOrder(localItems) }
                )

                val itemModifier = Modifier
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .shadow(elevation, shape = MaterialTheme.shapes.large)

                when (item) {
                    is DashboardItem.Divider -> Box(modifier = itemModifier.fillMaxWidth()) {
                        RowBreakDivider(
                            title = item.title,
                            modifier = Modifier.fillMaxWidth(),
                            onEditTitle = { onRequestRename(item) },
                            onRemove = { onRequestRemove(item) }
                        )
                        DragHandleIcon(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .then(dragHandleModifier)
                        )
                    }
                    is DashboardItem.Entity -> {
                        val comparable = item.isComparableDomain()
                        val isSelected = ComparisonEntity(item.connectionId, item.entityId) in comparisonSelection
                        Box(
                            modifier = itemModifier
                                .fillMaxWidth()
                                .then(
                                    if (comparable) {
                                        Modifier.pointerInput(item.key, comparisonModeActive) {
                                            detectTapGestures(
                                                onLongPress = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    onToggleComparison(item.connectionId, item.entityId)
                                                }
                                            )
                                        }
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            EntityCard(
                                item = item,
                                onAction = onAction,
                                onRequestRemove = { onRequestRemove(item) },
                                onRequestRename = { onRequestRename(item) },
                                onOpenDetail = onOpenDetail,
                                comparisonSelection = if (comparable && comparisonModeActive) {
                                    ComparisonSelectionUi(
                                        isSelected = isSelected,
                                        onToggle = {
                                            onToggleComparison(item.connectionId, item.entityId)
                                        }
                                    )
                                } else {
                                    null
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DragHandleIcon(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .then(dragHandleModifier)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun DashboardItem.Entity.isComparableDomain(): Boolean =
    ComparisonEntity(connectionId, entityId).isComparableDomain()
