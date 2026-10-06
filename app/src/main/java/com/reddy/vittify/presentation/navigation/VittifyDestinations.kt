package com.reddy.vittify.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavBackStackEntry
import kotlinx.serialization.Serializable

// Centralized transition definitions
object VittifyTransitions {
    
    private fun getBottomNavTabIndex(route: String?): Int = when {
        route == null -> -1
        route.contains("Home") -> 0
        route.contains("Analytics") -> 1
        route.contains("Transactions") -> 2
        else -> -1
    }

    // Directional horizontal transitions for bottom navigation tabs
    val bottomNavEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        val fromIndex = getBottomNavTabIndex(initialState.destination.route)
        val toIndex = getBottomNavTabIndex(targetState.destination.route)
        if (fromIndex != -1 && toIndex != -1) {
            val direction = if (toIndex > fromIndex) 1 else -1
            slideInHorizontally(
                initialOffsetX = { direction * it },
                animationSpec = spring(
                    stiffness = Spring.StiffnessLow,
                    dampingRatio = Spring.DampingRatioNoBouncy
                )
            ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow))
        } else {
            horizontalSlideEnter(this)
        }
    }

    val bottomNavExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        val fromIndex = getBottomNavTabIndex(initialState.destination.route)
        val toIndex = getBottomNavTabIndex(targetState.destination.route)
        if (fromIndex != -1 && toIndex != -1) {
            val direction = if (toIndex > fromIndex) -1 else 1
            slideOutHorizontally(
                targetOffsetX = { direction * it / 4 },
                animationSpec = spring(
                    stiffness = Spring.StiffnessLow,
                    dampingRatio = Spring.DampingRatioNoBouncy
                )
            ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow))
        } else {
            horizontalSlideExit(this)
        }
    }

    val bottomNavPopEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        val fromIndex = getBottomNavTabIndex(initialState.destination.route)
        val toIndex = getBottomNavTabIndex(targetState.destination.route)
        if (fromIndex != -1 && toIndex != -1) {
            val direction = if (toIndex > fromIndex) 1 else -1
            slideInHorizontally(
                initialOffsetX = { direction * it },
                animationSpec = spring(
                    stiffness = Spring.StiffnessLow,
                    dampingRatio = Spring.DampingRatioNoBouncy
                )
            ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow))
        } else {
            horizontalSlidePopEnter(this)
        }
    }

    val bottomNavPopExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        val fromIndex = getBottomNavTabIndex(initialState.destination.route)
        val toIndex = getBottomNavTabIndex(targetState.destination.route)
        if (fromIndex != -1 && toIndex != -1) {
            val direction = if (toIndex > fromIndex) -1 else 1
            slideOutHorizontally(
                targetOffsetX = { direction * it / 4 },
                animationSpec = spring(
                    stiffness = Spring.StiffnessLow,
                    dampingRatio = Spring.DampingRatioNoBouncy
                )
            ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow))
        } else {
            horizontalSlidePopExit(this)
        }
    }
    
    // Horizontal slide transitions for sub-screens
    val horizontalSlideEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = spring(
                stiffness = Spring.StiffnessLow,
                dampingRatio = Spring.DampingRatioNoBouncy
            )
        ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow))
    }
    
    val horizontalSlideExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { -it / 4 },
            animationSpec = spring(
                stiffness = Spring.StiffnessLow,
                dampingRatio = Spring.DampingRatioNoBouncy
            )
        ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow))
    }
    
    val horizontalSlidePopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(
            initialOffsetX = { -it / 4 },
            animationSpec = spring(
                stiffness = Spring.StiffnessLow,
                dampingRatio = Spring.DampingRatioNoBouncy
            )
        ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow))
    }
    
    val horizontalSlidePopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = spring(
                stiffness = Spring.StiffnessLow,
                dampingRatio = Spring.DampingRatioNoBouncy
            )
        ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow))
    }
    
    // Vertical slide transitions
    val verticalSlideEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                stiffness = Spring.StiffnessLow,
                dampingRatio = Spring.DampingRatioNoBouncy
            )
        ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow))
    }
    
    val verticalSlideExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutVertically(
            targetOffsetY = { -it / 4},
            animationSpec = spring(
                stiffness = Spring.StiffnessLow,
                dampingRatio = Spring.DampingRatioNoBouncy
            )
        ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow))
    }

    val verticalSlidePopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInVertically(
            initialOffsetY = { -it / 4 },
            animationSpec = spring(
                stiffness = Spring.StiffnessLow,
                dampingRatio = Spring.DampingRatioNoBouncy
            )
        ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow))
    }

    val verticalSlidePopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = spring(
                stiffness = Spring.StiffnessLow,
                dampingRatio = Spring.DampingRatioNoBouncy
            )
        ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow))
    }
    
    // FAB to screen scale transitions
    val fabScaleEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
            scaleIn(
                initialScale = 0.8f,
                animationSpec = spring(
                    stiffness = Spring.StiffnessMedium,
                    dampingRatio = Spring.DampingRatioLowBouncy
                )
            )
    }
    
    val fabScaleExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
            scaleOut(
                targetScale = 1.1f,
                animationSpec = spring(
                    stiffness = Spring.StiffnessMedium,
                    dampingRatio = Spring.DampingRatioLowBouncy
                )
            )
    }
    
    val fabScalePopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
            scaleIn(
                initialScale = 1.1f,
                animationSpec = spring(
                    stiffness = Spring.StiffnessMedium,
                    dampingRatio = Spring.DampingRatioLowBouncy
                )
            )
    }
    
    val fabScalePopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
            scaleOut(
                targetScale = 0.8f,
                animationSpec = spring(
                    stiffness = Spring.StiffnessMedium,
                    dampingRatio = Spring.DampingRatioLowBouncy
                )
            )
    }
    
    // Scale transitions for detail screens
    val scaleEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
            scaleIn(animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioLowBouncy))
    }
    
    val scaleExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
            scaleOut(animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioLowBouncy))
    }
    
    // None transitions - for screens using shared element transitions entirely
    val noneEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium))
    }
    
    val noneExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium))
    }
}

@Serializable object Splash

@Serializable object AppLock

@Serializable object OnBoarding

@Serializable object Home

@Serializable
data class Transactions(
    val category: String? = null,
    val merchant: String? = null,
    val period: String? = null,
    val currency: String? = null,
    val type: String? = null,
    val focusSearch: Boolean = false
)

@Serializable data class Settings(val targetSettingId: String? = null)
@Serializable data class Customization(val targetOptionId: String? = null)
@Serializable data class AiSettings(val targetOptionId: String? = null)
@Serializable data class CurrencySettings(val targetOptionId: String? = null)
@Serializable object Subscriptions
@Serializable object Categories

@Serializable object Analytics

@Serializable object Chat

@Serializable data class TransactionDetail(val transactionId: Long, val sharedElementKey: String? = null)

@Serializable data class AddTransaction(
    val initialTab: Int = 0,
    val subscriptionId: Long? = null,
    val type: String? = null,
    // NLP Quick Add prefill fields
    val nlpAmount: String? = null,
    val nlpMerchant: String? = null,
    val nlpType: String? = null,
    val nlpBankName: String? = null,
    val nlpNotes: String? = null,
    val nlpCategory: String? = null,
    val nlpSubcategory: String? = null,
)

@Serializable data class AccountDetail(val bankName: String, val accountLast4: String)

@Serializable object UnrecognizedSms

@Serializable object Faq

@Serializable object Guides

@Serializable object PrivacyPolicy

@Serializable object TermsOfService

@Serializable object Credits

@Serializable object Rules

@Serializable data class CreateRule(val ruleId: String? = null)

@Serializable data class Appearance(val targetOptionId: String? = null)

@Serializable object ManageAccounts

@Serializable object Profile

@Serializable data class SmsSettings(val targetOptionId: String? = null)

@Serializable data class DataPrivacy(val targetOptionId: String? = null)
@Serializable data class DataSanitization(val initialTab: String? = null)
@Serializable object ManageArchivedTransactions
@Serializable data class CloudBackup(val fromOnboarding: Boolean = false, val targetOptionId: String? = null)
@Serializable object P2pDeviceSync
@Serializable object CoupleTracker

@Serializable data class NotificationSettings(val targetOptionId: String? = null)
@Serializable object Webhooks
@Serializable data class WebhookEditor(val profileId: String? = null)

@Serializable data class Budgets(val sharedElementPrefix: Long? = null)

@Serializable data class BudgetDetail(
    val budgetId: Long, 
    val sharedElementKey: String? = null,
    val startDate: String? = null,
    val endDate: String? = null
)

@Serializable data class BudgetHistory(val budgetId: Long)

@Serializable object DeveloperOptions

@Serializable object AddAccount

@Serializable data class AccountAudit(val bankName: String, val accountLast4: String)

@Serializable object About

@Serializable object Licenses

@Serializable object PdfReport

@Serializable data class SyncSms(val forceResync: Boolean = false)

@Serializable data class TransactionSettings(val targetOptionId: String? = null)

// Routes where bottom navigation should be visible
val BOTTOM_NAV_ROUTES = setOf(
    Home::class.qualifiedName,
    Analytics::class.qualifiedName,
    Transactions::class.qualifiedName
)

// Setting routes where bottom navigation should also be visible when in normal navigation mode
val SETTINGS_ROUTES = setOf(
    Settings::class.qualifiedName,
    Appearance::class.qualifiedName,
    Customization::class.qualifiedName,
    AiSettings::class.qualifiedName,
    CurrencySettings::class.qualifiedName,
    Profile::class.qualifiedName,
    SmsSettings::class.qualifiedName,
    NotificationSettings::class.qualifiedName,
    DataPrivacy::class.qualifiedName,
    DataSanitization::class.qualifiedName,
    CloudBackup::class.qualifiedName,
    P2pDeviceSync::class.qualifiedName,
    CoupleTracker::class.qualifiedName,
    About::class.qualifiedName,
    Licenses::class.qualifiedName,
    Webhooks::class.qualifiedName,
    WebhookEditor::class.qualifiedName,
    DeveloperOptions::class.qualifiedName,
    Rules::class.qualifiedName,
    CreateRule::class.qualifiedName,
    Categories::class.qualifiedName,
    ManageAccounts::class.qualifiedName,
    Budgets::class.qualifiedName,
    Faq::class.qualifiedName,
    Guides::class.qualifiedName,
    PrivacyPolicy::class.qualifiedName,
    TermsOfService::class.qualifiedName,
    Credits::class.qualifiedName,
    PdfReport::class.qualifiedName,
    ManageArchivedTransactions::class.qualifiedName,
    TransactionSettings::class.qualifiedName
)
