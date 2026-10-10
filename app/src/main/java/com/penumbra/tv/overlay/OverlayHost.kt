package com.penumbra.tv.overlay

import android.content.Context
import android.provider.Settings
import com.penumbra.tv.core.FilterStore

/**
 * Decides who draws the filter:
 * 1. The accessibility service, when the user enabled it. Best option on Google TV: the setting
 *    is always reachable from the TV and its overlay covers system screens too.
 * 2. A foreground service with the "display over other apps" permission, as a fallback.
 */
object OverlayHost {
    enum class Mode { ACCESSIBILITY, OVERLAY, NONE }

    fun mode(context: Context): Mode = when {
        OverlayAccessibilityService.isConnected -> Mode.ACCESSIBILITY
        Settings.canDrawOverlays(context) -> Mode.OVERLAY
        else -> Mode.NONE
    }

    /** The overlay service also stays up while waiting for the schedule or for the TV to wake. */
    fun wantsService(context: Context): Boolean {
        val s = FilterStore.current(context)
        return s.enabled || s.scheduleEnabled || s.activateOnWake
    }

    fun ensure(context: Context) {
        if (mode(context) == Mode.OVERLAY && wantsService(context)) {
            OverlayService.start(context)
        } else {
            OverlayService.stop(context)
        }
    }
}
