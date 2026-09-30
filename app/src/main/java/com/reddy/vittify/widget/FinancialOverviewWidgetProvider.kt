package com.reddy.vittify.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.reddy.vittify.MainActivity
import com.reddy.vittify.R
import com.reddy.vittify.data.currency.CurrencyConversionService
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.vittify.data.repository.AccountBalanceRepository
import com.reddy.vittify.data.repository.CurrencyRepository
import com.reddy.vittify.data.repository.SubscriptionRepository
import com.reddy.vittify.data.repository.TransactionRepository
import com.reddy.vittify.utils.CurrencyFormatter
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FinancialOverviewWidgetProvider : AppWidgetProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface FinancialOverviewWidgetEntryPoint {
        fun accountBalanceRepository(): AccountBalanceRepository
        fun transactionRepository(): TransactionRepository
        fun subscriptionRepository(): SubscriptionRepository
        fun currencyRepository(): CurrencyRepository
        fun currencyConversionService(): CurrencyConversionService
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                updateWidgetsInternal(context.applicationContext, appWidgetManager, appWidgetIds)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update Financial Overview widget", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "FinancialOverviewWidget"

        fun requestUpdate(context: Context) {
            val appContext = context.applicationContext
            val appWidgetManager = AppWidgetManager.getInstance(appContext)
            val componentName = ComponentName(appContext, FinancialOverviewWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isEmpty()) return

            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    updateWidgetsInternal(appContext, appWidgetManager, appWidgetIds)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to refresh Financial Overview widget", e)
                }
            }
        }

        private suspend fun updateWidgetsInternal(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val entryPoint = EntryPointAccessors.fromApplication(
                context,
                FinancialOverviewWidgetEntryPoint::class.java
            )

            val baseCurrency = entryPoint.currencyRepository().effectiveBaseCurrencyCode.first()
            val conversionService = entryPoint.currencyConversionService()

            // 1. Calculate Net Worth
            val sharedPrefs = context.getSharedPreferences("account_prefs", Context.MODE_PRIVATE)
            val hiddenAccounts = sharedPrefs.getStringSet("hidden_accounts", emptySet()) ?: emptySet()
            val allBalances = entryPoint.accountBalanceRepository().getAllLatestBalances().first()
                .filter { account ->
                    val key = "${account.bankName}_${account.accountLast4}"
                    !hiddenAccounts.contains(key)
                }

            var netWorth = BigDecimal.ZERO
            for (account in allBalances) {
                val amt = if (account.currency == baseCurrency) {
                    account.balance
                } else {
                    conversionService.convertAmount(
                        amount = account.balance,
                        fromCurrency = account.currency,
                        toCurrency = baseCurrency
                    )
                }
                if (account.isCreditCard) {
                    netWorth = netWorth.subtract(amt)
                } else {
                    netWorth = netWorth.add(amt)
                }
            }

            // 2. Calculate Active Subscriptions
            val activeSubscriptionsCount = entryPoint.subscriptionRepository()
                .getActiveSubscriptions()
                .first()
                .size

            // 3. Calculate Current Month Income & Expense
            val now = LocalDate.now()
            val firstDay = now.withDayOfMonth(1)
            val lastDay = now.withDayOfMonth(now.lengthOfMonth())
            val monthTransactions = entryPoint.transactionRepository()
                .getTransactionsBetweenDates(firstDay, lastDay)
                .first()

            var monthlyIncome = BigDecimal.ZERO
            var monthlyExpense = BigDecimal.ZERO
            for (txn in monthTransactions) {
                val amt = if (txn.currency == baseCurrency) {
                    txn.amount
                } else {
                    conversionService.convertAmount(
                        amount = txn.amount,
                        fromCurrency = txn.currency,
                        toCurrency = baseCurrency
                    )
                }
                when (txn.transactionType) {
                    TransactionType.INCOME -> monthlyIncome = monthlyIncome.add(amt)
                    TransactionType.EXPENSE -> monthlyExpense = monthlyExpense.add(amt)
                    else -> {}
                }
            }

            val formattedNetWorth = CurrencyFormatter.formatCurrency(netWorth, baseCurrency)
            val formattedUpcoming = if (activeSubscriptionsCount == 1) {
                context.getString(R.string.active_subscriptions_singular)
            } else {
                context.getString(R.string.active_subscriptions_plural, activeSubscriptionsCount)
            }
            val formattedExpense = CurrencyFormatter.formatCurrency(monthlyExpense, baseCurrency)
            val formattedIncome = CurrencyFormatter.formatCurrency(monthlyIncome, baseCurrency)

            for (appWidgetId in appWidgetIds) {
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val views = RemoteViews(context.packageName, R.layout.widget_financial_overview).apply {
                    setTextViewText(R.id.widget_net_worth_value, formattedNetWorth)
                    setTextViewText(R.id.widget_upcoming_value, formattedUpcoming)
                    setTextViewText(R.id.widget_expense_value, formattedExpense)
                    setTextViewText(R.id.widget_income_value, formattedIncome)
                    setOnClickPendingIntent(android.R.id.background, pendingIntent)
                    setOnClickPendingIntent(R.id.widget_financial_overview_container, pendingIntent)
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}
