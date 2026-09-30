package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal

/**
 * Parser for Siddhartha Bank Limited (Nepal) SMS messages
 */
class SiddharthaBankParser : BankParser() {

    override fun getBankName() = "Siddhartha Bank"

    override fun getCurrency() = "NPR"

    override fun canHandle(sender: String): Boolean {
        val normalizedSender = sender.uppercase().replace("-", "_")
        return normalizedSender.contains("SBL") ||
                normalizedSender == "SBL_ALERT" ||
                normalizedSender.contains("SIDDHARTHA")
    }

    override fun extractAmount(message: String): BigDecimal? {
        val nprPattern = Regex(
            """NPR\s+([0-9,]+(?:\.\d{2})?)""",
            RegexOption.IGNORE_CASE
        )
        nprPattern.find(message)?.let { match ->
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

        if (lowerMessage.contains("withdrawn")) {
            return TransactionType.EXPENSE
        }

        if (lowerMessage.contains("deposited") || lowerMessage.contains("credited")) {
            return TransactionType.INCOME
        }

        return null
    }

    override fun extractMerchant(message: String, sender: String): String? {
        val lowerMessage = message.lowercase()

        val qrPattern = Regex(
            """qr payment to\s+([^-\n]+?)(?:\s+-|$)""",
            RegexOption.IGNORE_CASE
        )
        qrPattern.find(message)?.let { match ->
            val merchant = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(merchant)) {
                return merchant
            }
        }

        if (lowerMessage.contains("nea")) {
            return "Nepal Electricity Authority"
        }

        if (lowerMessage.contains("fund trf to") || lowerMessage.contains("fund transfer to")) {
            if (lowerMessage.contains("ibft")) {
                return "Fund Transfer (IBFT)"
            }
            return "Fund Transfer"
        }

        if (lowerMessage.contains("fund trf frm") || lowerMessage.contains("fund transfer from")) {
            if (lowerMessage.contains("ibft")) {
                return "Fund Transfer (IBFT)"
            }
            return "Fund Transfer"
        }

        if (lowerMessage.contains("deposited")) {
            return "Deposit"
        }

        return null
    }

    override fun extractAccountLast4(message: String): String? {
        val accountPattern = Regex(
            """AC\s+([X#\d]+)""",
            RegexOption.IGNORE_CASE
        )
        accountPattern.find(message)?.let { match ->
            return extractLast4Digits(match.groupValues[1])
        }

        return super.extractAccountLast4(message)
    }

    override fun extractReference(message: String): String? {
        val inPattern = Regex(
            """\(IN-(\d+)"""
        )
        inPattern.find(message)?.let { match ->
            return "IN-${match.groupValues[1]}"
        }

        val ibftPattern = Regex(
            """IBFT:(\d+)"""
        )
        ibftPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }

        val fonPattern = Regex(
            """FON:IBFT:(\d+)"""
        )
        fonPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }

        return null
    }

    override fun isTransactionMessage(message: String): Boolean {
        val lowerMessage = message.lowercase()

        if (lowerMessage.contains("otp") ||
            lowerMessage.contains("password") ||
            lowerMessage.contains("verification code")
        ) {
            return false
        }

        val hasAmount = lowerMessage.contains("npr")
        val hasTransactionKeyword = lowerMessage.contains("withdrawn") ||
                lowerMessage.contains("deposited") ||
                lowerMessage.contains("fund trf") ||
                lowerMessage.contains("fund transfer") ||
                lowerMessage.contains("qr payment")

        return hasAmount && hasTransactionKeyword
    }
}
