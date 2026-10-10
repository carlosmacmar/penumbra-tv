package com.penumbra.tv.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {

    @Test
    fun timeStepsSnapToFiveMinutes() {
        assertEquals(22 * 60 + 10, shiftMinute(22 * 60 + 5, delta = 1, repeat = 0))
        assertEquals(22 * 60, shiftMinute(22 * 60 + 3, delta = -1, repeat = 0))
        assertEquals(21 * 60 + 55, shiftMinute(22 * 60, delta = -1, repeat = 0))
    }

    @Test
    fun holdingTheKeyJumpsInQuarterHours() {
        assertEquals(22 * 60 + 15, shiftMinute(22 * 60 + 5, delta = 1, repeat = 10))
    }

    @Test
    fun timeWrapsAroundMidnight() {
        assertEquals(23 * 60 + 55, shiftMinute(0, delta = -1, repeat = 0))
        assertEquals(0, shiftMinute(23 * 60 + 55, delta = 1, repeat = 0))
    }

    @Test
    fun fadeStepsThroughTheOptionsAndStopsAtTheEnds() {
        assertEquals(30, shiftFade(15, 1))
        assertEquals(0, shiftFade(0, -1))
        assertEquals(60, shiftFade(60, 1))
        assertEquals(5, shiftFade(7, -1))
    }
}
