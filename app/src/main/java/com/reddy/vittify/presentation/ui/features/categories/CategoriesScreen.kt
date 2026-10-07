package com.reddy.vittify.presentation.ui.features.categories

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.reddy.vittify.presentation.navigation.LocalBottomNavPadding
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.reddy.vittify.R
import com.reddy.vittify.data.database.entity.CategoryEntity
import com.reddy.vittify.data.database.entity.CategoryType
import com.reddy.vittify.data.database.entity.SubcategoryEntity
import com.reddy.vittify.domain.catalogue.CategoryItemCatalogue
import com.reddy.vittify.presentation.effects.overScrollVertical
import com.reddy.vittify.presentation.effects.rememberOverscrollFlingBehavior
import com.reddy.vittify.presentation.ui.components.CategoryItem
import com.reddy.vittify.presentation.ui.components.CategorySelectionSheet
import com.reddy.vittify.presentation.ui.components.CustomTitleTopAppBar
import com.reddy.vittify.presentation.ui.components.ListItemPosition
import com.reddy.vittify.presentation.ui.components.toShape
import com.reddy.vittify.presentation.ui.components.DeleteCategoryDialog
import com.reddy.vittify.presentation.ui.components.GenericTypeSwitcher
import com.reddy.vittify.presentation.ui.components.SearchBarBox
import com.reddy.vittify.presentation.ui.components.SectionHeader
import com.reddy.vittify.presentation.ui.icons.Bag
import com.reddy.vittify.presentation.ui.icons.Box2
import com.reddy.vittify.presentation.ui.icons.CloseCircle
import com.reddy.vittify.presentation.ui.icons.Iconax
import com.reddy.vittify.presentation.ui.theme.Dimensions
import com.reddy.vittify.presentation.ui.theme.Spacing
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalHazeApi::class
)
@Composable
fun CategoriesScreen(
    onNavigateBack: () -> Unit,
    categoriesViewModel: CategoriesViewModel = hiltViewModel(),
    blurEffects: Boolean
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val categories by categoriesViewModel.filteredCategories.collectAsStateWithLifecycle()
    val searchQuery by categoriesViewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTab by categoriesViewModel.selectedTab.collectAsStateWithLifecycle()
    val categoryCounts by categoriesViewModel.categoryCounts.collectAsStateWithLifecycle()
    val addCategoryInitialType by categoriesViewModel.addCategoryInitialType.collectAsStateWithLifecycle()

    val categoryTabs = remember {
        listOf(
            CategoryType.EXPENSE,
            CategoryType.INCOME,
            CategoryType.CREDIT,
            CategoryType.INVESTMENT
        )
    }
    
    // Use local TextFieldValue state for SearchBarBox to handle cursor position
    var searchInput by remember { mutableStateOf(TextFieldValue(text = searchQuery)) }

    // Sync input with ViewModel state (in case it changes externally)
    LaunchedEffect(searchQuery) {
        if (searchQuery != searchInput.text) {
            searchInput = searchInput.copy(text = searchQuery)
        }
    }

    val showAddEditDialog by categoriesViewModel.showAddEditDialog.collectAsStateWithLifecycle()
    val editingCategory by categoriesViewModel.editingCategory.collectAsStateWithLifecycle()
    val snackbarMessage by categoriesViewModel.snackbarMessage.collectAsStateWithLifecycle()
    val subcategories by categoriesViewModel.subcategories.collectAsStateWithLifecycle()
    val showSubcategoryDialog by categoriesViewModel.showSubcategoryDialog.collectAsStateWithLifecycle()
    val editingSubcategory by categoriesViewModel.editingSubcategory.collectAsStateWithLifecycle()

    val showDeleteConfirmation by categoriesViewModel.showDeleteConfirmation.collectAsStateWithLifecycle()
    val categoryToDelete by categoriesViewModel.categoryToDelete.collectAsStateWithLifecycle()
    val hasTransactions by categoriesViewModel.hasTransactions.collectAsStateWithLifecycle()
    val showMigrationSheet by categoriesViewModel.showMigrationSheet.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }
    val lazyListState = rememberLazyListState()
    var showFloatingLabel by remember { mutableStateOf(true) }
    val expenseLabels = listOf(stringResource(R.string.search_fruits), stringResource(R.string.search_shopping), stringResource(R.string.search_fitness), stringResource(R.string.search_sports))
    val labels = remember(selectedTab, expenseLabels) {
        when (selectedTab) {
            CategoryType.INCOME -> listOf("Search Salary", "Search Bonus", "Search Freelance", "Search Dividends")
            CategoryType.INVESTMENT -> listOf("Search Stocks", "Search Mutual Funds", "Search Gold", "Search Crypto")
            CategoryType.CREDIT -> listOf("Search EMI", "Search Credit Bill", "Search Loan", "Search Repayment")
            CategoryType.EXPENSE -> expenseLabels
        }
    }
    var currentLabelIndex by remember { mutableIntStateOf(0) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(labels) {
        currentLabelIndex = 0
        while (true) {
            delay(3000)
            currentLabelIndex = (currentLabelIndex + 1) % labels.size
        }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }.collect { firstVisibleItem ->
            // Show the label only when the list is scrolled to the top
            showFloatingLabel = firstVisibleItem == 0
        }
    }
    LaunchedEffect(selectedTab) {
        lazyListState.scrollToItem(0)
    }
    // Show snackbar messages
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
                categoriesViewModel.clearSnackbarMessage()
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CustomTitleTopAppBar(
                title = stringResource(R.string.categories),
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = true,
                hasActionButton = false,
                navigationContent = { NavigationContent(onNavigateBack) },
                actionContent = {}
            )
        },
        floatingActionButton = {
            val fabContainerColor =  MaterialTheme.colorScheme.primaryContainer
            val fabContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ExtendedFloatingActionButton(
                onClick = { categoriesViewModel.showAddDialog(selectedTab) },
                expanded = showFloatingLabel,
                icon = { Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_category)) },
                text = { Text(text = stringResource(R.string.add_category)) },
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = (LocalBottomNavPadding.current - 16.dp).coerceAtLeast(0.dp))
                    .height(48.dp),
                containerColor = fabContainerColor,
                contentColor = fabContentColor
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = LocalBottomNavPadding.current),
                snackbar = {
                    Snackbar(
                        snackbarData = it,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.large,
                    )
                }
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.Padding.content)
                    .padding(top = 8.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                GenericTypeSwitcher(
                    selectedIndex = categoryTabs.indexOf(selectedTab).coerceAtLeast(0),
                    onIndexChange = { index -> categoriesViewModel.selectTab(categoryTabs[index]) },
                    options = categoryTabs.map { stringResource(it.labelRes) },
                    selectedColors = listOf(
                        MaterialTheme.colorScheme.error,
                        Color(0xFF388E3C),
                        Color(0xFF1976D2),
                        Color(0xFF00796B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                SearchBarBox(
                    searchQuery = searchInput,
                    onSearchQueryChange = {
                        searchInput = it
                        categoriesViewModel.updateSearchQuery(it.text)
                    },
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
                    leadingIcon = { },
                    trailingIcon = if (searchInput.text.isNotEmpty()) {
                        {
                            IconButton(onClick = {
                                searchInput = TextFieldValue("")
                                categoriesViewModel.updateSearchQuery("")
                            }) {
                                Icon(
                                    Iconax.CloseCircle,
                                    contentDescription = stringResource(R.string.clear_search)
                                )
                            }
                        }
                    } else {
                        {}
                    }
                )
            }

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    val targetIndex = categoryTabs.indexOf(targetState)
                    val initialIndex = categoryTabs.indexOf(initialState)
                    val direction = if (targetIndex >= initialIndex) 1 else -1
                    (slideInHorizontally(
                        animationSpec = spring(
                            dampingRatio = 0.62f,
                            stiffness = 340f
                        ),
                        initialOffsetX = { fullWidth -> direction * (fullWidth / 4) }
                    ) + fadeIn(
                        animationSpec = spring(dampingRatio = 0.62f, stiffness = 340f)
                    )).togetherWith(
                        slideOutHorizontally(
                            animationSpec = spring(
                                dampingRatio = 0.62f,
                                stiffness = 340f
                            ),
                            targetOffsetX = { fullWidth -> -direction * (fullWidth / 4) }
                        ) + fadeOut(
                            animationSpec = spring(dampingRatio = 0.62f, stiffness = 340f)
                        )
                    )
                },
                label = "CategoryPageTransition",
                modifier = Modifier.fillMaxSize()
            ) { currentTab ->
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .overScrollVertical()
                        .hazeSource(state = hazeState),
                    flingBehavior = rememberOverscrollFlingBehavior { lazyListState },
                    contentPadding = PaddingValues(
                        start = Dimensions.Padding.content,
                        end = Dimensions.Padding.content,
                        top = Spacing.sm,
                        bottom = 0.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    if (categories.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Iconax.Box2,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Text(
                                        text = if (searchQuery.isBlank()) {
                                            stringResource(R.string.no_categories_found, stringResource(currentTab.labelRes))
                                        } else {
                                            stringResource(R.string.no_matching_categories_found)
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (searchQuery.isBlank()) {
                                        androidx.compose.material3.FilledTonalButton(
                                            onClick = { categoriesViewModel.showAddDialog(currentTab) },
                                            shape = MaterialTheme.shapes.large
                                        ) {
                                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(stringResource(R.string.add_category))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(1.5.dp)
                            ) {
                                categories.forEachIndexed { index, category ->
                                    val categorySubcategories = subcategories[category.id] ?: emptyList()
                                    val q = searchQuery.trim()
                                    val displayedSubs = if (q.isNotBlank()) {
                                        val matchingSubs = categorySubcategories.filter { sub ->
                                            sub.name.contains(q, ignoreCase = true) ||
                                            CategoryItemCatalogue.matchesSubcategory(category.name, sub.name, q)
                                        }
                                        if (matchingSubs.isNotEmpty()) {
                                            matchingSubs
                                        } else {
                                            val catMatches = category.name.contains(q, ignoreCase = true) ||
                                                    CategoryItemCatalogue.matchesCategory(category.name, q)
                                            if (catMatches) categorySubcategories else emptyList()
                                        }
                                    } else {
                                        categorySubcategories
                                    }
                                    SwipeableCategoryItem(
                                        category = category,
                                        subcategories = displayedSubs,
                                        position = ListItemPosition.from(index, categories.size),
                                        onEdit = { categoriesViewModel.showEditDialog(category) },
                                        onDelete = { categoriesViewModel.deleteCategory(category) },
                                        onAddSubcategory = {
                                            categoriesViewModel.showAddSubcategoryDialog(category.id)
                                        },
                                        onEditSubcategory = { categoriesViewModel.showEditSubcategoryDialog(it) },
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(100.dp + LocalBottomNavPadding.current)) }
                }
            }
    }
}

    // Add/Edit Category Bottom Sheet
    if (showAddEditDialog) {
        com.reddy.vittify.presentation.ui.components.VittifyModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { categoriesViewModel.hideDialog() },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            EditCategorySheet(
                category = editingCategory,
                initialCategoryType = addCategoryInitialType,
                onDismiss = { categoriesViewModel.hideDialog() },
                onSave = { name, description, color, iconResId, iconName, categoryType ->
                    categoriesViewModel.saveCategory(name, description, color, iconResId, iconName, categoryType)
                },
                onReset = if (editingCategory?.isSystem == true) {
                    { categoryId -> categoriesViewModel.resetCategory(categoryId) }
                } else null,
                onDelete = if (editingCategory != null && !editingCategory!!.isSystem) {
                    { categoriesViewModel.deleteCategory(editingCategory!!) }
                } else null
            )
        }
    }

    // Edit Subcategory Bottom Sheet
    if (showSubcategoryDialog) {
        val currentCategory =
            editingSubcategory?.categoryId?.let { catId -> categories.find { it.id == catId } }

        com.reddy.vittify.presentation.ui.components.VittifyModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { categoriesViewModel.hideSubcategoryDialog() },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            EditSubcategorySheet(
                subcategory = editingSubcategory,
                categoryColor = currentCategory?.color ?: "#757575",
                categoryIconResId = currentCategory?.let { cat ->
                    if (cat.iconName.isNotEmpty()) {
                        com.reddy.vittify.utils.IconResolutionUtils.nameToResId(context, cat.iconName)
                            .takeIf { it != 0 } ?: cat.iconResId
                    } else {
                        cat.iconResId
                    }
                } ?: R.drawable.type_food_dining,
                onDismiss = { categoriesViewModel.hideSubcategoryDialog() },
                onSave = { name, iconResId, iconName, color ->
                    categoriesViewModel.saveSubcategory(name, iconResId, iconName, color)
                },
                onReset = if (editingSubcategory?.isSystem == true) {
                    { subcategoryId -> categoriesViewModel.resetSubcategory(subcategoryId) }
                } else null,
                onDelete = if (editingSubcategory != null) {
                    { subcategoryId ->
                        editingSubcategory?.let { categoriesViewModel.deleteSubcategory(it) }
                        categoriesViewModel.hideSubcategoryDialog()
                    }
                } else null
            )
        }
    }

    // Deletion Confirmation Dialog
    if (showDeleteConfirmation && categoryToDelete != null) {
        val categoryName = categoryToDelete?.name ?: "this category"
        DeleteCategoryDialog(
            hasTransactions = hasTransactions,
            categoryName = categoryName,
            onMoveOthers = { categoriesViewModel.showMigrationSheet() },
            onMoveDefault = { categoriesViewModel.confirmDelete(moveToMiscellaneous = true) },
            onDismiss = { categoriesViewModel.dismissDeleteConfirmation() },
            onDelete = { categoriesViewModel.confirmDelete() },
            blurEffects = blurEffects,
            hazeState = hazeState
        )
    }

    // Category Migration Bottom Sheet
    if (showMigrationSheet && categoryToDelete != null) {
        com.reddy.vittify.presentation.ui.components.VittifyModalBottomSheet(
            onDismissRequest = { categoriesViewModel.hideMigrationSheet() },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            Box(modifier = Modifier.padding(bottom = Spacing.xxl)) {
                CategorySelectionSheet(
                    categories = categories.filter { it.id != (categoryToDelete?.id ?: -1) },
                    subcategoriesMap = subcategories,
                    onSelectionComplete = { newCategory, newSubcategory ->
                        categoriesViewModel.confirmMigrationToCategory(newCategory, newSubcategory)
                    },
                    onDismiss = { categoriesViewModel.hideMigrationSheet() },
                    onAddSubcategory = { category ->
                        categoriesViewModel.showAddSubcategoryDialog(category.id)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableCategoryItem(
    category: CategoryEntity,
    subcategories: List<SubcategoryEntity>,
    position: ListItemPosition = ListItemPosition.Single,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddSubcategory: () -> Unit,
    onEditSubcategory: (SubcategoryEntity) -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {SwipeToDismissBoxValue.EndToStart -> {
                if (!category.isSystem) {
                    onDelete()
                }
                false // Don't dismiss until confirmed
            }
                else -> false
            }
        }
    )

    val itemShape = position.toShape()

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { if (!category.isSystem) {
            val color by
            animateColorAsState(
                when (dismissState.dismissDirection) {SwipeToDismissBoxValue.EndToStart ->
                    MaterialTheme.colorScheme.error
                    else -> Color.Transparent },
                label = "background color"
            )
            Box(modifier = Modifier
                .fillMaxSize()
                .background(
                    color = color,
                    shape = itemShape
                )
                .padding(horizontal = Dimensions.Padding.content),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                    Icon(
                        imageVector = Iconax.Bag,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onError
                    )
                }
            }
        } },
        content = {
            CategoryItem(
                category = category,
                subcategories = subcategories,
                onClick = onEdit,
                onAddSubcategory = onAddSubcategory,
                onEditSubcategory = onEditSubcategory,
                shape = itemShape,
            )
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = !category.isSystem
    )
}



@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NavigationContent(onNavigateBack: () -> Unit) {
    Box(
        modifier = Modifier
            .animateContentSize()
            .padding(start = 16.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onNavigateBack,
                ),
    ) {
        IconButton(
            onClick = onNavigateBack,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onBackground
            ),
            shapes =  IconButtonDefaults.shapes()
        ) {
            Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "Back Button",
            )
        }
    }
}
