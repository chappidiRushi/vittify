package com.reddy.vittify.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class CoupleSyncSchedulerTest {

    private val oneHourMillis = 3_600_000L

    private fun calculateNextUtcHourMillis(nowMillis: Long): Long {
        val currentHourStart = nowMillis - (nowMillis % oneHourMillis)
        var nextHour = currentHourStart + oneHourMillis
        if (nextHour - nowMillis < 10_000L) {
            nextHour += oneHourMillis
        }
        return nextHour
    }

    @Test
    fun testCalculateNextUtcHourMillis_midHour() {
        // Base top of hour (divisible by 3,600,000)
        val baseHourMillis = 1_700_000_000_000L - (1_700_000_000_000L % oneHourMillis)
        val midHourMillis = baseHourMillis + (25 * 60 * 1000L) // 25 minutes into the hour

        val nextHour = calculateNextUtcHourMillis(midHourMillis)

        assertEquals("Next hour should be exactly one hour after baseHourMillis", baseHourMillis + oneHourMillis, nextHour)
        assertEquals("Target timestamp must be an exact multiple of 3600000 (top of the hour)", 0L, nextHour % oneHourMillis)
    }

    @Test
    fun testCalculateNextUtcHourMillis_exactTopBoundary() {
        val baseHourMillis = 1_700_000_000_000L - (1_700_000_000_000L % oneHourMillis)

        val nextHour = calculateNextUtcHourMillis(baseHourMillis)

        assertEquals("When called right at :00.000, next hour must be 1 hour ahead", baseHourMillis + oneHourMillis, nextHour)
    }

    @Test
    fun testCalculateNextUtcHourMillis_jitterProtectionNearBoundary() {
        val baseHourMillis = 1_700_000_000_000L - (1_700_000_000_000L % oneHourMillis)
        // Alarm fires 4 seconds before the top of the hour due to clock jitter
        val slightlyBeforeHourMillis = (baseHourMillis + oneHourMillis) - 4_000L

        val nextHour = calculateNextUtcHourMillis(slightlyBeforeHourMillis)

        assertEquals(
            "When called within 10 seconds of the top boundary, scheduler must advance to subsequent hour to prevent rapid loops",
            baseHourMillis + (2 * oneHourMillis),
            nextHour
        )
    }

    @Test
    fun testCalculateNextUtcHourMillis_justAfterBoundary() {
        val baseHourMillis = 1_700_000_000_000L - (1_700_000_000_000L % oneHourMillis)
        // 12 seconds after top of the hour
        val justAfterHourMillis = baseHourMillis + 12_000L

        val nextHour = calculateNextUtcHourMillis(justAfterHourMillis)

        assertEquals(baseHourMillis + oneHourMillis, nextHour)
    }

    @Test
    fun testUtcArithmeticMatchesJavaTimeZoned() {
        val now = Instant.now()
        val nowMillis = now.toEpochMilli()

        val expectedNextHour = ZonedDateTime.ofInstant(now, ZoneOffset.UTC)
            .truncatedTo(ChronoUnit.HOURS)
            .plusHours(1)
            .toInstant()
            .toEpochMilli()

        val calculated = calculateNextUtcHourMillis(nowMillis)

        // If now is not within 10s of the hour boundary, they should match exactly
        if (expectedNextHour - nowMillis >= 10_000L) {
            assertEquals("Calculated next UTC hour must match java.time ZonedDateTime UTC top-of-hour", expectedNextHour, calculated)
        } else {
            assertEquals(expectedNextHour + oneHourMillis, calculated)
        }
    }
}

