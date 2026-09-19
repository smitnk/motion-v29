package com.smitnk.motioncanvas.reference

data class ReferenceImage(
    val id: String,
    val uri: String,
    val x: Float = 0f,
    val y: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val opacity: Float = .5f,
    val visible: Boolean = true
)