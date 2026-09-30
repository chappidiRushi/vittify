package com.reddy.vittify.data.repository

import android.content.ContextWrapper
import com.reddy.vittify.data.database.dao.AccountBalanceDao
import com.reddy.vittify.data.database.dao.RecentSubcategoryInfo
import com.reddy.vittify.data.database.dao.TransactionDao
import com.reddy.vittify.data.database.dao.TransactionItemDao
import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.database.entity.TransactionItemEntity
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.vittify.domain.service.TransactionBalanceService
import dagger.Lazy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class TransactionRepositoryArchivedTest {

    private lateinit var fakeDao: FakeArchivedTransactionDao
    private lateinit var fakeItemDao: FakeTransactionItemDao
    private lateinit var repository: TransactionRepository

    @Before
    fun setup() {
        fakeDao = FakeArchivedTransactionDao()
        fakeItemDao = FakeTransactionItemDao()
        val fakeBalanceDao = FakeAccountBalanceDao()
        val context = ContextWrapper(null)
        val accountBalanceRepo = AccountBalanceRepository(fakeBalanceDao, context)
        val balanceService = TransactionBalanceService(accountBalanceRepo)

        repository = TransactionRepository(
            transactionDao = fakeDao,
            transactionItemDao = fakeItemDao,
            transactionBalanceService = Lazy { balanceService }
        )
    }

    private fun createTestTransaction(id: Long, isDeleted: Boolean = true): TransactionEntity {
        return TransactionEntity(
            id = id,
            amount = BigDecimal("150.00"),
            merchantName = "Test Merchant",
            category = "Food",
            transactionType = TransactionType.EXPENSE,
            dateTime = LocalDateTime.now().minusDays(5),
            isDeleted = isDeleted,
            updatedAt = LocalDateTime.now().minusDays(5)
        )
    }

    @Test
    fun getArchivedTransactionsReturnsDeletedOnly() = runTest {
        val tx1 = createTestTransaction(id = 1, isDeleted = true)
        val tx2 = createTestTransaction(id = 2, isDeleted = false)
        fakeDao.transactions[1L] = tx1
        fakeDao.transactions[2L] = tx2

        val archived = repository.getArchivedTransactions().first()
        assertEquals(1, archived.size)
        assertEquals(1L, archived.first().id)
    }

    @Test
    fun restoreArchivedTransactionUpdatesEntity() = runTest {
        val archivedTx = createTestTransaction(id = 101, isDeleted = true)
        fakeDao.transactions[101L] = archivedTx

        repository.restoreArchivedTransaction(archivedTx)

        val updated = fakeDao.transactions[101L]
        assertTrue(updated != null)
        assertFalse(updated!!.isDeleted)
    }

    @Test
    fun restoreArchivedTransactionsRestoresAllSelected() = runTest {
        val tx1 = createTestTransaction(id = 1, isDeleted = true)
        val tx2 = createTestTransaction(id = 2, isDeleted = true)
        fakeDao.transactions[1L] = tx1
        fakeDao.transactions[2L] = tx2

        repository.restoreArchivedTransactions(listOf(tx1, tx2))

        assertFalse(fakeDao.transactions[1L]!!.isDeleted)
        assertFalse(fakeDao.transactions[2L]!!.isDeleted)
    }

    @Test
    fun permanentlyDeleteArchivedTransactionsDeletesByIds() = runTest {
        val tx1 = createTestTransaction(id = 10, isDeleted = true)
        val tx2 = createTestTransaction(id = 20, isDeleted = true)
        fakeDao.transactions[10L] = tx1
        fakeDao.transactions[20L] = tx2

        repository.permanentlyDeleteArchivedTransactions(listOf(tx1, tx2))

        assertEquals(listOf(10L, 20L), fakeDao.deletedIds)
    }

    @Test
    fun permanentlyDeleteAllArchivedTransactionsCallsDao() = runTest {
        val count = repository.permanentlyDeleteAllArchivedTransactions()
        assertEquals(fakeDao.allDeletedCount, count)
    }

    @Test
    fun autoDeleteArchivedTransactionsCalculatesCutoffDate() = runTest {
        val deletedCount = repository.autoDeleteArchivedTransactions(30)

        assertEquals(4, deletedCount)
        val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(fakeDao.lastCutoffDate!!, LocalDateTime.now())
        assertTrue("Cutoff date should be approximately 30 days before now", daysDiff in 29..31)
    }

    @Test
    fun markDuplicateDismissedUpdatesStatus() = runTest {
        val tx = createTestTransaction(id = 5, isDeleted = false)
        fakeDao.transactions[5L] = tx
        assertFalse(fakeDao.transactions[5L]!!.isDuplicateDismissed)

        repository.markDuplicateDismissed(5L, true)
        assertTrue(fakeDao.transactions[5L]!!.isDuplicateDismissed)

        repository.markDuplicateDismissed(5L, false)
        assertFalse(fakeDao.transactions[5L]!!.isDuplicateDismissed)
    }

    @Test
    fun markDuplicatesDismissedUpdatesBatch() = runTest {
        val tx1 = createTestTransaction(id = 11, isDeleted = false)
        val tx2 = createTestTransaction(id = 12, isDeleted = false)
        fakeDao.transactions[11L] = tx1
        fakeDao.transactions[12L] = tx2

        repository.markDuplicatesDismissed(listOf(11L, 12L), true)
        assertTrue(fakeDao.transactions[11L]!!.isDuplicateDismissed)
        assertTrue(fakeDao.transactions[12L]!!.isDuplicateDismissed)
    }

    private class FakeArchivedTransactionDao : TransactionDao {
        val transactions = mutableMapOf<Long, TransactionEntity>()
        val deletedIds = mutableListOf<Long>()
        var allDeletedCount = 7
        var lastCutoffDate: LocalDateTime? = null

        override suspend fun updateTransaction(transaction: TransactionEntity) {
            transactions[transaction.id] = transaction
        }

        override suspend fun updateDuplicateDismissed(transactionId: Long, isDismissed: Boolean): Int {
            transactions[transactionId]?.let {
                transactions[transactionId] = it.copy(isDuplicateDismissed = isDismissed)
            }
            return 1
        }

        override suspend fun updateDuplicatesDismissed(transactionIds: List<Long>, isDismissed: Boolean): Int {
            transactionIds.forEach { updateDuplicateDismissed(it, isDismissed) }
            return transactionIds.size
        }

        override suspend fun deletePartnerTransactions(partnerDeviceId: String, myDeviceId: String): Int = 0

        override suspend fun deleteTransactionsByIds(transactionIds: List<Long>) {
            deletedIds.addAll(transactionIds)
            transactionIds.forEach { transactions.remove(it) }
        }

        override suspend fun deleteAllArchivedTransactions(): Int {
            val count = allDeletedCount
            transactions.clear()
            return count
        }

        override suspend fun deleteArchivedTransactionsOlderThan(cutoffDate: LocalDateTime): Int {
            lastCutoffDate = cutoffDate
            return 4
        }

        override fun getArchivedTransactions(): Flow<List<TransactionEntity>> =
            flowOf(transactions.values.filter { it.isDeleted })

        override fun getArchivedTransactionsCount(): Flow<Int> =
            flowOf(transactions.values.count { it.isDeleted })

        override suspend fun restoreTransaction(transactionId: Long, updatedAt: LocalDateTime) {
            transactions[transactionId]?.let {
                transactions[transactionId] = it.copy(isDeleted = false, updatedAt = updatedAt)
            }
        }

        override suspend fun restoreTransactions(transactionIds: List<Long>, updatedAt: LocalDateTime) {
            transactionIds.forEach { id -> restoreTransaction(id, updatedAt) }
        }

        override suspend fun softDeleteTransaction(transactionId: Long, updatedAt: LocalDateTime) {
            transactions[transactionId]?.let {
                transactions[transactionId] = it.copy(isDeleted = true, updatedAt = updatedAt)
            }
        }

        override suspend fun softDeleteTransactions(transactionIds: List<Long>, updatedAt: LocalDateTime) {
            transactionIds.forEach { id -> softDeleteTransaction(id, updatedAt) }
        }

        override fun getAllTransactions(): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun getAllTransactionsIncludingDeleted(): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun getTransactionCount(): Flow<Int> = flowOf(0)
        override suspend fun getTransactionById(transactionId: Long): TransactionEntity? = transactions[transactionId]
        override fun getTransactionsBetweenDates(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun getTransactionsFiltered(startDate: LocalDateTime, endDate: LocalDateTime, currency: String, transactionType: TransactionType?): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun getTransactionsByCategory(category: String): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun searchTransactions(searchQuery: String): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override suspend fun searchTransactionsList(searchQuery: String): List<TransactionEntity> = emptyList()
        override suspend fun searchTransactionsForGlobal(query: String, limit: Int): List<TransactionEntity> = emptyList()
        override fun getAllCategories(): Flow<List<String>> = flowOf(emptyList())
        override suspend fun getTopCategoriesByUsage(limit: Int): List<String> = emptyList()
        override fun getAllMerchants(): Flow<List<String>> = flowOf(emptyList())
        override suspend fun getTotalAmountByTypeAndPeriod(type: TransactionType, startDate: LocalDateTime, endDate: LocalDateTime): Double? = null
        override suspend fun insertTransaction(transaction: TransactionEntity): Long = transaction.id
        override suspend fun insertTransactions(transactions: List<TransactionEntity>) = Unit
        override suspend fun deleteTransaction(transaction: TransactionEntity) = Unit
        override suspend fun deleteTransactionById(transactionId: Long) = Unit
        override suspend fun deleteAllTransactions() = Unit
        override suspend fun deleteSampleTransactions() = Unit
        override suspend fun updateCategoryForMerchant(merchantName: String, newCategory: String) = Unit
        override suspend fun updateCategoryAndSubcategoryForMerchantContains(merchantName: String, newCategory: String, newSubcategory: String?) = Unit
        override suspend fun getTransactionCountForMerchant(merchantName: String, excludeId: Long): Int = 0
        override suspend fun getTransactionsByMerchantContains(merchantName: String, excludeId: Long): List<TransactionEntity> = emptyList()
        override fun getAllCurrencies(): Flow<List<String>> = flowOf(emptyList())
        override fun getCurrenciesForPeriod(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<String>> = flowOf(emptyList())
        override suspend fun getTransactionBySmsBody(smsBody: String): TransactionEntity? = null
        override suspend fun getTransactionByUuid(uuid: String): TransactionEntity? = null
        override suspend fun getTransactionsByReferenceAndAmount(reference: String, amount: BigDecimal, accountId: String?, startDate: LocalDateTime, endDate: LocalDateTime): List<TransactionEntity> = emptyList()
        override suspend fun getTransactionsBetweenDatesList(startDate: LocalDateTime, endDate: LocalDateTime): List<TransactionEntity> = emptyList()
        override fun getTransactionsByAccountId(accountId: String, accountLast4: String): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun getTransactionsByAccount(bankName: String, accountLast4: String): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun getTransactionsByAccountIdAndDateRange(accountId: String, accountLast4: String, startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun getTransactionsByAccountAndDateRange(bankName: String, accountLast4: String, startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override suspend fun updateAccountIdForTransactions(oldAccountId: String, newAccountId: String) = Unit
        override suspend fun updateTransactionsCategory(oldCategory: String, newCategory: String, newSubcategory: String?) = Unit
        override suspend fun getTransactionCountByCategory(category: String): Int = 0
        override suspend fun findPotentialDuplicates(startDate: LocalDateTime, endDate: LocalDateTime): List<TransactionEntity> = emptyList()
        override suspend fun getTransactionsUpdatedBetween(updatedAfter: LocalDateTime, updatedBefore: LocalDateTime, currency: String): List<TransactionEntity> = emptyList()
        override suspend fun getTransactionsBetweenDatesByCurrency(startDate: LocalDateTime, endDate: LocalDateTime, currency: String): List<TransactionEntity> = emptyList()
        override fun getRecentlyUsedSubcategories(limit: Int): Flow<List<RecentSubcategoryInfo>> = flowOf(emptyList())
        override suspend fun updateOwnerIdForBlank(newOwnerId: String): Int = 0
        override suspend fun updateOwnerIdForTransactions(transactionIds: List<Long>, newOwnerId: String): Int = 0
        override suspend fun updateOwnerIdForOwner(oldOwnerId: String, newOwnerId: String): Int = 0
        override suspend fun alignTransactionOwnersWithAccounts(): Int = 0
        override suspend fun deleteZeroAmountTransactions(): Int = 0
        override suspend fun unlinkTransactionsByIds(transactionIds: List<Long>): Int = 0
        override suspend fun unlinkTransactionsForAccount(accountId: String): Int = 0
        override suspend fun updateMerchantName(transactionId: Long, merchantName: String): Int = 0
    }

    private class FakeTransactionItemDao : TransactionItemDao {
        override suspend fun insertItem(item: TransactionItemEntity): Long = 1L
        override suspend fun insertItems(items: List<TransactionItemEntity>) = Unit
        override fun getItemsForTransaction(transactionId: Long): Flow<List<TransactionItemEntity>> = flowOf(emptyList())
        override suspend fun getItemsForTransactionSync(transactionId: Long): List<TransactionItemEntity> = emptyList()
        override suspend fun getItemsForTransactions(transactionIds: List<Long>): List<TransactionItemEntity> = emptyList()
        override suspend fun getSplitTransactionIds(transactionIds: List<Long>): List<Long> = emptyList()
        override suspend fun deleteItem(item: TransactionItemEntity) = Unit
        override suspend fun deleteItemsForTransaction(transactionId: Long) = Unit
        override suspend fun updateItem(item: TransactionItemEntity) = Unit
        override fun getAllItems(): Flow<List<TransactionItemEntity>> = flowOf(emptyList())
        override suspend fun getAllItemsSync(): List<TransactionItemEntity> = emptyList()
        override suspend fun deleteAllItems() = Unit
    }

    private class FakeAccountBalanceDao : AccountBalanceDao() {
        private val accounts = mutableListOf<AccountBalanceEntity>()

        override suspend fun insertAccount(account: AccountBalanceEntity): Long {
            accounts.removeAll { it.id == account.id || (it.bankName == account.bankName && it.accountLast4 == account.accountLast4) }
            accounts.add(account)
            return 1L
        }

        override suspend fun insertBalance(balance: AccountBalanceEntity): Long {
            accounts.removeAll { it.id == balance.id || (it.bankName == balance.bankName && it.accountLast4 == balance.accountLast4) }
            accounts.add(balance)
            return 1L
        }

        override suspend fun deletePartnerBalances(partnerDeviceId: String, myDeviceId: String): Int = 0

        override suspend fun getAccountById(id: String): AccountBalanceEntity? =
            accounts.find { it.id == id }

        override suspend fun getBalanceById(id: String): AccountBalanceEntity? =
            accounts.find { it.id == id }

        override suspend fun getLatestBalance(bankName: String, accountLast4: String): AccountBalanceEntity? =
            accounts.find { it.bankName == bankName && it.accountLast4 == accountLast4 }

        override suspend fun getAccountLast4sEndingWith(bankName: String, suffix: String): List<String> =
            accounts.filter { it.bankName == bankName && it.accountLast4.endsWith(suffix) }.map { it.accountLast4 }

        override suspend fun getLatestBalanceOnOrBefore(
            bankName: String,
            accountLast4: String,
            timestamp: LocalDateTime
        ): AccountBalanceEntity? =
            accounts.filter { it.bankName == bankName && it.accountLast4 == accountLast4 && !it.timestamp.isAfter(timestamp) }
                .maxByOrNull { it.timestamp }

        override fun getLatestBalanceFlow(bankName: String, accountLast4: String): Flow<AccountBalanceEntity?> =
            flowOf(accounts.find { it.bankName == bankName && it.accountLast4 == accountLast4 })

        override fun getAllLatestBalances(): Flow<List<AccountBalanceEntity>> = flowOf(accounts.toList())
        override fun getAllBalances(): Flow<List<AccountBalanceEntity>> = flowOf(accounts.toList())
        override suspend fun deleteAllBalances() { accounts.clear() }
        override suspend fun deleteSampleBalances() { accounts.removeAll { it.isSample } }
        override fun getCurrentMonthLatestBalances(): Flow<List<AccountBalanceEntity>> = flowOf(accounts.toList())
        override fun getTotalBalance(): Flow<BigDecimal?> = flowOf(accounts.map { it.balance }.fold(BigDecimal.ZERO, BigDecimal::add))
        override fun getBalanceHistory(bankName: String, accountLast4: String, startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<AccountBalanceEntity>> = flowOf(emptyList())
        override fun getAccountCount(): Flow<Int> = flowOf(accounts.size)
        override suspend fun deleteOldBalances(beforeDate: LocalDateTime): Int = 0

        override suspend fun updateBalance(balance: AccountBalanceEntity) {
            val idx = accounts.indexOfFirst { it.id == balance.id || (it.bankName == balance.bankName && it.accountLast4 == balance.accountLast4) }
            if (idx != -1) {
                accounts[idx] = balance
            } else {
                accounts.add(balance)
            }
        }

        override suspend fun deleteBalance(balance: AccountBalanceEntity) {
            accounts.removeAll { it.id == balance.id }
        }

        override suspend fun getBalanceHistoryForAccount(bankName: String, accountLast4: String): List<AccountBalanceEntity> =
            accounts.filter { it.bankName == bankName && it.accountLast4 == accountLast4 }

        override suspend fun deleteBalanceById(id: String) {
            accounts.removeAll { it.id == id }
        }

        override suspend fun updateBalanceById(id: String, newBalance: BigDecimal) {
            val idx = accounts.indexOfFirst { it.id == id }
            if (idx != -1) {
                accounts[idx] = accounts[idx].copy(balance = newBalance)
            }
        }

        override suspend fun getBalanceCountForAccount(bankName: String, accountLast4: String): Int =
            accounts.count { it.bankName == bankName && it.accountLast4 == accountLast4 }

        override suspend fun deleteAccount(bankName: String, accountLast4: String): Int {
            val initial = accounts.size
            accounts.removeAll { it.bankName == bankName && it.accountLast4 == accountLast4 }
            return initial - accounts.size
        }

        override suspend fun updateAccountBankName(oldBankName: String, accountLast4: String, newBankName: String): Int {
            var updated = 0
            for (i in accounts.indices) {
                if (accounts[i].bankName == oldBankName && accounts[i].accountLast4 == accountLast4) {
                    accounts[i] = accounts[i].copy(bankName = newBankName)
                    updated++
                }
            }
            return updated
        }

        override suspend fun updateAccountCustomId(bankName: String, accountLast4: String, customId: String?): Int = 0

        override suspend fun getAccountByLast4(accountLast4: String): AccountBalanceEntity? =
            accounts.find { it.accountLast4 == accountLast4 || it.accountLast4.endsWith(accountLast4) }

        override suspend fun getEarliestBalance(bankName: String, accountLast4: String): AccountBalanceEntity? =
            accounts.filter { it.bankName == bankName && it.accountLast4 == accountLast4 }.minByOrNull { it.timestamp }

        override suspend fun updateOwnerIdForBlank(newOwnerId: String): Int = 0
        override suspend fun updateOwnerIdForAccount(accountId: String, newOwnerId: String): Int = 0
        override suspend fun updateOwnerIdForOwner(oldOwnerId: String, newOwnerId: String): Int = 0
    }
}
