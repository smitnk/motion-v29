package com.smitnk.motioncanvas.project

data class ProjectFeatureManifest(
    val version:Int=24,
    val supportsLayers:Boolean=true,
    val supportsBlendModes:Boolean=true,
    val supportsClipping:Boolean=true,
    val supportsTweening:Boolean=true,
    val supportsMotionGuide:Boolean=true,
    val supportsMultiFrameTransform:Boolean=true,
    val supportsSmudge:Boolean=true,
    val supportsLiquify:Boolean=true,
    val supportsParticles:Boolean=true,
    val supportsRigging:Boolean=true,
    val supportsCamera:Boolean=true,
    val supportsWaveform:Boolean=true,
    val supportsSpriteSheet:Boolean=true,
    val supportsAutosave:Boolean=true
)