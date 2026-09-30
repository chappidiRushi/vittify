package com.reddy.vittify.data.backup

import com.google.gson.GsonBuilder
import com.reddy.vittify.data.database.entity.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class BackupRestoreTest {

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeTypeAdapter())
        .registerTypeAdapter(LocalDate::class.java, LocalDateTypeAdapter())
        .registerTypeAdapter(BigDecimal::class.java, BigDecimalTypeAdapter())
        .registerTypeAdapter(String::class.java, FlexibleStringTypeAdapter())
        .registerTypeAdapter(DatabaseSnapshot::class.java, DatabaseSnapshotDeserializer())
        .create()

    @Test
    fun testAccountBalanceFullFieldPreservationAcrossJsonSerialization() {
        val originalBalance = AccountBalanceEntity(
            id = "acc_test_123",
            bankName = "State Bank of India",
            accountLast4 = "4321",
            accountName = "Salary Account",
            balance = BigDecimal("154200.50"),
            currency = "INR",
            isCreditCard = false,
            isWallet = false,
            creditLimit = BigDecimal("50000.00"),
            iconResId = 2131230845,
            iconName = "bank_sbi",
            color = "#1E88E5",
            isSample = false,
            customId = "custom_sbi_salary",
            timestamp = LocalDateTime.of(2026, 9, 6, 12, 0, 0),
            createdAt = LocalDateTime.of(2026, 1, 1, 10, 0, 0),
            updatedAt = LocalDateTime.of(2026, 9, 6, 12, 30, 0)
        )

        val originalTxn = TransactionEntity(
            id = 101L,
            amount = BigDecimal("1250.00"),
            merchantName = "Supermarket",
            category = "Food",
            subcategory = "Groceries",
            transactionType = TransactionType.EXPENSE,
            dateTime = LocalDateTime.of(2026, 9, 6, 11, 0, 0),
            description = "Weekly grocery shopping",
            smsBody = "Rs. 1250 debited from A/C 4321",
            smsSender = "SBIINB",
            balanceAfter = BigDecimal("154200.50"),
            uuid = "uuid-test-101",
            isRecurring = false,
            isDeleted = false,
            createdAt = LocalDateTime.of(2026, 9, 6, 11, 1, 0),
            updatedAt = LocalDateTime.of(2026, 9, 6, 11, 1, 0),
            currency = "INR",
            fromAccount = "4321",
            toAccount = "9876",
            reference = "UPI12345678",
            billingCycle = null,
            attachments = "",
            isSample = false,
            targetAmount = null,
            accountId = "acc_test_123",
            fromAccountId = "acc_test_123",
            toAccountId = "acc_dest_456"
        )

        val originalItem = TransactionItemEntity(
            id = 1L,
            transactionId = 101L,
            name = "Organic Milk",
            amount = BigDecimal("150.00"),
            category = "Food",
            subcategory = "Dairy"
        )

        val backup = VittifyBackup(
            metadata = BackupMetadata(
                exportId = "export_1",
                appVersion = "1.0.0",
                databaseVersion = 61,
                device = "Pixel 8",
                androidVersion = 34,
                statistics = BackupStatistics(
                    totalTransactions = 1,
                    totalCategories = 0,
                    totalCards = 0,
                    totalSubscriptions = 0,
                    totalAccountBalances = 1,
                    dateRange = null
                )
            ),
            database = DatabaseSnapshot(
                transactions = listOf(originalTxn),
                categories = emptyList(),
                cards = emptyList(),
                accountBalances = listOf(originalBalance),
                subscriptions = emptyList(),
                merchantMappings = emptyList(),
                unrecognizedSms = emptyList(),
                transactionItems = listOf(originalItem)
            ),
            preferences = PreferencesSnapshot(
                theme = ThemePreferences(isDarkThemeEnabled = false, isDynamicColorEnabled = true),
                sms = SmsPreferences(hasSkippedSmsPermission = false, smsScanMonths = 6, lastScanTimestamp = null, lastScanPeriod = null),
                developer = DeveloperPreferences(isDeveloperModeEnabled = false, systemPrompt = null),
                app = AppPreferences(hasShownScanTutorial = true, firstLaunchTime = 1000L, hasShownReviewPrompt = false, lastReviewPromptTime = null)
            )
        )

        // 1. Serialize to JSON
        val json = gson.toJson(backup)

        // Verify JSON contains key table fields
        assertTrue("JSON should contain accountName", json.contains("\"accountName\": \"Salary Account\""))
        assertTrue("JSON should contain customId", json.contains("\"customId\": \"custom_sbi_salary\""))
        assertTrue("JSON should contain accountLast4", json.contains("\"accountLast4\": \"4321\""))
        assertTrue("JSON should contain bankName", json.contains("\"bankName\": \"State Bank of India\""))
        assertTrue("JSON should contain creditLimit", json.contains("\"creditLimit\": \"50000.00\""))
        assertTrue("JSON should contain iconName", json.contains("\"iconName\": \"bank_sbi\""))
        assertTrue("JSON should contain color", json.contains("\"color\": \"#1E88E5\""))
        assertTrue("JSON should contain accountId", json.contains("\"accountId\": \"acc_test_123\""))
        assertTrue("JSON should contain fromAccountId", json.contains("\"fromAccountId\": \"acc_test_123\""))
        assertTrue("JSON should contain toAccountId", json.contains("\"toAccountId\": \"acc_dest_456\""))
        assertTrue("JSON should contain transaction_items", json.contains("\"transaction_items\""))
        assertTrue("JSON should contain total_account_balances", json.contains("\"total_account_balances\": 1"))

        // 2. Deserialize back from JSON
        val restoredBackup = gson.fromJson(json, VittifyBackup::class.java)

        // 3. Verify AccountBalance fields
        assertEquals(1, restoredBackup.database.accountBalances.size)
        val restoredBalance = restoredBackup.database.accountBalances[0].sanitize()
        assertEquals(originalBalance.id, restoredBalance.id)
        assertEquals(originalBalance.bankName, restoredBalance.bankName)
        assertEquals(originalBalance.accountLast4, restoredBalance.accountLast4)
        assertEquals(originalBalance.accountName, restoredBalance.accountName)
        assertEquals(originalBalance.balance, restoredBalance.balance)
        assertEquals(originalBalance.currency, restoredBalance.currency)
        assertEquals(originalBalance.isCreditCard, restoredBalance.isCreditCard)
        assertEquals(originalBalance.isWallet, restoredBalance.isWallet)
        assertEquals(originalBalance.creditLimit, restoredBalance.creditLimit)
        assertEquals(originalBalance.iconResId, restoredBalance.iconResId)
        assertEquals(originalBalance.iconName, restoredBalance.iconName)
        assertEquals(originalBalance.color, restoredBalance.color)
        assertEquals(originalBalance.isSample, restoredBalance.isSample)
        assertEquals(originalBalance.customId, restoredBalance.customId)
        assertEquals(originalBalance.timestamp, restoredBalance.timestamp)
        assertEquals(originalBalance.createdAt, restoredBalance.createdAt)
        assertEquals(originalBalance.updatedAt, restoredBalance.updatedAt)

        // 4. Verify TransactionEntity fields
        assertEquals(1, restoredBackup.database.transactions.size)
        val restoredTxn = restoredBackup.database.transactions[0].sanitize()
        assertEquals(originalTxn.id, restoredTxn.id)
        assertEquals(originalTxn.amount, restoredTxn.amount)
        assertEquals(originalTxn.merchantName, restoredTxn.merchantName)
        assertEquals(originalTxn.accountId, restoredTxn.accountId)
        assertEquals(originalTxn.fromAccountId, restoredTxn.fromAccountId)
        assertEquals(originalTxn.toAccountId, restoredTxn.toAccountId)
        assertEquals(originalTxn.fromAccount, restoredTxn.fromAccount)
        assertEquals(originalTxn.toAccount, restoredTxn.toAccount)
        assertEquals(originalTxn.uuid, restoredTxn.uuid)

        // 5. Verify TransactionItemEntity fields
        assertEquals(1, restoredBackup.database.transactionItems.size)
        val restoredItem = restoredBackup.database.transactionItems[0].sanitize()
        assertEquals(originalItem.id, restoredItem.id)
        assertEquals(originalItem.transactionId, restoredItem.transactionId)
        assertEquals(originalItem.name, restoredItem.name)
        assertEquals(originalItem.amount, restoredItem.amount)
        assertEquals(originalItem.category, restoredItem.category)
        assertEquals(originalItem.subcategory, restoredItem.subcategory)
    }

    @Test
    fun testAccountBalanceSanitizePreservesAllFields() {
        val balance = AccountBalanceEntity(
            id = "custom_id_999",
            bankName = "HDFC Bank",
            accountLast4 = "7777",
            accountName = "Savings A/C",
            balance = BigDecimal("25000.75"),
            currency = "INR",
            isCreditCard = true,
            isWallet = false,
            creditLimit = BigDecimal("150000.00"),
            iconResId = 1234,
            iconName = "hdfc_card",
            color = "#004B87",
            isSample = false,
            customId = "my_hdfc",
            timestamp = LocalDateTime.of(2026, 8, 1, 9, 30),
            createdAt = LocalDateTime.of(2026, 1, 1, 0, 0),
            updatedAt = LocalDateTime.of(2026, 8, 1, 9, 30)
        )

        val sanitized = balance.sanitize()

        assertEquals("custom_id_999", sanitized.id)
        assertEquals("HDFC Bank", sanitized.bankName)
        assertEquals("7777", sanitized.accountLast4)
        assertEquals("Savings A/C", sanitized.accountName)
        assertEquals(BigDecimal("25000.75"), sanitized.balance)
        assertEquals("INR", sanitized.currency)
        assertTrue(sanitized.isCreditCard)
        assertFalse(sanitized.isWallet)
        assertEquals(BigDecimal("150000.00"), sanitized.creditLimit)
        assertEquals(1234, sanitized.iconResId)
        assertEquals("hdfc_card", sanitized.iconName)
        assertEquals("#004B87", sanitized.color)
        assertFalse(sanitized.isSample)
        assertEquals("my_hdfc", sanitized.customId)
        assertEquals(LocalDateTime.of(2026, 8, 1, 9, 30), sanitized.timestamp)
        assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), sanitized.createdAt)
        assertEquals(LocalDateTime.of(2026, 8, 1, 9, 30), sanitized.updatedAt)
    }

    @Test
    fun testAccountBalanceSanitizeAssignsDefaultsForMissingFields() {
        val legacyJson = """
            {
                "bankName": "ICICI Bank",
                "accountLast4": "5555",
                "balance": "1000.00"
            }
        """.trimIndent()

        val parsed = gson.fromJson(legacyJson, AccountBalanceEntity::class.java)
        val sanitized = parsed.sanitize()

        assertNotNull(sanitized.id)
        assertTrue(sanitized.id.isNotBlank())
        assertEquals("ICICI Bank", sanitized.bankName)
        assertEquals("5555", sanitized.accountLast4)
        assertEquals(BigDecimal("1000.00"), sanitized.balance)
        assertNull(sanitized.accountName)
        assertNull(sanitized.customId)
        assertNull(sanitized.creditLimit)
        assertEquals("INR", sanitized.currency)
        assertFalse(sanitized.isCreditCard)
        assertFalse(sanitized.isWallet)
        assertEquals("#33B5E5", sanitized.color)
        assertNotNull(sanitized.timestamp)
        assertNotNull(sanitized.createdAt)
        assertNotNull(sanitized.updatedAt)
    }

    @Test
    fun testTransactionSanitizePreservesAccountLinkage() {
        val txn = TransactionEntity(
            id = 50L,
            amount = BigDecimal("999.00"),
            merchantName = "Amazon",
            category = "Shopping",
            transactionType = TransactionType.EXPENSE,
            dateTime = LocalDateTime.now(),
            fromAccount = "1234",
            toAccount = "5678",
            accountId = "acc_source_1",
            fromAccountId = "acc_source_1",
            toAccountId = "acc_target_2"
        )

        val sanitized = txn.sanitize()

        assertEquals("acc_source_1", sanitized.accountId)
        assertEquals("acc_source_1", sanitized.fromAccountId)
        assertEquals("acc_target_2", sanitized.toAccountId)
        assertEquals("1234", sanitized.fromAccount)
        assertEquals("5678", sanitized.toAccount)
    }

    @Test
    fun testSelectiveImportFilterDefaults() {
        val filter = SelectiveImportFilter()
        assertTrue(filter.includeTransactions)
        assertTrue(filter.includeCategories)
        assertTrue(filter.includeSubcategories)
        assertTrue(filter.includeCards)
        assertTrue(filter.includeAccountBalances)
        assertTrue(filter.includeTransactionItems)
        assertTrue(filter.includeBudgets)
        assertTrue(filter.includeSubscriptions)
        assertTrue(filter.includeMerchantMappings)
        assertTrue(filter.includeWebhookProfiles)
        assertTrue(filter.includeRules)
        assertTrue(filter.includeRuleApplications)
        assertTrue(filter.includeExchangeRates)
        assertTrue(filter.includePreferences)
    }

    @Test
    fun testDeserializeUserBackupFile() {
        val file = java.io.File("backup.json").takeIf { it.exists() } ?: java.io.File("../backup.json")
        if (!file.exists()) return
        val json = file.readText()
        val backup = gson.fromJson(json, VittifyBackup::class.java)
        assertNotNull(backup)
        assertNotNull(backup.database)
        assertEquals(backup.metadata.statistics.totalTransactions, backup.database.transactions.size)
        assertTrue(backup.database.transactions.isNotEmpty())

        // Verify all lists can be iterated over without NullPointerException
        var totalElements = 0
        backup.database.transactions.forEach { totalElements++ }
        backup.database.categories.forEach { totalElements++ }
        backup.database.cards.forEach { totalElements++ }
        backup.database.accountBalances.forEach { totalElements++ }
        backup.database.transactionItems.forEach { totalElements++ }
        backup.database.subscriptions.forEach { totalElements++ }
        backup.database.merchantMappings.forEach { totalElements++ }
        backup.database.unrecognizedSms.forEach { totalElements++ }
        backup.database.budgets.forEach { totalElements++ }
        backup.database.budgetCategoryLimits.forEach { totalElements++ }
        backup.database.subcategories.forEach { totalElements++ }
        backup.database.rules.forEach { totalElements++ }
        backup.database.ruleApplications.forEach { totalElements++ }
        backup.database.webhookProfiles.forEach { totalElements++ }
        backup.database.exchangeRates.forEach { totalElements++ }

        assertTrue(totalElements > 0)
    }
}

