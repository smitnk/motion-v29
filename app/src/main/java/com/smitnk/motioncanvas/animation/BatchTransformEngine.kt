package com.smitnk.motioncanvas.animation

import com.smitnk.motioncanvas.DrawStroke
import com.smitnk.motioncanvas.selection.MultiFrameTransform
import com.smitnk.motioncanvas.selection.MultiFrameTransformEngine

object BatchTransformEngine {
    fun apply(
        frames: List<List<DrawStroke>>,
        transform: MultiFrameTransform,
        from: Int,
        to: Int
    ): List<List<DrawStroke>> =
        frames.mapIndexed { index, frame ->
            if (index in from..to) MultiFrameTransformEngine.applyToFrames(listOf(frame), transform).first()
            else frame
        }
}
