package com.smitnk.motioncanvas.animation

import com.smitnk.motioncanvas.DrawStroke
import androidx.compose.ui.graphics.Color

data class MotionTrailSample(val stroke: DrawStroke, val alpha: Float, val tint: Color)

object MotionTrailEngine {
    fun samples(frames: List<List<DrawStroke>>, current: Int, radius: Int = 3): List<MotionTrailSample> {
        if (frames.isEmpty()) return emptyList()
        val out = mutableListOf<MotionTrailSample>()
        for (d in radius downTo 1) {
            val i = current - d
            if (i in frames.indices) {
                val a = (0.30f * (radius - d + 1) / radius).coerceIn(0.05f, 0.30f)
                frames[i].forEach { out += MotionTrailSample(it, a, Color(0xFFFF5C8A)) }
            }
        }
        for (d in 1..radius) {
            val i = current + d
            if (i in frames.indices) {
                val a = (0.30f * (radius - d + 1) / radius).coerceIn(0.05f, 0.30f)
                frames[i].forEach { out += MotionTrailSample(it, a, Color(0xFF4DD0E1)) }
            }
        }
        return out
    }
}