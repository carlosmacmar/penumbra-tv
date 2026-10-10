package com.penumbra.tv.core

import android.content.Context
import android.provider.Settings
import androidx.core.content.edit
import kotlin.math.roundToInt

/**
 * Optional real backlight reduction through Settings.System. Many TVs ignore it (the picture is
 * driven by the panel's own settings), so the overlay remains the main dimming mechanism.
 * The user's original brightness is saved before the first change and restored afterwards.
 */
object SystemBrightness {
    private const val PREFS = "penumbra_brightness"
    private const val ORIGINAL_LEVEL = "original_level"
    private const val ORIGINAL_MODE = "original_mode"
    private const val MIN_LEVEL = 10

    private var lastWritten = -1

    fun canWrite(context: Context): Boolean = Settings.System.canWrite(context)

    /** [amount] is 0 (no change) to 1 (darkest). */
    fun dim(context: Context, amount: Float) {
        if (!canWrite(context)) return
        val prefs = prefs(context)
        val resolver = context.contentResolver
        if (!prefs.contains(ORIGINAL_LEVEL)) {
            prefs.edit(commit = true) {
                putInt(ORIGINAL_LEVEL, Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS, 255))
                putInt(
                    ORIGINAL_MODE,
                    Settings.System.getInt(
                        resolver,
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
                    ),
                )
            }
        }
        val original = prefs.getInt(ORIGINAL_LEVEL, 255)
        val target = (original * (1f - amount.coerceIn(0f, 1f))).roundToInt().coerceIn(MIN_LEVEL, 255)
        if (target == lastWritten) return
        runCatching {
            Settings.System.putInt(
                resolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
            )
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, target)
            lastWritten = target
        }
    }

    fun restore(context: Context) {
        val prefs = prefs(context)
        if (!prefs.contains(ORIGINAL_LEVEL)) return
        if (canWrite(context)) {
            runCatching {
                val resolver = context.contentResolver
                Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, prefs.getInt(ORIGINAL_LEVEL, 255))
                Settings.System.putInt(
                    resolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    prefs.getInt(ORIGINAL_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL),
                )
            }
        }
        prefs.edit(commit = true) { clear() }
        lastWritten = -1
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
