# MotionCanvas - Android Animation Studio

MotionCanvas is a 2D hand-drawn animation and flipbook studio built natively with **Kotlin** and **Jetpack Compose**.

## Features
- **Interactive Multi-Touch Canvas**: Draw with smooth bezier paths, eraser, fill, and stroke width control.
- **Canvas Undo / Redo**: Robust, frame-isolated undo and redo history for vector strokes and eraser actions with disabled state handling.
- **Onion Skinning**: Ghosting overlay of previous frames at 25% opacity for frame-by-frame guidance.
- **Filmstrip & Timeline Manager**: Add, reorder, duplicate, and navigate frames.
- **Custom FPS & Canvas Sizes**: Presets for YouTube (1080p, 720p), TikTok (9:16), Instagram (1:1), and FPS ranging from 6 to 60.
- **Layers**: Multi-layer compositing and visibility toggles.
- **Modern Material 3 Dark UI**: Styled in #0D0D0F with signature #FF3F91 Pink accents.

## Third-Party License & Attribution
The drawing undo/redo architecture in `com.smitnk.motioncanvas.drawing.DrawingHistory` is based on [SmartToolFactory/Compose-Drawing-App](https://github.com/SmartToolFactory/Compose-Drawing-App) under the MIT License:
```
MIT License
Copyright (c) 2022 SmartToolFactory

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
```

## How to Open in Android Studio
1. Unzip `MotionCanvas-Android-Studio.zip`.
2. Open Android Studio (Hedgehog, Iguana, Jellyfish, Koala or later recommended).
3. Select **File > Open** and choose the extracted `MotionCanvas` directory.
4. Let Gradle sync dependencies.
5. Click **Run > Run 'app'** on your connected Android device or emulator (Android 8.0+ / API 26+).

## Project Requirements
- Android Studio Koala / Ladybug or newer
- JDK 17
- Android SDK 34 (Compile & Target SDK)
- Minimum SDK: 26 (Android 8.0 Oreo)

## V6 repository feature integration

V6 adds functional vector **Arrow** and **Shape** tools inspired by the open-source Android Canvas project `sameerasw/Canvas` (MPL-2.0). Shape modes include rectangle, circle, triangle and line. The implementation is native to MotionCanvas's existing stroke/frame model rather than copying source files from that repository.