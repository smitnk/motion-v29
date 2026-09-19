package com.smitnk.motioncanvas.brush

/** Built-in presets kept independent from the UI so the editor can expose them as needed. */
data class BrushPreset(
    val id: String,
    val name: String,
    val sizeMultiplier: Float,
    val opacity: Float,
    val spacing: Float,
    val pressureCurve: Float = 1f,
    val smoothing: Float = 0.5f
)

object BrushPresets {
    val Pencil = BrushPreset("pencil", "Pencil", 1f, .9f, .12f, 1.15f, .7f)
    val Ink = BrushPreset("ink", "Ink", 1.15f, 1f, .08f, .9f, .45f)
    val Marker = BrushPreset("marker", "Marker", 2.2f, .65f, .18f, .75f, .25f)
    val Airbrush = BrushPreset("airbrush", "Airbrush", 3.5f, .28f, .05f, .55f, .2f)
    val presets = listOf(Pencil, Ink, Marker, Airbrush)
}