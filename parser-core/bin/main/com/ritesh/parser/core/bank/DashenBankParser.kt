package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Parser for Dashen Bank - handles ETB currency transactions
 */
class DashenBankParser : BankParser() {

    override fun getBankName() = "Dashen Bank"

    override fun getCurrency() = "ETB"

    override fun canHandle(sender: String): Boolean {
        val normalized = sender.uppercase().trim()
        return normalized == "DASHENBANK"
    }

    override fun extractAmount(message: String): BigDecimal? {
        val amountPattern =
            Regex("""ETB\s+([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)

        amountPattern.find(message)?.let { match ->
            val raw = match.groupValues[1].replace(",", "")
            return parseScaledAmount(raw)
        }

        return super.extractAmount(message)
    }

    override fun extractTransactionType(message: String): TransactionType? {
        val lowerMessage = message.lowercase()

        return when {
            lowerMessage.contains("has been credited") ||
            lowerMessage.contains("credited with") ||
            lowerMessage.contains("you have received") -> TransactionType.INCOME
            lowerMessage.contains("has been debited") ||
            lowerMessage.contains("debited with") ||
            lowerMessage.contains("debited from") -> TransactionType.EXPENSE
            else -> super.extractTransactionType(message)
        }
    }

    override fun extractMerchant(message: String, sender: String): String? {
        val telebirrToPattern = Regex(
            """credited to the (Telebirr account [+\d]+)""",
            RegexOption.IGNORE_CASE
        )
        telebirrToPattern.find(message)?.let { match ->
            return match.groupValues[1].trim()
        }

        val fromPersonPattern = Regex(
            """from\s+([A-Z][A-Z\s]*?)\s+on\s+on""",
            RegexOption.IGNORE_CASE
        )
        fromPersonPattern.find(message)?.let { match ->
            val merchant = match.groupValues[1].trim()
            if (isValidMerchantName(merchant)) return merchant
        }

        val telebirrFromPattern = Regex(
            """from\s+(telebirr account number \d+\s)Ref""",
            RegexOption.IGNORE_CASE
        )
        telebirrFromPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }

        return super.extractMerchant(message, sender)
    }

    override fun extractAccountLast4(message: String): String? {
        val pattern = Regex("""(\d{4}\*+\d+)""", RegexOption.IGNORE_CASE)
        pattern.find(message)?.let { match ->
            return extractLast4Digits(match.groupValues[1])
        }
        return super.extractAccountLast4(message)
    }

    override fun extractBalance(message: String): BigDecimal? {
        val patterns = listOf(
            Regex("""Your\s+current\s+balance\s+is\s+ETB\s+([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE),
            Regex("""Your\s+account\s+balance\s+is\s+ETB\s+([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            pattern.find(message)?.let { match ->
                return parseScaledAmount(match.groupValues[1])
            }
        }

        return super.extractBalance(message)
    }

    override fun extractReference(message: String): String? {
        val receiptUrlPattern = Regex(
            """(https://receipt\.dashensuperapp\.com/receipt/[^\s]+)""",
            RegexOption.IGNORE_CASE
        )
        receiptUrlPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }

        val refNoPattern = Regex(
            """Ref\s+No:(\d+)""",
            RegexOption.IGNORE_CASE
        )
        refNoPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }

        return super.extractReference(message)
    }

    private fun parseScaledAmount(rawAmount: String): BigDecimal? {
        val normalized = rawAmount.replace(",", "")
        return try {
            BigDecimal(normalized).setScale(2, RoundingMode.HALF_UP)
        } catch (e: NumberFormatException) {
            null
        }
    }
}
