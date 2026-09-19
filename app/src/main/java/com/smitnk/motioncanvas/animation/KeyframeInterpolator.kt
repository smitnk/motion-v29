package com.smitnk.motioncanvas.animation

import androidx.compose.ui.geometry.Offset

object KeyframeInterpolator {
    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
    fun lerp(a: Offset, b: Offset, t: Float): Offset = Offset(lerp(a.x, b.x, t), lerp(a.y, b.y, t))

    fun ease(t: Float, mode: Easing): Float = when (mode) {
        Easing.LINEAR -> t
        Easing.EASE_IN -> t * t
        Easing.EASE_OUT -> 1f - (1f - t) * (1f - t)
        Easing.EASE_IN_OUT -> if (t < 0.5f) 2f * t * t else 1f - (-2f * t + 2f).let { it * it } / 2f
    }

    fun progress(frame: Int, start: Int, end: Int, mode: Easing): Float {
        if (end <= start) return 1f
        return ease(((frame - start).toFloat() / (end - start)).coerceIn(0f, 1f), mode)
    }
}
