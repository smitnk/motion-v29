package com.smitnk.motioncanvas.animation

import com.smitnk.motioncanvas.DrawPoint
import com.smitnk.motioncanvas.DrawStroke
import com.smitnk.motioncanvas.Frame
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.pow

enum class TweenEasing { LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT }

object TweenEngine {
    fun ease(t: Float, easing: TweenEasing): Float {
        val x = t.coerceIn(0f, 1f)
        return when (easing) {
            TweenEasing.LINEAR -> x
            TweenEasing.EASE_IN -> x * x
            TweenEasing.EASE_OUT -> 1f - (1f - x) * (1f - x)
            TweenEasing.EASE_IN_OUT -> if (x < 0.5f) 2f * x * x else 1f - (-2f * x + 2f).pow(2f) / 2f
        }
    }

    fun interpolate(a: Frame, b: Frame, rawT: Float, easing: TweenEasing): Frame {
        val t = ease(rawT, easing)
        val count = maxOf(a.strokes.size, b.strokes.size)
        val strokes = (0 until count).mapNotNull { i ->
            val sa = a.strokes.getOrNull(i)
            val sb = b.strokes.getOrNull(i)
            when {
                sa == null -> sb?.deepCopy()
                sb == null -> sa.deepCopy()
                else -> interpolateStroke(sa, sb, t)
            }
        }.toMutableList()
        return a.copy(strokes = strokes, redoStrokes = mutableListOf())
    }

    private fun DrawStroke.deepCopy(): DrawStroke = copy(
        id = java.util.UUID.randomUUID().toString(),
        points = points.map { DrawPoint(it.x, it.y, it.pressure) }
    )

    private fun interpolateStroke(a: DrawStroke, b: DrawStroke, t: Float): DrawStroke {
        val n = maxOf(a.points.size, b.points.size)
        if (n == 0) return a.copy(id = java.util.UUID.randomUUID().toString())
        fun sample(points: List<DrawPoint>, index: Int): DrawPoint {
            if (points.size == 1) return points[0]
            val p = index.toFloat() / (n - 1).coerceAtLeast(1)
            val x = p * (points.size - 1)
            val lo = x.toInt().coerceIn(0, points.lastIndex)
            val hi = (lo + 1).coerceAtMost(points.lastIndex)
            val f = x - lo
            val p0 = points[lo]; val p1 = points[hi]
            return DrawPoint(
                p0.x + (p1.x - p0.x) * f,
                p0.y + (p1.y - p0.y) * f,
                p0.pressure + (p1.pressure - p0.pressure) * f
            )
        }
        val points = (0 until n).map { i ->
            val pa = sample(a.points, i); val pb = sample(b.points, i)
            DrawPoint(
                pa.x + (pb.x - pa.x) * t,
                pa.y + (pb.y - pa.y) * t,
                pa.pressure + (pb.pressure - pa.pressure) * t
            )
        }
        return a.copy(
            id = java.util.UUID.randomUUID().toString(),
            points = points,
            color = Color(
                a.color.red + (b.color.red - a.color.red) * t,
                a.color.green + (b.color.green - a.color.green) * t,
                a.color.blue + (b.color.blue - a.color.blue) * t,
                a.color.alpha + (b.color.alpha - a.color.alpha) * t
            ),
            strokeWidth = a.strokeWidth + (b.strokeWidth - a.strokeWidth) * t,
            alpha = a.alpha + (b.alpha - a.alpha) * t
        )
    }
}
