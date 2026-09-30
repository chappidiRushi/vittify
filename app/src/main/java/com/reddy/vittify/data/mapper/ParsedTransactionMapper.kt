package com.reddy.vittify.data.mapper

import com.reddy.parser.core.ParsedTransaction
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.database.entity.TransactionType
import com.reddy.vittify.presentation.common.icons.CategoryMapping
import com.reddy.vittify.utils.capitalizeFirst
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

/**
 * Maps ParsedTransaction from parser-core to TransactionEntity
 */
fun ParsedTransaction.toEntity(): TransactionEntity {
    val dateTime = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(timestamp),
        ZoneId.systemDefault()
    )

    // Normalize merchant name to proper case
    val normalizedMerchant = merchant?.let { normalizeMerchantName(it) }

    // Map TransactionType from parser-core to database entity
    val entityType = when (type) {
        com.reddy.parser.core.TransactionType.INCOME -> TransactionType.INCOME
        com.reddy.parser.core.TransactionType.EXPENSE -> TransactionType.EXPENSE
        com.reddy.parser.core.TransactionType.CREDIT -> TransactionType.CREDIT
        com.reddy.parser.core.TransactionType.TRANSFER -> TransactionType.TRANSFER
        com.reddy.parser.core.TransactionType.INVESTMENT -> TransactionType.INVESTMENT
        com.reddy.parser.core.TransactionType.BALANCE_UPDATE -> TransactionType.BALANCE_UPDATE
    }

    return TransactionEntity(
        id = 0, // Auto-generated
        amount = amount,
        merchantName = normalizedMerchant ?: "Unknown Merchant",
        category = determineCategory(merchant, entityType),
        transactionType = entityType,
        dateTime = dateTime,
        description = null,
        smsBody = smsBody,
        smsSender = sender,
        balanceAfter = balance,
        uuid = UUID.randomUUID().toString(),
        isRecurring = false, // Will be determined later
        createdAt = LocalDateTime.now(),
        updatedAt = dateTime,
        currency = currency,
        fromAccount = fromAccount,
        toAccount = toAccount,
        reference = reference
    )
}

/**
 * Normalizes merchant name to consistent format.
 * Converts all-caps to proper case, preserves already mixed case.
 */
private fun normalizeMerchantName(name: String): String {
    val trimmed = name.trim()

    // If it's all uppercase, convert to proper case
    return if (trimmed == trimmed.uppercase()) {
        trimmed.lowercase().split(" ").joinToString(" ") { word ->
            word.capitalizeFirst()
        }
    } else {
        // Already has mixed case, keep as is
        trimmed
    }
}

/**
 * Determines the category based on merchant name and transaction type.
 */
private fun determineCategory(merchant: String?, type: TransactionType): String {
    val merchantName = merchant ?: return "Miscellaneous"

    // Special handling for income transactions
    if (type == TransactionType.INCOME) {
        val merchantLower = merchantName.lowercase()
        return when {
            merchantLower.contains("salary") -> "Salary"
            merchantLower.contains("refund") -> "Refunds"
            merchantLower.contains("cashback") -> "Cashback"
            merchantLower.contains("interest") -> "Interest"
            merchantLower.contains("dividend") -> "Dividends"
            else -> "Income"
        }
    }

    // Use unified category mapping for expenses
    return CategoryMapping.getCategory(merchantName)
}

/**
 * Extension to map parser-core TransactionType to database entity TransactionType
 */
fun com.reddy.parser.core.TransactionType.toEntityType(): TransactionType {
    return when (this) {
        com.reddy.parser.core.TransactionType.INCOME -> TransactionType.INCOME
        com.reddy.parser.core.TransactionType.EXPENSE -> TransactionType.EXPENSE
        com.reddy.parser.core.TransactionType.CREDIT -> TransactionType.CREDIT
        com.reddy.parser.core.TransactionType.TRANSFER -> TransactionType.TRANSFER
        com.reddy.parser.core.TransactionType.INVESTMENT -> TransactionType.INVESTMENT
        com.reddy.parser.core.TransactionType.BALANCE_UPDATE -> TransactionType.BALANCE_UPDATE
    }
}