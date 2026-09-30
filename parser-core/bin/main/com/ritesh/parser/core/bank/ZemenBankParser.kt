package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Parser for Zemen Bank - handles ETB currency transactions
 */
class ZemenBankParser : BankParser() {

    override fun getBankName() = "Zemen Bank"

    override fun getCurrency() = "ETB"

    override fun canHandle(sender: String): Boolean {
        val normalized = sender.uppercase().trim()
        return normalized == "ZEMEN BANK" ||
                normalized.replace(" ", "") == "ZEMENBANK" ||
                normalized.matches(Regex("""^[A-Z]{2}-ZEMENBANK-[A-Z]$"""))
    }

    override fun extractAmount(message: String): BigDecimal? {
        val amountPattern =
            Regex("""(?:ETB|Birr)\s+([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)

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
            lowerMessage.contains("credited with") -> TransactionType.INCOME
            lowerMessage.contains("has been debited") || 
            lowerMessage.contains("debited with") ||
            lowerMessage.contains("fund transfer has been made from") ||
            lowerMessage.contains("pos transaction has been made from") ||
            lowerMessage.contains("atm cash withdrawal has been made from") ||
            lowerMessage.contains("you have transfered") ||
            lowerMessage.contains("you have transferred") ||
            (lowerMessage.contains("transferred") && lowerMessage.contains("from a/c")) -> TransactionType.EXPENSE
            else -> super.extractTransactionType(message)
        }
    }

    override fun extractMerchant(message: String, sender: String): String? {
        val telebirrFromPattern = Regex(
            """from\s+(telebirr wallet\s+\d+)\s+with reference""",
            RegexOption.IGNORE_CASE
        )
        telebirrFromPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }

        val telebirrToPattern = Regex(
            """to\s+(telebirr wallet\s+\d+)\s+with reference""",
            RegexOption.IGNORE_CASE
        )
        telebirrToPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }

        val toAccountPattern = Regex(
            """to\s+A/c\s+of\s+(\d{6,})""",
            RegexOption.IGNORE_CASE
        )
        toAccountPattern.find(message)?.let { match ->
            return match.groupValues[1].trim()
        }

        val fromOtherBankPattern = Regex(
            """from\s+([^,\.]+?)\s+with reference""",
            RegexOption.IGNORE_CASE
        )
        fromOtherBankPattern.find(message)?.let { match ->
            val merchant = cleanMerchantName(match.groupValues[1]).trim()
            if (merchant.isNotEmpty() && isValidMerchantName(merchant)) return merchant
        }

        val posPurchasePattern = Regex(
            """pos purchase transaction at\s+(.+?)\s+on\s+\d{1,2}-[A-Za-z]{3}-\d{4}""",
            RegexOption.IGNORE_CASE
        )
        posPurchasePattern.find(message)?.let { match ->
            return cleanMerchantName(match.groupValues[1]).trim()
        }

        val posLocationPattern = Regex(
            """transaction POS location is\s+(.+?)\s*\. """,
            RegexOption.IGNORE_CASE
        )
        posLocationPattern.find(message)?.let { match ->
            return match.groupValues[1].trim()
        }

        val externalBeneficiaryPattern = Regex(
            """to\s+(.+?)\s+with reference""",
            RegexOption.IGNORE_CASE
        )
        externalBeneficiaryPattern.find(message)?.let { match ->
            return match.groupValues[1].trim()
        }

        val atmLocationPattern = Regex(
            """transaction ATM location is\s+(.+?)\s*\. """,
            RegexOption.IGNORE_CASE
        )
        atmLocationPattern.find(message)?.let { match ->
            return match.groupValues[1].trim()
        }

        return super.extractMerchant(message, sender)
    }

    override fun extractAccountLast4(message: String): String? {
        val patterns = listOf(
            Regex("""\b\d{3}x+(\d{4})\b""", RegexOption.IGNORE_CASE),
            Regex("""\(\d{3}x+(\d{4})\)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            pattern.find(message)?.let { match ->
                return match.groupValues[1]
            }
        }

        return super.extractAccountLast4(message)
    }

    override fun extractBalance(message: String): BigDecimal? {
        val patterns = listOf(
            Regex("""Your\s+Current\s+Balance\s+is\s+(?:ETB|Birr)\s+([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE),
            Regex("""A/c\s+Available\s+Bal\.\s+is\s+(?:ETB|Birr)\s+([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE),
            Regex("""Your\s+available\s+balance\s+is\s+(?:ETB|Birr)\s+([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            pattern.find(message)?.let { match ->
                return parseScaledAmount(match.groupValues[1])
            }
        }

        return super.extractBalance(message)
    }

    override fun extractReference(message: String): String? {
        val txnRefPattern = Regex(
            """transaction reference number is\s+([A-Z0-9]+)""",
            RegexOption.IGNORE_CASE
        )
        txnRefPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }

        val withReferencePattern = Regex(
            """with reference\s+([A-Z0-9]+)""",
            RegexOption.IGNORE_CASE
        )
        withReferencePattern.find(message)?.let { match ->
            return match.groupValues[1]
        }

        val linkPattern = Regex(
            """(https://share\.zemenbank\.com/[^\s]+?/pdf)""",
            RegexOption.IGNORE_CASE
        )
        linkPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }

        return super.extractReference(message)
    }

    override fun isTransactionMessage(message: String): Boolean {
        val lowerMessage = message.lowercase()

        val zemenKeywords = listOf(
            "dear customer", "your account", "has been credited", "has been debited",
            "fund transfer has been made from", "pos transaction has been made from",
            "atm cash withdrawal has been made from", "current balance", "available bal.",
            "thank you for banking with zemen bank", "etb", "birr"
        )

        if (zemenKeywords.any { lowerMessage.contains(it) }) {
            return true
        }

        return super.isTransactionMessage(message)
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
