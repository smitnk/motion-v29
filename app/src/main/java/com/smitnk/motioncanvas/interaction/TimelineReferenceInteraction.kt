package com.smitnk.motioncanvas.interaction

/**
 * MotionCanvas V17 interaction helpers.
 *
 * Inspired by common open-source animation-editor interaction patterns.
 * No upstream source is copied here.
 */
data class ClipBounds(val start: Int, val endExclusive: Int) {
    val duration: Int get() = (endExclusive - start).coerceAtLeast(1)
}

object AudioClipInteraction {
    fun move(bounds: ClipBounds, newStart: Int): ClipBounds =
        ClipBounds(newStart.coerceAtLeast(0), newStart.coerceAtLeast(0) + bounds.duration)

    fun trimLeft(bounds: ClipBounds, newStart: Int): ClipBounds {
        val s = newStart.coerceIn(0, bounds.endExclusive - 1)
        return ClipBounds(s, bounds.endExclusive)
    }

    fun trimRight(bounds: ClipBounds, newEndExclusive: Int): ClipBounds {
        val e = newEndExclusive.coerceIn(bounds.start + 1, Int.MAX_VALUE)
        return ClipBounds(bounds.start, e)
    }
}

data class ReferenceTransform(
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val scale: Float = 1f,
    val rotationDegrees: Float = 0f,
    val opacity: Float = 1f,
    val locked: Boolean = false
)

object ReferenceInteraction {
    fun applyGesture(
        state: ReferenceTransform,
        panX: Float,
        panY: Float,
        zoom: Float,
        rotation: Float
    ): ReferenceTransform {
        if (state.locked) return state
        return state.copy(
            translationX = state.translationX + panX,
            translationY = state.translationY + panY,
            scale = (state.scale * zoom).coerceIn(0.05f, 20f),
            rotationDegrees = state.rotationDegrees + rotation
        )
    }
}