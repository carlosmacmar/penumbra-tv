package com.penumbra.tv.core

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for the filter. Everything (UI, overlay, alarms, boot) reads and writes
 * through here; the overlay renderers observe [state] and redraw on every change.
 */
object FilterStore {
    private const val PREFS = "penumbra"
    private const val ENABLED = "enabled"
    private const val DIM = "dim"
    private const val TONE = "tone"
    private const val WARMTH = "warmth"
    private const val SCHEDULE = "schedule_enabled"
    private const val START = "start_minute"
    private const val END = "end_minute"
    private const val FADE = "fade_minutes"
    private const val WAKE = "activate_on_wake"
    private const val BRIGHTNESS = "system_brightness"
    private const val RAMP_START = "ramp_start_at"
    private const val RAMP_DURATION = "ramp_duration_ms"
    private const val LAST_EVENT = "last_event_at"

    private var prefs: SharedPreferences? = null
    private val _state = MutableStateFlow(FilterState())
    val state: StateFlow<FilterState> = _state.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        _state.value = read(p)
    }

    fun current(context: Context): FilterState {
        init(context)
        return _state.value
    }

    @Synchronized
    fun update(context: Context, transform: (FilterState) -> FilterState): FilterState {
        init(context)
        val old = _state.value
        val new = transform(old)
        if (new != old) {
            write(prefs!!, new)
            _state.value = new
        }
        return new
    }

    private fun read(p: SharedPreferences): FilterState {
        val d = FilterState()
        return FilterState(
            enabled = p.getBoolean(ENABLED, d.enabled),
            dim = p.getInt(DIM, d.dim),
            tone = p.getString(TONE, null)
                ?.let { name -> Tone.entries.firstOrNull { it.name == name } } ?: d.tone,
            warmth = p.getInt(WARMTH, d.warmth),
            scheduleEnabled = p.getBoolean(SCHEDULE, d.scheduleEnabled),
            startMinute = p.getInt(START, d.startMinute),
            endMinute = p.getInt(END, d.endMinute),
            fadeMinutes = p.getInt(FADE, d.fadeMinutes),
            activateOnWake = p.getBoolean(WAKE, d.activateOnWake),
            systemBrightness = p.getBoolean(BRIGHTNESS, d.systemBrightness),
            rampStartAt = p.getLong(RAMP_START, d.rampStartAt),
            rampDurationMs = p.getLong(RAMP_DURATION, d.rampDurationMs),
            lastEventAt = p.getLong(LAST_EVENT, d.lastEventAt),
        )
    }

    private fun write(p: SharedPreferences, s: FilterState) = p.edit {
        putBoolean(ENABLED, s.enabled)
        putInt(DIM, s.dim)
        putString(TONE, s.tone.name)
        putInt(WARMTH, s.warmth)
        putBoolean(SCHEDULE, s.scheduleEnabled)
        putInt(START, s.startMinute)
        putInt(END, s.endMinute)
        putInt(FADE, s.fadeMinutes)
        putBoolean(WAKE, s.activateOnWake)
        putBoolean(BRIGHTNESS, s.systemBrightness)
        putLong(RAMP_START, s.rampStartAt)
        putLong(RAMP_DURATION, s.rampDurationMs)
        putLong(LAST_EVENT, s.lastEventAt)
    }
}
