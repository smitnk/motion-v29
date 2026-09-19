package com.smitnk.motioncanvas.reference
import androidx.compose.ui.geometry.Offset
data class ReferenceTransform(val position: Offset = Offset.Zero, val scale: Float = 1f, val rotationDegrees: Float = 0f, val opacity: Float = 1f, val locked: Boolean = false)