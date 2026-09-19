package com.smitnk.motioncanvas.brush

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.sqrt

/**
 * Independent Kotlin brush layer inspired by Hokusai/libmypaint concepts:
 * pressure response, dab spacing and deterministic scatter.
 * No Hokusai Rust source is copied.
 */
object AdvancedBrushEngine {
    fun draw(
        canvas: Canvas,
        points: List<androidx.compose.ui.geometry.Offset>,
        pressures: List<Float>,
        color: Int,
        width: Float,
        opacity: Float,
        spacing: Float,
        scatter: Float
    ) {
        if (points.size < 2) return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        val step = (width * spacing.coerceIn(0.04f, 0.9f)).coerceAtLeast(1f)
        var seed = 0x13579BDF

        fun random(): Float {
            seed = seed * 1103515245 + 12345
            return ((seed ushr 8) and 0x00FFFFFF) / 16777215f
        }

        for (i in 1 until points.size) {
            val a = points[i - 1]
            val b = points[i]
            val dx = b.x - a.x
            val dy = b.y - a.y
            val length = sqrt(dx * dx + dy * dy)
            if (length <= 0f) continue
            var travelled = 0f
            while (travelled <= length) {
                val t = travelled / length
                val pressure = pressures.getOrNull(i)?.coerceIn(0.05f, 1.25f) ?: 1f
                val radius = (width * (0.38f + pressure * 0.72f)).coerceAtLeast(0.5f)
                val jitter = scatter.coerceIn(0f, 1f) * radius
                val x = a.x + dx * t + (random() - 0.5f) * 2f * jitter
                val y = a.y + dy * t + (random() - 0.5f) * 2f * jitter
                paint.alpha = (opacity.coerceIn(0f, 1f) * (0.55f + pressure * 0.45f) * 255f).toInt()
                canvas.drawCircle(x, y, radius, paint)
                travelled += step
            }
        }
    }
}