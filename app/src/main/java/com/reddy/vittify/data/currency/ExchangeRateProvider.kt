package com.reddy.vittify.data.currency

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.plugins.contentnegotiation.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Interface for exchange rate providers
 */
interface ExchangeRateProvider {
    suspend fun fetchExchangeRate(fromCurrency: String, toCurrency: String, date: LocalDate? = null): BigDecimal?
    suspend fun fetchAllExchangeRates(baseCurrency: String, date: LocalDate? = null): Map<String, BigDecimal>?
    suspend fun fetchAllExchangeRatesWithMetadata(baseCurrency: String, date: LocalDate? = null): ExchangeRateResponseWithMetadata?
    fun getProviderName(): String
    suspend fun getSupportedCurrencies(): List<String>
    suspend fun fetchAllCurrencies(): Map<String, String>?
}

/**
 * Implementation using the open-source fawazahmed0/exchange-api
 * Uses jsdelivr and cloudflare mirrors as primary/fallback endpoints
 */
class FreeExchangeRateProvider @Inject constructor() : ExchangeRateProvider {

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            })
        }
    }

    private fun getPrimaryUrlBase(date: LocalDate?): String {
        return if (date != null && date != LocalDate.now()) {
            val dateStr = date.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
            "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@$dateStr/v1"
        } else {
            "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1"
        }
    }

    private fun getFallbackUrlBase(date: LocalDate?): String {
        return if (date != null && date != LocalDate.now()) {
            val dateStr = date.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
            "https://$dateStr.currency-api.pages.dev/v1"
        } else {
            "https://latest.currency-api.pages.dev/v1"
        }
    }

    override suspend fun fetchExchangeRate(fromCurrency: String, toCurrency: String, date: LocalDate?): BigDecimal? {
        if (fromCurrency.equals(toCurrency, ignoreCase = true)) {
            return BigDecimal.ONE
        }

        return try {
            val rates = fetchAllExchangeRates(fromCurrency, date)
            rates?.get(toCurrency.uppercase())
        } catch (e: Exception) {
            println("Failed to fetch exchange rate from API: ${e.message}")
            null
        }
    }

    override suspend fun fetchAllExchangeRates(baseCurrency: String, date: LocalDate?): Map<String, BigDecimal>? {
        val response = fetchAllExchangeRatesWithMetadata(baseCurrency, date)
        return response?.rates
    }

    override suspend fun fetchAllExchangeRatesWithMetadata(baseCurrency: String, date: LocalDate?): ExchangeRateResponseWithMetadata? {
        val currencyCode = baseCurrency.lowercase()
        val endpoint = "currencies/$currencyCode.json"
        val primaryBase = getPrimaryUrlBase(date)
        val fallbackBase = getFallbackUrlBase(date)
        
        return try {
            withContext(Dispatchers.IO) {
                var responseBody: String? = null
                
                // Try Primary URL
                try {
                    val response = client.get("$primaryBase/$endpoint") {
                        header("User-Agent", "Vittify/1.0")
                    }
                    if (response.status.value in 200..299) {
                        responseBody = response.body<String>()
                    }
                } catch (e: Exception) {
                    println("Primary API failed, trying fallback: ${e.message}")
                }
                
                // Try Fallback URL if primary failed
                if (responseBody == null) {
                    try {
                        val response = client.get("$fallbackBase/$endpoint") {
                            header("User-Agent", "Vittify/1.0")
                        }
                        if (response.status.value in 200..299) {
                            responseBody = response.body<String>()
                        }
                    } catch (e: Exception) {
                        println("Fallback API also failed: ${e.message}")
                    }
                }

                if (responseBody != null) {
                    val json = Json { ignoreUnknownKeys = true }
                    val jsonObject = json.parseToJsonElement(responseBody).jsonObject
                    
                    val dateStr = jsonObject["date"]?.jsonPrimitive?.content ?: ""
                    val ratesObject = jsonObject[currencyCode]?.jsonObject ?: return@withContext null
                    
                    val lastUpdateTimeUnix = try {
                        LocalDate.parse(dateStr).atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
                    } catch (e: Exception) {
                        System.currentTimeMillis() / 1000
                    }
                    
                    // The API updates daily, so next update is roughly 24 hours later
                    val nextUpdateTimeUnix = lastUpdateTimeUnix + (24 * 3600)
                    
                    val ratesMap = mutableMapOf<String, BigDecimal>()
                    
                    // Always ensure the base currency has a rate of 1.0
                    ratesMap[baseCurrency.uppercase()] = BigDecimal.ONE.setScale(6, RoundingMode.HALF_UP)
                    
                    ratesObject.forEach { (key, value) ->
                        try {
                            val code = key.uppercase()
                            // Skip if already set by base override to maintain precision
                            if (code != baseCurrency.uppercase()) {
                                ratesMap[code] = BigDecimal(value.jsonPrimitive.content)
                                    .setScale(6, RoundingMode.HALF_UP)
                            }
                        } catch (e: Exception) {
                            // Skip invalid rates
                        }
                    }

                    ExchangeRateResponseWithMetadata(
                        rates = ratesMap,
                        nextUpdateTimeUnix = nextUpdateTimeUnix,
                        lastUpdateTimeUnix = lastUpdateTimeUnix,
                        provider = "fawazahmed0/currency-api",
                        baseCurrency = baseCurrency.uppercase()
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            println("Failed to fetch exchange rates: ${e.message}")
            null
        }
    }

    override fun getProviderName(): String {
        return "fawazahmed0/currency-api"
    }

    override suspend fun fetchAllCurrencies(): Map<String, String>? {
        val endpoint = "currencies.json"
        return try {
            withContext(Dispatchers.IO) {
                var responseBody: String? = null
                
                // Try Primary URL
                try {
                    val primaryBase = getPrimaryUrlBase(null)
                    val response = client.get("$primaryBase/$endpoint")
                    if (response.status.value in 200..299) {
                        responseBody = response.body<String>()
                    }
                } catch (e: Exception) {}
                
                // Try Fallback URL
                if (responseBody == null) {
                    try {
                        val fallbackBase = getFallbackUrlBase(null)
                        val response = client.get("$fallbackBase/$endpoint")
                        if (response.status.value in 200..299) {
                            responseBody = response.body<String>()
                        }
                    } catch (e: Exception) {}
                }

                if (responseBody != null) {
                    val json = Json { ignoreUnknownKeys = true }
                    val currencies = json.parseToJsonElement(responseBody).jsonObject
                    currencies.mapValues { it.value.jsonPrimitive.content }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            println("Failed to fetch currencies mapping: ${e.message}")
            null
        }
    }

    override suspend fun getSupportedCurrencies(): List<String> {
        val currencies = fetchAllCurrencies()
        return currencies?.keys?.map { it.uppercase() }?.toList() ?: listOf(
            "AED", "USD", "EUR", "GBP", "INR", "THB", "MYR", "SGD", "KWD", "KRW",
            "CAD", "AUD", "JPY", "CNY", "NPR", "ETB"
        )
    }
}

/**
 * Data class for parsing ExchangeRate-API response (Deprecated)
 */
@Serializable
data class ExchangeRateApiResponse(
    val result: String = "",
    val provider: String = "",
    val documentation: String = "",
    val terms_of_use: String = "",
    val time_last_update_unix: Long = 0,
    val time_last_update_utc: String = "",
    val time_next_update_unix: Long = 0,
    val time_next_update_utc: String = "",
    val time_eol_unix: Long = 0,
    val base_code: String = "",
    val rates: Map<String, Double> = emptyMap()
)

/**
 * Extended response data class that includes the full API response with timestamps
 */
data class ExchangeRateResponseWithMetadata(
    val rates: Map<String, BigDecimal>,
    val nextUpdateTimeUnix: Long,
    val lastUpdateTimeUnix: Long,
    val provider: String,
    val baseCurrency: String
)

/**
 * Factory for creating exchange rate providers
 */
object ExchangeRateProviderFactory {
    fun createProvider(): ExchangeRateProvider {
        return FreeExchangeRateProvider()
    }
}