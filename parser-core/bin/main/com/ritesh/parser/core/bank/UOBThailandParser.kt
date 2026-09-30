package com.ritesh.parser.core.bank

/**
 * UOB Thai parser for Thai banking SMS messages.
 */
class UOBThailandParser : BaseThailandBankParser() {

    override fun getBankName() = "UOB Thailand"

    override fun canHandle(sender: String): Boolean {
        val upperSender = sender.uppercase()
        return upperSender == "UOB" ||
                upperSender.contains("UOB THAILAND") ||
                upperSender.contains("UOBTHAI")
    }
}
