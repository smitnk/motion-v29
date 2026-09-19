package com.smitnk.motioncanvas.selection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smitnk.motioncanvas.DrawPoint
import com.smitnk.motioncanvas.DrawStroke
import com.smitnk.motioncanvas.timeline.deepCopy
import com.smitnk.motioncanvas.ui.theme.*
import kotlin.math.*

/**
 * Axis-aligned bounding box in canvas coordinate space.
 */
data class BoundingBox(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
) {
    val width: Float get() = (maxX - minX).coerceAtLeast(1f)
    val height: Float get() = (maxY - minY).coerceAtLeast(1f)
    val center: Offset get() = Offset((minX + maxX) / 2f, (minY + maxY) / 2f)

    val topLeft: Offset get() = Offset(minX, minY)
    val topRight: Offset get() = Offset(maxX, minY)
    val bottomLeft: Offset get() = Offset(minX, maxY)
    val bottomRight: Offset get() = Offset(maxX, maxY)

    val topCenter: Offset get() = Offset((minX + maxX) / 2f, minY)
    val bottomCenter: Offset get() = Offset((minX + maxX) / 2f, maxY)
    val centerLeft: Offset get() = Offset(minX, (minY + maxY) / 2f)
    val centerRight: Offset get() = Offset(maxX, (minY + maxY) / 2f)

    fun rotateHandle(zoom: Float): Offset {
        val stemLength = 28f / zoom
        return topCenter - Offset(0f, stemLength)
    }

    fun contains(point: Offset): Boolean {
        return point.x in minX..maxX && point.y in minY..maxY
    }

    fun intersects(other: BoundingBox): Boolean {
        return minX <= other.maxX && maxX >= other.minX &&
                minY <= other.maxY && maxY >= other.minY
    }
}

/**
 * Transform handle types for scaling, rotating, and translating selection.
 */
enum class TransformHandle {
    NONE,
    BODY,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    TOP_CENTER,
    BOTTOM_CENTER,
    CENTER_LEFT,
    CENTER_RIGHT,
    ROTATE
}

/**
 * Computes bounding box for an individual stroke.
 */
fun DrawStroke.computeBounds(): BoundingBox? {
    if (points.isEmpty()) return null
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE
    var maxY = -Float.MAX_VALUE
    val padding = strokeWidth / 2f

    for (p in points) {
        if (p.x < minX) minX = p.x
        if (p.y < minY) minY = p.y
        if (p.x > maxX) maxX = p.x
        if (p.y > maxY) maxY = p.y
    }
    return BoundingBox(
        minX = minX - padding,
        minY = minY - padding,
        maxX = maxX + padding,
        maxY = maxY + padding
    )
}

/**
 * Computes the aggregate bounding box of multiple selected strokes.
 */
fun computeBoundingBox(strokes: List<DrawStroke>): BoundingBox? {
    if (strokes.isEmpty()) return null
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE
    var maxY = -Float.MAX_VALUE
    var hasPoints = false

    for (s in strokes) {
        val b = s.computeBounds() ?: continue
        hasPoints = true
        if (b.minX < minX) minX = b.minX
        if (b.minY < minY) minY = b.minY
        if (b.maxX > maxX) maxX = b.maxX
        if (b.maxY > maxY) maxY = b.maxY
    }
    return if (hasPoints) BoundingBox(minX, minY, maxX, maxY) else null
}

/**
 * Point-to-line segment squared distance for stroke hit testing.
 */
private fun distanceToSegmentSq(p: Offset, a: Offset, b: Offset): Float {
    val ab = b - a
    val l2 = ab.x * ab.x + ab.y * ab.y
    if (l2 == 0f) {
        val dx = p.x - a.x
        val dy = p.y - a.y
        return dx * dx + dy * dy
    }
    val t = (((p.x - a.x) * ab.x + (p.y - a.y) * ab.y) / l2).coerceIn(0f, 1f)
    val proj = a + Offset(ab.x * t, ab.y * t)
    val dx = p.x - proj.x
    val dy = p.y - proj.y
    return dx * dx + dy * dy
}

/**
 * Hit tests a stroke against a canvas touch point.
 */
fun hitTestStroke(stroke: DrawStroke, canvasPoint: Offset, threshold: Float = 24f): Boolean {
    val maxDist = stroke.strokeWidth / 2f + threshold
    val maxDistSq = maxDist * maxDist

    val bounds = stroke.computeBounds() ?: return false
    if (canvasPoint.x < bounds.minX - threshold || canvasPoint.x > bounds.maxX + threshold ||
        canvasPoint.y < bounds.minY - threshold || canvasPoint.y > bounds.maxY + threshold
    ) {
        return false
    }

    val pts = stroke.points
    for (i in pts.indices) {
        val p1 = Offset(pts[i].x, pts[i].y)
        val dx = p1.x - canvasPoint.x
        val dy = p1.y - canvasPoint.y
        if (dx * dx + dy * dy <= maxDistSq) return true

        if (i + 1 < pts.size) {
            val p2 = Offset(pts[i + 1].x, pts[i + 1].y)
            if (distanceToSegmentSq(canvasPoint, p1, p2) <= maxDistSq) {
                return true
            }
        }
    }
    return false
}

/**
 * Checks if a stroke falls within a marquee rectangle.
 */
fun isStrokeInMarquee(stroke: DrawStroke, marquee: BoundingBox): Boolean {
    val bounds = stroke.computeBounds() ?: return false
    if (marquee.intersects(bounds)) {
        return stroke.points.any { p -> marquee.contains(Offset(p.x, p.y)) } ||
                marquee.contains(bounds.center)
    }
    return false
}

/**
 * Hit tests handles on the active selection bounding box.
 */
fun hitTestHandle(bounds: BoundingBox, canvasPoint: Offset, zoom: Float): TransformHandle {
    val cornerRadius = 18f / zoom
    val cornerRadiusSq = cornerRadius * cornerRadius

    val rotateCenter = bounds.rotateHandle(zoom)
    val rotDx = canvasPoint.x - rotateCenter.x
    val rotDy = canvasPoint.y - rotateCenter.y
    if (rotDx * rotDx + rotDy * rotDy <= cornerRadiusSq * 1.5f) {
        return TransformHandle.ROTATE
    }

    val corners = listOf(
        Pair(bounds.topLeft, TransformHandle.TOP_LEFT),
        Pair(bounds.topRight, TransformHandle.TOP_RIGHT),
        Pair(bounds.bottomLeft, TransformHandle.BOTTOM_LEFT),
        Pair(bounds.bottomRight, TransformHandle.BOTTOM_RIGHT),
        Pair(bounds.topCenter, TransformHandle.TOP_CENTER),
        Pair(bounds.bottomCenter, TransformHandle.BOTTOM_CENTER),
        Pair(bounds.centerLeft, TransformHandle.CENTER_LEFT),
        Pair(bounds.centerRight, TransformHandle.CENTER_RIGHT)
    )

    for ((pos, handle) in corners) {
        val dx = canvasPoint.x - pos.x
        val dy = canvasPoint.y - pos.y
        if (dx * dx + dy * dy <= cornerRadiusSq) {
            return handle
        }
    }

    // Body hit test (move)
    if (bounds.contains(canvasPoint)) {
        return TransformHandle.BODY
    }

    return TransformHandle.NONE
}

// -------------------------------------------------------------------------------------------------
// Transformation Utilities (operate directly on artwork points and preserve all stroke properties)
// -------------------------------------------------------------------------------------------------

fun transformTranslate(strokes: List<DrawStroke>, delta: Offset): List<DrawStroke> {
    return strokes.map { stroke ->
        val newPoints = stroke.points.map { p ->
            DrawPoint(p.x + delta.x, p.y + delta.y)
        }
        stroke.copy(points = newPoints)
    }
}

fun transformRotate(strokes: List<DrawStroke>, center: Offset, angleRad: Float): List<DrawStroke> {
    val cosVal = cos(angleRad)
    val sinVal = sin(angleRad)
    return strokes.map { stroke ->
        val newPoints = stroke.points.map { p ->
            val dx = p.x - center.x
            val dy = p.y - center.y
            val nx = center.x + (dx * cosVal - dy * sinVal)
            val ny = center.y + (dx * sinVal + dy * cosVal)
            DrawPoint(nx, ny)
        }
        stroke.copy(points = newPoints)
    }
}

fun transformScale(
    strokes: List<DrawStroke>,
    pivot: Offset,
    scaleX: Float,
    scaleY: Float
): List<DrawStroke> {
    val sx = if (scaleX.isFinite() && abs(scaleX) > 0.001f) scaleX else 1.0f
    val sy = if (scaleY.isFinite() && abs(scaleY) > 0.001f) scaleY else 1.0f
    return strokes.map { stroke ->
        val newPoints = stroke.points.map { p ->
            val nx = pivot.x + (p.x - pivot.x) * sx
            val ny = pivot.y + (p.y - pivot.y) * sy
            DrawPoint(nx, ny)
        }
        stroke.copy(points = newPoints)
    }
}

fun transformFlipHorizontal(strokes: List<DrawStroke>, centerX: Float): List<DrawStroke> {
    return strokes.map { stroke ->
        val newPoints = stroke.points.map { p ->
            DrawPoint(2f * centerX - p.x, p.y)
        }
        stroke.copy(points = newPoints)
    }
}

fun transformFlipVertical(strokes: List<DrawStroke>, centerY: Float): List<DrawStroke> {
    return strokes.map { stroke ->
        val newPoints = stroke.points.map { p ->
            DrawPoint(p.x, 2f * centerY - p.y)
        }
        stroke.copy(points = newPoints)
    }
}

// -------------------------------------------------------------------------------------------------
// Canvas Drawing Overlay
// -------------------------------------------------------------------------------------------------

/**
 * Draws the active visual selection boundary, corner/edge handles, and rotation handle.
 */
fun DrawScope.drawSelectionOverlay(
    bounds: BoundingBox,
    zoom: Float,
    accentColor: Color = PinkAccent
) {
    val strokeWidth = (2f / zoom).coerceAtLeast(1.5f)
    val dashWidth = 8f / zoom
    val dashGap = 6f / zoom
    val handleRadius = (7f / zoom).coerceAtLeast(4f)
    val rotateRadius = (8f / zoom).coerceAtLeast(5f)

    val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(dashWidth, dashGap))

    // Bounding Box Rect
    drawRect(
        color = accentColor,
        topLeft = bounds.topLeft,
        size = Size(bounds.width, bounds.height),
        style = Stroke(width = strokeWidth, pathEffect = dashPathEffect)
    )

    // Semi-transparent interior tint
    drawRect(
        color = accentColor.copy(alpha = 0.04f),
        topLeft = bounds.topLeft,
        size = Size(bounds.width, bounds.height),
        style = Fill
    )

    // Stem connecting top-center to rotation handle
    val rotHandle = bounds.rotateHandle(zoom)
    drawLine(
        color = accentColor,
        start = bounds.topCenter,
        end = rotHandle,
        strokeWidth = strokeWidth
    )

    // Rotation Knob
    drawCircle(
        color = Color.White,
        radius = rotateRadius,
        center = rotHandle,
        style = Fill
    )
    drawCircle(
        color = accentColor,
        radius = rotateRadius,
        center = rotHandle,
        style = Stroke(width = strokeWidth)
    )
    drawCircle(
        color = accentColor,
        radius = rotateRadius * 0.4f,
        center = rotHandle,
        style = Fill
    )

    // Corner Handles
    val corners = listOf(
        bounds.topLeft,
        bounds.topRight,
        bounds.bottomLeft,
        bounds.bottomRight
    )
    for (c in corners) {
        drawCircle(
            color = Color.White,
            radius = handleRadius,
            center = c,
            style = Fill
        )
        drawCircle(
            color = accentColor,
            radius = handleRadius,
            center = c,
            style = Stroke(width = strokeWidth)
        )
    }

    // Edge Midpoint Handles
    val edges = listOf(
        bounds.topCenter,
        bounds.bottomCenter,
        bounds.centerLeft,
        bounds.centerRight
    )
    val edgeRadius = handleRadius * 0.75f
    for (e in edges) {
        drawCircle(
            color = Color.White,
            radius = edgeRadius,
            center = e,
            style = Fill
        )
        drawCircle(
            color = accentColor,
            radius = edgeRadius,
            center = e,
            style = Stroke(width = strokeWidth)
        )
    }
}

/**
 * Draws the active marquee selection drag rectangle.
 */
fun DrawScope.drawMarqueeBox(
    start: Offset,
    current: Offset,
    zoom: Float,
    accentColor: Color = PinkAccent
) {
    val minX = min(start.x, current.x)
    val minY = min(start.y, current.y)
    val maxX = max(start.x, current.x)
    val maxY = max(start.y, current.y)
    val width = (maxX - minX).coerceAtLeast(1f)
    val height = (maxY - minY).coerceAtLeast(1f)

    val strokeWidth = (1.8f / zoom).coerceAtLeast(1.2f)
    val dashWidth = 8f / zoom
    val dashGap = 6f / zoom
    val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(dashWidth, dashGap))

    // Shaded fill
    drawRect(
        color = accentColor.copy(alpha = 0.12f),
        topLeft = Offset(minX, minY),
        size = Size(width, height),
        style = Fill
    )

    // Dashed border
    drawRect(
        color = accentColor.copy(alpha = 0.85f),
        topLeft = Offset(minX, minY),
        size = Size(width, height),
        style = Stroke(width = strokeWidth, pathEffect = dashPathEffect)
    )
}

// -------------------------------------------------------------------------------------------------
// Floating Action Bar / Transform HUD
// -------------------------------------------------------------------------------------------------

@Composable
fun SelectionTransformBar(
    selectedCount: Int,
    canPaste: Boolean,
    onFlipHorizontal: () -> Unit,
    onFlipVertical: () -> Unit,
    onDuplicate: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onDelete: () -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp)),
        color = PanelBackground,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Item count indicator badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(PanelBackground2)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$selectedCount selected",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            VerticalDivider(
                modifier = Modifier
                    .height(20.dp)
                    .width(1.dp),
                color = BorderSubtle
            )

            // Flip Horizontal
            IconButton(
                onClick = onFlipHorizontal,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Flip,
                    contentDescription = "Flip Horizontal",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Flip Vertical
            IconButton(
                onClick = onFlipVertical,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = "Flip Vertical",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Duplicate
            IconButton(
                onClick = onDuplicate,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Duplicate Artwork",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Copy
            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CopyAll,
                    contentDescription = "Copy Artwork",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Paste
            IconButton(
                onClick = onPaste,
                enabled = canPaste,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = "Paste Artwork",
                    tint = if (canPaste) TextPrimary else TextSecondary.copy(alpha = 0.35f),
                    modifier = Modifier.size(18.dp)
                )
            }

            VerticalDivider(
                modifier = Modifier                    .height(20.dp)
                    .width(1.dp),
                color = BorderSubtle
            )

            // Delete
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Artwork",
                    tint = Color(0xFFEF5350),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Deselect / Clear Selection
            IconButton(
                onClick = onClearSelection,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear Selection",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}