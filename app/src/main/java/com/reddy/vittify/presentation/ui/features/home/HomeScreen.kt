package com.reddy.vittify.presentation.ui.features.home

import android.app.Activity
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import com.reddy.vittify.data.nlp.NlpParsingMode
import com.reddy.vittify.data.preferences.NavigationBarStyle
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.ui.text.style.TextOverflow
import com.reddy.vittify.data.sync.ViewMode
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import com.reddy.vittify.presentation.ui.components.CompactCoupleViewModeSwitcher
import com.reddy.vittify.presentation.ui.theme.LocalVittifyTokens
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import com.reddy.vittify.presentation.ui.theme.VittifySpacing
import com.reddy.vittify.presentation.ui.theme.VittifySurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.reddy.vittify.R
import com.reddy.vittify.data.database.entity.CategoryEntity
import com.reddy.vittify.data.database.entity.SubcategoryEntity
import com.reddy.vittify.data.database.entity.SubscriptionEntity
import com.reddy.vittify.data.preferences.HomeWidget
import com.reddy.vittify.presentation.ui.features.profile.FinancialOverviewCard
import com.reddy.vittify.presentation.effects.overScrollVertical
import com.reddy.vittify.presentation.effects.rememberOverscrollFlingBehavior
import com.reddy.vittify.utils.capitalizeFirst
import com.reddy.vittify.presentation.navigation.AccountDetail
import com.reddy.vittify.presentation.navigation.AddTransaction
import com.reddy.vittify.presentation.navigation.AiSettings
import com.reddy.vittify.presentation.navigation.Analytics
import com.reddy.vittify.presentation.navigation.Categories
import com.reddy.vittify.presentation.navigation.CloudBackup
import com.reddy.vittify.presentation.navigation.CoupleTracker
import com.reddy.vittify.presentation.navigation.ManageAccounts
import com.reddy.vittify.presentation.navigation.NotificationSettings
import com.reddy.vittify.presentation.navigation.PdfReport
import com.reddy.vittify.presentation.navigation.Profile
import com.reddy.vittify.presentation.navigation.Rules
import com.reddy.vittify.presentation.navigation.SmsSettings
import com.reddy.vittify.presentation.navigation.SyncSms
import com.reddy.vittify.presentation.navigation.UnrecognizedSms
import com.reddy.vittify.presentation.navigation.safeNavigate
import com.reddy.vittify.presentation.ui.components.AccountCarousel
import com.reddy.vittify.presentation.ui.components.AccountCarouselSkeleton
import com.reddy.vittify.presentation.ui.components.BalanceCard
import com.reddy.vittify.presentation.ui.components.BudgetCarousel
import com.reddy.vittify.presentation.ui.components.TransactionPlatterSkeleton
import com.reddy.vittify.presentation.ui.components.CurrencySelectionBottomSheet
import com.reddy.vittify.presentation.ui.components.CustomTitleTopAppBar
import com.reddy.vittify.presentation.ui.components.GreetingCard
import com.reddy.vittify.presentation.ui.components.ProfileAvatar
import com.reddy.vittify.presentation.ui.components.HeatmapWidget
import com.reddy.vittify.presentation.ui.components.ListItem
import com.reddy.vittify.presentation.ui.components.ListItemPosition
import com.reddy.vittify.presentation.ui.components.LoadingCircle
import com.reddy.vittify.presentation.ui.components.PreferenceSwitch
import com.reddy.vittify.presentation.ui.components.SectionHeader
import com.reddy.vittify.presentation.ui.components.SubscriptionIconsStack
import com.reddy.vittify.presentation.ui.components.TransactionItem
import com.reddy.vittify.presentation.ui.components.toShape
import com.reddy.vittify.presentation.ui.features.settings.appearance.ThemeViewModel
import com.reddy.vittify.presentation.ui.icons.Convertshape2
import com.reddy.vittify.presentation.ui.icons.Gallery
import com.reddy.vittify.presentation.ui.icons.Iconax
import com.reddy.vittify.presentation.ui.icons.ReceiptItem
import com.reddy.vittify.presentation.ui.icons.RefreshCircle
import com.reddy.vittify.presentation.ui.icons.Search
import com.reddy.vittify.presentation.ui.icons.Setting2
import com.reddy.vittify.presentation.ui.theme.Dimensions
import com.reddy.vittify.presentation.ui.theme.Spacing
import com.reddy.vittify.presentation.ui.theme.expense_dark
import com.reddy.vittify.presentation.ui.theme.expense_light
import com.reddy.vittify.presentation.ui.theme.income_dark
import com.reddy.vittify.presentation.ui.theme.income_light
import com.reddy.vittify.utils.CurrencyFormatter
import com.reddy.vittify.utils.bottomFade
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeDefaults.tint
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.runtime.saveable.rememberSaveable
import com.reddy.vittify.presentation.ui.features.globalsearch.GlobalSearchViewModel
import com.reddy.vittify.presentation.ui.features.globalsearch.GlobalSearchResultsOverlay
import com.reddy.vittify.presentation.ui.features.globalsearch.HomeSearchBar
import com.reddy.vittify.presentation.navigation.Settings
import com.reddy.vittify.presentation.navigation.Home

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class,
    ExperimentalHazeApi::class
)
@Composable
fun SharedTransitionScope.HomeScreen(
    homeViewModel: HomeViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
    globalSearchViewModel: GlobalSearchViewModel = hiltViewModel(),
    navController: NavController,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToTransactions: () -> Unit = {},
    onNavigateToTransactionsWithSearch: () -> Unit = {},
    onNavigateToSubscriptions: () -> Unit = {},
    onNavigateToBudgets: (Long?) -> Unit = {},
    onNavigateToBudgetHistory: (Long) -> Unit = {},
    onTransactionClick: (Long, String) -> Unit = { _, _ -> },
    onFullResyncClick: () -> Unit = {},
    animatedContentScope: AnimatedContentScope? = null,
) {

    val uiState by homeViewModel.uiState.collectAsState()
    val themeUiState by themeViewModel.themeUiState.collectAsState()
    val deletedTransaction by homeViewModel.deletedTransaction.collectAsState()
    val categoriesMap by homeViewModel.categoriesMap.collectAsStateWithLifecycle()
    val subcategoriesMap by homeViewModel.subcategoriesMap.collectAsStateWithLifecycle()
    val accountsMap by homeViewModel.accountsMap.collectAsStateWithLifecycle()
    val homeWidgets by homeViewModel.homeWidgets.collectAsStateWithLifecycle()
    val homeShortcuts by homeViewModel.homeShortcuts.collectAsStateWithLifecycle()
    val nlpDraft by homeViewModel.nlpDraft.collectAsState()
    val searchUiState by globalSearchViewModel.uiState.collectAsStateWithLifecycle()
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val activity = LocalActivity.current
    val hazeState = remember { HazeState()}
    val blurEffects = themeUiState.blurEffects

    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isNormalNav = themeUiState.navigationBarStyle == NavigationBarStyle.NORMAL
    
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    val context = LocalContext.current
    
    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        globalSearchViewModel.clearSearch()
    }

    BackHandler(enabled = !isSearchActive) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 2000) {
            (context as? Activity)?.finish()
        } else {
            lastBackPressTime = currentTime
            Toast.makeText(context, context.getString(R.string.press_back_again_to_close), Toast.LENGTH_SHORT).show()
        }
    }
    val scope = rememberCoroutineScope()

    val showMoreBottomSheet by homeViewModel.showMoreBottomSheet.collectAsState()
    val showEditWidgetsSheet by homeViewModel.showEditWidgetsSheet.collectAsState()

    // Haptic feedback
    val view = LocalView.current


    // Check for app updates and reviews when the screen is first displayed
    LaunchedEffect(Unit) {
        // Refresh account balances to ensure proper currency conversion
        homeViewModel.refreshAccountBalances()

        // Check for app updates
        activity?.let {
            val componentActivity = it as ComponentActivity
            homeViewModel.checkForAppUpdate(
                activity = componentActivity,
                snackbarHostState = snackbarHostState,
                scope = scope
            )

            // Check for in-app review eligibility
            homeViewModel.checkForInAppReview(componentActivity)
        }
    }

    // ensures changes from ManageAccountsScreen are reflected immediately
    DisposableEffect(Unit) {
        homeViewModel.refreshHiddenAccounts()
        onDispose {}
    }

    // Handle delete undo snackbar
    LaunchedEffect(deletedTransaction) {
        deletedTransaction?.let { transaction ->
            // Clear the state immediately to prevent re-triggering
            homeViewModel.clearDeletedTransaction()

            scope.launch {
                val result =
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.transaction_deleted),
                        actionLabel = context.getString(R.string.undo),
                        duration = SnackbarDuration.Short
                    )
                if (result == SnackbarResult.ActionPerformed) {
                    // Pass the transaction directly since state is already
                    // cleared
                    homeViewModel.undoDeleteTransaction(transaction)
                }
            }
        }
    }

    // Clear snackbar when navigating away
    DisposableEffect(Unit) { onDispose { snackbarHostState.currentSnackbarData?.dismiss() } }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val lazyListState = rememberLazyListState()
    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            val start = scrollBehavior.state.heightOffset
            val target = scrollBehavior.state.heightOffsetLimit
            if (start != target && target < 0f) {
                androidx.compose.animation.core.animate(
                    initialValue = start,
                    targetValue = target,
                    animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)
                ) { value, _ ->
                    scrollBehavior.state.heightOffset = value
                }
            }
        } else if (lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0) {
            val start = scrollBehavior.state.heightOffset
            if (start < 0f) {
                androidx.compose.animation.core.animate(
                    initialValue = start,
                    targetValue = 0f,
                    animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)
                ) { value, _ ->
                    scrollBehavior.state.heightOffset = value
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            CustomTitleTopAppBar(
                title = stringResource(R.string.vittify_title),
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = false,
                extraInfoCard = {
                    GreetingCard(
                        userName = uiState.userName,
                        profileImageUri = uiState.profileImageUri,
                        profileBackgroundColor = uiState.profileBackgroundColor,
                        unreadUpdatesCount = uiState.unreadUpdatesCount,
                        onProfileClick = onNavigateToSettings,
                        showAvatar = false,
                        showMoreOption = isNormalNav,
                        onNotificationClick = { navController.safeNavigate(NotificationSettings()) },
                        onUpdatesClick = { navController.safeNavigate(UnrecognizedSms) },
                        onMoreClick = { homeViewModel.openMoreBottomSheet() },
                        coupleViewModeContent = if (uiState.isCoupleTrackingEnabled) {
                            {
                                CompactCoupleViewModeSwitcher(
                                    activeMode = uiState.activeViewMode,
                                    partnerName = uiState.partnerName,
                                    onModeSelected = { homeViewModel.setViewMode(it) },
                                    userName = uiState.userName
                                )
                            }
                        } else null
                    )
                },
                searchBar = { isCollapsed ->
                    HomeSearchBar(
                        query = searchUiState.query,
                        onQueryChange = { globalSearchViewModel.onQueryChange(it) },
                        isSearchActive = isSearchActive,
                        onActiveChange = { active ->
                            isSearchActive = active
                            if (!active) {
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                                globalSearchViewModel.clearSearch()
                            }
                        },
                        isCollapsed = isCollapsed,
                        onClear = { globalSearchViewModel.clearSearch() },
                        modifier = Modifier.fillMaxWidth(),
                        focusRequester = searchFocusRequester,
                        showMoreOption = isNormalNav,
                        onMoreClick = { homeViewModel.openMoreBottomSheet() }
                    )
                },
                navigationContent = {
                    ProfileAvatar(
                        modifier = Modifier.fillMaxSize(),
                        profileImageUri = uiState.profileImageUri,
                        profileBackgroundColor = uiState.profileBackgroundColor,
                        onClick = onNavigateToSettings
                    )
                }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = {
                    Snackbar(
                        snackbarData = it,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.large,
                    )
                }
            )
        }

    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            // NLP Quick Add: observe draft and navigate when parsing completes
            nlpDraft?.let { draft ->
                LaunchedEffect(draft) {
                    homeViewModel.clearNlpDraft()
                    navController.safeNavigate(
                        com.reddy.vittify.presentation.navigation.AddTransaction(
                            nlpAmount = draft.amount.ifBlank { null },
                            nlpMerchant = draft.merchant.ifBlank { null },
                            nlpType = draft.type.name,
                            nlpBankName = draft.bankName.ifBlank { null },
                            nlpNotes = draft.notes.ifBlank { null },
                            nlpCategory = draft.category.ifBlank { null },
                            nlpSubcategory = draft.subcategory.ifBlank { null },
                            nlpDate = draft.date?.toString(),
                        )
                    )
                }
            }

            // NLP Error Dialog: prompt user with error and let them change model
            if (!uiState.nlpError.isNullOrBlank()) {
                AlertDialog(
                    onDismissRequest = { homeViewModel.clearNlpError() },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    title = {
                        Text(
                            text = stringResource(R.string.ai_error_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    text = {
                        Text(
                            text = uiState.nlpError ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                homeViewModel.clearNlpError()
                                navController.safeNavigate(AiSettings(targetOptionId = "ai-model"))
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.change_model))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { homeViewModel.clearNlpError() }) {
                            Text(stringResource(R.string.dismiss))
                        }
                    },
                    shape = VittifyShapes.dialog
                )
            }

            PullToRefreshBox(
                isRefreshing = uiState.isScanning,
                onRefresh = {
                    homeViewModel.refreshAccountBalances()
                    homeViewModel.scanSmsMessages(forceResync = false)
                },
                state = pullToRefreshState,
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullToRefreshState,
                        isRefreshing = uiState.isScanning,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = paddingValues.calculateTopPadding())
                    )
                },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState)
                    .overScrollVertical(),
                flingBehavior = rememberOverscrollFlingBehavior { lazyListState },
                contentPadding =
                    PaddingValues(
                        top = VittifySpacing.scaledStandard + paddingValues.calculateTopPadding(),
                        bottom = 0.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(VittifySpacing.scaledStandard)
            ) {

                homeWidgets.forEach { widgetModel ->
                    if (widgetModel.isVisible) {
                        when (widgetModel.widget) {
                            HomeWidget.QUICK_ADD -> {
                                item(key = "quick_add") {
                                    QuickAddCard(
                                        isProcessing = uiState.nlpIsProcessing,
                                        parsingMode = uiState.nlpParsingMode,
                                        errorMessage = uiState.nlpError,
                                        onNavigateToAiSettings = {
                                            homeViewModel.clearNlpError()
                                            navController.safeNavigate(AiSettings(targetOptionId = "ai-model"))
                                        },
                                        onDismissError = {
                                            homeViewModel.clearNlpError()
                                        },
                                        onParseOffline = { input ->
                                            homeViewModel.parseNaturalLanguageWithRegex(input)
                                        },
                                        onSubmit = { input ->
                                            homeViewModel.parseNaturalLanguageTransaction(input)
                                        }
                                    )
                                }
                            }
                            HomeWidget.NETWORTH_SUMMARY -> {
                                item(key = "net_worth") {
                                    NetworthSummaryCards(
                                        uiState = uiState,
                                        onCurrencySelected = {
                                            homeViewModel.selectCurrency(it)
                                        },
                                        blurEffects = blurEffects,
                                        hazeState = hazeState
                                    )
                                }
                            }
                            HomeWidget.FINANCIAL_OVERVIEW -> {
                                item(key = "financial_overview") {
                                    FinancialOverviewCard(
                                        netWorth = uiState.totalBalance,
                                        income = uiState.currentMonthIncome,
                                        expense = uiState.currentMonthExpenses,
                                        activeSubscriptions = uiState.upcomingSubscriptions.size,
                                        baseCurrency = uiState.selectedCurrency,
                                        modifier = Modifier.padding(horizontal = VittifySpacing.scaledStandard)
                                    )
                                }
                            }
                            HomeWidget.TRANSACTION_HEATMAP -> {
                                item(key = "transaction_heatmap") {
                                    HeatmapWidget(
                                        data = uiState.transactionHeatmap,
                                        blurEffects = blurEffects,
                                        hazeState = hazeState
                                    )
                                }
                            }
                            HomeWidget.BUDGET_CAROUSEL -> {
                                if (uiState.activeBudgets.isNotEmpty()) {
                                    item(key = "budget_carousel") {
                                        var lastBudgetClickTime by remember { mutableLongStateOf(0L) }
                                        BudgetCarousel(
                                            budgets = uiState.activeBudgets,
                                            onBudgetClick = {
                                                val currentTime = System.currentTimeMillis()
                                                if (currentTime - lastBudgetClickTime > 500) {
                                                    lastBudgetClickTime = currentTime
                                                    onNavigateToBudgets(it)
                                                }
                                            },
                                            onEditClick = {
                                                val currentTime = System.currentTimeMillis()
                                                if (currentTime - lastBudgetClickTime > 500) {
                                                    lastBudgetClickTime = currentTime
                                                    onNavigateToBudgets(it)
                                                }
                                            },
                                            onHistoryClick = onNavigateToBudgetHistory,
                                            animatedVisibilityScope = animatedContentScope,
                                        )
                                    }
                                }
                            }
                            HomeWidget.ACCOUNT_CAROUSEL -> {
                                if (uiState.isLoading) {
                                    item(key = "account_carousel_skeleton") {
                                        AccountCarouselSkeleton()
                                    }
                                } else if (uiState.creditCards.isNotEmpty() ||
                                    uiState.accountBalances.isNotEmpty()
                                ) {
                                    item(key = "account_carousel") {
                                        AccountCarousel(
                                            creditCards = uiState.creditCards,
                                            bankAccounts = uiState.accountBalances,
                                            onAccountClick = { bankName, accountLast4 ->
                                                navController.safeNavigate(
                                                    AccountDetail(
                                                        bankName = bankName,
                                                        accountLast4 = accountLast4
                                                    )
                                                )
                                            },
                                            animatedContentScope = animatedContentScope,
                                            blurEffects = blurEffects,
                                            hazeState = hazeState
                                        )
                                    }
                                }
                            }
                            HomeWidget.UPCOMING_SUBSCRIPTIONS -> {
                                if (uiState.upcomingSubscriptions.isNotEmpty()) {
                                    item(key = "upcoming_subscriptions") {
                                        val cardModifier = Modifier.padding(
                                            start = VittifySpacing.scaledStandard,
                                            end = VittifySpacing.scaledStandard,
                                        )

                                        if (animatedContentScope != null) {
                                            UpcomingSubscriptionsCard(
                                                subscriptions = uiState.upcomingSubscriptions,
                                                totalAmount = uiState.upcomingSubscriptionsTotal,
                                                currency = uiState.upcomingSubscriptionsCurrency,
                                                categoriesMap = categoriesMap,
                                                subcategoriesMap = subcategoriesMap,
                                                onClick = onNavigateToSubscriptions,
                                                blurEffects = blurEffects,
                                                hazeState = hazeState,
                                                modifier = cardModifier.sharedBounds(
                                                    rememberSharedContentState(key = "upcoming_subscriptions_card"),
                                                    animatedVisibilityScope = animatedContentScope,
                                                    boundsTransform = { _, _ ->
                                                        spring(
                                                            stiffness = Spring.StiffnessLow,
                                                            dampingRatio = Spring.DampingRatioNoBouncy
                                                        )
                                                    },
                                                    resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                                                        contentScale = ContentScale.Fit,
                                                        alignment = Alignment.Center
                                                    ),
                                                    renderInOverlayDuringTransition = false
                                                )
                                                    .skipToLookaheadSize()
                                            )
                                        } else {
                                            UpcomingSubscriptionsCard(
                                                subscriptions = uiState.upcomingSubscriptions,
                                                totalAmount = uiState.upcomingSubscriptionsTotal,
                                                currency = uiState.upcomingSubscriptionsCurrency,
                                                categoriesMap = categoriesMap,
                                                subcategoriesMap = subcategoriesMap,
                                                onClick = onNavigateToSubscriptions,
                                                blurEffects = blurEffects,
                                                hazeState = hazeState,
                                                modifier = cardModifier
                                            )
                                        }
                                    }
                                }
                            }
                            HomeWidget.RECENT_TRANSACTIONS -> {
                                item(key = "recent_transactions") {
                                    val containerColor = MaterialTheme.colorScheme.surfaceContainer
                                    Column {
                                        Surface(
                                            modifier = Modifier
                                                .padding(horizontal = VittifySpacing.scaledStandard)
                                                .fillMaxWidth()
                                                .clip(VittifyShapes.platter)
                                                .then(
                                                    if (blurEffects) Modifier.hazeEffect(
                                                        state = hazeState,
                                                        block = fun HazeEffectScope.() {
                                                            style = HazeDefaults.style(
                                                                backgroundColor = Color.Transparent,
                                                                tint = HazeDefaults.tint(containerColor),
                                                                blurRadius = 20.dp,
                                                                noiseFactor = -1f,
                                                            )
                                                            blurredEdgeTreatment = BlurredEdgeTreatment.Unbounded
                                                        }
                                                    ) else Modifier
                                                ),
                                            shape = VittifyShapes.platter,
                                            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = VittifySurface.platterAlpha),
                                            border = VittifySurface.platterBorder(),
                                            contentColor = MaterialTheme.colorScheme.onSurface,
                                        ) {
                                            Column {
                                                SectionHeader(
                                                    title = stringResource(R.string.recent),
                                                    action = {
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            // Search button
                                                            TextButton(
                                                                onClick = onNavigateToTransactionsWithSearch,
                                                                modifier = Modifier.then(
                                                                    if (animatedContentScope != null) {
                                                                        Modifier.sharedBounds(
                                                                            rememberSharedContentState(key = "transactions_search"),
                                                                            animatedVisibilityScope = animatedContentScope,
                                                                            boundsTransform = { _, _ ->
                                                                                spring(
                                                                                    stiffness = Spring.StiffnessLow,
                                                                                    dampingRatio = Spring.DampingRatioNoBouncy
                                                                                )
                                                                            },
                                                                            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                                                                                contentScale = ContentScale.None,
                                                                                alignment = Alignment.Center
                                                                            ),
                                                                            renderInOverlayDuringTransition = false
                                                                        )
                                                                            .skipToLookaheadSize()
                                                                    } else Modifier
                                                                )
                                                            ) {
                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                    Icon(
                                                                        imageVector = Iconax.Search,
                                                                        contentDescription = stringResource(R.string.search_transactions),
                                                                        modifier = Modifier.size(Dimensions.Icon.small),
                                                                        tint = MaterialTheme.colorScheme.primary
                                                                    )
                                                                    Spacer(modifier = Modifier.width(4.dp))
                                                                    Text(stringResource(R.string.search))
                                                                }

                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.padding(
                                                        start = 16.dp,
                                                        end = 8.dp,
                                                    )
                                                )

                                                if (uiState.isLoading) {
                                                    TransactionPlatterSkeleton(
                                                        count = 3,
                                                        modifier = Modifier.padding(bottom = Spacing.sm)
                                                    )
                                                } else if (uiState.recentTransactions.isEmpty()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(Dimensions.Padding.card),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            ) {
                                                            Icon(
                                                                imageVector = Iconax.ReceiptItem,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(48.dp),
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                            Spacer(modifier = Modifier.height(Spacing.md))
                                                            Text(
                                                                text = stringResource(R.string.no_transactions_yet),
                                                                style = MaterialTheme.typography.bodyLarge,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    uiState.recentTransactions.forEachIndexed { index, transaction ->
                                                        val categoryEntity = categoriesMap[transaction.category]
                                                        val subcategoryEntity =
                                                            if (categoryEntity != null && transaction.subcategory != null) {
                                                                subcategoriesMap[transaction.subcategory]
                                                            } else null

                                                        val accountEntity = transaction.accountId?.let { accountsMap[it] }
                                                        TransactionItem(
                                                            transaction = transaction,
                                                            categoryEntity = categoryEntity,
                                                            subcategoryEntity = subcategoryEntity,
                                                            accountIconResId = accountEntity?.iconResId ?: 0,
                                                            accountIconName = accountEntity?.iconName,
                                                            accountColorHex = accountEntity?.color,
                                                            convertedAmount = uiState.convertedAmounts[transaction.id],
                                                            mainCurrency = uiState.baseCurrency,
                                                            isSplit = uiState.splitTransactionIds.contains(transaction.id),
                                                            onClick = {
                                                                onTransactionClick(
                                                                    transaction.id,
                                                                    "transaction_${transaction.id}"
                                                                )
                                                            },
                                                            modifier = Modifier.fillMaxWidth(),
                                                            animatedContentScope = animatedContentScope,
                                                            sharedElementKey = "transaction_${transaction.id}",
                                                            isPartner = uiState.isCoupleTrackingEnabled &&
                                                                uiState.activeViewMode == ViewMode.COMBINED &&
                                                                transaction.ownerId.isNotBlank() &&
                                                                uiState.myUserId.isNotBlank() &&
                                                                transaction.ownerId != uiState.myUserId,
                                                            partnerBadgeLabel = uiState.partnerName?.ifBlank { null } ?: "Partner",
                                                            useSeamlessPlatterStyle = true
                                                        )

                                                        if (index < uiState.recentTransactions.lastIndex) {
                                                            HorizontalDivider(
                                                                modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                                                                thickness = 0.5.dp,
                                                                color = VittifySurface.dividerColor()
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(Spacing.sm))
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            TextButton(
                                                onClick = onNavigateToTransactions,
                                                modifier = Modifier
                                                    .then(
                                                        if (animatedContentScope != null) {
                                                            Modifier.sharedBounds(
                                                                rememberSharedContentState(key = "transactions_screen"),
                                                                animatedVisibilityScope = animatedContentScope,
                                                                boundsTransform = { _, _ ->
                                                                    spring(
                                                                        stiffness = Spring.StiffnessLow,
                                                                        dampingRatio = Spring.DampingRatioNoBouncy
                                                                    )
                                                                },
                                                                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                                                                    contentScale = ContentScale.None,
                                                                    alignment = Alignment.Center
                                                                ),
                                                                renderInOverlayDuringTransition = false
                                                            )
                                                                .skipToLookaheadSize()
                                                        } else Modifier
                                                    )
                                                    .clip(VittifyShapes.pill)
                                                    .then(
                                                        if (blurEffects) Modifier.hazeEffect(
                                                            state = hazeState,
                                                            block = fun HazeEffectScope.() {
                                                                style = HazeDefaults.style(
                                                                    backgroundColor = Color.Transparent,
                                                                    tint = HazeDefaults.tint(containerColor),
                                                                    blurRadius = 20.dp,
                                                                    noiseFactor = -1f,
                                                                )
                                                                blurredEdgeTreatment = BlurredEdgeTreatment.Unbounded
                                                            }
                                                        ) else Modifier
                                                    )
                                                    .height(28.dp),
                                                colors = ButtonDefaults.textButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.primary,
                                                    containerColor = if (blurEffects)
                                                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(0.7f)
                                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                                ),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.view_all),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                                    modifier = Modifier.padding(horizontal = Spacing.md)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            HomeWidget.SHORTCUTS -> {
                                item(key = "quick_shortcuts") {
                                    HomeShortcutsWidget(
                                        activeShortcuts = homeShortcuts,
                                        onShortcutClick = { item ->
                                            when (item) {
                                                HomeShortcutItem.PROFILE -> navController.safeNavigate(Profile)
                                                HomeShortcutItem.BACKUP_SYNC -> navController.safeNavigate(CloudBackup())
                                                HomeShortcutItem.ACCOUNTS -> navController.safeNavigate(ManageAccounts)
                                                HomeShortcutItem.CATEGORIES -> navController.safeNavigate(Categories)
                                                HomeShortcutItem.BUDGETS -> onNavigateToBudgets(null)
                                                HomeShortcutItem.RULES -> navController.safeNavigate(Rules)
                                                HomeShortcutItem.ANALYTICS -> navController.safeNavigate(Analytics)
                                                HomeShortcutItem.SMS -> navController.safeNavigate(SmsSettings())
                                                HomeShortcutItem.REPORTS -> navController.safeNavigate(PdfReport)
                                                HomeShortcutItem.COUPLE -> navController.safeNavigate(CoupleTracker)
                                            }
                                        },
                                        onUpdateShortcuts = homeViewModel::updateHomeShortcuts,
                                        blurEffects = blurEffects,
                                        hazeState = hazeState
                                    )
                                }
                            }
                        }
                    }
                }

                item{
                    Spacer(Modifier.height(200.dp)) //Extra space for better scroll
                }
            }
            }

            // Global Search Results Overlay
            AnimatedVisibility(
                visible = isSearchActive,
                enter = fadeIn(animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)),
                exit = fadeOut(animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow))
            ) {
                GlobalSearchResultsOverlay(
                    uiState = searchUiState,
                    onAccountClick = { bankName, accountLast4 ->
                        isSearchActive = false
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        globalSearchViewModel.clearSearch()
                        navController.safeNavigate(
                            AccountDetail(bankName = bankName, accountLast4 = accountLast4)
                        )
                    },
                    onSettingClick = { setting ->
                        isSearchActive = false
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        globalSearchViewModel.clearSearch()
                        if (setting.destination != null) {
                            navController.safeNavigate(setting.destination)
                        } else {
                            navController.safeNavigate(
                                Settings(targetSettingId = setting.targetSettingId)
                            )
                        }
                    },
                    onTransactionClick = { transactionId ->
                        isSearchActive = false
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        globalSearchViewModel.clearSearch()
                        onTransactionClick(transactionId, "HOME")
                    },
                    onPageClick = { destination ->
                        isSearchActive = false
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        globalSearchViewModel.clearSearch()
                        if (destination != Home) {
                            navController.safeNavigate(destination)
                        }
                    },
                    hazeState = hazeState,
                    blurEffects = blurEffects,
                    contentPadding = PaddingValues(top = paddingValues.calculateTopPadding())
                )
            }

            // More Options BottomSheet
            if (showMoreBottomSheet) {
                com.reddy.vittify.presentation.ui.components.VittifyModalBottomSheet(
                    onDismissRequest = { homeViewModel.closeMoreBottomSheet() },
                    sheetState = rememberModalBottomSheetState(),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.more_options),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(vertical = Spacing.sm)
                                .fillMaxWidth()
                        )

                        // Add Transaction Option
                        ListItem(
                            headline = { Text(stringResource(R.string.add_transaction)) },
                            leading = {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                homeViewModel.closeMoreBottomSheet()
                                navController.safeNavigate(AddTransaction(initialTab = 0))
                            },
                            useDefaultBorder = false,
                            listColor = Color.Transparent,
                            padding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                        )

                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        // Edit Widgets Option
                        ListItem(
                            headline = { Text(stringResource(R.string.edit_widgets)) },
                            leading = {
                                Icon(
                                    imageVector = Iconax.Convertshape2,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                homeViewModel.closeMoreBottomSheet()
                                homeViewModel.openEditWidgetsSheet()
                            },
                            useDefaultBorder = false,
                            listColor = Color.Transparent,
                            padding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                        )

                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        // Sync SMS Option
                        ListItem(
                            headline = { Text(stringResource(R.string.sync_sms)) },
                            leading = {
                                Icon(
                                    imageVector = Icons.Rounded.Sync,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                homeViewModel.closeMoreBottomSheet()
                                navController.safeNavigate(SyncSms())
                            },
                            useDefaultBorder = false,
                            listColor = Color.Transparent,
                            padding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                        )

                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        // Sync Accounts Option
                        ListItem(
                            headline = { Text(stringResource(R.string.sync_accounts)) },
                            leading = {
                                Icon(
                                    imageVector = Icons.Rounded.Sync,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                homeViewModel.closeMoreBottomSheet()
                                homeViewModel.syncDevices()
                            },
                            useDefaultBorder = false,
                            listColor = Color.Transparent,
                            padding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                        )

                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        // Manage Accounts Option
                        ListItem(
                            headline = { Text(stringResource(R.string.title_accounts)) },
                            leading = {
                                Icon(
                                    imageVector = Icons.Rounded.AccountBalance,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                homeViewModel.closeMoreBottomSheet()
                                navController.safeNavigate(ManageAccounts)
                            },
                            useDefaultBorder = false,
                            listColor = Color.Transparent,
                            padding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                        )

                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        // Settings Option
                        ListItem(
                            headline = { Text(stringResource(R.string.settings)) },
                            leading = {
                                Icon(
                                    imageVector = Iconax.Setting2,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                homeViewModel.closeMoreBottomSheet()
                                onNavigateToSettings()
                            },
                            useDefaultBorder = false,
                            listColor = Color.Transparent,
                            padding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                        )
                    }
                }
            }

            // Breakdown Dialog
            if (uiState.showBreakdownDialog) {
                BreakdownDialog(
                    currentMonthIncome = uiState.currentMonthIncome,
                    currentMonthExpenses = uiState.currentMonthExpenses,
                    currentMonthTotal = uiState.currentMonthTotal,
                    lastMonthIncome = uiState.lastMonthIncome,
                    lastMonthExpenses = uiState.lastMonthExpenses,
                    lastMonthTotal = uiState.lastMonthTotal,
                    onDismiss = { homeViewModel.hideBreakdownDialog() }
                )
            }

            // Edit Widgets Sheet
            if (showEditWidgetsSheet) {
                EditWidgetsSheet(
                    onDismissRequest = { homeViewModel.closeEditWidgetsSheet() },
                    sheetState = rememberModalBottomSheetState(),
                    widgets = homeWidgets,
                    onToggleVisibility = homeViewModel::toggleHomeWidgetVisibility,
                    onReorder = homeViewModel::updateWidgetsOrder
                )
            }
        }
    }

}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BreakdownDialog(
    currentMonthIncome: BigDecimal,
    currentMonthExpenses: BigDecimal,
    currentMonthTotal: BigDecimal,
    lastMonthIncome: BigDecimal,
    lastMonthExpenses: BigDecimal,
    lastMonthTotal: BigDecimal,
    onDismiss: () -> Unit
) {
    val now = LocalDate.now()
    val currentPeriod = "${now.month.name.lowercase().capitalizeFirst()} 1-${now.dayOfMonth}"
    val lastMonth = now.minusMonths(1)
    val lastPeriod = "${lastMonth.month.name.lowercase().capitalizeFirst()} 1-${now.dayOfMonth}"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md), // Reduced horizontal padding for wider modal
            shape = VittifyShapes.dialog,
            border = VittifySurface.platterBorder(),
            colors =
                CardDefaults.cardColors(
                    containerColor = VittifySurface.platterContainerColor()
                )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Dimensions.Padding.card),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Title
                Text(
                    text = stringResource(R.string.calculation_breakdown),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // Current Period Section
                Text(
                    text = currentPeriod,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                BreakdownRow(
                    label = stringResource(R.string.income),
                    amount = currentMonthIncome,
                    isIncome = true
                )

                BreakdownRow(
                    label = stringResource(R.string.expenses),
                    amount = currentMonthExpenses,
                    isIncome = false
                )

                HorizontalDivider()

                BreakdownRow(
                    label = stringResource(R.string.net_balance),
                    amount = currentMonthTotal,
                    isIncome = currentMonthTotal >= BigDecimal.ZERO,
                    isBold = true
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                // Last Period Section
                Text(
                    text = lastPeriod,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                BreakdownRow(
                    label = stringResource(R.string.income),
                    amount = lastMonthIncome,
                    isIncome = true
                )

                BreakdownRow(
                    label = stringResource(R.string.expenses),
                    amount = lastMonthExpenses,
                    isIncome = false
                )

                HorizontalDivider()

                BreakdownRow(
                    label = stringResource(R.string.net_balance),
                    amount = lastMonthTotal,
                    isIncome = lastMonthTotal >= BigDecimal.ZERO,
                    isBold = true
                )

                // Formula explanation
                Spacer(modifier = Modifier.height(Spacing.sm))
                Card(
                    shape = VittifyShapes.input,
                    border = VittifySurface.platterBorder(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = LocalVittifyTokens.current.surfaceOpacity)
                        ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.breakdown_formula_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(Spacing.sm),
                        textAlign = TextAlign.Center
                    )
                }

                // Close button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) { Text(stringResource(R.string.close)) }
            }
        }
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    amount: BigDecimal,
    isIncome: Boolean,
    isBold: Boolean = false
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = "${if (isIncome) "+" else "-"}${CurrencyFormatter.formatCurrency(amount.abs())}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color =
                if (isIncome) {
                    if (!isSystemInDarkTheme()) income_light else income_dark
                } else {
                    if (!isSystemInDarkTheme()) expense_light else expense_dark
                }
        )
    }
}

@OptIn(ExperimentalHazeApi::class)
@Composable
private fun UpcomingSubscriptionsCard(
    modifier: Modifier = Modifier,
    subscriptions: List<SubscriptionEntity>,
    totalAmount: BigDecimal,
    currency: String,
    categoriesMap: Map<String, CategoryEntity> = emptyMap(),
    subcategoriesMap: Map<String, SubcategoryEntity> = emptyMap(),
    onClick: () -> Unit = {},
    blurEffects: Boolean,
    hazeState: HazeState = remember { HazeState() }
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainer

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(VittifyShapes.platter)
            .then(
                if (blurEffects) Modifier.hazeEffect(
                    state = hazeState,
                    block = fun HazeEffectScope.() {
                        style = HazeDefaults.style(
                            backgroundColor = Color.Transparent,
                            tint = HazeDefaults.tint(containerColor),
                            blurRadius = 20.dp,
                            noiseFactor = -1f,
                        )
                        blurredEdgeTreatment = BlurredEdgeTreatment.Unbounded
                    }
                ) else Modifier
            ),
        shape = VittifyShapes.platter,
        colors = CardDefaults.cardColors(
            containerColor = VittifySurface.platterContainerColor()
        ),
        border = VittifySurface.platterBorder(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimensions.Padding.content),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.padding(start = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.subscriptions_count_format, subscriptions.size),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = Dimensions.Alpha.subtitle
                    )
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text =
                            CurrencyFormatter.formatCurrency(
                                totalAmount,
                                currency
                            ).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text =
                            stringResource(R.string.per_month).uppercase(),
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = Dimensions.Alpha.subtitle
                        ),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

            }

            SubscriptionIconsStack(
                subscriptions = subscriptions,
                iconSize = 38.dp,
                modifier = Modifier.padding(end = Spacing.sm),
                borderColor = MaterialTheme.colorScheme.surfaceContainerLow,
                categoriesMap = categoriesMap,
                subcategoriesMap = subcategoriesMap
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NetworthSummaryCards(
    uiState: HomeUiState,
    onCurrencySelected: (String) -> Unit = {},
    blurEffects: Boolean,
    hazeState: HazeState = remember { HazeState() },
) {
    var showCurrencySheet by remember { mutableStateOf(false) }

    if (showCurrencySheet) {
        CurrencySelectionBottomSheet(
            selectedCurrency = uiState.selectedCurrency,
            availableCurrencies = uiState.availableCurrencies,
            onCurrencySelected = {
                onCurrencySelected(it)
                showCurrencySheet = false
            },
            onDismiss = { showCurrencySheet = false }
        )
    }

    val context = LocalContext.current
    val effectiveDisplayName = if (uiState.isCoupleTrackingEnabled) {
        when (uiState.activeViewMode) {
            com.reddy.vittify.data.sync.ViewMode.PARTNER -> uiState.partnerName?.ifBlank { null } ?: "Partner"
            com.reddy.vittify.data.sync.ViewMode.COMBINED -> "Shared"
            com.reddy.vittify.data.sync.ViewMode.PERSONAL -> uiState.userName
        }
    } else {
        uiState.userName
    }
    val abbreviatedName = remember(effectiveDisplayName) {
        if (effectiveDisplayName.contains(" ")) {
            effectiveDisplayName.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .map { it[0] }
                .joinToString("")
                .uppercase()
        } else if (effectiveDisplayName.length > 4) {
            effectiveDisplayName.filter { it !in "aeiouAEIOU" }.take(4).uppercase().ifEmpty { 
                effectiveDisplayName.take(4).uppercase() 
            }
        } else {
            effectiveDisplayName.uppercase()
        }
    }

    val dateRangeLabel = remember(uiState.balanceHistory) {
        if (uiState.balanceHistory.size < 2) ""
        else {
            val start = uiState.balanceHistory.first().timestamp.toLocalDate()
            val end = uiState.balanceHistory.last().timestamp.toLocalDate()
            val days = ChronoUnit.DAYS.between(start, end)
            context.getString(R.string.last_days_format, days)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {

        BalanceCard(
            totalBalance = uiState.totalBalance,
            monthlyChange = uiState.monthlyChange,
            monthlyChangePercent = uiState.monthlyChangePercent,
            spentThisMonth = uiState.currentMonthExpenses,
            currency = uiState.selectedCurrency,
            abbreviatedName = abbreviatedName,
            userName = effectiveDisplayName,
            balanceHistory = uiState.balanceHistory,
            dateRangeLabel = dateRangeLabel,
            thisMonthValue = CurrencyFormatter.formatCurrency(uiState.currentMonthTotal, uiState.selectedCurrency),
            thisYearValue = CurrencyFormatter.formatCurrency(uiState.currentYearTotal, uiState.selectedCurrency),
            availableCurrenciesCount = uiState.availableCurrencies.size,
            onCurrencyClick = { showCurrencySheet = true },
            blurEffects = blurEffects,
            hazeState = hazeState,
            modifier = Modifier.padding(
                start = VittifySpacing.scaledStandard,
                end = VittifySpacing.scaledStandard,
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Quick Add Card — NLP natural language transaction input
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun QuickAddCard(
    isProcessing: Boolean,
    parsingMode: NlpParsingMode? = null,
    errorMessage: String? = null,
    onNavigateToAiSettings: () -> Unit = {},
    onDismissError: () -> Unit = {},
    onParseOffline: ((String) -> Unit)? = null,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val hasText = inputText.isNotBlank()
    var showDisclaimerDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = VittifySpacing.scaledStandard),
        shape = VittifyShapes.platter,
        colors = CardDefaults.cardColors(
            containerColor = VittifySurface.platterContainerColor()
        ),
        border = VittifySurface.platterBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VittifySpacing.scaledStandard, vertical = VittifySpacing.scaled(14.dp)),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header row: icon + title + disclaimer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.quick_add_title),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.quick_add_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = { showDisclaimerDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = stringResource(R.string.quick_add_disclaimer_cd),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Input row
            OutlinedTextField(
                value = inputText,
                onValueChange = {
                    inputText = it
                    if (!errorMessage.isNullOrBlank()) {
                        onDismissError()
                    }
                },
                placeholder = {
                    Text(
                        text = stringResource(R.string.quick_add_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isProcessing,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (hasText && !isProcessing) {
                            onSubmit(inputText)
                        }
                    }
                ),
                trailingIcon = {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (hasText) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    inputText = ""
                                    if (!errorMessage.isNullOrBlank()) {
                                        onDismissError()
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.quick_add_clear_cd),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { onSubmit(inputText) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Send,
                                    contentDescription = stringResource(R.string.quick_add_parse_cd),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                },
                shape = VittifyShapes.input,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.60f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f),
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.30f),
                    disabledBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                )
            )

            // Hint label
            if (isProcessing) {
                val labelText = when (parsingMode) {
                    NlpParsingMode.AI -> stringResource(R.string.quick_add_parsing_ai)
                    NlpParsingMode.REGEX -> stringResource(R.string.quick_add_parsing_regex)
                    null -> stringResource(R.string.quick_add_parsing)
                }
                Text(
                    text = labelText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // Error banner with "Change Model" button
            if (!errorMessage.isNullOrBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(top = 2.dp)
                            )
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = onDismissError,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.close),
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (onParseOffline != null && inputText.isNotBlank()) {
                                TextButton(
                                    onClick = { onParseOffline(inputText) },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                ) {
                                    Text(
                                        text = stringResource(R.string.quick_add_use_offline),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                            Button(
                                onClick = onNavigateToAiSettings,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                shape = VittifyShapes.pill,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.change_model),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDisclaimerDialog) {
        QuickAddTutorialDialog(
            onDismiss = { showDisclaimerDialog = false }
        )
    }
}

