# MotionCanvas V27 Open-Source Notices

## Feature references / licenses
- Dolphin Animate — MIT. Reference: pressure-aware smooth vector drawing, spacing, exposure/timeline workflow, free transform, fill and export concepts.
- FrameBaker — MIT. Reference: frame editor, onion skin, per-frame transforms/duration, batch timeline operations and skeletal-motion workflow.
- Klecks — MIT. Reference: pressure/stabilizer drawing, touch gestures, brush families, selection, bucket, transform/warp and perspective concepts.
- OpenToonz — Modified BSD for the main project; third-party directories and MyPaint brushes have separate licenses. Reference only for production animation/timeline/drawing concepts.

## Implementation rule
MotionCanvas V27 does **not** bundle the upstream Dolphin Animate, FrameBaker, Klecks or OpenToonz applications or their source trees. The Android code added here is an independent Kotlin/Jetpack Compose implementation of compatible drawing concepts.

The OpenToonz repository specifically requires checking licenses for third-party directories, so those components were not copied into MotionCanvas.