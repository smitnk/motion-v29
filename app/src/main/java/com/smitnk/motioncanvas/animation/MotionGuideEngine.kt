package com.smitnk.motioncanvas.animation

import androidx.compose.ui.geometry.Offset
import kotlin.math.sqrt

data class MotionGuide(val points: List<Offset> = emptyList(), val visible: Boolean = true)

object MotionGuideEngine {
    fun sample(guide: MotionGuide, progress: Float): Offset? {
        if (guide.points.isEmpty()) return null
        if (guide.points.size == 1) return guide.points.first()
        val segments = guide.points.zipWithNext()
        val lengths = segments.map { (a, b) ->
            sqrt((b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y))
        }
        var distance = progress.coerceIn(0f, 1f) * lengths.sum().coerceAtLeast(0.001f)
        for (i in segments.indices) {
            if (distance <= lengths[i]) {
                val f = distance / lengths[i].coerceAtLeast(0.001f)
                val a = segments[i].first
                val b = segments[i].second
                return Offset(a.x + (b.x - a.x) * f, a.y + (b.y - a.y) * f)
            }
            distance -= lengths[i]
        }
        return guide.points.last()
    }
}