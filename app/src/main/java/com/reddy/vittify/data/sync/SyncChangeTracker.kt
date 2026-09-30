package com.reddy.vittify.data.sync

import android.util.Log
import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.data.database.entity.BudgetEntity
import com.reddy.vittify.data.database.entity.CardEntity
import com.reddy.vittify.data.database.entity.CategoryEntity
import com.reddy.vittify.data.database.entity.RuleEntity
import com.reddy.vittify.data.database.entity.SubcategoryEntity
import com.reddy.vittify.data.database.entity.SubscriptionEntity
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.sync.dao.SyncChangeDao
import com.reddy.vittify.data.sync.model.SubcategorySyncPayload
import com.reddy.vittify.data.sync.model.SyncChangeEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncChangeTracker @Inject constructor(
    private val syncChangeDao: SyncChangeDao,
    private val p2pPreferences: P2pSyncPreferencesRepository,
    private val serializer: SyncJsonSerializer
) {
    companion object {
        private const val TAG = "SyncChangeTracker"
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private val _liveChangesFlow = MutableSharedFlow<SyncChangeEntity>(extraBufferCapacity = 64)
    val liveChangesFlow: SharedFlow<SyncChangeEntity> = _liveChangesFlow.asSharedFlow()

    fun trackTransaction(entity: TransactionEntity, operation: String) {
        val entityId = if (entity.uuid.isNotBlank()) entity.uuid else entity.id.toString()
        trackChange(SyncChangeEntity.TYPE_TRANSACTION, entityId, operation, entity)
    }

    fun trackCategory(entity: CategoryEntity, operation: String) {
        trackChange(SyncChangeEntity.TYPE_CATEGORY, entity.name, operation, entity)
    }

    fun trackAccount(entity: AccountBalanceEntity, operation: String) {
        trackChange(SyncChangeEntity.TYPE_ACCOUNT, entity.id, operation, entity)
    }

    fun trackBudget(entity: BudgetEntity, operation: String) {
        trackChange(SyncChangeEntity.TYPE_BUDGET, entity.id.toString(), operation, entity)
    }

    fun trackSubcategory(payload: SubcategorySyncPayload, operation: String) {
        trackChange(SyncChangeEntity.TYPE_SUBCATEGORY, "${payload.categoryName}:${payload.name}", operation, payload)
    }

    fun trackSubcategory(entity: SubcategoryEntity, categoryName: String, operation: String) {
        val payload = SubcategorySyncPayload(
            categoryName = categoryName,
            name = entity.name,
            iconResId = entity.iconResId,
            iconName = entity.iconName,
            color = entity.color,
            isSystem = entity.isSystem,
            defaultName = entity.defaultName,
            defaultIconResId = entity.defaultIconResId,
            defaultIconName = entity.defaultIconName,
            defaultColor = entity.defaultColor
        )
        trackSubcategory(payload, operation)
    }

    fun trackSubcategory(entity: SubcategoryEntity, operation: String) {
        trackChange(SyncChangeEntity.TYPE_SUBCATEGORY, "${entity.categoryId}:${entity.name}", operation, entity)
    }

    fun trackRule(entity: RuleEntity, operation: String) {
        trackChange(SyncChangeEntity.TYPE_RULE, entity.id.toString(), operation, entity)
    }

    fun trackCard(entity: CardEntity, operation: String) {
        val entityId = "${entity.bankName}:${entity.cardLast4}"
        trackChange(SyncChangeEntity.TYPE_CARD, entityId, operation, entity)
    }

    fun trackSubscription(entity: SubscriptionEntity, operation: String) {
        val entityId = entity.id.toString()
        trackChange(SyncChangeEntity.TYPE_SUBSCRIPTION, entityId, operation, entity)
    }

    private fun trackChange(entityType: String, entityId: String, operation: String, entityObject: Any) {
        if (!p2pPreferences.isSyncEnabled()) return

        scope.launch {
            try {
                val originDeviceId = p2pPreferences.getDeviceId()
                val payloadJson = serializer.toJson(entityObject)
                val change = SyncChangeEntity(
                    entityType = entityType,
                    entityId = entityId,
                    operation = operation,
                    payloadJson = payloadJson,
                    timestamp = System.currentTimeMillis(),
                    originDeviceId = originDeviceId,
                    isSynced = false
                )
                val rowId = syncChangeDao.insertChange(change)
                val savedChange = change.copy(id = rowId)
                _liveChangesFlow.tryEmit(savedChange)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to track $operation on $entityType:$entityId", e)
            }
        }
    }
}

