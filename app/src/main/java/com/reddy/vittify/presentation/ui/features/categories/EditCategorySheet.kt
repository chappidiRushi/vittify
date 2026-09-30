package com.reddy.vittify.presentation.ui.features.categories

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.*
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.reddy.vittify.R
import com.reddy.vittify.data.database.entity.CategoryEntity
import com.reddy.vittify.presentation.ui.components.ColorPickerContent
import com.reddy.vittify.presentation.ui.components.GenericTypeSwitcher
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpOffset
import com.reddy.vittify.presentation.ui.icons.Bag
import com.reddy.vittify.presentation.ui.icons.Iconax
import androidx.compose.material3.OutlinedButton
import com.reddy.vittify.data.database.entity.CategoryType
import com.reddy.vittify.utils.IconResolutionUtils
import androidx.compose.ui.platform.LocalContext
import com.reddy.vittify.presentation.ui.theme.Spacing
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import com.reddy.vittify.presentation.ui.theme.VittifySurface

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EditCategorySheet(
    category: CategoryEntity?,
    initialCategoryType: CategoryType = CategoryType.EXPENSE,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, color: String, iconResId: Int, iconName: String, categoryType: CategoryType) -> Unit,
    onReset: ((Long) -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val defaultIconForType = remember(initialCategoryType) {
        when (initialCategoryType) {
            CategoryType.INCOME -> "type_finance_money_bag" to R.drawable.type_finance_money_bag
            CategoryType.INVESTMENT -> "type_flower_and_tree_herb" to R.drawable.type_flower_and_tree_herb
            CategoryType.CREDIT -> "type_stationary_card_file_box" to R.drawable.type_stationary_card_file_box
            CategoryType.EXPENSE -> "type_food_dining" to R.drawable.type_food_dining
        }
    }
    val defaultColorForType = remember(initialCategoryType) {
        when (initialCategoryType) {
            CategoryType.INCOME -> "#4CAF50"
            CategoryType.INVESTMENT -> "#00796B"
            CategoryType.CREDIT -> "#1976D2"
            CategoryType.EXPENSE -> "#33B5E5"
        }
    }

    var name by remember { mutableStateOf(category?.name ?: "") }
    var description by remember { mutableStateOf(category?.description ?: "") }
    var colorHex by remember { mutableStateOf(category?.color ?: defaultColorForType) }
    var iconName by remember(category) {
        mutableStateOf(
            category?.iconName?.takeIf { it.isNotEmpty() }
                ?: IconResolutionUtils.resIdToName(context, category?.iconResId ?: defaultIconForType.second)
                    .takeIf { it.isNotEmpty() } ?: defaultIconForType.first
        )
    }
    var iconResId by remember(iconName) {
        mutableIntStateOf(
            IconResolutionUtils.nameToResId(context, iconName)
                .takeIf { it != 0 } ?: defaultIconForType.second
        )
    }

    var isIncome by remember { mutableStateOf(category?.isIncome ?: false) }
    var categoryType by remember(category, initialCategoryType) {
        mutableStateOf(category?.categoryType ?: initialCategoryType)
    }

    var showIconSelector by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showIconSelector) {
        com.reddy.vittify.presentation.ui.components.VittifyModalBottomSheet(
            onDismissRequest = { showIconSelector = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            IconSelector(
                selectedIconName = iconName,
                onIconSelected = { name ->
                    iconName = name
                    iconResId = IconResolutionUtils.nameToResId(context, name)
                    showIconSelector = false
                }
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .imePadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Preview Section

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = VittifyShapes.hero,
                    colors = CardDefaults.cardColors(
                        containerColor = VittifySurface.surfaceContainerColor()
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Presentational category icon with selected 100% color
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(VittifyShapes.input)
                                .background(Color(colorHex.toColorInt())),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = iconResId),
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = Color.Unspecified
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = name.ifEmpty { stringResource(R.string.category_name) },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = description.ifEmpty { stringResource(R.string.category_description_placeholder) },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.preview),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.6f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Type Switcher
            val typeOptions = remember {
                listOf(
                    CategoryType.EXPENSE,
                    CategoryType.INCOME,
                    CategoryType.CREDIT,
                    CategoryType.INVESTMENT
                )
            }
            GenericTypeSwitcher(
                selectedIndex = typeOptions.indexOf(categoryType).coerceAtLeast(0),
                onIndexChange = { index -> categoryType = typeOptions[index] },
                options = typeOptions.map { stringResource(it.labelRes) },
                modifier = Modifier.fillMaxWidth()
            )

            // Add/Edit Icon Selector Button before Name & Description
            Surface(
                onClick = { showIconSelector = true },
                shape = VittifyShapes.input,
                color = VittifySurface.surfaceContainerLowColor(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(VittifyShapes.input)
                                .background(Color(colorHex.toColorInt())),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = iconResId),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = Color.Unspecified
                            )
                        }
                        Text(
                            text = stringResource(R.string.select_category_icon),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = stringResource(R.string.change_action),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }


            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(
                        text= stringResource(R.string.name_label),
                        fontWeight = FontWeight.SemiBold
                    ) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        lineHeight = 18.sp,
                        fontSize = 16.sp
                    ),
                    shape = VittifyShapes.scaled(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = 4.dp,
                        bottomEnd = 4.dp
                    ),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = VittifySurface.surfaceContainerLowColor(),
                        unfocusedContainerColor = VittifySurface.surfaceContainerLowColor(),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            0.7f
                        )
                    )
                )
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(
                        text= stringResource(R.string.description_label),
                        fontWeight = FontWeight.SemiBold
                    ) },
                    placeholder = { Text(stringResource(R.string.description_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        lineHeight = 18.sp,
                        fontSize = 16.sp
                    ),
                    shape = VittifyShapes.scaled(
                        topStart = 4.dp,
                        topEnd = 4.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    ),
                    maxLines = 2,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = VittifySurface.surfaceContainerLowColor(),
                        unfocusedContainerColor = VittifySurface.surfaceContainerLowColor(),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            0.7f
                        )
                    )
                )
            }


            // Color Picker Section

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = VittifyShapes.large,
                colors =
                    CardDefaults.cardColors(
                        containerColor = VittifySurface.surfaceContainerLowColor()
                    ),
                border = VittifySurface.platterBorder()
            ) {
                Column{
                    ColorPickerContent(
                        initialColor = colorHex.toColorInt(),
                        onColorChanged = { colorInt ->
                            colorHex = String.format("#%06X", 0xFFFFFF and colorInt)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(82.dp))

        }
        // Action Buttons at Bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Delete button (only for non-system categories)
                if (onDelete != null && category != null && !category.isSystem) {
                    OutlinedButton(
                        onClick = { onDelete() },
                        modifier = Modifier.height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.error,
                                    MaterialTheme.colorScheme.error
                                )
                            )
                        ),
                        shape = MaterialTheme.shapes.extraExtraLarge
                    ) {
                        Icon(
                            imageVector = Iconax.Bag,
                            contentDescription = stringResource(R.string.delete_category)
                        )
                    }
                }

                // Create/Update button
                Button(
                    onClick = { onSave(name, description, colorHex, iconResId, iconName, categoryType) },
                    enabled = name.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = MaterialTheme.shapes.extraExtraLarge
                ) {
                    Text(
                        text = if (category == null) stringResource(R.string.create_category) else stringResource(R.string.update_category),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Reset button (only for system categories)
                if (category?.isSystem == true && onReset != null) {
                    IconButton(
                        onClick = {
                            name = category.defaultName ?: category.name
                            description = category.defaultDescription ?: ""
                            colorHex = category.defaultColor ?: category.color
                            iconName = category.defaultIconName ?: category.iconName
                            iconResId = IconResolutionUtils.nameToResId(context, iconName)
                            categoryType = category.defaultCategoryType?.let { CategoryType.fromString(it) } ?: category.categoryType
                            onReset(category.id)
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.extraExtraLarge
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.RestartAlt,
                            contentDescription = stringResource(R.string.reset_to_default),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

