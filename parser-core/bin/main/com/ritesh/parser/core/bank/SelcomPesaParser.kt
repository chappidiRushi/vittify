package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal

/**
 * Parser for Selcom Pesa (Tanzania) mobile money SMS messages
 */
class SelcomPesaParser : BankParser() {

    override fun getBankName() = "Selcom Pesa"

    override fun getCurrency() = "TZS"

    override fun canHandle(sender: String): Boolean {
        val normalizedSender = sender.uppercase()
        return normalizedSender.contains("SELCOM") ||
                normalizedSender.contains("SELCOMPESA") ||
                normalizedSender == "SELCOM PESA" ||
                normalizedSender == "SELCOM"
    }

    override fun extractAmount(message: String): BigDecimal? {
        val tzsPattern = Regex(
            """TZS\s+([0-9,]+(?:\.[0-9]{2})?)""",
            RegexOption.IGNORE_CASE
        )
        tzsPattern.find(message)?.let { match ->
            val amountStr = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(amountStr)
            } catch (e: NumberFormatException) {
                null
            }
        }

        return null
    }

    override fun extractTransactionType(message: String): TransactionType? {
        val lowerMessage = message.lowercase()

        return when {
            lowerMessage.contains("you have received") -> TransactionType.INCOME
            lowerMessage.contains("you have sent") -> TransactionType.EXPENSE
            lowerMessage.contains("you have paid") -> TransactionType.EXPENSE
            lowerMessage.contains("you have withdrawn") -> TransactionType.EXPENSE
            else -> null
        }
    }

    override fun extractMerchant(message: String, sender: String): String? {
        val fromPattern = Regex(
            """from\s+([A-Z][A-Za-z\s]+?)(?:\s+-\s+[^(]+)?\s*\([^)]+\)""",
            RegexOption.IGNORE_CASE
        )
        fromPattern.find(message)?.let { match ->
            val merchant = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(merchant)) {
                return merchant
            }
        }

        val toNamePattern = Regex(
            """to\s+([A-Z][A-Za-z\s]+?)(?:\s+-\s+[^(]+)?\s*\([^)]+\)""",
            RegexOption.IGNORE_CASE
        )
        toNamePattern.find(message)?.let { match ->
            val merchant = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(merchant)) {
                return merchant
            }
        }

        val paidToPattern = Regex(
            """paid\s+TZS\s+[0-9,]+(?:\.[0-9]{2})?\s+to\s+([A-Za-z0-9\s]+?)(?:\s+using|\s+on)""",
            RegexOption.IGNORE_CASE
        )
        paidToPattern.find(message)?.let { match ->
            val merchant = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(merchant)) {
                return merchant
            }
        }

        if (message.contains("withdrawn", ignoreCase = true) && message.contains("ATM", ignoreCase = true)) {
            val atmPattern = Regex(
                """at\s+ATM\s+-?\s*([^u]+?)(?:\s+using|$)""",
                RegexOption.IGNORE_CASE
            )
            atmPattern.find(message)?.let { match ->
                val location = match.groupValues[1].trim()
                return if (location.isNotEmpty()) "ATM - $location" else "ATM Withdrawal"
            }
            return "ATM Withdrawal"
        }

        val simpleToPattern = Regex(
            """to\s+([A-Z][A-Za-z\s]+?)(?:\s+on\s+|\s*$)""",
            RegexOption.IGNORE_CASE
        )
        simpleToPattern.find(message)?.let { match ->
            val merchant = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(merchant)) {
                return merchant
            }
        }

        return null
    }

    override fun extractBalance(message: String): BigDecimal? {
        val balancePattern = Regex(
            """Updated balance is TZS\s+([0-9,]+(?:\.[0-9]{2})?)""",
            RegexOption.IGNORE_CASE
        )
        balancePattern.find(message)?.let { match ->
            val balanceStr = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(balanceStr)
            } catch (e: NumberFormatException) {
                null
            }
        }

        return null
    }

    override fun extractReference(message: String): String? {
        val txnIdPattern = Regex(
            """^([A-Z0-9]{8,9})\s+(?:Confirmed|Accepted)""",
            RegexOption.IGNORE_CASE
        )
        txnIdPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }

        val tipsPattern = Regex(
            """TIPS\s+Reference[:\s]+([A-Z0-9]+)""",
            RegexOption.IGNORE_CASE
        )
        tipsPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }

        return null
    }

    override fun extractAccountLast4(message: String): String? {
        super.extractAccountLast4(message)?.let { return it }
        val cardPattern = Regex(
            """card\s+ending\s+(?:with\s+)?(\d{4})""",
            RegexOption.IGNORE_CASE
        )
        cardPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }

        return null
    }

    override fun isTransactionMessage(message: String): Boolean {
        val lowerMessage = message.lowercase()

        if (!lowerMessage.contains("confirmed") && !lowerMessage.contains("accepted")) {
            return false
        }

        val transactionKeywords = listOf(
            "you have received", "you have sent", "you have paid", "you have withdrawn", "updated balance"
        )

        return transactionKeywords.any { lowerMessage.contains(it) }
    }

    override fun detectIsCard(message: String): Boolean {
        val lowerMessage = message.lowercase()
        return lowerMessage.contains("card ending") ||
                lowerMessage.contains("using your card")
    }

    override fun cleanMerchantName(merchant: String): String {
        return merchant
            .replace(Regex("""\s*\(.*?\)\s*$"""), "")
            .replace(Regex("""\s+-\s+.*$"""), "")
            .replace(Regex("""\s+on\s+\d{4}.*"""), "")
            .replace(Regex("""\s*-\s*$"""), "")
            .trim()
    }
}
