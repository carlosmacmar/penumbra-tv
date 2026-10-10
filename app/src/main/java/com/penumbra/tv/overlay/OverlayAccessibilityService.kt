package com.penumbra.tv.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.penumbra.tv.core.PenumbraController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Preferred host. It reads no screen content and ignores every event: the service exists only
 * because accessibility overlays can be enabled from the TV's own settings and sit above
 * everything, including system screens. The system also restarts it after every boot.
 */
class OverlayAccessibilityService : AccessibilityService() {
    private var renderer: OverlayRenderer? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        renderer = OverlayRenderer(this, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY).also { it.start() }
        _connected.value = true
        // Takes over from the fallback overlay service, if it was running.
        PenumbraController.sync(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        release()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        release()
        super.onDestroy()
    }

    private fun release() {
        val current = renderer ?: return
        renderer = null
        current.stop(restoreBrightness = true)
        _connected.value = false
        // Hands over to the fallback overlay service, if that permission is available.
        PenumbraController.sync(applicationContext)
    }

    companion object {
        private val _connected = MutableStateFlow(false)
        val connected: StateFlow<Boolean> = _connected.asStateFlow()
        val isConnected: Boolean get() = _connected.value
    }
}
