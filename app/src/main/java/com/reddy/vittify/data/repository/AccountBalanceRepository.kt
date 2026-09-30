package com.reddy.vittify.data.repository

import android.content.Context
import com.reddy.vittify.data.database.dao.AccountBalanceDao
import com.reddy.vittify.data.database.dao.CardDao
import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.parser.core.ParsedTransaction
import com.reddy.vittify.utils.IconResolutionUtils
import com.reddy.vittify.utils.NanoId
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountBalanceRepository @Inject constructor(
    private val accountBalanceDao: AccountBalanceDao,
    @ApplicationContext private val context: Context,
    private val cardDao: CardDao? = null,
    private val syncChangeTracker: dagger.Lazy<com.reddy.vittify.data.sync.SyncChangeTracker>? = null,
    private val p2pPreferences: dagger.Lazy<com.reddy.vittify.data.sync.P2pSyncPreferencesRepository>? = null
) {
    suspend fun insertBalance(balance: AccountBalanceEntity): String {
        val owner = if (balance.ownerId.isBlank()) {
            p2pPreferences?.get()?.getDeviceId() ?: ""
        } else balance.ownerId
        val baseEntity = balance.copy(ownerId = owner)
        val balanceWithIconName = if (baseEntity.iconName.isEmpty() && baseEntity.iconResId != 0) {
            baseEntity.copy(iconName = IconResolutionUtils.resIdToName(context, baseEntity.iconResId))
        } else {
            baseEntity
        }
        accountBalanceDao.insertAccount(balanceWithIconName)
        syncChangeTracker?.get()?.trackAccount(balanceWithIconName, com.reddy.vittify.data.sync.model.SyncChangeEntity.OP_INSERT)
        return balanceWithIconName.id
    }
    
    suspend fun getAccountById(id: String): AccountBalanceEntity? {
        return accountBalanceDao.getAccountById(id)
    }

    suspend fun getLatestBalance(bankName: String, accountLast4: String): AccountBalanceEntity? {
        return accountBalanceDao.getLatestBalance(bankName, accountLast4)
    }

    suspend fun getLatestBalanceOnOrBefore(
        bankName: String,
        accountLast4: String,
        timestamp: LocalDateTime
    ): AccountBalanceEntity? {
        return accountBalanceDao.getLatestBalanceOnOrBefore(bankName, accountLast4, timestamp)
    }

    suspend fun resolveAccountLast4(bankName: String, accountLast4: String): String {
        if (accountLast4.isBlank()) {
            return accountLast4
        }

        if (!accountLast4.all { it.isDigit() }) {
            return accountLast4
        }

        if (accountLast4.length >= 4) {
            return accountLast4.takeLast(4)
        }

        val matches = accountBalanceDao.getAccountLast4sEndingWith(bankName, accountLast4)
        return if (matches.size == 1) matches.first() else accountLast4
    }

    suspend fun resolveEntityAccountNumber(
        entity: TransactionEntity,
        parsedTransaction: ParsedTransaction
    ): TransactionEntity {
        val candidateBank = parsedTransaction.bankName?.takeIf { it.isNotBlank() }
        val candidateLast4 = parsedTransaction.accountLast4?.takeIf { it.isNotBlank() }
            ?: if (parsedTransaction.isMobileWallet) "WALLET" else null

        if (candidateLast4 != null) {
            // 1. Direct exact match in account_balances for bank + accountLast4
            if (candidateBank != null) {
                val directMatch = accountBalanceDao.getLatestBalance(candidateBank, candidateLast4)
                if (directMatch != null) {
                    return entity.copy(
                        accountId = directMatch.id,
                        fromAccount = entity.fromAccount ?: candidateLast4,
                        ownerId = if (entity.ownerId.isBlank() && directMatch.ownerId.isNotBlank()) directMatch.ownerId else entity.ownerId
                    )
                }
            }

            // 2. Exact last4 match across all banks
            if (candidateLast4.length == 4 && candidateLast4.all { it.isDigit() }) {
                val accByLast4 = accountBalanceDao.getAccountByLast4(candidateLast4)
                if (accByLast4 != null &&
                    (candidateBank == null || candidateBank.equals(accByLast4.bankName, ignoreCase = true))
                ) {
                    return entity.copy(
                        accountId = accByLast4.id,
                        fromAccount = entity.fromAccount ?: candidateLast4,
                        ownerId = if (entity.ownerId.isBlank() && accByLast4.ownerId.isNotBlank()) accByLast4.ownerId else entity.ownerId
                    )
                }
            }

            // 3. Check cards table if cardDao is available
            if (cardDao != null) {
                val card = if (candidateBank != null) {
                    cardDao.getCard(candidateBank, candidateLast4)
                } else {
                    cardDao.getCardsByLast4(candidateLast4).firstOrNull()
                } ?: cardDao.getCardsByLast4(candidateLast4).firstOrNull()

                if (card != null) {
                    val matchedAccount = if (card.accountLast4 != null) {
                        accountBalanceDao.getLatestBalance(card.bankName, card.accountLast4)
                    } else null
                    val cardOrAccountOwner = matchedAccount?.ownerId?.takeIf { it.isNotBlank() } ?: card.ownerId.takeIf { it.isNotBlank() }
                    return entity.copy(
                        accountId = matchedAccount?.id,
                        fromAccount = entity.fromAccount ?: candidateLast4,
                        ownerId = if (entity.ownerId.isBlank() && cardOrAccountOwner != null) cardOrAccountOwner else entity.ownerId
                    )
                }
            }
        }

        return entity.copy(
            accountId = entity.accountId,
            fromAccount = entity.fromAccount ?: candidateLast4,
            toAccount = entity.toAccount
        )
    }

    fun getLatestBalanceFlow(bankName: String, accountLast4: String): Flow<AccountBalanceEntity?> {
        return accountBalanceDao.getLatestBalanceFlow(bankName, accountLast4)
    }
    
    fun getAllLatestBalances(): Flow<List<AccountBalanceEntity>> {
        return accountBalanceDao.getAllLatestBalances()
    }
    
    fun getTotalBalance(): Flow<BigDecimal?> {
        return accountBalanceDao.getTotalBalance()
    }
    
    fun getBalanceHistory(
        bankName: String,
        accountLast4: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<AccountBalanceEntity>> {
        return accountBalanceDao.getBalanceHistory(bankName, accountLast4, startDate, endDate)
    }
    
    fun getAccountCount(): Flow<Int> {
        return accountBalanceDao.getAccountCount()
    }
    
    suspend fun deleteOldBalances(beforeDate: LocalDateTime): Int {
        return accountBalanceDao.deleteOldBalances(beforeDate)
    }
    
    suspend fun updateBalance(balance: AccountBalanceEntity) {
        accountBalanceDao.updateBalance(balance)
        val owner = balance.ownerId.ifBlank { p2pPreferences?.get()?.getDeviceId() ?: "" }
        val updated = balance.copy(ownerId = owner, updatedAt = LocalDateTime.now())
        accountBalanceDao.updateBalance(updated)
        syncChangeTracker?.get()?.trackAccount(updated, com.reddy.vittify.data.sync.model.SyncChangeEntity.OP_UPDATE)
    }
    
    fun getAllBalances(): Flow<List<AccountBalanceEntity>> {
        return accountBalanceDao.getAllBalances()
    }

    suspend fun deleteBalance(balance: AccountBalanceEntity) {
        accountBalanceDao.deleteBalance(balance)
        syncChangeTracker?.get()?.trackAccount(balance, com.reddy.vittify.data.sync.model.SyncChangeEntity.OP_DELETE)
    }

    suspend fun insertBalanceFromTransaction(
        bankName: String?,
        accountLast4: String?,
        balance: BigDecimal?,
        creditLimit: BigDecimal? = null,
        timestamp: LocalDateTime,
        transactionId: Long?,
        isCreditCard: Boolean = false
    ) {
        if (bankName != null && accountLast4 != null && (balance != null || creditLimit != null)) {
            val latest = getLatestBalance(bankName, accountLast4)
            val balanceEntity = AccountBalanceEntity(
                id = latest?.id ?: NanoId.generate(),
                bankName = bankName,
                accountLast4 = accountLast4,
                balance = balance ?: latest?.balance ?: BigDecimal.ZERO,
                timestamp = timestamp,
                creditLimit = creditLimit ?: latest?.creditLimit,
                isCreditCard = isCreditCard || (latest?.isCreditCard ?: false),
                iconResId = latest?.iconResId ?: 0,
                iconName = latest?.iconName ?: "",
                isWallet = latest?.isWallet ?: false,
                color = latest?.color ?: "#33B5E5"
            )
            insertBalance(balanceEntity)
        }
    }

    suspend fun insertTransactionBalance(
        bankName: String,
        accountLast4: String,
        amount: BigDecimal,
        transactionType: TransactionType,
        explicitBalance: BigDecimal?,
        timestamp: LocalDateTime,
        transactionId: Long?,
        creditLimit: BigDecimal?,
        isCreditCard: Boolean,
        smsSource: String?,
        currency: String,
        fromAccount: String? = null,
        toAccount: String? = null
    ): String {
        val latest = getLatestBalance(bankName, accountLast4)
        val newBalance = explicitBalance ?: latest?.balance ?: BigDecimal.ZERO
        val balanceEntity = AccountBalanceEntity(
            id = latest?.id ?: NanoId.generate(),
            bankName = bankName,
            accountLast4 = accountLast4,
            balance = newBalance,
            timestamp = timestamp,
            creditLimit = creditLimit ?: latest?.creditLimit,
            isCreditCard = isCreditCard || (latest?.isCreditCard ?: false),
            currency = currency,
            iconResId = latest?.iconResId ?: 0,
            iconName = latest?.iconName ?: "",
            isWallet = latest?.isWallet ?: false,
            color = latest?.color ?: "#33B5E5"
        )
        return insertBalance(balanceEntity)
    }

    suspend fun insertBalanceUpdate(
        bankName: String,
        accountLast4: String,
        balance: BigDecimal,
        timestamp: LocalDateTime,
        smsSource: String? = null,
        currency: String = "INR"
    ): String {
        val latest = getLatestBalance(bankName, accountLast4)
        val balanceEntity = AccountBalanceEntity(
            id = latest?.id ?: NanoId.generate(),
            bankName = bankName,
            accountLast4 = accountLast4,
            balance = balance,
            timestamp = timestamp,
            currency = currency,
            iconResId = latest?.iconResId ?: 0,
            iconName = latest?.iconName ?: "",
            isWallet = latest?.isWallet ?: false,
            isCreditCard = latest?.isCreditCard ?: false,
            creditLimit = latest?.creditLimit,
            color = latest?.color ?: "#33B5E5"
        )
        return insertBalance(balanceEntity)
    }
    
    suspend fun getBalanceHistoryForAccount(bankName: String, accountLast4: String): List<AccountBalanceEntity> {
        return accountBalanceDao.getBalanceHistoryForAccount(bankName, accountLast4)
    }
    
    suspend fun deleteBalanceById(id: String) {
        val existing = accountBalanceDao.getBalanceById(id)
        accountBalanceDao.deleteBalanceById(id)
        if (existing != null) {
            syncChangeTracker?.get()?.trackAccount(existing, com.reddy.vittify.data.sync.model.SyncChangeEntity.OP_DELETE)
        }
    }
    
    suspend fun updateBalanceById(id: String, newBalance: BigDecimal) {
        accountBalanceDao.updateBalanceById(id, newBalance)
        val updated = accountBalanceDao.getBalanceById(id)
        if (updated != null) {
            syncChangeTracker?.get()?.trackAccount(updated, com.reddy.vittify.data.sync.model.SyncChangeEntity.OP_UPDATE)
        }
    }
    
    suspend fun getBalanceCountForAccount(bankName: String, accountLast4: String): Int {
        return accountBalanceDao.getBalanceCountForAccount(bankName, accountLast4)
    }

    suspend fun deleteAccount(bankName: String, accountLast4: String): Int {
        val existing = accountBalanceDao.getLatestBalance(bankName, accountLast4)
        val result = accountBalanceDao.deleteAccount(bankName, accountLast4)
        if (existing != null) {
            syncChangeTracker?.get()?.trackAccount(existing, com.reddy.vittify.data.sync.model.SyncChangeEntity.OP_DELETE)
        }
        return result
    }

    suspend fun deleteAccountById(id: String) {
        val existing = accountBalanceDao.getBalanceById(id)
        accountBalanceDao.deleteBalanceById(id)
        if (existing != null) {
            syncChangeTracker?.get()?.trackAccount(existing, com.reddy.vittify.data.sync.model.SyncChangeEntity.OP_DELETE)
        }
    }

    suspend fun updateAccountBankName(oldBankName: String, accountLast4: String, newBankName: String): Int {
        val result = accountBalanceDao.updateAccountBankName(oldBankName, accountLast4, newBankName)
        val updated = accountBalanceDao.getLatestBalance(newBankName, accountLast4)
        if (updated != null) {
            syncChangeTracker?.get()?.trackAccount(updated, com.reddy.vittify.data.sync.model.SyncChangeEntity.OP_UPDATE)
        }
        return result
    }

    suspend fun updateAccountCustomId(bankName: String, accountLast4: String, customId: String?): Int {
        val result = accountBalanceDao.updateAccountCustomId(bankName, accountLast4, customId)
        val updated = accountBalanceDao.getLatestBalance(bankName, accountLast4)
        if (updated != null) {
            syncChangeTracker?.get()?.trackAccount(updated, com.reddy.vittify.data.sync.model.SyncChangeEntity.OP_UPDATE)
        }
        return result
    }

    suspend fun deleteAllBalances() {
        accountBalanceDao.deleteAllBalances()
    }

    suspend fun deleteSampleBalances() {
        accountBalanceDao.deleteSampleBalances()
    }

    suspend fun getAccountByLast4(accountLast4: String): AccountBalanceEntity? {
        return accountBalanceDao.getAccountByLast4(accountLast4)
    }

    suspend fun getBalanceByTransactionId(transactionId: Long): AccountBalanceEntity? {
        return null
    }

    suspend fun getBalancesByTransactionId(transactionId: Long): List<AccountBalanceEntity> {
        return emptyList()
    }

    suspend fun recalculateBalancesAfter(
        bankName: String,
        accountLast4: String,
        timestamp: LocalDateTime,
        startingBalance: BigDecimal
    ) {
        // Balances stored directly on AccountBalanceEntity
    }
}
