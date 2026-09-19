# Open-source feature integration — V6

## sameerasw/Canvas
Repository: https://github.com/sameerasw/Canvas
License: Mozilla Public License 2.0 (MPL-2.0)

MotionCanvas V6 adapts the feature concepts and interaction model demonstrated by this repository for:
- Arrow drawing
- Shape tool with rectangle, circle, triangle and line modes
- Dedicated drawing-tool separation

No source file from sameerasw/Canvas is copied into MotionCanvas V6. The feature behavior was reimplemented against MotionCanvas's existing vector-stroke and animation-frame model.

The repository also contains stylus-point, text, crop and Room persistence implementations that remain candidates for later direct integration after dependency/API review.

## SmartToolFactory/Compose-Drawing-App
Repository: https://github.com/SmartToolFactory/Compose-Drawing-App
License: MIT

Existing MotionCanvas drawing history and path smoothing already contain the documented MIT-derived integration from this project. See OPEN_SOURCE_NOTICES.md.

## V7 integration note
- `sameerasw/Canvas` (MPL-2.0) — TextItem data model pattern was adapted into MotionCanvas as `CanvasText`; MotionCanvas retains its own editor architecture and UI.
- Source: https://github.com/sameerasw/Canvas/blob/main/app/src/main/java/com/sameerasw/canvas/data/TextItem.kt