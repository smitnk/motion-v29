package com.smitnk.motioncanvas.brush

import androidx.compose.ui.graphics.Color
import java.util.UUID

data class CustomBrushPreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val color: Color,
    val width: Float,
    val alpha: Float = color.alpha,
    val smoothing: Float = 0.65f
)