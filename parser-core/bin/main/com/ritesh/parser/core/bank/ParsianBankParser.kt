package com.ritesh.parser.core.bank

/**
 * Parsian Bank parser for Iranian banking SMS messages.
 */
class ParsianBankParser : BaseIranianBankParser() {

    override fun getBankName() = "Parsian Bank"

    override fun canHandle(sender: String): Boolean {
        val upperSender = sender.uppercase()
        val parsianSenders = setOf(
            "PARSIANBANK",
            "PARSIAN",
            "PARSIAN BANK",
            "PERSIANBANK",
            "PERSIAN"
        )
        return upperSender in parsianSenders
    }
}
