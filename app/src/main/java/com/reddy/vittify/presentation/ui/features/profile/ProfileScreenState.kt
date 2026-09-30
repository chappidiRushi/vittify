package com.reddy.vittify.presentation.ui.features.profile

import android.net.Uri
import androidx.compose.ui.graphics.Color
import java.math.BigDecimal

data class ProfileScreenState(
    val userName: String = "User",
    val profileImageUri: Uri? = null,
    val profileBackgroundColor: Color = Color.Transparent,
    val totalTransactions: Int = 0,
    val netWorth: BigDecimal = BigDecimal.ZERO,
    val totalIncome: BigDecimal = BigDecimal.ZERO,
    val totalExpense: BigDecimal = BigDecimal.ZERO,
    val activeSubscriptions: Int = 0,
    val baseCurrency: String = "INR",
    val isLoading: Boolean = false,
    val isEditSheetOpen: Boolean = false,
    val hasStoragePermission: Boolean = false,
    val editState: EditProfileState = EditProfileState()
)

data class EditProfileState(
    val editedUserName: String = "",
    val editedProfileImageUri: Uri? = null,
    val editedProfileBackgroundColor: Color = Color.Transparent,
    val hasChanges: Boolean = false
)
