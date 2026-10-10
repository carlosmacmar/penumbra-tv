package com.penumbra.tv.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterStateTest {

    @Test
    fun disabledFilterDrawsNothing() {
        assertTrue(FilterState(enabled = false, dim = 80).levels(0L).isClear)
    }

    @Test
    fun fullStrengthMapsToMaximumOpacities() {
        val levels = FilterState(enabled = true, dim = 100, tone = Tone.RED, warmth = 100).levels(0L)
        assertEquals(MAX_DIM_ALPHA, levels.dim, 0.0001f)
        assertEquals(MAX_TINT_ALPHA, levels.tintAlpha, 0.0001f)
        assertEquals(Tone.RED.rgb, levels.tintRgb)
    }

    @Test
    fun neutralToneOnlyDims() {
        val levels = FilterState(enabled = true, dim = 50, tone = Tone.NEUTRAL, warmth = 100).levels(0L)
        assertEquals(0f, levels.tintAlpha, 0f)
        assertTrue(levels.dim > 0f)
    }

    @Test
    fun scheduledFadeInRampsSmoothlyFromZeroToFull() {
        val state = FilterState(enabled = true, dim = 100, rampStartAt = 1_000L, rampDurationMs = 10_000L)
        assertEquals(0f, state.levels(1_000L).dim, 0.0001f)
        assertEquals(MAX_DIM_ALPHA / 2, state.levels(6_000L).dim, 0.0001f)
        assertEquals(MAX_DIM_ALPHA, state.levels(11_000L).dim, 0.0001f)
        assertTrue(state.isRamping(6_000L))
        assertFalse(state.isRamping(11_000L))
    }
}
