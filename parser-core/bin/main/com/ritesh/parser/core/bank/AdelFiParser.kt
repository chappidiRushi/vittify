package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal

/**
 * Parser for AdelFi Credit Union transactions.
 * Handles messages from sender 42141 and similar.
 */
class AdelFiParser : BankParser() {

    override fun getBankName() = "AdelFi"

    override fun getCurrency() = "USD"

    override fun canHandle(sender: String): Boolean {
        return sender.contains("42141")
    }

    override fun isTransactionMessage(message: String): Boolean {
        return message.contains("Transaction Alert from AdelFi", ignoreCase = true) &&
                message.contains("had a transaction of", ignoreCase = true)
    }

    override fun extractAmount(message: String): BigDecimal? {
        val amountPattern = Regex("""\(\$(\d+(?:\.\d{2})?)\)""")
        return amountPattern.find(message)?.let {
            it.groupValues[1].toBigDecimalOrNull()
        }
    }

    override fun extractMerchant(message: String, sender: String): String? {
        val descriptionPattern = Regex("""Description:\s*(.+?)(?:\.\s*Date:|$)""", RegexOption.IGNORE_CASE)
        return descriptionPattern.find(message)?.let { match ->
            val description = match.groupValues[1].trim()
            if (description.isNotEmpty()) {
                val cleaned = description
                    .replace(Regex("""^\d+\s+"""), "")
                    .trim()
                cleanMerchantName(cleaned)
            } else {
                null
            }
        }
    }

    override fun extractAccountLast4(message: String): String? {
        super.extractAccountLast4(message)?.let { return it }
        val accountPattern = Regex("""\*\*(\d{4})""")
        return accountPattern.find(message)?.groupValues?.get(1)
    }

    override fun extractTransactionType(message: String): TransactionType {
        return TransactionType.CREDIT
    }
}
