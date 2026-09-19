# MotionCanvas V4 feature layer

Implemented foundations in this build:
- Brush presets (pencil, ink, marker, airbrush)
- Symmetry engine (vertical, horizontal, radial)
- Shape engine (line, rectangle, ellipse)
- JSON project persistence with atomic per-file replacement point
- PNG sequence exporter
- Animated GIF export utility
- Audio clip/track model and MediaPlayer preview controller
- Reference image model
- Rotoscope frame model
- Existing pressure-aware drawing and stroke smoothing retained

Still requires editor/UI wiring and device validation:
- Expose brush preset picker in toolbar
- Expose symmetry and shape tools in editor
- Wire Save/Open to ProjectRepository
- Build the timeline audio UI and waveform generation
- Production MP4 encoder/export pipeline (MediaCodec)
- GPU texture brushes / advanced blend modes
- Full lasso selection and boolean shape operations
- Multi-touch/stylus tilt UI calibration

Open-source note:
The project already contains the MIT-licensed stroke-smoothing adaptation from SmartToolFactory/Compose-Drawing-App. New V4 utilities are original integration code and do not copy a third-party repository wholesale.