package com.reddy.vittify.presentation.ui.features.settings.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reddy.vittify.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TransactionSettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val useCategoryAsMerchant: StateFlow<Boolean> = userPreferencesRepository.useCategoryAsMerchant
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val useCategoryAsMerchantSms: StateFlow<Boolean> = userPreferencesRepository.useCategoryAsMerchantSms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val preserveAccountOrder: StateFlow<Boolean> = userPreferencesRepository.preserveAccountOrder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val directFieldEditing: StateFlow<Boolean> = userPreferencesRepository.directFieldEditing
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setUseCategoryAsMerchant(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setUseCategoryAsMerchant(enabled) }
    }

    fun setUseCategoryAsMerchantSms(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setUseCategoryAsMerchantSms(enabled) }
    }

    fun setPreserveAccountOrder(preserve: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setPreserveAccountOrder(preserve) }
    }

    fun setDirectFieldEditing(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setDirectFieldEditing(enabled) }
    }
}

