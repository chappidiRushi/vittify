package com.reddy.vittify.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.reddy.vittify.R
import com.reddy.vittify.presentation.ui.icons.FavoriteChart
import com.reddy.vittify.presentation.ui.icons.Home
import com.reddy.vittify.presentation.ui.icons.ReceiptItem
import com.reddy.vittify.presentation.ui.icons.Setting2
import com.reddy.vittify.presentation.ui.icons.Iconax
import kotlin.reflect.KClass
import com.reddy.vittify.presentation.navigation.Home as HomeDestination
import com.reddy.vittify.presentation.navigation.Analytics as AnalyticsDestination
import com.reddy.vittify.presentation.navigation.Transactions as TransactionsDestination
import com.reddy.vittify.presentation.navigation.Settings as SettingsDestination

sealed class BottomNavItem(
    val route: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector,
    val destination: Any,
    val destinationType: KClass<*>
) {
    data object Home : BottomNavItem(
        route = "home",
        titleRes = R.string.home_title,
        icon = Iconax.Home,
        destination = HomeDestination,
        destinationType = HomeDestination::class
    )
    
    data object Analytics : BottomNavItem(
        route = "analytics",
        titleRes = R.string.analytics,
        icon = Iconax.FavoriteChart,
        destination = AnalyticsDestination,
        destinationType = AnalyticsDestination::class
    )

    data object Transactions : BottomNavItem(
        route = "transactions",
        titleRes = R.string.transactions,
        icon = Iconax.ReceiptItem,
        destination = TransactionsDestination(),
        destinationType = TransactionsDestination::class
    )

    data object Settings : BottomNavItem(
        route = "settings",
        titleRes = R.string.settings,
        icon = Iconax.Setting2,
        destination = SettingsDestination(),
        destinationType = SettingsDestination::class
    )
}