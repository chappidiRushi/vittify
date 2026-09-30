package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal

/**
 * Parser for BPCE (Banque Populaire Caisse d'Épargne) SMS messages (France).
 * Handles messages from sender "38015".
 */
class BPCEParser : BankParser() {

    override fun getBankName() = "BPCE"

    override fun getCurrency() = "EUR"

    override fun canHandle(sender: String): Boolean {
        return sender == "38015"
    }

    override fun isTransactionMessage(message: String): Boolean {
        val lowerMessage = message.lowercase()
        
        // Skip addition of beneficiary - not a transaction
        if (lowerMessage.contains("ajout d'un bénéficiaire")) {
            return false
        }

        // Must contain transaction keywords or specific BPCE patterns
        return lowerMessage.contains("virement instantané") ||
                super.isTransactionMessage(message)
    }

    override fun extractAmount(message: String): BigDecimal? {
        // Pattern 1: de 1000,00 EUR
        val patterns = listOf(
            Regex("""de\s+([0-9,]+(?:\.\d{2})?)\s+EUR""", RegexOption.IGNORE_CASE),
            Regex("""([0-9,]+(?:\.\d{2})?)\s*EUR""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            pattern.find(message)?.let { match ->
                // In French locale, comma is the decimal separator
                val amountStr = match.groupValues[1].replace(",", ".")
                return try {
                    BigDecimal(amountStr)
                } catch (e: NumberFormatException) {
                    null
                }
            }
        }

        return super.extractAmount(message)
    }

    override fun extractTransactionType(message: String): TransactionType? {
        val lowerMessage = message.lowercase()

        return when {
            // "virement instantané" is an outgoing transfer (Expense)
            lowerMessage.contains("virement instantané") -> TransactionType.EXPENSE
            
            // Standard keywords handled by BankParser (debited, credited, etc.)
            else -> super.extractTransactionType(message)
        }
    }

    override fun extractMerchant(message: String, sender: String): String? {
        // Pattern: "vers NAME FIRST NAME" or "vers PAYPAL"
        val patterns = listOf(
            Regex("""vers\s+([^.\n]+?)(?:\s+du\s+|\s+le\s+|$)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            pattern.find(message)?.let { match ->
                val merchant = cleanMerchantName(match.groupValues[1].trim())
                if (isValidMerchantName(merchant)) {
                    return merchant
                }
            }
        }

        return super.extractMerchant(message, sender)
    }

    override fun extractAccountLast4(message: String): String? {
        // BPCE SMS examples don't show account digits, let's try base patterns just in case
        return super.extractAccountLast4(message)
    }
}
