package com.smitnk.motioncanvas.drawing

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import kotlin.math.sqrt

/**
 * Custom pointer input modifier that simultaneously handles:
 * 1. Two-finger pinch-to-zoom and two-finger pan (navigation gesture)
 * 2. Dedicated Pan tool navigation when tool is Pan/Hand
 * 3. Single-finger artwork drawing when in a drawing tool (Brush, Eraser, etc.)
 *
 * Automatically detects multi-touch gestures and aborts any active single-touch drawing
 * to prevent accidental drawing marks when the user intends to pan or zoom.
 */
suspend fun PointerInputScope.detectZoomPanOrDraw(
    isPanTool: Boolean,
    zoomPanState: ZoomPanState,
    onDrawStart: (Offset, Float) -> Unit,
    onDraw: (Offset, Float) -> Unit,
    onDrawEnd: () -> Unit
) {
    forEachGesture {
        awaitPointerEventScope {
            // Wait for first pointer down
            val down = awaitFirstDown(requireUnconsumed = false)
            var isMultiTouchNavigation = false
            var isDrawing = false

            // If we are in dedicated Pan tool mode, handle single-finger or multi-finger pan
            if (isPanTool) {
                var prevPosition = down.position
                down.consume()

                while (true) {
                    val event = awaitPointerEvent()
                    val activePointers = event.changes.filter { it.pressed }
                    if (activePointers.isEmpty()) break

                    if (activePointers.size >= 2) {
                        // Multi-touch pinch & pan
                        val p1 = activePointers[0].position
                        val p2 = activePointers[1].position
                        val prevP1 = activePointers[0].previousPosition
                        val prevP2 = activePointers[1].previousPosition

                        val currentDist = (p1 - p2).getDistance()
                        val prevDist = (prevP1 - prevP2).getDistance()

                        val zoomFactor = if (prevDist > 0f) currentDist / prevDist else 1.0f
                        val centroid = (p1 + p2) / 2f
                        val prevCentroid = (prevP1 + prevP2) / 2f
                        val panDelta = centroid - prevCentroid

                        zoomPanState.updateTransform(zoomFactor, panDelta, centroid)
                        activePointers.forEach { it.consume() }
                    } else {
                        // Single pointer pan
                        val change = activePointers.first()
                        val delta = change.position - prevPosition
                        zoomPanState.addPan(delta)
                        prevPosition = change.position
                        change.consume()
                    }
                }
                return@awaitPointerEventScope
            }

            // Otherwise, we are in a drawing tool (e.g. Brush or Eraser)
            // Initially start single-touch drawing in transformed canvas artwork coordinates
            val initialCanvasPt = zoomPanState.screenToCanvas(down.position)
            onDrawStart(initialCanvasPt, stylusPressure(down))
            isDrawing = true
            down.consume()

            while (true) {
                val event = awaitPointerEvent()
                val activePointers = event.changes.filter { it.pressed }

                if (activePointers.isEmpty()) {
                    if (isDrawing) {
                        onDrawEnd()
                        isDrawing = false
                    }
                    break
                }

                if (activePointers.size >= 2) {
                    // Two fingers detected! Abort accidental drawing immediately
                    if (isDrawing) {
                        onDrawEnd()
                        isDrawing = false
                    }
                    isMultiTouchNavigation = true

                    val p1 = activePointers[0].position
                    val p2 = activePointers[1].position
                    val prevP1 = activePointers[0].previousPosition
                    val prevP2 = activePointers[1].previousPosition

                    val currentDist = (p1 - p2).getDistance()
                    val prevDist = (prevP1 - prevP2).getDistance()

                    val zoomFactor = if (prevDist > 0f) currentDist / prevDist else 1.0f
                    val centroid = (p1 + p2) / 2f
                    val prevCentroid = (prevP1 + prevP2) / 2f
                    val panDelta = centroid - prevCentroid

                    zoomPanState.updateTransform(zoomFactor, panDelta, centroid)
                    activePointers.forEach { it.consume() }
                } else if (!isMultiTouchNavigation && isDrawing) {
                    // Continue drawing with single finger, converting screen touch to canvas coordinates
                    val change = activePointers.first()
                    val canvasPt = zoomPanState.screenToCanvas(change.position)
                    onDraw(canvasPt, stylusPressure(change))
                    change.consume()
                } else {
                    activePointers.forEach { it.consume() }
                }
            }
        }
    }
}


private fun stylusPressure(change: PointerInputChange): Float {
    val type = change.type
    val pressure = change.pressure
    // Finger/mouse input has no useful stylus pressure for artwork sizing.
    // Stylus input uses the platform pressure value and is normalized for stable brush behavior.
    return if (type == PointerType.Stylus) pressure.coerceIn(0.05f, 1.5f) else 1.0f
}