package com.reddy.vittify.data.parser.pdf

import com.reddy.parser.core.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class PdfStatementParserTest {

    @Test
    fun `GPayPdfParser parses income and expense correctly`() {
        val parser = GPayPdfParser()
        val text = """
            01 Aug, 2025 05:19 PM   Received from JOHN DOE
            UPI Transaction ID: 531799428648
            Paid to Punjab National Bank 8690   ₹2,000
            
            08 Aug, 2025 03:55 PM   Paid to Tej Rajput
            UPI Transaction ID: 527032400498
            Paid by Punjab National Bank 8690   ₹150
        """.trimIndent()

        val transactions = parser.parse(text)

        assertEquals(2, transactions.size)

        // Income
        assertTrue(BigDecimal("2000").compareTo(transactions[0].amount) == 0)
        assertEquals(TransactionType.INCOME, transactions[0].type)
        assertEquals("JOHN DOE", transactions[0].merchant)
        assertEquals("8690", transactions[0].accountLast4)
        assertEquals("Punjab National Bank", transactions[0].bankName)

        // Expense
        assertTrue(BigDecimal("150").compareTo(transactions[1].amount) == 0)
        assertEquals(TransactionType.EXPENSE, transactions[1].type)
        assertEquals("Tej Rajput", transactions[1].merchant)
        assertEquals("8690", transactions[1].accountLast4)
        assertEquals("Punjab National Bank", transactions[1].bankName)
    }

    @Test
    fun `GPayPdfParser handles multi-line dates and merged text correctly`() {
        val parser = GPayPdfParser()
        // Simulate a case where dates might be on their own line or text is weirdly merged
        val text = """
            01 Aug, 2025 
            05:19 PM   Received from MERCH A
            UPI Transaction ID: 123
            Paid to BANK A 1111   ₹1,000
            02 Aug, 2025 06:10 AM Paid to MERCH B UPI Transaction ID: 456 Paid by BANK B 2222 ₹500
        """.trimIndent()

        val transactions = parser.parse(text)

        assertEquals(2, transactions.size)

        assertEquals("MERCH A", transactions[0].merchant)
        assertEquals("1111", transactions[0].accountLast4)
        assertEquals("BANK A", transactions[0].bankName)
        assertTrue(BigDecimal("1000").compareTo(transactions[0].amount) == 0)

        assertEquals("MERCH B", transactions[1].merchant)
        assertEquals("2222", transactions[1].accountLast4)
        assertEquals("BANK B", transactions[1].bankName)
        assertTrue(BigDecimal("500").compareTo(transactions[1].amount) == 0)
    }

    @Test
    fun `PhonePePdfParser parses income and expense correctly`() {
        val parser = PhonePePdfParser()
        val text = """
            Oct 22, 2025 03:31 pm   Paid to JAMES
            Transaction ID: T25104615305799766265553
            UTR No. 245678238100
            Paid by 0054XXXXXXXXX8690   DEBIT   ₹300
            
            Oct 20, 2025 07:37 pm   Received from ARYAN
            Transaction ID: T2510207847448851996644
            UTR No. 529314525654
            Credited to 0054XXXXXXXXX8690   CREDIT   ₹45
        """.trimIndent()

        val transactions = parser.parse(text)

        assertEquals(2, transactions.size)

        // Expense
        assertTrue(BigDecimal("300").compareTo(transactions[0].amount) == 0)
        assertEquals(TransactionType.EXPENSE, transactions[0].type)
        assertEquals("JAMES", transactions[0].merchant)
        assertEquals("8690", transactions[0].accountLast4)

        // Income
        assertTrue(BigDecimal("45").compareTo(transactions[1].amount) == 0)
        assertEquals(TransactionType.INCOME, transactions[1].type)
        assertEquals("ARYAN", transactions[1].merchant)
        assertEquals("8690", transactions[1].accountLast4)
    }

    @Test
    fun `PhonePePdfParser handles flexible date formats and multi-line content`() {
        val parser = PhonePePdfParser()
        val text = """
            22 Oct 2025 03:31 pm Paid to MERCH A DEBIT ₹300 Transaction ID: T123
            Oct 20, 2025 
            07:37 pm Received from MERCH B CREDIT ₹45 Transaction ID: T456
        """.trimIndent()

        val transactions = parser.parse(text)

        assertEquals(2, transactions.size)

        assertEquals("MERCH A", transactions[0].merchant)
        assertEquals(TransactionType.EXPENSE, transactions[0].type)
        assertTrue(BigDecimal("300").compareTo(transactions[0].amount) == 0)

        assertEquals("MERCH B", transactions[1].merchant)
        assertEquals(TransactionType.INCOME, transactions[1].type)
        assertTrue(BigDecimal("45").compareTo(transactions[1].amount) == 0)
    }

    @Test
    fun `PhonePePdfParser handles actual log format anomalies`() {
        val parser = PhonePePdfParser()
        val text = """
            Feb 06, 2026
            04??26 pm
            DEBIT ₹106Paid to Department of Posts
            Transact ion ID T2602061626150799891151
            UTR No. 160508839338
            Paid by 0054XXXXXXXX8690
            
            Feb 04, 2026
            05??40 pm
            DEBIT ₹219.5Paid to ZOMATO
            Transact ion ID T2602041740099617814750
            UTR No. 965066555357
            Paid by 0054XXXXXXXX8690
        """.trimIndent()

        val transactions = parser.parse(text)

        assertEquals(2, transactions.size)

        // First transaction
        assertEquals("Department of Posts", transactions[0].merchant)
        assertTrue(BigDecimal("106").compareTo(transactions[0].amount) == 0)
        assertEquals(TransactionType.EXPENSE, transactions[0].type)
        assertEquals("T2602061626150799891151", transactions[0].reference)

        // Second transaction
        assertEquals("ZOMATO", transactions[1].merchant)
        assertTrue(BigDecimal("219.5").compareTo(transactions[1].amount) == 0)
        assertEquals(TransactionType.EXPENSE, transactions[1].type)
        assertEquals("T2602041740099617814750", transactions[1].reference)
    }

    @Test
    fun `PhonePePdfParser does not pick up date values as amounts for months with r or s`() {
        val parser = PhonePePdfParser()
        // April has 'r', October has 'r', August has 's'
        val text = """
            October 22, 2025 03:31 pm   Paid to JAMES
            Transaction ID: T123
            UTR No. 456
            Paid by 0054XXXXXXXXX8690   DEBIT   ₹300
            
            August 20, 2025 07:37 pm   Received from ARYAN
            Transaction ID: T456
            UTR No. 789
            Credited to 0054XXXXXXXXX8690   CREDIT   ₹45
        """.trimIndent()

        val transactions = parser.parse(text)

        assertEquals(2, transactions.size)

        // October case: If bug existed, it might have picked '22' or '2025' as amount
        assertTrue("Amount should be 300, not date part", BigDecimal("300").compareTo(transactions[0].amount) == 0)
        
        // August case: If bug existed, it might have picked '20' or '2025' as amount
        assertTrue("Amount should be 45, not date part", BigDecimal("45").compareTo(transactions[1].amount) == 0)
    }
}
