package com.reddy.vittify.data.sync.engine

import android.util.Log
import androidx.annotation.Keep
import com.reddy.vittify.data.preferences.UserPreferencesRepository
import com.reddy.vittify.data.sync.P2pSyncPreferencesRepository
import com.reddy.vittify.data.sync.SyncChangeTracker
import com.reddy.vittify.data.sync.SyncJsonSerializer
import com.reddy.vittify.data.sync.dao.SyncChangeDao
import com.reddy.vittify.data.sync.model.HandshakePayload
import com.reddy.vittify.data.sync.model.P2pPacket
import com.reddy.vittify.data.sync.model.PairedDevice
import com.reddy.vittify.data.sync.model.SyncChangeEntity
import com.reddy.vittify.data.sync.model.SyncChangePayload
import com.reddy.vittify.data.sync.model.SyncSessionMetaPayload
import com.reddy.vittify.data.sync.security.P2pCryptoEngine
import com.reddy.vittify.data.sync.transport.ConnectionState
import com.reddy.vittify.data.sync.transport.P2pTransportCoordinator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import com.reddy.vittify.data.database.VittifyDatabase
import androidx.room.withTransaction
import com.reddy.vittify.data.sync.model.SubcategorySyncPayload

@Keep
enum class SyncProgressPhase {
    IDLE,
    CONNECTING,
    PREPARING,
    SENDING,
    RECEIVING,
    COMPLETED,
    ERROR
}

@Keep
enum class SyncSessionRole {
    NONE,
    INITIATOR, // Step 1 Sender -> Step 2 Receiver
    RESPONDER  // Step 1 Receiver -> Step 2 Sender
}

@Keep
data class RecordAckResult(
    val sequenceNumber: Int,
    val recordIds: List<Long> = emptyList()
)

@Keep
data class SyncProgressState(
    val isVisible: Boolean = false,
    val phase: SyncProgressPhase = SyncProgressPhase.IDLE,
    val current: Int = 0,
    val total: Int = 0,
    val entityType: String = "",
    val statusMessage: String = "",
    val partnerDeviceName: String = "",
    val errorMessage: String? = null,
    val sentCount: Int = 0,
    val receivedCount: Int = 0
)

@Keep
data class SyncQueueItem(
    val changeId: Long,
    val payload: SyncChangePayload
)

@Singleton
class P2pSyncEngine @Inject constructor(
    private val transportCoordinator: P2pTransportCoordinator,
    private val cryptoEngine: P2pCryptoEngine,
    private val p2pPreferences: P2pSyncPreferencesRepository,
    private val syncChangeDao: SyncChangeDao,
    private val syncChangeTracker: SyncChangeTracker,
    private val mergeEngine: SyncMergeEngine,
    private val serializer: SyncJsonSerializer,
    private val database: VittifyDatabase,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    companion object {
        private const val TAG = "P2pSyncEngine"
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var packetCollectorJob: Job? = null
    private var liveChangeJob: Job? = null
    private var connectionStateJob: Job? = null
    private var activeSecretKey: SecretKey? = null
    private var isEngineRunning = false

    private var currentRole: SyncSessionRole = SyncSessionRole.NONE
    private var currentSyncStep: Int = 0

    private val _isSyncingFlow = MutableStateFlow(false)
    val isSyncingFlow: StateFlow<Boolean> = _isSyncingFlow.asStateFlow()

    private val _syncProgressFlow = MutableStateFlow(SyncProgressState())
    val syncProgressFlow: StateFlow<SyncProgressState> = _syncProgressFlow.asStateFlow()

    private var pendingMetaAck: CompletableDeferred<Boolean>? = null
    private var pendingRecordAck: CompletableDeferred<RecordAckResult>? = null
    private var pendingStepCompleteAck: CompletableDeferred<Boolean>? = null

    val connectionState: StateFlow<ConnectionState> = transportCoordinator.activeConnectionState
    val localConnectionState: StateFlow<ConnectionState> = transportCoordinator.localConnectionState
    val remoteConnectionState: StateFlow<ConnectionState> = transportCoordinator.remoteConnectionState

    private var syncSessionJob: Job? = null

    fun isSyncRunning(): Boolean = isEngineRunning

    fun dismissProgressModal() {
        _syncProgressFlow.value = _syncProgressFlow.value.copy(isVisible = false)
    }

    fun showProgressModal() {
        _syncProgressFlow.value = _syncProgressFlow.value.copy(isVisible = true)
    }

    fun cancelSync() {
        scope.launch {
            try {
                sendPacket(
                    P2pPacket(
                        type = P2pPacket.TYPE_SYNC_CANCEL,
                        senderDeviceId = p2pPreferences.getDeviceId(),
                        senderDeviceName = p2pPreferences.getDeviceName()
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed sending SYNC_CANCEL packet", e)
            }
            syncSessionJob?.cancel()
            pendingMetaAck?.cancel()
            pendingRecordAck?.cancel()
            pendingStepCompleteAck?.cancel()
            currentRole = SyncSessionRole.NONE
            currentSyncStep = 0
            _syncProgressFlow.value = _syncProgressFlow.value.copy(
                phase = SyncProgressPhase.ERROR,
                errorMessage = "Sync cancelled."
            )
            _isSyncingFlow.value = false
        }
    }

    fun restartSync() {
        if (p2pPreferences.isSyncEnabled()) {
            startSync(force = true)
        } else {
            stopSync()
        }
    }

    fun isLocalOwner(ownerId: String): Boolean {
        val myDeviceId = p2pPreferences.getDeviceId()
        val partnerId = p2pPreferences.getPartnerUserId()
        if (ownerId.isBlank()) return true
        if (ownerId == myDeviceId) return true
        if (!partnerId.isNullOrBlank() && ownerId != partnerId) return true
        return false
    }

    suspend fun alignLocalDeviceIdIfNeeded() {
        try {
            val partnerId = p2pPreferences.getPartnerUserId()
            val transactions = database.transactionDao().getAllTransactionsIncludingDeleted().first()
            val accounts = database.accountBalanceDao().getAllBalances().first()

            val candidateOwners = (transactions.map { it.ownerId } + accounts.map { it.ownerId })
                .filter { it.isNotBlank() && (partnerId.isNullOrBlank() || it != partnerId) }
                .groupingBy { it }
                .eachCount()

            val dominantOwnerId = candidateOwners.maxByOrNull { it.value }?.key
            if (!dominantOwnerId.isNullOrBlank()) {
                val currentDeviceId = p2pPreferences.getDeviceId()
                if (currentDeviceId != dominantOwnerId) {
                    Log.d(TAG, "Adopting dominant local database ownerId: $dominantOwnerId (was $currentDeviceId)")
                    p2pPreferences.setDeviceId(dominantOwnerId)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed aligning local device ID", e)
        }
    }

    fun startSync(force: Boolean = false) {
        if (!p2pPreferences.isSyncEnabled()) return

        scope.launch {
            alignLocalDeviceIdIfNeeded()
        }

        val clusterId = p2pPreferences.getClusterId() ?: return
        val secretKeyBase64 = p2pPreferences.getSecretKeyBase64() ?: return
        val deviceId = p2pPreferences.getDeviceId()
        val deviceName = p2pPreferences.getDeviceName()

        activeSecretKey = cryptoEngine.keyFromBase64(secretKeyBase64)

        if (isEngineRunning && !force) {
            Log.d(TAG, "SyncEngine already running with active cluster; ensuring coordinator is started")
            transportCoordinator.start(clusterId, deviceId, deviceName, force = false)
            return
        }

        isEngineRunning = true

        // Ensure existing database data is seeded into sync_changes so peers get current records
        scope.launch {
            seedExistingDatabaseIfNeeded()
        }

        transportCoordinator.start(clusterId, deviceId, deviceName, force = force)
        startListening()
    }

    fun onAppForegrounded() {
        Log.d(TAG, "onAppForegrounded()")
        transportCoordinator.setForeground(true)
        if (p2pPreferences.isSyncEnabled()) {
            startSync(force = false)
        }
    }

    fun onAppBackgrounded() {
        Log.d(TAG, "onAppBackgrounded()")
        transportCoordinator.setForeground(false)
    }

    fun retryConnection() {
        Log.d(TAG, "retryConnection() called on engine")
        if (p2pPreferences.isSyncEnabled()) {
            startSync(force = false)
            transportCoordinator.retryAll()
        }
    }

    private fun startListening() {
        packetCollectorJob?.cancel()
        liveChangeJob?.cancel()
        connectionStateJob?.cancel()

        val key = activeSecretKey ?: return

        // Monitor transport state changes
        connectionStateJob = scope.launch {
            transportCoordinator.activeConnectionState.collect { state ->
                if (state is ConnectionState.Connected) {
                    p2pPreferences.updateLastConnectedTimestamp(System.currentTimeMillis())
                    alignLocalDeviceIdIfNeeded()
                    val isScanner = p2pPreferences.isScannerDevice()
                    sendHandshake(isScanner = isScanner)
                    if (isScanner && !_isSyncingFlow.value) {
                        Log.d(TAG, "Connected as scanner device! Initiating Step 1 of sequential lockstep sync...")
                        p2pPreferences.setIsScannerDevice(false)
                        startSequentialSyncSession(step = 1, isInitialPair = true)
                    } else {
                        Log.d(TAG, "Connected as partner. Waiting for peer to start sync session.")
                    }
                    Log.d(TAG, "Connected to transport. Sent handshake (isScanner=$isScanner). Awaiting peer handshake to determine sync turn.")
                }
            }
        }

        // Process incoming encrypted packets
        packetCollectorJob = scope.launch {
            transportCoordinator.incomingPackets().collect { rawCipherBytes ->
                try {
                    val decryptedBytes = cryptoEngine.decrypt(rawCipherBytes, key)
                    val packetJson = String(decryptedBytes, Charsets.UTF_8)
                    val packet = serializer.fromJson(packetJson, P2pPacket::class.java)

                    handleIncomingPacket(packet)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed decrypting or processing incoming packet", e)
                }
            }
        }

        // Live broadcast of newly created/updated/deleted entities
        liveChangeJob = scope.launch {
            syncChangeTracker.liveChangesFlow.collect { localChange ->
                if (!p2pPreferences.isShareMyDataWithPartner()) return@collect
                if (_isSyncingFlow.value) return@collect
                if (!p2pPreferences.isPartnerCanSeeMyBalances() && localChange.entityType == SyncChangeEntity.TYPE_ACCOUNT) return@collect
                val state = transportCoordinator.activeConnectionState.value
                if (state is ConnectionState.Connected) {
                    val payload = SyncChangePayload(
                        entityType = localChange.entityType,
                        entityId = localChange.entityId,
                        operation = localChange.operation,
                        payloadJson = localChange.payloadJson,
                        timestamp = localChange.timestamp,
                        originDeviceId = localChange.originDeviceId,
                        changeId = localChange.id
                    )
                    sendPacket(
                        P2pPacket(
                            type = P2pPacket.TYPE_CHANGES,
                            senderDeviceId = p2pPreferences.getDeviceId(),
                            senderDeviceName = p2pPreferences.getDeviceName(),
                            changes = listOf(payload),
                            recordIds = listOf(localChange.id)
                        )
                    )
                }
            }
        }
    }

    private suspend fun handleIncomingPacket(packet: P2pPacket) {
        p2pPreferences.updateLastConnectedTimestamp(System.currentTimeMillis())
        val myDeviceId = p2pPreferences.getDeviceId()
        if (packet.senderDeviceId == myDeviceId) return

        // Track peer in paired devices
        p2pPreferences.recordPairedDevice(
            PairedDevice(
                deviceId = packet.senderDeviceId,
                deviceName = packet.senderDeviceName,
                lastSeen = packet.timestamp
            )
        )

        // Capture partner profile name if sent
        val partnerProfileName = packet.senderUserName?.takeIf { it.isNotBlank() && it != "User" }
            ?: packet.handshake?.userName?.takeIf { it.isNotBlank() && it != "User" }
        if (partnerProfileName != null) {
            p2pPreferences.setPartnerName(partnerProfileName)
        }

        when (packet.type) {
            P2pPacket.TYPE_UNPAIR -> {
                Log.d(TAG, "Received UNPAIR packet from partner ${packet.senderDeviceName} (${packet.senderDeviceId})")
                val partnerId = packet.senderDeviceId
                scope.launch {
                    deletePartnerData(partnerId)
                    try {
                        syncChangeDao.clearAll()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to clear sync change queue on unpair", e)
                    }
                    p2pPreferences.unpairPartner()
                    stopSync()
                }
            }

            P2pPacket.TYPE_HANDSHAKE -> {
                val handshake = packet.handshake ?: return
                Log.d(TAG, "Received handshake from ${packet.senderDeviceName} (lastSync=${handshake.lastSyncTimestamp}, isScanner=${handshake.isScanner})")

                val partnerId = handshake.partnerUserId ?: packet.senderDeviceId
                p2pPreferences.setPartnerUserId(partnerId)
                p2pPreferences.setPartnerName(packet.senderDeviceName)
                val effectivePartnerName = handshake.userName?.takeIf { it.isNotBlank() && it != "User" }
                    ?: packet.senderUserName?.takeIf { it.isNotBlank() && it != "User" }
                    ?: packet.senderDeviceName
                p2pPreferences.setPartnerName(effectivePartnerName)
                if (handshake.permissions.isNotEmpty()) {
                    p2pPreferences.setPartnerPermissions(
                        shareData = handshake.permissions["share_data"] ?: true,
                        canEdit = handshake.permissions["can_edit"] ?: true,
                        canDelete = handshake.permissions["can_delete"] ?: true,
                        canSeeBalances = handshake.permissions["can_see_balances"] ?: true
                    )
                }

                alignLocalDeviceIdIfNeeded()
                seedExistingDatabaseIfNeeded()

                val iAmScanner = p2pPreferences.isScannerDevice()
                val partnerIsScanner = handshake.isScanner

                // Symmetric turn-taking: exactly one device initiates Step 1
                val shouldInitiate = when {
                    iAmScanner && !partnerIsScanner -> true
                    !iAmScanner && partnerIsScanner -> false
                    else -> myDeviceId < partnerId
                }

                if (iAmScanner) {
                    p2pPreferences.setIsScannerDevice(false)
                }

                if (shouldInitiate && !_isSyncingFlow.value) {
                    Log.d(TAG, "Device $myDeviceId is designated INITIATOR (Step 1 first). Partner $partnerId will be RESPONDER (Step 2 second).")
                    currentRole = SyncSessionRole.INITIATOR
                    syncSessionJob?.cancel()
                    syncSessionJob = scope.launch {
                        delay(600L)
                        startSequentialSyncSession(step = 1, isInitialPair = (iAmScanner || partnerIsScanner))
                    }
                } else if (!shouldInitiate) {
                    Log.d(TAG, "Device $myDeviceId is designated RESPONDER. Waiting for partner $partnerId to send Step 1 first; will send Step 2 second.")
                    currentRole = SyncSessionRole.RESPONDER
                }
            }

            P2pPacket.TYPE_SYNC_START_META -> {
                val meta = packet.syncMeta ?: return
                Log.d(TAG, "Received SYNC_START_META (step=${meta.step}, total=${meta.totalRecords}) from ${packet.senderDeviceName}")

                // Simultaneous initiation collision resolution
                if (meta.step == 1 && currentSyncStep == 1 && currentRole == SyncSessionRole.INITIATOR && _isSyncingFlow.value) {
                    Log.w(TAG, "Simultaneous Step 1 initiation detected! Resolving tie-breaker: myDeviceId=$myDeviceId vs partner=${packet.senderDeviceId}")
                    if (myDeviceId < packet.senderDeviceId) {
                        Log.d(TAG, "Tie-breaker won by $myDeviceId. Remaining Step 1 Initiator; ignoring peer start meta.")
                        return
                    } else {
                        Log.d(TAG, "Tie-breaker yielded by $myDeviceId. Cancelling outgoing Step 1 and switching to Step 1 Responder.")
                        syncSessionJob?.cancel()
                        pendingMetaAck?.cancel()
                        pendingRecordAck?.cancel()
                    }
                }

                currentRole = if (meta.step == 1) SyncSessionRole.RESPONDER else SyncSessionRole.INITIATOR
                currentSyncStep = meta.step
                _isSyncingFlow.value = true
                _syncProgressFlow.value = SyncProgressState(
                    isVisible = true,
                    phase = SyncProgressPhase.RECEIVING,
                    current = 0,
                    total = meta.totalRecords,
                    partnerDeviceName = packet.senderDeviceName,
                    statusMessage = if (meta.totalRecords == 0) "No records to receive from partner" else "Receiving from partner (0/${meta.totalRecords})...",
                    sentCount = _syncProgressFlow.value.sentCount,
                    receivedCount = _syncProgressFlow.value.receivedCount
                )
                sendPacket(
                    P2pPacket(
                        type = P2pPacket.TYPE_SYNC_META_ACK,
                        senderDeviceId = myDeviceId,
                        senderDeviceName = p2pPreferences.getDeviceName(),
                        step = meta.step
                    )
                )
            }

            P2pPacket.TYPE_SYNC_META_ACK -> {
                Log.d(TAG, "Received SYNC_META_ACK from ${packet.senderDeviceName}")
                pendingMetaAck?.complete(true)
            }

            P2pPacket.TYPE_SYNC_RECORD -> {
                val changes = packet.changes ?: listOfNotNull(packet.singleChange)
                if (changes.isEmpty()) return
                val seq = packet.sequenceNumber ?: return
                val total = packet.totalRecords ?: _syncProgressFlow.value.total
                val receivedRecordIds = packet.recordIds ?: changes.mapNotNull { it.changeId }

                try {
                    mergeEngine.applyRemoteChanges(changes)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed applying remote changes in lockstep sync", e)
                }

                _syncProgressFlow.value = _syncProgressFlow.value.copy(
                    phase = SyncProgressPhase.RECEIVING,
                    current = seq,
                    total = total,
                    entityType = changes.firstOrNull()?.entityType ?: "",
                    receivedCount = _syncProgressFlow.value.receivedCount + changes.size,
                    statusMessage = "Receiving from partner ($seq/$total)"
                )

                // Reply with ACK containing the exact record IDs received and processed
                sendPacket(
                    P2pPacket(
                        type = P2pPacket.TYPE_SYNC_RECORD_ACK,
                        senderDeviceId = myDeviceId,
                        senderDeviceName = p2pPreferences.getDeviceName(),
                        sequenceNumber = seq,
                        step = packet.step,
                        recordIds = receivedRecordIds
                    )
                )
            }

            P2pPacket.TYPE_SYNC_RECORD_ACK -> {
                val seq = packet.sequenceNumber ?: return
                val ackedIds = packet.recordIds ?: emptyList()
                Log.d(TAG, "Received SYNC_RECORD_ACK for seq=$seq with ${ackedIds.size} record ids from ${packet.senderDeviceName}")
                pendingRecordAck?.complete(RecordAckResult(sequenceNumber = seq, recordIds = ackedIds))
            }

            P2pPacket.TYPE_SYNC_STEP_COMPLETE -> {
                val step = packet.step ?: 1
                Log.d(TAG, "Received SYNC_STEP_COMPLETE for step $step from ${packet.senderDeviceName}")
                sendPacket(
                    P2pPacket(
                        type = P2pPacket.TYPE_SYNC_STEP_COMPLETE_ACK,
                        senderDeviceId = myDeviceId,
                        senderDeviceName = p2pPreferences.getDeviceName(),
                        step = step
                    )
                )
                if (step == 1) {
                    Log.d(TAG, "Step 1 finished. This device is now initiating Step 2 (sending local data to partner)...")
                    currentRole = SyncSessionRole.RESPONDER
                    syncSessionJob?.cancel()
                    syncSessionJob = scope.launch {
                        delay(400L)
                        startSequentialSyncSession(step = 2, isInitialPair = false)
                    }
                }
            }

            P2pPacket.TYPE_SYNC_STEP_COMPLETE_ACK -> {
                Log.d(TAG, "Received SYNC_STEP_COMPLETE_ACK from ${packet.senderDeviceName}")
                pendingStepCompleteAck?.complete(true)
            }

            P2pPacket.TYPE_SYNC_CANCEL -> {
                Log.d(TAG, "Received SYNC_CANCEL from partner ${packet.senderDeviceName}")
                syncSessionJob?.cancel()
                pendingMetaAck?.cancel()
                pendingRecordAck?.cancel()
                pendingStepCompleteAck?.cancel()
                currentRole = SyncSessionRole.NONE
                currentSyncStep = 0
                _syncProgressFlow.value = _syncProgressFlow.value.copy(
                    phase = SyncProgressPhase.ERROR,
                    errorMessage = "Sync was cancelled by your partner."
                )
                _isSyncingFlow.value = false
            }

            P2pPacket.TYPE_SYNC_SESSION_DONE -> {
                Log.d(TAG, "Received SYNC_SESSION_DONE from ${packet.senderDeviceName}")
                val now = System.currentTimeMillis()
                p2pPreferences.updateLastSyncTimestamp(now)
                _syncProgressFlow.value = _syncProgressFlow.value.copy(
                    phase = SyncProgressPhase.COMPLETED,
                    statusMessage = "Sync complete! ${_syncProgressFlow.value.sentCount} sent, ${_syncProgressFlow.value.receivedCount} received."
                )
                currentRole = SyncSessionRole.NONE
                currentSyncStep = 0
                _isSyncingFlow.value = false
            }

            P2pPacket.TYPE_CHANGES -> {
                val changes = packet.changes ?: return
                Log.d(TAG, "Received ${changes.size} changes from ${packet.senderDeviceName}")

                val appliedCount = mergeEngine.applyRemoteChanges(changes)
                Log.d(TAG, "Merged $appliedCount changes into database")

                val highestTimestamp = changes.maxOfOrNull { it.timestamp } ?: packet.timestamp
                p2pPreferences.updateLastSyncTimestamp(highestTimestamp)

                val receivedRecordIds = packet.recordIds ?: changes.mapNotNull { it.changeId }

                // Send ACK back containing record IDs
                sendPacket(
                    P2pPacket(
                        type = P2pPacket.TYPE_ACK,
                        senderDeviceId = myDeviceId,
                        senderDeviceName = p2pPreferences.getDeviceName(),
                        ackTimestamp = highestTimestamp,
                        recordIds = receivedRecordIds
                    )
                )
                _isSyncingFlow.value = false
            }

            P2pPacket.TYPE_ACK -> {
                val ackedRecordIds = packet.recordIds
                if (!ackedRecordIds.isNullOrEmpty()) {
                    Log.d(TAG, "Received ACK with ${ackedRecordIds.size} record IDs: $ackedRecordIds")
                    syncChangeDao.markAsSynced(ackedRecordIds)
                } else {
                    val ackTime = packet.ackTimestamp ?: return
                    Log.d(TAG, "Received legacy timestamp ACK for changes up to $ackTime")
                    val pending = syncChangeDao.getChangesSince(0L).filter { it.timestamp <= ackTime }
                    if (pending.isNotEmpty()) {
                        syncChangeDao.markAsSynced(pending.map { it.id })
                    }
                }
                _isSyncingFlow.value = false
            }

            P2pPacket.TYPE_SYNC_REQUEST -> {
                Log.d(TAG, "Received SYNC_REQUEST from ${packet.senderDeviceName} (${packet.senderDeviceId})")
                scope.launch {
                    startSequentialSyncSession(step = 1, isInitialPair = false)
                }
            }

            P2pPacket.TYPE_DEVICE_RENAME -> {
                Log.d(TAG, "Peer device ${packet.senderDeviceId} updated name to: ${packet.senderDeviceName}")
            }

            P2pPacket.TYPE_WEBRTC_SIGNALING -> {
                // Handled at transport coordinator level
            }
        }
    }



    suspend fun seedExistingDatabaseIfNeeded() {
        alignLocalDeviceIdIfNeeded()
        if (p2pPreferences.isInitialDataSeeded()) {
            val pending = syncChangeDao.getPendingChanges()
            val latest = syncChangeDao.getLatestChangeTimestamp()
            if (pending.isNotEmpty() || latest != null) {
                return
            }
        }
        try {
            val myDeviceId = p2pPreferences.getDeviceId()
            val now = System.currentTimeMillis()
            val allChanges = mutableListOf<SyncChangeEntity>()

            // 1. Categories
            val categories = database.categoryDao().getAllCategories().first()
            for (cat in categories) {
                allChanges.add(
                    SyncChangeEntity(
                        entityType = SyncChangeEntity.TYPE_CATEGORY,
                        entityId = cat.name,
                        operation = SyncChangeEntity.OP_INSERT,
                        payloadJson = serializer.toJson(cat),
                        timestamp = now,
                        originDeviceId = myDeviceId,
                        isSynced = false
                    )
                )
            }

            // 2. Subcategories
            val categoryIdToName = categories.associate { it.id to it.name }
            val subcategories = database.subcategoryDao().getAllSubcategories().first()
            for (sub in subcategories) {
                val catName = categoryIdToName[sub.categoryId] ?: ""
                val payload = SubcategorySyncPayload(
                    categoryName = catName,
                    name = sub.name,
                    iconResId = sub.iconResId,
                    iconName = sub.iconName,
                    color = sub.color,
                    isSystem = sub.isSystem,
                    defaultName = sub.defaultName,
                    defaultIconResId = sub.defaultIconResId,
                    defaultIconName = sub.defaultIconName,
                    defaultColor = sub.defaultColor
                )
                allChanges.add(
                    SyncChangeEntity(
                        entityType = SyncChangeEntity.TYPE_SUBCATEGORY,
                        entityId = if (catName.isNotBlank()) "$catName:${sub.name}" else "${sub.categoryId}:${sub.name}",
                        operation = SyncChangeEntity.OP_INSERT,
                        payloadJson = serializer.toJson(payload),
                        timestamp = now,
                        originDeviceId = myDeviceId,
                        isSynced = false
                    )
                )
            }

            // 3. Accounts
            val accounts = database.accountBalanceDao().getAllBalances().first()
            for (acc in accounts) {
                if (isLocalOwner(acc.ownerId)) {
                    val stampedAcc = if (acc.ownerId.isBlank()) acc.copy(ownerId = myDeviceId) else acc
                    allChanges.add(
                        SyncChangeEntity(
                            entityType = SyncChangeEntity.TYPE_ACCOUNT,
                            entityId = acc.id,
                            operation = SyncChangeEntity.OP_INSERT,
                            payloadJson = serializer.toJson(stampedAcc),
                            timestamp = now,
                            originDeviceId = myDeviceId,
                            isSynced = false
                        )
                    )
                }
            }

            // 4. Transactions
            val transactions = database.transactionDao().getAllTransactionsIncludingDeleted().first()
            for (txn in transactions) {
                if (isLocalOwner(txn.ownerId)) {
                    val stampedTxn = if (txn.ownerId.isBlank()) txn.copy(ownerId = myDeviceId) else txn
                    val entityId = stampedTxn.uuid.ifBlank { stampedTxn.id.toString() }
                    allChanges.add(
                        SyncChangeEntity(
                            entityType = SyncChangeEntity.TYPE_TRANSACTION,
                            entityId = entityId,
                            operation = if (stampedTxn.isDeleted) SyncChangeEntity.OP_DELETE else SyncChangeEntity.OP_INSERT,
                            payloadJson = serializer.toJson(stampedTxn),
                            timestamp = now,
                            originDeviceId = myDeviceId,
                            isSynced = false
                        )
                    )
                }
            }

            // 5. Budgets
            val budgets = database.budgetDao().getAllBudgets().first()
            for (b in budgets) {
                if (isLocalOwner(b.ownerId)) {
                    val stampedBudget = if (b.ownerId.isBlank()) b.copy(ownerId = myDeviceId) else b
                    allChanges.add(
                        SyncChangeEntity(
                            entityType = SyncChangeEntity.TYPE_BUDGET,
                            entityId = b.id.toString(),
                            operation = SyncChangeEntity.OP_INSERT,
                            payloadJson = serializer.toJson(stampedBudget),
                            timestamp = now,
                            originDeviceId = myDeviceId,
                            isSynced = false
                        )
                    )
                }
            }

            // 6. Cards
            val cards = database.cardDao().getAllCards().first()
            for (c in cards) {
                if (isLocalOwner(c.ownerId)) {
                    val stampedCard = if (c.ownerId.isBlank()) c.copy(ownerId = myDeviceId) else c
                    allChanges.add(
                        SyncChangeEntity(
                            entityType = SyncChangeEntity.TYPE_CARD,
                            entityId = "${c.bankName}:${c.cardLast4}",
                            operation = SyncChangeEntity.OP_INSERT,
                            payloadJson = serializer.toJson(stampedCard),
                            timestamp = now,
                            originDeviceId = myDeviceId,
                            isSynced = false
                        )
                    )
                }
            }

            // 7. Subscriptions
            val subs = database.subscriptionDao().getAllSubscriptions().first()
            for (s in subs) {
                if (isLocalOwner(s.ownerId)) {
                    val stampedSub = if (s.ownerId.isBlank()) s.copy(ownerId = myDeviceId) else s
                    allChanges.add(
                        SyncChangeEntity(
                            entityType = SyncChangeEntity.TYPE_SUBSCRIPTION,
                            entityId = s.id.toString(),
                            operation = SyncChangeEntity.OP_INSERT,
                            payloadJson = serializer.toJson(stampedSub),
                            timestamp = now,
                            originDeviceId = myDeviceId,
                            isSynced = false
                        )
                    )
                }
            }

            if (allChanges.isNotEmpty()) {
                syncChangeDao.insertChanges(allChanges)
            }
            p2pPreferences.setInitialDataSeeded(true)
            Log.d(TAG, "Seeded existing database into sync_changes: ${allChanges.size} total items in single batch")
        } catch (e: Exception) {
            Log.e(TAG, "Failed seeding existing database", e)
        }
    }

    private fun sendHandshake(isScanner: Boolean = false) {
        scope.launch {
            val myDeviceId = p2pPreferences.getDeviceId()
            val myUserName = try {
                userPreferencesRepository.userPreferences.first().userName
            } catch (e: Exception) {
                null
            }
            val lastSyncTime = p2pPreferences.getLastSyncTimestamp()
            val permissions = mapOf(
                "share_data" to p2pPreferences.isShareMyDataWithPartner(),
                "can_edit" to p2pPreferences.isPartnerCanEditMyData(),
                "can_delete" to p2pPreferences.isPartnerCanDeleteMyData(),
                "can_see_balances" to p2pPreferences.isPartnerCanSeeMyBalances()
            )

            sendPacket(
                P2pPacket(
                    type = P2pPacket.TYPE_HANDSHAKE,
                    senderDeviceId = myDeviceId,
                    senderDeviceName = p2pPreferences.getDeviceName(),
                    senderUserName = myUserName,
                    handshake = HandshakePayload(
                        lastSyncTimestamp = lastSyncTime,
                        schemaVersion = 64,
                        permissions = permissions,
                        partnerUserId = myDeviceId,
                        userName = myUserName,
                        isScanner = isScanner
                    )
                )
            )
        }
    }

    suspend fun sendPacket(packet: P2pPacket): Boolean {
        val key = activeSecretKey ?: return false
        return try {
            val packetToSend = if (packet.senderUserName.isNullOrBlank()) {
                val myUserName = try {
                    userPreferencesRepository.userPreferences.first().userName
                } catch (e: Exception) {
                    null
                }
                packet.copy(senderUserName = myUserName)
            } else {
                packet
            }
            val json = serializer.toJson(packetToSend)
            val encryptedBytes = cryptoEngine.encrypt(json.toByteArray(Charsets.UTF_8), key)
            transportCoordinator.broadcast(encryptedBytes)
        } catch (e: Exception) {
            Log.e(TAG, "Failed encrypting or sending packet", e)
            false
        }
    }

    suspend fun collectPendingQueueItems(): List<SyncQueueItem> {
        if (!p2pPreferences.isShareMyDataWithPartner()) {
            Log.d(TAG, "Skipping collectPendingQueueItems: sharing disabled")
            return emptyList()
        }
        alignLocalDeviceIdIfNeeded()
        seedExistingDatabaseIfNeeded()

        val myDeviceId = p2pPreferences.getDeviceId()
        val canSeeBalances = p2pPreferences.isPartnerCanSeeMyBalances()

        val pendingEntities = syncChangeDao.getPendingChanges()
        val items = mutableListOf<SyncQueueItem>()

        for (change in pendingEntities) {
            // Respect balance privacy permission for ACCOUNT type
            if (change.entityType == SyncChangeEntity.TYPE_ACCOUNT && !canSeeBalances) {
                continue
            }
            items.add(
                SyncQueueItem(
                    changeId = change.id,
                    payload = SyncChangePayload(
                        entityType = change.entityType,
                        entityId = change.entityId,
                        operation = change.operation,
                        payloadJson = change.payloadJson,
                        timestamp = change.timestamp,
                        originDeviceId = change.originDeviceId.ifBlank { myDeviceId },
                        changeId = change.id
                    )
                )
            )
        }
        return items
    }

    suspend fun collectAllLocalPayloads(): List<SyncChangePayload> {
        if (!p2pPreferences.isShareMyDataWithPartner()) {
            Log.d(TAG, "Skipping collectAllLocalPayloads: sharing disabled")
            return emptyList()
        }
        alignLocalDeviceIdIfNeeded()
        val myDeviceId = p2pPreferences.getDeviceId()
        val allPayloads = mutableListOf<SyncChangePayload>()

        // 1. Categories
        val categories = database.categoryDao().getAllCategories().first()
        for (cat in categories) {
            allPayloads.add(
                SyncChangePayload(
                    entityType = SyncChangeEntity.TYPE_CATEGORY,
                    entityId = cat.name,
                    operation = SyncChangeEntity.OP_INSERT,
                    payloadJson = serializer.toJson(cat),
                    timestamp = System.currentTimeMillis(),
                    originDeviceId = myDeviceId
                )
            )
        }

        // 2. Subcategories
        val subcategories = database.subcategoryDao().getAllSubcategories().first()
        for (sub in subcategories) {
            allPayloads.add(
                SyncChangePayload(
                    entityType = SyncChangeEntity.TYPE_SUBCATEGORY,
                    entityId = "${sub.categoryId}:${sub.name}",
                    operation = SyncChangeEntity.OP_INSERT,
                    payloadJson = serializer.toJson(sub),
                    timestamp = System.currentTimeMillis(),
                    originDeviceId = myDeviceId
                )
            )
        }

        // 3. Accounts (if allowed)
        if (p2pPreferences.isPartnerCanSeeMyBalances()) {
            val accounts = database.accountBalanceDao().getAllBalances().first()
            for (acc in accounts) {
                if (isLocalOwner(acc.ownerId)) {
                    val stampedAcc = if (acc.ownerId.isBlank()) acc.copy(ownerId = myDeviceId) else acc
                    allPayloads.add(
                        SyncChangePayload(
                            entityType = SyncChangeEntity.TYPE_ACCOUNT,
                            entityId = acc.id,
                            operation = SyncChangeEntity.OP_INSERT,
                            payloadJson = serializer.toJson(stampedAcc),
                            timestamp = System.currentTimeMillis(),
                            originDeviceId = myDeviceId
                        )
                    )
                }
            }
        }

        // 4. Cards
        val cards = database.cardDao().getAllCards().first()
        for (c in cards) {
            if (isLocalOwner(c.ownerId)) {
                val stampedCard = if (c.ownerId.isBlank()) c.copy(ownerId = myDeviceId) else c
                allPayloads.add(
                    SyncChangePayload(
                        entityType = SyncChangeEntity.TYPE_CARD,
                        entityId = "${c.bankName}:${c.cardLast4}",
                        operation = SyncChangeEntity.OP_INSERT,
                        payloadJson = serializer.toJson(stampedCard),
                        timestamp = System.currentTimeMillis(),
                        originDeviceId = myDeviceId
                    )
                )
            }
        }

        // 5. Transactions
        val transactions = database.transactionDao().getAllTransactionsIncludingDeleted().first()
        for (txn in transactions) {
            if (isLocalOwner(txn.ownerId)) {
                val stampedTxn = if (txn.ownerId.isBlank()) txn.copy(ownerId = myDeviceId) else txn
                val entityId = stampedTxn.uuid.ifBlank { stampedTxn.id.toString() }
                allPayloads.add(
                    SyncChangePayload(
                        entityType = SyncChangeEntity.TYPE_TRANSACTION,
                        entityId = entityId,
                        operation = if (stampedTxn.isDeleted) SyncChangeEntity.OP_DELETE else SyncChangeEntity.OP_INSERT,
                        payloadJson = serializer.toJson(stampedTxn),
                        timestamp = System.currentTimeMillis(),
                        originDeviceId = myDeviceId
                    )
                )
            }
        }

        // 6. Budgets
        val budgets = database.budgetDao().getAllBudgets().first()
        for (b in budgets) {
            if (isLocalOwner(b.ownerId)) {
                val stampedBudget = if (b.ownerId.isBlank()) b.copy(ownerId = myDeviceId) else b
                allPayloads.add(
                    SyncChangePayload(
                        entityType = SyncChangeEntity.TYPE_BUDGET,
                        entityId = b.id.toString(),
                        operation = SyncChangeEntity.OP_INSERT,
                        payloadJson = serializer.toJson(stampedBudget),
                        timestamp = System.currentTimeMillis(),
                        originDeviceId = myDeviceId
                    )
                )
            }
        }

        // 7. Subscriptions
        val subs = database.subscriptionDao().getAllSubscriptions().first()
        for (s in subs) {
            if (isLocalOwner(s.ownerId)) {
                val stampedSub = if (s.ownerId.isBlank()) s.copy(ownerId = myDeviceId) else s
                allPayloads.add(
                    SyncChangePayload(
                        entityType = SyncChangeEntity.TYPE_SUBSCRIPTION,
                        entityId = s.id.toString(),
                        operation = SyncChangeEntity.OP_INSERT,
                        payloadJson = serializer.toJson(stampedSub),
                        timestamp = System.currentTimeMillis(),
                        originDeviceId = myDeviceId
                    )
                )
            }
        }

        return allPayloads
    }

    suspend fun startSequentialSyncSession(step: Int, isInitialPair: Boolean = false) {
        if (activeSecretKey == null) {
            val secretKeyBase64 = p2pPreferences.getSecretKeyBase64() ?: return
            activeSecretKey = cryptoEngine.keyFromBase64(secretKeyBase64)
        }
        currentSyncStep = step
        _isSyncingFlow.value = true
        _syncProgressFlow.value = _syncProgressFlow.value.copy(
            isVisible = true,
            phase = SyncProgressPhase.PREPARING,
            current = 0,
            total = 0,
            partnerDeviceName = p2pPreferences.getPartnerName() ?: "Partner",
            statusMessage = if (step == 1) "Preparing data to send..." else "Preparing Phase 2 data..."
        )
        alignLocalDeviceIdIfNeeded()
        seedExistingDatabaseIfNeeded()

        val myDeviceId = p2pPreferences.getDeviceId()
        val queueItems = collectPendingQueueItems()

        _syncProgressFlow.value = _syncProgressFlow.value.copy(
            phase = SyncProgressPhase.SENDING,
            total = queueItems.size,
            current = 0,
            statusMessage = if (queueItems.isEmpty()) "No local records to send" else "Sending 1 of ${queueItems.size} records to partner..."
        )

        Log.d(TAG, "Starting sequential lockstep sync step $step (${queueItems.size} pending items)")
        val success = sendRecordsLockstep(queueItems, step)
        if (!success) {
            Log.e(TAG, "Sequential sync step $step failed or cancelled")
            if (_syncProgressFlow.value.phase != SyncProgressPhase.ERROR) {
                _syncProgressFlow.value = _syncProgressFlow.value.copy(
                    phase = SyncProgressPhase.ERROR,
                    errorMessage = "Sync interrupted. Connection lost or timed out. Please tap Sync Now to retry."
                )
            }
            currentSyncStep = 0
            currentRole = SyncSessionRole.NONE
            _isSyncingFlow.value = false
            return
        }

        if (step == 1) {
            Log.d(TAG, "Step 1 completed successfully! Waiting for partner to send Phase 2...")
            _syncProgressFlow.value = _syncProgressFlow.value.copy(
                phase = SyncProgressPhase.RECEIVING,
                current = 0,
                total = 0,
                statusMessage = "All local data sent. Waiting for partner's data..."
            )
        } else if (step == 2) {
            Log.d(TAG, "Step 2 completed successfully! Notifying partner that session is done...")
            sendPacket(
                P2pPacket(
                    type = P2pPacket.TYPE_SYNC_SESSION_DONE,
                    senderDeviceId = myDeviceId,
                    senderDeviceName = p2pPreferences.getDeviceName()
                )
            )
            val now = System.currentTimeMillis()
            p2pPreferences.updateLastSyncTimestamp(now)
            val pending = syncChangeDao.getChangesSince(0L).filter { it.timestamp <= now }
            if (pending.isNotEmpty()) {
                syncChangeDao.markAsSynced(pending.map { it.id })
            }
            _syncProgressFlow.value = _syncProgressFlow.value.copy(
                phase = SyncProgressPhase.COMPLETED,
                statusMessage = "Sync complete! ${_syncProgressFlow.value.sentCount} sent, ${_syncProgressFlow.value.receivedCount} received."
            )
            currentSyncStep = 0
            currentRole = SyncSessionRole.NONE
            _isSyncingFlow.value = false
        }
    }

    private suspend fun sendRecordsLockstep(items: List<SyncQueueItem>, step: Int): Boolean {
        val myDeviceId = p2pPreferences.getDeviceId()
        val myDeviceName = p2pPreferences.getDeviceName()
        val sessionId = UUID.randomUUID().toString()

        // 1. Send metadata
        var metaAcked = false
        for (attempt in 1..3) {
            if (!currentCoroutineContext().isActive) return false
            val metaDeferred = CompletableDeferred<Boolean>()
            pendingMetaAck = metaDeferred
            val sent = sendPacket(
                P2pPacket(
                    type = P2pPacket.TYPE_SYNC_START_META,
                    senderDeviceId = myDeviceId,
                    senderDeviceName = myDeviceName,
                    syncMeta = SyncSessionMetaPayload(
                        sessionId = sessionId,
                        totalRecords = items.size,
                        step = step
                    )
                )
            )
            if (!sent) {
                delay(400L)
                continue
            }
            val ack = withTimeoutOrNull(6000L) { metaDeferred.await() }
            if (ack == true) {
                metaAcked = true
                break
            }
            Log.w(TAG, "SYNC_START_META ack timeout (attempt $attempt), retrying...")
            delay(300L)
        }
        if (!metaAcked) {
            Log.e(TAG, "Failed to get SYNC_START_META ACK from partner")
            return false
        }

        // 2. Lockstep transmission: 5 records at a time
        val batchSize = 5
        var currentIndex = 0

        while (currentIndex < items.size) {
            if (!currentCoroutineContext().isActive) return false
            val currentBatchSize = minOf(batchSize, items.size - currentIndex)
            val batch = items.subList(currentIndex, currentIndex + currentBatchSize)
            val batchChanges = batch.map { it.payload.copy(changeId = it.changeId) }
            val batchRowIds = batch.map { it.changeId }
            val targetSeq = currentIndex + batch.size

            var recordAcked = false
            var ackedRecordIds: List<Long> = emptyList()
            for (attempt in 1..3) {
                if (!currentCoroutineContext().isActive) return false
                val recordDeferred = CompletableDeferred<RecordAckResult>()
                pendingRecordAck = recordDeferred
                val sent = sendPacket(
                    P2pPacket(
                        type = P2pPacket.TYPE_SYNC_RECORD,
                        senderDeviceId = myDeviceId,
                        senderDeviceName = myDeviceName,
                        sequenceNumber = targetSeq,
                        totalRecords = items.size,
                        changes = batchChanges,
                        recordIds = batchRowIds,
                        step = step
                    )
                )
                if (!sent) {
                    delay(300L)
                    continue
                }
                val ackResult = withTimeoutOrNull(6000L) { recordDeferred.await() }
                if (ackResult != null && ackResult.sequenceNumber == targetSeq) {
                    recordAcked = true
                    ackedRecordIds = if (ackResult.recordIds.isNotEmpty()) ackResult.recordIds else batchRowIds
                    break
                }
                Log.w(TAG, "Batch ending at $targetSeq ACK timeout/mismatch (attempt $attempt), retrying...")
                delay(200L)
            }

            if (!recordAcked) {
                Log.e(TAG, "Failed to deliver records ending at $targetSeq to partner after 3 retries")
                return false
            }

            // Immediately mark acknowledged records as synced in Room based on IDs returned by partner!
            try {
                syncChangeDao.markAsSynced(ackedRecordIds)
                Log.d(TAG, "Marked ${ackedRecordIds.size} records as synced based on ACK from partner: $ackedRecordIds")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark batch as synced in Room", e)
            }

            _syncProgressFlow.value = _syncProgressFlow.value.copy(
                phase = SyncProgressPhase.SENDING,
                current = targetSeq,
                total = items.size,
                entityType = batchChanges.firstOrNull()?.entityType ?: "",
                sentCount = _syncProgressFlow.value.sentCount + batch.size,
                statusMessage = "Sending ${batchChanges.firstOrNull()?.entityType?.lowercase() ?: "data"} ($targetSeq/${items.size})"
            )
            currentIndex += batch.size
            delay(20L)
        }

        // 3. Send step complete
        var stepAcked = false
        for (attempt in 1..3) {
            if (!currentCoroutineContext().isActive) return false
            val stepDeferred = CompletableDeferred<Boolean>()
            pendingStepCompleteAck = stepDeferred
            val sent = sendPacket(
                P2pPacket(
                    type = P2pPacket.TYPE_SYNC_STEP_COMPLETE,
                    senderDeviceId = myDeviceId,
                    senderDeviceName = myDeviceName,
                    step = step
                )
            )
            if (!sent) {
                delay(400L)
                continue
            }
            val ack = withTimeoutOrNull(6000L) { stepDeferred.await() }
            if (ack == true) {
                stepAcked = true
                break
            }
            Log.w(TAG, "SYNC_STEP_COMPLETE ack timeout (attempt $attempt), retrying...")
            delay(300L)
        }
        return stepAcked
    }

    fun triggerManualSync() {
        if (_isSyncingFlow.value && currentSyncStep != 0) {
            Log.d(TAG, "Sync already in progress; showing progress modal")
            showProgressModal()
            return
        }
        syncSessionJob?.cancel()
        syncSessionJob = scope.launch {
            _isSyncingFlow.value = true
            currentRole = SyncSessionRole.INITIATOR
            _syncProgressFlow.value = SyncProgressState(
                isVisible = true,
                phase = SyncProgressPhase.CONNECTING,
                statusMessage = "Connecting to partner..."
            )
            try {
                alignLocalDeviceIdIfNeeded()
                val alreadyConnected = transportCoordinator.activeConnectionState.value is ConnectionState.Connected
                if (!isEngineRunning) {
                    startSync(force = true)
                } else if (!alreadyConnected) {
                    transportCoordinator.retryAll()
                }

                // Wait for connection with timeout (up to 12 seconds)
                val isConnected = withTimeoutOrNull(12_000L) {
                    if (transportCoordinator.activeConnectionState.value is ConnectionState.Connected) {
                        true
                    } else {
                        transportCoordinator.activeConnectionState.filter { it is ConnectionState.Connected }.first()
                        true
                    }
                } ?: false

                if (!isConnected) {
                    Log.w(TAG, "Manual sync: partner not connected within timeout")
                    _syncProgressFlow.value = _syncProgressFlow.value.copy(
                        phase = SyncProgressPhase.ERROR,
                        errorMessage = "Could not connect to partner. Make sure your partner's device is nearby with the app open."
                    )
                    currentSyncStep = 0
                    currentRole = SyncSessionRole.NONE
                    _isSyncingFlow.value = false
                    return@launch
                }

                Log.d(TAG, "Manual sync: Connected! Initiating Step 1 sync...")
                startSequentialSyncSession(step = 1, isInitialPair = false)
            } catch (e: Exception) {
                if (e is CancellationException) {
                    Log.d(TAG, "Manual sync was cancelled")
                    return@launch
                }
                Log.e(TAG, "Manual sync error", e)
                _syncProgressFlow.value = _syncProgressFlow.value.copy(
                    phase = SyncProgressPhase.ERROR,
                    errorMessage = "Manual sync error: ${e.localizedMessage ?: "Unknown error"}"
                )
                currentSyncStep = 0
                currentRole = SyncSessionRole.NONE
                _isSyncingFlow.value = false
            }
        }
    }

    fun broadcastDeviceName(newName: String) {
        scope.launch {
            val myDeviceId = p2pPreferences.getDeviceId()
            sendPacket(
                P2pPacket(
                    type = P2pPacket.TYPE_DEVICE_RENAME,
                    senderDeviceId = myDeviceId,
                    senderDeviceName = newName
                )
            )
        }
    }

    suspend fun deletePartnerData(partnerDeviceId: String? = null) = database.withTransaction {
        val myDeviceId = p2pPreferences.getDeviceId()
        val partnerId = partnerDeviceId ?: p2pPreferences.getPartnerUserId() ?: ""
        try {
            Log.d(TAG, "Deleting partner data for partnerId=$partnerId, myDeviceId=$myDeviceId")
            database.transactionDao().deletePartnerTransactions(partnerDeviceId = partnerId, myDeviceId = myDeviceId)
            database.accountBalanceDao().deletePartnerBalances(partnerDeviceId = partnerId, myDeviceId = myDeviceId)
            database.cardDao().deletePartnerCards(partnerDeviceId = partnerId, myDeviceId = myDeviceId)
            database.budgetDao().deletePartnerBudgetCategoryLimits(partnerDeviceId = partnerId, myDeviceId = myDeviceId)
            database.budgetDao().deletePartnerBudgets(partnerDeviceId = partnerId, myDeviceId = myDeviceId)
            database.subscriptionDao().deletePartnerSubscriptions(partnerDeviceId = partnerId, myDeviceId = myDeviceId)
            Log.d(TAG, "Successfully deleted partner data")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete partner data", e)
        }
    }

    suspend fun unpair(notifyPartner: Boolean = true) {
        val partnerId = p2pPreferences.getPartnerUserId()
        val isConnected = transportCoordinator.activeConnectionState.value is ConnectionState.Connected
        if (notifyPartner && isConnected) {
            try {
                Log.d(TAG, "Sending UNPAIR packet to partner...")
                sendPacket(
                    P2pPacket(
                        type = P2pPacket.TYPE_UNPAIR,
                        senderDeviceId = p2pPreferences.getDeviceId(),
                        senderDeviceName = p2pPreferences.getDeviceName()
                    )
                )
                delay(500L)
            } catch (e: Exception) {
                Log.e(TAG, "Failed sending unpair packet", e)
            }
        }

        deletePartnerData(partnerId)

        try {
            syncChangeDao.clearAll()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear sync change queue on unpair", e)
        }

        p2pPreferences.unpairPartner()
        stopSync()
    }

    fun stopSync() {
        isEngineRunning = false
        currentRole = SyncSessionRole.NONE
        currentSyncStep = 0
        syncSessionJob?.cancel()
        packetCollectorJob?.cancel()
        liveChangeJob?.cancel()
        connectionStateJob?.cancel()
        transportCoordinator.stop()
        _isSyncingFlow.value = false
        _syncProgressFlow.value = SyncProgressState()
    }
}
