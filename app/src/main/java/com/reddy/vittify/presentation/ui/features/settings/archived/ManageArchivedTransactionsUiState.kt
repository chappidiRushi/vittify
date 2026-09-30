package com.reddy.vittify.presentation.ui.features.settings.archived

import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.data.database.entity.CategoryEntity
import com.reddy.vittify.data.database.entity.SubcategoryEntity
import com.reddy.vittify.data.database.entity.TransactionEntity
import java.math.BigDecimal

data class ManageArchivedTransactionsUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val filteredTransactions: List<TransactionEntity> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val autoDeleteEnabled: Boolean = true,
    val autoDeleteDays: Int = 30,
    val categoriesMap: Map<String, CategoryEntity> = emptyMap(),
    val subcategoriesMap: Map<String, SubcategoryEntity> = emptyMap(),
    val accountsMap: Map<String, AccountBalanceEntity> = emptyMap(),
    val baseCurrency: String = "INR",
    val totalAmount: BigDecimal = BigDecimal.ZERO,
    val userMessage: String? = null
)
