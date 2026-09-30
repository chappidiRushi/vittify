package com.reddy.vittify.presentation.ui.features.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.reddy.vittify.R
import com.reddy.vittify.data.backup.BackupImporter
import com.reddy.vittify.data.backup.ImportStrategy
import com.reddy.vittify.data.cloud.CloudFileInfo
import com.reddy.vittify.data.cloud.CloudProviderConfig
import com.reddy.vittify.data.cloud.CloudProviderType
import com.reddy.vittify.data.cloud.engine.CloudBackupManager
import com.reddy.vittify.data.cloud.providers.GoogleDriveStorageProvider
import com.reddy.vittify.data.cloud.security.CloudCredentialStore
import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.data.preferences.UserPreferencesRepository
import com.reddy.vittify.data.repository.AccountBalanceRepository
import com.reddy.vittify.data.repository.TransactionRepository
import com.reddy.vittify.data.sync.P2pSyncPreferencesRepository
import com.reddy.vittify.data.sync.engine.P2pSyncEngine
import com.reddy.vittify.data.sync.qr.QrCodeGenerator
import com.reddy.vittify.data.sync.qr.QrCodeScanner
import com.reddy.vittify.data.sync.security.P2pCryptoEngine
import com.reddy.vittify.presentation.ui.features.accounts.AccountType
import com.reddy.vittify.presentation.ui.util.ToastManager
import com.reddy.vittify.utils.ImageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class OnBoardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val transactionRepository: TransactionRepository,
    private val cloudBackupManager: CloudBackupManager,
    private val googleDriveProvider: GoogleDriveStorageProvider,
    private val cloudCredentialStore: CloudCredentialStore,
    private val backupImporter: BackupImporter,
    private val p2pPreferences: P2pSyncPreferencesRepository,
    private val syncEngine: P2pSyncEngine,
    private val cryptoEngine: P2pCryptoEngine,
    private val randomDataGeneratorService: com.reddy.vittify.data.generator.RandomDataGeneratorService
) : ViewModel() {

    companion object {
        private const val TAG = "OnBoardingViewModel"
        const val URI_SCHEME = "vittify"
        const val URI_HOST = "sync"
    }

    private val sharedPrefs = context.getSharedPreferences("account_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(OnBoardingUiState())
    val uiState: StateFlow<OnBoardingUiState> = _uiState.asStateFlow()

    init {
        checkAllPermissions()
        observeUserPreferences()
        observeAccounts()
        loadMainAccount()
        initCloudState()
        initPartnerState()
    }

    // ─── PERMISSIONS ──────────────────────────────────────────────────────────

    fun checkAllPermissions() {
        val smsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        val nearbyGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }

        val cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

        _uiState.update {
            it.copy(
                smsGranted = smsGranted,
                notificationGranted = notifGranted,
                nearbyGranted = nearbyGranted,
                cameraGranted = cameraGranted,
                hasPermission = smsGranted
            )
        }
    }

    fun onSmsPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(smsGranted = granted, hasPermission = granted) }
        if (granted) {
            showToast(context.getString(R.string.perm_status_granted))
            viewModelScope.launch { userPreferencesRepository.updateSkippedSmsPermission(false) }
        }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(notificationGranted = granted) }
        if (granted) showToast(context.getString(R.string.perm_status_granted))
    }

    fun onNearbyPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(nearbyGranted = granted) }
        if (granted) showToast(context.getString(R.string.perm_status_granted))
    }

    fun onCameraPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(cameraGranted = granted) }
        if (granted) showToast(context.getString(R.string.perm_status_granted))
    }

    // ─── STEP NAVIGATION ──────────────────────────────────────────────────────

    fun setStep(step: Int) {
        _uiState.update { it.copy(currentStep = step.coerceIn(1, 6)) }
    }

    fun nextStep() {
        _uiState.update { state ->
            val next = (state.currentStep + 1).coerceAtMost(6)
            state.copy(currentStep = next)
        }
    }

    fun previousStep() {
        _uiState.update { state ->
            val prev = (state.currentStep - 1).coerceAtLeast(1)
            state.copy(currentStep = prev)
        }
    }

    // ─── STEP 1: TERMS & PRIVACY ──────────────────────────────────────────────

    fun toggleTermsAccepted(accepted: Boolean) {
        _uiState.update { it.copy(termsAccepted = accepted) }
    }

    // ─── STEP 3: CLOUD & LOCAL DATA RECOVERY ──────────────────────────────────

    private fun initCloudState() {
        val gConfig = cloudCredentialStore.getGoogleDriveConfig()
        val isSignedIn = gConfig.isConfigured && gConfig.accountEmail.isNotBlank()
        _uiState.update {
            it.copy(
                isGoogleDriveSignedIn = isSignedIn,
                googleDriveEmail = if (isSignedIn) gConfig.accountEmail else null
            )
        }
        if (isSignedIn) {
            loadRemoteSnapshots()
        }
    }

    fun handleGoogleSignInResult(account: GoogleSignInAccount?) {
        if (account == null) {
            showToast(context.getString(R.string.google_signin_failed_null))
            return
        }
        val email = account.email ?: ""
        if (email.isBlank()) {
            showToast(context.getString(R.string.google_signin_failed_no_email))
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSnapshots = true) }
            try {
                val token = withContext(Dispatchers.IO) {
                    GoogleAuthUtil.getToken(
                        context,
                        email,
                        "oauth2:https://www.googleapis.com/auth/drive.appdata"
                    )
                }

                if (token.isNullOrBlank()) {
                    throw Exception(context.getString(R.string.err_token_null))
                }

                val config = CloudProviderConfig.GoogleDriveConfig(
                    accountEmail = email,
                    accessToken = token,
                    isEnabled = true
                )
                cloudCredentialStore.saveGoogleDriveConfig(config)
                cloudCredentialStore.setActiveProviderType(CloudProviderType.GOOGLE_DRIVE)
                _uiState.update {
                    it.copy(
                        isGoogleDriveSignedIn = true,
                        googleDriveEmail = email,
                        isLoadingSnapshots = false
                    )
                }
                showToast(context.getString(R.string.signed_in_as_format, email))
                loadRemoteSnapshots()
            } catch (e: Exception) {
                Log.e(TAG, "Google sign-in token error", e)
                _uiState.update { it.copy(isLoadingSnapshots = false) }
                showToast(context.getString(R.string.auth_failed_format, e.message ?: "Authentication failed"))
            }
        }
    }

    fun onGoogleDriveSignOut() {
        val config = CloudProviderConfig.GoogleDriveConfig(
            accountEmail = "",
            accessToken = "",
            isEnabled = false
        )
        cloudCredentialStore.saveGoogleDriveConfig(config)
        cloudCredentialStore.setActiveProviderType(CloudProviderType.LOCAL_ONLY)
        _uiState.update {
            it.copy(
                isGoogleDriveSignedIn = false,
                googleDriveEmail = null,
                remoteSnapshots = emptyList()
            )
        }
        showToast("Signed out of Google Drive")
    }

    fun loadRemoteSnapshots() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSnapshots = true) }
            val result = cloudBackupManager.listRemoteBackups()
            if (result.isSuccess) {
                val snapshots = result.getOrNull() ?: emptyList()
                _uiState.update { it.copy(remoteSnapshots = snapshots, isLoadingSnapshots = false) }
            } else {
                _uiState.update { it.copy(remoteSnapshots = emptyList(), isLoadingSnapshots = false) }
            }
        }
    }

    fun restoreSnapshot(snapshot: CloudFileInfo) {
        val isEncrypted = snapshot.name.endsWith(".enc")
        val storedPassphrase = cloudCredentialStore.getE2ePassphrase()
        if (isEncrypted && storedPassphrase.isBlank()) {
            _uiState.update {
                it.copy(
                    showPassphraseDialog = true,
                    pendingRestoreFile = snapshot,
                    passphraseInput = ""
                )
            }
            return
        }
        executeCloudRestore(snapshot)
    }

    fun onPassphraseChange(passphrase: String) {
        _uiState.update { it.copy(passphraseInput = passphrase) }
    }

    fun confirmPassphrase() {
        val snapshot = _uiState.value.pendingRestoreFile ?: return
        val passphrase = _uiState.value.passphraseInput
        cloudCredentialStore.setE2ePassphrase(passphrase)
        cloudCredentialStore.setE2eEncryptionEnabled(true)
        _uiState.update { it.copy(showPassphraseDialog = false, pendingRestoreFile = null) }
        executeCloudRestore(snapshot)
    }

    fun dismissPassphraseDialog() {
        _uiState.update { it.copy(showPassphraseDialog = false, pendingRestoreFile = null) }
    }

    private fun executeCloudRestore(snapshot: CloudFileInfo) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRestoring = true,
                    restoreProgress = 0,
                    restoreStatusMessage = context.getString(R.string.downloading_snapshot)
                )
            }
            val result = cloudBackupManager.restoreBackup(snapshot) { progress ->
                _uiState.update {
                    it.copy(
                        restoreProgress = progress,
                        restoreStatusMessage = context.getString(R.string.restoring_data_format, progress)
                    )
                }
            }
            if (result.isSuccess) {
                _uiState.update { it.copy(isRestoring = false, restoreSuccessful = true) }
                showToast(context.getString(R.string.snapshot_restored_success))
                // Refresh accounts and advance to Profile
                val accounts = accountBalanceRepository.getAllLatestBalances().first()
                if (accounts.isNotEmpty()) {
                    setAsMainAccount(accounts.first().bankName, accounts.first().accountLast4)
                }
                setStep(5)
            } else {
                val error = result.exceptionOrNull()?.message ?: context.getString(R.string.failed_restore_snapshot)
                _uiState.update { it.copy(isRestoring = false) }
                showToast(error)
            }
        }
    }

    fun restoreLocalBackup(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRestoring = true,
                    restoreProgress = 50,
                    restoreStatusMessage = "Importing local backup..."
                )
            }
            try {
                when (val result = backupImporter.importBackup(uri, ImportStrategy.REPLACE_ALL)) {
                    is com.reddy.vittify.data.backup.ImportResult.Success -> {
                        _uiState.update { it.copy(isRestoring = false, restoreSuccessful = true) }
                        showToast(context.getString(R.string.restored_data_success))
                        val accounts = accountBalanceRepository.getAllLatestBalances().first()
                        if (accounts.isNotEmpty()) {
                            setAsMainAccount(accounts.first().bankName, accounts.first().accountLast4)
                        }
                        setStep(5)
                    }
                    is com.reddy.vittify.data.backup.ImportResult.Error -> {
                        _uiState.update { it.copy(isRestoring = false) }
                        showToast("Restore failed: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error importing local backup", e)
                _uiState.update { it.copy(isRestoring = false) }
                showToast("Error importing backup: ${e.message}")
            }
        }
    }

    fun generateRandomData(config: com.reddy.vittify.data.generator.RandomDataConfig) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGeneratingSampleData = true,
                    sampleDataProgress = 5,
                    sampleDataStatusMessage = "Preparing random data generator..."
                )
            }
            try {
                val result = randomDataGeneratorService.generateRandomData(config) { fraction, status ->
                    _uiState.update {
                        it.copy(
                            sampleDataProgress = (fraction * 100).toInt().coerceIn(5, 100),
                            sampleDataStatusMessage = status
                        )
                    }
                }
                setAsMainAccount(result.primaryBankName, result.primaryAccountLast4)
                _uiState.update {
                    it.copy(
                        isGeneratingSampleData = false,
                        sampleDataProgress = 100,
                        isSampleDataSeeded = true,
                        selectedCurrency = result.baseCurrency
                    )
                }
                showToast("Generated ${result.accountsCreated} accounts & ${result.transactionsCreated} verified transactions!")
                setStep(5) // Skip manual account creation since accounts & balances are populated
            } catch (e: Exception) {
                Log.e(TAG, "Error generating random sample data", e)
                _uiState.update {
                    it.copy(
                        isGeneratingSampleData = false,
                        sampleDataStatusMessage = null
                    )
                }
                showToast("Failed to generate random data: ${e.message}")
            }
        }
    }

    fun clearRandomData() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGeneratingSampleData = true,
                    sampleDataProgress = 50,
                    sampleDataStatusMessage = "Removing sample data..."
                )
            }
            try {
                randomDataGeneratorService.clearSampleData()
                _uiState.update {
                    it.copy(
                        isGeneratingSampleData = false,
                        isSampleDataSeeded = false,
                        sampleDataStatusMessage = null
                    )
                }
                showToast("Sample data cleared")
            } catch (e: Exception) {
                _uiState.update { it.copy(isGeneratingSampleData = false) }
                showToast("Failed to clear sample data: ${e.message}")
            }
        }
    }

    // ─── STEP 4: PRIMARY ACCOUNT CREATION ─────────────────────────────────────

    fun updateAccountType(type: AccountType) {
        _uiState.update { it.copy(accountType = type) }
    }

    fun updateManualAccountName(name: String) {
        _uiState.update { it.copy(manualAccountName = name, accountErrorMessage = null) }
    }

    fun updateManualAccountBalance(balance: String) {
        if (balance.isEmpty() || balance.matches(Regex("^\\d*\\.?\\d*$"))) {
            _uiState.update { it.copy(manualAccountBalance = balance, accountErrorMessage = null) }
        }
    }

    fun updateManualAccountLast4(last4: String) {
        if (last4.length <= 4 && last4.all { it.isDigit() }) {
            _uiState.update { it.copy(manualAccountLast4 = last4, accountErrorMessage = null) }
        }
    }

    fun updateManualAccountCreditLimit(limit: String) {
        if (limit.isEmpty() || limit.matches(Regex("^\\d*\\.?\\d*$"))) {
            _uiState.update { it.copy(manualAccountCreditLimit = limit) }
        }
    }

    fun updateSelectedCurrency(currency: String) {
        _uiState.update { it.copy(selectedCurrency = currency) }
    }

    fun updateSelectedAccountColor(hex: String) {
        _uiState.update { it.copy(selectedAccountColor = hex) }
    }

    fun saveManualAccount() {
        val state = _uiState.value
        if (state.manualAccountName.isBlank() || state.manualAccountBalance.isBlank()) {
            _uiState.update { it.copy(accountErrorMessage = "Account name and balance are required") }
            return
        }
        if (state.accountType != AccountType.WALLET && state.manualAccountLast4.length != 4) {
            _uiState.update { it.copy(accountErrorMessage = "Please enter the last 4 digits") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, accountErrorMessage = null) }

            val last4 = if (state.accountType == AccountType.WALLET) "wallet" else state.manualAccountLast4
            val balance = try { BigDecimal(state.manualAccountBalance) } catch (_: Exception) { BigDecimal.ZERO }
            val creditLimit = if (state.accountType == AccountType.CREDIT && state.manualAccountCreditLimit.isNotBlank()) {
                try { BigDecimal(state.manualAccountCreditLimit) } catch (_: Exception) { null }
            } else null

            val newAccount = AccountBalanceEntity(
                bankName = state.manualAccountName.trim(),
                accountLast4 = last4,
                balance = balance,
                currency = state.selectedCurrency,
                isCreditCard = state.accountType == AccountType.CREDIT,
                isWallet = state.accountType == AccountType.WALLET,
                creditLimit = creditLimit,
                color = state.selectedAccountColor,
                iconResId = state.selectedAccountIconRes,
                timestamp = LocalDateTime.now()
            )

            accountBalanceRepository.insertBalance(newAccount)
            setAsMainAccount(newAccount.bankName, newAccount.accountLast4)
            userPreferencesRepository.updateBaseCurrency(state.selectedCurrency)

            // Update in-built cash wallet if different
            val cashWallet = accountBalanceRepository.getLatestBalance("Cash", "wallet")
            if (cashWallet != null && cashWallet.currency != state.selectedCurrency) {
                accountBalanceRepository.insertBalance(
                    cashWallet.copy(
                        currency = state.selectedCurrency,
                        timestamp = LocalDateTime.now()
                    )
                )
            }

            showToast("Account saved!")
            _uiState.update { it.copy(isLoading = false) }
            nextStep()
        }
    }

    // ─── STEP 5: PROFILE CUSTOMIZATION ────────────────────────────────────────

    private fun observeUserPreferences() {
        userPreferencesRepository
            .userPreferences
            .onEach { prefs ->
                _uiState.update { current ->
                    current.copy(
                        hasSkippedPermission = prefs.hasSkippedSmsPermission,
                        isSampleDataSeeded = prefs.isSampleDataSeeded,
                        profileState = if (current.profileState.hasChanges) {
                            current.profileState
                        } else {
                            current.profileState.copy(
                                editedUserName = prefs.userName,
                                editedProfileImageUri = prefs.profileImageUri?.let { uri -> Uri.parse(uri) },
                                editedProfileBackgroundColor = if (prefs.profileBackgroundColor != 0)
                                    Color(prefs.profileBackgroundColor)
                                else Color.Transparent
                            )
                        }
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onProfileNameChange(name: String) {
        _uiState.update {
            it.copy(profileState = it.profileState.copy(editedUserName = name, hasChanges = true))
        }
    }

    fun onProfileImageChange(uri: Uri?) {
        _uiState.update {
            it.copy(profileState = it.profileState.copy(editedProfileImageUri = uri, hasChanges = true))
        }
    }

    fun onBackgroundColorChange(color: Color) {
        _uiState.update {
            it.copy(profileState = it.profileState.copy(editedProfileBackgroundColor = color, hasChanges = true))
        }
    }

    fun saveProfile() {
        val profile = _uiState.value.profileState
        viewModelScope.launch {
            val persistentUri = profile.editedProfileImageUri?.let { uri ->
                if (uri.scheme == "content" || uri.scheme == "file") {
                    ImageUtils.saveImageToInternalStorage(context, uri, "profile")
                } else {
                    uri
                }
            }
            userPreferencesRepository.updateUserName(profile.editedUserName.trim())
            userPreferencesRepository.updateProfileImageUri(persistentUri?.toString())
            userPreferencesRepository.updateProfileBackgroundColor(profile.editedProfileBackgroundColor.toArgb())
            ToastManager.showSuccess("Profile saved!")
            nextStep()
        }
    }

    // ─── STEP 6: PARTNER SYNC ─────────────────────────────────────────────────

    private fun initPartnerState() {
        var clusterId = p2pPreferences.getClusterId()
        var keyBase64 = p2pPreferences.getSecretKeyBase64()

        if (clusterId.isNullOrBlank() || keyBase64.isNullOrBlank()) {
            clusterId = "vittify-" + UUID.randomUUID().toString().take(8)
            val secretKey = cryptoEngine.deriveKey(clusterId)
            keyBase64 = cryptoEngine.keyToBase64(secretKey)
            p2pPreferences.savePairingConfig(clusterId, keyBase64)
        }

        val partnerId = p2pPreferences.getPartnerUserId()
        val partnerName = p2pPreferences.getPartnerName()
        val isPaired = !partnerId.isNullOrBlank()

        _uiState.update {
            it.copy(
                clusterId = clusterId,
                pairingCode = clusterId,
                isPartnerPaired = isPaired,
                partnerName = partnerName,
                partnerDeviceId = partnerId
            )
        }

        generatePartnerQr(clusterId, keyBase64)
    }

    private fun generatePartnerQr(clusterId: String, keyBase64: String) {
        val myDeviceId = p2pPreferences.getDeviceId()
        val myDeviceName = p2pPreferences.getDeviceName()
        viewModelScope.launch(Dispatchers.Default) {
            val pairingUri = "$URI_SCHEME://$URI_HOST?cluster=$clusterId&deviceId=${Uri.encode(myDeviceId)}&name=${Uri.encode(myDeviceName)}"
            val bitmap = QrCodeGenerator.generateQrBitmap(pairingUri, 600)
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(qrBitmap = bitmap) }
            }
        }
    }

    fun onPairingInputChange(input: String) {
        _uiState.update { it.copy(pairingInput = input) }
    }

    fun scanPartnerQrCamera() {
        val scanner = QrCodeScanner(context)
        scanner.scan(
            onSuccess = { result ->
                p2pPreferences.setIsScannerDevice(true)
                applyPairingInput(result)
            },
            onFailure = { e ->
                Log.e(TAG, "QR scan failed", e)
                showToast("Camera scan failed: ${e.message}")
            }
        )
    }

    fun applyPairingInput(rawInput: String) {
        val input = rawInput.trim()
        val ownCluster = _uiState.value.clusterId ?: p2pPreferences.getClusterId()
        val ownDeviceId = p2pPreferences.getDeviceId()

        p2pPreferences.setIsScannerDevice(true)
        try {
            val uri = Uri.parse(input)
            val cluster = if (input.startsWith("$URI_SCHEME://$URI_HOST")) {
                uri.getQueryParameter("cluster")
            } else if (input.startsWith("vittify-")) {
                input
            } else {
                uri.getQueryParameter("cluster") ?: input
            }
            val partnerDeviceId = if (input.startsWith("$URI_SCHEME://$URI_HOST")) {
                uri.getQueryParameter("deviceId")
            } else null
            val partnerDeviceName = if (input.startsWith("$URI_SCHEME://$URI_HOST")) {
                uri.getQueryParameter("name")
            } else null

            if ((!cluster.isNullOrBlank() && cluster == ownCluster) ||
                (!partnerDeviceId.isNullOrBlank() && partnerDeviceId == ownDeviceId) ||
                (input == ownCluster)
            ) {
                showToast("Cannot pair with yourself")
                return
            }

            if (!cluster.isNullOrBlank()) {
                val derivedKey = cryptoEngine.deriveKey(cluster)
                val keyBase64 = cryptoEngine.keyToBase64(derivedKey)
                pairWithCluster(cluster, keyBase64, partnerDeviceId, partnerDeviceName)
                return
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing QR/URI", e)
        }

        if (input == ownCluster) {
            showToast("Cannot pair with yourself")
            return
        }

        if (input.isNotBlank()) {
            val key = cryptoEngine.deriveKey(input)
            pairWithCluster(input, cryptoEngine.keyToBase64(key))
        }
    }

    private fun pairWithCluster(
        clusterId: String,
        secretKeyBase64: String,
        partnerDeviceId: String? = null,
        partnerDeviceName: String? = null
    ) {
        p2pPreferences.savePairingConfig(clusterId, secretKeyBase64)
        p2pPreferences.setCoupleTrackingEnabled(true)
        val effectiveName = partnerDeviceName?.takeIf { it.isNotBlank() } ?: "Partner"
        if (!partnerDeviceId.isNullOrBlank()) {
            p2pPreferences.setPartnerUserId(partnerDeviceId)
            p2pPreferences.setPartnerName(effectiveName)
            p2pPreferences.recordPairedDevice(
                com.reddy.vittify.data.sync.model.PairedDevice(
                    deviceId = partnerDeviceId,
                    deviceName = effectiveName,
                    lastSeen = System.currentTimeMillis()
                )
            )
        }
        _uiState.update {
            it.copy(
                isPartnerPaired = true,
                partnerName = effectiveName,
                partnerDeviceId = partnerDeviceId,
                clusterId = clusterId,
                pairingCode = clusterId,
                pairingInput = ""
            )
        }
        syncEngine.startSync()
        showToast("Paired with $effectiveName!")
    }

    // ─── FINALIZATION & LEGACY HELPERS ────────────────────────────────────────

    fun setAsMainAccount(bankName: String, accountLast4: String) {
        val key = "${bankName}_${accountLast4}"
        sharedPrefs.edit { putString("main_account", key) }
        _uiState.update { it.copy(mainAccountKey = key) }
    }

    private fun loadMainAccount() {
        val main = sharedPrefs.getString("main_account", null)
        _uiState.update { it.copy(mainAccountKey = main) }
    }

    private fun observeAccounts() {
        accountBalanceRepository.getAllLatestBalances()
            .onEach { accounts ->
                _uiState.update { it.copy(accounts = accounts) }
                if (accounts.size == 1 && _uiState.value.mainAccountKey == null) {
                    val account = accounts.first()
                    setAsMainAccount(account.bankName, account.accountLast4)
                }
            }
            .launchIn(viewModelScope)
    }

    fun finishOnboarding() {
        viewModelScope.launch {
            userPreferencesRepository.markScanTutorialShown()
            _uiState.update { it.copy(onboardingFinished = true) }
        }
    }

    fun showToast(message: String) {
        ToastManager.show(message)
    }
}
