package com.smitnk.motioncanvas.selection
import androidx.compose.ui.geometry.Offset
import com.smitnk.motioncanvas.DrawPoint
import com.smitnk.motioncanvas.DrawStroke
object LassoSelection {
    fun contains(point: Offset, polygon: List<Offset>): Boolean {
        if (polygon.size < 3) return false
        var inside = false; var j = polygon.lastIndex
        for (i in polygon.indices) {
            val a = polygon[i]; val b = polygon[j]
            val dy = b.y - a.y
            val intersects = ((a.y > point.y) != (b.y > point.y)) &&
                (point.x < (b.x - a.x) * (point.y - a.y) / if (kotlin.math.abs(dy) < 1e-5f) 1e-5f else dy + a.x)
            if (intersects) inside = !inside
            j = i
        }
        return inside
    }
    fun strokeIntersects(stroke: DrawStroke, polygon: List<Offset>): Boolean = stroke.points.any { contains(Offset(it.x, it.y), polygon) }
    fun close(points: List<DrawPoint>): List<Offset> = points.map { Offset(it.x, it.y) }
}