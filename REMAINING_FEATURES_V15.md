# MotionCanvas V15 Remaining Features

Completed in this pass:
- MP4 export from animation frames.
- Optional AAC/M4A audio muxing onto the exported MP4.
- Video-to-animation-frame import pipeline with configurable FPS and frame cap.

Known limitations:
- Audio muxing currently attaches the first audio clip; full PCM mixing of overlapping multi-track clips remains a later pass.
- Export renderer covers strokes and text; advanced editor-only overlays are intentionally excluded from final video.