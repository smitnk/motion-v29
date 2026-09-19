package com.smitnk.motioncanvas

import androidx.compose.ui.geometry.Offset
import com.smitnk.motioncanvas.animation.*
import com.smitnk.motioncanvas.brush.SmudgeEngine
import com.smitnk.motioncanvas.camera.Camera2D
import com.smitnk.motioncanvas.project.AutosaveRecovery
import com.smitnk.motioncanvas.selection.*
import com.smitnk.motioncanvas.tools.*
import com.smitnk.motioncanvas.rigging.*

data class AdvancedFeatureState(
    val keyframes: List<GraphKeyframe> = emptyList(),
    val exposures: List<Exposure> = emptyList(),
    val selection: RectSelection? = null,
    val motionGuide: MotionGuide = MotionGuide(),
    val camera: Camera2D = Camera2D(),
    val referenceTransform: MultiFrameTransform = MultiFrameTransform(),
    val particles: List<Particle> = emptyList(),
    val bones: List<Bone> = emptyList(),
    val magicWandTolerance: Int = 24,
    val smudgeRadius: Float = 32f,
    val smudgeStrength: Float = .65f,
    val liquifyRadius: Float = 48f,
    val liquifyStrength: Float = .5f
)

object AdvancedFeatureController {
    fun valueAt(state: AdvancedFeatureState, frame: Int): Float =
        KeyframeGraphEngine.evaluate(state.keyframes, frame)

    fun exposedFrame(state: AdvancedFeatureState, frame: Int): Int =
        ExposureTrackEngine.frameAt(state.exposures, frame)

    fun guidePoint(state: AdvancedFeatureState, progress: Float): Offset? =
        MotionGuideEngine.sample(state.motionGuide, progress)

    fun camera(state: AdvancedFeatureState, dx: Float, dy: Float, zoom: Float): Camera2D =
        state.camera.moved(dx,dy).zoomed(zoom)

    fun solveIK(state: AdvancedFeatureState, target: Offset): List<Bone> =
        IKChainEngine.solve(state.bones,target)

    fun selectionBounds(points: List<Offset>): RectSelection? =
        AdvancedSelectionEngine.bounds(points)

    fun selectionContains(selection: List<Offset>, point: Offset): Boolean =
        AdvancedSelectionEngine.polygonContains(selection,point)

    fun newParticles(state: AdvancedFeatureState, count: Int): List<Particle> =
        ParticleEngine.emit(ParticleEmitter(0f,0f),count)
}