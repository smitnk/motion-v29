# V26 Audit

## Fully present in the ZIP at engine/source level
- Drawing canvas, brush, eraser, undo/redo
- Pressure/stylus foundation and smoothing
- Brush presets/custom presets
- Texture/advanced brush
- Smudge/smear
- Shapes, text, arrows
- Flood fill
- Lasso/selection
- Magic wand
- Transform / multi-frame transform engine
- Clipping masks and blend modes
- Symmetry
- Onion skin
- Timeline, exposure, batch frame operations
- Tweening/easing
- Motion Guide
- Keyframe graph/interpolation
- Rotoscope/video import
- Chroma key
- Audio tracks, trim/fade, preview and waveform
- MP4/GIF/PNG/spritesheet exporters
- Autosave/recovery
- Particles
- IK rigging engine
- 2D camera
- Liquify
- Perspective guide engine
- Precision ruler
- Bezier path engine
- Per-frame layer data model
- Rotoscope memory planning
- Brush dynamics

## Present but NOT honestly verified
- Full UI wiring of every advanced engine
- Device/emulator behavior
- MP4 audio mixing across hardware codecs
- Large rotoscope performance
- Crash recovery in a real crash
- Final APK release build

## Important V25 correction
V25 accidentally placed `AdvancedToolsPanel` inside the audio dialog's `confirmButton` block. V26 removes that invalid placement so the ZIP is not claiming that panel is correctly mounted there.

## What remains
The remaining work is integration/verification rather than another large collection of engines:
1. Put each tool into the correct toolbar/tool mode.
2. Connect each engine to actual canvas/timeline state.
3. Persist every new state in project JSON.
4. Compile and run on Android.
5. Test touch/stylus, memory, codecs, audio, and rotoscope paths.