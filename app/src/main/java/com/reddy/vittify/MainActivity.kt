package com.reddy.vittify

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.appcompat.app.AppCompatActivity
import com.reddy.vittify.receiver.SmsBroadcastReceiver
import com.reddy.vittify.data.manager.NotificationScheduler
import androidx.lifecycle.lifecycleScope
import com.reddy.vittify.data.currency.model.CurrencySymbols
import com.reddy.vittify.data.preferences.UserPreferencesRepository
import com.reddy.vittify.presentation.ui.features.accounts.AccountDetailViewModel
import com.reddy.vittify.presentation.ui.features.accounts.ManageAccountsViewModel
import com.reddy.vittify.presentation.ui.features.add.AddViewModel
import com.reddy.vittify.presentation.ui.features.analytics.AnalyticsViewModel
import com.reddy.vittify.presentation.ui.features.budgets.BudgetViewModel
import com.reddy.vittify.presentation.ui.features.categories.CategoriesViewModel
import com.reddy.vittify.presentation.ui.features.home.HomeViewModel
import com.reddy.vittify.presentation.ui.features.onboarding.OnBoardingViewModel
import com.reddy.vittify.presentation.ui.features.profile.ProfileViewModel
import com.reddy.vittify.presentation.ui.features.settings.SettingsViewModel
import com.reddy.vittify.presentation.ui.features.settings.appearance.ThemeViewModel
import com.reddy.vittify.presentation.ui.features.settings.applock.AppLockViewModel
import com.reddy.vittify.presentation.ui.features.settings.notifications.NotificationViewModel
import com.reddy.vittify.presentation.ui.features.settings.rules.RulesViewModel
import com.reddy.vittify.presentation.ui.features.settings.unrecognized.UnrecognizedSmsViewModel
import com.reddy.vittify.presentation.ui.features.spotlight.SpotlightViewModel
import com.reddy.vittify.presentation.ui.features.subscriptions.SubscriptionsViewModel
import com.reddy.vittify.presentation.ui.features.transactions.TransactionDetailViewModel
import com.reddy.vittify.presentation.ui.features.transactions.TransactionsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.getValue
import com.reddy.vittify.presentation.navigation.SettingsDeepLink

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    companion object {
        const val ACTION_ADD_TRANSACTION = "com.reddy.vittify.action.ADD_TRANSACTION"
        const val ACTION_ADD_SUBSCRIPTION = "com.reddy.vittify.action.ADD_SUBSCRIPTION"
        const val ACTION_ADD_TRANSFER = "com.reddy.vittify.action.ADD_TRANSFER"
        
        const val ACTION_QUICK_ADD_PREFILL = "com.reddy.vittify.action.QUICK_ADD_PREFILL"
        const val EXTRA_NLP_AMOUNT = "extra_nlp_amount"
        const val EXTRA_NLP_MERCHANT = "extra_nlp_merchant"
        const val EXTRA_NLP_TYPE = "extra_nlp_type"
        const val EXTRA_NLP_BANK_NAME = "extra_nlp_bank_name"
        const val EXTRA_NLP_NOTES = "extra_nlp_notes"
        const val EXTRA_NLP_CATEGORY = "extra_nlp_category"
        const val EXTRA_NLP_SUBCATEGORY = "extra_nlp_subcategory"
        const val EXTRA_NLP_DATE = "extra_nlp_date"
    }

    private val themeViewModel: ThemeViewModel by viewModels()
    private val appLockViewModel: AppLockViewModel by viewModels()
    
    @Inject
    lateinit var notificationScheduler: NotificationScheduler

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var p2pSyncEngine: com.reddy.vittify.data.sync.engine.P2pSyncEngine

    @Inject
    lateinit var p2pPreferences: com.reddy.vittify.data.sync.P2pSyncPreferencesRepository

    override fun onStart() {
        super.onStart()
        if (p2pPreferences.isSyncEnabled()) {
            p2pSyncEngine.startSync()
            p2pSyncEngine.onAppForegrounded()
        }
    }

    override fun onStop() {
        super.onStop()
        p2pSyncEngine.onAppBackgrounded()
    }

    // Transaction ID to edit when launched from notification
    var editTransactionId by mutableStateOf<Long?>(null)
        private set

    // Initial tab to show in Add Screen (0 for Transaction, 1 for Subscription)
    var addTransactionTab by mutableStateOf<Int?>(null)
        private set

    // Initial transaction type to pre-select
    var addTransactionType by mutableStateOf<String?>(null)
        private set

    // NLP Prefill fields
    var nlpAmount by mutableStateOf<String?>(null)
        private set
    var nlpMerchant by mutableStateOf<String?>(null)
        private set
    var nlpType by mutableStateOf<String?>(null)
        private set
    var nlpBankName by mutableStateOf<String?>(null)
        private set
    var nlpNotes by mutableStateOf<String?>(null)
        private set
    var nlpCategory by mutableStateOf<String?>(null)
        private set
    var nlpSubcategory by mutableStateOf<String?>(null)
        private set
    var nlpDate by mutableStateOf<String?>(null)
        private set

    // Pending deep link settings destination
    var pendingSettingsDestination by mutableStateOf<Any?>(null)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        
        // Keep the splash screen on-screen until the theme settings are loaded
        splashScreen.setKeepOnScreenCondition {
            !themeViewModel.themeUiState.value.isLoaded
        }

        // Fade out system splash smoothly so in-app expressive splash hands off seamlessly
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            val splashScreenView = splashScreenViewProvider.view
            val fadeOut = android.animation.ObjectAnimator.ofFloat(
                splashScreenView,
                android.view.View.ALPHA,
                1f,
                0f
            ).apply {
                interpolator = android.view.animation.AccelerateDecelerateInterpolator()
                duration = 300L
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        splashScreenViewProvider.remove()
                    }
                })
            }
            fadeOut.start()
        }
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
//            window.isStatusBarContrastEnforced = false
        }

        // Handle intent if activity is launched from notification or shortcut/tile
        handleIntent(intent)

        // Schedule daily reminders
        lifecycleScope.launch {
            notificationScheduler.scheduleDailyReminder()
        }

        // Load custom currencies into CurrencySymbols
        lifecycleScope.launch {
            userPreferencesRepository.customCurrencies.collect { customCurrencies ->
                val customMap = customCurrencies.associate { it.code to it.symbol }
                CurrencySymbols.setCustomSymbols(customMap)
            }
        }

        setContent {
            VittifyApp(
                editTransactionId = editTransactionId,
                onEditComplete = { editTransactionId = null },
                addTransactionTab = addTransactionTab,
                addTransactionType = addTransactionType,
                nlpAmount = nlpAmount,
                nlpMerchant = nlpMerchant,
                nlpType = nlpType,
                nlpBankName = nlpBankName,
                nlpNotes = nlpNotes,
                nlpCategory = nlpCategory,
                nlpSubcategory = nlpSubcategory,
                nlpDate = nlpDate,
                onAddComplete = { 
                    addTransactionTab = null
                    addTransactionType = null
                    nlpAmount = null
                    nlpMerchant = null
                    nlpType = null
                    nlpBankName = null
                    nlpNotes = null
                    nlpCategory = null
                    nlpSubcategory = null
                    nlpDate = null
                },
                pendingSettingsDestination = pendingSettingsDestination,
                onSettingsDestinationHandled = { pendingSettingsDestination = null },
                appLockViewModel = appLockViewModel,
                themeViewModel = themeViewModel,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        appLockViewModel.refreshLockState()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle intent when activity is already running
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        // Deep link URI parsing for settings and in-app navigation
        val data = intent?.data
        if (data != null) {
            val target = SettingsDeepLink.parse(data)
            if (target != null) {
                pendingSettingsDestination = target.destination
            }
        } else {
            val linkExtra = intent?.getStringExtra("deep_link") ?: intent?.getStringExtra("settings_link")
            if (!linkExtra.isNullOrBlank()) {
                val target = SettingsDeepLink.parse(linkExtra)
                if (target != null) {
                    pendingSettingsDestination = target.destination
                }
            }
        }

        when (intent?.action) {
            SmsBroadcastReceiver.ACTION_EDIT_TRANSACTION -> {
                val transactionId = intent.getLongExtra(SmsBroadcastReceiver.EXTRA_TRANSACTION_ID, -1)
                if (transactionId != -1L) {
                    editTransactionId = transactionId
                }
            }
            ACTION_ADD_TRANSACTION -> {
                addTransactionTab = 0
                val amount = intent.getStringExtra("amount") ?: intent.getStringExtra(EXTRA_NLP_AMOUNT)
                val merchant = intent.getStringExtra("merchant") ?: intent.getStringExtra(EXTRA_NLP_MERCHANT)
                val category = intent.getStringExtra("category") ?: intent.getStringExtra(EXTRA_NLP_CATEGORY)
                if (!amount.isNullOrBlank()) nlpAmount = amount
                if (!merchant.isNullOrBlank()) nlpMerchant = merchant
                if (!category.isNullOrBlank()) nlpCategory = category
            }
            ACTION_ADD_SUBSCRIPTION -> {
                addTransactionTab = 1
            }
            ACTION_ADD_TRANSFER -> {
                addTransactionTab = 0
                addTransactionType = "TRANSFER"
            }
            ACTION_QUICK_ADD_PREFILL -> {
                addTransactionTab = 0
                nlpAmount = intent.getStringExtra(EXTRA_NLP_AMOUNT)
                nlpMerchant = intent.getStringExtra(EXTRA_NLP_MERCHANT)
                nlpType = intent.getStringExtra(EXTRA_NLP_TYPE)
                nlpBankName = intent.getStringExtra(EXTRA_NLP_BANK_NAME)
                nlpNotes = intent.getStringExtra(EXTRA_NLP_NOTES)
                nlpCategory = intent.getStringExtra(EXTRA_NLP_CATEGORY)
                nlpSubcategory = intent.getStringExtra(EXTRA_NLP_SUBCATEGORY)
                nlpDate = intent.getStringExtra(EXTRA_NLP_DATE)
            }
        }
    }
}
