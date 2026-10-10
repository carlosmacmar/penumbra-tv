package com.penumbra.tv.ui

import android.content.Context
import android.text.format.DateFormat
import com.penumbra.tv.R
import com.penumbra.tv.core.FADE_OPTIONS
import com.penumbra.tv.core.Schedule
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val spanish: Locale = Locale.forLanguageTag("es-ES")
private val time24 = DateTimeFormatter.ofPattern("HH:mm", spanish)
private val time12 = DateTimeFormatter.ofPattern("h:mm a", spanish)
private val clock12 = DateTimeFormatter.ofPattern("h:mm", spanish)
private val dayPeriod = DateTimeFormatter.ofPattern("a", spanish)
private val longDate = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", spanish)

fun formatTime(context: Context, time: LocalTime): String =
    time.format(if (DateFormat.is24HourFormat(context)) time24 else time12)

fun formatMinuteOfDay(context: Context, minuteOfDay: Int): String =
    formatTime(context, LocalTime.of(minuteOfDay / 60, minuteOfDay % 60))

/** The big clock: digits and, on 12-hour devices, the "a. m." marker shown smaller beside them. */
fun formatClock(context: Context, now: Long): Pair<String, String?> {
    val time = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalTime()
    return if (DateFormat.is24HourFormat(context)) {
        time.format(time24) to null
    } else {
        time.format(clock12) to time.format(dayPeriod)
    }
}

fun formatDate(now: Long): String =
    Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).format(longDate)
        .replaceFirstChar { it.titlecase(spanish) }

fun formatFade(context: Context, minutes: Int): String = when {
    minutes <= 0 -> context.getString(R.string.fade_off)
    minutes == 60 -> context.getString(R.string.fade_hour)
    else -> context.getString(R.string.fade_minutes, minutes)
}

/** Moves a time by 5 minutes, or by 15 while the key is held, snapping to the step. */
fun shiftMinute(minuteOfDay: Int, delta: Int, repeat: Int): Int {
    val step = if (repeat >= 8) 15 else 5
    val snapped = if (delta > 0) {
        (Math.floorDiv(minuteOfDay, step) + 1) * step
    } else {
        (Math.floorDiv(minuteOfDay + step - 1, step) - 1) * step
    }
    return Math.floorMod(snapped, Schedule.MINUTES_PER_DAY)
}

fun shiftFade(minutes: Int, delta: Int): Int {
    val index = FADE_OPTIONS.indexOfFirst { it >= minutes }.let { if (it < 0) FADE_OPTIONS.lastIndex else it }
    return FADE_OPTIONS[(index + delta).coerceIn(0, FADE_OPTIONS.lastIndex)]
}
