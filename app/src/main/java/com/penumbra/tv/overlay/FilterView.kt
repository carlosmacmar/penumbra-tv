package com.penumbra.tv.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.graphics.ColorUtils
import com.penumbra.tv.core.FilterLevels
import kotlin.math.roundToInt

/** Full-screen view that paints the colour layer and the black layer on top of everything. */
@SuppressLint("ViewConstructor")
class FilterView(context: Context, private val onFadedOut: (FilterView) -> Unit) : View(context) {
    private var dim = 0f
    private var tintAlpha = 0f
    private var tintRgb = Color.BLACK
    private var animator: ValueAnimator? = null

    private val isClear: Boolean get() = dim <= 0.001f && tintAlpha <= 0.001f

    fun show(target: FilterLevels, durationMs: Long) {
        animator?.cancel()
        animator = null
        if (durationMs <= 0L) {
            set(target.dim, target.tintAlpha, target.tintRgb)
            if (isClear) onFadedOut(this)
            return
        }
        val fromDim = dim
        val fromAlpha = tintAlpha
        // Fading in from nothing: start directly in the target colour instead of from black.
        val fromRgb = if (tintAlpha <= 0.001f) target.tintRgb else tintRgb
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            interpolator = DecelerateInterpolator(1.5f)
            addUpdateListener {
                val f = it.animatedValue as Float
                set(
                    fromDim + (target.dim - fromDim) * f,
                    fromAlpha + (target.tintAlpha - fromAlpha) * f,
                    ColorUtils.blendARGB(fromRgb, target.tintRgb, f),
                )
            }
            addListener(object : AnimatorListenerAdapter() {
                private var cancelled = false
                override fun onAnimationCancel(animation: Animator) {
                    cancelled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (!cancelled && isClear) onFadedOut(this@FilterView)
                }
            })
            start()
        }
    }

    fun cancelAnimation() {
        animator?.cancel()
        animator = null
    }

    private fun set(dim: Float, tintAlpha: Float, tintRgb: Int) {
        this.dim = dim
        this.tintAlpha = tintAlpha
        this.tintRgb = tintRgb
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (tintAlpha > 0.001f) {
            canvas.drawColor(ColorUtils.setAlphaComponent(tintRgb, (tintAlpha * 255).roundToInt()))
        }
        if (dim > 0.001f) {
            canvas.drawColor(Color.argb((dim * 255).roundToInt(), 0, 0, 0))
        }
    }
}
