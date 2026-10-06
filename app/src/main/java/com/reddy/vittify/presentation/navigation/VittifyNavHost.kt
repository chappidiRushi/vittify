package com.reddy.vittify.presentation.navigation

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import com.reddy.vittify.presentation.ui.components.FloatingProfileSwitcher
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.reddy.vittify.R
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.reddy.vittify.data.preferences.NavigationBarStyle
import com.reddy.vittify.presentation.navigation.LocalBottomNavPadding
import com.reddy.vittify.presentation.ui.features.accounts.AccountDetailScreen
import com.reddy.vittify.presentation.ui.features.accounts.AccountAuditScreen
import com.reddy.vittify.presentation.ui.features.accounts.AddAccountScreen
import com.reddy.vittify.presentation.ui.features.accounts.ManageAccountsScreen
import com.reddy.vittify.presentation.ui.features.add.AddScreen
import com.reddy.vittify.presentation.ui.features.analytics.AnalyticsScreen
import com.reddy.vittify.presentation.ui.features.budgets.BudgetDetailScreen
import com.reddy.vittify.presentation.ui.features.budgets.BudgetHistoryScreen
import com.reddy.vittify.presentation.ui.features.budgets.BudgetsScreen
import com.reddy.vittify.presentation.ui.features.categories.CategoriesScreen
import com.reddy.vittify.presentation.ui.features.home.HomeScreen
import com.reddy.vittify.presentation.ui.features.home.HomeViewModel
import com.reddy.vittify.presentation.ui.features.onboarding.OnBoardingScreen
import com.reddy.vittify.presentation.ui.features.profile.ProfileScreen
import com.reddy.vittify.presentation.ui.features.settings.SettingsScreen
import com.reddy.vittify.presentation.ui.features.settings.customization.CustomizationScreen
import com.reddy.vittify.presentation.ui.features.settings.about.AboutScreen
import com.reddy.vittify.presentation.ui.features.settings.about.FaqScreen
import com.reddy.vittify.presentation.ui.features.settings.about.GuidesScreen
import com.reddy.vittify.presentation.ui.features.settings.about.PrivacyPolicyScreen
import com.reddy.vittify.presentation.ui.features.settings.about.TermsOfServiceScreen
import com.reddy.vittify.presentation.ui.features.settings.about.CreditsScreen
import com.reddy.vittify.presentation.ui.features.settings.pdfreport.PdfReportScreen
import com.reddy.vittify.presentation.ui.features.settings.about.LicensesScreen
import com.reddy.vittify.presentation.ui.features.settings.currency.CurrencySettingsScreen
import com.reddy.vittify.presentation.ui.features.settings.appearance.AppearanceScreen
import com.reddy.vittify.presentation.ui.features.settings.appearance.ThemeViewModel
import com.reddy.vittify.presentation.ui.features.settings.applock.AppLockScreen
import com.reddy.vittify.presentation.ui.features.settings.dataprivacy.DataPrivacyScreen
import com.reddy.vittify.presentation.ui.features.settings.archived.ManageArchivedTransactionsScreen
import com.reddy.vittify.presentation.ui.features.settings.cloudbackup.BackupSyncScreen
import com.reddy.vittify.presentation.ui.features.sync.p2p.P2pSyncScreen
import com.reddy.vittify.presentation.ui.features.couple.CoupleTrackerScreen
import com.reddy.vittify.presentation.ui.features.settings.developer.DeveloperScreen
import com.reddy.vittify.presentation.ui.features.splash.SplashScreen
import com.reddy.vittify.presentation.ui.features.settings.notifications.NotificationScreen
import com.reddy.vittify.presentation.ui.features.settings.rules.CreateRuleScreen
import com.reddy.vittify.presentation.ui.features.settings.rules.RulesScreen
import com.reddy.vittify.presentation.ui.features.settings.ai.AiSettingsScreen
import com.reddy.vittify.presentation.ui.features.settings.rules.RulesViewModel
import com.reddy.vittify.presentation.ui.features.settings.sms.SMSScreen
import com.reddy.vittify.presentation.ui.features.settings.transactions.TransactionSettingsScreen
import com.reddy.vittify.presentation.ui.features.settings.unrecognized.UnrecognizedSmsScreen
import com.reddy.vittify.presentation.ui.features.settings.webhooks.WebhookEditorScreen
import com.reddy.vittify.presentation.ui.features.sync.SyncSmsScreen
import com.reddy.vittify.presentation.ui.features.settings.webhooks.WebhooksScreen
import com.reddy.vittify.presentation.ui.features.subscriptions.SubscriptionsScreen
import com.reddy.vittify.presentation.ui.features.transactions.ExportTransactionsDialog
import com.reddy.vittify.presentation.ui.features.transactions.TransactionDetailScreen
import com.reddy.vittify.presentation.ui.features.transactions.TransactionsScreen
import com.reddy.vittify.presentation.ui.features.transactions.TransactionsViewModel
import com.reddy.vittify.presentation.ui.icons.Iconax
import com.reddy.vittify.presentation.ui.icons.Search
import com.reddy.vittify.presentation.ui.icons.ImportArrow01
import com.reddy.vittify.presentation.ui.icons.Convertshape2
import com.reddy.vittify.presentation.ui.icons.Setting2
import com.reddy.vittify.presentation.ui.icons.Gallery
import com.reddy.vittify.presentation.ui.theme.Dimensions
import com.reddy.vittify.presentation.ui.theme.Spacing
import com.reddy.vittify.presentation.ui.theme.VittifyShapes
import com.reddy.vittify.presentation.ui.theme.VittifySurface
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import com.reddy.vittify.presentation.ui.components.PlayfulBackgroundCanvas
import com.reddy.vittify.presentation.ui.theme.LocalBaseBackgroundColor

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalHazeApi::class)
@Composable
fun VittifyNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: Any = Home,
    onEditComplete: () -> Unit = {}
) {
    // Use a stable start destination
    val stableStartDestination = remember { startDestination }
    
    // Get theme settings for bottom nav style
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val themeUiState by themeViewModel.themeUiState.collectAsState()
    
    // Track current destination for bottom nav visibility
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    
    val isSettingsRoute = SETTINGS_ROUTES.any { qualifiedName ->
        currentRoute?.contains(qualifiedName ?: "") == true
    }
    // Check if current route is in bottom nav routes
    val showBottomNav = BOTTOM_NAV_ROUTES.any { qualifiedName ->
        currentRoute?.contains(qualifiedName ?: "") == true
    } || (themeUiState.navigationBarStyle == NavigationBarStyle.NORMAL && isSettingsRoute)

    val homeViewModel: HomeViewModel = hiltViewModel()
    val transactionsViewModel: TransactionsViewModel = hiltViewModel()
    val homeUiState by homeViewModel.uiState.collectAsState()
    val transactionsUiState by transactionsViewModel.uiState.collectAsState()
    val smsScanWorkInfo by homeViewModel.smsScanWorkInfo.collectAsState()
    val view = LocalView.current

    // State for full resync confirmation dialog
    var showFullResyncDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    val isHomeScreen = currentRoute?.contains(Home::class.qualifiedName ?: "") == true
    val isAnalyticsScreen = currentRoute?.contains(Analytics::class.qualifiedName ?: "") == true
    val isTransactionsScreen = currentRoute?.contains(Transactions::class.qualifiedName ?: "") == true
    val isAddTransactionScreen = currentRoute?.contains(AddTransaction::class.qualifiedName ?: "") == true
    val isSubscriptionsScreen = currentRoute?.contains(Subscriptions::class.qualifiedName ?: "") == true
    val isBudgetDetailScreen = currentRoute?.contains(BudgetDetail::class.qualifiedName ?: "") == true
    val isBudgetsScreen = currentRoute?.contains(Budgets::class.qualifiedName ?: "") == true
    val isManageAccountsScreen = currentRoute?.contains(ManageAccounts::class.qualifiedName ?: "") == true

    val showFloatingProfileSwitcher = homeUiState.isCoupleTrackingEnabled &&
        !themeUiState.profileSwitcherInFooter &&
        !isHomeScreen &&
        (showBottomNav || isBudgetsScreen || isSubscriptionsScreen || isManageAccountsScreen)

    val isFloatingNav = themeUiState.navigationBarStyle == NavigationBarStyle.FLOATING
    val hideFabsForFloatingNav = isFloatingNav && (isHomeScreen || isTransactionsScreen)
    val showFloatingFab = isFloatingNav && (isHomeScreen || isTransactionsScreen || isAnalyticsScreen)

    val hazeState = remember { HazeState() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LocalBaseBackgroundColor.current)
    ) {
        if (themeUiState.isPlayfulAnimationEnabled) {
            PlayfulBackgroundCanvas(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState),
                style = themeUiState.backgroundStyle,
                backgroundOpacity = themeUiState.backgroundOpacity
            )
        }
        val bottomNavPadding = if (showBottomNav && themeUiState.navigationBarStyle == NavigationBarStyle.NORMAL) 96.dp else 0.dp
        CompositionLocalProvider(LocalBottomNavPadding provides bottomNavPadding) {
            SharedTransitionLayout {
                NavHost(
                    navController = navController,
                    startDestination = stableStartDestination,
                    modifier = Modifier.fillMaxSize().hazeSource(hazeState),
                ) {
                // Splash Screen
                composable<Splash>(
                    enterTransition = VittifyTransitions.noneEnter,
                    exitTransition = VittifyTransitions.noneExit,
                    popEnterTransition = VittifyTransitions.noneEnter,
                    popExitTransition = VittifyTransitions.noneExit
                ) {
                    SplashScreen(
                        currentAppIcon = themeUiState.currentAppIcon,
                        onSplashFinished = {
                            val target = if (themeUiState.isOnboardingFinished) Home else OnBoarding
                            navController.safeNavigate(target) {
                                popUpTo(Splash) { inclusive = true }
                            }
                        }
                    )
                }

                // App Lock Screen
                composable<AppLock>(
                    enterTransition = VittifyTransitions.noneEnter,
                    exitTransition = VittifyTransitions.noneExit,
                    popEnterTransition = VittifyTransitions.noneEnter,
                    popExitTransition = VittifyTransitions.noneExit
                ) {
                    AppLockScreen(
                        onUnlocked = {
                            navController.safeNavigate(Home) {
                                popUpTo(AppLock) { inclusive = true }
                            }
                        }
                    )
                }

                // Onboarding Screen
                composable<OnBoarding>(
                    enterTransition = VittifyTransitions.noneEnter,
                    exitTransition = VittifyTransitions.noneExit,
                    popEnterTransition = VittifyTransitions.noneEnter,
                    popExitTransition = VittifyTransitions.noneExit
                ) {
                    OnBoardingScreen(
                        onOnBoardingComplete = {
                            navController.safeNavigate(Home) {
                                popUpTo(OnBoarding) { inclusive = true }
                            }
                        },
                        onRestoreBackup = {
                            navController.safeNavigate(CloudBackup(fromOnboarding = true))
                        },
                        onNavigateToTerms = {
                            navController.safeNavigate(TermsOfService)
                        },
                        onNavigateToPrivacy = {
                            navController.safeNavigate(PrivacyPolicy)
                        }
                    )
                }

                /* BOTTOM NAV SCREENS ---- */
                // Home Screen
                composable<Home>(
                    enterTransition = VittifyTransitions.bottomNavEnterTransition,
                    exitTransition = VittifyTransitions.bottomNavExitTransition,
                    popEnterTransition = VittifyTransitions.bottomNavPopEnterTransition,
                    popExitTransition = VittifyTransitions.bottomNavPopExitTransition
                ) {
                    HomeScreen(
                        homeViewModel = homeViewModel,
                        themeViewModel = themeViewModel,
                        navController = navController,
                        onNavigateToSettings = { navController.safeNavigate(Settings()) },
                        onNavigateToTransactions = { navController.safeNavigate(Transactions()) },
                        onNavigateToTransactionsWithSearch = {
                            navController.safeNavigate(Transactions(focusSearch = true))
                        },
                        onNavigateToSubscriptions = { navController.safeNavigate(Subscriptions) },
                        onNavigateToBudgets = { id ->
                            if (id != null) {
                                navController.safeNavigate(BudgetDetail(budgetId = id, sharedElementKey = "budget_card_$id"))
                            } else {
                                navController.safeNavigate(Budgets())
                            }
                        },
                        onNavigateToBudgetHistory = { id ->
                            navController.safeNavigate(BudgetHistory(id))
                        },
                        onTransactionClick = { transactionId, key ->
                            navController.safeNavigate(TransactionDetail(transactionId, key))
                        },
                        onFullResyncClick = { showFullResyncDialog = true },
                        animatedContentScope = this@composable,
                    )
                }

                // Analytics Screen
                composable<Analytics>(
                    enterTransition = VittifyTransitions.bottomNavEnterTransition,
                    exitTransition = VittifyTransitions.bottomNavExitTransition,
                    popEnterTransition = VittifyTransitions.bottomNavPopEnterTransition,
                    popExitTransition = VittifyTransitions.bottomNavPopExitTransition
                ) {
                    AnalyticsScreen(
                        onNavigateToTransactions = { category, merchant, period, currency ->
                            navController.safeNavigate(
                                Transactions(
                                    category = category,
                                    merchant = merchant,
                                    period = period,
                                    currency = currency
                                )
                            )
                        },
                        animatedContentScope = this@composable,
                        blurEffects = themeUiState.blurEffects,
                    )
                }


                /* SETTINGS & SUB-SCREENS ---- */
                composable<Settings>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) { backStackEntry ->
                    val targetSettingId = backStackEntry.toRoute<Settings>().targetSettingId
                    SettingsScreen(
                        targetSettingId = targetSettingId,
                        onNavigateBack = { navController.safePopBackStack() },
                        onNavigateToCategories = { navController.safeNavigate(Categories) },
                        onNavigateToManageAccounts = { navController.safeNavigate(ManageAccounts) },
                        onNavigateToRules = { navController.safeNavigate(Rules) },
                        onNavigateToAppearance = { navController.safeNavigate(Appearance()) },
                        onNavigateToProfile = { navController.safeNavigate(Profile) },
                        onNavigateToCustomization = { navController.safeNavigate(Customization()) },
                        onNavigateToSms = { navController.safeNavigate(SmsSettings()) },
                        onNavigateToNotifications = { navController.safeNavigate(NotificationSettings()) },
                        onNavigateToWebhooks = { navController.safeNavigate(Webhooks) },
                        onNavigateToBudgets = { navController.safeNavigate(Budgets()) },
                        onNavigateToDataPrivacy = { navController.safeNavigate(DataPrivacy()) },
                        onNavigateToCloudBackup = { navController.safeNavigate(CloudBackup()) },
                        onNavigateToP2pSync = { navController.safeNavigate(P2pDeviceSync) },
                        onNavigateToCoupleTracker = { navController.safeNavigate(CoupleTracker) },
                        onNavigateToAbout = { navController.safeNavigate(About) },
                        onNavigateToCurrency = { navController.safeNavigate(CurrencySettings()) },
                        onNavigateToPdfReport = { navController.safeNavigate(PdfReport) },
                        onNavigateToAi = { navController.safeNavigate(AiSettings()) },
                        onNavigateToManageArchivedTransactions = { navController.safeNavigate(DataSanitization(initialTab = "archived")) },
                        onNavigateToTransactionSettings = { navController.safeNavigate(TransactionSettings()) },
                        onNavigateToDeveloper = { navController.safeNavigate(DeveloperOptions) },
                        blurEffects = themeUiState.blurEffects
                    )
                }

                composable<Customization>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) { backStackEntry ->
                    val args = backStackEntry.toRoute<Customization>()
                    CustomizationScreen(
                        targetOptionId = args.targetOptionId,
                        onNavigateBack = { navController.safePopBackStack() }
                    )
                }

                composable<AiSettings>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) { backStackEntry ->
                    val args = backStackEntry.toRoute<AiSettings>()
                    AiSettingsScreen(
                        targetOptionId = args.targetOptionId,
                        onNavigateBack = { navController.safePopBackStack() },
                        blurEffects = themeUiState.blurEffects
                    )
                }

                composable<PdfReport>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) {
                    com.reddy.vittify.presentation.ui.features.settings.statementsreports.StatementsReportsScreen(
                        onNavigateBack = { navController.safePopBackStack() },
                        blurEffects = themeUiState.blurEffects
                    )
                }

                composable<SyncSms>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) {
                    SyncSmsScreen(
                        onNavigateBack = { navController.safePopBackStack() },
                        blurEffects = themeUiState.blurEffects
                    )
                }

                composable<Webhooks>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) {
                    WebhooksScreen(
                        onNavigateBack = { navController.safePopBackStack() },
                        onNavigateToEditor = { profileId ->
                            navController.safeNavigate(WebhookEditor(profileId))
                        }
                    )
                }

                composable<WebhookEditor>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) { backStackEntry ->
                    val route = backStackEntry.toRoute<WebhookEditor>()
                    WebhookEditorScreen(
                        profileId = route.profileId,
                        onNavigateBack = { navController.safePopBackStack() }
                    )
                }

                composable<About>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) {
                    AboutScreen(
                        onNavigateBack = { navController.safePopBackStack() },
                        onNavigateToLicenses = { navController.safeNavigate(Licenses) },
                        onNavigateToFaq = { navController.safeNavigate(Faq) },
                        onNavigateToGuides = { navController.safeNavigate(Guides) },
                        onNavigateToPrivacyPolicy = { navController.safeNavigate(PrivacyPolicy) },
                        onNavigateToTermsOfService = { navController.safeNavigate(TermsOfService) },
                        onNavigateToCredits = { navController.safeNavigate(Credits) },
                        blurEffects = themeUiState.blurEffects
                    )
                }

                composable<Licenses>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) {
                    LicensesScreen(
                        onNavigateBack = { navController.safePopBackStack() }
                    )
                }

                composable<Faq>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) {
                    FaqScreen(
                        onNavigateBack = { navController.safePopBackStack() }
                    )
                }

                composable<Guides>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) {
                    GuidesScreen(
                        onNavigateBack = { navController.safePopBackStack() }
                    )
                }

                composable<PrivacyPolicy>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) {
                    PrivacyPolicyScreen(
                        onNavigateBack = { navController.safePopBackStack() }
                    )
                }

                composable<TermsOfService>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) {
                    TermsOfServiceScreen(
                        onNavigateBack = { navController.safePopBackStack() }
                    )
                }

                composable<Credits>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) {
                    CreditsScreen(
                        onNavigateBack = { navController.safePopBackStack() }
                    )
                }

                composable<DataPrivacy>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) { backStackEntry ->
                    val args = backStackEntry.toRoute<DataPrivacy>()
                    DataPrivacyScreen(
                        targetOptionId = args.targetOptionId,
                        onNavigateBack = { navController.safePopBackStack() },
                        onNavigateToAccounts = { navController.safeNavigate(ManageAccounts) },
                        onNavigateToDataSanitization = { tab ->
                            navController.safeNavigate(DataSanitization(tab))
                        },
                        onNavigateToArchivedTransactions = { navController.safeNavigate(DataSanitization(initialTab = "archived")) },
                        blurEffects = themeUiState.blurEffects
                    )
                }

                composable<DataSanitization>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) { backStackEntry ->
                    val route = backStackEntry.toRoute<DataSanitization>()
                    com.reddy.vittify.presentation.ui.features.settings.sanitization.DataSanitizationScreen(
                        onNavigateBack = { navController.safePopBackStack() },
                        onNavigateToAccountAudit = { bankName, last4 ->
                            navController.safeNavigate(AccountAudit(bankName, last4))
                        },
                        initialTabName = route.initialTab,
                        blurEffects = themeUiState.blurEffects
                    )
                }

                composable<ManageArchivedTransactions>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) {
                    com.reddy.vittify.presentation.ui.features.settings.sanitization.DataSanitizationScreen(
                        onNavigateBack = { navController.safePopBackStack() },
                        onNavigateToAccountAudit = { bankName, last4 ->
                            navController.safeNavigate(AccountAudit(bankName, last4))
                        },
                        initialTabName = "archived",
                        blurEffects = themeUiState.blurEffects
                    )
                }

                composable<CloudBackup>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) { backStackEntry ->
                    val args = backStackEntry.toRoute<CloudBackup>()
                    BackupSyncScreen(
                        onNavigateBack = { navController.safePopBackStack() },
                        onNavigateToAccounts = { navController.safeNavigate(ManageAccounts) },
                        onRestoreSuccess = {
                            if (args.fromOnboarding) {
                                navController.safeNavigate(Home) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        },
                        blurEffects = themeUiState.blurEffects
                    )
                }

                composable<P2pDeviceSync>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) {
                    P2pSyncScreen(
                        onNavigateBack = { navController.safePopBackStack() }
                    )
                }

                composable<CoupleTracker>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) {
                    CoupleTrackerScreen(
                        onNavigateBack = { navController.safePopBackStack() }
                    )
                }

                composable<DeveloperOptions>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) {
                    DeveloperScreen(
                        onNavigateBack = { navController.safePopBackStack() }
                    )
                }

                composable<SmsSettings>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) { backStackEntry ->
                    val args = backStackEntry.toRoute<SmsSettings>()
                    SMSScreen(
                        targetOptionId = args.targetOptionId,
                        onNavigateBack = { navController.safePopBackStack() },
                        onNavigateToUnrecognizedSms = { navController.safeNavigate(UnrecognizedSms) },
                        blurEffects = themeUiState.blurEffects
                    )
                }

                composable<TransactionSettings>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) { backStackEntry ->
                    val args = backStackEntry.toRoute<TransactionSettings>()
                    TransactionSettingsScreen(
                        targetOptionId = args.targetOptionId,
                        onNavigateBack = { navController.safePopBackStack() }
                    )
                }

                composable<CurrencySettings>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) { backStackEntry ->
                    val args = backStackEntry.toRoute<CurrencySettings>()
                    CurrencySettingsScreen(
                        targetOptionId = args.targetOptionId,
                        onNavigateBack = { navController.safePopBackStack() }
                    )
                }

                composable<Profile>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) {
                    ProfileScreen(
                        onNavigateBack = { navController.safePopBackStack() }
                    )
                }

                composable<Appearance>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) { backStackEntry ->
                    val args = backStackEntry.toRoute<Appearance>()
                    AppearanceScreen(
                        targetOptionId = args.targetOptionId,
                        onNavigateBack = { navController.safePopBackStack() }
                    )
                }

                composable<NotificationSettings>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) { backStackEntry ->
                    val args = backStackEntry.toRoute<NotificationSettings>()
                    NotificationScreen(
                        targetOptionId = args.targetOptionId,
                        onNavigateBack = { navController.safePopBackStack() },
                        blurEffects = themeUiState.blurEffects,
                    )
                }

                composable<Categories>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) {
                    CategoriesScreen(
                        onNavigateBack = { navController.safePopBackStack() },
                        blurEffects = themeUiState.blurEffects
                    )
                }

                composable<UnrecognizedSms>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) {
                    UnrecognizedSmsScreen(
                        onNavigateBack = { navController.safePopBackStack() }
                    )
                }


                composable<ManageAccounts>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) {
                    ManageAccountsScreen(
                        onNavigateBack = { navController.safePopBackStack() },
                        onNavigateToAccountDetail = { bankName, last4 ->
                            navController.safeNavigate(AccountDetail(bankName, last4))
                        },
                        onNavigateToAccountAudit = { bankName, last4 ->
                            navController.safeNavigate(AccountAudit(bankName, last4))
                        },
                        onNavigateToDataSanitization = { tab ->
                            navController.safeNavigate(DataSanitization(tab))
                        },
                        blurEffects = themeUiState.blurEffects
                    )
                }

                composable<AddAccount>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) {
                    AddAccountScreen(
                        onNavigateBack = { navController.safePopBackStack() }
                    )
                }

                composable<AccountAudit>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) { backStackEntry ->
                    val accountAudit = backStackEntry.toRoute<AccountAudit>()
                    AccountAuditScreen(
                        bankName = accountAudit.bankName,
                        accountLast4 = accountAudit.accountLast4,
                        onNavigateBack = { navController.safePopBackStack() },
                        onTransactionClick = { transactionId ->
                            navController.safeNavigate(TransactionDetail(transactionId))
                        }
                    )
                }

                composable<Rules>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) {
                    RulesScreen(
                        onNavigateBack = { navController.safePopBackStack() },
                        onNavigateToCreateRule = { navController.safeNavigate(CreateRule()) },
                        onEditRule = { rule ->
                            navController.safeNavigate(CreateRule(ruleId = rule.id))
                        },
                        blurEffects = themeUiState.blurEffects
                    )
                }

                composable<CreateRule>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) { backStackEntry ->
                    val createRuleRoute = backStackEntry.toRoute<CreateRule>()
                    val rulesViewModel: RulesViewModel = hiltViewModel()
                    val rules by rulesViewModel.rules.collectAsState()
                    val existingRule = remember(createRuleRoute.ruleId, rules) {
                        rules.find { it.id == createRuleRoute.ruleId }
                    }
                    
                    CreateRuleScreen(
                        onNavigateBack = { navController.safePopBackStack() },
                        onSaveRule = { rule ->
                            if (createRuleRoute.ruleId != null) {
                                rulesViewModel.updateRule(rule)
                            } else {
                                rulesViewModel.createRule(rule)
                            }
                            navController.safePopBackStack()
                        },
                        existingRule = existingRule,
                        rulesViewModel = rulesViewModel
                    )
                }

                /* DETAIL SCREENS (with shared transitions) ---- */
                composable<TransactionDetail>(
                    enterTransition = VittifyTransitions.noneEnter,
                    exitTransition = VittifyTransitions.noneExit,
                    popEnterTransition = VittifyTransitions.noneEnter,
                    popExitTransition = VittifyTransitions.noneExit
                ) { backStackEntry ->
                    val transactionDetail = backStackEntry.toRoute<TransactionDetail>()
                    TransactionDetailScreen(
                        transactionId = transactionDetail.transactionId,
                        sharedElementKey = transactionDetail.sharedElementKey,
                        onNavigateBack = {
                            onEditComplete()
                            navController.safePopBackStack()
                        },
                        animatedContentScope = this@composable,
                        blurEffects = themeUiState.blurEffects,
                    )
                }

                composable<AddTransaction>(
                    enterTransition = VittifyTransitions.noneEnter,
                    exitTransition = VittifyTransitions.noneExit,
                    popEnterTransition = VittifyTransitions.noneEnter,
                    popExitTransition = VittifyTransitions.noneExit
                ) {
                    Box(Modifier.fillMaxSize())
                }

                composable<AccountDetail>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) { backStackEntry ->
                    val accountDetail = backStackEntry.toRoute<AccountDetail>()
                    AccountDetailScreen(
                        navController = navController,
                        bankName = accountDetail.bankName,
                        accountLast4 = accountDetail.accountLast4,
                        animatedContentScope = this@composable
                    )
                }

                composable<Subscriptions>(
                    enterTransition = VittifyTransitions.noneEnter,
                    exitTransition = VittifyTransitions.noneExit,
                    popEnterTransition = VittifyTransitions.noneEnter,
                    popExitTransition = VittifyTransitions.noneExit
                ) {
                    SubscriptionsScreen(
                        onNavigateBack = { navController.safePopBackStack() },
                        onEditSubscription = { id ->
                            navController.safeNavigate(AddTransaction(initialTab = 1, subscriptionId = id))
                        },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedContentScope = this@composable
                    )
                }

                composable<Transactions>(
                    enterTransition = VittifyTransitions.bottomNavEnterTransition,
                    exitTransition = VittifyTransitions.bottomNavExitTransition,
                    popEnterTransition = VittifyTransitions.bottomNavPopEnterTransition,
                    popExitTransition = VittifyTransitions.bottomNavPopExitTransition
                ) { backStackEntry ->
                    val transactions = backStackEntry.toRoute<Transactions>()
                    TransactionsScreen(
                        transactionsViewModel = transactionsViewModel,
                        initialCategory = transactions.category,
                        initialMerchant = transactions.merchant,
                        initialPeriod = transactions.period,
                        initialCurrency = transactions.currency,
                        initialType = transactions.type,
                        focusSearch = transactions.focusSearch,
                        onNavigateBack = { navController.safePopBackStack() },
                        onTransactionClick = { transactionId, key ->
                            navController.safeNavigate(TransactionDetail(transactionId, key))
                        },
                        onNavigateToSettings = {
                            navController.safeNavigate(Settings())
                        },
                        onNavigateToArchivedTransactions = {
                            navController.safeNavigate(DataSanitization(initialTab = "archived"))
                        },
                        animatedContentScope = this@composable,
                        blurEffects = themeUiState.blurEffects
                    )
                }

                composable<Budgets>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) { backStackEntry ->
                    val budgets = backStackEntry.toRoute<Budgets>()
                    BudgetsScreen(
                        onNavigateBack = { navController.safePopBackStack() },
                        onBudgetClick = { id, key ->
                            navController.safeNavigate(BudgetDetail(budgetId = id, sharedElementKey = key))
                        },
                        onHistoryClick = { id ->
                            navController.safeNavigate(BudgetHistory(id))
                        },
                        animatedContentScope = this@composable,
                        sharedElementPrefix = budgets.sharedElementPrefix,
                        blurEffects = themeUiState.blurEffects
                    )
                }

                composable<BudgetDetail>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) { backStackEntry ->
                    val budgetDetail = backStackEntry.toRoute<BudgetDetail>()
                    BudgetDetailScreen(
                        budgetId = budgetDetail.budgetId,
                        startDate = budgetDetail.startDate,
                        endDate = budgetDetail.endDate,
                        onNavigateBack = { navController.safePopBackStack() },
                        onNavigateToHistory = { id -> navController.safeNavigate(BudgetHistory(id)) },
                        onTransactionClick = { transactionId, key ->
                            navController.safeNavigate(TransactionDetail(transactionId, key))
                        },
                        animatedContentScope = this@composable,
                        sharedElementKey = budgetDetail.sharedElementKey,
                        blurEffects = themeUiState.blurEffects
                    )
                }

                composable<BudgetHistory>(
                    enterTransition = VittifyTransitions.horizontalSlideEnter,
                    exitTransition = VittifyTransitions.horizontalSlideExit,
                    popEnterTransition = VittifyTransitions.horizontalSlidePopEnter,
                    popExitTransition = VittifyTransitions.horizontalSlidePopExit
                ) { backStackEntry ->
                    val budgetHistory = backStackEntry.toRoute<BudgetHistory>()
                    BudgetHistoryScreen(
                        budgetId = budgetHistory.budgetId,
                        onNavigateBack = { navController.safePopBackStack() },
                        onNavigateToDetail = { id, start, end ->
                            navController.safeNavigate(BudgetDetail(
                                budgetId = id,
                                startDate = start?.toString(),
                                endDate = end?.toString()
                            ))
                        }
                    )
                }
            }
        }

        val optionsDesc = stringResource(R.string.options_desc)
        val addTransactionLbl = stringResource(R.string.add_transaction)
        val syncSmsLbl = stringResource(R.string.sync_sms)
        val exportLbl = stringResource(R.string.export)
        val searchLbl = stringResource(R.string.search)
        val syncAccountsLbl = stringResource(R.string.sync_accounts)
        val accountsLbl = stringResource(R.string.title_accounts)
        val editWidgetsLbl = stringResource(R.string.edit_widgets)
        val settingsLbl = stringResource(R.string.settings)

        val fabConfig = remember(showFloatingFab, isHomeScreen, isTransactionsScreen, isAnalyticsScreen) {
            if (showFloatingFab) {
                FabConfig(
                    icon = Icons.Rounded.Add,
                    contentDescription = optionsDesc,
                    dropdownContent = { dismiss ->
                        if (isHomeScreen || isTransactionsScreen) {
                            DropdownMenuItem(
                                text = { Text(
                                    text = addTransactionLbl,
                                ) },
                                onClick = { 
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    dismiss()
                                    navController.safeNavigate(AddTransaction(initialTab = 0))
                                },
                                leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null) }
                            )
                            HorizontalDivider(
                                thickness = 1.5.dp,
                                color = MaterialTheme.colorScheme.surface.copy(0.6f)
                            )
                        }

                        
                        if (isHomeScreen) {
                            DropdownMenuItem(
                                text = { Text(
                                    text = editWidgetsLbl,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                ) },
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    dismiss()
                                    homeViewModel.openEditWidgetsSheet()
                                },
                                leadingIcon = { Icon(Iconax.Convertshape2, contentDescription = null) }
                            )

                            HorizontalDivider(
                                thickness = 1.5.dp,
                                color = MaterialTheme.colorScheme.surface.copy(0.6f)
                            )

                            DropdownMenuItem(
                                text = { Text(
                                    text = settingsLbl,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                ) },
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    dismiss()
                                    navController.safeNavigate(Settings())
                                },
                                leadingIcon = { Icon(Iconax.Setting2, contentDescription = null) }
                            )

                            HorizontalDivider(
                                thickness = 1.5.dp,
                                color = MaterialTheme.colorScheme.surface.copy(0.6f)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .combinedClickable(
                                        onClick = {
                                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                            dismiss()
                                            navController.safeNavigate(SyncSms())
                                        },
                                        onLongClick = {
                                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                            dismiss()
                                            showFullResyncDialog = true
                                        }
                                    )
                                    .padding(MenuDefaults.DropdownMenuItemContentPadding),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Sync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = syncSmsLbl,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            HorizontalDivider(
                                thickness = 1.5.dp,
                                color = MaterialTheme.colorScheme.surface.copy(0.6f)
                            )

                            DropdownMenuItem(
                                text = { Text(
                                    text = syncAccountsLbl,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                ) },
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    dismiss()
                                    homeViewModel.syncDevices()
                                },
                                leadingIcon = { Icon(Icons.Rounded.Sync, contentDescription = null) }
                            )

                            HorizontalDivider(
                                thickness = 1.5.dp,
                                color = MaterialTheme.colorScheme.surface.copy(0.6f)
                            )

                            DropdownMenuItem(
                                text = { Text(
                                    text = accountsLbl,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                ) },
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    dismiss()
                                    navController.safeNavigate(ManageAccounts)
                                },
                                leadingIcon = { Icon(Icons.Rounded.AccountBalance, contentDescription = null) }
                            )

                            HorizontalDivider(
                                thickness = 1.5.dp,
                                color = MaterialTheme.colorScheme.surface.copy(0.6f)
                            )

                        } else if (isTransactionsScreen) {
                            DropdownMenuItem(
                                text = { Text(exportLbl) },
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    dismiss()
                                    showExportDialog = true
                                },
                                leadingIcon = { Icon(Iconax.ImportArrow01, contentDescription = null) }
                            )
                        } else if (isAnalyticsScreen) {
                            DropdownMenuItem(
                                text = { Text(searchLbl) },
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    dismiss()
                                    navController.safeNavigate(Transactions(focusSearch = true)) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = false
                                    }
                                },
                                leadingIcon = { Icon(Iconax.Search, contentDescription = null) }
                            )
                        }
                    }
                )
            } else null
        }

        SharedTransitionLayout {
            // Add Screen Overlay - Handled here for shared transition from FAB
            AnimatedVisibility(
                visible = isAddTransactionScreen,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                val addTransaction = if (isAddTransactionScreen) {
                    try { navBackStackEntry?.toRoute<AddTransaction>() ?: AddTransaction() }
                    catch (_: Exception) { AddTransaction() }
                } else AddTransaction()

                AddScreen(
                    onNavigateBack = { navController.safePopBackStack() },
                    animatedVisibilityScope = this@AnimatedVisibility,
                    initialTab = addTransaction.initialTab,
                    subscriptionId = addTransaction.subscriptionId,
                    transactionType = addTransaction.type,
                    nlpAmount = addTransaction.nlpAmount,
                    nlpMerchant = addTransaction.nlpMerchant,
                    nlpType = addTransaction.nlpType,
                    nlpBankName = addTransaction.nlpBankName,
                    nlpNotes = addTransaction.nlpNotes,
                    nlpCategory = addTransaction.nlpCategory,
                    nlpSubcategory = addTransaction.nlpSubcategory,
                    onNavigateToTransactionSettings = { navController.safeNavigate(TransactionSettings()) },
                    blurEffects = themeUiState.blurEffects,
                )
            }

            // FABs Container - Shown on Home, Transactions, Subscriptions, and Budget Detail
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                AnimatedVisibility(
                    visible = (isHomeScreen || isTransactionsScreen || isSubscriptionsScreen || isBudgetDetailScreen) && !hideFabsForFloatingNav,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(Dimensions.Padding.content)
                        .padding(
                            bottom = when (themeUiState.navigationBarStyle) {
                                NavigationBarStyle.FLOATING if showBottomNav -> 56.dp
                                NavigationBarStyle.NORMAL if showBottomNav -> 84.dp
                                else -> 10.dp
                            }
                        )
                        .navigationBarsPadding()
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        val smallFabContainerColor =  MaterialTheme.colorScheme.tertiaryContainer
                        val smallFabContentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        // Secondary FAB (Download on TransactionsScreen; HomeScreen uses Pull-to-refresh per Rule 36)
                        if (isTransactionsScreen) {
                            SmallFloatingActionButton(
                                onClick = {
                                    showExportDialog = true
                                },
                                modifier = Modifier
                                    .pointerInput(Unit) {
                                        detectTapGestures(onTap = {
                                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                            showExportDialog = true
                                        })
                                    },
                                shape = com.reddy.vittify.presentation.ui.theme.VittifyShapes.button,
                                containerColor = smallFabContainerColor,
                                contentColor = smallFabContentColor,
                            ) {
                                Icon(
                                    imageVector = Iconax.ImportArrow01,
                                    contentDescription = stringResource(R.string.export_transactions),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        val fabContainerColor =  MaterialTheme.colorScheme.primaryContainer
                        val fabContentColor = MaterialTheme.colorScheme.onPrimaryContainer

                        val standardFabSharedBoundsModifier = if (themeUiState.navigationBarStyle != NavigationBarStyle.FLOATING || !showBottomNav) {
                            Modifier.sharedBounds(
                                rememberSharedContentState(key = "fab_to_add"),
                                animatedVisibilityScope = this@AnimatedVisibility,
                                boundsTransform = { _, _ ->
                                    spring(
                                        stiffness = Spring.StiffnessLow,
                                        dampingRatio = Spring.DampingRatioLowBouncy
                                    )
                                },
                                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                                    contentScale = ContentScale.FillBounds,
                                    alignment = Alignment.Center
                                )
                            ).skipToLookaheadSize()
                        } else Modifier

                        // Add FAB - prominent squircle per Rule 37
                        FloatingActionButton(
                            onClick = { 
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                val initialTab = if (isSubscriptionsScreen) 1 else 0
                                navController.safeNavigate(AddTransaction(initialTab = initialTab)) 
                            },
                            shape = com.reddy.vittify.presentation.ui.theme.VittifyShapes.button,
                            modifier = Modifier
                                .then(standardFabSharedBoundsModifier)
                                .then(
                                    if (themeUiState.blurEffects) Modifier
                                        .clip(com.reddy.vittify.presentation.ui.theme.VittifyShapes.button)
                                        .hazeEffect(
                                            state = hazeState,
                                            block = fun HazeEffectScope.() {
                                                style = HazeDefaults.style(
                                                    backgroundColor = Color.Transparent,
                                                    tint = HazeDefaults.tint(fabContainerColor),
                                                    blurRadius = 20.dp,
                                                    noiseFactor = -1f,
                                                )
                                                blurredEdgeTreatment = BlurredEdgeTreatment.Unbounded
                                            }
                                        ) else Modifier
                                ),
                            containerColor = fabContainerColor,
                            contentColor = fabContentColor,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = stringResource(R.string.add_transaction_subscription_cd),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // Full Resync Confirmation Dialog
            if (showFullResyncDialog) {
                val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                AlertDialog(
                    onDismissRequest = { showFullResyncDialog = false },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    title = { Text(stringResource(R.string.full_resync)) },
                    text = {
                        Text(stringResource(R.string.full_resync_desc))
                    },
                    confirmButton = {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                            ) {
                                Button(
                                    onClick = { showFullResyncDialog = false },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = VittifySurface.surfaceColor(),
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = VittifyShapes.scaled(
                                        topStart = Dimensions.Radius.xxl,
                                        topEnd = Dimensions.Radius.xs,
                                        bottomStart = Dimensions.Radius.xxl,
                                        bottomEnd = Dimensions.Radius.xs
                                    ),
                                    modifier = Modifier
                                        .weight(0.8f)
                                        .fillMaxWidth()
                                ) {
                                    Text(
                                        text = stringResource(R.string.cancel),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Button(
                                    onClick = {
                                        showFullResyncDialog = false
                                        navController.safeNavigate(SyncSms(forceResync = true))
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    shape = VittifyShapes.scaled(
                                        topStart = Dimensions.Radius.xs,
                                        topEnd = Dimensions.Radius.xxl,
                                        bottomStart = Dimensions.Radius.xs,
                                        bottomEnd = Dimensions.Radius.xxl
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                ) {
                                    Text(
                                        text = stringResource(R.string.resync_all),
                                        style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    },
                    containerColor = VittifySurface.surfaceContainerLowColor(),
                    modifier = Modifier
                        .clip(VittifyShapes.dialog)
                        .then(
                            if (themeUiState.blurEffects) Modifier.hazeEffect(
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
                    shape = VittifyShapes.dialog,
                    dismissButton = {}
                )
            }


            // Export Transactions Dialog (Only when on TransactionsScreen)
            if (showExportDialog && isTransactionsScreen) {
                ExportTransactionsDialog(
                    transactions = transactionsUiState.transactions,
                    onDismiss = { showExportDialog = false },
                    blurEffects = themeUiState.blurEffects,
                    hazeState = hazeState
                )
            }



            // Floating Profile Switcher
            val floatingProfileBottomPadding = if (showBottomNav) {
                if (themeUiState.navigationBarStyle == NavigationBarStyle.NORMAL) 96.dp else 80.dp
            } else 24.dp

            AnimatedVisibility(
                visible = showFloatingProfileSwitcher,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, bottom = floatingProfileBottomPadding)
            ) {
                FloatingProfileSwitcher(
                    activeMode = homeUiState.activeViewMode,
                    partnerName = homeUiState.partnerName,
                    userName = homeUiState.userName,
                    onModeSelected = { mode -> homeViewModel.setViewMode(mode) },
                    blurEffects = themeUiState.blurEffects,
                    hazeState = hazeState
                )
            }

            // Bottom Navigation
            VittifyBottomNavigation(
                navController = navController,
                currentDestination = currentDestination,
                navigationBarStyle = themeUiState.navigationBarStyle,
                hideLabels = themeUiState.hideNavigationLabels,
                hidePill = themeUiState.hidePillIndicator,
                blurEffects = themeUiState.blurEffects,
                visible = showBottomNav,
                modifier = Modifier.align(Alignment.BottomCenter),
                hazeState = hazeState,
                fabConfig = fabConfig,
                sharedTransitionScope = this@SharedTransitionLayout,
                isCoupleTrackingEnabled = homeUiState.isCoupleTrackingEnabled,
                profileSwitcherInFooter = themeUiState.profileSwitcherInFooter,
                activeViewMode = homeUiState.activeViewMode,
                partnerName = homeUiState.partnerName,
                userName = homeUiState.userName,
                onViewModeSelected = { mode -> homeViewModel.setViewMode(mode) }
            )
        }
    }
}
}
