package com.reddy.vittify.presentation.ui.features.budgets

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.reddy.vittify.presentation.navigation.LocalBottomNavPadding
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.reddy.vittify.R
import com.reddy.vittify.data.repository.BudgetWithSpending
import com.reddy.vittify.presentation.effects.overScrollVertical
import com.reddy.vittify.presentation.effects.rememberOverscrollFlingBehavior
import com.reddy.vittify.presentation.ui.components.BudgetCard
import com.reddy.vittify.presentation.ui.components.CustomTitleTopAppBar
import com.reddy.vittify.presentation.ui.components.ListItemPosition
import com.reddy.vittify.presentation.ui.components.toShape
import com.reddy.vittify.presentation.ui.components.LoadingCircle
import com.reddy.vittify.presentation.ui.features.categories.CategoriesViewModel
import com.reddy.vittify.presentation.ui.features.categories.NavigationContent
import com.reddy.vittify.presentation.ui.theme.Spacing
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class,
    ExperimentalMaterial3ExpressiveApi::class, ExperimentalHazeApi::class
)
@Composable
fun SharedTransitionScope.BudgetsScreen(
    onNavigateBack: () -> Unit,
    onBudgetClick: (Long, String?) -> Unit,
    onHistoryClick: (Long) -> Unit = {},
    budgetViewModel: BudgetViewModel = hiltViewModel(),
    categoriesViewModel: CategoriesViewModel = hiltViewModel(),
    animatedContentScope: AnimatedContentScope? = null,
    sharedElementPrefix: Long? = null,
    blurEffects: Boolean
) {
    val uiState by budgetViewModel.uiState.collectAsStateWithLifecycle()
    val editBudgetState by budgetViewModel.editBudgetState.collectAsStateWithLifecycle()
    val categories by categoriesViewModel.categories.collectAsStateWithLifecycle()
    val subcategories by categoriesViewModel.subcategories.collectAsStateWithLifecycle()
    
    var showEditSheet by remember { mutableStateOf(false) }
    var showTypeWizard by remember { mutableStateOf(false) }
    var showTrackWizard by remember { mutableStateOf(false) }
    var editingBudgetId by remember { mutableStateOf<Long?>(null) }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    
    // Edit budget sheet
    if (showEditSheet) {
        com.reddy.vittify.presentation.ui.components.VittifyModalBottomSheet(
            onDismissRequest = { 
                showEditSheet = false
                editingBudgetId = null
                budgetViewModel.clearEditState()
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            EditBudgetSheet(
                budgetState = editBudgetState,
                categories = categories,
                subcategoriesMap = subcategories,
                allAccounts = uiState.allAccounts,
                onAmountChange = budgetViewModel::updateBudgetAmount,
                onNameChange = budgetViewModel::updateBudgetName,
                onStartDateChange = budgetViewModel::updateStartDate,
                onEndDateChange = budgetViewModel::updateEndDate,
                onPeriodTypeChange = budgetViewModel::updatePeriodType,
                onTrackTypeChange = budgetViewModel::updateTrackType,
                onBudgetTypeChange = budgetViewModel::updateBudgetType,
                onAccountIdsChange = budgetViewModel::updateAccountIds,
                onColorChange = budgetViewModel::updateColor,
                onAddCategoryLimit = budgetViewModel::addCategoryLimit,
                onRemoveCategoryLimit = budgetViewModel::removeCategoryLimit,
                onSave = {
                    budgetViewModel.saveBudget(
                        onSuccess = {
                            showEditSheet = false
                            editingBudgetId = null
                            budgetViewModel.clearEditState()
                        },
                        onError = { /* TODO: Show error */ }
                    )
                },
                onDelete = if (editingBudgetId != null) {
                    {
                        budgetViewModel.deleteBudget(
                            budgetId = editingBudgetId!!,
                            onSuccess = {
                                showEditSheet = false
                                editingBudgetId = null
                                budgetViewModel.clearEditState()
                            },
                            onError = { /* TODO: Show error */ }
                        )
                    }
                } else null,
                onDismiss = {
                    showEditSheet = false
                    editingBudgetId = null
                    budgetViewModel.clearEditState()
                },
                blurEffects = blurEffects
            )
        }
    }

    // Budget Type Wizard
    if (showTypeWizard) {
        com.reddy.vittify.presentation.ui.components.VittifyModalBottomSheet(
            onDismissRequest = { showTypeWizard = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            BudgetTypeSelectionSheet(
                onTypeSelected = { type ->
                    scope.launch {
                        sheetState.hide()
                        budgetViewModel.initNewBudget()
                        budgetViewModel.updateBudgetType(type)
                        showTypeWizard = false
                        showTrackWizard = true
                    }
                },
                onDismiss = { showTypeWizard = false }
            )
        }
    }

    // Budget Track Type Wizard
    if (showTrackWizard) {
        com.reddy.vittify.presentation.ui.components.VittifyModalBottomSheet(
            onDismissRequest = { showTrackWizard = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            BudgetTrackTypeSelectionSheet(
                onTrackTypeSelected = { trackType ->
                    scope.launch {
                        sheetState.hide()
                        budgetViewModel.updateTrackType(trackType)
                        showTrackWizard = false
                        editingBudgetId = null
                        showEditSheet = true
                    }
                },
                onDismiss = { showTrackWizard = false }
            )
        }
    }
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val lazyListState = rememberLazyListState()
    val hazeState = remember { HazeState() }

    var showFloatingLabel by remember { mutableStateOf(true) }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }.collect { firstVisibleItem ->
            // Show the label only when the list is scrolled to the top
            showFloatingLabel = firstVisibleItem == 0
        }
    }


    val sharedModifier = if (animatedContentScope != null && sharedElementPrefix != null) {
        Modifier.sharedBounds(
            rememberSharedContentState(key = "budget_card_$sharedElementPrefix"),
            animatedVisibilityScope = animatedContentScope,
            boundsTransform = { _, _ ->
                spring(
                    stiffness = Spring.StiffnessLow,
                    dampingRatio = Spring.DampingRatioNoBouncy
                )
            },
            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                contentScale = ContentScale.Inside,
                alignment = Alignment.Center
            ),
        ).skipToLookaheadSize()
    } else {
        Modifier
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .then(sharedModifier),
        topBar = {
            CustomTitleTopAppBar(
                title = stringResource(R.string.budgets),
                hazeState = hazeState,
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hasBackButton = true,
                navigationContent = {
                    NavigationContent { onNavigateBack() }
                }
            )
        },
        floatingActionButton = {
            val fabContainerColor = MaterialTheme.colorScheme.primaryContainer
            val fabContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ExtendedFloatingActionButton(
                onClick = {
                    showTypeWizard = true
                },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.new_budget)) },
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = (LocalBottomNavPadding.current - 16.dp).coerceAtLeast(0.dp))
                    .height(48.dp),
                containerColor = fabContainerColor,
                contentColor = fabContentColor
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .padding(top = paddingValues.calculateTopPadding())
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingCircle()
                        }
                    }
                    
                    uiState.budgets.isEmpty() -> {
                        EmptyBudgetsContent(
                            onCreateBudget = {
                                showTypeWizard = true
                            }
                        )
                    }
                    
                    else -> {
                        var lastClickTime by remember { mutableLongStateOf(0L) }
                        BudgetsList(
                            lazyListState = lazyListState,
                            paddingValues = PaddingValues(bottom = paddingValues.calculateBottomPadding()),
                            budgets = uiState.budgets,
                            onEditClick = { budgetId ->
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastClickTime > 500) { // Debounce 500ms
                                    lastClickTime = currentTime
                                    val budget = uiState.budgets.find { it.budget.id == budgetId }?.budget
                                    if (budget != null) {
                                        budgetViewModel.initEditBudget(budget)
                                        editingBudgetId = budgetId
                                        showEditSheet = true
                                    }
                                }
                            },
                            onBudgetClick = onBudgetClick,
                            onHistoryClick = onHistoryClick,
                            animatedContentScope = animatedContentScope,
                            sharedElementPrefix = sharedElementPrefix
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.BudgetsList(
    budgets: List<BudgetWithSpending>,
    onEditClick: (Long) -> Unit,
    onBudgetClick: (Long, String?) -> Unit,
    onHistoryClick: (Long) -> Unit = {},
    paddingValues: PaddingValues,
    lazyListState: LazyListState,
    animatedContentScope: AnimatedContentScope? = null,
    sharedElementPrefix: Long? = null
) {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .overScrollVertical(),
        flingBehavior = rememberOverscrollFlingBehavior { lazyListState },
        contentPadding = PaddingValues(
            start = Spacing.md,
            end = Spacing.md,
            top = Spacing.md,
            bottom = 100.dp + paddingValues.calculateBottomPadding() + LocalBottomNavPadding.current
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(1.5.dp)
            ) {
                budgets.forEachIndexed { index, budgetWithSpending ->
                    BudgetCard(
                        budgetWithSpending = budgetWithSpending,
                        shape = ListItemPosition.from(index, budgets.size).toShape(),
                        onClick = { onBudgetClick(budgetWithSpending.budget.id, "budget_card_${budgetWithSpending.budget.id}") },
                        onHistoryClick = onHistoryClick,
                        animatedVisibilityScope = animatedContentScope,
                        sharedElementKey = if (sharedElementPrefix != null) null else "budget_card_${budgetWithSpending.budget.id}"
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyBudgetsContent(
    onCreateBudget: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🎯",
            style = MaterialTheme.typography.displayLarge
        )
        
        Spacer(modifier = Modifier.height(Spacing.md))
        
        Text(
            text = stringResource(R.string.no_budgets_yet),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(Spacing.xs))
        
        Text(
            text = stringResource(R.string.create_first_budget_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.xl)
        )
        
        Spacer(modifier = Modifier.height(Spacing.lg))
        
        Button(onClick = onCreateBudget) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.create_budget))
        }
    }
}
