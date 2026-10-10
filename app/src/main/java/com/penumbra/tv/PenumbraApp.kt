package com.penumbra.tv

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.penumbra.tv.core.FilterStore

class PenumbraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FilterStore.init(this)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel),
            NotificationManager.IMPORTANCE_MIN,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "filter"
    }
}
