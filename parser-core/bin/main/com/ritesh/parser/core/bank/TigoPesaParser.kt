package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal

/**
 * Parser for Tigo Pesa / Mixx by Yas (Tanzania) mobile money SMS messages
 */
class TigoPesaParser : BankParser() {

    override fun getBankName() = "Tigo Pesa"

    override fun getCurrency() = "TZS"

    override fun canHandle(sender: String): Boolean {
        val normalizedSender = sender.uppercase()
        return normalizedSender.contains("TIGOPESA") ||
                normalizedSender.contains("TIGO PESA") ||
                normalizedSender.contains("MIXX BY YAS") ||
                normalizedSender.contains("MIXXBYYAS") ||
                normalizedSender == "TIGO" ||
                normalizedSender.startsWith("TIGOPESA")
    }

    override fun extractAmount(message: String): BigDecimal? {
        val tshPattern = Regex(
            """TSh\s*([0-9,]+(?:\.[0-9]{2})?)""",
            RegexOption.IGNORE_CASE
        )
        val transactionAmountPatterns = listOf(
            Regex("""Cash-In of TSh\s*([0-9,]+(?:\.[0-9]{2})?)""", RegexOption.IGNORE_CASE),
            Regex("""sent TSh\s*([0-9,]+(?:\.[0-9]{2})?)""", RegexOption.IGNORE_CASE),
            Regex("""received TSh\s*([0-9,]+(?:\.[0-9]{2})?)""", RegexOption.IGNORE_CASE),
            Regex("""paid TSh\s*([0-9,]+(?:\.[0-9]{2})?)""", RegexOption.IGNORE_CASE),
            Regex("""You have sent TSh\s*([0-9,]+(?:\.[0-9]{2})?)""", RegexOption.IGNORE_CASE),
            Regex("""You have paid TSh\s*([0-9,]+(?:\.[0-9]{2})?)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in transactionAmountPatterns) {
            pattern.find(message)?.let { match ->
                val amountStr = match.groupValues[1].replace(",", "")
                return try {
                    BigDecimal(amountStr)
                } catch (e: NumberFormatException) {
                    null
                }
            }
        }

        tshPattern.find(message)?.let { match ->
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
            lowerMessage.contains("cash-in") ||
            lowerMessage.contains("you have received") ||
            lowerMessage.contains("received tsh") ||
            (lowerMessage.contains("transfer successful") && lowerMessage.contains("received")) -> TransactionType.INCOME

            lowerMessage.contains("you have sent") ||
            lowerMessage.contains("you have paid") -> TransactionType.EXPENSE

            else -> null
        }
    }

    override fun extractMerchant(message: String, sender: String): String? {
        val agentPattern = Regex(
            """from Agent\s*-?\s*([A-Z][A-Za-z\s]+?)\s+is\s+successful""",
            RegexOption.IGNORE_CASE
        )
        agentPattern.find(message)?.let { match ->
            val merchant = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(merchant)) {
                return "Agent - $merchant"
            }
        }

        val toPhoneNamePattern = Regex(
            """to\s+[\dX]+\s*-\s*([A-Z][A-Za-z\s]+?)(?:\.|Total|$)""",
            RegexOption.IGNORE_CASE
        )
        toPhoneNamePattern.find(message)?.let { match ->
            val merchant = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(merchant)) {
                return merchant
            }
        }

        val paidToPattern = Regex(
            """paid\s+TSh\s*[0-9,]+(?:\.[0-9]{2})?\s+to\s+([A-Za-z0-9\s&]+?)(?:\.|Charges|$)""",
            RegexOption.IGNORE_CASE
        )
        paidToPattern.find(message)?.let { match ->
            val merchant = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(merchant)) {
                return merchant
            }
        }

        val tipsPattern = Regex(
            """from\s+(TIPS\.[A-Za-z0-9_.]+)""",
            RegexOption.IGNORE_CASE
        )
        tipsPattern.find(message)?.let { match ->
            val tipsSource = match.groupValues[1]
            return when {
                tipsSource.contains("Selcom", ignoreCase = true) -> "Selcom (TIPS Transfer)"
                tipsSource.contains("NMB", ignoreCase = true) -> "NMB Bank (TIPS Transfer)"
                tipsSource.contains("CRDB", ignoreCase = true) -> "CRDB Bank (TIPS Transfer)"
                else -> "TIPS Transfer"
            }
        }

        val simpleToPattern = Regex(
            """to\s+([A-Z][A-Za-z\s]+?)(?:\.|,|Charges|Total|$)""",
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
        val newBalancePattern = Regex(
            """New balance is TSh\s*([0-9,]+(?:\.[0-9]{2})?)""",
            RegexOption.IGNORE_CASE
        )
        newBalancePattern.find(message)?.let { match ->
            val balanceStr = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(balanceStr)
            } catch (e: NumberFormatException) {
                null
            }
        }

        val yourNewBalancePattern = Regex(
            """Your New balance is TSh\s*([0-9,]+(?:\.[0-9]{2})?)""",
            RegexOption.IGNORE_CASE
        )
        yourNewBalancePattern.find(message)?.let { match ->
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
            """TxnId:\s*(\d+)""",
            RegexOption.IGNORE_CASE
        )
        txnIdPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }

        val txnIDPattern = Regex(
            """TxnID:\s*(\d+)""",
            RegexOption.IGNORE_CASE
        )
        txnIDPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }

        val trnxIdPattern = Regex(
            """Trnx ID:\s*(\d+)""",
            RegexOption.IGNORE_CASE
        )
        trnxIdPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }

        val tipsRefPattern = Regex(
            """with TxnId:\s*\d+\.\s*([A-Z0-9_]+)""",
            RegexOption.IGNORE_CASE
        )
        tipsRefPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }

        return null
    }

    override fun isTransactionMessage(message: String): Boolean {
        val lowerMessage = message.lowercase()

        if (!lowerMessage.contains("tsh")) {
            return false
        }

        val transactionKeywords = listOf(
            "cash-in", "you have sent", "you have paid", "you have received",
            "transfer successful", "is successful", "new balance"
        )

        return transactionKeywords.any { lowerMessage.contains(it) }
    }

    override fun cleanMerchantName(merchant: String): String {
        return merchant
            .replace(Regex("""\s*\(.*?\)\s*$"""), "")
            .replace(Regex("""\s+on\s+\d{2}/.*"""), "")
            .replace(Regex("""\s*-\s*$"""), "")
            .replace(Regex("""^\s*-\s*"""), "")
            .trim()
    }
}
