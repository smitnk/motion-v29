# Open Source Notices — V16

## AndroidX Media3 Transformer
- License: Apache-2.0
- Used as an API dependency for multi-track composition/export.
- MotionCanvas uses `Composition`, `EditedMediaItemSequence`, clipping, gaps, and audio processors to mix overlapping audio sequences into one MP4 audio track.
- Source/documentation: https://developer.android.com/media/media3/transformer/composition

## Rotoscoping
- The V16 rotoscope importer is independently implemented with Android `MediaMetadataRetriever`.
- No proprietary repository source was copied.
- The pipeline is designed around the same general frame-sampling workflow used by open-source animation/video editors.