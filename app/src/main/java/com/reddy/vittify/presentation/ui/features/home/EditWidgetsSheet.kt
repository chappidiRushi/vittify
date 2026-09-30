package com.reddy.vittify.presentation.ui.features.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reddy.vittify.data.preferences.HomeWidget
import com.reddy.vittify.presentation.ui.components.PreferenceSwitch
import com.reddy.vittify.presentation.ui.theme.Spacing
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.ui.res.stringResource
import com.reddy.vittify.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWidgetsSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    widgets: List<HomeWidgetUiModel>,
    onToggleVisibility: (HomeWidget, Boolean) -> Unit,
    onReorder: (List<HomeWidget>) -> Unit
) {
    var reorderableWidgets by remember { mutableStateOf(widgets) }

    LaunchedEffect(widgets) {
        if (reorderableWidgets != widgets) {
             reorderableWidgets = widgets
        }
    }

    com.reddy.vittify.presentation.ui.components.VittifyModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.edit_widgets),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .padding(bottom = Spacing.md)
            )

            Text(
                text = stringResource(R.string.edit_widgets_desc),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f),
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .padding(bottom = Spacing.lg)
            )

            val listState = rememberLazyListState()
            val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
                val list = reorderableWidgets.toMutableList()
                val item = list.removeAt(from.index)
                list.add(to.index, item)
                reorderableWidgets = list
                
                // Notify parent to save (debounced)
                onReorder(list.map { it.widget })
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth()
            ) {
                items(reorderableWidgets, key = { it.widget.name }) { widgetModel ->
                    ReorderableItem(
                        state = reorderableState,
                        key = widgetModel.widget.name
                    ) { isDragging ->
                        val elevation = animateDpAsState(if (isDragging) 8.dp else 0.dp)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(elevation.value)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            WidgetItem(
                                widgetModel = widgetModel,
                                onToggleVisibility = { onToggleVisibility(widgetModel.widget, it) },
                                dragModifier = Modifier.draggableHandle()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetItem(
    dragModifier: Modifier = Modifier,
    widgetModel: HomeWidgetUiModel,
    onToggleVisibility: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PreferenceSwitch(
            title = widgetModel.widget.displayName,
            checked = widgetModel.isVisible,
            onCheckedChange = onToggleVisibility,
            leadingIcon = {
                // Drag Handle
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = stringResource(R.string.reorder),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = dragModifier
                        .padding(end = 16.dp)
                        .size(24.dp)
                )
            },
            isSingle = true
        )
    }
}

data class HomeWidgetUiModel(
    val widget: HomeWidget,
    val isVisible: Boolean
)
