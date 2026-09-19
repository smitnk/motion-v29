package com.smitnk.motioncanvas.tools

import androidx.compose.ui.geometry.Offset

enum class SymmetryMode { NONE, VERTICAL, HORIZONTAL, RADIAL }

object SymmetryEngine {    fun mirror(points: List<Offset>, width: Float, height: Float, mode: SymmetryMode): List<List<Offset>> {
        if (mode == SymmetryMode.NONE) return listOf(points)
        val cx = width / 2f
        val cy = height / 2f
        val mirrored = when (mode) {
            SymmetryMode.VERTICAL -> points.map { Offset(2f * cx - it.x, it.y) }
            SymmetryMode.HORIZONTAL -> points.map { Offset(it.x, 2f * cy - it.y) }
            else -> points
        }
        return listOf(points, mirrored)
    }

    fun radial(points: List<Offset>, center: Offset, segments: Int): List<List<Offset>> {
        val n = segments.coerceIn(2, 24)
        return (0 until n).map { i ->
            val a = (Math.PI * 2.0 * i / n).toFloat()
            val c = kotlin.math.cos(a); val s = kotlin.math.sin(a)
            points.map { p ->
                val x = p.x - center.x; val y = p.y - center.y
                Offset(center.x + x * c - y * s, center.y + x * s + y * c)
            }
        }
    }
}