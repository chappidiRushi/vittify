package com.reddy.vittify.data.backup

import com.google.gson.*
import java.lang.reflect.Type
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Gson TypeAdapter for LocalDateTime
 */
class LocalDateTimeTypeAdapter : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    
    override fun serialize(
        src: LocalDateTime?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return JsonPrimitive(src?.format(formatter))
    }
    
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): LocalDateTime? {
        return json?.asString?.let { 
            LocalDateTime.parse(it, formatter)
        }
    }
}

/**
 * Gson TypeAdapter for BigDecimal
 */
class BigDecimalTypeAdapter : JsonSerializer<BigDecimal>, JsonDeserializer<BigDecimal> {
    override fun serialize(
        src: BigDecimal?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return JsonPrimitive(src?.toPlainString())
    }
    
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): BigDecimal? {
        return json?.asString?.let { BigDecimal(it) }
    }
}

/**
 * Gson TypeAdapter for String to safely handle legacy numeric JSON primitives
 */
class FlexibleStringTypeAdapter : JsonSerializer<String>, JsonDeserializer<String> {
    override fun serialize(
        src: String?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return JsonPrimitive(src ?: "")
    }

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): String? {
        if (json == null || json.isJsonNull) return null
        return if (json.isJsonPrimitive) {
            json.asString
        } else {
            json.toString()
        }
    }
}

/**
 * Gson JsonDeserializer for DatabaseSnapshot to guarantee that every list is non-null,
 * even when deserializing older backups or backups missing newer tables.
 */
class DatabaseSnapshotDeserializer : JsonDeserializer<DatabaseSnapshot> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): DatabaseSnapshot {
        if (!json.isJsonObject) return DatabaseSnapshot()
        val obj = json.asJsonObject

        fun <T> getList(key: String, type: Type): List<T> {
            val element = obj.get(key) ?: return emptyList()
            if (element.isJsonNull || !element.isJsonArray) return emptyList()
            return context.deserialize(element, type) ?: emptyList()
        }

        return DatabaseSnapshot(
            transactions = getList("transactions", object : com.google.gson.reflect.TypeToken<List<com.reddy.vittify.data.database.entity.TransactionEntity>>() {}.type),
            categories = getList("categories", object : com.google.gson.reflect.TypeToken<List<com.reddy.vittify.data.database.entity.CategoryEntity>>() {}.type),
            cards = getList("cards", object : com.google.gson.reflect.TypeToken<List<com.reddy.vittify.data.database.entity.CardEntity>>() {}.type),
            accountBalances = getList("account_balances", object : com.google.gson.reflect.TypeToken<List<com.reddy.vittify.data.database.entity.AccountBalanceEntity>>() {}.type),
            subscriptions = getList("subscriptions", object : com.google.gson.reflect.TypeToken<List<com.reddy.vittify.data.database.entity.SubscriptionEntity>>() {}.type),
            merchantMappings = getList("merchant_mappings", object : com.google.gson.reflect.TypeToken<List<com.reddy.vittify.data.database.entity.MerchantMappingEntity>>() {}.type),
            unrecognizedSms = getList("unrecognized_sms", object : com.google.gson.reflect.TypeToken<List<com.reddy.vittify.data.database.entity.UnrecognizedSmsEntity>>() {}.type),
            budgets = getList("budgets", object : com.google.gson.reflect.TypeToken<List<com.reddy.vittify.data.database.entity.BudgetEntity>>() {}.type),
            budgetCategoryLimits = getList("budget_category_limits", object : com.google.gson.reflect.TypeToken<List<com.reddy.vittify.data.database.entity.BudgetCategoryLimitEntity>>() {}.type),
            subcategories = getList("subcategories", object : com.google.gson.reflect.TypeToken<List<com.reddy.vittify.data.database.entity.SubcategoryEntity>>() {}.type),
            rules = getList("rules", object : com.google.gson.reflect.TypeToken<List<com.reddy.vittify.data.database.entity.RuleEntity>>() {}.type),
            ruleApplications = getList("rule_applications", object : com.google.gson.reflect.TypeToken<List<com.reddy.vittify.data.database.entity.RuleApplicationEntity>>() {}.type),
            webhookProfiles = getList("webhook_profiles", object : com.google.gson.reflect.TypeToken<List<WebhookProfileBackup>>() {}.type),
            exchangeRates = getList("exchange_rates", object : com.google.gson.reflect.TypeToken<List<com.reddy.vittify.data.database.entity.ExchangeRateEntity>>() {}.type),
            transactionItems = getList("transaction_items", object : com.google.gson.reflect.TypeToken<List<com.reddy.vittify.data.database.entity.TransactionItemEntity>>() {}.type)
        )
    }
}