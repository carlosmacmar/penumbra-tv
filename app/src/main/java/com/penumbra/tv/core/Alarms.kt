package com.penumbra.tv.core

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.penumbra.tv.receiver.AlarmReceiver

/** Arms a single alarm for the next schedule boundary; [AlarmReceiver] re-arms it when it fires. */
object Alarms {
    private const val TAG = "PenumbraAlarms"
    private const val FALLBACK_WINDOW_MS = 10 * 60_000L

    fun scheduleNext(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = pendingIntent(context)
        alarmManager.cancel(pending)

        val state = FilterStore.current(context)
        if (!state.scheduleEnabled) return
        val next = Schedule.next(state, System.currentTimeMillis()) ?: return

        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.at, pending)
            } else {
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP, next.at, FALLBACK_WINDOW_MS, pending)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm refused, using a window", e)
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, next.at, FALLBACK_WINDOW_MS, pending)
        }
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, AlarmReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
