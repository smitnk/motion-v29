package com.smitnk.motioncanvas.tools

import androidx.compose.ui.geometry.Offset

object ShapeEngine {
    fun line(start: Offset, end: Offset, samples: Int = 2): List<Offset> = listOf(start, end)

    fun rectangle(start: Offset, end: Offset): List<Offset> = listOf(
        start, Offset(end.x, start.y), end, Offset(start.x, end.y), start
    )

    fun ellipse(center: Offset, radiusX: Float, radiusY: Float, segments: Int = 64): List<Offset> =
        (0..segments).map { i ->
            val a = (Math.PI * 2.0 * i / segments).toFloat()
            Offset(center.x + kotlin.math.cos(a) * radiusX, center.y + kotlin.math.sin(a) * radiusY)
        }
}