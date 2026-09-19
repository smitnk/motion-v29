# MotionCanvas V28 Full Wiring

## Wired
- Workspace visibility state is shared from root -> More screen -> Editor.
- More screen now controls both normal widgets and advanced engine switches.
- Advanced engine state is centralized in `AdvancedWorkspaceState`.
- `AdvancedEngineWiring.kt` provides a UI-to-engine bridge for:
  - Motion Guide
  - Camera
  - Keyframes
  - Perspective guide
  - Ruler
  - Bezier
  - Magic Wand
  - Liquify
  - Particles
  - Smudge
  - Brush dynamics
- Existing AdvancedToolsPanel receives the shared engine state.

## Open-source references
Dolphin Animate (MIT) documents pressure-aware smoothing, exposure/timeline workflow, free transform, fill and motion-guide concepts.
FrameBaker (MIT) documents frame editing, onion skin, transforms, keyframes and batch timeline operations.
These projects were used as feature references; their full applications are not embedded.

## Important
This is source-level wiring. It has not been claimed as a successful Android APK build or real-device regression test.