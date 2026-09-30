package com.reddy.vittify.presentation.ui.features.sync.p2p

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reddy.vittify.data.sync.P2pSyncPreferencesRepository
import com.reddy.vittify.data.sync.dao.SyncChangeDao
import com.reddy.vittify.data.sync.engine.P2pSyncEngine
import com.reddy.vittify.data.sync.model.PairedDevice
import com.reddy.vittify.data.sync.qr.QrCodeGenerator
import com.reddy.vittify.data.sync.qr.QrCodeScanner
import com.reddy.vittify.data.sync.security.P2pCryptoEngine
import com.reddy.vittify.data.sync.transport.ConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

data class P2pSyncUiState(
    val isSyncEnabled: Boolean = false,
    val isLocalP2pEnabled: Boolean = true,
    val isRemoteP2pEnabled: Boolean = false,
    val clusterId: String? = null,
    val secretKeyBase64: String? = null,
    val deviceId: String = "",
    val deviceName: String = "",
    val connectionState: ConnectionState = ConnectionState.Idle,
    val localConnectionState: ConnectionState = ConnectionState.Idle,
    val remoteConnectionState: ConnectionState = ConnectionState.Idle,
    val lastSyncTimestamp: Long = 0L,
    val pendingChangesCount: Int = 0,
    val pairedDevices: List<PairedDevice> = emptyList(),
    val showQrDialog: Boolean = false,
    val qrBitmap: Bitmap? = null,
    val showPairCodeDialog: Boolean = false,
    val pairingCodeInput: String = "",
    val showRenameDialog: Boolean = false,
    val renameTargetDeviceId: String? = null,
    val renameTargetInitialName: String = "",
    val statusBanner: String? = null
)

@HiltViewModel
class P2pSyncViewModel @Inject constructor(
    private val p2pPreferences: P2pSyncPreferencesRepository,
    private val syncEngine: P2pSyncEngine,
    private val syncChangeDao: SyncChangeDao,
    private val cryptoEngine: P2pCryptoEngine
) : ViewModel() {

    companion object {
        private const val TAG = "P2pSyncViewModel"
        const val URI_SCHEME = "vittify"
        const val URI_HOST = "sync"
    }

    private val _uiState = MutableStateFlow(
        P2pSyncUiState(
            isSyncEnabled = p2pPreferences.isSyncEnabled(),
            isLocalP2pEnabled = p2pPreferences.isLocalP2pEnabled(),
            isRemoteP2pEnabled = p2pPreferences.isRemoteP2pEnabled(),
            clusterId = p2pPreferences.getClusterId(),
            secretKeyBase64 = p2pPreferences.getSecretKeyBase64(),
            deviceId = p2pPreferences.getDeviceId(),
            deviceName = p2pPreferences.getDeviceName(),
            localConnectionState = syncEngine.localConnectionState.value,
            remoteConnectionState = syncEngine.remoteConnectionState.value,
            lastSyncTimestamp = p2pPreferences.getLastSyncTimestamp(),
            pairedDevices = p2pPreferences.getPairedDevices()
        )
    )
    val uiState: StateFlow<P2pSyncUiState> = _uiState.asStateFlow()

    init {
        // Observe connection state from engine
        viewModelScope.launch {
            syncEngine.connectionState.collect { state ->
                _uiState.value = _uiState.value.copy(connectionState = state)
            }
        }

        viewModelScope.launch {
            syncEngine.localConnectionState.collect { state ->
                _uiState.value = _uiState.value.copy(localConnectionState = state)
            }
        }

        viewModelScope.launch {
            syncEngine.remoteConnectionState.collect { state ->
                _uiState.value = _uiState.value.copy(remoteConnectionState = state)
            }
        }

        // Observe pending changes count
        viewModelScope.launch {
            syncChangeDao.observePendingChangesCount().collect { count ->
                _uiState.value = _uiState.value.copy(pendingChangesCount = count)
            }
        }

        // Observe preferences
        viewModelScope.launch {
            p2pPreferences.pairedDevicesFlow.collect { devices ->
                _uiState.value = _uiState.value.copy(pairedDevices = devices)
                _uiState.value = _uiState.value.copy(
                    pairedDevices = devices,
                    showQrDialog = if (devices.isNotEmpty() && _uiState.value.showQrDialog) false else _uiState.value.showQrDialog,
                    showPairCodeDialog = if (devices.isNotEmpty() && _uiState.value.showPairCodeDialog) false else _uiState.value.showPairCodeDialog
                )
            }
        }

        viewModelScope.launch {
            p2pPreferences.lastSyncTimestampFlow.collect { time ->
                _uiState.value = _uiState.value.copy(lastSyncTimestamp = time)
            }
        }

        viewModelScope.launch {
            p2pPreferences.deviceNameFlow.collect { name ->
                _uiState.value = _uiState.value.copy(deviceName = name)
            }
        }

        viewModelScope.launch {
            p2pPreferences.localP2pEnabledFlow.collect { enabled ->
                _uiState.value = _uiState.value.copy(isLocalP2pEnabled = enabled)
            }
        }

        viewModelScope.launch {
            p2pPreferences.remoteP2pEnabledFlow.collect { enabled ->
                _uiState.value = _uiState.value.copy(isRemoteP2pEnabled = enabled)
            }
        }

        // If already enabled and not running, kick off sync
        if (p2pPreferences.isSyncEnabled() && !syncEngine.isSyncRunning()) {
            syncEngine.startSync()
        }
    }

    fun retryConnection() {
        _uiState.value = _uiState.value.copy(statusBanner = "Reconnecting to devices...")
        syncEngine.retryConnection()
    }

    fun toggleSync(enabled: Boolean) {
        if (!enabled) {
            syncEngine.stopSync()
            p2pPreferences.setSyncEnabled(false)
            _uiState.value = _uiState.value.copy(isSyncEnabled = false)
        } else {
            if (_uiState.value.clusterId == null) {
                createNewCluster()
            } else {
                p2pPreferences.setSyncEnabled(true)
                _uiState.value = _uiState.value.copy(isSyncEnabled = true)
                syncEngine.startSync()
            }
        }
    }

    fun createNewCluster() {
        val clusterId = "vittify-" + UUID.randomUUID().toString().take(8)
        val secretKey = cryptoEngine.deriveKey(clusterId)
        val keyBase64 = cryptoEngine.keyToBase64(secretKey)

        p2pPreferences.savePairingConfig(clusterId, keyBase64)
        _uiState.value = _uiState.value.copy(
            isSyncEnabled = true,
            clusterId = clusterId,
            secretKeyBase64 = keyBase64
        )

        generateQrCode(clusterId, keyBase64)
        syncEngine.startSync()
    }

    fun showQrCodeDialog() {
        val clusterId = _uiState.value.clusterId
        val keyBase64 = _uiState.value.secretKeyBase64
        if (clusterId != null && keyBase64 != null) {
            generateQrCode(clusterId, keyBase64)
            _uiState.value = _uiState.value.copy(showQrDialog = true)
        }
    }

    fun dismissQrCodeDialog() {
        _uiState.value = _uiState.value.copy(showQrDialog = false)
    }

    private fun generateQrCode(clusterId: String, keyBase64: String) {
        val myDeviceId = p2pPreferences.getDeviceId()
        val myDeviceName = p2pPreferences.getDeviceName()
        viewModelScope.launch(Dispatchers.Default) {
            val pairingUri = "$URI_SCHEME://$URI_HOST?cluster=$clusterId&deviceId=${Uri.encode(myDeviceId)}&name=${Uri.encode(myDeviceName)}"
            val bitmap = QrCodeGenerator.generateQrBitmap(pairingUri, 600)
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(qrBitmap = bitmap)
            }
        }
    }

    fun showPairCodeDialog() {
        _uiState.value = _uiState.value.copy(showPairCodeDialog = true, pairingCodeInput = "")
    }

    fun dismissPairCodeDialog() {
        _uiState.value = _uiState.value.copy(showPairCodeDialog = false)
    }

    fun updatePairingCodeInput(input: String) {
        _uiState.value = _uiState.value.copy(pairingCodeInput = input)
    }

    fun applyPairingInput(rawInput: String) {
        val input = rawInput.trim()
        val ownCluster = _uiState.value.clusterId ?: p2pPreferences.getClusterId()
        val ownDeviceId = p2pPreferences.getDeviceId()

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
                _uiState.value = _uiState.value.copy(
                    statusBanner = "Cannot pair with yourself"
                )
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
            _uiState.value = _uiState.value.copy(
                statusBanner = "Cannot pair with yourself"
            )
            return
        }

        // Fallback: user entered a short cluster ID directly
        if (input.isNotBlank()) {
            val key = cryptoEngine.deriveKey(input)
            pairWithCluster(input, cryptoEngine.keyToBase64(key))
        }
    }

    fun scanQrCode(context: Context) {
        val scanner = QrCodeScanner(context)
        scanner.scan(
            onSuccess = { result ->
                applyPairingInput(result)
            },
            onFailure = { e ->
                Log.w(TAG, "QR scan failed or cancelled", e)
                _uiState.value = _uiState.value.copy(
                    statusBanner = "QR scan cancelled or unavailable"
                )
            }
        )
    }

    private fun pairWithCluster(
        clusterId: String,
        secretKeyBase64: String,
        partnerDeviceId: String? = null,
        partnerDeviceName: String? = null
    ) {
        p2pPreferences.savePairingConfig(clusterId, secretKeyBase64)
        if (!partnerDeviceId.isNullOrBlank()) {
            p2pPreferences.recordPairedDevice(
                PairedDevice(
                    deviceId = partnerDeviceId,
                    deviceName = partnerDeviceName ?: "Android Device",
                    lastSeen = System.currentTimeMillis()
                )
            )
        }
        _uiState.value = _uiState.value.copy(
            isSyncEnabled = true,
            clusterId = clusterId,
            secretKeyBase64 = secretKeyBase64,
            showPairCodeDialog = false,
            showQrDialog = false,
            statusBanner = "Successfully paired to sync group!"
        )
        syncEngine.restartSync()
    }

    fun triggerManualSync() {
        syncEngine.triggerManualSync()
        _uiState.value = _uiState.value.copy(statusBanner = "Sync requested...")
    }

    fun clearBanner() {
        _uiState.value = _uiState.value.copy(statusBanner = null)
    }

    fun toggleLocalP2p(enabled: Boolean) {
        p2pPreferences.setLocalP2pEnabled(enabled)
        _uiState.value = _uiState.value.copy(isLocalP2pEnabled = enabled)
        if (_uiState.value.isSyncEnabled) {
            syncEngine.restartSync()
        }
    }

    fun toggleRemoteP2p(enabled: Boolean) {
        p2pPreferences.setRemoteP2pEnabled(enabled)
        _uiState.value = _uiState.value.copy(isRemoteP2pEnabled = enabled)
        if (_uiState.value.isSyncEnabled) {
            syncEngine.restartSync()
        }
    }

    fun openRenameDialog(targetDeviceId: String?, initialName: String) {
        _uiState.value = _uiState.value.copy(
            showRenameDialog = true,
            renameTargetDeviceId = targetDeviceId,
            renameTargetInitialName = initialName
        )
    }

    fun dismissRenameDialog() {
        _uiState.value = _uiState.value.copy(
            showRenameDialog = false,
            renameTargetDeviceId = null,
            renameTargetInitialName = ""
        )
    }

    fun confirmRename(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return
        val targetId = _uiState.value.renameTargetDeviceId
        if (targetId == null) {
            // Renaming My Device
            p2pPreferences.setDeviceName(trimmed)
            _uiState.value = _uiState.value.copy(deviceName = trimmed)
            syncEngine.broadcastDeviceName(trimmed)
        } else {
            // Renaming a paired peer
            p2pPreferences.renamePairedDevice(targetId, trimmed)
        }
        dismissRenameDialog()
    }

    fun unpairAndReset() {
        syncEngine.stopSync()
        p2pPreferences.clearPairingConfig()
        _uiState.value = _uiState.value.copy(
            isSyncEnabled = false,
            clusterId = null,
            secretKeyBase64 = null,
            pairedDevices = emptyList(),
            lastSyncTimestamp = 0L,
            qrBitmap = null
        )
    }
}

