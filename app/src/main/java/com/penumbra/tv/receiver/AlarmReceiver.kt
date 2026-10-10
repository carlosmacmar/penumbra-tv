package com.penumbra.tv.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.penumbra.tv.core.PenumbraController

/** Fires at each schedule boundary: applies it and arms the next one. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        PenumbraController.sync(context)
    }
}
