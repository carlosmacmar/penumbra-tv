package com.penumbra.tv.core

import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleTest {
    private val zone = ZoneId.of("Europe/Madrid")
    private val night = FilterState(scheduleEnabled = true, startMinute = 22 * 60, endMinute = 7 * 60)

    private fun at(text: String) = LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun lateEveningIsInsideAWindowThatCrossesMidnight() {
        val now = at("2026-10-10T23:00")
        val previous = Schedule.previous(night, now, zone)!!
        assertTrue(previous.turnsOn)
        assertEquals(at("2026-10-10T22:00"), previous.at)

        val next = Schedule.next(night, now, zone)!!
        assertFalse(next.turnsOn)
        assertEquals(at("2026-10-11T07:00"), next.at)
    }

    @Test
    fun earlyMorningBelongsToYesterdaysWindow() {
        val now = at("2026-10-11T06:00")
        val previous = Schedule.previous(night, now, zone)!!
        assertTrue(previous.turnsOn)
        assertEquals(at("2026-10-10T22:00"), previous.at)
        assertEquals(at("2026-10-11T07:00"), Schedule.next(night, now, zone)!!.at)
    }

    @Test
    fun middayIsOutsideTheWindow() {
        val now = at("2026-10-10T12:00")
        val previous = Schedule.previous(night, now, zone)!!
        assertFalse(previous.turnsOn)
        assertEquals(at("2026-10-10T07:00"), previous.at)

        val next = Schedule.next(night, now, zone)!!
        assertTrue(next.turnsOn)
        assertEquals(at("2026-10-10T22:00"), next.at)
    }

    @Test
    fun exactBoundaryCountsAsAlreadyReached() {
        val now = at("2026-10-10T22:00")
        assertEquals(Boundary(now, turnsOn = true), Schedule.previous(night, now, zone))
        assertEquals(at("2026-10-11T07:00"), Schedule.next(night, now, zone)!!.at)
    }

    @Test
    fun daytimeWindowWorksToo() {
        val day = night.copy(startMinute = 9 * 60, endMinute = 18 * 60)
        assertTrue(Schedule.previous(day, at("2026-10-10T10:30"), zone)!!.turnsOn)
        assertFalse(Schedule.previous(day, at("2026-10-10T20:00"), zone)!!.turnsOn)
        assertFalse(Schedule.previous(day, at("2026-10-10T08:00"), zone)!!.turnsOn)
    }

    @Test
    fun equalStartAndEndMeansNoSchedule() {
        val empty = night.copy(startMinute = 600, endMinute = 600)
        assertNull(Schedule.previous(empty, at("2026-10-10T12:00"), zone))
        assertNull(Schedule.next(empty, at("2026-10-10T12:00"), zone))
    }

    @Test
    fun survivesDaylightSavingGaps() {
        // 29 March 2026: clocks jump from 02:00 to 03:00 in Madrid, so 02:30 does not exist.
        val gap = night.copy(startMinute = 2 * 60 + 30, endMinute = 6 * 60)
        val next = Schedule.next(gap, at("2026-03-29T01:00"), zone)
        assertNotNull(next)
        assertTrue(next!!.turnsOn)
        assertTrue(next.at > at("2026-03-29T01:00"))
    }
}
