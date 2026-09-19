# Pro features added to this ZIP

## 1. Layer clipping masks + blend modes
- Layers now have `clipToBelow` and `blendMode` state.
- Blend modes: Normal, Multiply, Screen, Overlay, Add.
- Clipping uses the alpha coverage of the layer immediately below.
- Rendering is composited through Android Canvas/PorterDuff.
- Layer controls are available in the Layers screen.

## 2. Textured brush
- New Textured Brush toggle in the editor.
- Adjustable grain amount.
- Stroke data records whether a stroke used the textured brush.
- Texture is rendered into the layer compositor.

## Verification limitation
The included ZIP cannot be locally compiled in this environment because the source project is missing `gradle/wrapper/gradle-wrapper.jar`. The GitHub Actions build should be used for compile verification.