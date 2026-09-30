import com.ritesh.parser.core.bank.BankParserFactory


fun main() {
    val sender = "JK-NOBRKR-S"
    val parser = BankParserFactory.getParser(sender)
    println("Parser for $sender: ${parser?.getBankName() ?: "None"}")
}
