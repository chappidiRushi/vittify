package com.reddy.vittify.domain.usecase

import android.content.ContextWrapper
import com.reddy.vittify.data.database.dao.AccountBalanceDao
import com.reddy.vittify.data.database.dao.SubscriptionDao
import com.reddy.vittify.data.database.dao.TransactionDao
import com.reddy.vittify.data.database.dao.TransactionItemDao
import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.database.entity.TransactionItemEntity
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.vittify.data.repository.AccountBalanceRepository
import com.reddy.vittify.data.repository.SubscriptionRepository
import com.reddy.vittify.data.repository.TransactionRepository
import com.reddy.vittify.domain.service.TransactionBalanceService
import com.reddy.vittify.utils.NanoId
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AddTransactionUseCaseTest {

    private val testBank = "Test Bank"
    private val testLast4 = "1234"
    private val testCurrency = "INR"
    private val baseTime = LocalDateTime.of(2025, 1, 15, 10, 30)
    private var transactionDao: FakeTransactionDao? = null

    @Test
    fun `CREDIT transaction adds to existing outstanding balance`() = runTest {
        val balanceDao = FakeAccountBalanceDao()
        val useCase = createUseCase(balanceDao)

        balanceDao.seedBalance(
            AccountBalanceEntity(
                bankName = testBank,
                accountLast4 = testLast4,
                balance = BigDecimal("2000"),
                timestamp = baseTime.minusDays(1),
                isCreditCard = true,
                creditLimit = BigDecimal("50000")
            )
        )

        useCase.execute(
            amount = BigDecimal("500"),
            merchant = "Ice Cream Shop",
            category = "Food",
            type = TransactionType.CREDIT,
            date = baseTime,
            bankName = testBank,
            accountLast4 = testLast4,
            currency = testCurrency
        )

        val latest = balanceDao.getLatestBalance(testBank, testLast4)
        assertNotNull(latest)
        assertEquals(BigDecimal("2500"), latest!!.balance)
        assertEquals(true, latest.isCreditCard)
        assertEquals(BigDecimal("50000"), latest.creditLimit)
    }

    @Test
    fun `CREDIT transaction creates first balance entry`() = runTest {
        val balanceDao = FakeAccountBalanceDao()
        val useCase = createUseCase(balanceDao)

        useCase.execute(
            amount = BigDecimal("750"),
            merchant = "Coffee Shop",
            category = "Food",
            type = TransactionType.CREDIT,
            date = baseTime,
            bankName = testBank,
            accountLast4 = testLast4,
            currency = testCurrency
        )

        val latest = balanceDao.getLatestBalance(testBank, testLast4)
        assertNotNull(latest)
        assertEquals(BigDecimal("750"), latest!!.balance)
    }

    @Test
    fun `INCOME transaction adds to balance`() = runTest {
        val balanceDao = FakeAccountBalanceDao()
        val useCase = createUseCase(balanceDao)

        balanceDao.seedBalance(
            AccountBalanceEntity(
                bankName = testBank,
                accountLast4 = testLast4,
                balance = BigDecimal("10000"),
                timestamp = baseTime.minusDays(1)
            )
        )

        useCase.execute(
            amount = BigDecimal("5000"),
            merchant = "Salary",
            category = "Income",
            type = TransactionType.INCOME,
            date = baseTime,
            bankName = testBank,
            accountLast4 = testLast4,
            currency = testCurrency
        )

        val latest = balanceDao.getLatestBalance(testBank, testLast4)
        assertNotNull(latest)
        assertEquals(BigDecimal("15000"), latest!!.balance)
    }

    @Test
    fun `EXPENSE transaction subtracts from balance`() = runTest {
        val balanceDao = FakeAccountBalanceDao()
        val useCase = createUseCase(balanceDao)

        balanceDao.seedBalance(
            AccountBalanceEntity(
                bankName = testBank,
                accountLast4 = testLast4,
                balance = BigDecimal("10000"),
                timestamp = baseTime.minusDays(1)
            )
        )

        useCase.execute(
            amount = BigDecimal("3000"),
            merchant = "Rent",
            category = "Housing",
            type = TransactionType.EXPENSE,
            date = baseTime,
            bankName = testBank,
            accountLast4 = testLast4,
            currency = testCurrency
        )

        val latest = balanceDao.getLatestBalance(testBank, testLast4)
        assertNotNull(latest)
        assertEquals(BigDecimal("7000"), latest!!.balance)
    }

    @Test
    fun `INVESTMENT transaction subtracts from balance`() = runTest {
        val balanceDao = FakeAccountBalanceDao()
        val useCase = createUseCase(balanceDao)

        balanceDao.seedBalance(
            AccountBalanceEntity(
                bankName = testBank,
                accountLast4 = testLast4,
                balance = BigDecimal("50000"),
                timestamp = baseTime.minusDays(1)
            )
        )

        useCase.execute(
            amount = BigDecimal("10000"),
            merchant = "Mutual Fund SIP",
            category = "Investment",
            type = TransactionType.INVESTMENT,
            date = baseTime,
            bankName = testBank,
            accountLast4 = testLast4,
            currency = testCurrency
        )

        val latest = balanceDao.getLatestBalance(testBank, testLast4)
        assertNotNull(latest)
        assertEquals(BigDecimal("40000"), latest!!.balance)
    }

    @Test
    fun `CREDIT transaction preserves isCreditCard and creditLimit`() = runTest {
        val balanceDao = FakeAccountBalanceDao()
        val useCase = createUseCase(balanceDao)

        balanceDao.seedBalance(
            AccountBalanceEntity(
                bankName = testBank,
                accountLast4 = testLast4,
                balance = BigDecimal("5000"),
                timestamp = baseTime.minusDays(1),
                isCreditCard = true,
                creditLimit = BigDecimal("100000")
            )
        )

        useCase.execute(
            amount = BigDecimal("1500"),
            merchant = "Electronics",
            category = "Shopping",
            type = TransactionType.CREDIT,
            date = baseTime,
            bankName = testBank,
            accountLast4 = testLast4,
            currency = testCurrency
        )

        val latest = balanceDao.getLatestBalance(testBank, testLast4)
        assertEquals(BigDecimal("6500"), latest!!.balance)
        assertEquals(true, latest.isCreditCard)
        assertEquals(BigDecimal("100000"), latest.creditLimit)
    }

    @Test
    fun `CREDIT multiple transactions accumulate correctly`() = runTest {
        val balanceDao = FakeAccountBalanceDao()
        val useCase = createUseCase(balanceDao)

        balanceDao.seedBalance(
            AccountBalanceEntity(
                bankName = testBank,
                accountLast4 = testLast4,
                balance = BigDecimal.ZERO,
                timestamp = baseTime.minusDays(2),
                isCreditCard = true,
                creditLimit = BigDecimal("50000")
            )
        )

        useCase.execute(
            amount = BigDecimal("200"),
            merchant = "Lunch",
            category = "Food",
            type = TransactionType.CREDIT,
            date = baseTime.minusDays(1),
            bankName = testBank,
            accountLast4 = testLast4,
            currency = testCurrency
        )

        useCase.execute(
            amount = BigDecimal("350"),
            merchant = "Dinner",
            category = "Food",
            type = TransactionType.CREDIT,
            date = baseTime,
            bankName = testBank,
            accountLast4 = testLast4,
            currency = testCurrency
        )

        val latest = balanceDao.getLatestBalance(testBank, testLast4)
        assertEquals(BigDecimal("550"), latest!!.balance)
    }

    @Test
    fun `deleting CREDIT transaction reverses the balance`() = runTest {
        val balanceDao = FakeAccountBalanceDao()
        val (useCase, transactionRepo) = createUseCaseWithRepo(balanceDao)

        balanceDao.seedBalance(
            AccountBalanceEntity(
                bankName = testBank,
                accountLast4 = testLast4,
                balance = BigDecimal("25000"),
                timestamp = baseTime.minusDays(1),
                isCreditCard = true,
                creditLimit = BigDecimal("50000")
            )
        )

        useCase.execute(
            amount = BigDecimal("5000"),
            merchant = "Ice Cream",
            category = "Food",
            type = TransactionType.CREDIT,
            date = baseTime,
            bankName = testBank,
            accountLast4 = testLast4,
            currency = testCurrency
        )

        var latest = balanceDao.getLatestBalance(testBank, testLast4)
        assertEquals(BigDecimal("30000"), latest!!.balance)

        val transaction = transactionDao!!.insertedTransactions.last()
        transactionRepo.deleteTransaction(transaction)

        latest = balanceDao.getLatestBalance(testBank, testLast4)
        assertEquals(BigDecimal("25000"), latest!!.balance)
    }

    @Test
    fun `deleting EXPENSE transaction reverses the balance`() = runTest {
        val balanceDao = FakeAccountBalanceDao()
        val (useCase, transactionRepo) = createUseCaseWithRepo(balanceDao)

        balanceDao.seedBalance(
            AccountBalanceEntity(
                bankName = testBank,
                accountLast4 = testLast4,
                balance = BigDecimal("25000"),
                timestamp = baseTime.minusDays(1)
            )
        )

        useCase.execute(
            amount = BigDecimal("5000"),
            merchant = "Ice Cream",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = baseTime,
            bankName = testBank,
            accountLast4 = testLast4,
            currency = testCurrency
        )

        var latest = balanceDao.getLatestBalance(testBank, testLast4)
        assertEquals(BigDecimal("20000"), latest!!.balance)

        val transaction = transactionDao!!.insertedTransactions.last()
        transactionRepo.deleteTransaction(transaction)

        latest = balanceDao.getLatestBalance(testBank, testLast4)
        assertEquals(BigDecimal("25000"), latest!!.balance)
    }

    @Test
    fun `undoing CREDIT delete re-applies the balance`() = runTest {
        val balanceDao = FakeAccountBalanceDao()
        val (useCase, transactionRepo) = createUseCaseWithRepo(balanceDao)

        balanceDao.seedBalance(
            AccountBalanceEntity(
                bankName = testBank,
                accountLast4 = testLast4,
                balance = BigDecimal("25000"),
                timestamp = baseTime.minusDays(1),
                isCreditCard = true,
                creditLimit = BigDecimal("50000")
            )
        )

        useCase.execute(
            amount = BigDecimal("5000"),
            merchant = "Ice Cream",
            category = "Food",
            type = TransactionType.CREDIT,
            date = baseTime,
            bankName = testBank,
            accountLast4 = testLast4,
            currency = testCurrency
        )

        val transaction = transactionDao!!.insertedTransactions.last()
        transactionRepo.deleteTransaction(transaction)

        var latest = balanceDao.getLatestBalance(testBank, testLast4)
        assertEquals(BigDecimal("25000"), latest!!.balance)

        transactionRepo.undoDeleteTransaction(
            transaction.copy(isDeleted = true)
        )

        latest = balanceDao.getLatestBalance(testBank, testLast4)
        assertEquals(BigDecimal("30000"), latest!!.balance)
    }

    private fun createUseCase(balanceDao: FakeAccountBalanceDao): AddTransactionUseCase {
        return createUseCaseWithRepo(balanceDao).first
    }

    private fun createUseCaseWithRepo(
        balanceDao: FakeAccountBalanceDao
    ): Pair<AddTransactionUseCase, TransactionRepository> {
        val context = ContextWrapper(null)
        val accountBalanceRepo = AccountBalanceRepository(balanceDao, context)
        val balanceService = TransactionBalanceService(accountBalanceRepo)
        val dao = FakeTransactionDao()
        transactionDao = dao
        val itemDao = FakeTransactionItemDao()
        val transactionRepo = TransactionRepository(
            transactionDao = dao,
            transactionItemDao = itemDao,
            transactionBalanceService = dagger.Lazy { balanceService }
        )
        val subscriptionDao = FakeSubscriptionDao()
        val subscriptionRepo = SubscriptionRepository(subscriptionDao)
        return Pair(
            AddTransactionUseCase(transactionRepo, subscriptionRepo, balanceService, accountBalanceRepo),
            transactionRepo
        )
    }

    private class FakeTransactionItemDao : TransactionItemDao {
        override fun getItemsForTransaction(transactionId: Long): Flow<List<TransactionItemEntity>> = flowOf(emptyList())
        override suspend fun getItemsForTransactionSync(transactionId: Long): List<TransactionItemEntity> = emptyList()
        override suspend fun getItemsForTransactions(transactionIds: List<Long>): List<TransactionItemEntity> = emptyList()
        override suspend fun getSplitTransactionIds(transactionIds: List<Long>): List<Long> = emptyList()
        override suspend fun insertItem(item: TransactionItemEntity): Long = 1L
        override suspend fun insertItems(items: List<TransactionItemEntity>) = Unit
        override suspend fun updateItem(item: TransactionItemEntity) = Unit
        override suspend fun deleteItem(item: TransactionItemEntity) = Unit
        override suspend fun deleteItemsForTransaction(transactionId: Long) = Unit
        override fun getAllItems(): Flow<List<TransactionItemEntity>> = flowOf(emptyList())
        override suspend fun getAllItemsSync(): List<TransactionItemEntity> = emptyList()
        override suspend fun deleteAllItems() = Unit
    }

    private class FakeTransactionDao : TransactionDao {
        var insertedTransactions = mutableListOf<TransactionEntity>()
        private var nextId = 1L

        override suspend fun insertTransaction(transaction: TransactionEntity): Long {
            val id = nextId++
            insertedTransactions.add(transaction.copy(id = id))
            return id
        }

        override suspend fun deletePartnerTransactions(partnerDeviceId: String, myDeviceId: String): Int = 0

        override fun getAllTransactions(): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun getAllTransactionsIncludingDeleted(): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun getTransactionCount(): Flow<Int> = flowOf(0)
        override suspend fun getTransactionById(transactionId: Long): TransactionEntity? = null
        override fun getTransactionsBetweenDates(
            startDate: LocalDateTime, endDate: LocalDateTime
        ): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun getTransactionsFiltered(
            startDate: LocalDateTime, endDate: LocalDateTime,
            currency: String, transactionType: TransactionType?
        ): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun getTransactionsByCategory(category: String): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun searchTransactions(searchQuery: String): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override suspend fun searchTransactionsList(searchQuery: String): List<TransactionEntity> = emptyList()
        override suspend fun searchTransactionsForGlobal(query: String, limit: Int): List<TransactionEntity> = emptyList()
        override fun getAllCategories(): Flow<List<String>> = flowOf(emptyList())
        override suspend fun getTopCategoriesByUsage(limit: Int): List<String> = emptyList()
        override fun getAllMerchants(): Flow<List<String>> = flowOf(emptyList())
        override suspend fun getTotalAmountByTypeAndPeriod(
            type: TransactionType, startDate: LocalDateTime, endDate: LocalDateTime
        ): Double? = null
        override suspend fun insertTransactions(transactions: List<TransactionEntity>) = Unit
        override suspend fun updateTransaction(transaction: TransactionEntity) = Unit
        override suspend fun updateDuplicateDismissed(transactionId: Long, isDismissed: Boolean): Int = 0
        override suspend fun updateDuplicatesDismissed(transactionIds: List<Long>, isDismissed: Boolean): Int = 0
        override suspend fun deleteTransaction(transaction: TransactionEntity) = Unit
        override suspend fun deleteTransactionById(transactionId: Long) = Unit
        override suspend fun deleteAllTransactions() = Unit
        override suspend fun deleteSampleTransactions() = Unit
        override suspend fun updateCategoryForMerchant(merchantName: String, newCategory: String) = Unit
        override suspend fun updateCategoryAndSubcategoryForMerchantContains(
            merchantName: String, newCategory: String, newSubcategory: String?
        ) = Unit
        override suspend fun getTransactionCountForMerchant(merchantName: String, excludeId: Long): Int = 0
        override suspend fun getTransactionsByMerchantContains(
            merchantName: String, excludeId: Long
        ): List<TransactionEntity> = emptyList()
        override fun getAllCurrencies(): Flow<List<String>> = flowOf(emptyList())
        override fun getCurrenciesForPeriod(
            startDate: LocalDateTime, endDate: LocalDateTime
        ): Flow<List<String>> = flowOf(emptyList())
        override suspend fun softDeleteTransaction(transactionId: Long, updatedAt: LocalDateTime) = Unit
        override suspend fun softDeleteTransactions(transactionIds: List<Long>, updatedAt: LocalDateTime) = Unit
        override fun getArchivedTransactions(): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun getArchivedTransactionsCount(): Flow<Int> = flowOf(0)
        override suspend fun restoreTransaction(transactionId: Long, updatedAt: LocalDateTime) = Unit
        override suspend fun restoreTransactions(transactionIds: List<Long>, updatedAt: LocalDateTime) = Unit
        override suspend fun deleteAllArchivedTransactions(): Int = 0
        override suspend fun deleteArchivedTransactionsOlderThan(cutoffDate: LocalDateTime): Int = 0
        override suspend fun deleteTransactionsByIds(transactionIds: List<Long>) = Unit
        override suspend fun getTransactionBySmsBody(smsBody: String): TransactionEntity? = null
        override suspend fun getTransactionByUuid(uuid: String): TransactionEntity? = null
        override suspend fun getTransactionsByReferenceAndAmount(
            reference: String,
            amount: java.math.BigDecimal,
            accountId: String?,
            startDate: LocalDateTime,
            endDate: LocalDateTime
        ): List<TransactionEntity> = emptyList()
        override suspend fun getTransactionsBetweenDatesList(
            startDate: LocalDateTime, endDate: LocalDateTime
        ): List<TransactionEntity> = emptyList()
        override fun getTransactionsByAccountId(
            accountId: String, accountLast4: String
        ): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun getTransactionsByAccount(
            bankName: String, accountLast4: String
        ): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun getTransactionsByAccountIdAndDateRange(
            accountId: String, accountLast4: String,
            startDate: LocalDateTime, endDate: LocalDateTime
        ): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun getTransactionsByAccountAndDateRange(
            bankName: String, accountLast4: String,
            startDate: LocalDateTime, endDate: LocalDateTime
        ): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override suspend fun updateAccountIdForTransactions(
            oldAccountId: String, newAccountId: String
        ) = Unit
        override suspend fun updateTransactionsCategory(
            oldCategory: String, newCategory: String, newSubcategory: String?
        ) = Unit
        override suspend fun getTransactionCountByCategory(category: String): Int = 0
        override suspend fun findPotentialDuplicates(
            startDate: LocalDateTime, endDate: LocalDateTime
        ): List<TransactionEntity> = emptyList()
        override suspend fun getTransactionsUpdatedBetween(
            updatedAfter: LocalDateTime, updatedBefore: LocalDateTime, currency: String
        ): List<TransactionEntity> = emptyList()
        override suspend fun getTransactionsBetweenDatesByCurrency(
            startDate: LocalDateTime, endDate: LocalDateTime, currency: String
        ): List<TransactionEntity> = emptyList()
        override fun getRecentlyUsedSubcategories(limit: Int): Flow<List<com.reddy.vittify.data.database.dao.RecentSubcategoryInfo>> = flowOf(emptyList())
        override suspend fun updateOwnerIdForBlank(newOwnerId: String): Int = 0
        override suspend fun updateOwnerIdForTransactions(transactionIds: List<Long>, newOwnerId: String): Int = 0
        override suspend fun updateOwnerIdForOwner(oldOwnerId: String, newOwnerId: String): Int = 0
        override suspend fun alignTransactionOwnersWithAccounts(): Int = 0
        override suspend fun deleteZeroAmountTransactions(): Int = 0
        override suspend fun unlinkTransactionsByIds(transactionIds: List<Long>): Int = 0
        override suspend fun unlinkTransactionsForAccount(accountId: String): Int = 0
        override suspend fun updateMerchantName(transactionId: Long, merchantName: String): Int = 0
    }

    private class FakeSubscriptionDao : SubscriptionDao {
        override suspend fun insertSubscription(subscription: com.reddy.vittify.data.database.entity.SubscriptionEntity): Long = 1
        override suspend fun deletePartnerSubscriptions(partnerDeviceId: String, myDeviceId: String): Int = 0
        override fun getAllSubscriptions(): Flow<List<com.reddy.vittify.data.database.entity.SubscriptionEntity>> = flowOf(emptyList())
        override fun getSubscriptionsByState(state: com.reddy.vittify.data.database.entity.SubscriptionState): Flow<List<com.reddy.vittify.data.database.entity.SubscriptionEntity>> = flowOf(emptyList())
        override fun getActiveSubscriptions(): Flow<List<com.reddy.vittify.data.database.entity.SubscriptionEntity>> = flowOf(emptyList())
        override fun getUpcomingSubscriptions(date: java.time.LocalDate): Flow<List<com.reddy.vittify.data.database.entity.SubscriptionEntity>> = flowOf(emptyList())
        override suspend fun getActiveSubscriptionByMerchant(merchantName: String): com.reddy.vittify.data.database.entity.SubscriptionEntity? = null
        override suspend fun getHiddenSubscriptionByMerchant(merchantName: String): com.reddy.vittify.data.database.entity.SubscriptionEntity? = null
        override suspend fun getSubscriptionByUmn(umn: String): com.reddy.vittify.data.database.entity.SubscriptionEntity? = null
        override suspend fun getSubscriptionByMerchantAmountAndDate(merchantName: String, amount: BigDecimal, paymentDate: java.time.LocalDate): com.reddy.vittify.data.database.entity.SubscriptionEntity? = null
        override suspend fun getSubscriptionByMerchantAndAmount(merchantName: String, amount: BigDecimal): com.reddy.vittify.data.database.entity.SubscriptionEntity? = null
        override suspend fun getSubscriptionById(id: Long): com.reddy.vittify.data.database.entity.SubscriptionEntity? = null
        override suspend fun updateSubscription(subscription: com.reddy.vittify.data.database.entity.SubscriptionEntity) = Unit
        override suspend fun updateSubscriptionState(id: Long, state: com.reddy.vittify.data.database.entity.SubscriptionState) = Unit
        override suspend fun updateNextPaymentDate(id: Long, nextPaymentDate: java.time.LocalDate) = Unit
        override suspend fun updatePaymentStatus(id: Long, nextPaymentDate: java.time.LocalDate, lastPaidDate: java.time.LocalDate?) = Unit
        override suspend fun deleteSubscription(subscription: com.reddy.vittify.data.database.entity.SubscriptionEntity) = Unit
        override suspend fun deleteSubscriptionById(id: Long) = Unit
        override suspend fun getSubscriptionsByStateList(state: com.reddy.vittify.data.database.entity.SubscriptionState): List<com.reddy.vittify.data.database.entity.SubscriptionEntity> = emptyList()
        override suspend fun deleteSampleSubscriptions() = Unit
        override suspend fun deleteAllSubscriptions() = Unit
        override suspend fun updateOwnerIdForBlank(newOwnerId: String): Int = 0
        override suspend fun updateOwnerIdForSubscription(subscriptionId: Long, newOwnerId: String): Int = 0
        override suspend fun updateOwnerIdForOwner(oldOwnerId: String, newOwnerId: String): Int = 0
    }

    private class FakeAccountBalanceDao : AccountBalanceDao() {
        private val accounts = mutableListOf<AccountBalanceEntity>()

        fun seedBalance(entity: AccountBalanceEntity) {
            accounts.removeAll { it.bankName == entity.bankName && it.accountLast4 == entity.accountLast4 }
            accounts.add(entity)
        }

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
