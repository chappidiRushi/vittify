package com.reddy.vittify.data.generator

import kotlin.random.Random

/**
 * Predefined configuration presets for random financial data generation.
 */
enum class RandomDataPreset(
    val title: String,
    val subtitle: String,
    val badge: String? = null
) {
    REALISTIC_2_YEARS(
        title = "Realistic 2 Years",
        subtitle = "4 accounts • 24 months timeline • Full category & feature mix",
        badge = "Recommended"
    ),
    MINIMAL_STARTER(
        title = "Minimal Starter",
        subtitle = "2 accounts • 6 months timeline • Lightweight spending history"
    ),
    GLOBAL_MULTI_CURRENCY(
        title = "Global Multi-Currency",
        subtitle = "6 accounts • 24 months • INR, USD, EUR & AED with offline rates",
        badge = "Multi-FX"
    ),
    SURPRISE_ME(
        title = "Surprise Me",
        subtitle = "Freshly randomized accounts, date span, currencies & spending habits"
    );

    fun toConfig(random: Random = Random(System.nanoTime())): RandomDataConfig {
        return when (this) {
            REALISTIC_2_YEARS -> RandomDataConfig(
                preset = REALISTIC_2_YEARS,
                accountCount = 4,
                historyDuration = HistoryDuration.YEARS_2,
                baseCurrency = "INR",
                isMultiCurrency = false,
                secondaryCurrencies = setOf("USD", "EUR"),
                density = TransactionDensity.REALISTIC,
                includeTransfersAndCcPayments = true,
                includeBudgets = true,
                includeSubscriptions = true,
                includeInvestments = true,
                includeCards = true
            )
            MINIMAL_STARTER -> RandomDataConfig(
                preset = MINIMAL_STARTER,
                accountCount = 2,
                historyDuration = HistoryDuration.MONTHS_6,
                baseCurrency = "INR",
                isMultiCurrency = false,
                secondaryCurrencies = emptySet(),
                density = TransactionDensity.LIGHT,
                includeTransfersAndCcPayments = true,
                includeBudgets = true,
                includeSubscriptions = true,
                includeInvestments = false,
                includeCards = true
            )
            GLOBAL_MULTI_CURRENCY -> RandomDataConfig(
                preset = GLOBAL_MULTI_CURRENCY,
                accountCount = 6,
                historyDuration = HistoryDuration.YEARS_2,
                baseCurrency = "INR",
                isMultiCurrency = true,
                secondaryCurrencies = setOf("USD", "EUR", "AED", "GBP"),
                density = TransactionDensity.HEAVY,
                includeTransfersAndCcPayments = true,
                includeBudgets = true,
                includeSubscriptions = true,
                includeInvestments = true,
                includeCards = true
            )
            SURPRISE_ME -> {
                val baseOptions = listOf("INR", "USD", "EUR", "GBP", "AED")
                val chosenBase = baseOptions.random(random)
                val multi = random.nextBoolean()
                val secondaries = if (multi) {
                    (baseOptions - chosenBase).shuffled(random).take(random.nextInt(1, 4)).toSet()
                } else {
                    emptySet()
                }
                val durations = listOf(
                    HistoryDuration.YEAR_1,
                    HistoryDuration.YEARS_2,
                    HistoryDuration.YEARS_2,
                    HistoryDuration.YEARS_3
                )
                RandomDataConfig(
                    preset = SURPRISE_ME,
                    accountCount = random.nextInt(3, 8),
                    historyDuration = durations.random(random),
                    baseCurrency = chosenBase,
                    isMultiCurrency = multi,
                    secondaryCurrencies = secondaries,
                    density = TransactionDensity.entries.random(random),
                    includeTransfersAndCcPayments = true,
                    includeBudgets = true,
                    includeSubscriptions = true,
                    includeInvestments = random.nextDouble() > 0.2,
                    includeCards = true
                )
            }
        }
    }
}

enum class HistoryDuration(
    val months: Int,
    val label: String
) {
    MONTHS_3(3, "3 Months"),
    MONTHS_6(6, "6 Months"),
    YEAR_1(12, "1 Year"),
    YEARS_2(24, "2 Years"),
    YEARS_3(36, "3 Years")
}

enum class TransactionDensity(
    val label: String,
    val avgTxPerMonth: Int,
    val dailyTxRange: IntRange
) {
    LIGHT("Light", 16, 0..1),
    REALISTIC("Realistic", 42, 1..2),
    HEAVY("Heavy", 72, 2..4)
}

data class RandomDataConfig(
    val preset: RandomDataPreset = RandomDataPreset.REALISTIC_2_YEARS,
    val accountCount: Int = 4,
    val historyDuration: HistoryDuration = HistoryDuration.YEARS_2,
    val baseCurrency: String = "INR",
    val isMultiCurrency: Boolean = false,
    val secondaryCurrencies: Set<String> = setOf("USD", "EUR"),
    val density: TransactionDensity = TransactionDensity.REALISTIC,
    val includeTransfersAndCcPayments: Boolean = true,
    val includeBudgets: Boolean = true,
    val includeSubscriptions: Boolean = true,
    val includeInvestments: Boolean = true,
    val includeCards: Boolean = true
) {
    val estimatedTransactionsCount: Int
        get() = historyDuration.months * density.avgTxPerMonth + (accountCount * 4)
}

data class RandomDataGenerationResult(
    val accountsCreated: Int,
    val transactionsCreated: Int,
    val budgetsCreated: Int,
    val subscriptionsCreated: Int,
    val cardsCreated: Int,
    val currenciesUsed: List<String>,
    val primaryBankName: String,
    val primaryAccountLast4: String,
    val baseCurrency: String,
    val allBalancesVerified: Boolean
)
