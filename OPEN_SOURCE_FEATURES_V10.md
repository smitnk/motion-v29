# MotionCanvas V10 — Open-source feature integration

This release connects previously isolated engines into the editor and adds independently implemented features based on documented open-source animation workflows.

## Connected engines
- Lasso selection: freeform polygon selects strokes on the active frame.
- Flood fill: rasterizes the current vector boundaries, applies `FloodFillEngine`, then composites the fill under the vector strokes.
- Autosave: project state is periodically encoded through `ProjectRepository` into the existing autosave store.
- Keyframes: frames can be marked as keyframes.
- Variable frame duration: playback now honors each frame's duration.
- Frame tags: labels can be attached to individual frames.
- Reference images: image import plus scale/rotation/opacity/lock controls.
- Voice recording: Android microphone -> AAC/M4A, attached to the project and previewable.

## New feature references
The feature set is informed by public open-source projects including ToonFrame Studio and sameerasw/Canvas. MotionCanvas V10 uses its own Kotlin implementation rather than copying proprietary source.

See `OPEN_SOURCE_NOTICES_V10.md` for attribution/licensing notes.