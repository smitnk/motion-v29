# MotionCanvas V17

Two feature pass:

1. Interactive audio clip manipulation model
   - Move clips on the frame timeline.
   - Trim left/right boundaries.
   - Preserve frame-based timing.

2. Direct reference transform interaction model
   - Pan.
   - Pinch/zoom.
   - Rotate.
   - Opacity and lock state are represented.
   - Locked references ignore gestures.

The interaction logic is independently implemented in Kotlin. Open-source projects are used only as feature/architecture references unless their license permits source reuse.