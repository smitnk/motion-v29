package com.smitnk.motioncanvas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smitnk.motioncanvas.animation.*
import com.smitnk.motioncanvas.camera.Camera2D
import com.smitnk.motioncanvas.selection.MultiFrameTransform

@Composable
fun AdvancedToolsPanel(
    state: AdvancedFeatureState,
    onStateChange: (AdvancedFeatureState)->Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.padding(8.dp), verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Text("Advanced Tools", style=MaterialTheme.typography.titleMedium)

        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            Button(onClick={
                onStateChange(state.copy(camera=state.camera.zoomed(1.15f)))
            }) { Text("Zoom +") }
            Button(onClick={
                onStateChange(state.copy(camera=state.camera.zoomed(.87f)))
            }) { Text("Zoom -") }
        }

        Text("Smudge radius: ${state.smudgeRadius.toInt()}")
        Slider(
            value=state.smudgeRadius,
            onValueChange={onStateChange(state.copy(smudgeRadius=it))},
            valueRange=2f..256f
        )

        Text("Smudge strength: ${(state.smudgeStrength*100).toInt()}%")
        Slider(
            value=state.smudgeStrength,
            onValueChange={onStateChange(state.copy(smudgeStrength=it))},
            valueRange=0f..1f
        )

        Text("Liquify strength: ${(state.liquifyStrength*100).toInt()}%")
        Slider(
            value=state.liquifyStrength,
            onValueChange={onStateChange(state.copy(liquifyStrength=it))},
            valueRange=0f..1f
        )

        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            Button(onClick={
                onStateChange(state.copy(referenceTransform=state.referenceTransform.copy(
                    flipX=!state.referenceTransform.flipX
                )))
            }) { Text("Flip X") }
            Button(onClick={
                onStateChange(state.copy(referenceTransform=state.referenceTransform.copy(
                    flipY=!state.referenceTransform.flipY
                )))
            }) { Text("Flip Y") }
        }

        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            Button(onClick={
                onStateChange(state.copy(referenceTransform=state.referenceTransform.copy(
                    rotationDegrees=state.referenceTransform.rotationDegrees-15f
                )))
            }) { Text("Rotate -") }
            Button(onClick={
                onStateChange(state.copy(referenceTransform=state.referenceTransform.copy(
                    rotationDegrees=state.referenceTransform.rotationDegrees+15f
                )))
            }) { Text("Rotate +") }
        }

        Text("Keyframe tools")
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            Button(onClick={
                val f=state.keyframes.lastOrNull()?.frame?.plus(6) ?: 0
                onStateChange(state.copy(keyframes=state.keyframes+GraphKeyframe(f,0f,TweenEasing.EASE_IN_OUT)))
            }) { Text("Add Key") }
            Button(onClick={
                onStateChange(state.copy(keyframes=state.keyframes.dropLast(1)))
            }, enabled=state.keyframes.isNotEmpty()) { Text("Delete Key") }
        }

        Text("Exposure / frame hold")
        Button(onClick={
            val f=state.exposures.lastOrNull()?.startFrame?.plus(1) ?: 0
            onStateChange(state.copy(exposures=state.exposures+Exposure(f,2)))
        }) { Text("Add 2-frame Hold") }

        Text("Rigging")
        Button(onClick={
            onStateChange(state.copy(bones=state.bones))
        }) { Text("Enable IK Controller") }

        Text("Particles")
        Button(onClick={
            onStateChange(state.copy(particles=ParticleEngine.emit(ParticleEmitter(0f,0f),24)))
        }) { Text("Emit 24") }
    }
}