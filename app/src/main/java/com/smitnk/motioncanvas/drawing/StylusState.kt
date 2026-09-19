package com.smitnk.motioncanvas.drawing

data class StylusState(
    val pressure: Float = 1f,
    val tiltX: Float = 0f,
    val tiltY: Float = 0f,
    val orientation: Float = 0f,
    val isStylus: Boolean = false
)

object StylusBrushModel {
    fun size(base: Float, state: StylusState, pressureSensitivity: Float = 1f): Float {
        val p = state.pressure.coerceIn(.05f, 1.5f)
        return (base * (1f + (p - 1f) * pressureSensitivity)).coerceAtLeast(.5f)
    }

    fun opacity(base: Float, state: StylusState, pressureSensitivity: Float = .5f): Float =
        (base * (1f + (state.pressure.coerceIn(0f,1f)-.5f) * pressureSensitivity)).coerceIn(0f,1f)
}