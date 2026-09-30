package com.reddy.vittify.presentation.ui.features.settings.sanitization

import androidx.annotation.StringRes
import com.reddy.vittify.R
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.model.DataHealthAudit

enum class SanitizationTab(val title: String) {
    OVERVIEW("Overview"),
    OWNERSHIP("Ownership"),
    ACCOUNTS("Accounts"),
    TRANSACTIONS("Transactions"),
    ARCHIVED("Archived"),
    SYSTEM("System & Junk")
}

data class DataSanitizationUiState(
    val isLoading: Boolean = true,
    val isScanning: Boolean = false,
    val healthAudit: DataHealthAudit = DataHealthAudit(),
    val activeTab: SanitizationTab = SanitizationTab.OVERVIEW,
    val expandedSectionId: String? = null,
    val myDeviceId: String = "",
    val partnerUserId: String? = null,
    val partnerName: String? = null,
    val isCoupleTrackingEnabled: Boolean = false,
    val userMessage: String? = null,
    val errorMessage: String? = null,

    // Archived Transactions Tab State
    val archivedSearchQuery: String = "",
    val isArchivedSelectionMode: Boolean = false,
    val archivedSelectedIds: Set<Long> = emptySet(),
    val archivedTransactions: List<TransactionEntity> = emptyList(),
    val allArchivedTransactions: List<TransactionEntity> = emptyList(),
    val autoDeleteArchivedEnabled: Boolean = false,
    val autoDeleteArchivedDays: Int = 30,
    val lastRestoredTransactions: List<TransactionEntity>? = null
)
