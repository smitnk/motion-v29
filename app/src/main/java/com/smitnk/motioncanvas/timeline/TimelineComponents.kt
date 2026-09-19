package com.smitnk.motioncanvas.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickableimport androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smitnk.motioncanvas.DrawPoint
import com.smitnk.motioncanvas.DrawStroke
import com.smitnk.motioncanvas.Frame
import com.smitnk.motioncanvas.Project
import com.smitnk.motioncanvas.ui.theme.*

/**
 * Deep copies a DrawStroke with fresh id and cloned point coordinates.
 */
fun DrawStroke.deepCopy(): DrawStroke = DrawStroke(
    id = java.util.UUID.randomUUID().toString(),
    points = points.map { DrawPoint(it.x, it.y, it.pressure) },
    color = color,
    strokeWidth = strokeWidth,
    alpha = alpha,
    isEraser = isEraser
)

/**
 * Deep copies an animation Frame, creating independent lists of strokes
 * so future drawing actions on the copy do not modify the original.
 */
fun Frame.deepCopy(): Frame = Frame(
    id = java.util.UUID.randomUUID().toString(),
    strokes = strokes.map { it.deepCopy() }.toMutableList(),
    redoStrokes = redoStrokes.map { it.deepCopy() }.toMutableList(),
    fills = fills.toMutableList(),
    durationFrames = durationFrames,
    isKeyframe = isKeyframe,
    tag = tag,
    tagColor = tagColor
)

/**
 * High-fidelity thumbnail canvas rendering all vector strokes for a given frame.
 */
@Composable
fun FrameThumbnail(
    frame: Frame,
    canvasW: Int = 1280,
    canvasH: Int = 720,
    backgroundColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (frame.strokes.isEmpty()) {
                return@Canvas
            }

            val scaleX = size.width / canvasW.coerceAtLeast(1)
            val scaleY = size.height / canvasH.coerceAtLeast(1)
            val renderScale = minOf(scaleX, scaleY)

            scale(scale = renderScale, pivot = Offset.Zero) {
                frame.strokes.forEach { stroke ->
                    if (stroke.points.size > 1) {
                        val path = Path().apply {
                            moveTo(stroke.points[0].x, stroke.points[0].y)
                            for (i in 1 until stroke.points.size) {
                                lineTo(stroke.points[i].x, stroke.points[i].y)
                            }
                        }
                        val strokeColor = if (stroke.isEraser) backgroundColor else stroke.color
                        drawPath(
                            path = path,
                            color = strokeColor.copy(alpha = stroke.alpha),
                            style = Stroke(
                                width = (stroke.strokeWidth * 1.2f).coerceAtLeast(2f),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Complete Horizontal Animation Timeline and Filmstrip dock for MotionCanvas.
 * Optimized for mobile phone screens with expandable/collapsible view.
 */
@Composable
fun AnimationTimelineDock(
    project: Project,
    currentFrameIndex: Int,
    isPlaying: Boolean,
    isLooping: Boolean,
    fps: Int,
    copiedFrame: Frame?,
    onFrameIndexChange: (Int) -> Unit,
    onPlayToggle: () -> Unit,
    onLoopToggle: () -> Unit,
    onFpsChange: (Int) -> Unit,
    onAddBlankFrame: () -> Unit,
    onInsertBefore: () -> Unit,
    onInsertAfter: () -> Unit,
    onDuplicateFrame: () -> Unit,
    onCopyFrame: () -> Unit,
    onPasteFrame: () -> Unit,
    onDeleteFrame: () -> Unit,
    onMoveFrame: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    var showFpsDialog by remember { mutableStateOf(false) }
    var showActionMenu by remember { mutableStateOf(false) }

    Surface(
        color = PanelBackground,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // Header Row: Frame Count, Scrubber, Play/Pause, Loop, FPS, Expand/Collapse
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play / Pause + Frame index badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPlayToggle,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = PinkAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Loop toggle
                    IconButton(
                        onClick = onLoopToggle,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Repeat,
                            contentDescription = "Loop playback",
                            tint = if (isLooping) PinkAccent else TextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Frame Indicator & Timestamp
                    val currentSec = if (fps > 0) String.format("%.2f", currentFrameIndex.toFloat() / fps) else "0.00"
                    val totalSec = if (fps > 0) String.format("%.2f", project.frames.size.toFloat() / fps) else "0.00"
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${currentFrameIndex + 1}",
                                color = PinkAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = " / ${project.frames.size}",
                                color = White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = "${currentSec}s / ${totalSec}s",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                // Center Scrubber Slider (Scrubs in real-time)
                if (project.frames.size > 1) {
                    Slider(
                        value = currentFrameIndex.toFloat(),
                        onValueChange = { newIdx ->
                            onFrameIndexChange(newIdx.toInt().coerceIn(0, project.frames.size - 1))
                        },
                        valueRange = 0f..(project.frames.size - 1).toFloat(),
                        steps = (project.frames.size - 2).coerceAtLeast(0),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                            .height(24.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = PinkAccent,
                            activeTrackColor = PinkAccent,
                            inactiveTrackColor = PanelBackground2
                        )
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Right controls: FPS button, Actions dropdown, and Collapse/Expand
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // FPS Badge Button
                    TextButton(
                        onClick = { showFpsDialog = true },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = "$fps FPS",
                            color = White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Quick Frame Actions Menu Button
                    IconButton(
                        onClick = { showActionMenu = !showActionMenu },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Frame Actions",
                            tint = White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Expand / Collapse Filmstrip
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = if (isExpanded) "Collapse Timeline" else "Expand Timeline",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Expandable Filmstrip and Editing Toolbar
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(4.dp))

                    // Secondary Action Rail: Quick Buttons (Add, Insert, Duplicate, Copy, Paste, Delete, Move)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Add Blank Frame
                        FilledTonalButton(
                            onClick = onAddBlankFrame,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = PinkAccent.copy(alpha = 0.2f),
                                contentColor = PinkAccent
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("+ Blank", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Insert Before
                        OutlinedButton(
                            onClick = onInsertBefore,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PanelBackground2)
                        ) {
                            Text("Insert Before", fontSize = 11.sp, color = White)
                        }

                        // Insert After
                        OutlinedButton(
                            onClick = onInsertAfter,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PanelBackground2)
                        ) {
                            Text("Insert After", fontSize = 11.sp, color = White)
                        }

                        // Duplicate
                        OutlinedButton(
                            onClick = onDuplicateFrame,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PanelBackground2)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp), tint = White)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Duplicate", fontSize = 11.sp, color = White)
                        }

                        // Copy Frame
                        OutlinedButton(
                            onClick = onCopyFrame,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PanelBackground2)
                        ) {
                            Text("Copy", fontSize = 11.sp, color = White)
                        }

                        // Paste Frame
                        OutlinedButton(
                            onClick = onPasteFrame,
                            enabled = copiedFrame != null,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (copiedFrame != null) PinkAccent.copy(alpha = 0.5f) else PanelBackground2
                            )
                        ) {
                            Text("Paste", fontSize = 11.sp, color = if (copiedFrame != null) PinkAccent else TextSecondary)
                        }

                        // Move Left
                        IconButton(
                            onClick = {
                                if (currentFrameIndex > 0) {
                                    onMoveFrame(currentFrameIndex, currentFrameIndex - 1)
                                }
                            },
                            enabled = currentFrameIndex > 0,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Move Frame Left",
                                tint = if (currentFrameIndex > 0) White else TextSecondary.copy(alpha = 0.3f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Move Right
                        IconButton(
                            onClick = {
                                if (currentFrameIndex < project.frames.size - 1) {
                                    onMoveFrame(currentFrameIndex, currentFrameIndex + 1)
                                }
                            },
                            enabled = currentFrameIndex < project.frames.size - 1,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = "Move Frame Right",
                                tint = if (currentFrameIndex < project.frames.size - 1) White else TextSecondary.copy(alpha = 0.3f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Delete
                        IconButton(
                            onClick = onDeleteFrame,
                            enabled = project.frames.size > 1,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Frame",
                                tint = if (project.frames.size > 1) Color(0xFFEF4444) else TextSecondary.copy(alpha = 0.3f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Horizontal Filmstrip of Frames with Real Thumbnails
                    val scrollState = rememberScrollState()
                    LaunchedEffect(currentFrameIndex) {
                        // Keep current frame in view
                        val approxItemWidthPx = 220
                        val targetScroll = (currentFrameIndex * approxItemWidthPx - 100).coerceAtLeast(0)
                        scrollState.animateScrollTo(targetScroll)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        project.frames.forEachIndexed { idx, frame ->
                            val isSelected = idx == currentFrameIndex
                            var dragOffsetX by remember { mutableFloatStateOf(0f) }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(68.dp)
                                    .pointerInput(idx, project.frames.size) {
                                        detectDragGestures(
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffsetX += dragAmount.x
                                                if (dragOffsetX > 50f && idx < project.frames.size - 1) {
                                                    onMoveFrame(idx, idx + 1)
                                                    dragOffsetX = 0f
                                                } else if (dragOffsetX < -50f && idx > 0) {
                                                    onMoveFrame(idx, idx - 1)
                                                    dragOffsetX = 0f
                                                }
                                            },
                                            onDragEnd = { dragOffsetX = 0f },
                                            onDragCancel = { dragOffsetX = 0f }
                                        )
                                    }
                            ) {
                                // Frame Thumbnail Card
                                Box(
                                    modifier = Modifier
                                        .size(width = 68.dp, height = 44.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.White)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) PinkAccent else Color(0xFF3F3F46),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable { onFrameIndexChange(idx) }
                                ) {
                                    FrameThumbnail(
                                        frame = frame,
                                        canvasW = project.canvasW,
                                        canvasH = project.canvasH,
                                        backgroundColor = project.backgroundColor,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // Active Indicator Dot
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(3.dp)
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(PinkAccent)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(3.dp))

                                // Frame Number Label
                                Text(
                                    text = "${idx + 1}",
                                    color = if (isSelected) PinkAccent else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Append Frame '+' Button at the end of the filmstrip
                        Box(
                            modifier = Modifier
                                .size(width = 44.dp, height = 44.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(PanelBackground2)
                                .border(1.dp, Color(0xFF3F3F46), RoundedCornerShape(6.dp))
                                .clickable { onAddBlankFrame() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add Frame at End",
                                tint = PinkAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // FPS Quick Selector Dialog
    if (showFpsDialog) {
        AlertDialog(
            onDismissRequest = { showFpsDialog = false },
            containerColor = PanelBackground,
            title = {
                Text("Playback Speed (FPS)", color = White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Set frames per second for smooth flipbook preview:",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )

                    // Current FPS slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Current FPS:", color = White, fontSize = 14.sp)
                        Text("$fps FPS", color = PinkAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Slider(
                        value = fps.toFloat(),
                        onValueChange = { onFpsChange(it.toInt().coerceIn(1, 60)) },
                        valueRange = 1f..30f,
                        colors = SliderDefaults.colors(thumbColor = PinkAccent, activeTrackColor = PinkAccent)
                    )

                    // Common Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(6, 8, 12, 15, 24).forEach { preset ->
                            FilledTonalButton(
                                onClick = {
                                    onFpsChange(preset)
                                    showFpsDialog = false
                                },
                                modifier = Modifier.weight(1f).height(34.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (fps == preset) PinkAccent else PanelBackground2,
                                    contentColor = White
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("$preset", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showFpsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PinkAccent)
                ) {
                    Text("Done", color = White)
                }
            }
        )
    }
}