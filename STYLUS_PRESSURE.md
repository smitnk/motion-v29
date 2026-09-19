# Stylus Pressure Integration

MotionCanvas V3 records a pressure value on every DrawPoint.

- Stylus: reads PointerInputChange.pressure.
- Finger/mouse: uses 1.0 as neutral pressure.
- Rendering: average stroke pressure modulates the base brush width.
- Smoothing: the existing MIT-derived quadratic midpoint smoother remains active.
- Copies/transforms preserve pressure values.

This is intentionally dependency-free and uses Android/Compose pointer APIs directly. It leaves room for a future per-point variable-width raster/vector brush engine.