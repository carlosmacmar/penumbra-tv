package com.penumbra.tv.core

import android.graphics.Color
import androidx.annotation.StringRes
import com.penumbra.tv.R

/** Opacity of the black layer at 100 % dimming. Below 1 so the picture never goes fully black. */
const val MAX_DIM_ALPHA = 0.92f

/** Opacity of the colour layer at 100 % filter intensity. */
const val MAX_TINT_ALPHA = 0.5f

/** Durations offered for the gradual dimming at the scheduled start, in minutes. */
val FADE_OPTIONS = listOf(0, 5, 15, 30, 60)

/** Night-filter tones, from the softest to the warmest. */
enum class Tone(val rgb: Int, @StringRes val label: Int) {
    NEUTRAL(0xFFD8D4E6.toInt(), R.string.tone_neutral),
    WARM(0xFFFFC890.toInt(), R.string.tone_warm),
    AMBER(0xFFFFA64A.toInt(), R.string.tone_amber),
    CANDLE(0xFFFF8530.toInt(), R.string.tone_candle),
    SUNSET(0xFFFF6142.toInt(), R.string.tone_sunset),
    RED(0xFFF23A2E.toInt(), R.string.tone_red);

    /** Neutral only dims; every other tone adds a colour layer. */
    val tints: Boolean get() = this != NEUTRAL
}

data class FilterState(
    val enabled: Boolean = false,
    /** Dimming, 0–100 %. */
    val dim: Int = 40,
    val tone: Tone = Tone.AMBER,
    /** Colour filter intensity, 0–100 %. */
    val warmth: Int = 40,
    val scheduleEnabled: Boolean = false,
    /** Daily start and end of the schedule, in minutes after midnight. */
    val startMinute: Int = 22 * 60,
    val endMinute: Int = 7 * 60,
    val fadeMinutes: Int = 15,
    val activateOnWake: Boolean = false,
    val systemBrightness: Boolean = false,
    /** Gradual fade-in started by the schedule; a duration of 0 means "no ramp". */
    val rampStartAt: Long = 0L,
    val rampDurationMs: Long = 0L,
    /** Time of the last schedule boundary already applied, so manual changes win until the next one. */
    val lastEventAt: Long = 0L,
) {
    fun isRamping(now: Long): Boolean =
        enabled && rampDurationMs > 0L && now < rampStartAt + rampDurationMs

    /** Eased 0–1 progress of the scheduled fade-in. */
    fun rampProgress(now: Long): Float {
        if (rampDurationMs <= 0L) return 1f
        val t = ((now - rampStartAt).toFloat() / rampDurationMs).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    fun levels(now: Long): FilterLevels {
        if (!enabled) return FilterLevels.Clear
        val progress = rampProgress(now)
        return FilterLevels(
            dim = dim / 100f * MAX_DIM_ALPHA * progress,
            tintRgb = tone.rgb,
            tintAlpha = if (tone.tints) warmth / 100f * MAX_TINT_ALPHA * progress else 0f,
        )
    }
}

/** What the overlay actually draws: a colour layer under a black layer. */
data class FilterLevels(val dim: Float, val tintRgb: Int, val tintAlpha: Float) {
    val isClear: Boolean get() = dim <= 0.001f && tintAlpha <= 0.001f

    companion object {
        val Clear = FilterLevels(0f, Color.BLACK, 0f)
    }
}
