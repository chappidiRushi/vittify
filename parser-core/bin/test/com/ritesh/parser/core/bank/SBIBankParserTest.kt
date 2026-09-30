package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.bank.SBIBankParser
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.DynamicTest
import java.math.BigDecimal

class SBIBankParserTest {

    @TestFactory
    fun `sbi parser handles debit alerts`(): List<DynamicTest> {
        val parser = SBIBankParser()

        ParserTestUtils.printTestHeader(
            parserName = "SBI Bank",
            bankName = parser.getBankName(),
            currency = parser.getCurrency()
        )

        val testCases = listOf(
            ParserTestCase(
                name = "Debit card transaction",
                message = "Dear Customer, transaction number 1234 for Rs.383.00 by SBI Debit Card 0000 done at merchant on 13Sep25 at 21:38:26. Your updated available balance is Rs.999999999. If not done by you, forward this SMS to 7400165218/ call 1800111109/9449112211 to block card. GOI helpline for cyber fraud 1930.",
                sender = "ATMSBI",
                expected = ExpectedTransaction(
                    amount = BigDecimal("383.00"),
                    currency = "INR",
                    type = TransactionType.EXPENSE,
                    accountLast4 = "0000"
                )
            ),
            ParserTestCase(
                name = "Standard debit message",
                message = "Rs.500 debited from A/c X1234 on 13Sep25. Avl Bal Rs.999999999",
                sender = "ATMSBI",
                expected = ExpectedTransaction(
                    amount = BigDecimal("500"),
                    currency = "INR",
                    type = TransactionType.EXPENSE,
                    accountLast4 = "1234"
                )
            ),
            ParserTestCase(
                name = "Debit by transfer message",
                message = "Dear Customer, Your A/C XXXXX901234 has a debit by transfer of Rs 230.00 on 18/09/25. Avl Bal Rs 6,500.00.-SBI",
                sender = "AD-CBSSBI-S",
                expected = ExpectedTransaction(
                    amount = BigDecimal("230.00"),
                    currency = "INR",
                    type = TransactionType.EXPENSE,
                    accountLast4 = "1234",
                    balance = BigDecimal("6500.00")
                )
            ),
            ParserTestCase(
                name = "Credit for merchant (BHPL)",
                message = "Your A/C XXXXX314502 has credit for AOFS23546782123411BHPL of Rs 10,700.00 on 02/05/22. Avl Bal Rs 13,50,000.00.-SBI",
                sender = "JD-CBSSBI",
                expected = ExpectedTransaction(
                    amount = BigDecimal("10700.00"),
                    currency = "INR",
                    type = TransactionType.INCOME,
                    accountLast4 = "4502",
                    balance = BigDecimal("1350000.00"),
                    merchant = "AOFS23546782123411BHPL"
                )
            ),
            ParserTestCase(
                name = "Credit by Cheque",
                message = "Dear Customer, Your A/C XXXXX314567 has a credit by Cheque of Rs 12,07,000.00 on 07/10/22. Avl Bal Rs 18,06,500.00.-SBI",
                sender = "AD-CBSSBI",
                expected = ExpectedTransaction(
                    amount = BigDecimal("1207000.00"),
                    currency = "INR",
                    type = TransactionType.INCOME,
                    accountLast4 = "4567",
                    balance = BigDecimal("1806500.00"),
                    merchant = "Cheque"
                )
            ),
            ParserTestCase(
                name = "Credited INR with reverse ATM suffix",
                message = "Your AC XXXXX314567 Credited INR 9,000.00 on 22/05/22 -REVERSE ATM WDL. Avl Bal INR 13,08,900.00.-SBI",
                sender = "AD-CBSSBI",
                expected = ExpectedTransaction(
                    amount = BigDecimal("9000.00"),
                    currency = "INR",
                    type = TransactionType.INCOME,
                    accountLast4 = "4567",
                    balance = BigDecimal("1308900.00"),
                    merchant = "REVERSE ATM WDL"
                )
            ),
            ParserTestCase(
                name = "Credit for Salary",
                message = "Your A/C XXXXX314567 has credit for BY SALARY of Rs 4,000.00 on 31/12/22. Avl Bal Rs 17,70,200.00.-SBI",
                sender = "JK-CBSSBI",
                expected = ExpectedTransaction(
                    amount = BigDecimal("4000.00"),
                    currency = "INR",
                    type = TransactionType.INCOME,
                    accountLast4 = "4567",
                    balance = BigDecimal("1770200.00"),
                    merchant = "BY SALARY"
                )
            ),
            ParserTestCase(
                name = "Credit for another merchant (BHPL)",
                message = "Your A/C XXXXX314567 has credit for AOFS1112345677890BHPL of Rs 66,000.00 on 01/05/22. Avl Bal Rs 13,40,000.00.-SBI",
                sender = "JK-CBSSBI",
                expected = ExpectedTransaction(
                    amount = BigDecimal("66000.00"),
                    currency = "INR",
                    type = TransactionType.INCOME,
                    accountLast4 = "4567",
                    balance = BigDecimal("1340000.00"),
                    merchant = "AOFS1112345677890BHPL"
                )
            ),
            ParserTestCase(
                name = "Issue #35: Debit with credited destination",
                message = "Dear Customer, Your a/c no. XXXXXXXX5045 is debited for Rs.500.00 on 31-03-26 and a/c XXXXXXX418 credited (IMPS Ref no ---------------). -SBI",
                sender = "VA-SBIPSG-T",
                expected = ExpectedTransaction(
                    amount = BigDecimal("500.00"),
                    currency = "INR",
                    type = TransactionType.EXPENSE,
                    accountLast4 = "5045"
                )
            )
        )

        val handleChecks = listOf(
            "ATMSBI" to true,
            "SBICRD" to true,
            "SBIBK" to true,
            "UNKNOWN" to false
        )

        return ParserTestUtils.runTestSuite(
            parser = parser,
            testCases = testCases,
            handleCases = handleChecks,
            suiteName = "SBI Parser"
        )
    }
}
