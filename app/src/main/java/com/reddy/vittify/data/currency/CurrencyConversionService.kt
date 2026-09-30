package com.reddy.vittify.data.currency

import com.reddy.vittify.data.database.dao.ExchangeRateDao
import com.reddy.vittify.data.database.entity.ExchangeRateEntity
import com.reddy.vittify.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class CurrencyConversionService @Inject constructor(
    private val exchangeRateDao: ExchangeRateDao,
    private val exchangeRateProvider: ExchangeRateProvider,
    private val userPreferencesRepository: UserPreferencesRepository?
) {
    constructor() : this(
        exchangeRateDao = object : ExchangeRateDao {
            override suspend fun insertExchangeRate(exchangeRate: ExchangeRateEntity) = 1L
            override suspend fun insertExchangeRates(exchangeRates: List<ExchangeRateEntity>) {}
            override suspend fun getExchangeRate(fromCurrency: String, toCurrency: String, currentTime: LocalDateTime) = null
            override suspend fun getHistoricalExchangeRate(fromCurrency: String, toCurrency: String, dateStr: String) = null
            override fun getExchangeRateFlow(fromCurrency: String, toCurrency: String, currentTime: LocalDateTime) = flowOf(null)
            override suspend fun getExchangeRatesForCurrency(fromCurrency: String, currentTime: LocalDateTime) = emptyList<ExchangeRateEntity>()
            override suspend fun getExchangeRatesForCurrencyUnix(fromCurrency: String, currentTimeUnix: Long) = emptyList<ExchangeRateEntity>()
            override suspend fun getAllRatesForCurrency(fromCurrency: String) = emptyList<ExchangeRateEntity>()
            override suspend fun getExpiredRates(expiryTime: LocalDateTime) = emptyList<ExchangeRateEntity>()
            override suspend fun deleteExpiredRates(expiryTime: LocalDateTime) = 0
            override suspend fun hasValidRate(fromCurrency: String, toCurrency: String, currentTime: LocalDateTime) = 0
            override suspend fun getLatestRate() = null
            override suspend fun getAvailableCurrencies(currentTime: LocalDateTime) = emptyList<String>()
            override suspend fun getMultipleRates(from1: String, to1: String, from2: String, to2: String, curr: LocalDateTime) = emptyList<ExchangeRateEntity>()
            override suspend fun getMaxExpiryTimeUnix(fromCurrency: String) = null
            override suspend fun getCustomRate(fromCurrency: String, toCurrency: String) = null
            override suspend fun getCustomRatesForCurrency(fromCurrency: String) = emptyList<ExchangeRateEntity>()
            override suspend fun upsertCustomRate(entity: ExchangeRateEntity) {}
            override suspend fun resetCustomRate(fromCurrency: String, toCurrency: String) {}
            override fun getAllRates() = flowOf(emptyList<ExchangeRateEntity>())
            override suspend fun deleteAllRates() {}
        },
        exchangeRateProvider = object : ExchangeRateProvider {
            override suspend fun fetchExchangeRate(fromCurrency: String, toCurrency: String, date: java.time.LocalDate?) = null
            override suspend fun fetchAllExchangeRates(baseCurrency: String, date: java.time.LocalDate?) = null
            override suspend fun fetchAllExchangeRatesWithMetadata(baseCurrency: String, date: java.time.LocalDate?) = null
            override fun getProviderName() = "Default"
            override suspend fun getSupportedCurrencies() = emptyList<String>()
            override suspend fun fetchAllCurrencies() = null
        },
        userPreferencesRepository = null
    )

    private val backgroundScope = CoroutineScope(Dispatchers.IO)

    // Cache rates for performance
    private val rateCache = mutableMapOf<String, BigDecimal>()
    private var lastCacheUpdate: LocalDateTime = LocalDateTime.MIN

    // Emits a new value whenever a custom rate is saved or reset, so ViewModels can react
    private val _rateChangeTrigger = MutableStateFlow(0L)
    val rateChangeTrigger: StateFlow<Long> = _rateChangeTrigger.asStateFlow()

    /**
     * Convert amount from one currency to another
     */
    open suspend fun convertAmount(
        amount: BigDecimal,
        fromCurrency: String,
        toCurrency: String,
        date: java.time.LocalDate? = null,
        forceRefresh: Boolean = false
    ): BigDecimal {
        if (fromCurrency.equals(toCurrency, ignoreCase = true)) {
            return amount
        }

        val rate = getExchangeRate(fromCurrency, toCurrency, date, forceRefresh)
        return if (rate != null) {
            amount.multiply(rate).setScale(2, RoundingMode.HALF_UP)
        } else {
            amount // Return original amount if conversion fails
        }
    }

    /**
     * Get exchange rate between two currencies
     */
    suspend fun getExchangeRate(
        fromCurrency: String,
        toCurrency: String,
        date: java.time.LocalDate? = null,
        forceRefresh: Boolean = false
    ): BigDecimal? {
        val isHistorical = date != null && date != java.time.LocalDate.now()
        val cacheKey = if (isHistorical) "${fromCurrency.uppercase()}_${toCurrency.uppercase()}_$date" else "${fromCurrency.uppercase()}_${toCurrency.uppercase()}"

        // Check cache first (unless forced refresh)
        if (!forceRefresh && isCacheValid()) {
            rateCache[cacheKey]?.let { return it }
        }

        // Check database for custom rates first (always takes priority)
        if (!forceRefresh) {
            val customRate = exchangeRateDao.getCustomRate(fromCurrency.uppercase(), toCurrency.uppercase())
            if (customRate != null) {
                updateCache(cacheKey, customRate.rate)
                return customRate.rate
            }

            val reverseCustomRate = exchangeRateDao.getCustomRate(toCurrency.uppercase(), fromCurrency.uppercase())
            if (reverseCustomRate != null) {
                try {
                    val invertedRate = BigDecimal.ONE.divide(reverseCustomRate.rate, MathContext(10))
                    updateCache(cacheKey, invertedRate)
                    return invertedRate
                } catch (_: ArithmeticException) {
                }
            }
        }

        if (isHistorical) {
            val dateStr = date.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
            val historicalRate = exchangeRateDao.getHistoricalExchangeRate(fromCurrency.uppercase(), toCurrency.uppercase(), dateStr)
            if (historicalRate != null && !forceRefresh) {
                updateCache(cacheKey, historicalRate.rate)
                return historicalRate.rate
            }

            val reverseHistoricalRate = exchangeRateDao.getHistoricalExchangeRate(toCurrency.uppercase(), fromCurrency.uppercase(), dateStr)
            if (reverseHistoricalRate != null && !forceRefresh) {
                try {
                    val invertedRate = BigDecimal.ONE.divide(reverseHistoricalRate.rate, MathContext(10))
                    updateCache(cacheKey, invertedRate)
                    return invertedRate
                } catch (_: ArithmeticException) {
                }
            }

            // Fetch historical rates from API
            val fetchedRate = fetchAndCacheHistoricalRate(fromCurrency, toCurrency, date!!)
            if (fetchedRate != null) {
                updateCache(cacheKey, fetchedRate)
                return fetchedRate
            }
        }

        // Check database for fresh rates
        val currentTime = LocalDateTime.now()
        val dbRate = exchangeRateDao.getExchangeRate(fromCurrency, toCurrency, currentTime)

        if (dbRate != null && !forceRefresh) {
            // Rate is still valid (expires_at > currentTime), use it
            updateCache(cacheKey, dbRate.rate)
            return dbRate.rate
        }

        // Check if we have any expired rate that we might be able to use if rates aren't stale overall
        if (!forceRefresh) {
            val expiredRate = exchangeRateDao.getExchangeRate(
                fromCurrency,
                toCurrency,
                currentTime.minusHours(24) // Look back up to 24 hours for expired rates
            )

            if (expiredRate != null && !areOverallRatesStale()) {
                // Use expired rate if overall rates aren't stale, but fetch fresh ones soon
                updateCache(cacheKey, expiredRate.rate)
                // Trigger background refresh for next time
                backgroundScope.launch {
                    refreshExchangeRates(listOf(fromCurrency, toCurrency, "USD"))
                }
                return expiredRate.rate
            }
        }

        // Check reverse pair (e.g., custom rate stored as INR→USD when looking for USD→INR)
        val reverseRate = exchangeRateDao.getExchangeRate(toCurrency, fromCurrency, currentTime)
        if (reverseRate != null && !forceRefresh) {
            try {
                val invertedRate = BigDecimal.ONE.divide(reverseRate.rate, MathContext(10))
                updateCache(cacheKey, invertedRate)
                return invertedRate
            } catch (_: ArithmeticException) {
                // Division by zero or non-terminating decimal — fall through to API
            }
        }

        // Fetch from API if not found, forced refresh, or rates are stale
        return fetchAndCacheRate(fromCurrency, toCurrency)
    }

    /**
     * Check if we have a valid rate for this currency pair
     */
    suspend fun hasValidRate(fromCurrency: String, toCurrency: String): Boolean {
        if (fromCurrency.equals(toCurrency, ignoreCase = true)) {
            return true
        }

        val cacheKey = "${fromCurrency.uppercase()}_${toCurrency.uppercase()}"
        if (isCacheValid() && rateCache.containsKey(cacheKey)) {
            return true
        }

        return exchangeRateDao.hasValidRate(fromCurrency, toCurrency) > 0
    }

    /**
     * Refresh exchange rates for an account's currencies
     */
    suspend fun refreshExchangeRatesForAccount(currencies: List<String>) {
        if (currencies.size < 2) return // No conversion needed for single currency

        // Get unique currencies and ensure USD is included for API compatibility
        val uniqueCurrencies = currencies.distinct().toMutableList()
        if (!uniqueCurrencies.contains("USD")) {
            uniqueCurrencies.add("USD")
        }

        refreshExchangeRates(uniqueCurrencies)
    }

    /**
     * Refresh exchange rates for specific currencies using USD as base
     */
    suspend fun refreshExchangeRates(currencies: List<String>) {
        // Use USD as the base currency for the API since it's most commonly supported
        val apiBaseCurrency = "USD"

        // Check if we need to refresh by looking at the newest rate in our database
        if (!shouldRefreshRates(apiBaseCurrency)) {
            println("Currency rates are fresh, skipping refresh")
            return // Rates are still fresh, no need to refresh
        }
        println("Currency rates are stale, refreshing from API")
        fetchAndSaveAllRates(apiBaseCurrency, currencies)
    }

    /**
     * Fetch all relevant rates from the API and save to the database.
     */
    suspend fun fetchAndSaveAllRates(baseCurrency: String, targetCurrencies: List<String> = emptyList()) {
        val response = exchangeRateProvider.fetchAllExchangeRatesWithMetadata(baseCurrency)

        if (response != null) {
            val allRates = response.rates
            val nextUpdateTime = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(response.nextUpdateTimeUnix),
                ZoneId.systemDefault()
            )
            val lastUpdateTime = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(response.lastUpdateTimeUnix),
                ZoneId.systemDefault()
            )

            val entities = mutableListOf<ExchangeRateEntity>()

            // If targetCurrencies is empty, we'll cache all received rates (usually ~150-200)
            val sourceRates = if (targetCurrencies.isEmpty()) allRates.keys else targetCurrencies

            sourceRates.forEach { toCurrency ->
                val rate = allRates[toCurrency.uppercase()] ?: allRates[toCurrency.lowercase()]
                if (rate != null) {
                    entities.add(
                        ExchangeRateEntity(
                            fromCurrency = baseCurrency.uppercase(),
                            toCurrency = toCurrency.uppercase(),
                            rate = rate,
                            provider = response.provider,
                            updatedAt = lastUpdateTime,
                            updatedAtUnix = response.lastUpdateTimeUnix,
                            expiresAt = nextUpdateTime,
                            expiresAtUnix = response.nextUpdateTimeUnix
                        )
                    )
                }
            }

            if (entities.isNotEmpty()) {
                val customRates = exchangeRateDao.getCustomRatesForCurrency(baseCurrency.uppercase())
                val customPairs = customRates.map { it.fromCurrency.uppercase() to it.toCurrency.uppercase() }.toSet()

                val filteredEntities = entities.filterNot { entity ->
                    (entity.fromCurrency.uppercase() to entity.toCurrency.uppercase()) in customPairs
                }

                if (filteredEntities.isNotEmpty()) {
                    exchangeRateDao.insertExchangeRates(filteredEntities)
                }
            }
        }
    }

    suspend fun saveCustomRate(fromCurrency: String, toCurrency: String, rate: BigDecimal) {
        val now = LocalDateTime.now()
        val entity = ExchangeRateEntity(
            fromCurrency = fromCurrency.uppercase(),
            toCurrency = toCurrency.uppercase(),
            rate = rate,
            provider = "custom",
            updatedAt = now,
            updatedAtUnix = now.atZone(ZoneId.systemDefault()).toEpochSecond(),
            expiresAt = now.plusYears(100),
            expiresAtUnix = now.plusYears(100).atZone(ZoneId.systemDefault()).toEpochSecond(),
            isCustom = true
        )
        exchangeRateDao.upsertCustomRate(entity)
        rateCache.clear()
        _rateChangeTrigger.value++
    }

    suspend fun resetCustomRate(fromCurrency: String, toCurrency: String) {
        exchangeRateDao.resetCustomRate(fromCurrency.uppercase(), toCurrency.uppercase())
        rateCache.clear()
        _rateChangeTrigger.value++
    }

    /**
     * Retrieve stored conversions for a base currency from the local database.
     */
    suspend fun getStoredConversions(baseCurrency: String): Pair<List<ExchangeRateEntity>, Long> {
        val rates = exchangeRateDao.getAllRatesForCurrency(baseCurrency.uppercase())
        val lastUpdated = rates.maxByOrNull { it.updatedAtUnix }?.updatedAtUnix ?: 0L
        return Pair(rates, lastUpdated)
    }

    private suspend fun fetchAndCacheHistoricalRate(
        fromCurrency: String,
        toCurrency: String,
        date: java.time.LocalDate
    ): BigDecimal? {
        return try {
            val baseCurrency = "USD"
            val response = exchangeRateProvider.fetchAllExchangeRatesWithMetadata(baseCurrency, date)
            if (response != null) {
                val allRates = response.rates
                val dateTime = date.atStartOfDay()
                val epochSec = dateTime.atZone(ZoneId.systemDefault()).toEpochSecond()
                val expiresDateTime = dateTime.plusYears(10)
                val expiresEpochSec = expiresDateTime.atZone(ZoneId.systemDefault()).toEpochSecond()

                val entities = allRates.map { (code, rate) ->
                    ExchangeRateEntity(
                        fromCurrency = baseCurrency.uppercase(),
                        toCurrency = code.uppercase(),
                        rate = rate,
                        provider = response.provider,
                        updatedAt = dateTime,
                        updatedAtUnix = epochSec,
                        expiresAt = expiresDateTime,
                        expiresAtUnix = expiresEpochSec
                    )
                }
                if (entities.isNotEmpty()) {
                    exchangeRateDao.insertExchangeRates(entities)
                }

                val fromRate = if (fromCurrency.equals(baseCurrency, ignoreCase = true)) BigDecimal.ONE else allRates[fromCurrency.uppercase()] ?: allRates[fromCurrency.lowercase()]
                val toRate = if (toCurrency.equals(baseCurrency, ignoreCase = true)) BigDecimal.ONE else allRates[toCurrency.uppercase()] ?: allRates[toCurrency.lowercase()]

                if (fromRate != null && toRate != null && fromRate.compareTo(BigDecimal.ZERO) != 0) {
                    toRate.divide(fromRate, MathContext(10))
                } else null
            } else null
        } catch (e: Exception) {
            println("Error fetching historical exchange rate: ${e.message}")
            null
        }
    }

    /**
     * Get the base currency for the app
     */
    private suspend fun getBaseCurrency(): String {
        return userPreferencesRepository?.baseCurrency?.first() ?: "USD"
    }

    /**
     * Fetch exchange rate from API and cache it
     */
    private suspend fun fetchAndCacheRate(fromCurrency: String, toCurrency: String): BigDecimal? {
        try {
            // Use the metadata method to get proper expiry times even for individual rates
            // We'll use USD as base since that's what the API uses and then convert
            val baseCurrency = "USD"
            val response = exchangeRateProvider.fetchAllExchangeRatesWithMetadata(baseCurrency)

            if (response != null) {
                val allRates = response.rates
                val nextUpdateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(response.nextUpdateTimeUnix),
                    ZoneId.systemDefault()
                )
                val lastUpdateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(response.lastUpdateTimeUnix),
                    ZoneId.systemDefault()
                )

                // Calculate the rate we need
                val rate = if (fromCurrency == baseCurrency) {
                    allRates[toCurrency]
                } else if (toCurrency == baseCurrency) {
                    allRates[fromCurrency]?.let { fromRate ->
                        BigDecimal.ONE.divide(fromRate, MathContext(10))
                    }
                } else {
                    // Cross-currency: fromCurrency -> USD -> toCurrency
                    val fromToUsd = allRates[fromCurrency]
                    val usdToTo = allRates[toCurrency]
                    if (fromToUsd != null && usdToTo != null) {
                        usdToTo.divide(fromToUsd, MathContext(10))
                    } else {
                        null
                    }
                }

                if (rate != null) {
                    val entity = ExchangeRateEntity(
                        fromCurrency = fromCurrency,
                        toCurrency = toCurrency,
                        rate = rate,
                        provider = response.provider,
                        updatedAt = lastUpdateTime,
                        updatedAtUnix = response.lastUpdateTimeUnix,
                        expiresAt = nextUpdateTime, // Use the API's actual next update time
                        expiresAtUnix = response.nextUpdateTimeUnix
                    )

                    exchangeRateDao.insertExchangeRate(entity)
                    val cacheKey = "${fromCurrency.uppercase()}_${toCurrency.uppercase()}"
                    updateCache(cacheKey, rate)
                    return rate
                }
            }
        } catch (e: Exception) {
            // Log error but don't crash
            println("Failed to fetch exchange rate for $fromCurrency to $toCurrency: ${e.message}")
        }

        return null
    }

    /**
     * Check if we should refresh rates for the given base currency
     * Returns true if rates are stale or we don't have any rates
     */
    private suspend fun shouldRefreshRates(baseCurrency: String): Boolean {
        val currentTimeUnix = System.currentTimeMillis() / 1000

        // Use the efficient Unix timestamp query to get the latest expiry time
        val maxExpiryTimeUnix = exchangeRateDao.getMaxExpiryTimeUnix(baseCurrency)

        // If we don't have any rates, or they're from old records (timestamp 0), or they've expired, refresh
        return maxExpiryTimeUnix == null || maxExpiryTimeUnix == 0L || maxExpiryTimeUnix < currentTimeUnix
    }

    /**
     * Check if overall rates are stale across all currencies
     */
    private suspend fun areOverallRatesStale(): Boolean {
        return shouldRefreshRates("USD") // USD is our main base currency, so check its rates
    }

    /**
     * Get information about rate freshness for debugging
     */
    suspend fun getRateFreshnessInfo(): RateFreshnessInfo {
        val currentTime = LocalDateTime.now()
        val usdRates = exchangeRateDao.getExchangeRatesForCurrency("USD", currentTime)
        val latestRate = exchangeRateDao.getLatestRate()

        return RateFreshnessInfo(
            hasValidUsdRates = usdRates.isNotEmpty(),
            validUsdRatesCount = usdRates.size,
            latestUpdateTime = latestRate?.updatedAt,
            latestExpiryTime = usdRates.maxByOrNull { it.expiresAt }?.expiresAt,
            isStale = areOverallRatesStale(),
            currentTime = currentTime
        )
    }

    /**
     * Update cache with new rate
     */
    private fun updateCache(key: String, rate: BigDecimal) {
        rateCache[key] = rate
        lastCacheUpdate = LocalDateTime.now()
    }

    /**
     * Check if cache is still valid (less than 1 hour old)
     */
    private fun isCacheValid(): Boolean {
        return lastCacheUpdate.isAfter(LocalDateTime.now().minusHours(1))
    }

    /**
     * Clear expired rates from database
     */
    suspend fun cleanupExpiredRates() {
        val expiryTime = LocalDateTime.now().minusDays(7) // Keep rates for 7 days
        exchangeRateDao.deleteExpiredRates(expiryTime)
    }

    /**
     * Get all available currencies with exchange rates
     */
    suspend fun getAvailableCurrencies(): List<String> {
        return exchangeRateDao.getAvailableCurrencies()
    }

    /**
     * Convert multiple amounts to base currency
     */
    suspend fun convertToBaseCurrency(
        transactions: List<TransactionData>,
        baseCurrency: String
    ): Map<String, BigDecimal> {
        val convertedAmounts = mutableMapOf<String, BigDecimal>()

        transactions.forEach { transaction ->
            val convertedAmount = convertAmount(
                amount = transaction.amount,
                fromCurrency = transaction.currency,
                toCurrency = baseCurrency
            )
            convertedAmounts[transaction.id] = convertedAmount
        }

        return convertedAmounts
    }

    data class TransactionData(
        val id: String,
        val amount: BigDecimal,
        val currency: String
    )

    data class RateFreshnessInfo(
        val hasValidUsdRates: Boolean,
        val validUsdRatesCount: Int,
        val latestUpdateTime: LocalDateTime?,
        val latestExpiryTime: LocalDateTime?,
        val isStale: Boolean,
        val currentTime: LocalDateTime
    )
}