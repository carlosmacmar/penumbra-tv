package com.penumbra.tv.core

import android.content.Context
import com.penumbra.tv.overlay.OverlayHost

/**
 * Every state change goes through here so the store, the alarm and the overlay host stay in step.
 *
 * Schedule model: each daily boundary (start/end) acts like pressing the power button at that
 * time. Manual changes are kept until the next boundary. Missed boundaries (TV asleep, reboot,
 * delayed alarm) are caught up by [sync], which runs on alarms, every minute, on screen-on and on boot.
 */
object PenumbraController {

    fun toggle(context: Context) = setEnabled(context, !FilterStore.current(context).enabled)

    fun setEnabled(context: Context, enabled: Boolean) {
        FilterStore.update(context) { it.copy(enabled = enabled, rampDurationMs = 0L) }
        OverlayHost.ensure(context)
    }

    /** Look and behaviour settings that don't move the schedule window. */
    fun update(context: Context, transform: (FilterState) -> FilterState) {
        FilterStore.update(context, transform)
        OverlayHost.ensure(context)
    }

    /** Schedule settings: when the window changes, the filter follows it immediately. */
    fun updateSchedule(context: Context, transform: (FilterState) -> FilterState) {
        val now = System.currentTimeMillis()
        FilterStore.update(context) { old ->
            val new = transform(old)
            val windowChanged = new.scheduleEnabled && (
                !old.scheduleEnabled ||
                    new.startMinute != old.startMinute ||
                    new.endMinute != old.endMinute
                )
            val last = if (windowChanged) Schedule.previous(new, now) else null
            if (last == null) {
                new
            } else {
                new.copy(enabled = last.turnsOn, rampDurationMs = 0L, lastEventAt = last.at)
            }
        }
        Alarms.scheduleNext(context)
        OverlayHost.ensure(context)
    }

    /** Applies any boundary that hasn't been applied yet and re-arms the alarm. */
    fun sync(context: Context) {
        applyMissedBoundary(context)
        Alarms.scheduleNext(context)
        OverlayHost.ensure(context)
    }

    fun onMinuteTick(context: Context) {
        if (applyMissedBoundary(context)) {
            Alarms.scheduleNext(context)
            OverlayHost.ensure(context)
        }
    }

    fun onScreenOn(context: Context) {
        applyMissedBoundary(context)
        FilterStore.update(context) {
            if (it.activateOnWake && !it.enabled) it.copy(enabled = true, rampDurationMs = 0L) else it
        }
        Alarms.scheduleNext(context)
        OverlayHost.ensure(context)
    }

    fun onBoot(context: Context) {
        val now = System.currentTimeMillis()
        FilterStore.update(context) { s ->
            val last = if (s.scheduleEnabled) Schedule.previous(s, now) else null
            s.copy(
                enabled = s.activateOnWake || last?.turnsOn == true,
                rampDurationMs = 0L,
                lastEventAt = last?.at ?: s.lastEventAt,
            )
        }
        Alarms.scheduleNext(context)
        OverlayHost.ensure(context)
    }

    /** If the clock moved backwards, forget boundaries "from the future" so the schedule keeps working. */
    fun onClockChanged(context: Context) {
        val now = System.currentTimeMillis()
        FilterStore.update(context) { s ->
            if (s.lastEventAt > now) s.copy(lastEventAt = Schedule.previous(s, now)?.at ?: 0L) else s
        }
        sync(context)
    }

    private fun applyMissedBoundary(context: Context): Boolean {
        val now = System.currentTimeMillis()
        var applied = false
        FilterStore.update(context) { s ->
            val last = if (s.scheduleEnabled) Schedule.previous(s, now) else null
            when {
                last == null || last.at <= s.lastEventAt -> s
                last.turnsOn -> s.copy(
                    enabled = true,
                    rampStartAt = last.at,
                    rampDurationMs = s.fadeMinutes * 60_000L,
                    lastEventAt = last.at,
                )
                else -> s.copy(enabled = false, rampDurationMs = 0L, lastEventAt = last.at)
            }.also { applied = it !== s }
        }
        return applied
    }
}
