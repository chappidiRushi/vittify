package com.reddy.vittify.presentation.ui.features.settings.importwizard

import com.reddy.vittify.data.backup.importwizard.ImportAccountItem
import com.reddy.vittify.data.backup.importwizard.ImportCategoryItem
import com.reddy.vittify.data.backup.importwizard.ImportSource
import com.reddy.vittify.data.backup.importwizard.ImportSubscriptionItem
import com.reddy.vittify.data.backup.importwizard.ImportTransactionItem
import com.reddy.vittify.data.backup.importwizard.ParsedImportData
import com.reddy.vittify.data.backup.importwizard.RangeSelectionTracker
import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.data.database.entity.CategoryEntity
import com.reddy.vittify.data.database.entity.SubcategoryEntity

enum class ImportWizardStep(val stepNumber: Int, val title: String) {
    SELECT_SOURCE(1, "Select Source"),
    WARNING(2, "Data Wipe Warning"),
    PARSING(3, "Reading File"),
    PREVIEW_BANKS(4, "Bank Accounts"),
    PREVIEW_CATEGORIES(5, "Categories"),
    PREVIEW_SUBSCRIPTIONS(6, "Subscriptions"),
    PREVIEW_TRANSACTIONS(7, "Transactions"),
    PREVIEW_SUMMARY(8, "Import Preview"),
    IMPORTING(9, "Importing Data"),
    SUCCESS(10, "Import Completed")
}

data class ImportWizardUiState(
    val currentStep: ImportWizardStep = ImportWizardStep.SELECT_SOURCE,
    val selectedSource: ImportSource? = null,
    val parsedData: ParsedImportData? = null,
    val isParsing: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,

    // Step items
    val accounts: List<ImportAccountItem> = emptyList(),
    val categories: List<ImportCategoryItem> = emptyList(),
    val subscriptions: List<ImportSubscriptionItem> = emptyList(),
    val transactions: List<ImportTransactionItem> = emptyList(),

    // In-app entities for matching
    val existingAccounts: List<AccountBalanceEntity> = emptyList(),
    val existingCategories: List<CategoryEntity> = emptyList(),
    val existingSubcategories: List<SubcategoryEntity> = emptyList(),

    // Range selection trackers
    val rangeTrackerBanks: RangeSelectionTracker = RangeSelectionTracker(),
    val rangeTrackerCategories: RangeSelectionTracker = RangeSelectionTracker(),
    val rangeTrackerSubscriptions: RangeSelectionTracker = RangeSelectionTracker(),
    val rangeTrackerTransactions: RangeSelectionTracker = RangeSelectionTracker(),

    // Import execution state
    val importProgressFraction: Float = 0f,
    val importProgressMessage: String = "",
    val importedAccountsCount: Int = 0,
    val importedCategoriesCount: Int = 0,
    val importedSubscriptionsCount: Int = 0,
    val importedTransactionsCount: Int = 0
) {
    val selectedAccountsCount: Int get() = accounts.count { it.isSelected }
    val selectedCategoriesCount: Int get() = categories.count { it.isSelected }
    val selectedSubscriptionsCount: Int get() = subscriptions.count { it.isSelected }
    val selectedTransactionsCount: Int get() = transactions.count { it.isSelected }

    val subcategoriesMap: Map<Long, List<SubcategoryEntity>>
        get() = existingSubcategories.groupBy { it.categoryId }

    val hasUnmappedTransactions: Boolean
        get() = transactions.any { it.isSelected && it.mappedAccountId.isNullOrBlank() }

    val unmappedTransactionsCount: Int
        get() = transactions.count { it.isSelected && it.mappedAccountId.isNullOrBlank() }
}
