# V27 Hide/Unhide + Drawing Wiring Audit

Implemented:
- New `ScreenType.MORE` workspace-controls screen.
- Immediate hide/unhide switches for:
  - left toolbar
  - top toolbar/widgets
  - timeline
  - reference widget
  - frame tools widget
  - audio/recording widget
  - advanced animation widget
  - pro tools widget
  - brush presets widget
  - color widget
- `Show all` and `Minimal` workspace presets.
- More button in the editor top bar.
- Visibility state is passed directly into `EditorScreen`.
- New `OpenSourceDrawingEngine.kt` provides independent stabilizer, spacing/resampling, pressure-width and smoothing helpers.
- Existing stroke rendering paths now run through the new engine before the existing path builder.

Open-source references:
- Dolphin Animate (MIT)
- FrameBaker (MIT)
- Klecks (MIT)
- OpenToonz (Modified BSD main project; third-party licenses vary)

Not claimed:
- The complete Dolphin Animate, FrameBaker, Klecks or OpenToonz applications are not embedded.
- Final APK/device validation was not performed in this ZIP generation step.