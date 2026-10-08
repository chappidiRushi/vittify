package com.reddy.vittify.data.nlp

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class DateExtractionResult(
    val date: LocalDate?,
    val cleanedText: String
)

object NlpDateParser {

    private val MONTH_MAP = mapOf(
        "jan" to 1, "january" to 1,
        "feb" to 2, "february" to 2,
        "mar" to 3, "march" to 3,
        "apr" to 4, "april" to 4,
        "may" to 5,
        "jun" to 6, "june" to 6,
        "jul" to 7, "july" to 7,
        "aug" to 8, "august" to 8,
        "sep" to 9, "september" to 9,
        "oct" to 10, "october" to 10,
        "nov" to 11, "november" to 11,
        "dec" to 12, "december" to 12
    )

    private val DAY_OF_WEEK_MAP = mapOf(
        "monday" to DayOfWeek.MONDAY, "mon" to DayOfWeek.MONDAY,
        "tuesday" to DayOfWeek.TUESDAY, "tue" to DayOfWeek.TUESDAY, "tues" to DayOfWeek.TUESDAY,
        "wednesday" to DayOfWeek.WEDNESDAY, "wed" to DayOfWeek.WEDNESDAY,
        "thursday" to DayOfWeek.THURSDAY, "thu" to DayOfWeek.THURSDAY, "thur" to DayOfWeek.THURSDAY, "thurs" to DayOfWeek.THURSDAY,
        "friday" to DayOfWeek.FRIDAY, "fri" to DayOfWeek.FRIDAY,
        "saturday" to DayOfWeek.SATURDAY, "sat" to DayOfWeek.SATURDAY,
        "sunday" to DayOfWeek.SUNDAY, "sun" to DayOfWeek.SUNDAY
    )

    private val DAY_OF_WEEK_PATTERN =
        """\b(?:on\s+)?(?:(last\s+week|past\s+week|previous\s+week|last|past|prev|previous|this)\s+)?(monday|mon|tuesday|tue|tues|wednesday|wed|thursday|thu|thur|thurs|friday|fri|saturday|sat|sunday|sun)\b"""
            .toRegex(RegexOption.IGNORE_CASE)

    private val DAYS_AGO_PATTERN =
        """\b(?:on\s+)?(\d{1,3})\s+days?\s+ago\b"""
            .toRegex(RegexOption.IGNORE_CASE)

    private val RELATIVE_DAY_PATTERN =
        """\b(?:on\s+)?(today|tonight|this\s+morning|this\s+afternoon|this\s+evening|yesterday|yesturday|day\s+before\s+yesterday)\b"""
            .toRegex(RegexOption.IGNORE_CASE)

    // Formats like: "15 Jan", "15th Jan", "15th January 2024", "on 15th jan"
    private val DAY_MONTH_YEAR_PATTERN =
        """\b(?:on\s+|dated\s+)?(\d{1,2})(?:st|nd|rd|th)?\s+(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)(?:\s+(\d{4}))?\b"""
            .toRegex(RegexOption.IGNORE_CASE)

    // Formats like: "Jan 15", "January 15th", "Jan 15, 2024", "January 15th 2024"
    private val MONTH_DAY_YEAR_PATTERN =
        """\b(?:on\s+|dated\s+)?(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\s+(\d{1,2})(?:st|nd|rd|th)?(?:\s*,?\s*(\d{4}))?\b"""
            .toRegex(RegexOption.IGNORE_CASE)

    // ISO format: 2024-05-15 or 2024/05/15
    private val ISO_DATE_PATTERN =
        """\b(\d{4})[-/](\d{1,2})[-/](\d{1,2})\b"""
            .toRegex()

    // Numeric format: 15/05/2024 or 15-05-2024 or 15/05
    private val NUMERIC_DATE_PATTERN =
        """\b(?:on\s+)?(\d{1,2})[-/](\d{1,2})(?:[-/](\d{2,4}))?\b"""
            .toRegex(RegexOption.IGNORE_CASE)

    /**
     * Extracts date from natural language and returns both the resolved [LocalDate]
     * and the input string with the matched date token removed.
     */
    fun extractDateAndCleanInput(
        input: String,
        referenceDate: LocalDate = LocalDate.now()
    ): DateExtractionResult {
        if (input.isBlank()) return DateExtractionResult(null, input)

        // 1. Relative keywords: yesterday, today, day before yesterday, etc.
        RELATIVE_DAY_PATTERN.find(input)?.let { match ->
            val keyword = match.groups[1]?.value?.lowercase() ?: ""
            val resolvedDate = when {
                keyword.contains("day before yesterday") -> referenceDate.minusDays(2)
                keyword == "yesterday" || keyword == "yesturday" -> referenceDate.minusDays(1)
                else -> referenceDate // today, tonight, this morning, etc.
            }
            val cleaned = removeMatch(input, match.range)
            return DateExtractionResult(resolvedDate, cleaned)
        }

        // 2. "X days ago"
        DAYS_AGO_PATTERN.find(input)?.let { match ->
            val days = match.groups[1]?.value?.toLongOrNull() ?: 0L
            val resolvedDate = referenceDate.minusDays(days)
            val cleaned = removeMatch(input, match.range)
            return DateExtractionResult(resolvedDate, cleaned)
        }

        // 3. Weekday expressions: "last week monday", "last monday", "tuesday", etc.
        DAY_OF_WEEK_PATTERN.find(input)?.let { match ->
            val modifier = match.groups[1]?.value?.lowercase()?.trim() ?: ""
            val dayName = match.groups[2]?.value?.lowercase()?.trim() ?: ""
            val targetDay = DAY_OF_WEEK_MAP[dayName]
            if (targetDay != null) {
                val resolvedDate = resolveWeekday(targetDay, modifier, referenceDate)
                val cleaned = removeMatch(input, match.range)
                return DateExtractionResult(resolvedDate, cleaned)
            }
        }

        // 4. Formats with month name: "15th Jan", "15 January 2024"
        DAY_MONTH_YEAR_PATTERN.find(input)?.let { match ->
            val day = match.groups[1]?.value?.toIntOrNull()
            val monthStr = match.groups[2]?.value?.lowercase()
            val year = match.groups[3]?.value?.toIntOrNull() ?: referenceDate.year
            val month = MONTH_MAP[monthStr]

            if (day != null && month != null) {
                try {
                    val resolvedDate = LocalDate.of(year, month, day)
                    val cleaned = removeMatch(input, match.range)
                    return DateExtractionResult(resolvedDate, cleaned)
                } catch (_: Exception) {
                    // Invalid calendar date (e.g. 31 Feb)
                }
            }
        }

        // 5. Formats with month name first: "Jan 15", "January 15th 2024"
        MONTH_DAY_YEAR_PATTERN.find(input)?.let { match ->
            val monthStr = match.groups[1]?.value?.lowercase()
            val day = match.groups[2]?.value?.toIntOrNull()
            val year = match.groups[3]?.value?.toIntOrNull() ?: referenceDate.year
            val month = MONTH_MAP[monthStr]

            if (day != null && month != null) {
                try {
                    val resolvedDate = LocalDate.of(year, month, day)
                    val cleaned = removeMatch(input, match.range)
                    return DateExtractionResult(resolvedDate, cleaned)
                } catch (_: Exception) {
                    // Invalid calendar date
                }
            }
        }

        // 6. ISO dates: 2024-05-15
        ISO_DATE_PATTERN.find(input)?.let { match ->
            val year = match.groups[1]?.value?.toIntOrNull()
            val month = match.groups[2]?.value?.toIntOrNull()
            val day = match.groups[3]?.value?.toIntOrNull()
            if (year != null && month != null && day != null) {
                try {
                    val resolvedDate = LocalDate.of(year, month, day)
                    val cleaned = removeMatch(input, match.range)
                    return DateExtractionResult(resolvedDate, cleaned)
                } catch (_: Exception) {
                    // Invalid date
                }
            }
        }

        // 7. Numeric dates: 15/05/2024 or 15-05-2024
        NUMERIC_DATE_PATTERN.find(input)?.let { match ->
            val day = match.groups[1]?.value?.toIntOrNull()
            val month = match.groups[2]?.value?.toIntOrNull()
            var year = match.groups[3]?.value?.toIntOrNull() ?: referenceDate.year
            if (year < 100) year += 2000 // Handle 2-digit years like '24' -> 2024

            if (day != null && month != null && day in 1..31 && month in 1..12) {
                try {
                    val resolvedDate = LocalDate.of(year, month, day)
                    val cleaned = removeMatch(input, match.range)
                    return DateExtractionResult(resolvedDate, cleaned)
                } catch (_: Exception) {
                    // Invalid date
                }
            }
        }

        return DateExtractionResult(null, input)
    }

    private fun resolveWeekday(
        targetDay: DayOfWeek,
        modifier: String,
        referenceDate: LocalDate
    ): LocalDate {
        return when {
            modifier.contains("last week") || modifier.contains("past week") || modifier.contains("previous week") -> {
                // Monday of current week minus 7 days, then target day
                val currentWeekMonday = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val lastWeekMonday = currentWeekMonday.minusWeeks(1)
                lastWeekMonday.plusDays((targetDay.value - DayOfWeek.MONDAY.value).toLong())
            }
            modifier.contains("last") || modifier.contains("past") || modifier.contains("prev") -> {
                // Previous occurrence of targetDay
                referenceDate.with(TemporalAdjusters.previous(targetDay))
            }
            modifier.contains("this") -> {
                // Target day within the current week
                val currentWeekMonday = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                currentWeekMonday.plusDays((targetDay.value - DayOfWeek.MONDAY.value).toLong())
            }
            else -> {
                // Standalone weekday (e.g. "tuesday", "on monday"):
                // In financial logging, references to weekdays are typically past events.
                if (referenceDate.dayOfWeek == targetDay) {
                    referenceDate
                } else {
                    referenceDate.with(TemporalAdjusters.previous(targetDay))
                }
            }
        }
    }

    /**
     * Parses an ISO date string (YYYY-MM-DD or YYYY-MM-DDTHH:mm:ss) returned by AI,
     * or falls back to standard extraction if the AI returned a natural language date.
     */
    fun parseAiDate(rawDate: String, referenceDate: LocalDate = LocalDate.now()): LocalDate? {
        val trimmed = rawDate.trim()
        if (trimmed.isBlank()) return null

        // Try standard ISO LocalDate parse: YYYY-MM-DD
        try {
            return LocalDate.parse(trimmed.take(10))
        } catch (_: Exception) {}

        // Fallback to NLP extraction on rawDate
        return extractDateAndCleanInput(trimmed, referenceDate).date
    }

    private fun removeMatch(text: String, range: IntRange): String {
        val sb = StringBuilder(text)
        sb.replace(range.first, range.last + 1, " ")
        return sb.toString().replace(Regex("""\s+"""), " ").trim()
    }
}
