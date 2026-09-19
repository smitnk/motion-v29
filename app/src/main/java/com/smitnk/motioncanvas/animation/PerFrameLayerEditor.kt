package com.smitnk.motioncanvas.animation

import com.smitnk.motioncanvas.DrawStroke

data class FrameLayerData(
    val frame: Int,
    val layerIndex: Int,
    val strokes: List<DrawStroke>
)

object PerFrameLayerEditor {
    fun replace(
        data: List<FrameLayerData>,
        frame: Int,
        layer: Int,
        strokes: List<DrawStroke>
    ): List<FrameLayerData> {
        val out = data.toMutableList()
        val index = out.indexOfFirst { it.frame == frame && it.layerIndex == layer }
        if (index >= 0) out[index] = FrameLayerData(frame, layer, strokes)
        else out += FrameLayerData(frame, layer, strokes)
        return out
    }

    fun strokes(data: List<FrameLayerData>, frame: Int, layer: Int): List<DrawStroke> =
        data.firstOrNull { it.frame == frame && it.layerIndex == layer }?.strokes.orEmpty()
}
