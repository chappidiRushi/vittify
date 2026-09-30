package com.reddy.vittify

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.reddy.vittify.presentation.navigation.AppLock
import com.reddy.vittify.presentation.navigation.Home
import com.reddy.vittify.presentation.navigation.Splash
import com.reddy.vittify.presentation.navigation.VittifyNavHost
import com.reddy.vittify.presentation.navigation.OnBoarding
import com.reddy.vittify.presentation.navigation.Settings
import com.reddy.vittify.presentation.navigation.AddTransaction
import com.reddy.vittify.presentation.navigation.TransactionDetail
import com.reddy.vittify.presentation.ui.theme.VittifyTheme
import com.reddy.vittify.presentation.ui.features.settings.applock.AppLockViewModel
import com.reddy.vittify.presentation.ui.features.settings.appearance.ThemeViewModel
import com.reddy.vittify.presentation.ui.components.GlobalToastHost

@Composable
fun VittifyApp(
    themeViewModel: ThemeViewModel = hiltViewModel(),
    appLockViewModel: AppLockViewModel = hiltViewModel(),
    editTransactionId: Long? = null,
    onEditComplete: () -> Unit = {},
    addTransactionTab: Int? = null,
    addTransactionType: String? = null,
    nlpAmount: String? = null,
    nlpMerchant: String? = null,
    nlpType: String? = null,
    nlpBankName: String? = null,
    nlpNotes: String? = null,
    nlpCategory: String? = null,
    nlpSubcategory: String? = null,
    onAddComplete: () -> Unit = {}
) {
    val themeUiState by themeViewModel.themeUiState.collectAsStateWithLifecycle()
    val appLockUiState by appLockViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val darkTheme = themeUiState.isDarkTheme ?: isSystemInDarkTheme()

    val navController = rememberNavController()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Observe lifecycle events and refresh lock state when app resumes from background
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // App came to foreground - check if it should be locked
                appLockViewModel.refreshLockState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Only render the app once the theme state is loaded
    if (!themeUiState.isLoaded) return

    // Determine initial destination based on intent or splash
    val startDestination = remember {
        if (editTransactionId != null || addTransactionTab != null) {
            if (themeUiState.isOnboardingFinished) Home else OnBoarding
        } else {
            Splash
        }
    }

    // Observe lock state changes and navigate to lock screen if needed
    LaunchedEffect(appLockUiState.isLocked, appLockUiState.isLockEnabled) {
        if (appLockUiState.isLocked && appLockUiState.isLockEnabled) {
            val currentRoute = navController.currentDestination?.route
            // Don't navigate if already on lock screen
            if (currentRoute != AppLock::class.qualifiedName) {
                navController.navigate(AppLock) {
                    launchSingleTop = true
                }
            }
        }
    }
    
    // Navigate to transaction detail when editTransactionId changes
    LaunchedEffect(editTransactionId) {
        editTransactionId?.let { transactionId ->
            navController.navigate(TransactionDetail(transactionId))
        }
    }

    // Navigate to Add screen when addTransactionTab changes
    LaunchedEffect(addTransactionTab, addTransactionType, nlpAmount, nlpMerchant, nlpType, nlpBankName, nlpNotes, nlpCategory, nlpSubcategory) {
        addTransactionTab?.let { tab ->
            navController.navigate(
                AddTransaction(
                    initialTab = tab,
                    type = addTransactionType,
                    nlpAmount = nlpAmount,
                    nlpMerchant = nlpMerchant,
                    nlpType = nlpType,
                    nlpBankName = nlpBankName,
                    nlpNotes = nlpNotes,
                    nlpCategory = nlpCategory,
                    nlpSubcategory = nlpSubcategory
                )
            )
            onAddComplete()
        }
    }

    VittifyTheme(
        darkTheme = darkTheme,
        themeStyle = themeUiState.themeStyle,
        dynamicColor = themeUiState.isDynamicColorEnabled,
        isAmoledMode = themeUiState.isAmoledMode,
        accentColor = themeUiState.accentColor,
        appFont = themeUiState.appFont,
        customFontPath = themeUiState.customFontPath,
        dynamicSeedColor = themeUiState.dynamicSeedColor,
        blurEffects = themeUiState.blurEffects,
        playfulAnimation = themeUiState.isPlayfulAnimationEnabled,
        surfaceOpacity = themeUiState.surfaceOpacity,
        borderThickness = themeUiState.borderThickness,
        borderOpacity = themeUiState.borderOpacity,
        cornerRadiusScale = themeUiState.cornerRadiusScale,
        paddingScale = themeUiState.paddingScale
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            VittifyNavHost(
                navController = navController,
                startDestination = startDestination,
                onEditComplete = onEditComplete
            )
            GlobalToastHost()
        }
    }
}