package com.penumbra.tv.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.penumbra.tv.core.FilterState
import com.penumbra.tv.core.FilterStore
import com.penumbra.tv.core.MAX_DIM_ALPHA
import com.penumbra.tv.core.PenumbraController
import com.penumbra.tv.core.SystemBrightness
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Draws [FilterStore.state] as a window on top of everything. Shared by both hosts; only the
 * window type differs (accessibility overlay or application overlay).
 */
class OverlayRenderer(private val context: Context, private val windowType: Int) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var view: FilterView? = null
    private var lastState: FilterState? = null
    private var receiverRegistered = false

    private val rampTick = Runnable { render(FilterStore.state.value, durationMs = 0L) }

    private val systemEvents = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> PenumbraController.onScreenOn(context)
                Intent.ACTION_TIME_TICK -> PenumbraController.onMinuteTick(context)
            }
        }
    }

    fun start() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_TIME_TICK)
        }
        // Not exported: only the system (which sends these protected broadcasts) can reach it.
        ContextCompat.registerReceiver(context, systemEvents, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        receiverRegistered = true
        scope.launch { FilterStore.state.collect(::onStateChanged) }
    }

    fun stop(restoreBrightness: Boolean) {
        scope.cancel()
        handler.removeCallbacks(rampTick)
        if (receiverRegistered) {
            runCatching { context.unregisterReceiver(systemEvents) }
            receiverRegistered = false
        }
        detach()
        if (restoreBrightness) SystemBrightness.restore(context)
    }

    private fun onStateChanged(state: FilterState) {
        val previous = lastState
        lastState = state
        val duration = when {
            previous == null -> FADE_IN_MS
            previous.enabled != state.enabled -> if (state.enabled) FADE_IN_MS else FADE_OUT_MS
            else -> ADJUST_MS
        }
        render(state, duration)
    }

    private fun render(state: FilterState, durationMs: Long) {
        handler.removeCallbacks(rampTick)
        val now = System.currentTimeMillis()
        val levels = state.levels(now)

        if (state.enabled && state.systemBrightness) {
            SystemBrightness.dim(context, levels.dim / MAX_DIM_ALPHA)
        } else {
            SystemBrightness.restore(context)
        }

        val target = view ?: if (levels.isClear) null else attach()
        target?.show(levels, durationMs)

        // A long scheduled fade-in advances in small steps; each one is imperceptible.
        if (state.isRamping(now)) handler.postDelayed(rampTick, RAMP_TICK_MS)
    }

    private fun attach(): FilterView? {
        val filterView = FilterView(context) { faded ->
            if (faded === view && FilterStore.state.value.levels(System.currentTimeMillis()).isClear) detach()
        }
        // Square as large as the longest side, so the layer covers the screen in any orientation.
        val side = screenLongSide()
        val params = WindowManager.LayoutParams(
            side,
            side,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            title = "Penumbra"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        return try {
            windowManager.addView(filterView, params)
            view = filterView
            filterView
        } catch (e: RuntimeException) {
            Log.w(TAG, "Could not add the filter window", e)
            null
        }
    }

    private fun detach() {
        val current = view ?: return
        view = null
        current.cancelAnimation()
        runCatching { windowManager.removeViewImmediate(current) }
    }

    private fun screenLongSide(): Int {
        val bounds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.maximumWindowMetrics.bounds.let { maxOf(it.width(), it.height()) }
        } else {
            @Suppress("DEPRECATION")
            android.util.DisplayMetrics().also { windowManager.defaultDisplay.getRealMetrics(it) }
                .let { maxOf(it.widthPixels, it.heightPixels) }
        }
        return bounds.coerceAtLeast(1)
    }

    private companion object {
        const val TAG = "PenumbraOverlay"
        const val FADE_IN_MS = 900L
        const val FADE_OUT_MS = 1400L
        const val ADJUST_MS = 250L
        const val RAMP_TICK_MS = 2_000L
    }
}
