package com.reddy.vittify.presentation.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reddy.vittify.data.database.entity.CategoryEntity
import com.reddy.vittify.data.database.entity.SubcategoryEntity
import com.reddy.vittify.presentation.ui.features.categories.SubcategoryRow
import com.reddy.vittify.presentation.effects.BlurredAnimatedVisibility
import com.reddy.vittify.presentation.ui.theme.Dimensions
import com.reddy.vittify.presentation.ui.theme.Spacing

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CategoryItem(
    category: CategoryEntity,
    subcategories: List<SubcategoryEntity>,
    onClick: (() -> Unit)?,
    onAddSubcategory: () -> Unit,
    onEditSubcategory: (SubcategoryEntity) -> Unit,
    showAddSubcategoryButton: Boolean = true,
    shape: CornerBasedShape? = null,
    modifier: Modifier = Modifier
) {
    val showAddButton = showAddSubcategoryButton

    VittifyCard(
        modifier = modifier
            .animateContentSize()
            .fillMaxWidth(),
        onClick = onClick,
        shape = shape,
        contentPadding = 0.dp
    ) {
        Column(modifier = Modifier.animateContentSize().fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(Dimensions.Padding.content),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category with Icon
                CategoryChip(
                    category = category,
                    onClick = onClick,
                    showText = true,
                    modifier = Modifier.weight(1f)
                )

                // Subcategory Add Button
                if (showAddButton) {
                    FilledTonalIconButton(
                        onClick = onAddSubcategory,
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = "Add Subcategory",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Subcategory Row (horizontal chips)
            BlurredAnimatedVisibility(subcategories.isNotEmpty()) {
                SubcategoryRow(
                        subcategories = subcategories,
                        onSubcategoryClick = onEditSubcategory,
                        onAddClick = onAddSubcategory,
                        modifier = Modifier.padding(bottom = Spacing.xs),
                        showAddButton = false
                )
            }
            BlurredAnimatedVisibility(subcategories.isEmpty()) {
                // Show add button if no subcategories (and allowed)
                 if (showAddSubcategoryButton) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                 }
            }
        }
    }
}
