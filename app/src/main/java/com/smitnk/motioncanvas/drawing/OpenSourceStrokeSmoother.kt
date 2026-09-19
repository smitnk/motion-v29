package com.smitnk.motioncanvas.drawing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path

/**
 * Smooth freehand stroke path builder.
 *
 * Adapted from the quadratic-Bezier midpoint technique demonstrated by
 * SmartToolFactory/Compose-Drawing-App (MIT License).
 * Source: https://github.com/SmartToolFactory/Compose-Drawing-App
 *
 * This class is intentionally kept independent of the MotionCanvas UI so the
 * drawing engine can evolve without changing the editor screens.
 */
object OpenSourceStrokeSmoother {
    fun build(points: List<Offset>): Path {
        val path = Path()
        if (points.isEmpty()) return path
        if (points.size == 1) {
            path.moveTo(points[0].x, points[0].y)
            return path
        }

        path.moveTo(points[0].x, points[0].y)
        var previous = points[0]

        for (i in 1 until points.size) {
            val current = points[i]
            val midpoint = Offset(
                (previous.x + current.x) / 2f,
                (previous.y + current.y) / 2f
            )
            path.quadraticBezierTo(
                previous.x,
                previous.y,
                midpoint.x,
                midpoint.y
            )
            previous = current
        }

        path.lineTo(previous.x, previous.y)
        return path
    }
}