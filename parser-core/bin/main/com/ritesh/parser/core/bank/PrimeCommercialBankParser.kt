package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal

/*
 Parser for Prime Commercial Bank (Nepal) SMS messages.
 */
class PrimeCommercialBankParser : BankParser() {

    override fun getBankName() = "Prime Commercial Bank"

    override fun getCurrency() = "NPR"

    override fun canHandle(sender: String): Boolean {
        val normalizedSender = sender.uppercase().replace("-", "_")
        return normalizedSender.contains("PCBLNPKA") ||
                normalizedSender == "PRIME_ALERT" ||
                normalizedSender.contains("PRIME")
    }

    override fun extractAmount(message: String): BigDecimal? {
        val txnAmountPattern = Regex(
            """NPR\s+([0-9,]+\.\d{2})""",
            RegexOption.IGNORE_CASE
        )

        txnAmountPattern.find(message)?.let { match ->
            val amountStr = match.groupValues[1].replace(",", "")
            return amountStr.toBigDecimalOrNull()
        }

        return null
    }

    override fun extractTransactionType(message: String): TransactionType? {
        val lowerMessage = message.lowercase()
        return when {
            lowerMessage.contains("withdrawn") -> TransactionType.EXPENSE
            lowerMessage.contains("deposited") -> TransactionType.INCOME
            else -> null
        }
    }

    override fun extractMerchant(message: String, sender: String): String? {
        // Extract the value after "Rmk:"
        val remarkPattern = Regex("""Rmk:\s*([^.\s]+)""", RegexOption.IGNORE_CASE)
        remarkPattern.find(message)?.let { match ->
            return match.groupValues[1].trim()
        }
        return null
    }

    override fun extractAccountLast4(message: String): String? {
        // Targets the 4 digits after # in "A/C XXX#XXXX"
        val accountPattern = Regex("""#(\d{4})""", RegexOption.IGNORE_CASE)
        return accountPattern.find(message)?.groupValues?.get(1)
    }

    override fun extractReference(message: String): String? {
        // Extract date-time after "on" in "... on 01/01/2026 05:55."
        val referencePattern = Regex(
            """\bon\s+(\d{2}/\d{2}/\d{4}\s+\d{2}:\d{2})\b""",
            RegexOption.IGNORE_CASE
        )
        return referencePattern.find(message)?.groupValues?.get(1)
    }

    override fun isTransactionMessage(message: String): Boolean {
        val lower = message.lowercase()
        // Must have NPR and a movement keyword to be a transaction
        return lower.contains("npr") &&
               (lower.contains("withdrawn") || lower.contains("deposited"))
    }
}
