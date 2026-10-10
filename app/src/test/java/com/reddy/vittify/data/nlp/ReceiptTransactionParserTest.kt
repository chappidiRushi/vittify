package com.reddy.vittify.data.nlp

import com.reddy.vittify.data.database.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class ReceiptTransactionParserTest {

    @Test
    fun testParsedReceiptDraftInstantiation() {
        val item1 = ParsedReceiptItemDraft(
            name = "Organic Milk",
            amount = "4.50",
            category = "Groceries",
            subcategory = "Dairy"
        )
        val item2 = ParsedReceiptItemDraft(
            name = "Whole Wheat Bread",
            amount = "3.20",
            category = "Groceries",
            subcategory = "Bakery"
        )

        val draft = ParsedTransactionDraft(
            amount = "7.70",
            merchant = "Trader Joe's",
            type = TransactionType.EXPENSE,
            bankName = "Visa",
            notes = "Invoice #12345",
            category = "Groceries",
            items = listOf(item1, item2)
        )

        assertEquals("7.70", draft.amount)
        assertEquals("Trader Joe's", draft.merchant)
        assertEquals(TransactionType.EXPENSE, draft.type)
        assertEquals(2, draft.items.size)
        assertEquals("Organic Milk", draft.items[0].name)
        assertEquals("4.50", draft.items[0].amount)
        assertEquals("Dairy", draft.items[0].subcategory)
    }

    @Test
    fun testReceiptDraftFallbackTotalCalculation() {
        val items = listOf(
            ParsedReceiptItemDraft(name = "Coffee", amount = "3.50", category = "Food & Drinks"),
            ParsedReceiptItemDraft(name = "Croissant", amount = "4.25", category = "Food & Drinks")
        )
        val total = items.fold(BigDecimal.ZERO) { acc, item ->
            acc + (item.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO)
        }
        assertEquals("7.75", total.stripTrailingZeros().toPlainString())
    }
}
