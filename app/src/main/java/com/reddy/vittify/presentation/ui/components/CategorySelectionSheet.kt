package com.reddy.vittify.presentation.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reddy.vittify.data.database.entity.CategoryEntity
import com.reddy.vittify.data.database.entity.CategoryType
import com.reddy.vittify.data.database.entity.SubcategoryEntity
import com.reddy.vittify.presentation.ui.icons.CloseCircle
import com.reddy.vittify.presentation.ui.icons.Iconax
import com.reddy.vittify.presentation.ui.theme.Dimensions
import com.reddy.vittify.presentation.ui.theme.Spacing
import kotlinx.coroutines.delay

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.focus.FocusRequester
import androidx.core.graphics.toColorInt

@Composable
fun CategorySelectionSheet(
    categories: List<CategoryEntity>,
    subcategoriesMap: Map<Long, List<SubcategoryEntity>>,
    onSelectionComplete: (CategoryEntity, SubcategoryEntity?) -> Unit,
    onDismiss: () -> Unit,
    onAddSubcategory: ((CategoryEntity) -> Unit)? = null,
    recentSubcategories: List<Pair<CategoryEntity, SubcategoryEntity>> = emptyList(),
    focusRequester: FocusRequester? = null
) {
    val categoryType = remember(categories) {
        categories.firstOrNull()?.categoryType ?: CategoryType.EXPENSE
    }
    val labels = remember(categoryType) {
        when (categoryType) {
            CategoryType.INCOME -> listOf("Search Salary", "Search Bonus", "Search Freelance", "Search Dividends")
            CategoryType.INVESTMENT -> listOf("Search Stocks", "Search Mutual Funds", "Search Gold", "Search Crypto")
            CategoryType.CREDIT -> listOf("Search EMI", "Search Credit Bill", "Search Loan", "Search Repayment")
            CategoryType.EXPENSE -> listOf("Search Fruits", "Search Shopping", "Search Fitness", "Search Dining")
        }
    }
    var currentLabelIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(labels) {
        currentLabelIndex = 0
        while (true) {
            delay(3000)
            currentLabelIndex = (currentLabelIndex + 1) % labels.size
        }
    }
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }

    // Auto-focus search field if focusRequester is provided
    LaunchedEffect(focusRequester) {
        if (focusRequester != null) {
            delay(150)
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                // Focus request might fail if not attached yet
            }
        }
    }
    
    // Filter categories based on search
    val filteredCategories = remember(categories, subcategoriesMap, searchQuery.text) {
        if (searchQuery.text.isBlank()) {
            categories
        } else {
            categories.filter { category ->
                val categoryMatches = category.name.contains(searchQuery.text, ignoreCase = true)
                val subcategoriesMatch = subcategoriesMap[category.id]?.any {
                    it.name.contains(searchQuery.text, ignoreCase = true)
                } == true
                categoryMatches || subcategoriesMatch
            }
        }
    }

    val expandedStates = remember { mutableStateMapOf<Long, Boolean>() }

    // Auto-expand categories that have matching subcategories when searching
    LaunchedEffect(searchQuery.text) {
        if (searchQuery.text.isNotBlank()) {
            filteredCategories.forEach { category ->
                val hasMatchingSubcategory = subcategoriesMap[category.id]?.any {
                    it.name.contains(searchQuery.text, ignoreCase = true)
                } == true
                if (hasMatchingSubcategory) {
                    expandedStates[category.id] = true
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Search Bar
        Box(modifier = Modifier.padding(horizontal = Dimensions.Padding.content, vertical = Spacing.sm)) {
             SearchBarBox(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                focusRequester = focusRequester,
                 label = {
                     AnimatedContent(
                         targetState = labels[currentLabelIndex],
                         transitionSpec = {
                             (fadeIn(animationSpec = tween(400, delayMillis = 100)) +
                                     slideInVertically(
                                         initialOffsetY = { it },
                                         animationSpec = tween(400, delayMillis = 100)
                                     ))
                                 .togetherWith(
                                     fadeOut(animationSpec = tween(400)) +
                                             slideOutVertically(
                                                 targetOffsetY = { -it },
                                                 animationSpec = tween(400)
                                             )
                                 )
                         },
                         label = "SearchBarLabelAnimation"
                     ) { labelText ->
                         Text(
                             text = labelText,
                             fontSize = 14.sp,
                             lineHeight = 14.sp,
                             fontWeight = FontWeight.SemiBold,
                             fontStyle = FontStyle.Italic,
                             textAlign = TextAlign.Center,
                             color = MaterialTheme.colorScheme.inverseSurface.copy(0.5f),
                             modifier = Modifier.fillMaxWidth()
                         )
                     }
                 },
                leadingIcon = {},
                trailingIcon = if (searchQuery.text.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = TextFieldValue("") }) {
                            Icon(Iconax.CloseCircle, contentDescription = "Clear search")
                        }
                    }
                } else { {} }
            )
        }

        val validCategoryIds = remember(categories) { categories.map { it.id }.toSet() }
        val filteredRecentSubcategories = remember(recentSubcategories, validCategoryIds) {
            recentSubcategories.filter { it.first.id in validCategoryIds }
        }

        // Quick Suggestions: dynamically changes based on search query
        val isSearching = searchQuery.text.isNotBlank()
        val matchingSuggestions = remember(categories, subcategoriesMap, searchQuery.text) {
            if (!isSearching) {
                emptyList()
            } else {
                val query = searchQuery.text.trim().lowercase()
                val list = mutableListOf<Pair<CategoryEntity, SubcategoryEntity?>>()
                // First: matching subcategories
                for (cat in categories) {
                    val subs = subcategoriesMap[cat.id] ?: emptyList()
                    for (sub in subs) {
                        if (sub.name.lowercase().contains(query)) {
                            list.add(Pair(cat, sub))
                        }
                    }
                }
                // Second: matching categories (if not already included)
                for (cat in categories) {
                    if (cat.name.lowercase().contains(query) && list.none { it.first.id == cat.id }) {
                        list.add(Pair(cat, null))
                    }
                }
                list.take(12)
            }
        }

        val showSuggestions = (!isSearching && filteredRecentSubcategories.isNotEmpty()) ||
                (isSearching && matchingSuggestions.isNotEmpty())

        if (showSuggestions) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.xs)
            ) {
                Text(
                    text = if (isSearching) "Matching Suggestions" else "Recently Used",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Dimensions.Padding.content, vertical = Spacing.xs)
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = Dimensions.Padding.content),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isSearching) {
                        items(
                            items = filteredRecentSubcategories,
                            key = { "recent_${it.first.id}_${it.second.id}" }
                        ) { (category, subcategory) ->
                            QuickSuggestionChip(
                                category = category,
                                subcategory = subcategory,
                                onClick = { onSelectionComplete(category, subcategory) }
                            )
                        }
                    } else {
                        items(
                            items = matchingSuggestions,
                            key = { "match_${it.first.id}_${it.second?.id ?: -1}" }
                        ) { (category, subcategory) ->
                            QuickSuggestionChip(
                                category = category,
                                subcategory = subcategory,
                                onClick = { onSelectionComplete(category, subcategory) }
                            )
                        }
                    }
                }
            }
        }

        if (filteredCategories.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No categories found",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(
                        start = Dimensions.Padding.content,
                        end = Dimensions.Padding.content,
                        top = Spacing.sm,
                        bottom = 0.dp
                    )
                    .clip(com.reddy.vittify.presentation.ui.theme.VittifyShapes.input),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                items(
                    items = filteredCategories,
                    key = { it.id }
                ) { category ->
                    val subs = subcategoriesMap[category.id] ?: emptyList()
                    
                    val isExpanded = expandedStates[category.id] == true
                    
                    val displayedSubcategories = if (searchQuery.text.isNotBlank()) {
                        // When searching, show only matching subcategories OR all if category matches
                        val categoryMatches = category.name.contains(searchQuery.text, ignoreCase = true)
                        if (categoryMatches) {
                            subs
                        } else {
                            subs.filter { it.name.contains(searchQuery.text, ignoreCase = true) }
                        }
                    } else if (isExpanded) {
                        subs
                    } else {
                        emptyList()
                    }

                    AnimatedContent(
                        targetState = category,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220)) +
                                    scaleIn(initialScale = 0.95f) togetherWith fadeOut(animationSpec = tween(90))
                        },
                        label = "CategoryItemAnimation"
                    ) { animatedCategory ->
                         CategoryItem(
                            category = animatedCategory,
                            subcategories = displayedSubcategories,
                            onClick = {
                                if (subs.isNotEmpty()) {
                                    if (isExpanded) {
                                        // If already expanded, select the category itself
                                        onSelectionComplete(animatedCategory, null)
                                    } else {
                                        // Otherwise, expand to show subcategories
                                        expandedStates[animatedCategory.id] = true
                                    }
                                } else {
                                    onSelectionComplete(animatedCategory, null)
                                }
                            },
                            onAddSubcategory = {
                                onAddSubcategory?.invoke(animatedCategory)
                            },
                            onEditSubcategory = { sub ->
                                onSelectionComplete(animatedCategory, sub)
                            },
                            showAddSubcategoryButton = onAddSubcategory != null
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(Spacing.xxl))
                }
            }
        }
    }
}

@Composable
private fun QuickSuggestionChip(
    category: CategoryEntity,
    subcategory: SubcategoryEntity?,
    onClick: () -> Unit
) {
    val backgroundColor = remember(subcategory?.color, category.color) {
        try {
            Color((subcategory?.color ?: category.color).toColorInt()).copy(alpha = 0.2f)
        } catch (e: Exception) {
            Color(0xFF757575).copy(alpha = 0.2f)
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val iconResId = remember(subcategory?.iconName, subcategory?.iconResId, category.iconName, category.iconResId) {
        if (subcategory != null && subcategory.iconName.isNotEmpty()) {
            com.reddy.vittify.utils.IconResolutionUtils.nameToResId(context, subcategory.iconName)
                .takeIf { it != 0 } ?: 0
        } else if (subcategory != null && subcategory.iconResId != 0) {
            com.reddy.vittify.utils.IconResolutionUtils.getSafeResId(context, subcategory.iconResId, 0)
        } else if (category.iconName.isNotEmpty()) {
            com.reddy.vittify.utils.IconResolutionUtils.nameToResId(context, category.iconName)
                .takeIf { it != 0 } ?: 0
        } else {
            com.reddy.vittify.utils.IconResolutionUtils.getSafeResId(context, category.iconResId, 0)
        }
    }

    Surface(
        onClick = onClick,
        shape = com.reddy.vittify.presentation.ui.theme.VittifyShapes.pill,
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconResId != 0) {
                Icon(
                    painter = painterResource(id = iconResId),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.Unspecified
                )
            }
            Text(
                text = subcategory?.name ?: category.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

