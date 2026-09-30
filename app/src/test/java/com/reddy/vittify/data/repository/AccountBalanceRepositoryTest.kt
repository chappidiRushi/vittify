package com.reddy.vittify.data.repository

import android.content.ContextWrapper
import com.reddy.vittify.data.database.dao.AccountBalanceDao
import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.data.database.entity.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AccountBalanceRepositoryTest {

    @Test
    fun insertTransactionBalanceWithExplicitBalanceUpdatesBalance() = runTest {
        val transactionTime = LocalDateTime.of(2026, 5, 23, 20, 35)
        val dao = FakeAccountBalanceDao(
            latestBalances = mutableMapOf(
                accountKey("Test Bank", "1234") to balance(
                    id = "1",
                    bankName = "Test Bank",
                    accountLast4 = "1234",
                    balance = BigDecimal("100.00"),
                    timestamp = transactionTime.minusMinutes(5)
                )
            )
        )
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        repository.insertTransactionBalance(
            bankName = "Test Bank",
            accountLast4 = "1234",
            amount = BigDecimal("50.00"),
            transactionType = TransactionType.INCOME,
            explicitBalance = BigDecimal("150.00"),
            timestamp = transactionTime,
            transactionId = 11,
            creditLimit = null,
            isCreditCard = false,
            smsSource = "credited alert",
            currency = "INR"
        )

        assertEquals(BigDecimal("150.00"), dao.insertedBalances.single().balance)
    }

    @Test
    fun insertTransactionBalanceWithoutExplicitBalancePreservesLatestBalance() = runTest {
        val transactionTime = LocalDateTime.of(2026, 5, 23, 20, 35)
        val dao = FakeAccountBalanceDao(
            latestBalances = mutableMapOf(
                accountKey("Test Bank", "1234") to balance(
                    id = "1",
                    bankName = "Test Bank",
                    accountLast4 = "1234",
                    balance = BigDecimal("100.00"),
                    timestamp = transactionTime.minusMinutes(5)
                )
            )
        )
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        repository.insertTransactionBalance(
            bankName = "Test Bank",
            accountLast4 = "1234",
            amount = BigDecimal("50.00"),
            transactionType = TransactionType.INCOME,
            explicitBalance = null,
            timestamp = transactionTime,
            transactionId = 11,
            creditLimit = null,
            isCreditCard = false,
            smsSource = "credited alert",
            currency = "INR"
        )

        assertEquals(BigDecimal("100.00"), dao.insertedBalances.single().balance)
    }

    @Test
    fun resolveAccountLast4ExpandsUniqueSameBankSuffix() = runTest {
        val dao = FakeAccountBalanceDao(
            suffixMatches = mapOf("Indian Overseas Bank" to mapOf("99" to listOf("1999")))
        )
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        assertEquals("1999", repository.resolveAccountLast4("Indian Overseas Bank", "99"))
    }

    @Test
    fun resolveAccountLast4DoesNotGuessAmbiguousSuffix() = runTest {
        val dao = FakeAccountBalanceDao(
            suffixMatches = mapOf("Test Bank" to mapOf("99" to listOf("1999", "2099")))
        )
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        assertEquals("99", repository.resolveAccountLast4("Test Bank", "99"))
    }

    @Test
    fun resolveAccountLast4DoesNotQueryForNonDigitSuffix() = runTest {
        val dao = FakeAccountBalanceDao()
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        assertEquals("%", repository.resolveAccountLast4("Test Bank", "%"))
        assertEquals(0, dao.suffixLookupCount)
    }

    @Test
    fun resolveAccountLast4TrimsToLast4ForLongInputs() = runTest {
        val dao = FakeAccountBalanceDao()
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        assertEquals("6789", repository.resolveAccountLast4("Test Bank", "123456789"))
    }

    @Test
    fun resolveAccountLast4DoesNotQueryForBlankSuffix() = runTest {
        val dao = FakeAccountBalanceDao()
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        assertEquals("", repository.resolveAccountLast4("Test Bank", ""))
        assertEquals(0, dao.suffixLookupCount)
    }

    @Test
    fun resolveEntityAccountNumberClearsAccountIfNoExistingAccountOrCardMatches() = runTest {
        val dao = FakeAccountBalanceDao()
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        val entity = com.reddy.vittify.data.database.entity.TransactionEntity(
            amount = BigDecimal("100"),
            merchantName = "Test",
            category = "General",
            transactionType = TransactionType.EXPENSE,
            dateTime = LocalDateTime.now()
        )
        val parsedTx = com.reddy.parser.core.ParsedTransaction(
            amount = BigDecimal("100"),
            type = com.reddy.parser.core.TransactionType.EXPENSE,
            merchant = "Test Merchant",
            reference = null,
            accountLast4 = "9999",
            balance = null,
            smsBody = "Test SMS",
            sender = "TESTBK",
            bankName = "NonExistent Bank",
            timestamp = System.currentTimeMillis()
        )

        val resolved = repository.resolveEntityAccountNumber(entity, parsedTx)
        assertNull(resolved.accountId)
    }

    @Test
    fun resolveEntityAccountNumberMatchesExistingAccount() = runTest {
        val dao = FakeAccountBalanceDao(
            latestBalances = mutableMapOf(
                accountKey("HDFC Bank", "1234") to balance(
                    id = "1",
                    bankName = "HDFC Bank",
                    accountLast4 = "1234",
                    balance = BigDecimal("500.00"),
                    timestamp = LocalDateTime.now()
                )
            )
        )
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        val entity = com.reddy.vittify.data.database.entity.TransactionEntity(
            amount = BigDecimal("100"),
            merchantName = "Test",
            category = "General",
            transactionType = TransactionType.EXPENSE,
            dateTime = LocalDateTime.now()
        )
        val parsedTx = com.reddy.parser.core.ParsedTransaction(
            amount = BigDecimal("100"),
            type = com.reddy.parser.core.TransactionType.EXPENSE,
            merchant = "Test Merchant",
            reference = null,
            accountLast4 = "1234",
            balance = null,
            smsBody = "Test SMS",
            sender = "HDFCBK",
            bankName = "HDFC Bank",
            timestamp = System.currentTimeMillis()
        )

        val resolved = repository.resolveEntityAccountNumber(entity, parsedTx)
        assertEquals("1", resolved.accountId)
    }

    @Test
    fun resolveEntityAccountNumberMatchesExistingAccountAndPropagatesOwnerId() = runTest {
        val account = balance(
            id = "partner-acc-1",
            bankName = "HDFC Bank",
            accountLast4 = "1720",
            balance = BigDecimal("500.00"),
            timestamp = LocalDateTime.now()
        ).copy(ownerId = "partner-device-123")
        val dao = FakeAccountBalanceDao(
            latestBalances = mutableMapOf(
                accountKey("HDFC Bank", "1720") to account
            )
        )
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        val entity = com.reddy.vittify.data.database.entity.TransactionEntity(
            amount = BigDecimal("100"),
            merchantName = "Swiggy",
            category = "Food & Drinks",
            transactionType = TransactionType.EXPENSE,
            dateTime = LocalDateTime.now(),
            ownerId = ""
        )
        val parsedTx = com.reddy.parser.core.ParsedTransaction(
            amount = BigDecimal("100"),
            type = com.reddy.parser.core.TransactionType.EXPENSE,
            merchant = "Swiggy",
            reference = null,
            accountLast4 = "1720",
            balance = null,
            smsBody = "Test SMS",
            sender = "HDFCBK",
            bankName = "HDFC Bank",
            timestamp = System.currentTimeMillis()
        )

        val resolved = repository.resolveEntityAccountNumber(entity, parsedTx)
        assertEquals("partner-acc-1", resolved.accountId)
        assertEquals("partner-device-123", resolved.ownerId)
    }

    @Test
    fun insertBalanceUpdateUpdatesAccountBalance() = runTest {
        val dao = FakeAccountBalanceDao()
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        val timestamp = LocalDateTime.of(2026, 5, 23, 20, 35)
        repository.insertBalanceUpdate(
            bankName = "Test Bank",
            accountLast4 = "1234",
            balance = BigDecimal("25000.00"),
            timestamp = timestamp,
            currency = "INR"
        )

        val latest = dao.getLatestBalance("Test Bank", "1234")
        assertNotNull(latest)
        assertEquals(BigDecimal("25000.00"), latest.balance)
    }

    private class FakeAccountBalanceDao(
        private val latestBalances: MutableMap<String, AccountBalanceEntity> = mutableMapOf(),
        private val suffixMatches: Map<String, Map<String, List<String>>> = emptyMap()
    ) : AccountBalanceDao() {
        val insertedBalances = mutableListOf<AccountBalanceEntity>()
        val updatedBalances = mutableMapOf<String, BigDecimal>()
        var suffixLookupCount = 0
        val accounts = mutableListOf<AccountBalanceEntity>()

        init {
            latestBalances.values.forEach { seedBalance(it) }
        }

        fun seedBalance(entity: AccountBalanceEntity) {
            accounts.removeAll { it.bankName == entity.bankName && it.accountLast4 == entity.accountLast4 }
            accounts.add(entity)
            latestBalances[accountKey(entity.bankName, entity.accountLast4)] = entity
        }

        override suspend fun insertAccount(account: AccountBalanceEntity): Long {
            seedBalance(account)
            insertedBalances.add(account)
            return 1L
        }

        override suspend fun deletePartnerBalances(partnerDeviceId: String, myDeviceId: String): Int {
            val count = accounts.count { it.ownerId == partnerDeviceId }
            accounts.removeAll { it.ownerId == partnerDeviceId }
            return count
        }

        override suspend fun insertBalance(balance: AccountBalanceEntity): Long {
            seedBalance(balance)
            insertedBalances.add(balance)
            return 1L
        }

        override suspend fun getAccountById(id: String): AccountBalanceEntity? =
            accounts.find { it.id == id }

        override suspend fun getBalanceById(id: String): AccountBalanceEntity? =
            accounts.find { it.id == id }

        override suspend fun getLatestBalance(bankName: String, accountLast4: String): AccountBalanceEntity? {
            return latestBalances[accountKey(bankName, accountLast4)]
                ?: accounts.filter { it.bankName == bankName && it.accountLast4 == accountLast4 }
                    .maxByOrNull { it.timestamp }
        }

        override suspend fun getLatestBalanceOnOrBefore(
            bankName: String,
            accountLast4: String,
            timestamp: LocalDateTime
        ): AccountBalanceEntity? {
            return accounts.filter {
                it.bankName == bankName && it.accountLast4 == accountLast4 && !it.timestamp.isAfter(timestamp)
            }.maxByOrNull { it.timestamp }
        }

        override suspend fun getAccountLast4sEndingWith(bankName: String, suffix: String): List<String> {
            suffixLookupCount++
            return suffixMatches[bankName]?.get(suffix).orEmpty()
        }

        override fun getLatestBalanceFlow(
            bankName: String,
            accountLast4: String
        ): Flow<AccountBalanceEntity?> = flowOf(latestBalances[accountKey(bankName, accountLast4)])

        override fun getAllLatestBalances(): Flow<List<AccountBalanceEntity>> = flowOf(accounts.toList())

        override fun getAllBalances(): Flow<List<AccountBalanceEntity>> = flowOf(accounts.toList())

        override suspend fun deleteAllBalances() {
            accounts.clear()
            latestBalances.clear()
        }

        override suspend fun deleteSampleBalances() {
            accounts.removeAll { it.isSample }
        }

        override fun getCurrentMonthLatestBalances(): Flow<List<AccountBalanceEntity>> = flowOf(accounts.toList())

        override fun getTotalBalance(): Flow<BigDecimal?> =
            flowOf(accounts.map { it.balance }.fold(BigDecimal.ZERO, BigDecimal::add))

        override fun getBalanceHistory(
            bankName: String,
            accountLast4: String,
            startDate: LocalDateTime,
            endDate: LocalDateTime
        ): Flow<List<AccountBalanceEntity>> = flowOf(emptyList())

        override fun getAccountCount(): Flow<Int> = flowOf(accounts.size)

        override suspend fun deleteOldBalances(beforeDate: LocalDateTime): Int = 0

        override suspend fun updateBalance(balance: AccountBalanceEntity) {
            updatedBalances[balance.id] = balance.balance
            seedBalance(balance)
        }

        override suspend fun deleteBalance(balance: AccountBalanceEntity) {
            accounts.removeAll { it.id == balance.id }
        }

        override suspend fun getBalanceHistoryForAccount(
            bankName: String,
            accountLast4: String
        ): List<AccountBalanceEntity> = emptyList()

        override suspend fun deleteBalanceById(id: String) {
            accounts.removeAll { it.id == id }
        }

        override suspend fun updateBalanceById(id: String, newBalance: BigDecimal) {
            updatedBalances[id] = newBalance
            val index = accounts.indexOfFirst { it.id == id }
            if (index != -1) {
                val updated = accounts[index].copy(balance = newBalance)
                accounts[index] = updated
                latestBalances[accountKey(updated.bankName, updated.accountLast4)] = updated
            }
        }

        override suspend fun getBalanceCountForAccount(bankName: String, accountLast4: String): Int =
            accounts.count { it.bankName == bankName && it.accountLast4 == accountLast4 }

        override suspend fun deleteAccount(bankName: String, accountLast4: String): Int {
            val beforeSize = accounts.size
            accounts.removeAll { it.bankName == bankName && it.accountLast4 == accountLast4 }
            latestBalances.remove(accountKey(bankName, accountLast4))
            return beforeSize - accounts.size
        }

        override suspend fun updateAccountBankName(
            oldBankName: String,
            accountLast4: String,
            newBankName: String
        ): Int = 0

        override suspend fun getAccountByLast4(accountLast4: String): AccountBalanceEntity? =
            accounts.find { it.accountLast4 == accountLast4 || it.accountLast4.endsWith(accountLast4) }

        override suspend fun getEarliestBalance(bankName: String, accountLast4: String): AccountBalanceEntity? {
            return accounts.filter { it.bankName == bankName && it.accountLast4 == accountLast4 }
                .minByOrNull { it.timestamp }
        }

        override suspend fun updateAccountCustomId(bankName: String, accountLast4: String, customId: String?): Int = 0

        override suspend fun updateOwnerIdForBlank(newOwnerId: String): Int = 0

        override suspend fun updateOwnerIdForAccount(accountId: String, newOwnerId: String): Int = 0

        override suspend fun updateOwnerIdForOwner(oldOwnerId: String, newOwnerId: String): Int = 0
    }

    private companion object {
        fun accountKey(bankName: String, accountLast4: String) = "$bankName:$accountLast4"

        fun balance(
            id: String,
            bankName: String,
            accountLast4: String,
            balance: BigDecimal,
            timestamp: LocalDateTime,
            isCreditCard: Boolean = false
        ) = AccountBalanceEntity(
            id = id,
            bankName = bankName,
            accountLast4 = accountLast4,
            balance = balance,
            timestamp = timestamp,
            isCreditCard = isCreditCard
        )
    }
}
