package com.reddy.vittify.data.preferences

enum class HomeWidget(val displayName: String, val defaultOrder: Int) {
    QUICK_ADD("Quick Add", 0),
    NETWORTH_SUMMARY("Net Worth", 1),
    FINANCIAL_OVERVIEW("Financial Overview", 2),
    ACCOUNT_CAROUSEL("Accounts", 3),
    UPCOMING_SUBSCRIPTIONS("Upcoming Subscriptions", 4),
    RECENT_TRANSACTIONS("Recent Transactions", 5),
    BUDGET_CAROUSEL("Budgets", 6),
    TRANSACTION_HEATMAP("Activity Heatmap", 7),
    SHORTCUTS("Quick Shortcuts", 8);
    
    companion object {
        fun fromName(name: String): HomeWidget? {
            return entries.find { it.name == name }
        }
    }
}
