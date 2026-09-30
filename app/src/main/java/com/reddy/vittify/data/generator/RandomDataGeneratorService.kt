package com.reddy.vittify.data.generator

import android.content.Context
import androidx.core.content.edit
import androidx.room.withTransaction
import com.reddy.vittify.R
import com.reddy.vittify.data.database.VittifyDatabase
import com.reddy.vittify.data.database.dao.ExchangeRateDao
import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.data.database.entity.BudgetEntity
import com.reddy.vittify.data.database.entity.BudgetPeriod
import com.reddy.vittify.data.database.entity.BudgetTrackType
import com.reddy.vittify.data.database.entity.BudgetType
import com.reddy.vittify.data.database.entity.CardEntity
import com.reddy.vittify.data.database.entity.CardType
import com.reddy.vittify.data.database.entity.ExchangeRateEntity
import com.reddy.vittify.data.database.entity.SubscriptionEntity
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.vittify.data.preferences.UserPreferencesRepository
import com.reddy.vittify.data.repository.AccountBalanceRepository
import com.reddy.vittify.data.repository.BudgetRepository
import com.reddy.vittify.data.repository.CardRepository
import com.reddy.vittify.data.repository.SubscriptionRepository
import com.reddy.vittify.data.repository.TransactionRepository
import com.reddy.vittify.data.sync.P2pSyncPreferencesRepository
import com.reddy.vittify.domain.model.BalanceCalculator
import com.reddy.vittify.utils.IconResolutionUtils
import com.reddy.vittify.utils.NanoId
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class RandomDataGeneratorService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: VittifyDatabase,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val transactionRepository: TransactionRepository,
    private val cardRepository: CardRepository,
    private val budgetRepository: BudgetRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val exchangeRateDao: ExchangeRateDao,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val p2pPreferences: P2pSyncPreferencesRepository
) {

    private data class BankTemplate(
        val bankName: String,
        val isCreditCard: Boolean = false,
        val isWallet: Boolean = false,
        val defaultLast4: String? = null,
        val colorHex: String,
        val iconResId: Int,
        val preferredForeignCurrency: String? = null
    )

    private data class SimAccount(
        val id: String,
        val bankName: String,
        val accountLast4: String,
        val currency: String,
        val isCreditCard: Boolean,
        val isWallet: Boolean,
        val creditLimit: BigDecimal?,
        val colorHex: String,
        val iconResId: Int,
        var runningBalance: BigDecimal = BigDecimal.ZERO
    )

    private data class SpendTemplate(
        val category: String,
        val subcategory: String,
        val merchants: List<String>,
        val minInr: Int,
        val maxInr: Int,
        val preferCreditOrBank: Boolean = false,
        val canUseCash: Boolean = true
    )

    private data class SubscriptionSpec(
        val merchantName: String,
        val category: String,
        val subcategory: String,
        val billingCycle: String,
        val inrAmount: Int,
        val dayOfMonth: Int
    )

    private val bankPool = listOf(
        BankTemplate("HDFC Bank", colorHex = "#33B5E5", iconResId = R.drawable.type_finance_bank),
        BankTemplate("ICICI Bank", isCreditCard = true, colorHex = "#E91E63", iconResId = R.drawable.type_stationary_card_file_box),
        BankTemplate("SBI Bank", colorHex = "#1976D2", iconResId = R.drawable.type_finance_bank),
        BankTemplate("Axis Bank", colorHex = "#9C27B0", iconResId = R.drawable.type_finance_bank),
        BankTemplate("Kotak Mahindra", colorHex = "#F44336", iconResId = R.drawable.type_finance_classical_building),
        BankTemplate("Chase Sapphire", isCreditCard = true, colorHex = "#0066CC", iconResId = R.drawable.type_stationary_card_file_box, preferredForeignCurrency = "USD"),
        BankTemplate("Revolut Global", colorHex = "#00C853", iconResId = R.drawable.type_finance_currency_exchange, preferredForeignCurrency = "EUR"),
        BankTemplate("Emirates NBD", colorHex = "#FF9800", iconResId = R.drawable.type_finance_bank, preferredForeignCurrency = "AED"),
        BankTemplate("Citibank", colorHex = "#3F51B5", iconResId = R.drawable.type_finance_bank, preferredForeignCurrency = "GBP")
    )

    private val spendTaxonomy = listOf(
        // Food & Drinks
        SpendTemplate("Food & Drinks", "Swiggy", listOf("Swiggy", "Swiggy Instamart"), 180, 950),
        SpendTemplate("Food & Drinks", "Zomato", listOf("Zomato", "Zomato Dining"), 220, 1200),
        SpendTemplate("Food & Drinks", "Tea & Coffee", listOf("Starbucks", "Third Wave Coffee", "Blue Tokai", "Chai Point"), 90, 480),
        SpendTemplate("Food & Drinks", "Eating out", listOf("Truffles", "Social", "Barbeque Nation", "Mainland China", "Chili's"), 650, 3200, preferCreditOrBank = true),
        SpendTemplate("Food & Drinks", "Fast Food", listOf("McDonald's", "Burger King", "KFC", "Subway"), 160, 680),
        SpendTemplate("Food & Drinks", "Pizza", listOf("Domino's Pizza", "Pizza Hut", "La Pino'z"), 290, 980),
        // Groceries
        SpendTemplate("Groceries", "Zepto", listOf("Zepto", "Blinkit"), 240, 1400),
        SpendTemplate("Groceries", "Vegetables", listOf("BigBasket Daily", "Nature's Basket", "FreshToHome"), 150, 850),
        SpendTemplate("Groceries", "Dairy", listOf("Country Delight", "Nandini Milk Parlor", "Amul Store"), 80, 420),
        SpendTemplate("Groceries", "Staples", listOf("DMart", "Reliance Smart", "More Supermarket", "Star Bazaar"), 900, 4200, preferCreditOrBank = true, canUseCash = false),
        // Transport
        SpendTemplate("Transport", "Uber", listOf("Uber Trip", "Uber Auto"), 110, 680),
        SpendTemplate("Transport", "Rapido", listOf("Rapido Bike", "Ola Cabs", "Namma Yatri"), 65, 380),
        SpendTemplate("Transport", "Fuel", listOf("Indian Oil", "HP Petrol Pump", "Shell Fuel", "Bharat Petroleum"), 500, 3200, preferCreditOrBank = true, canUseCash = false),
        SpendTemplate("Transport", "Metro", listOf("Metro Rail SmartCard", "BMTC Pass", "FASTag Recharge"), 100, 600),
        // Shopping
        SpendTemplate("Shopping", "Clothes", listOf("Myntra", "Zara", "H&M", "Uniqlo", "Westside"), 1200, 5800, preferCreditOrBank = true, canUseCash = false),
        SpendTemplate("Shopping", "Electronics", listOf("Amazon", "Croma", "Flipkart", "Reliance Digital"), 850, 14500, preferCreditOrBank = true, canUseCash = false),
        SpendTemplate("Shopping", "Books", listOf("Amazon Kindle", "Crossword", "Blossom Book House"), 250, 950),
        SpendTemplate("Shopping", "Cosmetics", listOf("Nykaa", "Sephora", "Tira Beauty"), 450, 2600, preferCreditOrBank = true),
        // Entertainment
        SpendTemplate("Entertainment", "Movies", listOf("BookMyShow", "PVR Inox", "Cinepolis"), 380, 1400, preferCreditOrBank = true),
        SpendTemplate("Entertainment", "Bowling", listOf("Amoeba Bowling", "Smaaash", "Timezone Arcade"), 500, 1800),
        // Medical & Fitness
        SpendTemplate("Medical", "Medicines", listOf("Apollo Pharmacy", "Pharmeasy", "Tata 1mg", "MedPlus"), 180, 1600),
        SpendTemplate("Medical", "Clinic", listOf("Practo Consultation", "Manipal Clinic", "Clove Dental"), 500, 2500, preferCreditOrBank = true),
        SpendTemplate("Fitness", "Gym", listOf("Cult.fit", "Gold's Gym", "Decathlon Sports"), 600, 2800, preferCreditOrBank = true),
        // Home & Services
        SpendTemplate("Home", "Cleaning", listOf("Urban Company", "Home Centre", "IKEA"), 450, 3400, preferCreditOrBank = true),
        SpendTemplate("Services", "Laundry", listOf("Tumbledry Laundry", "BlueDart Courier", "3M Car Care"), 220, 1100),
        // Personal, Events, Travel, Gift
        SpendTemplate("Personal", "Grooming", listOf("Looks Salon", "Enrich Salon", "Toni&Guy"), 400, 1900),
        SpendTemplate("Events", "Birthday", listOf("Ferns N Petals", "Theobroma Bakery", "Archies"), 450, 2200),
        SpendTemplate("Travel", "Hotel", listOf("MakeMyTrip", "Airbnb", "Goibibo", "Agoda"), 2800, 12500, preferCreditOrBank = true, canUseCash = false),
        SpendTemplate("Gift", "Gift", listOf("Amazon Gift Card", "Hamleys", "Lifestyle"), 500, 2500)
    )

    private val subscriptionPool = listOf(
        SubscriptionSpec("Netflix", "Subscription", "Netflix", "MONTHLY", 649, 15),
        SubscriptionSpec("Spotify Premium", "Subscription", "Spotify", "MONTHLY", 119, 8),
        SubscriptionSpec("YouTube Premium", "Subscription", "Youtube", "MONTHLY", 149, 18),
        SubscriptionSpec("ChatGPT Plus", "Subscription", "ChatGPT", "MONTHLY", 1950, 22),
        SubscriptionSpec("NoBroker House Rent", "Bill", "Rent", "MONTHLY", 22000, 2),
        SubscriptionSpec("Airtel Fiber & Postpaid", "Bill", "Internet", "MONTHLY", 1179, 5),
        SubscriptionSpec("BESCOM Electricity", "Bill", "Electricity", "MONTHLY", 1850, 12),
        SubscriptionSpec("Amazon Prime", "Subscription", "Prime", "YEARLY", 1499, 20)
    )

    // Currency conversion factors relative to INR (1 unit of currency = X INR, so 1 INR = 1/X currency)
    private val inrPerUnit = mapOf(
        "INR" to BigDecimal("1.0"),
        "USD" to BigDecimal("84.0"),
        "EUR" to BigDecimal("91.0"),
        "GBP" to BigDecimal("108.0"),
        "AED" to BigDecimal("22.9"),
        "SGD" to BigDecimal("63.0"),
        "JPY" to BigDecimal("0.56")
    )

    private fun convertFromInr(inrAmount: Int, targetCurrency: String): BigDecimal {
        if (targetCurrency == "INR") return BigDecimal(inrAmount)
        val rate = inrPerUnit[targetCurrency] ?: BigDecimal("84.0")
        val raw = BigDecimal(inrAmount).divide(rate, 2, RoundingMode.HALF_UP)
        // Round to clean numbers for readability
        return if (raw >= BigDecimal("10")) {
            raw.setScale(0, RoundingMode.HALF_UP)
        } else {
            raw.max(BigDecimal("2.50"))
        }
    }

    /**
     * Deletes all existing sample/test data from the database.
     */
    suspend fun clearSampleData() = withContext(Dispatchers.IO) {
        database.withTransaction {
            transactionRepository.deleteSampleTransactions()
            accountBalanceRepository.deleteSampleBalances()
            cardRepository.deleteSampleCards()
            budgetRepository.deleteSampleBudgets()
            subscriptionRepository.deleteSampleSubscriptions()
        }
        userPreferencesRepository.setSampleDataSeeded(false)
    }

    /**
     * Clears any previous sample data and generates a brand-new, randomized financial dataset
     * across the configured duration (e.g. 2 years) where every account's transaction ledger
     * and final balance 100% check out with zero discrepancy.
     */
    suspend fun generateRandomData(
        config: RandomDataConfig,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): RandomDataGenerationResult = withContext(Dispatchers.IO) {
        val random = Random(System.nanoTime())
        val ownerId = p2pPreferences.getDeviceId()
        val now = LocalDateTime.now()
        val totalMonths = config.historyDuration.months
        val startDate = now.minusMonths(totalMonths.toLong()).withDayOfMonth(1).withHour(8).withMinute(0).withSecond(0).withNano(0)

        onProgress(0.05f, "Cleaning up previous sample data...")
        clearSampleData()

        // 1. Seed offline exchange rates if multi-currency or non-INR base currency is used
        val activeCurrencies = buildSet {
            add(config.baseCurrency)
            if (config.isMultiCurrency) {
                addAll(config.secondaryCurrencies)
            }
        }
        if (activeCurrencies.size > 1 || config.baseCurrency != "INR") {
            onProgress(0.10f, "Configuring multi-currency exchange rates...")
            seedOfflineExchangeRates(activeCurrencies + "INR", now)
        }

        // 2. Build randomized account list according to config.accountCount
        onProgress(0.15f, "Creating ${config.accountCount} accounts & wallets...")
        val usedLast4s = mutableSetOf("wallet", "apay")
        fun nextUniqueLast4(): String {
            while (true) {
                val candidate = random.nextInt(1000, 9999).toString()
                if (usedLast4s.add(candidate)) return candidate
            }
        }

        val simAccounts = mutableListOf<SimAccount>()
        val targetCount = config.accountCount.coerceIn(2, 8)

        // Slot 1: Primary Savings Account (Always in baseCurrency)
        val primaryBankTemplate = bankPool.first { !it.isCreditCard && !it.isWallet }
        val primaryAccount = SimAccount(
            id = NanoId.generate(),
            bankName = primaryBankTemplate.bankName,
            accountLast4 = nextUniqueLast4(),
            currency = config.baseCurrency,
            isCreditCard = false,
            isWallet = false,
            creditLimit = null,
            colorHex = primaryBankTemplate.colorHex,
            iconResId = primaryBankTemplate.iconResId
        )
        simAccounts.add(primaryAccount)

        // Slot 2: Cash Wallet (Always in baseCurrency)
        val cashWallet = SimAccount(
            id = NanoId.generate(),
            bankName = "Cash",
            accountLast4 = "wallet",
            currency = config.baseCurrency,
            isCreditCard = false,
            isWallet = true,
            creditLimit = null,
            colorHex = "#4CAF50",
            iconResId = R.drawable.type_finance_dollar_banknote
        )
        simAccounts.add(cashWallet)

        // Slot 3+: Credit Card & Additional Domestic/Foreign Accounts
        val remainingTemplates = (bankPool - primaryBankTemplate).shuffled(random)
        val secondaryCurrenciesList = config.secondaryCurrencies.toList()
        var foreignIdx = 0

        for (template in remainingTemplates) {
            if (simAccounts.size >= targetCount) break
            // Ensure at least one Credit Card when targetCount >= 3
            val isFirstExtraSlot = simAccounts.size == 2
            val chosenTemplate = if (isFirstExtraSlot && !template.isCreditCard) {
                remainingTemplates.firstOrNull { it.isCreditCard } ?: template
            } else {
                template
            }
            if (simAccounts.any { it.bankName == chosenTemplate.bankName }) continue

            val accountCurrency = if (config.isMultiCurrency && simAccounts.size >= 3 && secondaryCurrenciesList.isNotEmpty()) {
                val pref = chosenTemplate.preferredForeignCurrency
                if (pref != null && pref in config.secondaryCurrencies) pref
                else secondaryCurrenciesList[foreignIdx++ % secondaryCurrenciesList.size]
            } else {
                config.baseCurrency
            }

            val creditLimit = if (chosenTemplate.isCreditCard) {
                convertFromInr(random.nextInt(150_000, 450_000) / 1000 * 1000, accountCurrency)
            } else null

            simAccounts.add(
                SimAccount(
                    id = NanoId.generate(),
                    bankName = chosenTemplate.bankName,
                    accountLast4 = nextUniqueLast4(),
                    currency = accountCurrency,
                    isCreditCard = chosenTemplate.isCreditCard,
                    isWallet = chosenTemplate.isWallet,
                    creditLimit = creditLimit,
                    colorHex = chosenTemplate.colorHex,
                    iconResId = chosenTemplate.iconResId
                )
            )
        }

        // 3. Chronological Transaction Simulation Engine
        // Guarantees:
        // - Every account starts at runningBalance = 0 at t_0
        // - Regular accounts get an Opening Balance / Initial Deposit transaction on Day 1
        // - Timestamps are strictly monotonically increasing (at least 17 minutes apart) so zero duplicate warnings occur
        // - BalanceCalculator.apply() updates runningBalance on every single transaction
        // - Non-transfer transactions record balanceAfter = runningBalance; TRANSFER transactions keep balanceAfter = null
        val generatedTransactions = mutableListOf<TransactionEntity>()
        var chronologicalCursor = startDate

        fun advanceCursor(targetDateTime: LocalDateTime): LocalDateTime {
            val candidate = if (targetDateTime.isAfter(chronologicalCursor.plusMinutes(16))) {
                targetDateTime
            } else {
                chronologicalCursor.plusMinutes(random.nextLong(17, 38))
            }
            val capped = if (candidate.isAfter(now.minusMinutes(10))) {
                chronologicalCursor.plusSeconds(random.nextLong(40, 120)).coerceAtMost(now.minusMinutes(2))
            } else {
                candidate
            }
            chronologicalCursor = capped
            return capped
        }

        fun recordSingleAccountTx(
            account: SimAccount,
            amount: BigDecimal,
            type: TransactionType,
            merchantName: String,
            category: String,
            subcategory: String?,
            dateTime: LocalDateTime,
            billingCycle: String? = null,
            description: String? = null
        ) {
            val safeAmount = amount.max(BigDecimal("1.00"))
            val effectiveDateTime = advanceCursor(dateTime)
            val newBalance = BalanceCalculator.apply(
                currentBalance = account.runningBalance,
                amount = safeAmount,
                transactionType = type,
                isCreditCard = account.isCreditCard,
                direction = BalanceCalculator.TransferDirection.NONE
            )
            account.runningBalance = newBalance

            generatedTransactions.add(
                TransactionEntity(
                    amount = safeAmount,
                    merchantName = merchantName,
                    category = category,
                    subcategory = subcategory,
                    transactionType = type,
                    dateTime = effectiveDateTime,
                    description = description,
                    balanceAfter = newBalance,
                    uuid = UUID.randomUUID().toString(),
                    isRecurring = billingCycle != null,
                    currency = account.currency,
                    fromAccount = account.accountLast4,
                    accountId = account.id,
                    fromAccountId = account.id,
                    billingCycle = billingCycle,
                    isSample = true,
                    ownerId = ownerId
                )
            )
        }

        fun recordTransferTx(
            source: SimAccount,
            destination: SimAccount,
            amount: BigDecimal,
            merchantName: String,
            category: String,
            subcategory: String?,
            dateTime: LocalDateTime
        ) {
            if (source.id == destination.id || source.currency != destination.currency) return
            val safeAmount = amount.max(BigDecimal("1.00"))
            val effectiveDateTime = advanceCursor(dateTime)

            source.runningBalance = BalanceCalculator.apply(
                currentBalance = source.runningBalance,
                amount = safeAmount,
                transactionType = TransactionType.TRANSFER,
                isCreditCard = source.isCreditCard,
                direction = BalanceCalculator.TransferDirection.SOURCE
            )
            destination.runningBalance = BalanceCalculator.apply(
                currentBalance = destination.runningBalance,
                amount = safeAmount,
                transactionType = TransactionType.TRANSFER,
                isCreditCard = destination.isCreditCard,
                direction = BalanceCalculator.TransferDirection.DESTINATION
            )

            generatedTransactions.add(
                TransactionEntity(
                    amount = safeAmount,
                    merchantName = merchantName,
                    category = category,
                    subcategory = subcategory,
                    transactionType = TransactionType.TRANSFER,
                    dateTime = effectiveDateTime,
                    description = "Transfer from ${source.bankName} to ${destination.bankName}",
                    balanceAfter = null, // Crucial: prevents shared TRANSFER row from skewing counterparty anchor balance
                    uuid = UUID.randomUUID().toString(),
                    currency = source.currency,
                    fromAccount = source.accountLast4,
                    toAccount = destination.accountLast4,
                    accountId = source.id,
                    fromAccountId = source.id,
                    toAccountId = destination.id,
                    isSample = true,
                    ownerId = ownerId
                )
            )
        }

        // Step 3A: Day 1 Opening Balances for all non-credit-card accounts
        simAccounts.filter { !it.isCreditCard }.forEachIndexed { idx, account ->
            val openingInr = if (account.isWallet) {
                random.nextInt(4_500, 9_500)
            } else if (idx == 0) {
                random.nextInt(65_000, 145_000)
            } else {
                random.nextInt(35_000, 95_000)
            }
            val openingAmount = convertFromInr(openingInr, account.currency)
            recordSingleAccountTx(
                account = account,
                amount = openingAmount,
                type = TransactionType.INCOME,
                merchantName = if (account.isWallet) "Opening Cash in Hand" else "${account.bankName} Opening Balance",
                category = "Income",
                subcategory = null,
                dateTime = startDate.plusHours(idx.toLong() + 1)
            )
        }

        // Select active subscriptions if enabled
        val chosenSubscriptions = if (config.includeSubscriptions) {
            subscriptionPool.shuffled(random).take(random.nextInt(4, 7))
        } else emptyList()

        // Step 3B: Simulate Month-by-Month and Day-by-Day across the entire duration
        val regularBankAccounts = simAccounts.filter { !it.isCreditCard && !it.isWallet }
        val creditCardAccounts = simAccounts.filter { it.isCreditCard }

        for (monthIndex in 0..totalMonths) {
            val monthStart = startDate.plusMonths(monthIndex.toLong())
            if (monthStart.isAfter(now)) break

            val progressFraction = 0.20f + (0.60f * (monthIndex.toFloat() / totalMonths.coerceAtLeast(1)))
            if (monthIndex % 3 == 0) {
                onProgress(
                    progressFraction,
                    "Simulating ${monthStart.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${monthStart.year} ledger..."
                )
            }

            val daysInMonth = monthStart.toLocalDate().lengthOfMonth()

            for (day in 1..daysInMonth) {
                val currentDayDate = monthStart.toLocalDate().withDayOfMonth(day)
                if (currentDayDate.isAfter(now.toLocalDate())) break

                // 1) Day 1: Monthly Salary & Freelance/Interest Inflows on regular bank accounts
                if (day == 1) {
                    regularBankAccounts.forEachIndexed { idx, bankAcc ->
                        val salaryInr = if (idx == 0) {
                            random.nextInt(85_000, 140_000) / 500 * 500
                        } else {
                            random.nextInt(25_000, 55_000) / 500 * 500
                        }
                        val salaryAmt = convertFromInr(salaryInr, bankAcc.currency)
                        recordSingleAccountTx(
                            account = bankAcc,
                            amount = salaryAmt,
                            type = TransactionType.INCOME,
                            merchantName = if (idx == 0) "Monthly Salary Credit" else "Freelance & Consulting Payout",
                            category = "Income",
                            subcategory = null,
                            dateTime = currentDayDate.atTime(9, random.nextInt(5, 45))
                        )
                    }
                }

                // 2) Day 5: Credit Card Bill Settlement & Inter-Account Transfers
                if (day == 5 && config.includeTransfersAndCcPayments) {
                    creditCardAccounts.forEach { ccAcc ->
                        if (ccAcc.runningBalance > BigDecimal.ZERO) {
                            val payer = regularBankAccounts.firstOrNull { it.currency == ccAcc.currency }
                            val payAmount = ccAcc.runningBalance
                            if (payer != null && payer.runningBalance > payAmount.multiply(BigDecimal("1.2"))) {
                                recordTransferTx(
                                    source = payer,
                                    destination = ccAcc,
                                    amount = payAmount,
                                    merchantName = "${ccAcc.bankName} CC Bill Payment",
                                    category = "Credit Bill",
                                    subcategory = "Credit Card",
                                    dateTime = currentDayDate.atTime(11, random.nextInt(10, 50))
                                )
                            } else {
                                // Direct repayment if CC is in a foreign currency without a matching domestic bank
                                recordSingleAccountTx(
                                    account = ccAcc,
                                    amount = payAmount,
                                    type = TransactionType.INCOME,
                                    merchantName = "${ccAcc.bankName} Auto-Debit Settlement",
                                    category = "Credit Bill",
                                    subcategory = "Credit Card",
                                    dateTime = currentDayDate.atTime(11, random.nextInt(10, 50))
                                )
                            }
                        }
                    }
                }

                // 3) Day 10: Monthly SIP / Investments
                if (day == 10 && config.includeInvestments) {
                    val invAccount = primaryAccount
                    val invInr = random.nextInt(5_000, 18_000) / 500 * 500
                    val invAmt = convertFromInr(invInr, invAccount.currency)
                    if (invAccount.runningBalance > invAmt.multiply(BigDecimal("1.5"))) {
                        val invSubcats = listOf(
                            "Mutual Funds" to "Zerodha Coin SIP",
                            "Stocks" to "Groww Equity SIP",
                            "Gold" to "Sovereign Gold Bond",
                            "PPF" to "Public Provident Fund"
                        )
                        val (subcat, merchant) = invSubcats.random(random)
                        recordSingleAccountTx(
                            account = invAccount,
                            amount = invAmt,
                            type = TransactionType.INVESTMENT,
                            merchantName = merchant,
                            category = "Investment",
                            subcategory = subcat,
                            dateTime = currentDayDate.atTime(10, random.nextInt(10, 50))
                        )
                    }
                }

                // 4) Recurring Subscriptions on their billing day
                chosenSubscriptions.forEach { sub ->
                    val shouldCharge = when (sub.billingCycle) {
                        "MONTHLY" -> day == sub.dayOfMonth
                        "YEARLY" -> day == sub.dayOfMonth && (monthIndex % 12 == 2)
                        else -> false
                    }
                    if (shouldCharge) {
                        val subTargetAccount = if (creditCardAccounts.isNotEmpty() && sub.inrAmount < 5000) {
                            creditCardAccounts.first()
                        } else {
                            primaryAccount
                        }
                        val subAmt = convertFromInr(sub.inrAmount, subTargetAccount.currency)
                        if (subTargetAccount.isCreditCard || subTargetAccount.runningBalance > subAmt.add(BigDecimal("500"))) {
                            recordSingleAccountTx(
                                account = subTargetAccount,
                                amount = subAmt,
                                type = TransactionType.EXPENSE,
                                merchantName = sub.merchantName,
                                category = sub.category,
                                subcategory = sub.subcategory,
                                dateTime = currentDayDate.atTime(8, random.nextInt(5, 55)),
                                billingCycle = sub.billingCycle
                            )
                        }
                    }
                }

                // 5) Daily Randomized Variable Spending across Categories & Subcategories
                val dailyCount = config.density.dailyTxRange.random(random)
                for (txIdx in 0 until dailyCount) {
                    val template = spendTaxonomy.random(random)
                    val merchant = template.merchants.random(random)
                    val rawInr = random.nextInt(template.minInr, template.maxInr + 1)

                    // Pick an account for this expense
                    val candidateAccount = when {
                        template.preferCreditOrBank && creditCardAccounts.isNotEmpty() && random.nextDouble() < 0.45 ->
                            creditCardAccounts.random(random)
                        template.canUseCash && rawInr < 600 && random.nextDouble() < 0.22 ->
                            cashWallet
                        else -> simAccounts.filter { !it.isWallet }.random(random)
                    }

                    val spendAmt = convertFromInr(rawInr, candidateAccount.currency)

                    // Balance Guard: ensure non-credit-card accounts NEVER go below buffer
                    val minBuffer = convertFromInr(1500, candidateAccount.currency)
                    if (!candidateAccount.isCreditCard) {
                        if (candidateAccount.isWallet && candidateAccount.runningBalance < spendAmt.add(minBuffer)) {
                            // Auto-replenish Cash Wallet via ATM Transfer from Primary Account
                            val topUpAmt = convertFromInr(random.nextInt(3000, 6000) / 500 * 500, primaryAccount.currency)
                            if (config.includeTransfersAndCcPayments && primaryAccount.runningBalance > topUpAmt.multiply(BigDecimal("1.5"))) {
                                recordTransferTx(
                                    source = primaryAccount,
                                    destination = cashWallet,
                                    amount = topUpAmt,
                                    merchantName = "ATM Cash Withdrawal",
                                    category = "Cash Withdrawal",
                                    subcategory = null,
                                    dateTime = currentDayDate.atTime(12 + txIdx, random.nextInt(5, 25))
                                )
                            } else {
                                recordSingleAccountTx(
                                    account = cashWallet,
                                    amount = topUpAmt,
                                    type = TransactionType.INCOME,
                                    merchantName = "Wallet Cash Top-Up",
                                    category = "Top-up",
                                    subcategory = null,
                                    dateTime = currentDayDate.atTime(12 + txIdx, random.nextInt(5, 25))
                                )
                            }
                        } else if (!candidateAccount.isWallet && candidateAccount.runningBalance < spendAmt.add(minBuffer)) {
                            // Top up bank account with a secondary income/refund credit so it stays healthy
                            val topUpAmt = convertFromInr(random.nextInt(15_000, 35_000) / 500 * 500, candidateAccount.currency)
                            recordSingleAccountTx(
                                account = candidateAccount,
                                amount = topUpAmt,
                                type = TransactionType.INCOME,
                                merchantName = "Interest & Cashback Credit",
                                category = "Income",
                                subcategory = null,
                                dateTime = currentDayDate.atTime(12 + txIdx, random.nextInt(5, 25))
                            )
                        }
                    } else {
                        // Credit Card limit guard
                        val limit = candidateAccount.creditLimit ?: convertFromInr(200_000, candidateAccount.currency)
                        if (candidateAccount.runningBalance.add(spendAmt) > limit.multiply(BigDecimal("0.85"))) {
                            // Settle half of CC debt first
                            val partialPay = candidateAccount.runningBalance.divide(BigDecimal("2"), 0, RoundingMode.HALF_UP)
                            if (partialPay > BigDecimal.ZERO) {
                                recordSingleAccountTx(
                                    account = candidateAccount,
                                    amount = partialPay,
                                    type = TransactionType.INCOME,
                                    merchantName = "${candidateAccount.bankName} Mid-Cycle Payment",
                                    category = "Credit Bill",
                                    subcategory = "Credit Card",
                                    dateTime = currentDayDate.atTime(12 + txIdx, random.nextInt(5, 25))
                                )
                            }
                        }
                    }

                    val hour = (13 + (txIdx * 3) + random.nextInt(0, 2)).coerceAtMost(22)
                    val minute = random.nextInt(5, 55)
                    recordSingleAccountTx(
                        account = candidateAccount,
                        amount = spendAmt,
                        type = TransactionType.EXPENSE,
                        merchantName = merchant,
                        category = template.category,
                        subcategory = template.subcategory,
                        dateTime = currentDayDate.atTime(hour, minute)
                    )
                }
            }
        }

        // Step 3C: Ensure every account ends with a non-transfer anchor transaction so
        // BalanceCalculator.calculateAuditBalances() has an exact final balanceAfter anchor
        simAccounts.forEachIndexed { idx, acc ->
            val cashbackAmt = convertFromInr(random.nextInt(50, 250), acc.currency)
            recordSingleAccountTx(
                account = acc,
                amount = cashbackAmt,
                type = if (acc.isCreditCard) TransactionType.EXPENSE else TransactionType.INCOME,
                merchantName = if (acc.isCreditCard) "Coffee Bar" else "Rewards Cashback",
                category = if (acc.isCreditCard) "Food & Drinks" else "Income",
                subcategory = if (acc.isCreditCard) "Tea & Coffee" else null,
                dateTime = now.minusMinutes((simAccounts.size - idx + 2).toLong())
            )
        }

        // 4. Verify mathematically via BalanceCalculator.calculateAuditBalances() before saving!
        onProgress(0.85f, "Auditing account ledgers & verifying balances...")
        var allHealthy = true
        for (acc in simAccounts) {
            val accTxs = generatedTransactions.filter { tx ->
                tx.accountId == acc.id ||
                    (tx.transactionType == TransactionType.TRANSFER &&
                        (tx.fromAccount == acc.accountLast4 || tx.toAccount == acc.accountLast4))
            }
            val audit = BalanceCalculator.calculateAuditBalances(
                transactions = accTxs,
                accountLast4 = acc.accountLast4,
                isCreditCard = acc.isCreditCard,
                currentBalance = acc.runningBalance
            )
            if (audit.expectedBalance.compareTo(acc.runningBalance) != 0) {
                acc.runningBalance = audit.expectedBalance
            }
            val discrepancy = audit.expectedBalance.subtract(acc.runningBalance)
            if (discrepancy.compareTo(BigDecimal.ZERO) != 0) {
                allHealthy = false
            }
        }

        // 5. Persist all generated entities inside a single database transaction
        onProgress(0.92f, "Saving ${generatedTransactions.size} transactions & accounts...")
        var budgetsCount = 0
        var subscriptionsCount = 0
        var cardsCount = 0

        database.withTransaction {
            // A) Insert AccountBalanceEntities with final verified runningBalance
            simAccounts.forEach { acc ->
                accountBalanceRepository.insertBalance(
                    AccountBalanceEntity(
                        id = acc.id,
                        bankName = acc.bankName,
                        accountLast4 = acc.accountLast4,
                        balance = acc.runningBalance,
                        currency = acc.currency,
                        isCreditCard = acc.isCreditCard,
                        isWallet = acc.isWallet,
                        creditLimit = acc.creditLimit,
                        iconResId = acc.iconResId,
                        iconName = IconResolutionUtils.resIdToName(context, acc.iconResId),
                        color = acc.colorHex,
                        isSample = true,
                        timestamp = now,
                        createdAt = startDate,
                        updatedAt = now,
                        ownerId = ownerId
                    )
                )
            }

            // B) Batch insert all generated transactions
            transactionRepository.insertTransactions(generatedTransactions)

            // C) Insert Cards if enabled
            if (config.includeCards) {
                simAccounts.filter { !it.isWallet }.forEach { acc ->
                    val cardLast4 = nextUniqueLast4()
                    cardRepository.insertCard(
                        CardEntity(
                            cardLast4 = cardLast4,
                            cardType = if (acc.isCreditCard) CardType.CREDIT else CardType.DEBIT,
                            bankName = acc.bankName,
                            accountLast4 = if (acc.isCreditCard) null else acc.accountLast4,
                            nickname = if (acc.isCreditCard) "${acc.bankName} Rewards CC" else "${acc.bankName} Platinum Debit",
                            lastBalance = acc.runningBalance,
                            lastBalanceSource = "Avl Bal: ${acc.currency} ${acc.runningBalance}",
                            lastBalanceDate = now,
                            currency = acc.currency,
                            isSample = true,
                            ownerId = ownerId
                        )
                    )
                    cardsCount++
                }
            }

            // D) Insert Budgets & Category Limits if enabled
            if (config.includeBudgets) {
                val monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0)
                val monthlyBudgetId = budgetRepository.insertBudget(
                    BudgetEntity(
                        name = "Monthly Living & Dining",
                        amount = convertFromInr(25_000, config.baseCurrency),
                        year = monthStart.year,
                        month = monthStart.monthValue,
                        currency = config.baseCurrency,
                        startDate = monthStart,
                        endDate = monthStart.plusMonths(1).minusSeconds(1),
                        periodType = BudgetPeriod.MONTHLY,
                        trackType = BudgetTrackType.ALL_TRANSACTIONS,
                        budgetType = BudgetType.EXPENSE,
                        createdAt = startDate,
                        color = "#FF9800",
                        isSample = true,
                        ownerId = ownerId
                    )
                )
                budgetRepository.addCategoryLimit(monthlyBudgetId, "Food & Drinks", convertFromInr(10_000, config.baseCurrency))
                budgetRepository.addCategoryLimit(monthlyBudgetId, "Groceries", convertFromInr(8_000, config.baseCurrency))
                budgetRepository.addCategoryLimit(monthlyBudgetId, "Transport", convertFromInr(5_000, config.baseCurrency))
                budgetsCount++

                val weeklyStart = now.with(java.time.DayOfWeek.MONDAY).withHour(0).withMinute(0).withSecond(0).withNano(0)
                budgetRepository.insertBudget(
                    BudgetEntity(
                        name = "Weekly Lifestyle & Fun",
                        amount = convertFromInr(3_500, config.baseCurrency),
                        year = weeklyStart.year,
                        month = weeklyStart.monthValue,
                        currency = config.baseCurrency,
                        startDate = weeklyStart,
                        endDate = weeklyStart.plusWeeks(1).minusSeconds(1),
                        periodType = BudgetPeriod.WEEKLY,
                        trackType = BudgetTrackType.ALL_TRANSACTIONS,
                        budgetType = BudgetType.EXPENSE,
                        createdAt = startDate,
                        color = "#9C27B0",
                        isSample = true,
                        ownerId = ownerId
                    )
                )
                budgetsCount++

                budgetRepository.insertBudget(
                    BudgetEntity(
                        name = "Emergency & Dream Travel Fund",
                        amount = convertFromInr(300_000, config.baseCurrency),
                        year = monthStart.year,
                        month = monthStart.monthValue,
                        currency = config.baseCurrency,
                        startDate = startDate,
                        endDate = now.plusYears(1),
                        periodType = BudgetPeriod.CUSTOM,
                        trackType = BudgetTrackType.ALL_TRANSACTIONS,
                        budgetType = BudgetType.SAVINGS,
                        createdAt = startDate,
                        color = "#00C853",
                        isSample = true,
                        ownerId = ownerId
                    )
                )
                budgetsCount++
            }

            // E) Insert Subscriptions if enabled
            if (config.includeSubscriptions) {
                val today = LocalDate.now()
                chosenSubscriptions.forEach { sub ->
                    val nextDate = today.withDayOfMonth(sub.dayOfMonth.coerceAtMost(today.lengthOfMonth())).let {
                        if (it.isBefore(today)) it.plusMonths(1) else it
                    }
                    subscriptionRepository.insertSubscription(
                        SubscriptionEntity(
                            merchantName = sub.merchantName,
                            amount = convertFromInr(sub.inrAmount, config.baseCurrency),
                            nextPaymentDate = nextDate,
                            billingCycle = sub.billingCycle,
                            category = sub.category,
                            subcategory = sub.subcategory,
                            bankName = primaryAccount.bankName,
                            currency = config.baseCurrency,
                            lastPaidDate = nextDate.minusMonths(1),
                            isSample = true,
                            ownerId = ownerId
                        )
                    )
                    subscriptionsCount++
                }
            }
        }

        // 6. Update preferences (Main Account, Base Currency, Sample Data flag)
        val sharedPrefs = context.getSharedPreferences("account_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit {
            putString("main_account", "${primaryAccount.bankName}_${primaryAccount.accountLast4}")
        }
        userPreferencesRepository.updateBaseCurrency(config.baseCurrency)
        userPreferencesRepository.setSampleDataSeeded(true)

        onProgress(1.0f, "Completed! Generated ${generatedTransactions.size} verified transactions.")

        RandomDataGenerationResult(
            accountsCreated = simAccounts.size,
            transactionsCreated = generatedTransactions.size,
            budgetsCreated = budgetsCount,
            subscriptionsCreated = subscriptionsCount,
            cardsCreated = cardsCount,
            currenciesUsed = simAccounts.map { it.currency }.distinct(),
            primaryBankName = primaryAccount.bankName,
            primaryAccountLast4 = primaryAccount.accountLast4,
            baseCurrency = config.baseCurrency,
            allBalancesVerified = allHealthy
        )
    }

    private suspend fun seedOfflineExchangeRates(currencies: Set<String>, now: LocalDateTime) {
        val expiresAt = now.plusYears(1)
        val nowUnix = now.toEpochSecond(ZoneOffset.UTC)
        val expiresUnix = expiresAt.toEpochSecond(ZoneOffset.UTC)
        val rates = mutableListOf<ExchangeRateEntity>()

        for (from in currencies) {
            for (to in currencies) {
                if (from == to) continue
                val fromInr = inrPerUnit[from] ?: BigDecimal("84.0")
                val toInr = inrPerUnit[to] ?: BigDecimal("1.0")
                // 1 unit of `from` = (fromInr / toInr) units of `to`
                val rate = fromInr.divide(toInr, 6, RoundingMode.HALF_UP)
                rates.add(
                    ExchangeRateEntity(
                        fromCurrency = from,
                        toCurrency = to,
                        rate = rate,
                        provider = "VittifySampleSeeder",
                        updatedAt = now,
                        updatedAtUnix = nowUnix,
                        expiresAt = expiresAt,
                        expiresAtUnix = expiresUnix,
                        isCustom = false
                    )
                )
            }
        }
        if (rates.isNotEmpty()) {
            exchangeRateDao.insertExchangeRates(rates)
        }
    }
}
