package com.xinxu.mood.data
import org.junit.Assert.*
import org.junit.Test
import java.time.*
class InsightsTest {
    @Test fun gapsStayNullAndMultipleEntriesAreAveraged() {
        val today = LocalDate.of(2026, 10, 24); val zone = ZoneId.of("Asia/Shanghai")
        fun entry(day: LocalDate, score: Int) = MoodEntry(occurredAt = day.atStartOfDay(zone).toInstant().toEpochMilli(), feeling = score)
        val result = Insights.daily(listOf(entry(today, 1), entry(today, 5), entry(today.minusDays(2), 2), entry(today.minusDays(8), 5)), 7, today, zone)
        assertEquals(7, result.size); assertEquals(3f, result.last().average); assertEquals(2, result.last().count); assertNull(result[5].average); assertEquals(2f, result[4].average)
    }
    @Test fun localMidnightUsesUsersTimezone() {
        val entry = MoodEntry(occurredAt = Instant.parse("2026-10-23T17:00:00Z").toEpochMilli(), feeling = 4)
        val result = Insights.daily(listOf(entry), 7, LocalDate.of(2026,10,24), ZoneId.of("Asia/Shanghai"))
        assertEquals(4f, result.last().average)
    }
}
