package com.reddy.vittify.presentation.ui.features.settings.importwizard

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.reddy.vittify.data.backup.importwizard.CategoryMatchStatus
import com.reddy.vittify.data.backup.importwizard.ImportAccountItem
import com.reddy.vittify.data.backup.importwizard.ImportCategoryItem
import com.reddy.vittify.data.backup.importwizard.ImportSource
import com.reddy.vittify.data.backup.importwizard.ImportSubscriptionItem
import com.reddy.vittify.data.backup.importwizard.ImportTransactionItem
import com.reddy.vittify.data.backup.importwizard.ParsedImportData
import com.reddy.vittify.data.backup.importwizard.parsers.CashewSqliteParser
import com.reddy.vittify.data.backup.importwizard.parsers.CsvImportParser
import com.reddy.vittify.data.backup.importwizard.parsers.ExcelImportParser
import com.reddy.vittify.data.backup.importwizard.parsers.VittifyBackupParser
import com.reddy.vittify.data.backup.importwizard.templates.CsvTemplateGenerator
import com.reddy.vittify.data.backup.importwizard.templates.ExcelTemplateGenerator
import com.reddy.vittify.data.database.VittifyDatabase
import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.data.database.entity.CategoryEntity
import com.reddy.vittify.data.database.entity.CategoryType
import com.reddy.vittify.data.database.entity.SubcategoryEntity
import com.reddy.vittify.data.database.entity.SubscriptionEntity
import com.reddy.vittify.data.database.entity.SubscriptionState
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.vittify.data.repository.AccountBalanceRepository
import com.reddy.vittify.data.repository.CategoryRepository
import com.reddy.vittify.data.repository.SubcategoryRepository
import com.reddy.vittify.data.sync.P2pSyncPreferencesRepository
import com.reddy.vittify.utils.NanoId
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ImportWizardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: VittifyDatabase,
    private val cashewSqliteParser: CashewSqliteParser,
    private val vittifyBackupParser: VittifyBackupParser,
    private val excelImportParser: ExcelImportParser,
    private val csvImportParser: CsvImportParser,
    private val excelTemplateGenerator: ExcelTemplateGenerator,
    private val csvTemplateGenerator: CsvTemplateGenerator,
    private val categoryRepository: CategoryRepository,
    private val subcategoryRepository: SubcategoryRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val p2pPreferences: P2pSyncPreferencesRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ImportWizardViewModel"
    }

    private val _uiState = MutableStateFlow(ImportWizardUiState())
    val uiState: StateFlow<ImportWizardUiState> = _uiState.asStateFlow()

    init {
        loadExistingAppData()
    }

    private fun loadExistingAppData() {
        viewModelScope.launch {
            try {
                val cats = categoryRepository.getAllCategories().first()
                val subcats = subcategoryRepository.getAllSubcategories().first()
                val accounts = accountBalanceRepository.getAllBalances().first()

                _uiState.update {
                    it.copy(
                        existingCategories = cats,
                        existingSubcategories = subcats,
                        existingAccounts = accounts
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading existing app data", e)
            }
        }
    }

    fun selectSource(source: ImportSource) {
        _uiState.update {
            it.copy(
                selectedSource = source,
                currentStep = ImportWizardStep.WARNING,
                errorMessage = null
            )
        }
    }

    fun proceedToChooseFile() {
        // UI launcher triggers file picker based on selectedSource
    }

    fun parseSelectedFile(uri: Uri) {
        val source = _uiState.value.selectedSource ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isParsing = true, currentStep = ImportWizardStep.PARSING, errorMessage = null) }
            try {
                val parsed: ParsedImportData = when (source) {
                    ImportSource.VITTIFY -> vittifyBackupParser.parse(uri)
                    ImportSource.EXCEL -> excelImportParser.parse(uri)
                    ImportSource.CASHEW_SQLITE -> cashewSqliteParser.parse(uri)
                    ImportSource.CSV -> csvImportParser.parse(uri)
                }

                // Match categories against existing app categories
                val matchedCategories = autoMapCategories(parsed.categories)

                // Match transactions to accounts
                val matchedTransactions = autoMapTransactions(parsed.transactions, parsed.accounts)

                _uiState.update {
                    it.copy(
                        isParsing = false,
                        parsedData = parsed,
                        accounts = parsed.accounts,
                        categories = matchedCategories,
                        subscriptions = parsed.subscriptions,
                        transactions = matchedTransactions,
                        currentStep = determineFirstPreviewStep(parsed)
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Parsing failed", e)
                _uiState.update {
                    it.copy(
                        isParsing = false,
                        errorMessage = "Failed to parse file: ${e.message}",
                        currentStep = ImportWizardStep.SELECT_SOURCE
                    )
                }
            }
        }
    }

    private fun determineFirstPreviewStep(data: ParsedImportData): ImportWizardStep {
        return when {
            data.accounts.isNotEmpty() -> ImportWizardStep.PREVIEW_BANKS
            data.categories.isNotEmpty() -> ImportWizardStep.PREVIEW_CATEGORIES
            data.subscriptions.isNotEmpty() -> ImportWizardStep.PREVIEW_SUBSCRIPTIONS
            data.transactions.isNotEmpty() -> ImportWizardStep.PREVIEW_TRANSACTIONS
            else -> ImportWizardStep.PREVIEW_SUMMARY
        }
    }

    private fun autoMapCategories(imported: List<ImportCategoryItem>): List<ImportCategoryItem> {
        val appCats = _uiState.value.existingCategories
        val appSubcats = _uiState.value.existingSubcategories

        return imported.map { item ->
            val cleanName = item.name.trim().lowercase(Locale.ROOT)

            // 1. Exact match on parent category
            val exactCat = appCats.firstOrNull { it.name.trim().lowercase(Locale.ROOT) == cleanName }
            if (exactCat != null) {
                return@map item.copy(
                    matchStatus = CategoryMatchStatus.EXACT_MATCH,
                    mappedCategoryId = exactCat.id,
                    mappedCategoryName = exactCat.name,
                    createAsNew = false
                )
            }

            // 2. Exact match on subcategory
            val exactSub = appSubcats.firstOrNull { it.name.trim().lowercase(Locale.ROOT) == cleanName }
            if (exactSub != null) {
                val parent = appCats.firstOrNull { it.id == exactSub.categoryId }
                return@map item.copy(
                    matchStatus = CategoryMatchStatus.EXACT_MATCH,
                    mappedCategoryId = exactSub.categoryId,
                    mappedCategoryName = parent?.name,
                    mappedSubcategoryId = exactSub.id,
                    mappedSubcategoryName = exactSub.name,
                    createAsNew = false
                )
            }

            // 3. Fuzzy / partial match
            val fuzzyCat = appCats.firstOrNull {
                val catLow = it.name.trim().lowercase(Locale.ROOT)
                cleanName.contains(catLow) || catLow.contains(cleanName)
            }
            if (fuzzyCat != null) {
                return@map item.copy(
                    matchStatus = CategoryMatchStatus.SUGGESTED_MATCH,
                    mappedCategoryId = fuzzyCat.id,
                    mappedCategoryName = fuzzyCat.name,
                    createAsNew = false
                )
            }

            // Otherwise, keep as new category
            item.copy(
                matchStatus = CategoryMatchStatus.NEW_CATEGORY,
                createAsNew = true
            )
        }
    }

    private fun autoMapTransactions(
        transactions: List<ImportTransactionItem>,
        accounts: List<ImportAccountItem>
    ): List<ImportTransactionItem> {
        val accountNameMap = accounts.associateBy { it.bankName.trim().lowercase(Locale.ROOT) }
        val accountLast4Map = accounts.filter { it.accountLast4.isNotBlank() }.associateBy { it.accountLast4.trim() }

        return transactions.map { txn ->
            val bankMatch = txn.originalBankName?.let { accountNameMap[it.trim().lowercase(Locale.ROOT)] }
            val last4Match = txn.originalAccountLast4?.takeIf { it.isNotBlank() }?.let { accountLast4Map[it.trim()] }
            val fromAccount = txn.mappedAccountId ?: bankMatch?.id ?: last4Match?.id ?: accounts.firstOrNull()?.id

            val isTransfer = txn.transactionType == TransactionType.TRANSFER
            val toAccount = if (isTransfer) {
                val toBankMatch = txn.toBankName?.let { accountNameMap[it.trim().lowercase(Locale.ROOT)] }
                val toLast4Match = txn.toAccountLast4?.takeIf { it.isNotBlank() }?.let { accountLast4Map[it.trim()] }
                txn.mappedToAccountId ?: toBankMatch?.id ?: toLast4Match?.id ?: accounts.firstOrNull { it.id != fromAccount }?.id ?: accounts.firstOrNull()?.id
            } else null

            txn.copy(
                mappedAccountId = fromAccount,
                mappedToAccountId = toAccount
            )
        }
    }

    fun mapTransactionToAccount(index: Int, toAccountId: String) {
        val list = _uiState.value.transactions.toMutableList()
        if (index in list.indices) {
            val toAcc = _uiState.value.accounts.firstOrNull { it.id == toAccountId }
            list[index] = list[index].copy(
                mappedToAccountId = toAccountId,
                toBankName = toAcc?.bankName
            )
            _uiState.update { it.copy(transactions = list) }
        }
    }

    // --- STEP NAVIGATION ---
    fun goToNextStep() {
        val current = _uiState.value.currentStep
        val next = when (current) {
            ImportWizardStep.PREVIEW_BANKS -> ImportWizardStep.PREVIEW_CATEGORIES
            ImportWizardStep.PREVIEW_CATEGORIES -> {
                if (_uiState.value.subscriptions.isNotEmpty()) ImportWizardStep.PREVIEW_SUBSCRIPTIONS
                else ImportWizardStep.PREVIEW_TRANSACTIONS
            }
            ImportWizardStep.PREVIEW_SUBSCRIPTIONS -> ImportWizardStep.PREVIEW_TRANSACTIONS
            ImportWizardStep.PREVIEW_TRANSACTIONS -> ImportWizardStep.PREVIEW_SUMMARY
            ImportWizardStep.PREVIEW_SUMMARY -> ImportWizardStep.IMPORTING
            else -> current
        }
        _uiState.update { it.copy(currentStep = next) }
    }

    fun goToPreviousStep() {
        val current = _uiState.value.currentStep
        val prev = when (current) {
            ImportWizardStep.WARNING -> ImportWizardStep.SELECT_SOURCE
            ImportWizardStep.PARSING -> ImportWizardStep.WARNING
            ImportWizardStep.PREVIEW_BANKS -> ImportWizardStep.WARNING
            ImportWizardStep.PREVIEW_CATEGORIES -> {
                if (_uiState.value.accounts.isNotEmpty()) ImportWizardStep.PREVIEW_BANKS
                else ImportWizardStep.WARNING
            }
            ImportWizardStep.PREVIEW_SUBSCRIPTIONS -> ImportWizardStep.PREVIEW_CATEGORIES
            ImportWizardStep.PREVIEW_TRANSACTIONS -> {
                if (_uiState.value.subscriptions.isNotEmpty()) ImportWizardStep.PREVIEW_SUBSCRIPTIONS
                else ImportWizardStep.PREVIEW_CATEGORIES
            }
            ImportWizardStep.PREVIEW_SUMMARY -> ImportWizardStep.PREVIEW_TRANSACTIONS
            else -> current
        }
        _uiState.update { it.copy(currentStep = prev) }
    }

    // --- ACCOUNTS PREVIEW OPERATIONS ---
    fun toggleAccount(index: Int) {
        val list = _uiState.value.accounts.toMutableList()
        if (index in list.indices) {
            list[index] = list[index].copy(isSelected = !list[index].isSelected)
            val updatedTracker = _uiState.value.rangeTrackerBanks.withToggled(index)
            _uiState.update { it.copy(accounts = list, rangeTrackerBanks = updatedTracker) }
        }
    }

    fun selectAllAccounts() {
        _uiState.update { state ->
            state.copy(
                accounts = state.accounts.map { it.copy(isSelected = true) },
                rangeTrackerBanks = state.rangeTrackerBanks.reset()
            )
        }
    }

    fun deselectAllAccounts() {
        _uiState.update { state ->
            state.copy(
                accounts = state.accounts.map { it.copy(isSelected = false) },
                rangeTrackerBanks = state.rangeTrackerBanks.reset()
            )
        }
    }

    fun applyRangeSelectionAccounts() {
        val range = _uiState.value.rangeTrackerBanks.rangeIndices ?: return
        _uiState.update { state ->
            state.copy(
                accounts = state.accounts.mapIndexed { idx, item ->
                    if (idx in range) item.copy(isSelected = true) else item
                },
                rangeTrackerBanks = state.rangeTrackerBanks.reset()
            )
        }
    }

    fun updateAccount(index: Int, updatedAccount: ImportAccountItem) {
        val list = _uiState.value.accounts.toMutableList()
        if (index in list.indices) {
            val oldAccount = list[index]
            list[index] = updatedAccount
            val updatedTxns = _uiState.value.transactions.map { txn ->
                if (txn.mappedAccountId == oldAccount.id || txn.originalBankName.equals(oldAccount.bankName, ignoreCase = true)) {
                    txn.copy(mappedAccountId = updatedAccount.id, originalBankName = updatedAccount.bankName)
                } else txn
            }
            _uiState.update { it.copy(accounts = list, transactions = updatedTxns) }
        }
    }

    // --- CATEGORIES PREVIEW OPERATIONS ---
    fun toggleCategory(index: Int) {
        val list = _uiState.value.categories.toMutableList()
        if (index in list.indices) {
            list[index] = list[index].copy(isSelected = !list[index].isSelected)
            val updatedTracker = _uiState.value.rangeTrackerCategories.withToggled(index)
            _uiState.update { it.copy(categories = list, rangeTrackerCategories = updatedTracker) }
        }
    }

    fun selectAllCategories() {
        _uiState.update { state ->
            state.copy(
                categories = state.categories.map { it.copy(isSelected = true) },
                rangeTrackerCategories = state.rangeTrackerCategories.reset()
            )
        }
    }

    fun deselectAllCategories() {
        _uiState.update { state ->
            state.copy(
                categories = state.categories.map { it.copy(isSelected = false) },
                rangeTrackerCategories = state.rangeTrackerCategories.reset()
            )
        }
    }

    fun applyRangeSelectionCategories() {
        val range = _uiState.value.rangeTrackerCategories.rangeIndices ?: return
        _uiState.update { state ->
            state.copy(
                categories = state.categories.mapIndexed { idx, item ->
                    if (idx in range) item.copy(isSelected = true) else item
                },
                rangeTrackerCategories = state.rangeTrackerCategories.reset()
            )
        }
    }

    fun mapCategorySelection(index: Int, category: CategoryEntity, subcategory: SubcategoryEntity?) {
        val list = _uiState.value.categories.toMutableList()
        if (index in list.indices) {
            if (subcategory == null) {
                // User picked parent category: will create a subcategory named item.name under this category
                list[index] = list[index].copy(
                    matchStatus = CategoryMatchStatus.EXACT_MATCH,
                    mappedCategoryId = category.id,
                    mappedCategoryName = category.name,
                    mappedSubcategoryId = null,
                    mappedSubcategoryName = null,
                    createAsSubcategoryUnderParent = true,
                    createAsNew = false
                )
            } else {
                // User picked existing subcategory: map directly to this subcategory
                list[index] = list[index].copy(
                    matchStatus = CategoryMatchStatus.EXACT_MATCH,
                    mappedCategoryId = category.id,
                    mappedCategoryName = category.name,
                    mappedSubcategoryId = subcategory.id,
                    mappedSubcategoryName = subcategory.name,
                    createAsSubcategoryUnderParent = false,
                    createAsNew = false
                )
            }
            _uiState.update { it.copy(categories = list) }
        }
    }

    fun toggleCreateCategoryAsNew(index: Int, isNew: Boolean) {
        val list = _uiState.value.categories.toMutableList()
        if (index in list.indices) {
            list[index] = list[index].copy(
                createAsNew = isNew,
                createAsSubcategoryUnderParent = false,
                matchStatus = if (isNew) CategoryMatchStatus.NEW_CATEGORY else list[index].matchStatus,
                mappedCategoryId = if (isNew) null else list[index].mappedCategoryId,
                mappedCategoryName = if (isNew) null else list[index].mappedCategoryName,
                mappedSubcategoryId = if (isNew) null else list[index].mappedSubcategoryId,
                mappedSubcategoryName = if (isNew) null else list[index].mappedSubcategoryName
            )
            _uiState.update { it.copy(categories = list) }
        }
    }

    // --- SUBSCRIPTIONS PREVIEW OPERATIONS ---
    fun toggleSubscription(index: Int) {
        val list = _uiState.value.subscriptions.toMutableList()
        if (index in list.indices) {
            list[index] = list[index].copy(isSelected = !list[index].isSelected)
            val updatedTracker = _uiState.value.rangeTrackerSubscriptions.withToggled(index)
            _uiState.update { it.copy(subscriptions = list, rangeTrackerSubscriptions = updatedTracker) }
        }
    }

    fun selectAllSubscriptions() {
        _uiState.update { state ->
            state.copy(
                subscriptions = state.subscriptions.map { it.copy(isSelected = true) },
                rangeTrackerSubscriptions = state.rangeTrackerSubscriptions.reset()
            )
        }
    }

    fun deselectAllSubscriptions() {
        _uiState.update { state ->
            state.copy(
                subscriptions = state.subscriptions.map { it.copy(isSelected = false) },
                rangeTrackerSubscriptions = state.rangeTrackerSubscriptions.reset()
            )
        }
    }

    fun applyRangeSelectionSubscriptions() {
        val range = _uiState.value.rangeTrackerSubscriptions.rangeIndices ?: return
        _uiState.update { state ->
            state.copy(
                subscriptions = state.subscriptions.mapIndexed { idx, item ->
                    if (idx in range) item.copy(isSelected = true) else item
                },
                rangeTrackerSubscriptions = state.rangeTrackerSubscriptions.reset()
            )
        }
    }

    // --- TRANSACTIONS PREVIEW OPERATIONS ---
    fun toggleTransaction(index: Int) {
        val list = _uiState.value.transactions.toMutableList()
        if (index in list.indices) {
            list[index] = list[index].copy(isSelected = !list[index].isSelected)
            val updatedTracker = _uiState.value.rangeTrackerTransactions.withToggled(index)
            _uiState.update { it.copy(transactions = list, rangeTrackerTransactions = updatedTracker) }
        }
    }

    fun selectAllTransactions() {
        _uiState.update { state ->
            state.copy(
                transactions = state.transactions.map { it.copy(isSelected = true) },
                rangeTrackerTransactions = state.rangeTrackerTransactions.reset()
            )
        }
    }

    fun deselectAllTransactions() {
        _uiState.update { state ->
            state.copy(
                transactions = state.transactions.map { it.copy(isSelected = false) },
                rangeTrackerTransactions = state.rangeTrackerTransactions.reset()
            )
        }
    }

    fun applyRangeSelectionTransactions() {
        val range = _uiState.value.rangeTrackerTransactions.rangeIndices ?: return
        _uiState.update { state ->
            state.copy(
                transactions = state.transactions.mapIndexed { idx, item ->
                    if (idx in range) item.copy(isSelected = true) else item
                },
                rangeTrackerTransactions = state.rangeTrackerTransactions.reset()
            )
        }
    }

    fun mapTransactionAccount(index: Int, accountId: String) {
        val list = _uiState.value.transactions.toMutableList()
        if (index in list.indices) {
            list[index] = list[index].copy(mappedAccountId = accountId)
            _uiState.update { it.copy(transactions = list) }
        }
    }

    fun bulkMapUnmappedTransactions(accountId: String) {
        _uiState.update { state ->
            state.copy(
                transactions = state.transactions.map { txn ->
                    if (txn.mappedAccountId.isNullOrBlank()) txn.copy(mappedAccountId = accountId) else txn
                }
            )
        }
    }

    // --- IMPORT EXECUTION WITH DATA WIPE ---
    fun executeImport() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    currentStep = ImportWizardStep.IMPORTING,
                    importProgressFraction = 0.05f,
                    importProgressMessage = "Preparing database..."
                )
            }

            try {
                withContext(Dispatchers.IO) {
                    val currentDeviceId = p2pPreferences.getDeviceId()

                    database.withTransaction {
                        // 1. Wipe all existing data while PRESERVING default system categories & subcategories
                        _uiState.update { it.copy(importProgressFraction = 0.15f, importProgressMessage = "Wiping existing data...") }
                        database.transactionDao().deleteAllTransactions()
                        database.subcategoryDao().deleteNonSystemSubcategories()
                        database.categoryDao().deleteNonSystemCategories()
                        database.cardDao().deleteAllCards()
                        database.accountBalanceDao().deleteAllBalances()
                        database.subscriptionDao().deleteAllSubscriptions()
                        database.merchantMappingDao().deleteAllMappings()
                        database.unrecognizedSmsDao().deleteAll()
                        database.budgetDao().deleteAllBudgets()
                        database.ruleDao().deleteAllRules()
                        database.ruleApplicationDao().deleteAllApplications()
                        database.exchangeRateDao().deleteAllRates()
                        database.transactionItemDao().deleteAllItems()

                        // 2. Insert Selected Bank Accounts
                        _uiState.update { it.copy(importProgressFraction = 0.35f, importProgressMessage = "Importing bank accounts...") }
                        val activeAccounts = _uiState.value.accounts.filter { it.isSelected }
                        val accountIdMap = mutableMapOf<String, String>() // oldId -> newId

                        activeAccounts.forEach { acc ->
                            val finalId = acc.id.ifBlank { NanoId.generate() }
                            val entity = AccountBalanceEntity(
                                id = finalId,
                                bankName = acc.bankName,
                                accountLast4 = acc.accountLast4,
                                accountName = acc.accountName,
                                balance = acc.balance,
                                currency = acc.currency,
                                isCreditCard = acc.isCreditCard,
                                isWallet = acc.isWallet,
                                color = acc.color,
                                ownerId = currentDeviceId
                            )
                            database.accountBalanceDao().insertBalance(entity)
                            acc.originalId?.let { accountIdMap[it] = finalId }
                            accountIdMap[acc.id] = finalId
                        }

                        // Fallback default account if none selected
                        val primaryAccountId = activeAccounts.firstOrNull()?.id ?: run {
                            val defaultAcc = AccountBalanceEntity(
                                id = NanoId.generate(),
                                bankName = "Primary Account",
                                accountLast4 = "",
                                accountName = "Primary Account",
                                ownerId = currentDeviceId
                            )
                            database.accountBalanceDao().insertBalance(defaultAcc)
                            defaultAcc.id
                        }

                        // 3. Insert / Map Categories
                        _uiState.update { it.copy(importProgressFraction = 0.55f, importProgressMessage = "Configuring categories...") }
                        val activeCategories = _uiState.value.categories.filter { it.isSelected }
                        val categoryNameToFinalName = mutableMapOf<String, String>()
                        val subcategoryNameToFinalName = mutableMapOf<String, String>()

                        activeCategories.forEach { cat ->
                            if (cat.createAsNew) {
                                if (cat.parentCategoryName.isNullOrBlank()) {
                                    // New parent category
                                    val existing = database.categoryDao().getCategoryByName(cat.name)
                                    if (existing == null) {
                                        val catEntity = CategoryEntity(
                                            name = cat.name,
                                            color = cat.color,
                                            iconName = cat.iconName,
                                            isIncome = cat.isIncome,
                                            categoryType = if (cat.isIncome) CategoryType.INCOME else CategoryType.EXPENSE
                                        )
                                        database.categoryDao().insertCategory(catEntity)
                                    }
                                    categoryNameToFinalName[cat.name] = cat.name
                                } else {
                                    // New subcategory under parent
                                    var parent = database.categoryDao().getCategoryByName(cat.parentCategoryName)
                                    if (parent == null) {
                                        val parentEntity = CategoryEntity(
                                            name = cat.parentCategoryName,
                                            color = "#757575",
                                            isIncome = cat.isIncome
                                        )
                                        val pId = database.categoryDao().insertCategory(parentEntity)
                                        parent = database.categoryDao().getCategoryById(pId)
                                    }
                                    if (parent != null) {
                                        val subEntity = SubcategoryEntity(
                                            categoryId = parent.id,
                                            name = cat.name,
                                            color = cat.color,
                                            iconName = cat.iconName
                                        )
                                        database.subcategoryDao().insertSubcategory(subEntity)
                                        subcategoryNameToFinalName[cat.name] = cat.name
                                        categoryNameToFinalName[cat.name] = parent.name
                                    }
                                }
                            } else if (cat.createAsSubcategoryUnderParent && cat.mappedCategoryId != null) {
                                // Create a subcategory with this imported category's name under chosen parent category
                                val existingSubs = database.subcategoryDao().getSubcategoriesByCategoryId(cat.mappedCategoryId).first()
                                if (existingSubs.none { it.name.equals(cat.name, ignoreCase = true) }) {
                                    val subEntity = SubcategoryEntity(
                                        categoryId = cat.mappedCategoryId,
                                        name = cat.name,
                                        color = cat.color,
                                        iconName = cat.iconName
                                    )
                                    database.subcategoryDao().insertSubcategory(subEntity)
                                }
                                categoryNameToFinalName[cat.name] = cat.mappedCategoryName ?: ""
                                subcategoryNameToFinalName[cat.name] = cat.name
                            } else {
                                // Mapped to existing in-app category
                                cat.mappedCategoryName?.let { categoryNameToFinalName[cat.name] = it }
                                cat.mappedSubcategoryName?.let { subcategoryNameToFinalName[cat.name] = it }
                            }
                        }

                        // 4. Insert Subscriptions
                        _uiState.update { it.copy(importProgressFraction = 0.70f, importProgressMessage = "Adding subscriptions...") }
                        val activeSubscriptions = _uiState.value.subscriptions.filter { it.isSelected }
                        activeSubscriptions.forEach { sub ->
                            val finalCat = sub.categoryName?.let { categoryNameToFinalName[it] ?: it }
                            val subEntity = SubscriptionEntity(
                                merchantName = sub.merchantName,
                                amount = sub.amount,
                                billingCycle = sub.billingCycle,
                                nextPaymentDate = sub.nextPaymentDate,
                                category = finalCat,
                                bankName = sub.bankName,
                                currency = sub.currency,
                                ownerId = currentDeviceId,
                                state = SubscriptionState.ACTIVE
                            )
                            database.subscriptionDao().insertSubscription(subEntity)
                        }

                        // 5. Insert Transactions
                        _uiState.update { it.copy(importProgressFraction = 0.85f, importProgressMessage = "Writing transactions...") }
                        val activeTransactions = _uiState.value.transactions.filter { it.isSelected }
                        val transactionEntities = activeTransactions.map { txn ->
                            val isTransfer = txn.transactionType == TransactionType.TRANSFER
                            val finalAccountId = txn.mappedAccountId?.let { accountIdMap[it] ?: it } ?: primaryAccountId
                            val finalToAccountId = if (isTransfer) {
                                txn.mappedToAccountId?.let { accountIdMap[it] ?: it }
                            } else null
                            val finalCat = categoryNameToFinalName[txn.categoryName] ?: txn.categoryName
                            val finalSubcat = txn.subcategoryName?.let { subcategoryNameToFinalName[it] ?: it }

                            TransactionEntity(
                                amount = txn.amount,
                                targetAmount = if (isTransfer) txn.targetAmount else null,
                                merchantName = txn.merchantName,
                                category = finalCat,
                                subcategory = finalSubcat,
                                transactionType = txn.transactionType,
                                dateTime = txn.dateTime,
                                description = txn.description,
                                currency = txn.currency,
                                accountId = finalAccountId,
                                fromAccountId = if (isTransfer) finalAccountId else null,
                                toAccountId = finalToAccountId,
                                fromAccount = txn.originalBankName,
                                toAccount = txn.toBankName,
                                uuid = UUID.randomUUID().toString(),
                                ownerId = currentDeviceId
                            )
                        }

                        database.transactionDao().insertTransactions(transactionEntities)

                        p2pPreferences.setInitialDataSeeded(false)

                        _uiState.update {
                            it.copy(
                                importProgressFraction = 1f,
                                importProgressMessage = "Import completed successfully!",
                                importedAccountsCount = activeAccounts.size,
                                importedCategoriesCount = activeCategories.size,
                                importedSubscriptionsCount = activeSubscriptions.size,
                                importedTransactionsCount = transactionEntities.size,
                                currentStep = ImportWizardStep.SUCCESS
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Import execution failed", e)
                _uiState.update {
                    it.copy(
                        errorMessage = "Import failed: ${e.message}",
                        currentStep = ImportWizardStep.PREVIEW_SUMMARY
                    )
                }
            }
        }
    }

    // --- TEMPLATE EXPORTS ---
    fun downloadExcelTemplate(uri: Uri) {
        viewModelScope.launch {
            try {
                val ok = excelTemplateGenerator.saveTemplateToUri(uri)
                if (ok) {
                    _uiState.update { it.copy(infoMessage = "Excel template saved successfully!") }
                } else {
                    _uiState.update { it.copy(errorMessage = "Could not save Excel template") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error generating template: ${e.message}") }
            }
        }
    }

    fun downloadCsvTemplate(uri: Uri) {
        viewModelScope.launch {
            try {
                val ok = csvTemplateGenerator.saveTemplateToUri(uri)
                if (ok) {
                    _uiState.update { it.copy(infoMessage = "CSV template saved successfully!") }
                } else {
                    _uiState.update { it.copy(errorMessage = "Could not save CSV template") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error generating template: ${e.message}") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
    }
}
