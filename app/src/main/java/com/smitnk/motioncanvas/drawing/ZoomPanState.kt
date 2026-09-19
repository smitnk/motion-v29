/*
 * Zoom & Pan Transformation and Gesture State
 * Modeled after KDraw / SmartToolFactory gesture-transformation architecture:
 * - Decouples view transformation (zoom scale, pan offset) from underlying canvas coordinate space.
 * - Coordinates for strokes are stored in normalized canvas coordinate space (virtual coordinates),
 *   guaranteeing artwork coordinates remain 100% stable while zooming, panning, or resetting.
 * - Supports pinch-to-zoom, two-finger pan, dedicated Pan/Hand tool navigation, and clamp controls.
 */

package com.smitnk.motioncanvas.drawing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

class ZoomPanState(
    val minZoom: Float = 0.25f,
    val maxZoom: Float = 5.0f,
    initialZoom: Float = 1.0f,
    initialPan: Offset = Offset.Zero
) {
    var zoom by mutableFloatStateOf(initialZoom)
        private set

    var pan by mutableStateOf(initialPan)
        private set

    val zoomPercent: Int
        get() = (zoom * 100).toInt()

    fun updateTransform(zoomFactor: Float, panDelta: Offset, centroid: Offset = Offset.Zero) {
        val oldZoom = zoom
        val newZoom = (zoom * zoomFactor).coerceIn(minZoom, maxZoom)
        zoom = newZoom

        // Adjust pan relative to centroid so zooming pivots around user's fingers
        if (centroid != Offset.Zero && oldZoom != newZoom) {
            val scaleChange = newZoom / oldZoom
            pan = Offset(
                x = (pan.x - centroid.x) * scaleChange + centroid.x + panDelta.x,
                y = (pan.y - centroid.y) * scaleChange + centroid.y + panDelta.y
            )
        } else {
            pan += panDelta
        }
    }

    fun addPan(delta: Offset) {
        pan += delta
    }

    fun zoomIn(step: Float = 1.25f) {
        zoom = (zoom * step).coerceIn(minZoom, maxZoom)
    }

    fun zoomOut(step: Float = 0.8f) {
        zoom = (zoom * step).coerceIn(minZoom, maxZoom)
    }

    fun reset() {
        zoom = 1.0f
        pan = Offset.Zero
    }

    /**
     * Translates a touch coordinate from screen viewport space into the
     * canvas artwork coordinate space.
     */
    fun screenToCanvas(screenPoint: Offset): Offset {
        return Offset(
            x = (screenPoint.x - pan.x) / zoom,
            y = (screenPoint.y - pan.y) / zoom
        )
    }

    /**
     * Translates an artwork coordinate into screen viewport space.
     */
    fun canvasToScreen(canvasPoint: Offset): Offset {
        return Offset(
            x = canvasPoint.x * zoom + pan.x,
            y = canvasPoint.y * zoom + pan.y
        )
    }
}

@Composable
fun rememberZoomPanState(
    minZoom: Float = 0.25f,
    maxZoom: Float = 5.0f
): ZoomPanState {
    return remember {
        ZoomPanState(minZoom = minZoom, maxZoom = maxZoom)
    }
}