package com.reddy.vittify.data.sync.model

import com.reddy.vittify.data.sync.P2pSyncPreferencesRepository
import com.reddy.vittify.data.sync.ViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class CoupleViewState(
    val viewMode: ViewMode = ViewMode.PERSONAL,
    val coupleEnabled: Boolean = false,
    val partnerName: String? = null,
    val partnerDeviceId: String? = null
)

val P2pSyncPreferencesRepository.coupleViewState: Flow<CoupleViewState>
    get() = combine(
        activeViewModeFlow,
        coupleTrackingEnabledFlow,
        partnerNameFlow,
        partnerUserIdFlow
    ) { mode, enabled, partnerName, partnerUserId ->
        CoupleViewState(mode, enabled, partnerName, partnerUserId)
    }

/**
 * Filter a list of items based on active Couple ViewMode.
 *
 * - PERSONAL (Me): only items belonging to the local user (or unowned/legacy items, or != partnerDeviceId)
 * - PARTNER (Partner): only items belonging to the partner (ownerId == partnerDeviceId or ownerId != myDeviceId)
 * - COMBINED (Both): all items
 * - When couple tracking is disabled: all items
 */
fun <T> List<T>.filterByViewMode(
    viewMode: ViewMode,
    coupleTrackingEnabled: Boolean,
    myDeviceId: String,
    partnerDeviceId: String? = null,
    ownerIdSelector: (T) -> String
): List<T> {
    if (!coupleTrackingEnabled) return this
    return when (viewMode) {
        ViewMode.PERSONAL -> filter {
            val owner = ownerIdSelector(it)
            owner.isBlank() || owner == myDeviceId || (!partnerDeviceId.isNullOrBlank() && owner != partnerDeviceId)
        }
        ViewMode.PARTNER -> filter {
            val owner = ownerIdSelector(it)
            if (!partnerDeviceId.isNullOrBlank()) {
                owner == partnerDeviceId
            } else {
                owner.isNotBlank() && owner != myDeviceId
            }
        }
        ViewMode.COMBINED -> this
    }
}
