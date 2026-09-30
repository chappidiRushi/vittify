package com.reddy.vittify.presentation.ui.features.sync

import com.reddy.parser.core.ParsedTransaction
import com.reddy.parser.core.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class SyncSelectionTest {

    private fun createDummyItem(id: String, daysAgo: Long, isSelected: Boolean = true): SyncMessageItem {
        val timestamp = System.currentTimeMillis() - (daysAgo * 24 * 60 * 60 * 1000L)
        val parsed = ParsedTransaction(
            amount = BigDecimal("150.00"),
            type = TransactionType.EXPENSE,
            merchant = "Merchant $id",
            reference = "REF$id",
            accountLast4 = "1234",
            balance = BigDecimal("5000.00"),
            smsBody = "Spent 150 at Merchant $id",
            sender = "HDFCBK",
            timestamp = timestamp,
            bankName = "HDFC Bank"
        )
        return SyncMessageItem(
            id = id,
            parsedTransaction = parsed,
            sender = "HDFCBK",
            smsBody = "Spent 150 at Merchant $id",
            timestamp = timestamp,
            isSelected = isSelected
        )
    }

    @Test
    fun testInitialItemsAreCheckedByDefault() {
        val items = listOf(
            createDummyItem("1", 1),
            createDummyItem("2", 2),
            createDummyItem("3", 3)
        )
        val state = SyncSmsUiState(discoveredMessages = items)
        assertEquals(3, state.totalDiscoveredCount)
        assertEquals(3, state.selectedCount)
        assertTrue(items.all { it.isSelected })
    }

    @Test
    fun testSelectAllAndUnselectAll() {
        val items = listOf(
            createDummyItem("1", 1, isSelected = false),
            createDummyItem("2", 2, isSelected = false),
            createDummyItem("3", 3, isSelected = false)
        )
        var state = SyncSmsUiState(discoveredMessages = items)
        assertEquals(0, state.selectedCount)

        // Select All
        state = state.copy(discoveredMessages = state.discoveredMessages.map { it.copy(isSelected = true) })
        assertEquals(3, state.selectedCount)

        // Unselect All
        state = state.copy(discoveredMessages = state.discoveredMessages.map { it.copy(isSelected = false) })
        assertEquals(0, state.selectedCount)
    }

    @Test
    fun testSelectBetweenTwoTransactions() {
        val items = listOf(
            createDummyItem("0", 0, isSelected = false),
            createDummyItem("1", 1, isSelected = true),  // Selected 1
            createDummyItem("2", 2, isSelected = false),
            createDummyItem("3", 3, isSelected = false),
            createDummyItem("4", 4, isSelected = true),  // Selected 2
            createDummyItem("5", 5, isSelected = false)
        )
        val state = SyncSmsUiState(discoveredMessages = items)

        assertTrue(state.canSelectBetween)
        assertEquals(4, state.selectBetweenCount) // Indices 1, 2, 3, 4 -> 4 items

        val selectedIndices = state.discoveredMessages.mapIndexedNotNull { index, item ->
            if (item.isSelected) index else null
        }
        val start = minOf(selectedIndices[0], selectedIndices[1])
        val end = maxOf(selectedIndices[0], selectedIndices[1])

        val updatedMessages = state.discoveredMessages.mapIndexed { index, item ->
            if (index in start..end) item.copy(isSelected = true) else item
        }
        val newState = state.copy(discoveredMessages = updatedMessages)

        assertEquals(4, newState.selectedCount)
        assertTrue(newState.discoveredMessages[1].isSelected)
        assertTrue(newState.discoveredMessages[2].isSelected)
        assertTrue(newState.discoveredMessages[3].isSelected)
        assertTrue(newState.discoveredMessages[4].isSelected)
        assertFalse(newState.discoveredMessages[0].isSelected)
        assertFalse(newState.discoveredMessages[5].isSelected)
    }

    @Test
    fun testCanSelectBetweenCondition() {
        // Adjacent selected items: indices 1 and 2 (difference is 1, so no messages between them)
        val itemsAdjacent = listOf(
            createDummyItem("0", 0, isSelected = false),
            createDummyItem("1", 1, isSelected = true),
            createDummyItem("2", 2, isSelected = true),
            createDummyItem("3", 3, isSelected = false)
        )
        val stateAdjacent = SyncSmsUiState(discoveredMessages = itemsAdjacent)
        assertFalse(stateAdjacent.canSelectBetween)

        // 3 items selected: canSelectBetween should be false (only enabled when exactly 2 are selected)
        val itemsThree = listOf(
            createDummyItem("0", 0, isSelected = true),
            createDummyItem("1", 1, isSelected = false),
            createDummyItem("2", 2, isSelected = true),
            createDummyItem("3", 3, isSelected = true)
        )
        val stateThree = SyncSmsUiState(discoveredMessages = itemsThree)
        assertFalse(stateThree.canSelectBetween)
    }

    @Test
    fun testMonthGrouping() {
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy")
        val septItem = createDummyItem("sept", 0) // Now (September 2026)
        val prevMonthItem = createDummyItem("aug", 35) // ~August 2026

        val septMonth = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(septItem.timestamp),
            ZoneId.systemDefault()
        ).format(formatter)

        val augMonth = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(prevMonthItem.timestamp),
            ZoneId.systemDefault()
        ).format(formatter)

        val grouped = listOf(septItem, prevMonthItem).groupBy { item ->
            LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(item.timestamp),
                ZoneId.systemDefault()
            ).format(formatter)
        }

        assertEquals(2, grouped.keys.size)
        assertTrue(grouped.containsKey(septMonth))
        assertTrue(grouped.containsKey(augMonth))
    }
}

