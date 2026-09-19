# Open-source notices — Pro features

MotionCanvas adds two advanced features in this build using compatible open-source technology and references:

1. **Layer clipping masks and blend modes** — implemented with Android `Canvas`/`PorterDuff` compositing APIs. Android platform source is available under Apache-2.0-compatible licensing.
2. **Textured brush** — implemented with Android Canvas raster primitives and a procedural grain algorithm. Brush-engine direction was informed by open-source drawing projects including SmartToolFactory/Compose-Drawing-App (MIT) and Hokusai (MIT/Apache-2.0).
3. **Drawing/canvas architecture reference** — sameerasw/Canvas (MPL-2.0) was used as a feature/architecture reference in the MotionCanvas project. No repository UI or unrelated architecture is copied here.

MotionCanvas does not copy proprietary/all-rights-reserved code. When upstream source is directly incorporated in future changes, its license and required notices must be preserved.