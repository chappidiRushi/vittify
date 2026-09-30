package com.reddy.vittify.presentation.ui.features.onboarding

import android.Manifest
import android.app.Activity
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import com.reddy.vittify.presentation.ui.features.accounts.AccountType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.reddy.vittify.R
import com.reddy.vittify.presentation.ui.features.onboarding.steps.AccountCreationStep
import com.reddy.vittify.presentation.ui.features.onboarding.steps.DataRecoveryStep
import com.reddy.vittify.presentation.ui.features.onboarding.steps.OnboardingProgressIndicator
import com.reddy.vittify.presentation.ui.features.onboarding.steps.PartnerSyncStep
import com.reddy.vittify.presentation.ui.features.onboarding.steps.PermissionsHubStep
import com.reddy.vittify.presentation.ui.features.onboarding.steps.ProfileSetupStep
import com.reddy.vittify.presentation.ui.features.onboarding.steps.TermsAndPrivacyStep
import com.reddy.vittify.presentation.ui.theme.LocalBaseBackgroundColor

private fun getP2pPermissions(): Array<String> {
    val list = mutableListOf<String>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        list.add(Manifest.permission.BLUETOOTH_SCAN)
        list.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        list.add(Manifest.permission.BLUETOOTH_CONNECT)
        list.add(Manifest.permission.NEARBY_WIFI_DEVICES)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        list.add(Manifest.permission.BLUETOOTH_SCAN)
        list.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        list.add(Manifest.permission.BLUETOOTH_CONNECT)
        list.add(Manifest.permission.ACCESS_FINE_LOCATION)
    } else {
        list.add(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    return list.toTypedArray()
}

private fun getAllPermissions(): Array<String> {
    val list = mutableListOf<String>()
    list.add(Manifest.permission.READ_SMS)
    list.add(Manifest.permission.RECEIVE_SMS)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        list.add(Manifest.permission.POST_NOTIFICATIONS)
    }
    list.addAll(getP2pPermissions())
    list.add(Manifest.permission.CAMERA)
    return list.distinct().toTypedArray()
}

@Composable
fun OnBoardingScreen(
    modifier: Modifier = Modifier,
    onOnBoardingComplete: () -> Unit,
    onRestoreBackup: () -> Unit = {},
    onNavigateToTerms: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
    onBoardingViewModel: OnBoardingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val uiState by onBoardingViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.onboardingFinished) {
        if (uiState.onboardingFinished) {
            onOnBoardingComplete()
        }
    }

    // Google Sign-In Activity Result Launcher
    val gDriveSignInClient = remember(context) {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.appdata"))
            .build()
        GoogleSignIn.getClient(context, options)
    }

    val gDriveSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                onBoardingViewModel.handleGoogleSignInResult(account)
            } catch (e: ApiException) {
                Log.e("OnBoardingScreen", "Google Sign-In failed code=${e.statusCode}", e)
                onBoardingViewModel.showToast(context.getString(R.string.signin_failed_code_format, e.statusCode))
            }
        }
    }

    // Individual Permission Launchers
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.READ_SMS] == true
        onBoardingViewModel.onSmsPermissionResult(granted)
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onBoardingViewModel.onNotificationPermissionResult(granted)
    }

    val nearbyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        onBoardingViewModel.onNearbyPermissionResult(allGranted)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onBoardingViewModel.onCameraPermissionResult(granted)
    }

    val allPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        onBoardingViewModel.checkAllPermissions()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(LocalBaseBackgroundColor.current),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                OnboardingProgressIndicator(
                    currentStep = uiState.currentStep,
                    totalSteps = uiState.totalSteps,
                    onBack = { onBoardingViewModel.previousStep() },
                    onSkip = {
                        if (uiState.currentStep == 3) {
                            onBoardingViewModel.nextStep() // Skip restore to manual account
                        } else if (uiState.currentStep == 6) {
                            onBoardingViewModel.finishOnboarding() // Skip partner sync to app
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = uiState.currentStep,
            transitionSpec = {
                val animationSpec = tween<Float>(durationMillis = 350, easing = FastOutSlowInEasing)
                val slideSpec = tween<androidx.compose.ui.unit.IntOffset>(durationMillis = 350, easing = FastOutSlowInEasing)

                if (targetState > initialState) {
                    slideInHorizontally(animationSpec = slideSpec) { it } + fadeIn(animationSpec) togetherWith
                            slideOutHorizontally(animationSpec = slideSpec) { -it } + fadeOut(animationSpec)
                } else {
                    slideInHorizontally(animationSpec = slideSpec) { -it } + fadeIn(animationSpec) togetherWith
                            slideOutHorizontally(animationSpec = slideSpec) { it } + fadeOut(animationSpec)
                }.using(SizeTransform(clip = false))
            },
            label = "OnboardingStepTransition",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .navigationBarsPadding()
        ) { step ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (step) {
                    1 -> TermsAndPrivacyStep(
                        isAccepted = uiState.termsAccepted,
                        onAcceptToggle = { onBoardingViewModel.toggleTermsAccepted(it) },
                        onContinue = { onBoardingViewModel.nextStep() },
                        onViewTerms = onNavigateToTerms,
                        onViewPrivacy = onNavigateToPrivacy
                    )

                    2 -> PermissionsHubStep(
                        smsGranted = uiState.smsGranted,
                        notificationGranted = uiState.notificationGranted,
                        nearbyGranted = uiState.nearbyGranted,
                        cameraGranted = uiState.cameraGranted,
                        smsDenied = uiState.smsDenied,
                        notificationDenied = uiState.notificationDenied,
                        nearbyDenied = uiState.nearbyDenied,
                        cameraDenied = uiState.cameraDenied,
                        onRequestSms = {
                            smsPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_SMS,
                                    Manifest.permission.RECEIVE_SMS
                                )
                            )
                        },
                        onRequestNotification = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onBoardingViewModel.onNotificationPermissionResult(true)
                            }
                        },
                        onRequestNearby = {
                            nearbyPermissionLauncher.launch(getP2pPermissions())
                        },
                        onRequestCamera = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        onRequestAllPermissions = {
                            allPermissionsLauncher.launch(getAllPermissions())
                        },
                        onContinue = { onBoardingViewModel.nextStep() }
                    )

                    3 -> DataRecoveryStep(
                        isGoogleDriveSignedIn = uiState.isGoogleDriveSignedIn,
                        googleDriveEmail = uiState.googleDriveEmail,
                        remoteSnapshots = uiState.remoteSnapshots,
                        isLoadingSnapshots = uiState.isLoadingSnapshots,
                        isRestoring = uiState.isRestoring,
                        restoreProgress = uiState.restoreProgress,
                        restoreStatusMessage = uiState.restoreStatusMessage,
                        isGeneratingSampleData = uiState.isGeneratingSampleData,
                        sampleDataProgress = uiState.sampleDataProgress,
                        sampleDataStatusMessage = uiState.sampleDataStatusMessage,
                        isSampleDataSeeded = uiState.isSampleDataSeeded,
                        showPassphraseDialog = uiState.showPassphraseDialog,
                        passphraseInput = uiState.passphraseInput,
                        onPassphraseChange = { onBoardingViewModel.onPassphraseChange(it) },
                        onConfirmPassphrase = { onBoardingViewModel.confirmPassphrase() },
                        onDismissPassphraseDialog = { onBoardingViewModel.dismissPassphraseDialog() },
                        onSignInGoogleDrive = {
                            gDriveSignInLauncher.launch(gDriveSignInClient.signInIntent)
                        },
                        onSignOutGoogleDrive = { onBoardingViewModel.onGoogleDriveSignOut() },
                        onRefreshSnapshots = { onBoardingViewModel.loadRemoteSnapshots() },
                        onRestoreSnapshot = { snapshot -> onBoardingViewModel.restoreSnapshot(snapshot) },
                        onRestoreLocalBackup = { uri -> onBoardingViewModel.restoreLocalBackup(uri) },
                        onGenerateRandomData = { config -> onBoardingViewModel.generateRandomData(config) },
                        onClearRandomData = { onBoardingViewModel.clearRandomData() },
                        onSkip = { onBoardingViewModel.nextStep() }
                    )

                    4 -> AccountCreationStep(
                        accountType = uiState.accountType,
                        accountName = uiState.manualAccountName,
                        balance = uiState.manualAccountBalance,
                        currency = uiState.selectedCurrency,
                        last4 = uiState.manualAccountLast4,
                        creditLimit = uiState.manualAccountCreditLimit,
                        selectedColorHex = uiState.selectedAccountColor,
                        selectedIconResId = uiState.selectedAccountIconRes,
                        errorMessage = uiState.accountErrorMessage,
                        onTypeChange = { onBoardingViewModel.updateAccountType(it) },
                        onNameChange = { onBoardingViewModel.updateManualAccountName(it) },
                        onBalanceChange = { onBoardingViewModel.updateManualAccountBalance(it) },
                        onCurrencyChange = { onBoardingViewModel.updateSelectedCurrency(it) },
                        onLast4Change = { onBoardingViewModel.updateManualAccountLast4(it) },
                        onCreditLimitChange = { onBoardingViewModel.updateManualAccountCreditLimit(it) },
                        onColorChange = { onBoardingViewModel.updateSelectedAccountColor(it) },
                        onSaveAndContinue = { onBoardingViewModel.saveManualAccount() }
                    )

                    5 -> ProfileSetupStep(
                        profileState = uiState.profileState,
                        onNameChange = { onBoardingViewModel.onProfileNameChange(it) },
                        onProfileImageChange = { onBoardingViewModel.onProfileImageChange(it) },
                        onBackgroundColorChange = { onBoardingViewModel.onBackgroundColorChange(it) },
                        onSaveProfile = { onBoardingViewModel.saveProfile() }
                    )

                    6 -> PartnerSyncStep(
                        clusterId = uiState.clusterId,
                        pairingCode = uiState.pairingCode,
                        qrBitmap = uiState.qrBitmap,
                        isGeneratingQr = uiState.isGeneratingQr,
                        isPartnerPaired = uiState.isPartnerPaired,
                        partnerName = uiState.partnerName,
                        pairingInput = uiState.pairingInput,
                        onPairingInputChange = { onBoardingViewModel.onPairingInputChange(it) },
                        onApplyPairingCode = { onBoardingViewModel.applyPairingInput(it) },
                        onScanPartnerQrCamera = { onBoardingViewModel.scanPartnerQrCamera() },
                        onCopyCode = { code ->
                            clipboardManager.setText(AnnotatedString(code))
                            onBoardingViewModel.showToast(context.getString(R.string.code_copied_toast))
                        },
                        onCompleteSetup = { onBoardingViewModel.finishOnboarding() },
                        onPairLater = { onBoardingViewModel.finishOnboarding() }
                    )
                }
            }
        }
    }
}

// ─── STEP PREVIEWS ────────────────────────────────────────────────────────────

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Step 1: Terms & Privacy")
@Composable
fun Step1TermsPreview() {
    com.reddy.vittify.presentation.ui.theme.VittifyTheme {
        TermsAndPrivacyStep(
            isAccepted = true,
            onAcceptToggle = {},
            onContinue = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Step 2: Permissions Hub")
@Composable
fun Step2PermissionsPreview() {
    com.reddy.vittify.presentation.ui.theme.VittifyTheme {
        PermissionsHubStep(
            smsGranted = true,
            notificationGranted = false,
            nearbyGranted = true,
            cameraGranted = false,
            onRequestSms = {},
            onRequestNotification = {},
            onRequestNearby = {},
            onRequestCamera = {},
            onContinue = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Step 3: Data Recovery")
@Composable
fun Step3DataRecoveryPreview() {
    com.reddy.vittify.presentation.ui.theme.VittifyTheme {
        DataRecoveryStep(
            isGoogleDriveSignedIn = true,
            googleDriveEmail = "rushi@example.com",
            remoteSnapshots = listOf(
                com.reddy.vittify.data.cloud.CloudFileInfo(
                    id = "snapshot-1",
                    name = "vittify-backup-2026.zip",
                    path = "/vittify-backup-2026.zip",
                    size = 524288,
                    lastModified = System.currentTimeMillis(),
                    providerType = com.reddy.vittify.data.cloud.CloudProviderType.GOOGLE_DRIVE
                )
            ),
            isLoadingSnapshots = false,
            isRestoring = false,
            restoreProgress = 0,
            restoreStatusMessage = null,
            showPassphraseDialog = false,
            passphraseInput = "",
            onPassphraseChange = {},
            onConfirmPassphrase = {},
            onDismissPassphraseDialog = {},
            onSignInGoogleDrive = {},
            onSignOutGoogleDrive = {},
            onRefreshSnapshots = {},
            onRestoreSnapshot = {},
            onRestoreLocalBackup = {},
            onSkip = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Step 4: Primary Account Creation")
@Composable
fun Step4AccountCreationPreview() {
    com.reddy.vittify.presentation.ui.theme.VittifyTheme {
        AccountCreationStep(
            accountType = AccountType.SAVINGS,
            accountName = "HDFC Salary Account",
            balance = "45000.00",
            currency = "INR",
            last4 = "4821",
            creditLimit = "",
            selectedColorHex = "#33B5E5",
            selectedIconResId = R.drawable.type_finance_dollar_banknote,
            errorMessage = null,
            onTypeChange = {},
            onNameChange = {},
            onBalanceChange = {},
            onCurrencyChange = {},
            onLast4Change = {},
            onCreditLimitChange = {},
            onColorChange = {},
            onSaveAndContinue = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Step 5: Profile Customization")
@Composable
fun Step5ProfilePreview() {
    com.reddy.vittify.presentation.ui.theme.VittifyTheme {
        ProfileSetupStep(
            profileState = com.reddy.vittify.presentation.ui.features.profile.EditProfileState(
                editedUserName = "Rushi"
            ),
            onNameChange = {},
            onProfileImageChange = {},
            onBackgroundColorChange = {},
            onSaveProfile = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Step 6: Partner Sync")
@Composable
fun Step6PartnerSyncPreview() {
    com.reddy.vittify.presentation.ui.theme.VittifyTheme {
        PartnerSyncStep(
            clusterId = "vittify-7a8f3b",
            pairingCode = "vittify-7a8f3b",
            qrBitmap = null,
            isGeneratingQr = false,
            isPartnerPaired = false,
            partnerName = null,
            pairingInput = "",
            onPairingInputChange = {},
            onApplyPairingCode = {},
            onScanPartnerQrCamera = {},
            onCopyCode = {},
            onCompleteSetup = {},
            onPairLater = {}
        )
    }
}

