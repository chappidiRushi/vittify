package com.reddy.vittify.data.sync

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.reddy.vittify.data.sync.model.PairedDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class P2pSyncPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()
    companion object {
        private const val PREFS_NAME = "vittify_p2p_sync_prefs"
        private const val KEY_DEVICE_ID = "p2p_device_id"
        private const val KEY_DEVICE_NAME = "p2p_device_name"
        private const val KEY_CLUSTER_ID = "p2p_cluster_id"
        private const val KEY_SECRET_KEY = "p2p_secret_key"
        private const val KEY_SYNC_ENABLED = "p2p_sync_enabled"
        private const val KEY_LAST_SYNC_TIME = "p2p_last_sync_timestamp"
        private const val KEY_LAST_CONNECTED_TIME = "p2p_last_connected_timestamp"
        private const val KEY_PAIRED_DEVICES = "p2p_paired_devices_json"
        private const val KEY_INITIAL_DATA_SEEDED = "p2p_initial_data_seeded"
        private const val KEY_LOCAL_P2P_ENABLED = "p2p_local_p2p_enabled"
        private const val KEY_REMOTE_P2P_ENABLED = "p2p_remote_p2p_enabled"

        // Couple Tracker Keys
        private const val KEY_COUPLE_TRACKING_ENABLED = "couple_tracking_enabled"
        private const val KEY_PARTNER_USER_ID = "couple_partner_user_id"
        private const val KEY_PARTNER_NAME = "couple_partner_name"
        private const val KEY_SHARE_MY_DATA = "couple_share_my_data"
        private const val KEY_PARTNER_CAN_EDIT = "couple_partner_can_edit"
        private const val KEY_PARTNER_CAN_DELETE = "couple_partner_can_delete"
        private const val KEY_PARTNER_CAN_SEE_BALANCES = "couple_partner_can_see_balances"
        private const val KEY_PARTNER_ALLOWS_SHARE_DATA = "couple_partner_allows_share_data"
        private const val KEY_PARTNER_ALLOWS_EDIT_DATA = "couple_partner_allows_edit_data"
        private const val KEY_PARTNER_ALLOWS_DELETE_DATA = "couple_partner_allows_delete_data"
        private const val KEY_PARTNER_ALLOWS_SEE_BALANCES = "couple_partner_allows_see_balances"
        private const val KEY_ACTIVE_VIEW_MODE = "couple_active_view_mode"
        private const val KEY_HOURLY_SYNC_ENABLED = "couple_hourly_sync_enabled"
        private const val KEY_IS_SCANNER_DEVICE = "p2p_is_scanner_device"
    }

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.w("P2pSyncPrefs", "EncryptedSharedPreferences fallback", e)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    private val _syncEnabledFlow = MutableStateFlow(isSyncEnabled())
    val syncEnabledFlow: StateFlow<Boolean> = _syncEnabledFlow.asStateFlow()

    private val _clusterIdFlow = MutableStateFlow(getClusterId())
    val clusterIdFlow: StateFlow<String?> = _clusterIdFlow.asStateFlow()

    private val _pairedDevicesFlow = MutableStateFlow(getPairedDevices())
    val pairedDevicesFlow: StateFlow<List<PairedDevice>> = _pairedDevicesFlow.asStateFlow()

    private val _lastSyncTimestampFlow = MutableStateFlow(getLastSyncTimestamp())
    val lastSyncTimestampFlow: StateFlow<Long> = _lastSyncTimestampFlow.asStateFlow()

    private val _lastConnectedTimestampFlow = MutableStateFlow(getLastConnectedTimestamp())
    val lastConnectedTimestampFlow: StateFlow<Long> = _lastConnectedTimestampFlow.asStateFlow()

    private val _localP2pEnabledFlow = MutableStateFlow(isLocalP2pEnabled())
    val localP2pEnabledFlow: StateFlow<Boolean> = _localP2pEnabledFlow.asStateFlow()

    private val _remoteP2pEnabledFlow = MutableStateFlow(isRemoteP2pEnabled())
    val remoteP2pEnabledFlow: StateFlow<Boolean> = _remoteP2pEnabledFlow.asStateFlow()

    private val _deviceNameFlow = MutableStateFlow(getDeviceName())
    val deviceNameFlow: StateFlow<String> = _deviceNameFlow.asStateFlow()

    private val _coupleTrackingEnabledFlow = MutableStateFlow(isCoupleTrackingEnabled())
    val coupleTrackingEnabledFlow: StateFlow<Boolean> = _coupleTrackingEnabledFlow.asStateFlow()

    private val _partnerUserIdFlow = MutableStateFlow(getPartnerUserId())
    val partnerUserIdFlow: StateFlow<String?> = _partnerUserIdFlow.asStateFlow()

    private val _partnerNameFlow = MutableStateFlow(getPartnerName())
    val partnerNameFlow: StateFlow<String?> = _partnerNameFlow.asStateFlow()

    private val _shareMyDataFlow = MutableStateFlow(isShareMyDataWithPartner())
    val shareMyDataFlow: StateFlow<Boolean> = _shareMyDataFlow.asStateFlow()

    private val _partnerCanEditFlow = MutableStateFlow(isPartnerCanEditMyData())
    val partnerCanEditFlow: StateFlow<Boolean> = _partnerCanEditFlow.asStateFlow()

    private val _partnerCanDeleteFlow = MutableStateFlow(isPartnerCanDeleteMyData())
    val partnerCanDeleteFlow: StateFlow<Boolean> = _partnerCanDeleteFlow.asStateFlow()

    private val _partnerCanSeeBalancesFlow = MutableStateFlow(isPartnerCanSeeMyBalances())
    val partnerCanSeeBalancesFlow: StateFlow<Boolean> = _partnerCanSeeBalancesFlow.asStateFlow()

    private val _partnerAllowsShareDataFlow = MutableStateFlow(isPartnerAllowsShareData())
    val partnerAllowsShareDataFlow: StateFlow<Boolean> = _partnerAllowsShareDataFlow.asStateFlow()

    private val _partnerAllowsEditDataFlow = MutableStateFlow(isPartnerAllowsEditData())
    val partnerAllowsEditDataFlow: StateFlow<Boolean> = _partnerAllowsEditDataFlow.asStateFlow()

    private val _partnerAllowsDeleteDataFlow = MutableStateFlow(isPartnerAllowsDeleteData())
    val partnerAllowsDeleteDataFlow: StateFlow<Boolean> = _partnerAllowsDeleteDataFlow.asStateFlow()

    private val _partnerAllowsSeeBalancesFlow = MutableStateFlow(isPartnerAllowsSeeBalances())
    val partnerAllowsSeeBalancesFlow: StateFlow<Boolean> = _partnerAllowsSeeBalancesFlow.asStateFlow()

    private val _activeViewModeFlow = MutableStateFlow(getActiveViewMode())
    val activeViewModeFlow: StateFlow<ViewMode> = _activeViewModeFlow.asStateFlow()

    private val _hourlySyncEnabledFlow = MutableStateFlow(isHourlySyncEnabled())
    val hourlySyncEnabledFlow: StateFlow<Boolean> = _hourlySyncEnabledFlow.asStateFlow()

    fun getDeviceId(): String {
        var id = prefs.getString(KEY_DEVICE_ID, null)
        if (id.isNullOrBlank()) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        }
        return id
    }

    fun setDeviceId(id: String) {
        val trimmed = id.trim()
        if (trimmed.isNotBlank()) {
            prefs.edit().putString(KEY_DEVICE_ID, trimmed).apply()
        }
    }

    fun getDeviceName(): String {
        return prefs.getString(KEY_DEVICE_NAME, null) ?: Build.MODEL ?: "Android Device"
    }

    fun setDeviceName(name: String) {
        prefs.edit().putString(KEY_DEVICE_NAME, name.trim()).apply()
        val trimmed = name.trim()
        prefs.edit().putString(KEY_DEVICE_NAME, trimmed).apply()
        _deviceNameFlow.value = trimmed
    }

    fun isLocalP2pEnabled(): Boolean {
        return prefs.getBoolean(KEY_LOCAL_P2P_ENABLED, true)
    }

    fun setLocalP2pEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCAL_P2P_ENABLED, enabled).apply()
        _localP2pEnabledFlow.value = enabled
    }

    fun isRemoteP2pEnabled(): Boolean {
        // Remote P2P disabled for now
        return false
    }

    fun setRemoteP2pEnabled(enabled: Boolean) {
        // Remote P2P disabled for now
        prefs.edit().putBoolean(KEY_REMOTE_P2P_ENABLED, false).apply()
        _remoteP2pEnabledFlow.value = false
    }

    fun getClusterId(): String? {
        return prefs.getString(KEY_CLUSTER_ID, null)
    }

    fun getSecretKeyBase64(): String? {
        return prefs.getString(KEY_SECRET_KEY, null)
    }

    fun isSyncEnabled(): Boolean {
        return prefs.getBoolean(KEY_SYNC_ENABLED, false)
    }

    fun setSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SYNC_ENABLED, enabled).apply()
        _syncEnabledFlow.value = enabled
    }

    fun savePairingConfig(clusterId: String, secretKeyBase64: String) {
        prefs.edit()
            .putString(KEY_CLUSTER_ID, clusterId.trim())
            .putString(KEY_SECRET_KEY, secretKeyBase64.trim())
            .putBoolean(KEY_SYNC_ENABLED, true)
            .apply()
        _clusterIdFlow.value = clusterId.trim()
        _syncEnabledFlow.value = true
    }

    fun clearPairingConfig() {
        prefs.edit()
            .remove(KEY_CLUSTER_ID)
            .remove(KEY_SECRET_KEY)
            .putBoolean(KEY_SYNC_ENABLED, false)
            .remove(KEY_PAIRED_DEVICES)
            .remove(KEY_LAST_SYNC_TIME)
            .remove(KEY_LAST_CONNECTED_TIME)
            .apply()
        _clusterIdFlow.value = null
        _syncEnabledFlow.value = false
        _pairedDevicesFlow.value = emptyList()
        _lastSyncTimestampFlow.value = 0L
        _lastConnectedTimestampFlow.value = 0L
    }

    fun isInitialDataSeeded(): Boolean {
        return prefs.getBoolean(KEY_INITIAL_DATA_SEEDED, false)
    }

    fun setInitialDataSeeded(seeded: Boolean) {
        prefs.edit().putBoolean(KEY_INITIAL_DATA_SEEDED, seeded).apply()
    }

    fun getLastSyncTimestamp(): Long {
        return prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
    }

    fun updateLastSyncTimestamp(timestamp: Long) {
        val newTime = maxOf(getLastSyncTimestamp(), timestamp)
        prefs.edit().putLong(KEY_LAST_SYNC_TIME, newTime).apply()
        _lastSyncTimestampFlow.value = newTime
    }

    fun getLastConnectedTimestamp(): Long {
        return prefs.getLong(KEY_LAST_CONNECTED_TIME, 0L)
    }

    fun updateLastConnectedTimestamp(timestamp: Long) {
        val newTime = maxOf(getLastConnectedTimestamp(), timestamp)
        prefs.edit().putLong(KEY_LAST_CONNECTED_TIME, newTime).apply()
        _lastConnectedTimestampFlow.value = newTime
    }

    fun getPairedDevices(): List<PairedDevice> {
        val json = prefs.getString(KEY_PAIRED_DEVICES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<PairedDevice>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun recordPairedDevice(device: PairedDevice) {
        val current = getPairedDevices().toMutableList()
        val index = current.indexOfFirst { it.deviceId == device.deviceId }
        if (index >= 0) {
            current[index] = device.copy(lastSeen = System.currentTimeMillis())
            val existing = current[index]
            current[index] = device.copy(
                customNickname = existing.customNickname,
                lastSeen = System.currentTimeMillis()
            )
        } else {
            current.add(device.copy(lastSeen = System.currentTimeMillis()))
        }
        val json = gson.toJson(current)
        prefs.edit().putString(KEY_PAIRED_DEVICES, json).apply()
        _pairedDevicesFlow.value = current
    }

    fun renamePairedDevice(deviceId: String, newNickname: String) {
        val current = getPairedDevices().toMutableList()
        val index = current.indexOfFirst { it.deviceId == deviceId }
        if (index >= 0) {
            val trimmed = newNickname.trim()
            current[index] = current[index].copy(customNickname = if (trimmed.isBlank()) null else trimmed)
            val json = gson.toJson(current)
            prefs.edit().putString(KEY_PAIRED_DEVICES, json).apply()
            _pairedDevicesFlow.value = current
        }
    }

    fun removePairedDevice(deviceId: String) {
        val current = getPairedDevices().filterNot { it.deviceId == deviceId }
        val json = gson.toJson(current)
        prefs.edit().putString(KEY_PAIRED_DEVICES, json).apply()
        _pairedDevicesFlow.value = current
    }

    // --- Couple Tracker APIs ---

    fun isCoupleTrackingEnabled(): Boolean = prefs.getBoolean(KEY_COUPLE_TRACKING_ENABLED, false)

    fun setCoupleTrackingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_COUPLE_TRACKING_ENABLED, enabled).apply()
        _coupleTrackingEnabledFlow.value = enabled
        // Also enable sync
        setSyncEnabled(enabled)
    }

    fun getPartnerUserId(): String? = prefs.getString(KEY_PARTNER_USER_ID, null)

    fun setPartnerUserId(userId: String?) {
        prefs.edit().putString(KEY_PARTNER_USER_ID, userId).apply()
        _partnerUserIdFlow.value = userId
    }

    fun getPartnerName(): String? = prefs.getString(KEY_PARTNER_NAME, null)

    fun setPartnerName(name: String?) {
        prefs.edit().putString(KEY_PARTNER_NAME, name).apply()
        _partnerNameFlow.value = name
    }

    fun isShareMyDataWithPartner(): Boolean = prefs.getBoolean(KEY_SHARE_MY_DATA, true)
    fun setShareMyDataWithPartner(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHARE_MY_DATA, enabled).apply()
        _shareMyDataFlow.value = enabled
    }

    fun isPartnerCanEditMyData(): Boolean = prefs.getBoolean(KEY_PARTNER_CAN_EDIT, true)
    fun setPartnerCanEditMyData(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PARTNER_CAN_EDIT, enabled).apply()
        _partnerCanEditFlow.value = enabled
    }

    fun isPartnerCanDeleteMyData(): Boolean = prefs.getBoolean(KEY_PARTNER_CAN_DELETE, true)
    fun setPartnerCanDeleteMyData(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PARTNER_CAN_DELETE, enabled).apply()
        _partnerCanDeleteFlow.value = enabled
    }

    fun isPartnerCanSeeMyBalances(): Boolean = prefs.getBoolean(KEY_PARTNER_CAN_SEE_BALANCES, true)
    fun setPartnerCanSeeMyBalances(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PARTNER_CAN_SEE_BALANCES, enabled).apply()
        _partnerCanSeeBalancesFlow.value = enabled
    }

    fun isPartnerAllowsShareData(): Boolean = prefs.getBoolean(KEY_PARTNER_ALLOWS_SHARE_DATA, true)
    fun isPartnerAllowsEditData(): Boolean = prefs.getBoolean(KEY_PARTNER_ALLOWS_EDIT_DATA, true)
    fun isPartnerAllowsDeleteData(): Boolean = prefs.getBoolean(KEY_PARTNER_ALLOWS_DELETE_DATA, true)
    fun isPartnerAllowsSeeBalances(): Boolean = prefs.getBoolean(KEY_PARTNER_ALLOWS_SEE_BALANCES, true)

    fun setPartnerPermissions(
        shareData: Boolean,
        canEdit: Boolean,
        canDelete: Boolean,
        canSeeBalances: Boolean
    ) {
        prefs.edit()
            .putBoolean(KEY_PARTNER_ALLOWS_SHARE_DATA, shareData)
            .putBoolean(KEY_PARTNER_ALLOWS_EDIT_DATA, canEdit)
            .putBoolean(KEY_PARTNER_ALLOWS_DELETE_DATA, canDelete)
            .putBoolean(KEY_PARTNER_ALLOWS_SEE_BALANCES, canSeeBalances)
            .apply()
        _partnerAllowsShareDataFlow.value = shareData
        _partnerAllowsEditDataFlow.value = canEdit
        _partnerAllowsDeleteDataFlow.value = canDelete
        _partnerAllowsSeeBalancesFlow.value = canSeeBalances
    }

    fun getActiveViewMode(): ViewMode {
        val name = prefs.getString(KEY_ACTIVE_VIEW_MODE, null)
        return try {
            if (name != null) ViewMode.valueOf(name) else ViewMode.COMBINED
        } catch (e: Exception) {
            ViewMode.COMBINED
        }
    }

    fun setActiveViewMode(mode: ViewMode) {
        prefs.edit().putString(KEY_ACTIVE_VIEW_MODE, mode.name).apply()
        _activeViewModeFlow.value = mode
    }

    fun isHourlySyncEnabled(): Boolean = prefs.getBoolean(KEY_HOURLY_SYNC_ENABLED, false)

    fun setHourlySyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HOURLY_SYNC_ENABLED, enabled).apply()
        _hourlySyncEnabledFlow.value = enabled
    }

    fun isScannerDevice(): Boolean = prefs.getBoolean(KEY_IS_SCANNER_DEVICE, false)

    fun setIsScannerDevice(isScanner: Boolean) {
        prefs.edit().putBoolean(KEY_IS_SCANNER_DEVICE, isScanner).apply()
    }

    fun unpairPartner() {
        prefs.edit()
            .remove(KEY_PARTNER_USER_ID)
            .remove(KEY_PARTNER_NAME)
            .remove(KEY_PARTNER_ALLOWS_SHARE_DATA)
            .remove(KEY_PARTNER_ALLOWS_EDIT_DATA)
            .remove(KEY_PARTNER_ALLOWS_DELETE_DATA)
            .remove(KEY_PARTNER_ALLOWS_SEE_BALANCES)
            .remove(KEY_HOURLY_SYNC_ENABLED)
            .remove(KEY_IS_SCANNER_DEVICE)
            .remove(KEY_INITIAL_DATA_SEEDED)
            .putBoolean(KEY_COUPLE_TRACKING_ENABLED, false)
            .putString(KEY_ACTIVE_VIEW_MODE, ViewMode.PERSONAL.name)
            .apply()
        _partnerUserIdFlow.value = null
        _partnerNameFlow.value = null
        _activeViewModeFlow.value = ViewMode.PERSONAL
        _hourlySyncEnabledFlow.value = false
        _coupleTrackingEnabledFlow.value = false
        clearPairingConfig()
    }
}

enum class ViewMode {
    PERSONAL,
    COMBINED,
    PARTNER
}

