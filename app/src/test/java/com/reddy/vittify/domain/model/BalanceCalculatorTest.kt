package com.reddy.vittify.domain.model

import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.database.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class BalanceCalculatorTest {

    @Test
    fun testResolveTransferDirection_Destination() {
        val direction = BalanceCalculator.resolveTransferDirection(
            accountLast4 = "1234",
            fromAccount = "5678",
            toAccount = "1234"
        )
        assertEquals(BalanceCalculator.TransferDirection.DESTINATION, direction)
    }

    @Test
    fun testResolveTransferDirection_Source() {
        val direction = BalanceCalculator.resolveTransferDirection(
            accountLast4 = "1234",
            fromAccount = "1234",
            toAccount = "5678"
        )
        assertEquals(BalanceCalculator.TransferDirection.SOURCE, direction)
    }

    @Test
    fun testResolveTransferDirection_None() {
        val direction = BalanceCalculator.resolveTransferDirection(
            accountLast4 = "1234",
            fromAccount = "5678",
            toAccount = "9999"
        )
        assertEquals(BalanceCalculator.TransferDirection.NONE, direction)
    }

    @Test
    fun testBalanceEffect_RegularAccount_Income() {
        val effect = BalanceCalculator.balanceEffect(
            amount = BigDecimal("100"),
            transactionType = TransactionType.INCOME,
            isCreditCard = false,
            direction = BalanceCalculator.TransferDirection.NONE
        )
        assertEquals(BigDecimal("100"), effect)
    }

    @Test
    fun testBalanceEffect_RegularAccount_Expense() {
        val effect = BalanceCalculator.balanceEffect(
            amount = BigDecimal("100"),
            transactionType = TransactionType.EXPENSE,
            isCreditCard = false,
            direction = BalanceCalculator.TransferDirection.NONE
        )
        assertEquals(BigDecimal("-100"), effect)
    }

    @Test
    fun testBalanceEffect_CreditCard_Income() {
        // Income/Payment reduces the outstanding debt on a credit card
        val effect = BalanceCalculator.balanceEffect(
            amount = BigDecimal("100"),
            transactionType = TransactionType.INCOME,
            isCreditCard = true,
            direction = BalanceCalculator.TransferDirection.NONE
        )
        assertEquals(BigDecimal("-100"), effect)
    }

    @Test
    fun testBalanceEffect_CreditCard_Expense() {
        // Expense increases the outstanding debt on a credit card
        val effect = BalanceCalculator.balanceEffect(
            amount = BigDecimal("100"),
            transactionType = TransactionType.EXPENSE,
            isCreditCard = true,
            direction = BalanceCalculator.TransferDirection.NONE
        )
        assertEquals(BigDecimal("100"), effect)
    }

    @Test
    fun testBalanceEffect_RegularAccount_TransferDestination() {
        val effect = BalanceCalculator.balanceEffect(
            amount = BigDecimal("100"),
            transactionType = TransactionType.TRANSFER,
            isCreditCard = false,
            direction = BalanceCalculator.TransferDirection.DESTINATION
        )
        assertEquals(BigDecimal("100"), effect)
    }

    @Test
    fun testBalanceEffect_CreditCard_TransferDestination() {
        // Transferring money TO a credit card (e.g., paying it off) decreases the debt
        val effect = BalanceCalculator.balanceEffect(
            amount = BigDecimal("100"),
            transactionType = TransactionType.TRANSFER,
            isCreditCard = true,
            direction = BalanceCalculator.TransferDirection.DESTINATION
        )
        assertEquals(BigDecimal("-100"), effect)
    }

    @Test
    fun testApply_CreditLimitNotExceeded() {
        val currentBalance = BigDecimal("500") // Owe 500
        val amount = BigDecimal("100") // Spend 100
        
        val newBalance = BalanceCalculator.apply(
            currentBalance = currentBalance,
            amount = amount,
            transactionType = TransactionType.EXPENSE,
            isCreditCard = true,
            direction = BalanceCalculator.TransferDirection.NONE
        )
        assertEquals(BigDecimal("600"), newBalance)
    }

    @Test
    fun testCalculateAuditBalances_EmptyTransactions() {
        val result = BalanceCalculator.calculateAuditBalances(
            transactions = emptyList(),
            accountLast4 = "1234",
            isCreditCard = false,
            currentBalance = BigDecimal("1500")
        )
        assertEquals(BigDecimal("1500"), result.initialBalance)
        assertEquals(BigDecimal("1500"), result.expectedBalance)
    }

    @Test
    fun testCalculateAuditBalances_RegularAccount_WithoutAnchor() {
        val now = LocalDateTime.now()
        val txs = listOf(
            TransactionEntity(
                id = 1,
                amount = BigDecimal("2600"),
                merchantName = "Transfer In",
                category = "Transfer",
                transactionType = TransactionType.TRANSFER,
                dateTime = now.minusDays(5),
                toAccount = "4362"
            ),
            TransactionEntity(
                id = 2,
                amount = BigDecimal("9926.3"),
                merchantName = "Store",
                category = "Shopping",
                transactionType = TransactionType.EXPENSE,
                dateTime = now.minusDays(3),
                fromAccount = "4362"
            ),
            TransactionEntity(
                id = 3,
                amount = BigDecimal("10000"),
                merchantName = "Transfer In 2",
                category = "Transfer",
                transactionType = TransactionType.TRANSFER,
                dateTime = now.minusDays(1),
                toAccount = "4362"
            )
        )
        val result = BalanceCalculator.calculateAuditBalances(
            transactions = txs,
            accountLast4 = "4362",
            isCreditCard = false,
            currentBalance = BigDecimal.ZERO
        )
        // initialBalance should be ZERO, NOT negative!
        assertEquals(BigDecimal.ZERO, result.initialBalance)
        // expectedBalance = 0 + 2600 - 9926.3 + 10000 = 2673.7
        assertEquals(BigDecimal("2673.7"), result.expectedBalance)
    }

    @Test
    fun testCalculateAuditBalances_RegularAccount_WithAnchor() {
        val now = LocalDateTime.now()
        val txs = listOf(
            TransactionEntity(
                id = 1,
                amount = BigDecimal("100"),
                merchantName = "Food",
                category = "Food",
                transactionType = TransactionType.EXPENSE,
                dateTime = now.minusDays(3),
                fromAccount = "1234"
            ),
            TransactionEntity(
                id = 2,
                amount = BigDecimal("200"),
                merchantName = "Groceries",
                category = "Groceries",
                transactionType = TransactionType.EXPENSE,
                dateTime = now.minusDays(2),
                fromAccount = "1234",
                balanceAfter = BigDecimal("5000")
            ),
            TransactionEntity(
                id = 3,
                amount = BigDecimal("1000"),
                merchantName = "Salary",
                category = "Salary",
                transactionType = TransactionType.INCOME,
                dateTime = now.minusDays(1),
                toAccount = "1234"
            )
        )
        val result = BalanceCalculator.calculateAuditBalances(
            transactions = txs,
            accountLast4 = "1234",
            isCreditCard = false,
            currentBalance = BigDecimal("5000")
        )
        // Anchor is at tx2 (balanceAfter = 5000)
        // Tx3 (+1000) happened after anchor -> expectedBalance = 5000 + 1000 = 6000
        assertEquals(BigDecimal("6000"), result.expectedBalance)
        // Tx1 (-100) and Tx2 (-200) led to anchor 5000 -> initialBalance = 5000 - (-300) = 5300
        assertEquals(BigDecimal("5300"), result.initialBalance)
    }

    @Test
    fun testCalculateAuditBalances_CreditCard() {
        val now = LocalDateTime.now()
        val txs = listOf(
            TransactionEntity(
                id = 1,
                amount = BigDecimal("500"),
                merchantName = "Shopping",
                category = "Shopping",
                transactionType = TransactionType.EXPENSE,
                dateTime = now.minusDays(2),
                fromAccount = "0005"
            ),
            TransactionEntity(
                id = 2,
                amount = BigDecimal("200"),
                merchantName = "Bill Payment",
                category = "Payment",
                transactionType = TransactionType.INCOME,
                dateTime = now.minusDays(1),
                toAccount = "0005"
            )
        )
        val result = BalanceCalculator.calculateAuditBalances(
            transactions = txs,
            accountLast4 = "0005",
            isCreditCard = true,
            currentBalance = BigDecimal("300")
        )
        assertEquals(BigDecimal.ZERO, result.initialBalance)
        // Credit card debt: +500 (expense) - 200 (payment) = 300
        assertEquals(BigDecimal("300"), result.expectedBalance)
    }

    @Test
    fun testCalculatePortfolioStartDate_NullTransactionDate_ReturnsDefault180DaysAgo() {
        val today = LocalDate.of(2026, 9, 6)
        val startDate = BalanceCalculator.calculatePortfolioStartDate(
            firstTransactionDate = null,
            endDate = today,
            maxDays = 180
        )
        assertEquals(today.minusDays(180), startDate)
    }

    @Test
    fun testCalculatePortfolioStartDate_FirstTransactionWithin180Days_ReturnsFirstTransactionDate() {
        val today = LocalDate.of(2026, 9, 6)
        val firstTxDate = LocalDate.of(2026, 8, 10) // 27 days ago
        val startDate = BalanceCalculator.calculatePortfolioStartDate(
            firstTransactionDate = firstTxDate,
            endDate = today,
            maxDays = 180
        )
        assertEquals(firstTxDate, startDate)
    }

    @Test
    fun testCalculatePortfolioStartDate_FirstTransactionOlderThan180Days_Returns180DaysAgo() {
        val today = LocalDate.of(2026, 9, 6)
        val firstTxDate = LocalDate.of(2024, 1, 1) // > 900 days ago
        val startDate = BalanceCalculator.calculatePortfolioStartDate(
            firstTransactionDate = firstTxDate,
            endDate = today,
            maxDays = 180
        )
        assertEquals(today.minusDays(180), startDate)
    }

    @Test
    fun testCalculatePortfolioStartDate_FirstTransactionExactly180DaysAgo_Returns180DaysAgo() {
        val today = LocalDate.of(2026, 9, 6)
        val firstTxDate = today.minusDays(180)
        val startDate = BalanceCalculator.calculatePortfolioStartDate(
            firstTransactionDate = firstTxDate,
            endDate = today,
            maxDays = 180
        )
        assertEquals(firstTxDate, startDate)
    }

    @Test
    fun testCalculatePortfolioStartDate_FirstTransactionToday_ReturnsToday() {
        val today = LocalDate.of(2026, 9, 6)
        val startDate = BalanceCalculator.calculatePortfolioStartDate(
            firstTransactionDate = today,
            endDate = today,
            maxDays = 180
        )
        assertEquals(today, startDate)
    }

    @Test
    fun testCalculatePortfolioStartDate_FirstTransactionInFuture_ClampedToToday() {
        val today = LocalDate.of(2026, 9, 6)
        val futureTxDate = today.plusDays(10)
        val startDate = BalanceCalculator.calculatePortfolioStartDate(
            firstTransactionDate = futureTxDate,
            endDate = today,
            maxDays = 180
        )
        assertEquals(today, startDate)
    }
}
