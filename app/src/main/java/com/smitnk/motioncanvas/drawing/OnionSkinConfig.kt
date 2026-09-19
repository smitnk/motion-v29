package com.smitnk.motioncanvas.drawing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Configuration and reactive state for Onion Skinning in MotionCanvas.
 * Controls frame visibility ranges (previous and next), ghost opacity, and color tinting.
 */
class OnionSkinState(
    initialEnabled: Boolean = true,
    initialPrevFrames: Int = 1,
    initialNextFrames: Int = 1,
    initialOpacity: Float = 0.35f,
    initialColoredTint: Boolean = true
) {
    var enabled by mutableStateOf(initialEnabled)
    var prevFrames by mutableIntStateOf(initialPrevFrames)
    var nextFrames by mutableIntStateOf(initialNextFrames)
    var opacity by mutableFloatStateOf(initialOpacity)
    var coloredTint by mutableStateOf(initialColoredTint)

    fun toggle() {
        enabled = !enabled
    }

    val prevFramesText: String
        get() = "$prevFrames ${if (prevFrames == 1) "frame" else "frames"}"

    val nextFramesText: String
        get() = "$nextFrames ${if (nextFrames == 1) "frame" else "frames"}"

    val opacityPercent: Int
        get() = (opacity * 100).toInt()
}

@Composable
fun rememberOnionSkinState(
    initialEnabled: Boolean = true,
    initialPrevFrames: Int = 1,
    initialNextFrames: Int = 1,
    initialOpacity: Float = 0.35f,
    initialColoredTint: Boolean = true
): OnionSkinState {
    return remember {
        OnionSkinState(
            initialEnabled = initialEnabled,
            initialPrevFrames = initialPrevFrames,
            initialNextFrames = initialNextFrames,
            initialOpacity = initialOpacity,
            initialColoredTint = initialColoredTint
        )
    }
}