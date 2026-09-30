package com.ritesh.parser.core.bank

/**
 * TMBThanachart Bank (TTB) parser for Thai banking SMS messages.
 */
class TTBBankParser : BaseThailandBankParser() {

    override fun getBankName() = "TTB"

    override fun canHandle(sender: String): Boolean {
        val upperSender = sender.uppercase()
        return upperSender == "TTB" ||
                upperSender.contains("THANACHART") ||
                upperSender.contains("TMB")
    }
}
