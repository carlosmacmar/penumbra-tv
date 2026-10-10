package com.penumbra.tv.overlay

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.penumbra.tv.PenumbraApp
import com.penumbra.tv.R
import com.penumbra.tv.ui.MainActivity

/** Fallback host: a foreground service drawing with the "display over other apps" permission. */
class OverlayService : Service() {
    private var renderer: OverlayRenderer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Must be called promptly after startForegroundService(), even if we stop right away.
        if (!promoteToForeground()) {
            stopSelf()
            return START_NOT_STICKY
        }
        val shouldRun = !OverlayAccessibilityService.isConnected &&
            Settings.canDrawOverlays(this) &&
            OverlayHost.wantsService(this)
        if (!shouldRun) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (renderer == null) {
            renderer = OverlayRenderer(this, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY).also { it.start() }
        }
        running = true
        return START_STICKY
    }

    override fun onDestroy() {
        // If accessibility is taking over, it owns the brightness from now on.
        renderer?.stop(restoreBrightness = !OverlayAccessibilityService.isConnected)
        renderer = null
        running = false
        super.onDestroy()
    }

    private fun promoteToForeground(): Boolean {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, PenumbraApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_moon)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_text))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        return try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
            true
        } catch (e: RuntimeException) {
            Log.w(TAG, "Could not enter the foreground", e)
            false
        }
    }

    companion object {
        private const val TAG = "PenumbraService"
        private const val NOTIFICATION_ID = 1

        @Volatile
        private var running = false

        fun start(context: Context) {
            if (running) return
            try {
                ContextCompat.startForegroundService(context, Intent(context, OverlayService::class.java))
            } catch (e: IllegalStateException) {
                // Background start not allowed right now; the next alarm, boot or app launch retries.
                Log.w(TAG, "Could not start the overlay service", e)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OverlayService::class.java))
        }
    }
}
