package com.smitnk.motioncanvas.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smitnk.motioncanvas.DrawPoint
import com.smitnk.motioncanvas.DrawStroke
import com.smitnk.motioncanvas.Frame
import com.smitnk.motioncanvas.Project
import com.smitnk.motioncanvas.ui.theme.*

/**
 * Deep copies a DrawStroke with fresh id and cloned point coordinates.
 */
fun DrawStroke.deepCopy(): DrawStroke = DrawStroke(
    id = java.util.UUID.randomUUID().toString(),
    points = points.map { DrawPoint(it.x, it.y, it.pressure) },
    color = color,
    strokeWidth = strokeWidth,
    alpha = alpha,
    isEraser = isEraser
)

/**
 * Deep copies an animation Frame, creating independent lists of strokes
 * so future drawing actions on the copy do not modify the original.
 */
fun Frame.deepCopy(): Frame = Frame(
    id = java.util.UUID.randomUUID().toString(),
    strokes = strokes.map { it.deepCopy() }.toMutableList(),
    redoStrokes = redoStrokes.map { it.deepCopy() }.toMutableList(),
    fills = fills.toMutableList(),
    durationFrames = durationFrames,
    isKeyframe = isKeyframe,
    tag = tag,
    tagColor = tagColor
)

/**
 * High-fidelity thumbnail canvas rendering all vector strokes for a given frame.
 */
@Composable
fun FrameThumbnail(
    frame: Frame,
    canvasW: Int = 1280,
    canvasH: Int = 720,
    backgroundColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (frame.strokes.isEmpty()) {
                return@Canvas
            }

            val scaleX = size.width / canvasW.coerceAtLeast(1)
            val scaleY = size.height / canvasH.coerceAtLeast(1)
            val renderScale = minOf(scaleX, scaleY)

            scale(scale = renderScale, pivot = Offset.Zero) {
                frame.strokes.forEach { stroke ->
                    if (stroke.points.size > 1) {
                        val path = Path().apply {
                            moveTo(stroke.points[0].x, stroke.points[0].y)
                            for (i in 1 until stroke.points.size) {
                                lineTo(stroke.points[i].x, stroke.points[i].y)
                            }
                        }
