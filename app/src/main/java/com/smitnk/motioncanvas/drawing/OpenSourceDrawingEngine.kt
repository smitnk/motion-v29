package com.smitnk.motioncanvas.drawing

import androidx.compose.ui.geometry.Offset
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * MotionCanvas native drawing engine.
 *
 * Feature references:
 * - Dolphin Animate (MIT): pressure-aware smooth vector strokes, spacing and
 *   Catmull-Rom/Bezier-style smoothing concepts.
 * - FrameBaker (MIT): frame editor / transform / timeline-oriented drawing flow.
 * - Klecks (MIT): pressure + stabilizer drawing, smudge/selection-oriented UX.
 * - OpenToonz (Modified BSD / third-party licenses vary): production animation
 *   drawing/timeline concepts.
 *
 * This file is an independent Kotlin implementation; it does not bundle their
 * source code. See OPEN_SOURCE_NOTICES_V27.md for attribution and licenses.
 */
object OpenSourceDrawingEngine {

    data class Config(
        val stabilizer: Float = 0.65f,
        val spacing: Float = 0.18f,
        val pressureMin: Float = 0.15f,
        val pressureMax: Float = 1f
    )

    fun normalizePressure(raw: Float, config: Config = Config()): Float =
        raw.coerceIn(config.pressureMin, config.pressureMax)

    fun stabilize(points: List<Offset>, config: Config = Config()): List<Offset> {
        if (points.size < 3) return points
        val strength = config.stabilizer.coerceIn(0f, 0.95f)
        val out = ArrayList<Offset>(points.size)
        out += points.first()
        for (i in 1 until points.lastIndex) {
            val prev = points[i - 1]
            val cur = points[i]
            val next = points[i + 1]
            val target = Offset(
                (prev.x + cur.x + next.x) / 3f,
                (prev.y + cur.y + next.y) / 3f
            )
            out += Offset(
                cur.x + (target.x - cur.x) * strength,
                cur.y + (target.y - cur.y) * strength
            )
        }
        out += points.last()
        return out
    }

    fun resampleBySpacing(points: List<Offset>, spacing: Float = 0.18f): List<Offset> {
        if (points.size < 2) return points
        val factor = spacing.coerceIn(0.02f, 1f)
        val out = ArrayList<Offset>()
        out += points.first()
        var carry = 0f
        for (i in 1 until points.size) {
            val a = points[i - 1]
            val b = points[i]
            val dx = b.x - a.x
            val dy = b.y - a.y
            val d = hypot(dx.toDouble(), dy.toDouble()).toFloat()
            if (d <= 0.001f) continue
            carry += d
            if (carry >= max(1f, d * factor)) {
                out += b
                carry = 0f
            }
        }
        if (out.last() != points.last()) out += points.last()
        return out
    }

    fun smooth(points: List<Offset>, config: Config = Config()): List<Offset> =
        resampleBySpacing(stabilize(points, config), config.spacing)

    fun pressureWidth(baseWidth: Float, pressure: Float, config: Config = Config()): Float {
        val p = normalizePressure(pressure, config)
        return baseWidth * (0.45f + 0.85f * p)
    }

    fun bounds(points: List<Offset>): Pair<Offset, Offset>? {
        if (points.isEmpty()) return null
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        for (p in points) {
            minX = min(minX, p.x); minY = min(minY, p.y)
            maxX = max(maxX, p.x); maxY = max(maxY, p.y)
        }
        return Offset(minX, minY) to Offset(maxX, maxY)
    }
}