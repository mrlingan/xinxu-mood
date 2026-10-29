package com.xinxu.mood.data

import java.time.*
data class DayScore(val date: LocalDate, val average: Float?, val count: Int)
object Insights {
    fun daily(entries: List<MoodEntry>, days: Int, today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): List<DayScore> {
        val groups = entries.groupBy { Instant.ofEpochMilli(it.occurredAt).atZone(zone).toLocalDate() }
        return (days - 1 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong()); val group = groups[date].orEmpty()
            DayScore(date, if (group.isEmpty()) null else group.map { it.feeling }.average().toFloat(), group.size)
        }
    }
}
