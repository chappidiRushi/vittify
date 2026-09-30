package com.reddy.vittify.presentation.ui.features.settings.currency

data class CurrencySettingsUiState(
    val unifiedCurrencyEnabled: Boolean = false,
    val unifiedCurrencyCode: String? = null,
    val defaultCurrencyEnabled: Boolean = false,
    val defaultCurrencyCode: String? = null
)
