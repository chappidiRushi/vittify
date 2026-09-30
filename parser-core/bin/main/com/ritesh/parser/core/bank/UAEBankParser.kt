package com.ritesh.parser.core.bank

import com.ritesh.parser.core.CompiledPatterns
import com.ritesh.parser.core.ParsedTransaction
import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal

/**
 * Base abstract class for UAE bank parsers.
 * Handles common patterns across UAE banks (AED currency, specific transaction types, etc.).
 */
abstract class UAEBankParser : BankParser() {

    /**
     * Checks if the message contains a credit/debit card purchase pattern.
     * Common across UAE banks.
     */
    protected open fun containsCardPurchase(message: String): Boolean {
        return message.contains("Credit Card Purchase", ignoreCase = true) ||
                message.contains("Debit Card Purchase", ignoreCase = true)
    }

    override fun parse(smsBody: String, sender: String, timestamp: Long): ParsedTransaction? {
        val transaction = super.parse(smsBody, sender, timestamp) ?: return null
        val extractedCurrency = extractCurrency(smsBody)
        return if (extractedCurrency != null) {
            transaction.copy(currency = extractedCurrency)
        } else {
            transaction
        }
    }

    override fun extractCurrency(message: String): String? {
        val currencyPatterns = listOf(
            Regex("""Amount\s+([A-Z]{3})""", RegexOption.IGNORE_CASE),
            Regex("""\b([A-Z]{3})\s+[0-9,]+(?:\.\d{2})?""", RegexOption.IGNORE_CASE),
            Regex("""for\s+([A-Z]{3})\s+[0-9,]+(?:\.\d{2})?""", RegexOption.IGNORE_CASE),
            Regex("""of\s+([A-Z]{3})\s+[0-9,]+(?:\.\d{2})?""", RegexOption.IGNORE_CASE),
            Regex("""[A-Z]{3}\s+([A-Z]{3})""", RegexOption.IGNORE_CASE)
        )

        for (pattern in currencyPatterns) {
            val found = pattern.find(message)
            if (found != null) {
                for (i in 1 until found.groups.size) {
                    val groupVal = found.groupValues[i]
                    val upperVal = groupVal.uppercase()
                    if (upperVal.length == 3 && 
                        upperVal.all { it.isLetter() } && 
                        !isMonthAbbreviation(upperVal)) {
                        return upperVal
                    }
                }
            }
        }
        
        val simplePattern = Regex("""\b([A-Z]{3})\s+\d""", RegexOption.IGNORE_CASE)
        simplePattern.find(message)?.let { 
             val code = it.groupValues[1].uppercase()
             if (!isMonthAbbreviation(code)) return code
        }

        return null
    }

    override fun getCurrency() = "AED"

    override fun extractAmount(message: String): BigDecimal? {
        val patterns = listOf(
            Regex("""(?:purchase of|transfer of|amount|for|of)\s+([A-Z]{3})\s+([0-9,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE),
            Regex("""([A-Z]{3})\s+([0-9,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE),
            Regex("""([A-Z]{3})\s+\*+([0-9,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            pattern.find(message)?.let { match ->
                val currencyCode = match.groupValues[1].uppercase()
                if (isMonthAbbreviation(currencyCode)) {
                    return@let
                }

                var amountStr = match.groupValues[2].replace(",", "")
                if (amountStr.contains("*")) {
                    amountStr = amountStr.replace("*", "")
                    if (amountStr.isEmpty() || amountStr == ".") return@let
                }

                return try {
                    BigDecimal(amountStr)
                } catch (e: NumberFormatException) {
                    null
                }
            }
        }

        return super.extractAmount(message)
    }

    private fun isMonthAbbreviation(code: String): Boolean {
        val months = setOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
        return months.contains(code)
    }

    override fun extractTransactionType(message: String): TransactionType? {
        val lowerMessage = message.lowercase()

        return when {
            lowerMessage.contains("credit card purchase") -> TransactionType.CREDIT
            containsCardPurchase(message) -> TransactionType.EXPENSE
            lowerMessage.contains("cheque credited") -> TransactionType.INCOME
            lowerMessage.contains("cheque returned") -> TransactionType.EXPENSE
            lowerMessage.contains("atm cash withdrawal") || 
            (lowerMessage.contains("atm") && lowerMessage.contains("withdrawn")) -> TransactionType.EXPENSE
            lowerMessage.contains("inward remittance") -> TransactionType.INCOME
            lowerMessage.contains("cash deposit") -> TransactionType.INCOME
            lowerMessage.contains("has been credited") -> TransactionType.INCOME
            lowerMessage.contains("is credited") -> TransactionType.INCOME
            lowerMessage.contains("outward remittance") -> TransactionType.EXPENSE
            lowerMessage.contains("payment instructions") -> TransactionType.EXPENSE
            lowerMessage.contains("funds transfer request") -> TransactionType.TRANSFER
            lowerMessage.contains("has been processed") -> TransactionType.EXPENSE
            lowerMessage.contains("credit") && !lowerMessage.contains("credit card") &&
                    !lowerMessage.contains("debit") &&
                    !lowerMessage.contains("purchase") &&
                    !lowerMessage.contains("payment") -> TransactionType.INCOME
            lowerMessage.contains("debit") && !lowerMessage.contains("credit") -> TransactionType.EXPENSE
            lowerMessage.contains("purchase") -> TransactionType.EXPENSE
            lowerMessage.contains("payment") -> TransactionType.EXPENSE
            else -> super.extractTransactionType(message)
        }
    }
}
