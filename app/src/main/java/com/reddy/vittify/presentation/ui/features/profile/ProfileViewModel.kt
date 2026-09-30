package com.reddy.vittify.presentation.ui.features.profile

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reddy.vittify.data.currency.CurrencyConversionService
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.vittify.data.preferences.UserPreferencesRepository
import com.reddy.vittify.data.repository.AccountBalanceRepository
import com.reddy.vittify.data.repository.CurrencyRepository
import com.reddy.vittify.data.repository.SubscriptionRepository
import com.reddy.vittify.data.repository.TransactionRepository
import com.reddy.vittify.utils.ImageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val transactionRepository: TransactionRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val currencyRepository: CurrencyRepository,
    private val currencyConversionService: CurrencyConversionService
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileScreenState())
    val state: StateFlow<ProfileScreenState> = _state.asStateFlow()

    init {
        observeBaseCurrency()
        observePreferences()
        observeTransactionCount()
        observeNetWorth()
        observeMonthlyFinancials()
        observeActiveSubscriptions()
    }

    private fun observeBaseCurrency() {
        viewModelScope.launch {
            currencyRepository.effectiveBaseCurrencyCode.collectLatest { code ->
                _state.update { it.copy(baseCurrency = code) }
            }
        }
    }

    private fun observePreferences() {
        userPreferencesRepository
            .userPreferences
            .onEach { prefs ->
                _state.update {
                    it.copy(
                        userName = prefs.userName,
                        profileImageUri =
                            prefs.profileImageUri?.let { uri -> Uri.parse(uri) },
                        profileBackgroundColor = Color(prefs.profileBackgroundColor)
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeTransactionCount() {
        transactionRepository
            .getAllTransactions()
            .onEach { transactions ->
                _state.update { it.copy(totalTransactions = transactions.size) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeNetWorth() {
        combine(
            accountBalanceRepository.getAllLatestBalances(),
            currencyRepository.effectiveBaseCurrencyCode,
            currencyConversionService.rateChangeTrigger
        ) { allBalances, baseCurrency, _ ->
            if (allBalances.isEmpty()) return@combine BigDecimal.ZERO

            var total = BigDecimal.ZERO
            for (account in allBalances) {
                val amt = if (account.currency == baseCurrency) {
                    account.balance
                } else {
                    currencyConversionService.convertAmount(
                        amount = account.balance,
                        fromCurrency = account.currency,
                        toCurrency = baseCurrency
                    )
                }
                total = total.add(amt)
            }
            total
        }.onEach { total ->
            _state.update { it.copy(netWorth = total) }
        }.launchIn(viewModelScope)
    }

    private data class MonthlyTotals(val income: BigDecimal, val expense: BigDecimal)

    private fun observeMonthlyFinancials() {
        val now = LocalDate.now()
        val firstDay = now.withDayOfMonth(1)
        val lastDay = now.withDayOfMonth(now.lengthOfMonth())

        combine(
            transactionRepository.getAllTransactions(),
            currencyRepository.effectiveBaseCurrencyCode,
            currencyConversionService.rateChangeTrigger
        ) { transactions, baseCurrency, _ ->
            val monthTransactions =
                transactions.filter {
                    val date = it.dateTime.toLocalDate()
                    !date.isBefore(firstDay) && !date.isAfter(lastDay)
                }

            var income = BigDecimal.ZERO
            var expense = BigDecimal.ZERO
            for (txn in monthTransactions) {
                val amt = if (txn.currency == baseCurrency) {
                    txn.amount
                } else {
                    currencyConversionService.convertAmount(
                        amount = txn.amount,
                        fromCurrency = txn.currency,
                        toCurrency = baseCurrency
                    )
                }
                if (txn.transactionType == TransactionType.INCOME) {
                    income = income.add(amt)
                } else if (txn.transactionType == TransactionType.EXPENSE) {
                    expense = expense.add(amt)
                }
            }

            MonthlyTotals(income = income, expense = expense)
        }.onEach { totals ->
            _state.update { it.copy(totalIncome = totals.income, totalExpense = totals.expense) }
        }.launchIn(viewModelScope)
    }

    private fun observeActiveSubscriptions() {
        subscriptionRepository
            .getActiveSubscriptions()
            .onEach { subscriptions ->
                _state.update { it.copy(activeSubscriptions = subscriptions.size) }
            }
            .launchIn(viewModelScope)
    }

    fun toggleEditSheet() {
        _state.update {
            val newState = !it.isEditSheetOpen
            if (newState) { // Initialize edit state when opening
                it.copy(
                    isEditSheetOpen = true,
                    editState =
                        EditProfileState(
                            editedUserName = it.userName,
                            editedProfileImageUri = it.profileImageUri,
                            editedProfileBackgroundColor = it.profileBackgroundColor,
                            hasChanges = false
                        )
                )
            } else {
                it.copy(isEditSheetOpen = false)
            }
        }
    }

    fun dismissEditSheet() {
        _state.update { it.copy(isEditSheetOpen = false) }
    }

    fun updateEditUserName(name: String) {
        _state.update {
            it.copy(editState = it.editState.copy(editedUserName = name, hasChanges = true))
        }
    }

    fun updateEditProfileImage(uri: Uri?) {
        _state.update {
            it.copy(editState = it.editState.copy(editedProfileImageUri = uri, hasChanges = true))
        }
    }

    fun updateEditProfileBackgroundColor(color: Color) {
        _state.update {
            it.copy(
                editState =
                    it.editState.copy(
                        editedProfileBackgroundColor = color,
                        hasChanges = true
                    )
            )
        }
    }

    fun updateStoragePermission(isGranted: Boolean) {
        _state.update { it.copy(hasStoragePermission = isGranted) }
    }

    fun saveProfileChanges() {
        val currentState = _state.value
        val editState = currentState.editState

        viewModelScope.launch {
            // Save profile image to internal storage if it's a new gallery image
            val profileImagePersistentUri =
                editState.editedProfileImageUri?.let { uri ->
                    ImageUtils.saveImageToInternalStorage(context, uri, "profile")
                }

            userPreferencesRepository.updateUserName(editState.editedUserName)
            userPreferencesRepository.updateProfileImageUri(profileImagePersistentUri?.toString())
            userPreferencesRepository.updateProfileBackgroundColor(
                    editState.editedProfileBackgroundColor.toArgb()
            )

            _state.update { it.copy(isEditSheetOpen = false) }
        }
    }
}
