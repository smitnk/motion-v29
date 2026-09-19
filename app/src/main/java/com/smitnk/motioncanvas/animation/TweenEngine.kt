package com.smitnk.motioncanvas.animation

import androidx.compose.ui.geometry.Offset
import com.smitnk.motioncanvas.Frame
import com.smitnk.motioncanvas.LayerFrame
import com.smitnk.motioncanvas.Stroke
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
            TweenEasing.EASE_IN_OUT ->
                if (x < 0.5f) 2f * x * x else 1f - (-2f * x + 2f).pow(2f) / 2f
        }
    }

    fun interpolate(a: Frame, b: Frame, rawT: Float, easing: TweenEasing): Frame {
        val t = ease(rawT, easing)
        val count = maxOf(a.layers.size, b.layers.size)
        val layers = (0 until count).map { i ->
            val la = a.layers.getOrNull(i) ?: LayerFrame()
            val lb = b.layers.getOrNull(i) ?: LayerFrame()
            LayerFrame(interpolateStrokes(la.strokes, lb.strokes, t), if (t < 0.5f) la.hold else lb.hold)
        }
        return Frame(layers)
    }

    private fun interpolateStrokes(a: List<Stroke>, b: List<Stroke>, t: Float): List<Stroke> {
        val count = maxOf(a.size, b.size)
        return (0 until count).mapNotNull { i ->
            val sa = a.getOrNull(i) ?: b.getOrNull(i) ?: return@mapNotNull null
            val sb = b.getOrNull(i) ?: sa
            val n = maxOf(sa.points.size, sb.points.size)
            if (n == 0) return@mapNotNull null

            fun sample(points: List<Offset>, index: Int): Offset {
                if (points.size == 1) return points[0]
                val p = index.toFloat() / (n - 1).coerceAtLeast(1)
                val x = p * (points.size - 1)
                val lo = x.toInt().coerceIn(0, points.lastIndex)
                val hi = (lo + 1).coerceAtMost(points.lastIndex)
                val f = x - lo
                return Offset(
                    points[lo].x + (points[hi].x - points[lo].x) * f,
                    points[lo].y + (points[hi].y - points[lo].y) * f
                )
            }

            val points = (0 until n).map { j ->
                val pa = sample(sa.points, j)
                val pb = sample(sb.points, j)
                Offset(pa.x + (pb.x - pa.x) * t, pa.y + (pb.y - pa.y) * t)
            }

            sa.copy(
                points = points,
                color = Color(
                    sa.color.red + (sb.color.red - sa.color.red) * t,
                    sa.color.green + (sb.color.green - sa.color.green) * t,
                    sa.color.blue + (sb.color.blue - sa.color.blue) * t,
                    sa.color.alpha + (sb.color.alpha - sa.color.alpha) * t
                ),
                width = sa.width + (sb.width - sa.width) * t,
                opacity = sa.opacity + (sb.opacity - sa.opacity) * t
            )
        }
    }
}