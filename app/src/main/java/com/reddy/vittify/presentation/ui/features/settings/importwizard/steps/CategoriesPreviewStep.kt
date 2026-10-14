package com.reddy.vittify.presentation.ui.features.settings.importwizard.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reddy.vittify.data.backup.importwizard.ImportCategoryItem
import com.reddy.vittify.data.backup.importwizard.RangeSelectionTracker
import com.reddy.vittify.data.database.entity.CategoryEntity
import com.reddy.vittify.data.database.entity.SubcategoryEntity
import com.reddy.vittify.presentation.ui.components.CategorySelectionSheet
import com.reddy.vittify.presentation.ui.components.ListItem
import com.reddy.vittify.presentation.ui.components.ListItemPosition
import com.reddy.vittify.presentation.ui.components.toShape
import com.reddy.vittify.presentation.ui.components.VittifyCheckbox
import com.reddy.vittify.presentation.ui.features.settings.importwizard.components.SelectionActionBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesPreviewStep(
    categories: List<ImportCategoryItem>,
    existingAppCategories: List<CategoryEntity>,
    existingAppSubcategories: List<SubcategoryEntity>,
    subcategoriesMap: Map<Long, List<SubcategoryEntity>>,
    rangeTracker: RangeSelectionTracker,
    onToggleCategory: (Int) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onApplyRange: () -> Unit,
    onMapCategorySelection: (index: Int, category: CategoryEntity, subcategory: SubcategoryEntity?) -> Unit,
    onToggleCreateAsNew: (index: Int, isNew: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var mappingCategoryIndex by remember { mutableStateOf<Int?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Preview & Map Categories",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Tap any category to map it to an in-app category, or toggle 'New' to create as root.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            SelectionActionBar(
                totalCount = categories.size,
                selectedCount = categories.count { it.isSelected },
                rangeTracker = rangeTracker,
                onSelectAll = onSelectAll,
                onDeselectAll = onDeselectAll,
                onApplyRange = onApplyRange,
                itemNameSingular = "category",
                itemNamePlural = "categories"
            )
        }

        // Clean Grouped List of Categories
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.5.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            itemsIndexed(categories) { index, category ->
                val position = ListItemPosition.from(index, categories.size)

                ListItem(
                    shape = position.toShape(),
                    padding = PaddingValues(horizontal = 16.dp, vertical = 1.dp),
                    onClick = { mappingCategoryIndex = index },
                    leading = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            VittifyCheckbox(
                                checked = category.isSelected,
                                onCheckedChange = { onToggleCategory(index) }
                            )

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Category,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    headline = {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    supporting = {
                        val (desc, chipBg, chipText) = when {
                            category.createAsNew -> Triple(
                                "✨ Create as new root category",
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            category.createAsSubcategoryUnderParent -> Triple(
                                "↳ Create under: ${category.mappedCategoryName}",
                                Color(0xFFFFF3E0),
                                Color(0xFFE65100)
                            )
                            category.mappedSubcategoryName != null -> Triple(
                                "✓ ${category.mappedCategoryName} ↳ ${category.mappedSubcategoryName}",
                                Color(0xFFE8F5E9),
                                Color(0xFF2E7D32)
                            )
                            category.mappedCategoryName != null -> Triple(
                                "✓ ${category.mappedCategoryName}",
                                Color(0xFFE8F5E9),
                                Color(0xFF2E7D32)
                            )
                            else -> Triple(
                                "Tap to map to category...",
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = chipBg,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = chipText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.Rounded.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = chipText
                                )
                            }
                        }
                    },
                    trailing = {
                        FilterChip(
                            selected = category.createAsNew,
                            onClick = { onToggleCreateAsNew(index, !category.createAsNew) },
                            label = {
                                Text(
                                    text = if (category.createAsNew) "New" else "Map",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                )
            }
        }
    }

    // Reusable CategorySelectionSheet Modal Bottom Sheet from Transaction Add Page
    mappingCategoryIndex?.let { index ->
        ModalBottomSheet(
            onDismissRequest = { mappingCategoryIndex = null },
            sheetState = sheetState
        ) {
            CategorySelectionSheet(
                categories = existingAppCategories,
                subcategoriesMap = subcategoriesMap,
                onSelectionComplete = { selectedCategory, selectedSubcategory ->
                    onMapCategorySelection(index, selectedCategory, selectedSubcategory)
                    mappingCategoryIndex = null
                },
                onDismiss = { mappingCategoryIndex = null }
            )
        }
    }
}


