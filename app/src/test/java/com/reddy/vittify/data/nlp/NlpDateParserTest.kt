package com.reddy.vittify.data.nlp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class NlpDateParserTest {

    // Fixed reference date: Wednesday, October 14, 2026
    private val refDate: LocalDate = LocalDate.of(2026, 10, 14)

    @Test
    fun testRelativeDates_Today() {
        val result = NlpDateParser.extractDateAndCleanInput("lunch 150 today", refDate)
        assertEquals(refDate, result.date)
        assertEquals("lunch 150", result.cleanedText)
    }

    @Test
    fun testRelativeDates_Yesterday() {
        val result = NlpDateParser.extractDateAndCleanInput("paid 500 for milk from ac1 yesterday", refDate)
        assertEquals(refDate.minusDays(1), result.date)
        assertEquals("paid 500 for milk from ac1", result.cleanedText)
    }

    @Test
    fun testRelativeDates_YesterdayTypo() {
        val result = NlpDateParser.extractDateAndCleanInput("paid 500 for milk yesturday", refDate)
        assertEquals(refDate.minusDays(1), result.date)
        assertEquals("paid 500 for milk", result.cleanedText)
    }

    @Test
    fun testRelativeDates_DayBeforeYesterday() {
        val result = NlpDateParser.extractDateAndCleanInput("bought shoes 2000 day before yesterday", refDate)
        assertEquals(refDate.minusDays(2), result.date)
        assertEquals("bought shoes 2000", result.cleanedText)
    }

    @Test
    fun testRelativeDates_DaysAgo() {
        val result = NlpDateParser.extractDateAndCleanInput("recharge 399 3 days ago", refDate)
        assertEquals(refDate.minusDays(3), result.date)
        assertEquals("recharge 399", result.cleanedText)
    }

    @Test
    fun testWeekday_LastMonday() {
        // refDate is Wednesday Oct 14, 2026.
        // Last Monday was Oct 12, 2026.
        val result = NlpDateParser.extractDateAndCleanInput("sent 500 to mom last monday", refDate)
        assertEquals(LocalDate.of(2026, 10, 12), result.date)
        assertEquals("sent 500 to mom", result.cleanedText)
    }

    @Test
    fun testWeekday_LastWeekMonday() {
        // refDate is Wednesday Oct 14, 2026.
        // Current week Monday is Oct 12, 2026.
        // Last week Monday was Oct 5, 2026.
        val result = NlpDateParser.extractDateAndCleanInput("transferred 1000 to John last week monday", refDate)
        assertEquals(LocalDate.of(2026, 10, 5), result.date)
        assertEquals("transferred 1000 to John", result.cleanedText)
    }

    @Test
    fun testWeekday_StandaloneTuesday() {
        // refDate is Wednesday Oct 14, 2026.
        // Standalone Tuesday should resolve to most recent past Tuesday = Oct 13, 2026.
        val result = NlpDateParser.extractDateAndCleanInput("invested 5000 in sip tuesday", refDate)
        assertEquals(LocalDate.of(2026, 10, 13), result.date)
        assertEquals("invested 5000 in sip", result.cleanedText)
    }

    @Test
    fun testWeekday_OnTuesday() {
        val result = NlpDateParser.extractDateAndCleanInput("coffee 120 on tuesday", refDate)
        assertEquals(LocalDate.of(2026, 10, 13), result.date)
        assertEquals("coffee 120", result.cleanedText)
    }

    @Test
    fun testExplicitDate_DayMonth() {
        val result = NlpDateParser.extractDateAndCleanInput("dinner 1200 on 15 Jan", refDate)
        assertEquals(LocalDate.of(2026, 1, 15), result.date)
        assertEquals("dinner 1200", result.cleanedText)
    }

    @Test
    fun testExplicitDate_DayMonthYear() {
        val result = NlpDateParser.extractDateAndCleanInput("flight 4500 on 15th January 2024", refDate)
        assertEquals(LocalDate.of(2024, 1, 15), result.date)
        assertEquals("flight 4500", result.cleanedText)
    }

    @Test
    fun testExplicitDate_MonthDay() {
        val result = NlpDateParser.extractDateAndCleanInput("books 350 Jan 15", refDate)
        assertEquals(LocalDate.of(2026, 1, 15), result.date)
        assertEquals("books 350", result.cleanedText)
    }

    @Test
    fun testExplicitDate_Iso() {
        val result = NlpDateParser.extractDateAndCleanInput("service 2500 2024-05-15", refDate)
        assertEquals(LocalDate.of(2024, 5, 15), result.date)
        assertEquals("service 2500", result.cleanedText)
    }

    @Test
    fun testExplicitDate_NumericSlash() {
        val result = NlpDateParser.extractDateAndCleanInput("groceries 800 on 15/05/2024", refDate)
        assertEquals(LocalDate.of(2024, 5, 15), result.date)
        assertEquals("groceries 800", result.cleanedText)
    }

    @Test
    fun testNoDate_PreservesText() {
        val text = "paid 500 for milk from ac1"
        val result = NlpDateParser.extractDateAndCleanInput(text, refDate)
        assertNull(result.date)
        assertEquals(text, result.cleanedText)
    }

    @Test
    fun testParseAiDate() {
        val isoDate = NlpDateParser.parseAiDate("2024-06-20", refDate)
        assertEquals(LocalDate.of(2024, 6, 20), isoDate)

        val relativeDate = NlpDateParser.parseAiDate("yesterday", refDate)
        assertEquals(refDate.minusDays(1), relativeDate)

        val blankDate = NlpDateParser.parseAiDate("", refDate)
        assertNull(blankDate)
    }
}
