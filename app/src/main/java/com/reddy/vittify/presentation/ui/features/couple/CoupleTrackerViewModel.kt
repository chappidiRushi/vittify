package com.reddy.vittify.presentation.ui.features.couple

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reddy.vittify.data.sync.P2pSyncPreferencesRepository
import com.reddy.vittify.data.sync.dao.SyncChangeDao
import com.reddy.vittify.data.sync.engine.P2pSyncEngine
import com.reddy.vittify.data.sync.engine.SyncProgressState
import com.reddy.vittify.data.sync.qr.QrCodeGenerator
import com.reddy.vittify.data.sync.qr.QrCodeScanner
import com.reddy.vittify.data.sync.security.P2pCryptoEngine
import com.reddy.vittify.data.sync.transport.ConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

data class CoupleTrackerUiState(
    val isCoupleTrackingEnabled: Boolean = false,
    val partnerUserId: String? = null,
    val partnerName: String? = null,
    val clusterId: String? = null,
    val secretKeyBase64: String? = null,
    val myDeviceId: String = "",
    val myDeviceName: String = "",
    val connectionState: ConnectionState = ConnectionState.Idle,
    val localConnectionState: ConnectionState = ConnectionState.Idle,
    val remoteConnectionState: ConnectionState = ConnectionState.Idle,
    val isLocalP2pEnabled: Boolean = true,
    val isRemoteP2pEnabled: Boolean = false,
    val lastConnectedTimestamp: Long = 0L,
    val lastSyncTimestamp: Long = 0L,
    val pendingChangesCount: Int = 0,
    val isHourlySyncEnabled: Boolean = false,
    // Privacy settings (My permissions)
    val shareMyData: Boolean = true,
    val partnerCanEditMyData: Boolean = true,
    val partnerCanDeleteMyData: Boolean = true,
    val partnerCanSeeMyBalances: Boolean = true,
    // Partner's permissions
    val partnerAllowsShareData: Boolean = true,
    val partnerAllowsEditData: Boolean = true,
    val partnerAllowsDeleteData: Boolean = true,
    val partnerAllowsSeeBalances: Boolean = true,
    // Dialog states
    val showQrDialog: Boolean = false,
    val qrBitmap: Bitmap? = null,
    val showPairCodeDialog: Boolean = false,
    val pairingCodeInput: String = "",
    val showUnpairDialog: Boolean = false,
    val isSyncing: Boolean = false,
    val statusMessage: String? = null,
    val syncProgress: SyncProgressState = SyncProgressState()
)

@HiltViewModel
class CoupleTrackerViewModel @Inject constructor(
    private val p2pPreferences: P2pSyncPreferencesRepository,
    private val syncEngine: P2pSyncEngine,
    private val syncChangeDao: SyncChangeDao,
    private val cryptoEngine: P2pCryptoEngine,
    private val coupleSyncScheduler: com.reddy.vittify.data.sync.scheduler.CoupleSyncScheduler
) : ViewModel() {

    companion object {
        private const val TAG = "CoupleTrackerVM"
        const val URI_SCHEME = "vittify"
        const val URI_HOST = "sync"
    }

    private val _uiState = MutableStateFlow(
        CoupleTrackerUiState(
            isCoupleTrackingEnabled = p2pPreferences.isCoupleTrackingEnabled(),
            partnerUserId = p2pPreferences.getPartnerUserId(),
            partnerName = p2pPreferences.getPartnerName(),
            clusterId = p2pPreferences.getClusterId(),
            secretKeyBase64 = p2pPreferences.getSecretKeyBase64(),
            myDeviceId = p2pPreferences.getDeviceId(),
            myDeviceName = p2pPreferences.getDeviceName(),
            connectionState = syncEngine.connectionState.value,
            localConnectionState = syncEngine.localConnectionState.value,
            remoteConnectionState = syncEngine.remoteConnectionState.value,
            isLocalP2pEnabled = p2pPreferences.isLocalP2pEnabled(),
            isRemoteP2pEnabled = p2pPreferences.isRemoteP2pEnabled(),
            lastConnectedTimestamp = p2pPreferences.getLastConnectedTimestamp(),
            lastSyncTimestamp = p2pPreferences.getLastSyncTimestamp(),
            isHourlySyncEnabled = p2pPreferences.isHourlySyncEnabled(),
            shareMyData = p2pPreferences.isShareMyDataWithPartner(),
            partnerCanEditMyData = p2pPreferences.isPartnerCanEditMyData(),
            partnerCanDeleteMyData = p2pPreferences.isPartnerCanDeleteMyData(),
            partnerCanSeeMyBalances = p2pPreferences.isPartnerCanSeeMyBalances(),
            partnerAllowsShareData = p2pPreferences.isPartnerAllowsShareData(),
            partnerAllowsEditData = p2pPreferences.isPartnerAllowsEditData(),
            partnerAllowsDeleteData = p2pPreferences.isPartnerAllowsDeleteData(),
            partnerAllowsSeeBalances = p2pPreferences.isPartnerAllowsSeeBalances()
        )
    )
    val uiState: StateFlow<CoupleTrackerUiState> = _uiState.asStateFlow()

    init {
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

        viewModelScope.launch {
            p2pPreferences.lastConnectedTimestampFlow.collect { time ->
                _uiState.value = _uiState.value.copy(lastConnectedTimestamp = time)
            }
        }

        viewModelScope.launch {
            p2pPreferences.hourlySyncEnabledFlow.collect { enabled ->
                _uiState.value = _uiState.value.copy(isHourlySyncEnabled = enabled)
            }
        }

        viewModelScope.launch {
            syncChangeDao.observePendingChangesCount().collect { count ->
                _uiState.value = _uiState.value.copy(pendingChangesCount = count)
            }
        }

        viewModelScope.launch {
            syncEngine.isSyncingFlow.collect { syncing ->
                val prev = _uiState.value.isSyncing
                _uiState.value = _uiState.value.copy(isSyncing = syncing)
                if (prev && !syncing) {
                    val conn = _uiState.value.connectionState
                    if (conn is ConnectionState.Connected) {
                        _uiState.value = _uiState.value.copy(statusMessage = "Sync completed!")
                    } else if (_uiState.value.statusMessage?.contains("Syncing") == true || _uiState.value.statusMessage?.contains("Connecting") == true) {
                        _uiState.value = _uiState.value.copy(statusMessage = "Sync finished. Ensure both devices are nearby.")
                    }
                }
            }
        }

        viewModelScope.launch {
            syncEngine.syncProgressFlow.collect { progress ->
                _uiState.value = _uiState.value.copy(syncProgress = progress)
            }
        }

        viewModelScope.launch {
            p2pPreferences.coupleTrackingEnabledFlow.collect { enabled ->
                _uiState.value = _uiState.value.copy(isCoupleTrackingEnabled = enabled)
            }
        }

        viewModelScope.launch {
            p2pPreferences.partnerUserIdFlow.collect { partnerId ->
                _uiState.value = _uiState.value.copy(partnerUserId = partnerId)
                _uiState.value = _uiState.value.copy(
                    partnerUserId = partnerId,
                    showQrDialog = if (!partnerId.isNullOrBlank()) false else _uiState.value.showQrDialog,
                    showPairCodeDialog = if (!partnerId.isNullOrBlank()) false else _uiState.value.showPairCodeDialog
                )
            }
        }

        viewModelScope.launch {
            p2pPreferences.partnerNameFlow.collect { partnerName ->
                _uiState.value = _uiState.value.copy(partnerName = partnerName)
            }
        }

        viewModelScope.launch {
            p2pPreferences.clusterIdFlow.collect { clusterId ->
                _uiState.value = _uiState.value.copy(clusterId = clusterId)
            }
        }

        viewModelScope.launch {
            p2pPreferences.lastSyncTimestampFlow.collect { time ->
                _uiState.value = _uiState.value.copy(lastSyncTimestamp = time)
            }
        }

        viewModelScope.launch {
            p2pPreferences.shareMyDataFlow.collect { v ->
                _uiState.value = _uiState.value.copy(shareMyData = v)
            }
        }

        viewModelScope.launch {
            p2pPreferences.partnerCanEditFlow.collect { v ->
                _uiState.value = _uiState.value.copy(partnerCanEditMyData = v)
            }
        }

        viewModelScope.launch {
            p2pPreferences.partnerCanDeleteFlow.collect { v ->
                _uiState.value = _uiState.value.copy(partnerCanDeleteMyData = v)
            }
        }

        viewModelScope.launch {
            p2pPreferences.partnerCanSeeBalancesFlow.collect { v ->
                _uiState.value = _uiState.value.copy(partnerCanSeeMyBalances = v)
            }
        }

        viewModelScope.launch {
            p2pPreferences.partnerAllowsShareDataFlow.collect { v ->
                _uiState.value = _uiState.value.copy(partnerAllowsShareData = v)
            }
        }

        viewModelScope.launch {
            p2pPreferences.partnerAllowsEditDataFlow.collect { v ->
                _uiState.value = _uiState.value.copy(partnerAllowsEditData = v)
            }
        }

        viewModelScope.launch {
            p2pPreferences.partnerAllowsDeleteDataFlow.collect { v ->
                _uiState.value = _uiState.value.copy(partnerAllowsDeleteData = v)
            }
        }

        viewModelScope.launch {
            p2pPreferences.partnerAllowsSeeBalancesFlow.collect { v ->
                _uiState.value = _uiState.value.copy(partnerAllowsSeeBalances = v)
            }
        }

        // Auto-start sync only if not already running
        if (p2pPreferences.isCoupleTrackingEnabled() && !syncEngine.isSyncRunning()) {
            syncEngine.startSync()
        }
    }

    fun retryConnection() {
        _uiState.value = _uiState.value.copy(statusMessage = "Reconnecting to partner...")
        syncEngine.retryConnection()
    }

    fun toggleCoupleTracking(enabled: Boolean) {
        if (!enabled) {
            syncEngine.stopSync()
            p2pPreferences.setCoupleTrackingEnabled(false)
            coupleSyncScheduler.cancelHourlySync()
        } else {
            if (_uiState.value.clusterId == null) {
                createNewCluster()
            }
            p2pPreferences.setCoupleTrackingEnabled(true)
            syncEngine.startSync()
            syncEngine.restartSync()
            coupleSyncScheduler.applyScheduling()
        }
    }

    fun toggleHourlySync(enabled: Boolean) {
        p2pPreferences.setHourlySyncEnabled(enabled)
        coupleSyncScheduler.applyScheduling()
        if (enabled) {
            triggerSync()
            _uiState.value = _uiState.value.copy(
                statusMessage = "Hourly background sync enabled"
            )
        }
    }

    fun createNewCluster() {
        val clusterId = "vittify-" + UUID.randomUUID().toString().take(8)
        val secretKey = cryptoEngine.deriveKey(clusterId)
        val keyBase64 = cryptoEngine.keyToBase64(secretKey)

        p2pPreferences.savePairingConfig(clusterId, keyBase64)
        _uiState.value = _uiState.value.copy(
            clusterId = clusterId,
            secretKeyBase64 = keyBase64
        )
        generateQrCode(clusterId, keyBase64)
        syncEngine.startSync()
    }

    fun setShareMyData(enabled: Boolean) {
        p2pPreferences.setShareMyDataWithPartner(enabled)
    }

    fun setPartnerCanEditMyData(enabled: Boolean) {
        p2pPreferences.setPartnerCanEditMyData(enabled)
    }

    fun setPartnerCanDeleteMyData(enabled: Boolean) {
        p2pPreferences.setPartnerCanDeleteMyData(enabled)
    }

    fun setPartnerCanSeeMyBalances(enabled: Boolean) {
        p2pPreferences.setPartnerCanSeeMyBalances(enabled)
    }

    fun triggerSync() {
        if (_uiState.value.isSyncing) return
        if (_uiState.value.isSyncing) {
            if (!_uiState.value.syncProgress.isVisible) {
                showSyncProgressModal()
            }
            return
        }
        val conn = _uiState.value.connectionState
        val msg = if (conn is ConnectionState.Connected) {
            "Syncing data with partner..."
        } else {
            "Connecting to partner & syncing..."
        }
        _uiState.value = _uiState.value.copy(statusMessage = msg)
        syncEngine.triggerManualSync()
    }

    fun showQrCodeDialog() {
        val clusterId = _uiState.value.clusterId
        val keyBase64 = _uiState.value.secretKeyBase64
        if (clusterId != null && keyBase64 != null) {
            generateQrCode(clusterId, keyBase64)
            _uiState.value = _uiState.value.copy(showQrDialog = true)
        } else {
            createNewCluster()
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
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Cannot pair with yourself"
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
                statusMessage = "Cannot pair with yourself"
            )
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
        if (!partnerDeviceId.isNullOrBlank()) {
            p2pPreferences.setPartnerUserId(partnerDeviceId)
            val effectiveName = partnerDeviceName?.takeIf { it.isNotBlank() } ?: "Partner"
            p2pPreferences.setPartnerName(effectiveName)
            p2pPreferences.recordPairedDevice(
                com.reddy.vittify.data.sync.model.PairedDevice(
                    deviceId = partnerDeviceId,
                    deviceName = effectiveName,
                    lastSeen = System.currentTimeMillis()
                )
            )
        }
        _uiState.value = _uiState.value.copy(
            isCoupleTrackingEnabled = true,
            clusterId = clusterId,
            secretKeyBase64 = secretKeyBase64,
            partnerUserId = partnerDeviceId ?: _uiState.value.partnerUserId,
            partnerName = partnerDeviceName ?: _uiState.value.partnerName,
            showPairCodeDialog = false,
            showQrDialog = false,
            statusMessage = "Pairing successful!"
        )
        syncEngine.restartSync()
        coupleSyncScheduler.applyScheduling()
    }

    fun scanQrCode(context: Context) {
        val scanner = QrCodeScanner(context)
        scanner.scan(
            onSuccess = { result ->
                p2pPreferences.setIsScannerDevice(true)
                applyPairingInput(result)
            },
            onFailure = { e ->
                Log.e(TAG, "QR scan failed", e)
                _uiState.value = _uiState.value.copy(statusMessage = "QR scan failed: ${e.message}")
            }
        )
    }

    fun dismissSyncProgressModal() {
        syncEngine.dismissProgressModal()
    }

    fun showSyncProgressModal() {
        syncEngine.showProgressModal()
    }

    fun cancelSync() {
        syncEngine.cancelSync()
    }

    fun showUnpairDialog() {
        _uiState.value = _uiState.value.copy(showUnpairDialog = true)
    }

    fun dismissUnpairDialog() {
        _uiState.value = _uiState.value.copy(showUnpairDialog = false)
    }

    fun confirmUnpair() {
        syncEngine.stopSync()
        coupleSyncScheduler.cancelHourlySync()
        p2pPreferences.unpairPartner()
        _uiState.value = _uiState.value.copy(
            partnerUserId = null,
            partnerName = null,
            showUnpairDialog = false,
            statusMessage = "Unpaired from partner"
        )
        viewModelScope.launch {
            coupleSyncScheduler.cancelHourlySync()
            syncEngine.unpair(notifyPartner = true)
            _uiState.value = _uiState.value.copy(
                isCoupleTrackingEnabled = false,
                partnerUserId = null,
                partnerName = null,
                clusterId = null,
                secretKeyBase64 = null,
                showUnpairDialog = false,
                statusMessage = "Unpaired from partner and removed partner data"
            )
        }
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }
}

