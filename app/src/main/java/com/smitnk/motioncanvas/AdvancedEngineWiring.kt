package com.smitnk.motioncanvas

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import com.smitnk.motioncanvas.animation.GraphKeyframe
import com.smitnk.motioncanvas.animation.KeyframeGraphEngine
import com.smitnk.motioncanvas.animation.MotionGuideEngine
import com.smitnk.motioncanvas.brush.BrushDynamics
import com.smitnk.motioncanvas.brush.BrushDynamicsEngine
import com.smitnk.motioncanvas.brush.SmudgeEngine
import com.smitnk.motioncanvas.camera.Camera2D
import com.smitnk.motioncanvas.tools.BezierPath
import com.smitnk.motioncanvas.tools.BezierPathEngine
import com.smitnk.motioncanvas.tools.LiquifyEngine
import com.smitnk.motioncanvas.tools.MagicWandEngine
import com.smitnk.motioncanvas.tools.Particle
import com.smitnk.motioncanvas.tools.ParticleEmitter
import com.smitnk.motioncanvas.tools.ParticleEngine
import com.smitnk.motioncanvas.tools.PerspectiveGuide
import com.smitnk.motioncanvas.tools.PerspectiveGuideEngine
import com.smitnk.motioncanvas.tools.PrecisionRulerEngine

/**
 * Adapter layer between the Compose UI and the independently implemented
 * MotionCanvas feature engines. This class intentionally uses the actual
 * engine APIs present in v29.
 */
class AdvancedEngineWiring {

    fun guidePoint(points: List<Offset>, position: Float): Offset? =
        MotionGuideEngine.sample(com.smitnk.motioncanvas.animation.MotionGuide(points), position)

    fun cameraScale(base: Float, zoom: Float): Float =
        Camera2D(zoom = base).zoomed(zoom).zoom

    fun keyframeValue(frame: Int, keys: List<GraphKeyframe>): Float =
        KeyframeGraphEngine.evaluate(keys, frame)

    fun perspectivePoint(point: Offset, guide: PerspectiveGuide): Offset =
        PerspectiveGuideEngine.snap(point, guide)

    fun rulerDistance(a: Offset, b: Offset): Float =
        PrecisionRulerEngine.measure(a, b).length

    fun bezierSample(
        p0: Offset, p1: Offset, p2: Offset, p3: Offset, t: Float
    ): Offset =
        BezierPathEngine.sample(BezierPath(p0, p1, p2, p3), t)

    fun selectionMask(
        bitmap: Bitmap,
        point: Offset,
        tolerance: Int
    ): BooleanArray =
        MagicWandEngine.select(
            bitmap = bitmap,
            x = point.x.toInt(),
            y = point.y.toInt(),
            tolerance = tolerance
        )

    fun liquify(
        bitmap: Bitmap,
        center: Offset,
        delta: Offset,
        radius: Float,
        strength: Float
    ) {
        LiquifyEngine.push(
            bitmap = bitmap,
            cx = center.x,
            cy = center.y,
            dx = delta.x,
            dy = delta.y,
            radius = radius,
            strength = strength
        )
    }

    fun particleBurst(origin: Offset, count: Int): List<Particle> =
        ParticleEngine.emit(
            ParticleEmitter(x = origin.x, y = origin.y, rate = count),
            count
        )

    fun smudge(
        bitmap: Bitmap,
        from: Offset,
        to: Offset,
        radius: Float,
        strength: Float
    ) {
        SmudgeEngine.smear(bitmap, from, to, radius, strength)
    }

    fun dynamicBrushWidth(
        baseWidth: Float,
        pressure: Float,
        dynamics: BrushDynamics = BrushDynamics()
    ): Float =
        BrushDynamicsEngine.size(baseWidth, pressure, dynamics)
}
