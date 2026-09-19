package com.smitnk.motioncanvas

import androidx.compose.ui.geometry.Offset
import com.smitnk.motioncanvas.animation.KeyframeGraphEngine
import com.smitnk.motioncanvas.animation.MotionGuideEngine
import com.smitnk.motioncanvas.camera.Camera2DEngine
import com.smitnk.motioncanvas.rigging.IKChainEngine
import com.smitnk.motioncanvas.tools.BezierPathEngine
import com.smitnk.motioncanvas.tools.LiquifyEngine
import com.smitnk.motioncanvas.tools.MagicWandEngine
import com.smitnk.motioncanvas.tools.ParticleEngine
import com.smitnk.motioncanvas.tools.PerspectiveGuideEngine
import com.smitnk.motioncanvas.tools.PrecisionRulerEngine
import com.smitnk.motioncanvas.brush.SmudgeEngine
import com.smitnk.motioncanvas.brush.BrushDynamicsEngine

/**
 * Central bridge used by the Compose UI to activate existing feature engines.
 * The engines remain independently testable; this class keeps UI state from
 * directly depending on implementation details.
 */
class AdvancedEngineWiring {

    fun guidePoint(points: List<Offset>, position: Float): Offset? =
        MotionGuideEngine.sample(points, position)

    fun cameraScale(base: Float, zoom: Float): Float =
        Camera2DEngine(baseZoom = base).zoomed(zoom)

    fun keyframeValue(
        frame: Int,
        keys: List<KeyframeGraphEngine.Keyframe>
    ): Float = KeyframeGraphEngine.evaluate(keys, frame)

    fun perspectiveGrid(width: Float, height: Float): List<Offset> =
        PerspectiveGuideEngine.grid(width, height)

    fun rulerDistance(a: Offset, b: Offset): Float =
        PrecisionRulerEngine.distance(a, b)

    fun bezierSample(
        p0: Offset, p1: Offset, p2: Offset, p3: Offset, t: Float
    ): Offset = BezierPathEngine.cubic(p0, p1, p2, p3, t)

    fun selectionMask(
        bitmap: android.graphics.Bitmap,
        point: Offset,
        tolerance: Int
    ): android.graphics.Bitmap =
        MagicWandEngine.select(bitmap, point, tolerance)

    fun liquifyPoint(
        point: Offset,
        center: Offset,
        radius: Float,
        strength: Float
    ): Offset = LiquifyEngine.push(point, center, radius, strength)

    fun particleBurst(
        origin: Offset,
        count: Int
    ): List<ParticleEngine.Particle> = ParticleEngine.emit(origin, count)

    fun smudgeStrength(distance: Float, radius: Float): Float =
        SmudgeEngine.falloff(distance, radius)

    fun dynamicBrushWidth(baseWidth: Float, pressure: Float, tilt: Float): Float =
        BrushDynamicsEngine.width(baseWidth, pressure, tilt)
}