package com.penumbra.tv.core

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** A daily schedule edge: the moment the filter turns on or off. */
data class Boundary(val at: Long, val turnsOn: Boolean)

/** Pure time arithmetic for the daily on/off window. Windows may cross midnight (22:00 → 07:00). */
object Schedule {
    const val MINUTES_PER_DAY = 24 * 60

    /** The latest boundary at or before [now], or null if start and end coincide. */
    fun previous(state: FilterState, now: Long, zone: ZoneId = ZoneId.systemDefault()): Boundary? {
        if (state.startMinute == state.endMinute) return null
        val time = Instant.ofEpochMilli(now).atZone(zone)
        val on = Boundary(atOrBefore(time, state.startMinute), turnsOn = true)
        val off = Boundary(atOrBefore(time, state.endMinute), turnsOn = false)
        return if (on.at >= off.at) on else off
    }

    /** The first boundary strictly after [now], or null if start and end coincide. */
    fun next(state: FilterState, now: Long, zone: ZoneId = ZoneId.systemDefault()): Boundary? {
        if (state.startMinute == state.endMinute) return null
        val time = Instant.ofEpochMilli(now).atZone(zone)
        val on = Boundary(after(time, state.startMinute), turnsOn = true)
        val off = Boundary(after(time, state.endMinute), turnsOn = false)
        return if (on.at <= off.at) on else off
    }

    private fun atOrBefore(now: ZonedDateTime, minuteOfDay: Int): Long {
        val time = minuteOfDay.toLocalTime()
        var candidate = ZonedDateTime.of(now.toLocalDate(), time, now.zone)
        if (candidate.isAfter(now)) candidate = ZonedDateTime.of(now.toLocalDate().minusDays(1), time, now.zone)
        return candidate.toInstant().toEpochMilli()
    }

    private fun after(now: ZonedDateTime, minuteOfDay: Int): Long {
        val time = minuteOfDay.toLocalTime()
        var candidate = ZonedDateTime.of(now.toLocalDate(), time, now.zone)
        if (!candidate.isAfter(now)) candidate = ZonedDateTime.of(now.toLocalDate().plusDays(1), time, now.zone)
        return candidate.toInstant().toEpochMilli()
    }

    private fun Int.toLocalTime(): LocalTime = LocalTime.of(this / 60, this % 60)
}
