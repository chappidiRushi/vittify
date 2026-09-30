package com.ritesh.parser.core.bank

import com.ritesh.parser.core.ParsedTransaction
import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal

/**
 * Parser for M-Pesa Tanzania (Vodacom) mobile money SMS messages
 */
class MPesaTanzaniaParser : BankParser() {

    override fun getBankName() = "M-Pesa Tanzania"

    override fun getCurrency() = "TZS"

    override fun canHandle(sender: String): Boolean {
        val normalizedSender = sender.uppercase()
        return normalizedSender.contains("MPESA") ||
                normalizedSender.contains("M-PESA") ||
                normalizedSender == "MPESA" ||
                normalizedSender == "M-PESA" ||
                normalizedSender.contains("VODACOM")
    }

    override fun parse(smsBody: String, sender: String, timestamp: Long): ParsedTransaction? {
        if (!smsBody.contains("TZS", ignoreCase = true)) {
            return null
        }
        return super.parse(smsBody, sender, timestamp)
    }

    override fun extractAmount(message: String): BigDecimal? {
        val tzsSpacePattern = Regex(
            """TZS\s+([0-9,]+(?:\.[0-9]{2})?)""",
            RegexOption.IGNORE_CASE
        )
        tzsSpacePattern.find(message)?.let { match ->
            val amountStr = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(amountStr)
            } catch (e: NumberFormatException) {
                null
            }
        }

        val tzsNoSpacePattern = Regex(
            """TZS([0-9,]+(?:\.[0-9]{2})?)""",
            RegexOption.IGNORE_CASE
        )
        tzsNoSpacePattern.find(message)?.let { match ->
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
            lowerMessage.contains("you have received") ||
            lowerMessage.contains("received tsh") ||
            lowerMessage.contains("received tzs") -> TransactionType.INCOME

            lowerMessage.contains("sent to") ||
            lowerMessage.contains("paid to") ||
            lowerMessage.contains("withdrawn") -> TransactionType.EXPENSE

            else -> null
        }
    }

    override fun extractMerchant(message: String, sender: String): String? {
        val fromPattern = Regex(
            """from\s+([A-Z][A-Za-z\s]+?)(?:\s*\(|$)""",
            RegexOption.IGNORE_CASE
        )
        fromPattern.find(message)?.let { match ->
            val merchant = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(merchant)) {
                return merchant
            }
        }

        val sentToPattern = Regex(
            """sent to\s+([A-Z][A-Za-z\s]+?)(?:\s*\(|$)""",
            RegexOption.IGNORE_CASE
        )
        sentToPattern.find(message)?.let { match ->
            val merchant = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(merchant)) {
                return merchant
            }
        }

        val paidToMerchantPattern = Regex(
            """paid to\s+([A-Za-z0-9\s]+?)(?:\s*\(Merchant|\s+on|\s*$)""",
            RegexOption.IGNORE_CASE
        )
        paidToMerchantPattern.find(message)?.let { match ->
            val merchant = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(merchant)) {
                return merchant
            }
        }

        val utilityPattern = Regex(
            """paid to\s+(\w+)\s+for\s+account""",
            RegexOption.IGNORE_CASE
        )
        utilityPattern.find(message)?.let { match ->
            return match.groupValues[1].trim()
        }

        return null
    }

    override fun extractBalance(message: String): BigDecimal? {
        val balancePattern = Regex(
            """New M-Pesa balance is TZS\s*([0-9,]+(?:\.[0-9]{2})?)""",
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
            """^([A-Z0-9]{10})\s+Confirmed""",
            RegexOption.IGNORE_CASE
        )
        txnIdPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }

        val txnIdAltPattern = Regex(
            """^([A-Z0-9]{10})\s+Confirmed\.""",
            RegexOption.IGNORE_CASE
        )
        txnIdAltPattern.find(message)?.let { match ->
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

    override fun isTransactionMessage(message: String): Boolean {
        val lowerMessage = message.lowercase()

        if (!lowerMessage.contains("confirmed")) {
            return false
        }

        if (!lowerMessage.contains("tzs")) {
            return false
        }

        val transactionKeywords = listOf(
            "received", "sent to", "paid to", "withdrawn", "new m-pesa balance"
        )

        return transactionKeywords.any { lowerMessage.contains(it) }
    }

    override fun cleanMerchantName(merchant: String): String {
        return merchant
            .replace(Regex("""\s*\(.*?\)\s*$"""), "")
            .replace(Regex("""\s+on\s+\d{4}.*"""), "")
            .replace(Regex("""\s+at\s+\d{2}:\d{2}.*"""), "")
            .replace(Regex("""\s*-\s*$"""), "")
            .trim()
    }
}
