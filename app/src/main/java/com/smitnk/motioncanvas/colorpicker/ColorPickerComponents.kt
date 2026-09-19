package com.smitnk.motioncanvas.colorpicker

import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SweepGradient
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.smitnk.motioncanvas.ui.theme.*
import kotlin.math.*

/**
 * Color utilities for conversion between Compose Color, HSV, RGB, and HEX.
 */
object ColorUtils {
    fun colorToHsv(color: Color): FloatArray {
        val hsv = FloatArray(3)
        AndroidColor.RGBToHSV(
            (color.red * 255).roundToInt(),
            (color.green * 255).roundToInt(),
            (color.blue * 255).roundToInt(),
            hsv
        )
        return hsv
    }

    fun hsvToColor(hue: Float, saturation: Float, value: Float, alpha: Float = 1.0f): Color {
        val hsv = floatArrayOf(
            hue.coerceIn(0f, 360f),
            saturation.coerceIn(0f, 1f),
            value.coerceIn(0f, 1f)
        )
        val intColor = AndroidColor.HSVToColor((alpha.coerceIn(0f, 1f) * 255).roundToInt(), hsv)
        return Color(intColor)
    }

    fun colorToHex(color: Color, includeAlpha: Boolean = false): String {
        val r = (color.red * 255).roundToInt().coerceIn(0, 255)
        val g = (color.green * 255).roundToInt().coerceIn(0, 255)
        val b = (color.blue * 255).roundToInt().coerceIn(0, 255)
        val a = (color.alpha * 255).roundToInt().coerceIn(0, 255)
        return if (includeAlpha) {
            String.format("#%02X%02X%02X%02X", a, r, g, b)
        } else {
            String.format("#%02X%02X%02X", r, g, b)
        }
    }

    fun parseHex(hex: String, defaultColor: Color): Color {
        return try {
            val clean = hex.trim().removePrefix("#")
            when (clean.length) {
                6 -> {
                    val r = clean.substring(0, 2).toInt(16)
                    val g = clean.substring(2, 4).toInt(16)
                    val b = clean.substring(4, 6).toInt(16)
                    Color(r, g, b, 255)
                }
                8 -> {
                    val a = clean.substring(0, 2).toInt(16)
                    val r = clean.substring(2, 4).toInt(16)
                    val g = clean.substring(4, 6).toInt(16)
                    val b = clean.substring(6, 8).toInt(16)
                    Color(r, g, b, a)
                }
                3 -> {
                    val r = clean.substring(0, 1).repeat(2).toInt(16)
                    val g = clean.substring(1, 2).repeat(2).toInt(16)
                    val b = clean.substring(2, 3).repeat(2).toInt(16)
                    Color(r, g, b, 255)
                }
                else -> defaultColor
            }
        } catch (_: Exception) {
            defaultColor
        }
    }
}

/**
 * Standard preset palettes inspired by KvColorPicker-Android
 */
val DefaultPresetPalettes = listOf(
    // Row 1: Grayscale & Neutrals
    listOf(
        Color(0xFF000000), Color(0xFF262626), Color(0xFF525252), Color(0xFF737373),
        Color(0xFFA3A3A3), Color(0xFFD4D4D4), Color(0xFFF5F5F5), Color(0xFFFFFFFF)
    ),
    // Row 2: Primaries & Bold
    listOf(
        Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFF59E0B), Color(0xFF10B981),
        Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFF8B5CF6), Color(0xFFEC4899)
    ),
    // Row 3: Rich & Deep
    listOf(
        Color(0xFF991B1B), Color(0xFF9A3412), Color(0xFF92400E), Color(0xFF065F46),
        Color(0xFF155E75), Color(0xFF1E40AF), Color(0xFF5B21B6), Color(0xFF9D174D)
    ),
    // Row 4: Pastels & Skin Tones
    listOf(
        Color(0xFFFEE2E2), Color(0xFFFFEDD5), Color(0xFFFEF3C7), Color(0xFFD1FAE5),
        Color(0xFFCFFAFE), Color(0xFFDBEAFE), Color(0xFFEDE9FE), Color(0xFFFCE7F3)
    )
)

/**
 * Interactive HSV Color Wheel Composable.
 * Allows intuitive 2D touch selection of Hue and Saturation.
 */
@Composable
fun HsvColorWheel(
    hue: Float,
    saturation: Float,
    brightness: Float,
    onHueSaturationChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val sizePx = constraints.maxWidth.toFloat().coerceAtMost(constraints.maxHeight.toFloat())
        val radius = sizePx / 2f
        val center = Offset(radius, radius)

        // Calculate thumb offset from center
        val angleRad = Math.toRadians(hue.toDouble())
        val thumbDist = saturation * (radius - 16f)
        val thumbX = center.x + thumbDist * cos(angleRad).toFloat()
        val thumbY = center.y + thumbDist * sin(angleRad).toFloat()

        Canvas(
            modifier = Modifier
                .size(maxWidth)
                .aspectRatio(1f)
                .pointerInput(Unit) {
                    fun updateColor(touchOffset: Offset) {
                        val dx = touchOffset.x - center.x
                        val dy = touchOffset.y - center.y
                        val dist = sqrt(dx * dx + dy * dy).coerceAtMost(radius - 16f)
                        val sat = (dist / (radius - 16f)).coerceIn(0f, 1f)

                        var angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                        if (angleDeg < 0) angleDeg += 360f

                        onHueSaturationChange(angleDeg, sat)
                    }

                    detectTapGestures { offset -> updateColor(offset) }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val dx = change.position.x - center.x
                        val dy = change.position.y - center.y
                        val dist = sqrt(dx * dx + dy * dy).coerceAtMost(radius - 16f)
                        val sat = (dist / (radius - 16f)).coerceIn(0f, 1f)

                        var angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                        if (angleDeg < 0) angleDeg += 360f

                        onHueSaturationChange(angleDeg, sat)
                    }
                }
        ) {
            // Draw Hue Sweep Gradient
            val sweepColors = listOf(
                Color.Red, Color.Yellow, Color.Green, Color.Cyan,
                Color.Blue, Color.Magenta, Color.Red
            )
            drawCircle(
                brush = Brush.sweepGradient(sweepColors, center),
                radius = radius - 16f,
                center = center
            )

            // Draw Radial Saturation Gradient (Center White to Outer Transparent)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color.White.copy(alpha = 0f)),
                    center = center,
                    radius = radius - 16f
                ),
                radius = radius - 16f,
                center = center
            )

            // Value / Brightness Dimming Overlay
            if (brightness < 1.0f) {
                drawCircle(
                    color = Color.Black.copy(alpha = 1.0f - brightness),
                    radius = radius - 16f,
                    center = center
                )
            }

            // Outer ring border
            drawCircle(
                color = PanelBackground2,
                radius = radius - 16f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Current Selector Thumb
            drawCircle(
                color = Color.White,
                radius = 11.dp.toPx(),
                center = Offset(thumbX, thumbY)
            )
            drawCircle(
                color = Color.Black,
                radius = 9.dp.toPx(),
                center = Offset(thumbX, thumbY)
            )
            val currentSelectedColor = ColorUtils.hsvToColor(hue, saturation, brightness)
            drawCircle(
                color = currentSelectedColor,
                radius = 7.dp.toPx(),
                center = Offset(thumbX, thumbY)
            )
        }
    }
}

/**
 * Full Professional Color Selection Dialog integrating all KvColorPicker-Android features.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedColorPickerDialog(
    currentColor: Color,
    recentColors: List<Color>,
    customPalette: List<Color>,
    onColorChanged: (Color) -> Unit,
    onSaveToPalette: (Color) -> Unit,
    onRemoveFromPalette: (Color) -> Unit,
    onEyedropperClick: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    // Initial color values
    val initialHsv = remember(currentColor) { ColorUtils.colorToHsv(currentColor) }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var brightness by remember { mutableFloatStateOf(initialHsv[2]) }
    var alpha by remember { mutableFloatStateOf(currentColor.alpha) }

    // Active computed color
    val activeColor = remember(hue, saturation, brightness, alpha) {
        ColorUtils.hsvToColor(hue, saturation, brightness, alpha)
    }

    // HEX state
    var hexText by remember(activeColor) {
        mutableStateOf(ColorUtils.colorToHex(activeColor, includeAlpha = false))
    }

    // Active tab in picker: 0 = Wheel, 1 = RGB / HSL Sliders, 2 = Palettes & Custom
    var selectedTab by remember { mutableIntStateOf(0) }

    // Update color when user adjusts
    fun notifyColorChange(newColor: Color) {
        val hsv = ColorUtils.colorToHsv(newColor)
        hue = hsv[0]
        saturation = hsv[1]
        brightness = hsv[2]
        alpha = newColor.alpha
        onColorChanged(newColor)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(16.dp),
            color = PanelBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Header: Title, Eyedropper, Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = null,
                            tint = PinkAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Color Selection",
                            color = White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onEyedropperClick != null) {
                            IconButton(
                                onClick = {
                                    onDismiss()
                                    onEyedropperClick()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Colorize,
                                    contentDescription = "Sample Color from Canvas",
                                    tint = PinkAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Split Preview Bar (Original vs New) + Quick Hex Display + Save to Palette
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(PanelBackground2)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Original Swatch
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Current", color = TextSecondary, fontSize = 9.sp)
                            Box(
                                modifier = Modifier
                                    .size(width = 38.dp, height = 28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(currentColor)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                                    .clickable { notifyColorChange(currentColor) }
                            )
                        }

                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )

                        // New Selected Swatch
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("New", color = PinkAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .size(width = 46.dp, height = 28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(activeColor)
                                    .border(1.5.dp, White, RoundedCornerShape(6.dp))
                            )
                        }
                    }

                    // Hex code pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = hexText,
                            onValueChange = { input ->
                                hexText = input
                                val parsed = ColorUtils.parseHex(input, activeColor)
                                if (parsed != activeColor) {
                                    notifyColorChange(parsed)
                                }
                            },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = White,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .width(100.dp)
                                .height(46.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PinkAccent,
                                unfocusedBorderColor = BorderSubtle,
                                focusedContainerColor = AppBackground,
                                unfocusedContainerColor = AppBackground
                            )
                        )

                        IconButton(
                            onClick = { onSaveToPalette(activeColor) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PinkAccent.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                Icons.Default.BookmarkAdd,
                                contentDescription = "Save Custom Color",
                                tint = PinkAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Navigation Tabs: Wheel | Sliders | Palettes
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = PanelBackground2,
                    contentColor = PinkAccent,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Wheel", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("RGB / HSL", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Palettes", fontSize = 12.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Body content based on selected tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (selectedTab) {
                        0 -> {
                            // Wheel Mode
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                HsvColorWheel(
                                    hue = hue,
                                    saturation = saturation,
                                    brightness = brightness,
                                    onHueSaturationChange = { newHue, newSat ->
                                        hue = newHue
                                        saturation = newSat
                                        val updated = ColorUtils.hsvToColor(newHue, newSat, brightness, alpha)
                                        onColorChanged(updated)
                                    },
                                    modifier = Modifier
                                        .size(180.dp)
                                        .padding(8.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Brightness Slider
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Brightness / Value", color = TextSecondary, fontSize = 11.sp)
                                        Text("${(brightness * 100).roundToInt()}%", color = White, fontSize = 11.sp)
                                    }
                                    Slider(
                                        value = brightness,
                                        onValueChange = {
                                            brightness = it
                                            val updated = ColorUtils.hsvToColor(hue, saturation, it, alpha)
                                            onColorChanged(updated)
                                        },
                                        valueRange = 0f..1f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = White,
                                            activeTrackColor = PinkAccent,
                                            inactiveTrackColor = PanelBackground2
                                        )
                                    )
                                }

                                // Alpha / Opacity Slider
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Opacity / Alpha", color = TextSecondary, fontSize = 11.sp)
                                        Text("${(alpha * 100).roundToInt()}%", color = White, fontSize = 11.sp)
                                    }
                                    Slider(
                                        value = alpha,
                                        onValueChange = {
                                            alpha = it
                                            val updated = ColorUtils.hsvToColor(hue, saturation, brightness, it)
                                            onColorChanged(updated)
                                        },
                                        valueRange = 0f..1f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = White,
                                            activeTrackColor = PinkAccent,
                                            inactiveTrackColor = PanelBackground2
                                        )
                                    )
                                }
                            }
                        }
                        1 -> {
                            // RGB / HSL Granular Sliders
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("RGB Controls", color = White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                                val r = (activeColor.red * 255).roundToInt()
                                val g = (activeColor.green * 255).roundToInt()
                                val b = (activeColor.blue * 255).roundToInt()

                                ColorChannelSlider(
                                    label = "Red",
                                    value = r,
                                    max = 255,
                                    color = Color.Red,
                                    onValueChange = { newR ->
                                        val updated = Color(newR, g, b, (alpha * 255).roundToInt())
                                        notifyColorChange(updated)
                                    }
                                )

                                ColorChannelSlider(
                                    label = "Green",
                                    value = g,
                                    max = 255,
                                    color = Color.Green,
                                    onValueChange = { newG ->
                                        val updated = Color(r, newG, b, (alpha * 255).roundToInt())
                                        notifyColorChange(updated)
                                    }
                                )

                                ColorChannelSlider(
                                    label = "Blue",
                                    value = b,
                                    max = 255,
                                    color = Color.Blue,
                                    onValueChange = { newB ->
                                        val updated = Color(r, g, newB, (alpha * 255).roundToInt())
                                        notifyColorChange(updated)
                                    }
                                )

                                Divider(color = BorderSubtle, thickness = 1.dp)

                                Text("HSL Controls", color = White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                                ColorChannelSlider(
                                    label = "Hue",
                                    value = hue.roundToInt(),
                                    max = 360,
                                    color = PinkAccent,
                                    unit = "°",
                                    onValueChange = { newH ->
                                        hue = newH.toFloat()
                                        val updated = ColorUtils.hsvToColor(hue, saturation, brightness, alpha)
                                        onColorChanged(updated)
                                    }
                                )

                                ColorChannelSlider(
                                    label = "Saturation",
                                    value = (saturation * 100).roundToInt(),
                                    max = 100,
                                    color = PinkAccent,
                                    unit = "%",
                                    onValueChange = { newS ->
                                        saturation = newS / 100f
                                        val updated = ColorUtils.hsvToColor(hue, saturation, brightness, alpha)
                                        onColorChanged(updated)
                                    }
                                )

                                ColorChannelSlider(
                                    label = "Lightness / Value",
                                    value = (brightness * 100).roundToInt(),
                                    max = 100,
                                    color = PinkAccent,
                                    unit = "%",
                                    onValueChange = { newV ->
                                        brightness = newV / 100f
                                        val updated = ColorUtils.hsvToColor(hue, saturation, brightness, alpha)
                                        onColorChanged(updated)
                                    }
                                )
                            }
                        }
                        2 -> {
                            // Palettes, Recents, and Custom Saved Colors
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Recently Used Colors
                                if (recentColors.isNotEmpty()) {
                                    Column {
                                        Text(
                                            "Recently Used",
                                            color = White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            recentColors.forEach { c ->
                                                ColorSwatch(
                                                    color = c,
                                                    isSelected = c == activeColor,
                                                    onClick = { notifyColorChange(c) }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Saved Custom Palette
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Saved Custom Palette (${customPalette.size})",
                                            color = White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        TextButton(
                                            onClick = { onSaveToPalette(activeColor) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = PinkAccent)
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("Save Current", color = PinkAccent, fontSize = 11.sp)
                                        }
                                    }

                                    if (customPalette.isEmpty()) {
                                        Text(
                                            "No custom colors saved yet. Tap 'Save Current' or the bookmark icon above.",
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    } else {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)                                        ) {
                                            customPalette.forEach { c ->
                                                Box(contentAlignment = Alignment.TopEnd) {
                                                    ColorSwatch(
                                                        color = c,
                                                        isSelected = c == activeColor,
                                                        onClick = { notifyColorChange(c) }
                                                    )
                                                    // Quick delete badge on custom swatch
                                                    Box(
                                                        modifier = Modifier
                                                            .offset(x = 4.dp, y = (-4).dp)
                                                            .size(16.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFFEF4444))
                                                            .clickable { onRemoveFromPalette(c) },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Close,
                                                            contentDescription = "Remove",
                                                            tint = White,
                                                            modifier = Modifier.size(10.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Divider(color = BorderSubtle, thickness = 1.dp)

                                // Preset Palettes
                                Column {
                                    Text(
                                        "Preset Swatches",
                                        color = White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    DefaultPresetPalettes.forEach { paletteRow ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            paletteRow.forEach { c ->
                                                ColorSwatch(
                                                    color = c,
                                                    isSelected = c == activeColor,
                                                    onClick = { notifyColorChange(c) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action Buttons: Select & Apply
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PinkAccent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Apply & Use Color", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) PinkAccent else BorderSubtle,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = if (color.red * 0.299 + color.green * 0.587 + color.blue * 0.114 > 0.5) Color.Black else Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun ColorChannelSlider(
    label: String,
    value: Int,
    max: Int,
    color: Color,
    unit: String = "",
    onValueChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = TextSecondary, fontSize = 11.sp)
            Text("$value$unit", color = White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt().coerceIn(0, max)) },
            valueRange = 0f..max.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = PanelBackground2
            )
        )
    }
}