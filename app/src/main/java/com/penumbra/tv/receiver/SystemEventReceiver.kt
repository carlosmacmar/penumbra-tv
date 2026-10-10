package com.penumbra.tv.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.penumbra.tv.core.PenumbraController

class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> PenumbraController.onBoot(context)
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> PenumbraController.onClockChanged(context)
            Intent.ACTION_MY_PACKAGE_REPLACED -> PenumbraController.sync(context)
        }
    }
}
