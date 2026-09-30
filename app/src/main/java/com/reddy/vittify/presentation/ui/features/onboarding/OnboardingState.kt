package com.reddy.vittify.presentation.ui.features.onboarding

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.work.WorkInfo
import com.reddy.vittify.R
import com.reddy.vittify.data.cloud.CloudFileInfo
import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.presentation.ui.features.accounts.AccountType
import com.reddy.vittify.presentation.ui.features.profile.EditProfileState

data class OnBoardingUiState(
    val currentStep: Int = 1,
    val totalSteps: Int = 6,

    // Step 1: Terms & Privacy
    val termsAccepted: Boolean = false,

    // Step 2: Unified Permissions Hub
    val smsGranted: Boolean = false,
    val notificationGranted: Boolean = false,
    val nearbyGranted: Boolean = false,
    val cameraGranted: Boolean = false,
    val smsDenied: Boolean = false,
    val notificationDenied: Boolean = false,
    val nearbyDenied: Boolean = false,
    val cameraDenied: Boolean = false,
    val hasPermission: Boolean = false, // legacy compat
    val hasSkippedPermission: Boolean = false,
    val showRationale: Boolean = false,
    val permissionDenialNotice: String? = null,

    // Step 3: Data Recovery (Cloud & Local)
    val isGoogleDriveSignedIn: Boolean = false,
    val googleDriveEmail: String? = null,
    val remoteSnapshots: List<CloudFileInfo> = emptyList(),
    val isLoadingSnapshots: Boolean = false,
    val isRestoring: Boolean = false,
    val restoreProgress: Int = 0,
    val restoreStatusMessage: String? = null,
    val restoreSuccessful: Boolean = false,
    val showPassphraseDialog: Boolean = false,
    val pendingRestoreFile: CloudFileInfo? = null,
    val passphraseInput: String = "",
    val isGeneratingSampleData: Boolean = false,
    val sampleDataProgress: Int = 0,
    val sampleDataStatusMessage: String? = null,
    val isSampleDataSeeded: Boolean = false,

    // Step 4: Primary Account Creation / Result Verification
    val accountType: AccountType = AccountType.SAVINGS,
    val manualAccountName: String = "",
    val manualAccountBalance: String = "",
    val manualAccountLast4: String = "",
    val manualAccountCreditLimit: String = "",
    val selectedCurrency: String = "INR",
    val selectedAccountColor: String = "#33B5E5",
    val selectedAccountIconRes: Int = R.drawable.type_finance_dollar_banknote,
    val selectedAccountIconName: String = "",
    val showCurrencyBottomSheet: Boolean = false,
    val showIconPicker: Boolean = false,
    val accountErrorMessage: String? = null,
    val accounts: List<AccountBalanceEntity> = emptyList(),
    val mainAccountKey: String? = null,
    val selectedAccountsForMerge: Set<String> = emptySet(),
    val isScanning: Boolean = false,
    val scanWorkInfo: WorkInfo? = null,

    // Step 5: Profile Customization
    val profileState: EditProfileState = EditProfileState(),

    // Step 6: Partner Sync (Couple)
    val clusterId: String? = null,
    val pairingCode: String? = null,
    val qrBitmap: Bitmap? = null,
    val isGeneratingQr: Boolean = false,
    val isPartnerPaired: Boolean = false,
    val partnerName: String? = null,
    val partnerDeviceId: String? = null,
    val pairingInput: String = "",
    val partnerSyncStatusMessage: String? = null,

    // Global UI Feedback
    val toastMessage: String? = null,
    val isLoading: Boolean = false,
    val onboardingFinished: Boolean = false,
    val permissionSubStep: Int = 0 // legacy compat
)
