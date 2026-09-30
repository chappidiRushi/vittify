package com.reddy.vittify.data.sync.engine

import android.util.Log
import androidx.room.withTransaction
import com.reddy.vittify.data.database.VittifyDatabase
import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.data.database.entity.BudgetEntity
import com.reddy.vittify.data.database.entity.CardEntity
import com.reddy.vittify.data.database.entity.CategoryEntity
import com.reddy.vittify.data.database.entity.RuleEntity
import com.reddy.vittify.data.database.entity.SubcategoryEntity
import com.reddy.vittify.data.database.entity.SubscriptionEntity
import com.reddy.vittify.data.database.entity.SubscriptionState
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.sync.SyncJsonSerializer
import com.reddy.vittify.data.sync.model.SubcategorySyncPayload
import com.reddy.vittify.data.sync.model.SyncChangeEntity
import com.reddy.vittify.data.sync.model.SyncChangePayload
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import com.reddy.vittify.data.sync.P2pSyncPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncMergeEngine @Inject constructor(
    private val database: VittifyDatabase,
    private val serializer: SyncJsonSerializer,
    private val p2pPreferences: P2pSyncPreferencesRepository
) {
    companion object {
        private const val TAG = "SyncMergeEngine"
    }

    suspend fun applyRemoteChanges(changes: List<SyncChangePayload>): Int = database.withTransaction {
        var appliedCount = 0

        val existingTxns = database.transactionDao().getAllTransactionsIncludingDeleted().first()
        val existingByUuid = existingTxns.filter { it.uuid.isNotBlank() }.associateBy { it.uuid }.toMutableMap()
        val existingByContent = existingTxns.associateBy {
            "${it.merchantName}_${it.amount}_${it.dateTime}_${it.ownerId}"
        }.toMutableMap()

        for (change in changes) {
            try {
                val applied = when (change.entityType) {
                    SyncChangeEntity.TYPE_TRANSACTION -> mergeTransaction(change, existingByUuid, existingByContent)
                    SyncChangeEntity.TYPE_CATEGORY -> mergeCategory(change)
                    SyncChangeEntity.TYPE_ACCOUNT -> mergeAccount(change)
                    SyncChangeEntity.TYPE_CARD -> mergeCard(change)
                    SyncChangeEntity.TYPE_BUDGET -> mergeBudget(change)
                    SyncChangeEntity.TYPE_SUBSCRIPTION -> mergeSubscription(change)
                    SyncChangeEntity.TYPE_SUBCATEGORY -> mergeSubcategory(change)
                    SyncChangeEntity.TYPE_RULE -> mergeRule(change)
                    else -> false
                }
                if (applied) appliedCount++
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply remote change for ${change.entityType}:${change.entityId}", e)
            }
        }

        appliedCount
    }

    private suspend fun mergeTransaction(
        change: SyncChangePayload,
        existingByUuid: MutableMap<String, TransactionEntity>,
        existingByContent: MutableMap<String, TransactionEntity>
    ): Boolean {
        val remoteTxn = serializer.fromJson(change.payloadJson, TransactionEntity::class.java)
        val txnUuid = remoteTxn.uuid.ifBlank { change.entityId }
        val myDeviceId = p2pPreferences.getDeviceId()
        val effectiveOwnerId = remoteTxn.ownerId.ifBlank { change.originDeviceId }

        val existing = when {
            txnUuid.isNotBlank() && existingByUuid.containsKey(txnUuid) -> existingByUuid[txnUuid]
            else -> {
                val contentKey = "${remoteTxn.merchantName}_${remoteTxn.amount}_${remoteTxn.dateTime}_${effectiveOwnerId}"
                existingByContent[contentKey] ?: existingByContent["${remoteTxn.merchantName}_${remoteTxn.amount}_${remoteTxn.dateTime}_"]
            }
        }
        val isMyRecord = existing != null && (existing.ownerId == myDeviceId || existing.ownerId.isBlank())

        return when (change.operation) {
            SyncChangeEntity.OP_INSERT -> {
                if (existing == null) {
                    val finalUuid = txnUuid.ifBlank { UUID.randomUUID().toString() }
                    val toInsert = remoteTxn.copy(
                        id = 0,
                        uuid = finalUuid,
                        ownerId = effectiveOwnerId
                    )
                    val newId = database.transactionDao().insertTransaction(toInsert)
                    val inserted = toInsert.copy(id = newId)
                    existingByUuid[finalUuid] = inserted
                    existingByContent["${inserted.merchantName}_${inserted.amount}_${inserted.dateTime}_${inserted.ownerId}"] = inserted
                    true
                } else {
                    if (isMyRecord && !p2pPreferences.isPartnerCanEditMyData()) {
                        Log.d(TAG, "Rejecting remote update on local transaction: partnerCanEditMyData disabled")
                        return false
                    }
                    // Conflict: Last-Write-Wins
                    val remoteTime = toEpochMillis(remoteTxn.updatedAt)
                    val localTime = toEpochMillis(existing.updatedAt)
                    if (remoteTime > localTime) {
                        val toUpdate = remoteTxn.copy(
                            id = existing.id,
                            uuid = existing.uuid,
                            ownerId = existing.ownerId.ifBlank { effectiveOwnerId }
                        )
                        database.transactionDao().updateTransaction(toUpdate)
                        if (toUpdate.uuid.isNotBlank()) {
                            existingByUuid[toUpdate.uuid] = toUpdate
                        }
                        existingByContent["${toUpdate.merchantName}_${toUpdate.amount}_${toUpdate.dateTime}_${toUpdate.ownerId}"] = toUpdate
                        true
                    } else false
                }
            }
            SyncChangeEntity.OP_UPDATE -> {
                if (existing != null) {
                    if (isMyRecord && !p2pPreferences.isPartnerCanEditMyData()) {
                        Log.d(TAG, "Rejecting remote update on local transaction: partnerCanEditMyData disabled")
                        return false
                    }
                    val remoteTime = toEpochMillis(remoteTxn.updatedAt)
                    val localTime = toEpochMillis(existing.updatedAt)
                    if (remoteTime > localTime) {
                        val toUpdate = remoteTxn.copy(
                            id = existing.id,
                            uuid = existing.uuid,
                            ownerId = existing.ownerId.ifBlank { effectiveOwnerId }
                        )
                        database.transactionDao().updateTransaction(toUpdate)
                        if (toUpdate.uuid.isNotBlank()) {
                            existingByUuid[toUpdate.uuid] = toUpdate
                        }
                        existingByContent["${toUpdate.merchantName}_${toUpdate.amount}_${toUpdate.dateTime}_${toUpdate.ownerId}"] = toUpdate
                        true
                    } else false
                } else {
                    val finalUuid = txnUuid.ifBlank { UUID.randomUUID().toString() }
                    val toInsert = remoteTxn.copy(
                        id = 0,
                        uuid = finalUuid,
                        ownerId = effectiveOwnerId
                    )
                    val newId = database.transactionDao().insertTransaction(toInsert)
                    val inserted = toInsert.copy(id = newId)
                    existingByUuid[finalUuid] = inserted
                    existingByContent["${inserted.merchantName}_${inserted.amount}_${inserted.dateTime}_${inserted.ownerId}"] = inserted
                    true
                }
            }
            SyncChangeEntity.OP_DELETE -> {
                if (existing != null && !existing.isDeleted) {
                    if (isMyRecord && !p2pPreferences.isPartnerCanDeleteMyData()) {
                        Log.d(TAG, "Rejecting remote delete on local transaction: partnerCanDeleteMyData disabled")
                        return false
                    }
                    val toDelete = existing.copy(isDeleted = true, updatedAt = LocalDateTime.now())
                    database.transactionDao().updateTransaction(toDelete)
                    if (toDelete.uuid.isNotBlank()) {
                        existingByUuid[toDelete.uuid] = toDelete
                    }
                    existingByContent["${toDelete.merchantName}_${toDelete.amount}_${toDelete.dateTime}_${toDelete.ownerId}"] = toDelete
                    true
                } else false
            }
            else -> false
        }
    }

    private suspend fun mergeCategory(change: SyncChangePayload): Boolean {
        val remoteCat = serializer.fromJson(change.payloadJson, CategoryEntity::class.java)
        val existing = database.categoryDao().getCategoryByName(remoteCat.name)

        return when (change.operation) {
            SyncChangeEntity.OP_INSERT -> {
                if (existing == null) {
                    database.categoryDao().insertCategory(remoteCat.copy(id = 0))
                    true
                } else false
            }
            SyncChangeEntity.OP_UPDATE -> {
                if (existing != null) {
                    val remoteTime = toEpochMillis(remoteCat.updatedAt)
                    val localTime = toEpochMillis(existing.updatedAt)
                    if (remoteTime > localTime) {
                        database.categoryDao().updateCategory(remoteCat.copy(id = existing.id))
                        true
                    } else false
                } else {
                    database.categoryDao().insertCategory(remoteCat.copy(id = 0))
                    true
                }
            }
            SyncChangeEntity.OP_DELETE -> {
                if (existing != null && !existing.isSystem) {
                    database.categoryDao().deleteCategory(existing.id)
                    true
                } else false
            }
            else -> false
        }
    }

    private suspend fun mergeAccount(change: SyncChangePayload): Boolean {
        val remoteAccount = serializer.fromJson(change.payloadJson, AccountBalanceEntity::class.java)
        val effectiveOwnerId = remoteAccount.ownerId.ifBlank { change.originDeviceId }
        val existing = database.accountBalanceDao().getBalanceById(remoteAccount.id)
            ?: database.accountBalanceDao().getLatestBalance(remoteAccount.bankName, remoteAccount.accountLast4)
        val accountWithOwner = remoteAccount.copy(
            id = existing?.id ?: remoteAccount.id,
            ownerId = existing?.ownerId?.ifBlank { effectiveOwnerId } ?: effectiveOwnerId
        )

        return when (change.operation) {
            SyncChangeEntity.OP_INSERT -> {
                if (existing == null) {
                    database.accountBalanceDao().insertBalance(accountWithOwner)
                    true
                } else {
                    val remoteTime = toEpochMillis(remoteAccount.updatedAt)
                    val localTime = toEpochMillis(existing.updatedAt)
                    if (remoteTime > localTime) {
                        database.accountBalanceDao().updateBalance(accountWithOwner)
                        true
                    } else false
                }
            }
            SyncChangeEntity.OP_UPDATE -> {
                if (existing != null) {
                    val remoteTime = toEpochMillis(remoteAccount.updatedAt)
                    val localTime = toEpochMillis(existing.updatedAt)
                    if (remoteTime > localTime) {
                        database.accountBalanceDao().updateBalance(accountWithOwner)
                        true
                    } else false
                } else {
                    database.accountBalanceDao().insertBalance(accountWithOwner)
                    true
                }
            }
            SyncChangeEntity.OP_DELETE -> {
                if (existing != null) {
                    database.accountBalanceDao().deleteBalance(existing)
                    true
                } else false
            }
            else -> false
        }
    }

    private suspend fun mergeBudget(change: SyncChangePayload): Boolean {
        val remoteBudget = serializer.fromJson(change.payloadJson, BudgetEntity::class.java)
        val effectiveOwnerId = remoteBudget.ownerId.ifBlank { change.originDeviceId }
        val existingList = database.budgetDao().getAllBudgets().first()
        val existing = existingList.firstOrNull {
            (it.id == remoteBudget.id && it.ownerId == effectiveOwnerId) ||
            (it.name == remoteBudget.name && it.year == remoteBudget.year && it.month == remoteBudget.month && it.ownerId == effectiveOwnerId)
        }

        return when (change.operation) {
            SyncChangeEntity.OP_INSERT -> {
                if (existing == null) {
                    database.budgetDao().insertBudget(remoteBudget.copy(id = 0, ownerId = effectiveOwnerId))
                    true
                } else false
            }
            SyncChangeEntity.OP_UPDATE -> {
                if (existing != null) {
                    database.budgetDao().updateBudget(remoteBudget.copy(id = existing.id, ownerId = existing.ownerId.ifBlank { effectiveOwnerId }))
                    true
                } else {
                    database.budgetDao().insertBudget(remoteBudget.copy(id = 0, ownerId = effectiveOwnerId))
                    true
                }
            }
            SyncChangeEntity.OP_DELETE -> {
                if (existing != null) {
                    database.budgetDao().deleteBudget(existing.id)
                    database.budgetDao().deleteCategoryLimitsForBudget(existing.id)
                    true
                } else false
            }
            else -> false
        }
    }

    private suspend fun mergeSubcategory(change: SyncChangePayload): Boolean {
        var catName = ""
        var subName = ""
        var iconRes = 0
        var iconN = ""
        var col = "#757575"
        var isSys = false
        var defName: String? = null
        var defIconRes: Int? = null
        var defIconName: String? = null
        var defColor: String? = null

        try {
            val payload = serializer.fromJson(change.payloadJson, SubcategorySyncPayload::class.java)
            if (payload != null && payload.categoryName.isNotBlank() && payload.name.isNotBlank()) {
                catName = payload.categoryName
                subName = payload.name
                iconRes = payload.iconResId
                iconN = payload.iconName
                col = payload.color
                isSys = payload.isSystem
                defName = payload.defaultName
                defIconRes = payload.defaultIconResId
                defIconName = payload.defaultIconName
                defColor = payload.defaultColor
            }
        } catch (_: Exception) {}

        if (catName.isBlank() || subName.isBlank()) {
            try {
                val remoteSub = serializer.fromJson(change.payloadJson, SubcategoryEntity::class.java)
                subName = remoteSub.name
                iconRes = remoteSub.iconResId
                iconN = remoteSub.iconName
                col = remoteSub.color
                isSys = remoteSub.isSystem
                defName = remoteSub.defaultName
                defIconRes = remoteSub.defaultIconResId
                defIconName = remoteSub.defaultIconName
                defColor = remoteSub.defaultColor
                catName = database.categoryDao().getCategoryById(remoteSub.categoryId)?.name ?: ""
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse subcategory payload", e)
                return false
            }
        }

        if (catName.isBlank() || subName.isBlank()) {
            Log.w(TAG, "Cannot merge subcategory without valid categoryName and subcategory name")
            return false
        }

        val parentCategory = database.categoryDao().getCategoryByName(catName)
            ?: run {
                Log.w(TAG, "Parent category '$catName' not found locally for subcategory '$subName'")
                return false
            }

        val existingList = database.subcategoryDao().getSubcategoriesByCategoryId(parentCategory.id).first()
        val existing = existingList.firstOrNull { it.name.equals(subName, ignoreCase = true) }

        return when (change.operation) {
            SyncChangeEntity.OP_INSERT -> {
                if (existing == null) {
                    database.subcategoryDao().insertSubcategory(
                        SubcategoryEntity(
                            id = 0,
                            categoryId = parentCategory.id,
                            name = subName,
                            iconResId = iconRes,
                            iconName = iconN,
                            color = col,
                            isSystem = isSys,
                            defaultName = defName,
                            defaultIconResId = defIconRes,
                            defaultIconName = defIconName,
                            defaultColor = defColor
                        )
                    )
                    true
                } else false
            }
            SyncChangeEntity.OP_UPDATE -> {
                if (existing != null) {
                    database.subcategoryDao().updateSubcategory(
                        existing.copy(
                            name = subName,
                            iconResId = iconRes,
                            iconName = iconN,
                            color = col,
                            isSystem = isSys,
                            defaultName = defName,
                            defaultIconResId = defIconRes,
                            defaultIconName = defIconName,
                            defaultColor = defColor
                        )
                    )
                    true
                } else {
                    database.subcategoryDao().insertSubcategory(
                        SubcategoryEntity(
                            id = 0,
                            categoryId = parentCategory.id,
                            name = subName,
                            iconResId = iconRes,
                            iconName = iconN,
                            color = col,
                            isSystem = isSys,
                            defaultName = defName,
                            defaultIconResId = defIconRes,
                            defaultIconName = defIconName,
                            defaultColor = defColor
                        )
                    )
                    true
                }
            }
            SyncChangeEntity.OP_DELETE -> {
                if (existing != null) {
                    database.subcategoryDao().deleteSubcategory(existing)
                    true
                } else false
            }
            else -> false
        }
    }

    private suspend fun mergeRule(change: SyncChangePayload): Boolean {
        val remoteRule = serializer.fromJson(change.payloadJson, RuleEntity::class.java)
        val existingList = database.ruleDao().getAllRules().first()
        val existing = existingList.firstOrNull { it.id == remoteRule.id }

        return when (change.operation) {
            SyncChangeEntity.OP_INSERT -> {
                if (existing == null) {
                    database.ruleDao().insertRule(remoteRule)
                    true
                } else false
            }
            SyncChangeEntity.OP_UPDATE -> {
                if (existing != null) {
                    database.ruleDao().updateRule(remoteRule.copy(id = existing.id))
                    true
                } else false
            }
            SyncChangeEntity.OP_DELETE -> {
                if (existing != null) {
                    database.ruleDao().deleteRule(existing)
                    true
                } else false
            }
            else -> false
        }
    }

    private suspend fun mergeCard(change: SyncChangePayload): Boolean {
        val remoteCard = serializer.fromJson(change.payloadJson, CardEntity::class.java)
        val effectiveOwnerId = remoteCard.ownerId.ifBlank { change.originDeviceId }
        val existing = database.cardDao().getCard(remoteCard.bankName, remoteCard.cardLast4)

        return when (change.operation) {
            SyncChangeEntity.OP_INSERT, SyncChangeEntity.OP_UPDATE -> {
                if (existing == null) {
                    database.cardDao().insertCard(
                        remoteCard.copy(id = 0, ownerId = effectiveOwnerId)
                    )
                    true
                } else {
                    val remoteTime = toEpochMillis(remoteCard.updatedAt)
                    val localTime = toEpochMillis(existing.updatedAt)
                    if (remoteTime >= localTime) {
                        database.cardDao().updateCard(
                            remoteCard.copy(id = existing.id, ownerId = existing.ownerId.ifBlank { effectiveOwnerId })
                        )
                        true
                    } else false
                }
            }
            SyncChangeEntity.OP_DELETE -> {
                if (existing != null) {
                    database.cardDao().deleteCard(existing)
                    true
                } else false
            }
            else -> false
        }
    }

    private suspend fun mergeSubscription(change: SyncChangePayload): Boolean {
        val remoteSub = serializer.fromJson(change.payloadJson, SubscriptionEntity::class.java)
        val effectiveOwnerId = remoteSub.ownerId.ifBlank { change.originDeviceId }
        val existingList = database.subscriptionDao().getAllSubscriptions().first()
        val existing = existingList.firstOrNull {
            it.merchantName.equals(remoteSub.merchantName, ignoreCase = true) &&
            it.amount.compareTo(remoteSub.amount) == 0
        }

        return when (change.operation) {
            SyncChangeEntity.OP_INSERT, SyncChangeEntity.OP_UPDATE -> {
                if (existing == null) {
                    database.subscriptionDao().insertSubscription(
                        remoteSub.copy(id = 0, ownerId = effectiveOwnerId)
                    )
                    true
                } else {
                    val remoteTime = toEpochMillis(remoteSub.updatedAt)
                    val localTime = toEpochMillis(existing.updatedAt)
                    if (remoteTime >= localTime) {
                        database.subscriptionDao().updateSubscription(
                            remoteSub.copy(id = existing.id, ownerId = existing.ownerId.ifBlank { effectiveOwnerId })
                        )
                        true
                    } else false
                }
            }
            SyncChangeEntity.OP_DELETE -> {
                if (existing != null) {
                    database.subscriptionDao().updateSubscriptionState(existing.id, SubscriptionState.HIDDEN)
                    true
                } else false
            }
            else -> false
        }
    }

    private fun toEpochMillis(dateTime: LocalDateTime): Long {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
