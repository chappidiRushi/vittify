package com.reddy.vittify.domain.service

import android.content.ContextWrapper
import com.reddy.vittify.data.currency.CurrencyConversionService
import com.reddy.vittify.data.database.dao.AccountBalanceDao
import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.vittify.data.repository.AccountBalanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime

class TransactionBalanceServiceTest {

    private lateinit var balanceDao: FakeAccountBalanceDao
    private lateinit var accountBalanceRepo: AccountBalanceRepository
    private lateinit var service: TransactionBalanceService

    private val now = LocalDateTime.now()

    @Before
    fun setup() {
        balanceDao = FakeAccountBalanceDao()
        accountBalanceRepo = AccountBalanceRepository(balanceDao, ContextWrapper(null))
        service = TransactionBalanceService(accountBalanceRepo)
    }

    @Test
    fun `editing transaction and changing account from A to B credits A and debits B`() = runTest {
        val accountA = AccountBalanceEntity(
            id = "acc_a",
            bankName = "HDFC",
            accountLast4 = "1111",
            balance = BigDecimal("1000"),
            currency = "INR",
            isCreditCard = false
        )
        val accountB = AccountBalanceEntity(
            id = "acc_b",
            bankName = "SBI",
            accountLast4 = "2222",
            balance = BigDecimal("500"),
            currency = "INR",
            isCreditCard = false
        )
        balanceDao.seedBalance(accountA)
        balanceDao.seedBalance(accountB)

        val oldTxn = TransactionEntity(
            id = 1,
            accountId = "acc_a",
            fromAccountId = "acc_a",
            fromAccount = "1111",
            amount = BigDecimal("200"),
            merchantName = "Amazon",
            category = "Shopping",
            transactionType = TransactionType.EXPENSE,
            dateTime = now,
            currency = "INR"
        )

        val newTxn = oldTxn.copy(
            accountId = "acc_b",
            fromAccountId = "acc_b",
            fromAccount = "2222"
        )

        service.updateTransaction(oldTxn, newTxn)

        val updatedA = balanceDao.getAccountById("acc_a")!!
        val updatedB = balanceDao.getAccountById("acc_b")!!

        // A was debited 200 originally, now credited back 200 -> 1000 + 200 = 1200
        assertEquals(BigDecimal("1200"), updatedA.balance)
        // B was 500, now debited 200 -> 500 - 200 = 300
        assertEquals(BigDecimal("300"), updatedB.balance)
    }

    @Test
    fun `editing income transaction and changing account from A to B debits A and credits B`() = runTest {
        val accountA = AccountBalanceEntity(
            id = "acc_a",
            bankName = "HDFC",
            accountLast4 = "1111",
            balance = BigDecimal("1000"),
            currency = "INR",
            isCreditCard = false
        )
        val accountB = AccountBalanceEntity(
            id = "acc_b",
            bankName = "SBI",
            accountLast4 = "2222",
            balance = BigDecimal("500"),
            currency = "INR",
            isCreditCard = false
        )
        balanceDao.seedBalance(accountA)
        balanceDao.seedBalance(accountB)

        val oldTxn = TransactionEntity(
            id = 2,
            accountId = "acc_a",
            fromAccountId = "acc_a",
            fromAccount = "1111",
            amount = BigDecimal("300"),
            merchantName = "Employer",
            category = "Salary",
            transactionType = TransactionType.INCOME,
            dateTime = now,
            currency = "INR"
        )

        val newTxn = oldTxn.copy(
            accountId = "acc_b",
            fromAccountId = "acc_b",
            fromAccount = "2222"
        )

        service.updateTransaction(oldTxn, newTxn)

        val updatedA = balanceDao.getAccountById("acc_a")!!
        val updatedB = balanceDao.getAccountById("acc_b")!!

        // A had +300, now reverted -> 1000 - 300 = 700
        assertEquals(BigDecimal("700"), updatedA.balance)
        // B now receives income -> 500 + 300 = 800
        assertEquals(BigDecimal("800"), updatedB.balance)
    }

    @Test
    fun `editing transaction amount increases expense - difference is debited`() = runTest {
        val accountA = AccountBalanceEntity(
            id = "acc_a",
            bankName = "HDFC",
            accountLast4 = "1111",
            balance = BigDecimal("1000"),
            currency = "INR",
            isCreditCard = false
        )
        balanceDao.seedBalance(accountA)

        val oldTxn = TransactionEntity(
            id = 3,
            accountId = "acc_a",
            fromAccountId = "acc_a",
            fromAccount = "1111",
            amount = BigDecimal("100"),
            merchantName = "Coffee",
            category = "Food",
            transactionType = TransactionType.EXPENSE,
            dateTime = now,
            currency = "INR"
        )

        val newTxn = oldTxn.copy(amount = BigDecimal("150"))

        service.updateTransaction(oldTxn, newTxn)

        val updatedA = balanceDao.getAccountById("acc_a")!!
        // Difference 150 - 100 = 50 debited -> 1000 - 50 = 950
        assertEquals(BigDecimal("950"), updatedA.balance)
    }

    @Test
    fun `editing transaction amount decreases expense - difference is credited`() = runTest {
        val accountA = AccountBalanceEntity(
            id = "acc_a",
            bankName = "HDFC",
            accountLast4 = "1111",
            balance = BigDecimal("1000"),
            currency = "INR",
            isCreditCard = false
        )
        balanceDao.seedBalance(accountA)

        val oldTxn = TransactionEntity(
            id = 4,
            accountId = "acc_a",
            fromAccountId = "acc_a",
            fromAccount = "1111",
            amount = BigDecimal("100"),
            merchantName = "Coffee",
            category = "Food",
            transactionType = TransactionType.EXPENSE,
            dateTime = now,
            currency = "INR"
        )

        val newTxn = oldTxn.copy(amount = BigDecimal("40"))

        service.updateTransaction(oldTxn, newTxn)

        val updatedA = balanceDao.getAccountById("acc_a")!!
        // Difference 100 - 40 = 60 credited -> 1000 + 60 = 1060
        assertEquals(BigDecimal("1060"), updatedA.balance)
    }

    @Test
    fun `editing credit card expense amount increases - outstanding debt increases by difference`() = runTest {
        val card = AccountBalanceEntity(
            id = "card_1",
            bankName = "HDFC",
            accountLast4 = "9999",
            balance = BigDecimal("1000"), // outstanding debt = 1000
            currency = "INR",
            isCreditCard = true,
            creditLimit = BigDecimal("50000")
        )
        balanceDao.seedBalance(card)

        val oldTxn = TransactionEntity(
            id = 5,
            accountId = "card_1",
            fromAccountId = "card_1",
            fromAccount = "9999",
            amount = BigDecimal("200"),
            merchantName = "Electronics",
            category = "Shopping",
            transactionType = TransactionType.EXPENSE,
            dateTime = now,
            currency = "INR"
        )

        val newTxn = oldTxn.copy(amount = BigDecimal("350"))

        service.updateTransaction(oldTxn, newTxn)

        val updatedCard = balanceDao.getAccountById("card_1")!!
        // Spent 150 more on CC -> debt increases from 1000 to 1150
        assertEquals(BigDecimal("1150"), updatedCard.balance)
    }

    @Test
    fun `editing credit card expense amount decreases - outstanding debt decreases by difference`() = runTest {
        val card = AccountBalanceEntity(
            id = "card_1",
            bankName = "HDFC",
            accountLast4 = "9999",
            balance = BigDecimal("1000"), // outstanding debt = 1000
            currency = "INR",
            isCreditCard = true,
            creditLimit = BigDecimal("50000")
        )
        balanceDao.seedBalance(card)

        val oldTxn = TransactionEntity(
            id = 6,
            accountId = "card_1",
            fromAccountId = "card_1",
            fromAccount = "9999",
            amount = BigDecimal("200"),
            merchantName = "Electronics",
            category = "Shopping",
            transactionType = TransactionType.EXPENSE,
            dateTime = now,
            currency = "INR"
        )

        val newTxn = oldTxn.copy(amount = BigDecimal("50"))

        service.updateTransaction(oldTxn, newTxn)

        val updatedCard = balanceDao.getAccountById("card_1")!!
        // Spent 150 less on CC -> debt decreases from 1000 to 850
        assertEquals(BigDecimal("850"), updatedCard.balance)
    }

    @Test
    fun `editing transaction switches from regular account to credit card`() = runTest {
        val account = AccountBalanceEntity(
            id = "acc_1",
            bankName = "HDFC",
            accountLast4 = "1111",
            balance = BigDecimal("1000"),
            currency = "INR",
            isCreditCard = false
        )
        val card = AccountBalanceEntity(
            id = "card_1",
            bankName = "ICICI",
            accountLast4 = "9999",
            balance = BigDecimal("500"), // outstanding debt = 500
            currency = "INR",
            isCreditCard = true
        )
        balanceDao.seedBalance(account)
        balanceDao.seedBalance(card)

        val oldTxn = TransactionEntity(
            id = 7,
            accountId = "acc_1",
            fromAccountId = "acc_1",
            fromAccount = "1111",
            amount = BigDecimal("100"),
            merchantName = "Store",
            category = "Shopping",
            transactionType = TransactionType.EXPENSE,
            dateTime = now,
            currency = "INR"
        )

        val newTxn = oldTxn.copy(
            accountId = "card_1",
            fromAccountId = "card_1",
            fromAccount = "9999"
        )

        service.updateTransaction(oldTxn, newTxn)

        val updatedAccount = balanceDao.getAccountById("acc_1")!!
        val updatedCard = balanceDao.getAccountById("card_1")!!

        // Account credited back 100 -> 1100
        assertEquals(BigDecimal("1100"), updatedAccount.balance)
        // Credit card debt increases by 100 -> 600
        assertEquals(BigDecimal("600"), updatedCard.balance)
    }

    @Test
    fun `editing transaction switches from credit card to cash wallet`() = runTest {
        val card = AccountBalanceEntity(
            id = "card_1",
            bankName = "HDFC",
            accountLast4 = "9999",
            balance = BigDecimal("500"), // debt = 500
            currency = "INR",
            isCreditCard = true
        )
        val wallet = AccountBalanceEntity(
            id = "wallet_1",
            bankName = "Cash",
            accountLast4 = "wallet",
            balance = BigDecimal("200"),
            currency = "INR",
            isWallet = true,
            isCreditCard = false
        )
        balanceDao.seedBalance(card)
        balanceDao.seedBalance(wallet)

        val oldTxn = TransactionEntity(
            id = 8,
            accountId = "card_1",
            fromAccountId = "card_1",
            fromAccount = "9999",
            amount = BigDecimal("50"),
            merchantName = "Snacks",
            category = "Food",
            transactionType = TransactionType.EXPENSE,
            dateTime = now,
            currency = "INR"
        )

        val newTxn = oldTxn.copy(
            accountId = "wallet_1",
            fromAccountId = "wallet_1",
            fromAccount = "wallet"
        )

        service.updateTransaction(oldTxn, newTxn)

        val updatedCard = balanceDao.getAccountById("card_1")!!
        val updatedWallet = balanceDao.getAccountById("wallet_1")!!

        // Card debt reduced by 50 -> 450
        assertEquals(BigDecimal("450"), updatedCard.balance)
        // Wallet cash debited by 50 -> 150
        assertEquals(BigDecimal("150"), updatedWallet.balance)
    }

    @Test
    fun `transfer transaction amount updated adjusts both accounts by difference`() = runTest {
        val accountA = AccountBalanceEntity(
            id = "acc_a",
            bankName = "HDFC",
            accountLast4 = "1111",
            balance = BigDecimal("1000"),
            currency = "INR",
            isCreditCard = false
        )
        val accountB = AccountBalanceEntity(
            id = "acc_b",
            bankName = "SBI",
            accountLast4 = "2222",
            balance = BigDecimal("500"),
            currency = "INR",
            isCreditCard = false
        )
        balanceDao.seedBalance(accountA)
        balanceDao.seedBalance(accountB)

        val oldTxn = TransactionEntity(
            id = 9,
            accountId = "acc_a",
            fromAccountId = "acc_a",
            fromAccount = "1111",
            toAccountId = "acc_b",
            toAccount = "2222",
            amount = BigDecimal("100"),
            merchantName = "Self Transfer",
            category = "Transfer",
            transactionType = TransactionType.TRANSFER,
            dateTime = now,
            currency = "INR"
        )

        val newTxn = oldTxn.copy(amount = BigDecimal("150"))

        service.updateTransaction(oldTxn, newTxn)

        val updatedA = balanceDao.getAccountById("acc_a")!!
        val updatedB = balanceDao.getAccountById("acc_b")!!

        // Source A debited extra 50 -> 1000 - 50 = 950
        assertEquals(BigDecimal("950"), updatedA.balance)
        // Destination B credited extra 50 -> 500 + 50 = 550
        assertEquals(BigDecimal("550"), updatedB.balance)
    }

    @Test
    fun `transfer destination account changed credits old target and debits new target`() = runTest {
        val accountA = AccountBalanceEntity(
            id = "acc_a",
            bankName = "HDFC",
            accountLast4 = "1111",
            balance = BigDecimal("1000"),
            currency = "INR",
            isCreditCard = false
        )
        val accountB = AccountBalanceEntity(
            id = "acc_b",
            bankName = "SBI",
            accountLast4 = "2222",
            balance = BigDecimal("500"),
            currency = "INR",
            isCreditCard = false
        )
        val accountC = AccountBalanceEntity(
            id = "acc_c",
            bankName = "ICICI",
            accountLast4 = "3333",
            balance = BigDecimal("200"),
            currency = "INR",
            isCreditCard = false
        )
        balanceDao.seedBalance(accountA)
        balanceDao.seedBalance(accountB)
        balanceDao.seedBalance(accountC)

        val oldTxn = TransactionEntity(
            id = 10,
            accountId = "acc_a",
            fromAccountId = "acc_a",
            fromAccount = "1111",
            toAccountId = "acc_b",
            toAccount = "2222",
            amount = BigDecimal("100"),
            merchantName = "Self Transfer",
            category = "Transfer",
            transactionType = TransactionType.TRANSFER,
            dateTime = now,
            currency = "INR"
        )

        val newTxn = oldTxn.copy(
            toAccountId = "acc_c",
            toAccount = "3333"
        )

        service.updateTransaction(oldTxn, newTxn)

        val updatedA = balanceDao.getAccountById("acc_a")!!
        val updatedB = balanceDao.getAccountById("acc_b")!!
        val updatedC = balanceDao.getAccountById("acc_c")!!

        // Source A unchanged
        assertEquals(BigDecimal("1000"), updatedA.balance)
        // Old target B loses the 100 it received -> 500 - 100 = 400
        assertEquals(BigDecimal("400"), updatedB.balance)
        // New target C receives the 100 -> 200 + 100 = 300
        assertEquals(BigDecimal("300"), updatedC.balance)
    }

    @Test
    fun `multi-currency transaction edit converts amount difference to account currency`() = runTest {
        val fakeConversion = object : CurrencyConversionService() {
            override suspend fun convertAmount(
                amount: BigDecimal,
                fromCurrency: String,
                toCurrency: String,
                date: LocalDate?,
                forceRefresh: Boolean
            ): BigDecimal {
                if (fromCurrency.equals(toCurrency, ignoreCase = true)) return amount
                // 1 EUR = 1.10 USD
                if (fromCurrency.equals("EUR", ignoreCase = true) && toCurrency.equals("USD", ignoreCase = true)) {
                    return amount.multiply(BigDecimal("1.10")).setScale(2, RoundingMode.HALF_UP)
                }
                return amount
            }
        }

        val serviceWithConversion = TransactionBalanceService(accountBalanceRepo, fakeConversion)

        val accountUsd = AccountBalanceEntity(
            id = "acc_usd",
            bankName = "Chase",
            accountLast4 = "4444",
            balance = BigDecimal("100.00"),
            currency = "USD",
            isCreditCard = false
        )
        balanceDao.seedBalance(accountUsd)

        // Old txn: 10 EUR expense -> 11.00 USD
        val oldTxn = TransactionEntity(
            id = 11,
            accountId = "acc_usd",
            fromAccountId = "acc_usd",
            fromAccount = "4444",
            amount = BigDecimal("10.00"),
            merchantName = "Paris Cafe",
            category = "Food",
            transactionType = TransactionType.EXPENSE,
            dateTime = now,
            currency = "EUR"
        )

        // New txn: 20 EUR expense -> 22.00 USD
        val newTxn = oldTxn.copy(amount = BigDecimal("20.00"))

        serviceWithConversion.updateTransaction(oldTxn, newTxn)

        val updatedUsd = balanceDao.getAccountById("acc_usd")!!
        // Difference: 22.00 - 11.00 = 11.00 USD debited -> 100.00 - 11.00 = 89.00 USD
        assertEquals(BigDecimal("89.00"), updatedUsd.balance)
    }

    private class FakeAccountBalanceDao : AccountBalanceDao() {
        private val accounts = mutableListOf<AccountBalanceEntity>()

        fun seedBalance(entity: AccountBalanceEntity) {
            accounts.removeAll { it.id == entity.id || (it.bankName == entity.bankName && it.accountLast4 == entity.accountLast4) }
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

        override suspend fun getAccountByLast4(accountLast4: String): AccountBalanceEntity? =
            accounts.find { it.accountLast4 == accountLast4 }

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
            val count = accounts.count { it.bankName == bankName && it.accountLast4 == accountLast4 }
            accounts.removeAll { it.bankName == bankName && it.accountLast4 == accountLast4 }
            return count
        }

        override suspend fun updateAccountBankName(oldBankName: String, accountLast4: String, newBankName: String): Int = 1
        override suspend fun updateAccountCustomId(bankName: String, accountLast4: String, customId: String?): Int = 1
        override suspend fun getEarliestBalance(bankName: String, accountLast4: String): AccountBalanceEntity? = null
        override suspend fun updateOwnerIdForBlank(newOwnerId: String): Int = 0
        override suspend fun updateOwnerIdForAccount(accountId: String, newOwnerId: String): Int = 0
        override suspend fun updateOwnerIdForOwner(oldOwnerId: String, newOwnerId: String): Int = 0
    }
}
