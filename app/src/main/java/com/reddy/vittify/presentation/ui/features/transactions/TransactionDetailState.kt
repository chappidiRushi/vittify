package com.reddy.vittify.presentation.ui.features.transactions

import com.reddy.vittify.data.database.entity.SubscriptionEntity
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.database.entity.TransactionItemEntity
import java.math.BigDecimal
import java.time.LocalDate

data class TransactionDetailUiState(
    val transaction: TransactionEntity? = null,
    val primaryCurrency: String = "INR",
    val convertedAmount: BigDecimal? = null,
    val isEditMode: Boolean = false,
    val editableTransaction: TransactionEntity? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null,
    val applyToAllFromMerchant: Boolean = false,
    val updateExistingTransactions: Boolean = false,
    val existingTransactionCount: Int = 0,
    val showDeleteDialog: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteSuccess: Boolean = false,
    val duplicateSuccess: Boolean = false,
    val subscription: SubscriptionEntity? = null,
    val accountIconName: String? = null,
    val isCustomCycle: Boolean = false,
    val customCycleCount: Int = 1,
    val customCycleUnit: String = "month",
    val customCycleEndDate: LocalDate? = null,
    // Preview match sheet state
    val showMatchPreviewSheet: Boolean = false,
    val matchedTransactions: List<TransactionEntity> = emptyList(),
    val selectedMatchIds: Set<Long> = emptySet(),
    val isLoadingMatches: Boolean = false,
    val matchSearchQuery: String = "",
    val matchSearchResults: List<TransactionEntity> = emptyList(),
    val isAmoledMode: Boolean = false,
    val darkThemeConfig: Boolean? = null,
    val transactionItems: List<TransactionItemEntity> = emptyList(),
    val editableItems: List<TransactionItemEntity> = emptyList(),
    val transferExchangeRate: BigDecimal? = null,
    val targetAmount: BigDecimal? = null,
    val isLoadingTransferRate: Boolean = false
)
