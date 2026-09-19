package com.smitnk.motioncanvas

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smitnk.motioncanvas.ui.theme.*
import com.smitnk.motioncanvas.brush.CustomBrushPreset
import com.smitnk.motioncanvas.brush.AdvancedBrushEngine
import com.smitnk.motioncanvas.brush.BrushPresetStore
import com.smitnk.motioncanvas.drawing.FrameDrawingHistory
import com.smitnk.motioncanvas.drawing.ZoomPanState
import com.smitnk.motioncanvas.drawing.rememberZoomPanState
import com.smitnk.motioncanvas.drawing.detectZoomPanOrDraw
import com.smitnk.motioncanvas.drawing.OnionSkinState
import com.smitnk.motioncanvas.drawing.OpenSourceStrokeSmoother
import com.smitnk.motioncanvas.drawing.OpenSourceDrawingEngine
import com.smitnk.motioncanvas.drawing.rememberOnionSkinState
import com.smitnk.motioncanvas.timeline.*
import com.smitnk.motioncanvas.colorpicker.*
import com.smitnk.motioncanvas.selection.*
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import com.smitnk.motioncanvas.fill.FloodFillEngine
import com.smitnk.motioncanvas.selection.LassoSelection
import com.smitnk.motioncanvas.reference.ReferenceTransform
import com.smitnk.motioncanvas.audio.AudioRecorderController
import com.smitnk.motioncanvas.audio.AudioPreviewController
import com.smitnk.motioncanvas.project.ProjectRepository
import com.smitnk.motioncanvas.animation.MotionTrailEngine
import com.smitnk.motioncanvas.export.SpriteSheetExporter
import com.smitnk.motioncanvas.audio.AudioClip
import com.smitnk.motioncanvas.audio.AudioWaveformAnalyzer
import com.smitnk.motioncanvas.audio.AudioTimelineEngine
import com.smitnk.motioncanvas.audio.AudioTrack
import com.smitnk.motioncanvas.audio.AudioTrackBridge
import com.smitnk.motioncanvas.audio.SharedAudioVideoClock
import com.smitnk.motioncanvas.video.RotoscopeVideoImporter
import com.smitnk.motioncanvas.video.RotoscopeSequenceImporter
import com.smitnk.motioncanvas.video.Mp4VideoExporter
import com.smitnk.motioncanvas.video.AudioMuxedMp4Exporter
import com.smitnk.motioncanvas.video.Media3MultiTrackExporter
import com.smitnk.motioncanvas.video.ProjectBitmapRenderer
import com.smitnk.motioncanvas.effects.ChromaKeyEngine
import com.smitnk.motioncanvas.drawing.SmudgeEngine
import com.smitnk.motioncanvas.animation.BatchFrameOperations

enum class ScreenType {
    HOME, CREATE, SIZE, FPS, EDITOR, SETTINGS, MORE, TIMELINE, LAYERS
}

enum class ToolType {
    Brush, Eraser, Select, Pan, Lasso, Fill, Text, Eyedropper, Arrow, Shape, More
}

data class WorkspaceVisibility(
    val leftToolbar: Boolean = true,
    val topBar: Boolean = true,
    val timeline: Boolean = true,
    val referenceWidget: Boolean = true,
    val frameToolsWidget: Boolean = true,
    val audioWidget: Boolean = true,
    val advancedWidget: Boolean = true,
    val proToolsWidget: Boolean = true,
    val brushPresetsWidget: Boolean = true,
    val colorWidget: Boolean = true
)

data class AdvancedWorkspaceState(
    val perspectiveEnabled: Boolean = false,
    val rulerEnabled: Boolean = false,
    val bezierEnabled: Boolean = false,
    val motionGuideEnabled: Boolean = false,
    val cameraEnabled: Boolean = false,
    val ikEnabled: Boolean = false,
    val smudgeEnabled: Boolean = false,
    val liquifyEnabled: Boolean = false,
    val particlesEnabled: Boolean = false,
    val magicWandEnabled: Boolean = false,
    val autosaveEnabled: Boolean = true,
    val brushDynamicsEnabled: Boolean = true,
    val multiFrameTransformEnabled: Boolean = true
)

enum class ShapeType { RECTANGLE, CIRCLE, TRIANGLE, LINE }

data class DrawPoint(val x: Float, val y: Float, val pressure: Float = 1.0f)

data class DrawStroke(
    val id: String = java.util.UUID.randomUUID().toString(),
    val points: List<DrawPoint>,
    val color: Color,
    val strokeWidth: Float,
    val alpha: Float = 1.0f,
    val isEraser: Boolean = false,
    val layerIndex: Int = 0,
    val textured: Boolean = false
)

data class CanvasText(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val x: Float,
    val y: Float,
    val size: Float = 40f,
    val color: Color = Color.Black
)

data class FillMark(
    val x: Int, val y: Int, val color: Color, val tolerance: Int = 12
)

data class Frame(
    val id: String = java.util.UUID.randomUUID().toString(),
    val strokes: MutableList<DrawStroke> = mutableListOf(),
    val texts: MutableList<CanvasText> = mutableListOf(),
    val redoStrokes: MutableList<DrawStroke> = mutableListOf(),
    val fills: MutableList<FillMark> = mutableListOf(),
    var durationFrames: Int = 1,
    var isKeyframe: Boolean = false,
    var tag: String = "",
    var tagColor: Color = Color.Transparent
)

data class Layer(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "Layer 1",
    val visible: Boolean = true,
    val opacity: Float = 1.0f,
    val clipToBelow: Boolean = false,
    val blendMode: String = "NORMAL"
)

data class Project(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    var fps: Int = 12,
    val canvasW: Int = 1280,
    val canvasH: Int = 720,
    val frames: MutableList<Frame> = mutableListOf(Frame()),
    val layers: MutableList<Layer> = mutableListOf(Layer()),
    val backgroundColor: Color = Color.White,
    var audioPath: String? = null,
    val audioClips: MutableList<AudioClip> = mutableListOf(),
    val audioTracks: MutableList<com.smitnk.motioncanvas.audio.AudioTrack> = mutableListOf()
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
        var advancedFeatureState by remember { mutableStateOf(AdvancedFeatureState()) }
            MotionCanvasTheme {
                MotionCanvasApp()
            }
        }
    }
}

private fun textureAlpha(x: Int, y: Int, amount: Float): Int {
    val n = ((x * 73856093) xor (y * 19349663)) and 255
    return (n * amount.coerceIn(0f, 1f) * 0.55f).toInt().coerceIn(0, 140)
}

private fun blendMode(mode: String): PorterDuff.Mode? = when (mode) {
    "MULTIPLY" -> PorterDuff.Mode.MULTIPLY
    "SCREEN" -> PorterDuff.Mode.SCREEN
    "OVERLAY" -> PorterDuff.Mode.OVERLAY
    "ADD" -> PorterDuff.Mode.ADD
    else -> null
}

private fun renderLayerComposite(project: Project, frame: Frame, textureAmount: Float): Bitmap {
    val out = Bitmap.createBitmap(project.canvasW, project.canvasH, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(out)
    canvas.drawColor(project.backgroundColor.toArgb())
    var previousLayerBitmap: Bitmap? = null
    project.layers.forEachIndexed { li, layer ->
        if (!layer.visible) return@forEachIndexed
        val layerBmp = Bitmap.createBitmap(project.canvasW, project.canvasH, Bitmap.Config.ARGB_8888)
        val lc = AndroidCanvas(layerBmp)
        frame.strokes.filter { it.layerIndex == li }.forEach { s ->
            if (s.points.size < 2) return@forEach
            val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG or AndroidPaint.DITHER_FLAG)
            paint.color = if (s.isEraser) project.backgroundColor.toArgb() else s.color.toArgb()
            paint.alpha = (s.alpha * 255f).toInt().coerceIn(0,255)
            paint.style = AndroidPaint.Style.STROKE
            paint.strokeWidth = s.strokeWidth.coerceAtLeast(1f)
            paint.strokeCap = AndroidPaint.Cap.ROUND
            paint.strokeJoin = AndroidPaint.Join.ROUND
            val path = android.graphics.Path()
            path.moveTo(s.points.first().x, s.points.first().y)
            s.points.drop(1).forEach { path.lineTo(it.x, it.y) }
            lc.drawPath(path, paint)
            if (s.textured) {
                AdvancedBrushEngine.draw(
                    canvas = lc,
                    points = s.points.map { Offset(it.x, it.y) },
                    pressures = s.points.map { it.pressure },
                    color = s.color.toArgb(),
                    width = s.strokeWidth,
                    opacity = s.alpha,
                    spacing = 0.18f,
                    scatter = textureAmount
                )
            }
        }
        if (layer.clipToBelow && previousLayerBitmap != null) {
            val maskPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG)
            maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            AndroidCanvas(layerBmp).drawBitmap(previousLayerBitmap!!, 0f, 0f, maskPaint)
            maskPaint.xfermode = null
        }
        val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG)
        paint.alpha = (layer.opacity * 255f).toInt().coerceIn(0,255)
        blendMode(layer.blendMode)?.let { paint.xfermode = PorterDuffXfermode(it) }
        canvas.drawBitmap(layerBmp, 0f, 0f, paint)
        paint.xfermode = null
        previousLayerBitmap?.recycle()
        previousLayerBitmap = layerBmp.copy(Bitmap.Config.ARGB_8888, true)
        layerBmp.recycle()
    }
    previousLayerBitmap?.recycle()
    return out
}

@Composable
fun MotionCanvasApp() {
    var screen by remember { mutableStateOf(ScreenType.HOME) }
    var projects by remember {
        mutableStateOf(
            listOf(
                Project(
                    name = "My Animation",
                    fps = 12,
                    frames = mutableListOf(
                        Frame(
                            strokes = mutableListOf(
                                DrawStroke(
                                    points = listOf(DrawPoint(100f, 150f), DrawPoint(150f, 100f), DrawPoint(200f, 150f)),
                                    color = Color.Black,
                                    strokeWidth = 10f
                                )
                            )
                        ),
                        Frame(
                            strokes = mutableListOf(
                                DrawStroke(
                                    points = listOf(DrawPoint(120f, 160f), DrawPoint(170f, 110f), DrawPoint(220f, 160f)),
                                    color = Color.Black,
                                    strokeWidth = 10f
                                )
                            )
                        )
                    )
                ),
                Project(
                    name = "Walk Cycle",
                    fps = 12,
                    frames = mutableListOf(Frame(), Frame(), Frame())
                )
            )
        )
    }

    var activeProject by remember { mutableStateOf<Project?>(null) }
    var workspaceVisibility by remember { mutableStateOf(WorkspaceVisibility()) }
    var advancedWorkspaceState by remember { mutableStateOf(AdvancedWorkspaceState()) }

    var currentFrameIndex by remember { mutableIntStateOf(0) }
    var selectedTool by remember { mutableStateOf(ToolType.Brush) }
    var brushColor by remember { mutableStateOf(Color.Black) }
    var brushSize by remember { mutableFloatStateOf(8f) }
    var texturedBrush by remember { mutableStateOf(false) }
    var textureAmount by remember { mutableFloatStateOf(0.55f) }
    var selectedLayerIndex by remember { mutableIntStateOf(0) }
    var recentColors by remember {
        mutableStateOf(listOf(
            Color(0xFF000000),
            Color(0xFFFF3F91),
            Color(0xFF3B82F6),
            Color(0xFF10B981),
            Color(0xFFF59E0B),
            Color(0xFFEF4444),
            Color(0xFF8B5CF6),
            Color(0xFFFFFFFF)
        ))
    }
    var customPalette by remember {
        mutableStateOf(listOf(
            Color(0xFFFF3F91),
            Color(0xFF6366F1),
            Color(0xFF14B8A6),
            Color(0xFFF97316)
        ))
    }

    val onBrushColorChange: (Color) -> Unit = { newColor ->
        brushColor = newColor
        if (!recentColors.contains(newColor)) {
            recentColors = (listOf(newColor) + recentColors).take(16)
        }
    }

    val onSaveToPalette: (Color) -> Unit = { colorToSave ->
        if (!customPalette.contains(colorToSave)) {
            customPalette = customPalette + colorToSave
        }
    }

    val onRemoveFromPalette: (Color) -> Unit = { colorToRemove ->
        customPalette = customPalette.filter { it != colorToRemove }
    }

    val onionSkinState = rememberOnionSkinState()
    var showGrid by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var isLooping by remember { mutableStateOf(true) }
    var timelineLoopMode by remember { mutableStateOf(com.smitnk.motioncanvas.animation.TimelineLoopMode.LOOP) }
    var copiedFrame by remember { mutableStateOf<Frame?>(null) }
    val brushPresetStore = remember { BrushPresetStore() }
    val context = LocalContext.current
    val sharedAudioClock = remember { SharedAudioVideoClock(context) }
    DisposableEffect(Unit) { onDispose { sharedAudioClock.release() } }
    val autosaveStore = remember { com.smitnk.motioncanvas.project.AutosaveStore(context) }

    // Lightweight crash-safe autosave. The repository encoder is shared with manual project saves.
    LaunchedEffect(activeProject) {
        while (activeProject != null) {
            delay(3000)
            activeProject?.let { autosaveStore.save(it.id, ProjectRepository.encode(it).toString()) }
        }
    }

    // Shared audio/video clock: audio is prepared from the same frame timeline used by playback.
    LaunchedEffect(activeProject) {
        activeProject?.let { project ->
            AudioTrackBridge.ensureTracks(project.audioTracks, project.audioClips)
            sharedAudioClock.prepare(project.audioTracks, project.fps)
            sharedAudioClock.bindClips(project.audioTracks)
        }
    }

    LaunchedEffect(isPlaying, activeProject, activeProject?.fps, timelineLoopMode) {
        val project = activeProject ?: return@LaunchedEffect
        if (!isPlaying || project.frames.isEmpty()) return@LaunchedEffect
        sharedAudioClock.prepare(project.audioTracks, project.fps)
        sharedAudioClock.bindClips(project.audioTracks)
        sharedAudioClock.startAtFrame(currentFrameIndex)
        val startNanos = System.nanoTime()
        val baseFrame = currentFrameIndex
        while (isPlaying) {
            val elapsedFrames = (((System.nanoTime() - startNanos) / 1_000_000_000.0) * project.fps).toInt()
            val total = project.frames.size
            var target = baseFrame + elapsedFrames
            when (timelineLoopMode) {
                com.smitnk.motioncanvas.animation.TimelineLoopMode.LOOP -> target %= total
                com.smitnk.motioncanvas.animation.TimelineLoopMode.ONCE -> if (target >= total) { isPlaying = false; break }
                com.smitnk.motioncanvas.animation.TimelineLoopMode.PING_PONG -> {
                    val period = (total - 1).coerceAtLeast(1) * 2
                    val x = target % period
                    target = if (x < total) x else period - x
                }
            }
            currentFrameIndex = target.coerceIn(0, total - 1)
            sharedAudioClock.seekToFrame(currentFrameIndex)
            delay(16)
        }
        sharedAudioClock.pause()
    }

    // Frame timeline operations
    val onAddBlankFrame: () -> Unit = {
        activeProject?.let { proj ->
            proj.frames.add(Frame())
            currentFrameIndex = proj.frames.size - 1
        }
    }

    val onInsertBefore: () -> Unit = {
        activeProject?.let { proj ->
            proj.frames.add(currentFrameIndex, Frame())
        }
    }

    val onInsertAfter: () -> Unit = {
        activeProject?.let { proj ->
            proj.frames.add(currentFrameIndex + 1, Frame())
            currentFrameIndex += 1
        }
    }

    val onDuplicateFrame: () -> Unit = {
        activeProject?.let { proj ->
            if (proj.frames.isNotEmpty()) {
                val clone = proj.frames[currentFrameIndex].deepCopy()
                proj.frames.add(currentFrameIndex + 1, clone)
                currentFrameIndex += 1
            }
        }
    }

    val onCopyFrame: () -> Unit = {
        activeProject?.let { proj ->
            if (proj.frames.isNotEmpty()) {
                copiedFrame = proj.frames[currentFrameIndex].deepCopy()
            }
        }
    }

    val onPasteFrame: () -> Unit = {
        activeProject?.let { proj ->
            copiedFrame?.let { clip ->
                val pasted = clip.deepCopy()
                proj.frames.add(currentFrameIndex + 1, pasted)
                currentFrameIndex += 1
            }
        }
    }

    val onDeleteFrame: () -> Unit = {
        activeProject?.let { proj ->
            if (proj.frames.size > 1) {
                proj.frames.removeAt(currentFrameIndex)
                currentFrameIndex = currentFrameIndex.coerceAtMost(proj.frames.size - 1)
            }
        }
    }

    val onMoveFrame: (Int, Int) -> Unit = { fromIndex, toIndex ->
        activeProject?.let { proj ->
            if (fromIndex in proj.frames.indices && toIndex in proj.frames.indices && fromIndex != toIndex) {
                val item = proj.frames.removeAt(fromIndex)
                proj.frames.add(toIndex, item)
                currentFrameIndex = toIndex
            }
        }
    }

    val onFpsChange: (Int) -> Unit = { newFps ->
        activeProject?.let { proj ->
            proj.fps = newFps
            activeProject = proj.copy(fps = newFps)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        when (screen) {
            ScreenType.HOME -> {
                HomeScreen(
                    projects = projects,
                    onOpenProject = { proj ->
                        activeProject = proj
                        currentFrameIndex = 0
                        screen = ScreenType.EDITOR
                    },
                    onCreateNew = {
                        screen = ScreenType.CREATE
                    }
                )
            }
            ScreenType.CREATE -> {
                CreateProjectScreen(
                    onBack = { screen = ScreenType.HOME },
                    onCreate = { name, fps, w, h, bg ->
                        val newProj = Project(
                            name = name.ifEmpty { "Untitled" },
                            fps = fps,
                            canvasW = w,
                            canvasH = h,
                            backgroundColor = bg
                        )
                        projects = projects + newProj
                        activeProject = newProj
                        currentFrameIndex = 0
                        screen = ScreenType.EDITOR
                    },
                    onSelectSize = { screen = ScreenType.SIZE },
                    onSelectFps = { screen = ScreenType.FPS }
                )
            }
            ScreenType.SIZE -> {
                CanvasSizeScreen(
                    onBack = { screen = ScreenType.CREATE },
                    onSelected = { _, _ -> screen = ScreenType.CREATE }
                )
            }
            ScreenType.FPS -> {
                FpsScreen(
                    onBack = { screen = ScreenType.CREATE },
                    onSelected = { screen = ScreenType.CREATE }
                )
            }
            ScreenType.EDITOR -> {
                activeProject?.let { proj ->
                    EditorScreen(
                        project = proj,
                        frameIndex = currentFrameIndex,
                        onFrameIndexChange = { currentFrameIndex = it },
                        tool = selectedTool,
                        onToolChange = { selectedTool = it },
                        color = brushColor,
                        onColorChange = onBrushColorChange,
                        recentColors = recentColors,
                        customPalette = customPalette,
                        onSaveToPalette = onSaveToPalette,
                        onRemoveFromPalette = onRemoveFromPalette,
                        size = brushSize,
                        onSizeChange = { brushSize = it },
                        texturedBrush = texturedBrush,
                        onTexturedBrushChange = { texturedBrush = it },
                        textureAmount = textureAmount,
                        onTextureAmountChange = { textureAmount = it },
                        onionSkinState = onionSkinState,
                        grid = showGrid,
                        isPlaying = isPlaying,
                        onPlayToggle = { isPlaying = !isPlaying },
                        isLooping = isLooping,
                        onLoopToggle = { isLooping = !isLooping },
                        fps = proj.fps,
                        onFpsChange = onFpsChange,
                        copiedFrame = copiedFrame,
                        onAddBlankFrame = onAddBlankFrame,
                        onInsertBefore = onInsertBefore,
                        onInsertAfter = onInsertAfter,
                        onDuplicateFrame = onDuplicateFrame,
                        onCopyFrame = onCopyFrame,
                        onPasteFrame = onPasteFrame,
                        onDeleteFrame = onDeleteFrame,
                        onMoveFrame = onMoveFrame,
                        onBack = {
                            isPlaying = false
                            screen = ScreenType.HOME
                        },
                        onOpenSettings = { screen = ScreenType.SETTINGS },
                        onOpenTimeline = { screen = ScreenType.TIMELINE },
                        onOpenLayers = { screen = ScreenType.LAYERS },
                        brushPresetStore = brushPresetStore,
                        workspaceVisibility = workspaceVisibility,
                        advancedWorkspaceState = advancedWorkspaceState,
                        onAdvancedWorkspaceStateChange = { advancedWorkspaceState = it },
                        onOpenMore = { screen = ScreenType.MORE }
                    )
                }
            }
            ScreenType.MORE -> {
                MoreToolsScreen(
                    visibility = workspaceVisibility,
                    onVisibilityChange = { workspaceVisibility = it },
                    advancedState = advancedWorkspaceState,
                    onAdvancedStateChange = { advancedWorkspaceState = it },
                    onBack = { screen = ScreenType.EDITOR }
                )
            }
            ScreenType.SETTINGS -> {
                SettingsScreen(
                    onionSkinState = onionSkinState,
                    grid = showGrid,
                    onToggleGrid = { showGrid = !showGrid },
                    onBack = { screen = ScreenType.EDITOR }
                )
            }
            ScreenType.TIMELINE -> {
                activeProject?.let { proj ->
                    AudioTrackBridge.ensureTracks(activeProject!!.audioTracks, activeProject!!.audioClips)
                    TimelineScreen(
                        project = proj,
                        currentIndex = currentFrameIndex,
                        isPlaying = isPlaying,
                        isLooping = isLooping,
                        fps = proj.fps,
                        copiedFrame = copiedFrame,
                        onSelectFrame = { currentFrameIndex = it },
                        onPlayToggle = { isPlaying = !isPlaying },
                        onLoopToggle = { isLooping = !isLooping },
                        onFpsChange = onFpsChange,
                        onAddBlankFrame = onAddBlankFrame,
                        onInsertBefore = onInsertBefore,
                        onInsertAfter = onInsertAfter,
                        onDuplicateFrame = onDuplicateFrame,
                        onCopyFrame = onCopyFrame,
                        onPasteFrame = onPasteFrame,
                        onDeleteFrame = onDeleteFrame,
                        onMoveFrame = onMoveFrame,
                        onBack = { screen = ScreenType.EDITOR }
                    )
                }
            }
            ScreenType.LAYERS -> {
                activeProject?.let { proj ->
                    LayersScreen(
                        layers = proj.layers,
                        selectedLayer = selectedLayerIndex,
                        onSelectLayer = { selectedLayerIndex = it },
                        onAddLayer = {
                            proj.layers.add(Layer(name = "Layer ${proj.layers.size + 1}"))
                            selectedLayerIndex = proj.layers.lastIndex
                        },
                        onBack = { screen = ScreenType.EDITOR }
                    )
                }
            }
            else -> {
                screen = ScreenType.HOME
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Screens
// -------------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    projects: List<Project>,
    onOpenProject: (Project) -> Unit,
    onCreateNew: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = { Text("MotionCanvas", color = White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = White)
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateNew,
                containerColor = PinkAccent,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Project", tint = White)
            }
        },
        bottomBar = {
            NavigationBar(containerColor = PanelBackground) {
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PinkAccent,
                        selectedTextColor = PinkAccent,
                        indicatorColor = PanelBackground2
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {},
                    icon = { Icon(Icons.Default.Explore, contentDescription = null) },
                    label = { Text("Discover") },
                    colors = NavigationBarItemDefaults.colors(unselectedTextColor = TextSecondary)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = AppBackground,
                contentColor = PinkAccent
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Projects (${projects.size})", color = if (selectedTab == 0) PinkAccent else TextSecondary) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Movies", color = if (selectedTab == 1) PinkAccent else TextSecondary) }
                )
            }

            if (selectedTab == 0) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(projects) { project ->
                        ProjectCard(project = project, onClick = { onOpenProject(project) })
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No rendered movies yet. Export one from the editor!", color = TextSecondary)
                }
            }
        }
    }
}

@Composable
fun ProjectCard(project: Project, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PanelBackground)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Color.White)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val frame = project.frames.firstOrNull()
                    frame?.strokes?.forEach { stroke ->
                        if (stroke.points.size > 1) {
                            val path = Path().apply {
                                moveTo(stroke.points[0].x, stroke.points[0].y)
                                for (i in 1 until stroke.points.size) {
                                    lineTo(stroke.points[i].x, stroke.points[i].y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = stroke.color,
                                style = Stroke(width = stroke.strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    }
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = project.name,
                    color = White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${project.frames.size} frames • ${project.fps} FPS",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProjectScreen(
    onBack: () -> Unit,
    onCreate: (String, Int, Int, Int, Color) -> Unit,
    onSelectSize: () -> Unit,
    onSelectFps: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var fps by remember { mutableIntStateOf(12) }
    var width by remember { mutableIntStateOf(1280) }
    var height by remember { mutableIntStateOf(720) }
    var bg by remember { mutableStateOf(Color.White) }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = { Text("Create Project", color = White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Project Name") },
                placeholder = { Text("e.g. Bouncing Ball") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = White,
                    unfocusedTextColor = White,
                    focusedBorderColor = PinkAccent,
                    unfocusedBorderColor = PanelBackground2
                )
            )

            Text("Background Color", color = White, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(Color.White, Color.Black, Color.LightGray).forEach { colorOption ->
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(colorOption)
                            .border(
                                width = if (bg == colorOption) 3.dp else 1.dp,
                                color = if (bg == colorOption) PinkAccent else Color.Gray,
                                shape = CircleShape
                            )
                            .clickable { bg = colorOption }
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectSize() },
                colors = CardDefaults.cardColors(containerColor = PanelBackground)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Canvas Size", color = White, fontWeight = FontWeight.Medium)
                        Text("${width}x${height} (16:9 720p)", color = TextSecondary, fontSize = 13.sp)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectFps() },
                colors = CardDefaults.cardColors(containerColor = PanelBackground)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Frames Per Second", color = White, fontWeight = FontWeight.Medium)
                        Text("$fps FPS (Standard)", color = TextSecondary, fontSize = 13.sp)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { onCreate(name, fps, width, height, bg) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PinkAccent)
            ) {
                Text("Create Project", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasSizeScreen(onBack: () -> Unit, onSelected: (Int, Int) -> Unit) {
    val presets = listOf(
        "YouTube 1080p" to Pair(1920, 1080),
        "YouTube 720p" to Pair(1280, 720),
        "Instagram (1:1)" to Pair(1080, 1080),
        "TikTok (9:16)" to Pair(1080, 1920)
    )

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = { Text("Canvas Size", color = White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { (label, dims) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelected(dims.first, dims.second) },
                    colors = CardDefaults.cardColors(containerColor = PanelBackground)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, color = White, fontWeight = FontWeight.Medium)
                        Text("${dims.first} x ${dims.second}", color = TextSecondary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FpsScreen(onBack: () -> Unit, onSelected: (Int) -> Unit) {
    val fpsOptions = listOf(6, 8, 12, 15, 24, 30)

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = { Text("Frames Per Second", color = White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            fpsOptions.forEach { rate ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelected(rate) },
                    colors = CardDefaults.cardColors(containerColor = PanelBackground)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("$rate FPS", color = White, fontWeight = FontWeight.Medium)
                        Text(if (rate == 12) "Recommended" else "", color = PinkAccent)
                    }
                }
            }
        }
    }
}

private fun pressureAdjustedWidth(base: Float, points: List<DrawPoint>): Float {
    if (points.isEmpty()) return base
    val avg = points.map { it.pressure.coerceIn(0.35f, 1.35f) }.average().toFloat()
    return (base * (0.55f + avg * 0.75f)).coerceAtLeast(0.5f)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun shapePoints(type: ShapeType, start: Offset, end: Offset): List<Offset> {
    val left = minOf(start.x, end.x)
    val right = maxOf(start.x, end.x)
    val top = minOf(start.y, end.y)
    val bottom = maxOf(start.y, end.y)
    return when (type) {
        ShapeType.LINE -> listOf(start, end)
        ShapeType.RECTANGLE -> listOf(
            Offset(left, top), Offset(right, top), Offset(right, bottom), Offset(left, bottom), Offset(left, top)
        )
        ShapeType.TRIANGLE -> listOf(
            Offset((left + right) / 2f, top), Offset(right, bottom), Offset(left, bottom), Offset((left + right) / 2f, top)
        )
        ShapeType.CIRCLE -> {
            val cx = (left + right) / 2f
            val cy = (top + bottom) / 2f
            val rx = (right - left) / 2f
            val ry = (bottom - top) / 2f
            (0..40).map { i ->
                val t = i * (2f * Math.PI.toFloat() / 40f)
                Offset(cx + rx * kotlin.math.cos(t), cy + ry * kotlin.math.sin(t))
            }
        }
    }
}

fun arrowPoints(start: Offset, end: Offset): List<Offset> {
    val angle = kotlin.math.atan2(end.y - start.y, end.x - start.x)
    val head = 22f
    val wing = Math.PI.toFloat() / 6f
    val p1 = Offset(
        end.x - head * kotlin.math.cos(angle - wing),
        end.y - head * kotlin.math.sin(angle - wing)
    )
    val p2 = Offset(
        end.x - head * kotlin.math.cos(angle + wing),
        end.y - head * kotlin.math.sin(angle + wing)
    )
    return listOf(start, end, p1, end, p2)
}

fun EditorScreen(
    project: Project,
    frameIndex: Int,
    onFrameIndexChange: (Int) -> Unit,
    tool: ToolType,
    onToolChange: (ToolType) -> Unit,
    color: Color,
    onColorChange: (Color) -> Unit,
    recentColors: List<Color> = emptyList(),
    customPalette: List<Color> = emptyList(),
    onSaveToPalette: (Color) -> Unit = {},
    onRemoveFromPalette: (Color) -> Unit = {},
    size: Float,
    onSizeChange: (Float) -> Unit,
    texturedBrush: Boolean,
    onTexturedBrushChange: (Boolean) -> Unit,
    textureAmount: Float,
    onTextureAmountChange: (Float) -> Unit,
    onionSkinState: OnionSkinState,
    grid: Boolean,
    isPlaying: Boolean,
    onPlayToggle: () -> Unit,
    isLooping: Boolean,
    onLoopToggle: () -> Unit,
    fps: Int,
    onFpsChange: (Int) -> Unit,
    copiedFrame: Frame?,
    onAddBlankFrame: () -> Unit,
    onInsertBefore: () -> Unit,
    onInsertAfter: () -> Unit,
    onDuplicateFrame: () -> Unit,
    onCopyFrame: () -> Unit,
    onPasteFrame: () -> Unit,
    onDeleteFrame: () -> Unit,
    onMoveFrame: (Int, Int) -> Unit,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTimeline: () -> Unit,
    onOpenLayers: () -> Unit,
    brushPresetStore: BrushPresetStore,
    workspaceVisibility: WorkspaceVisibility = WorkspaceVisibility(),
    advancedWorkspaceState: AdvancedWorkspaceState = AdvancedWorkspaceState(),
    onAdvancedWorkspaceStateChange: (AdvancedWorkspaceState) -> Unit = {},
    onOpenMore: () -> Unit = {}
) {
    val currentFrame = project.frames.getOrNull(frameIndex) ?: project.frames.first()
    val history = remember(currentFrame.id) { FrameDrawingHistory(currentFrame) }
    var currentDrawingPoints = remember { mutableStateListOf<DrawPoint>() }
    val zoomPanState = rememberZoomPanState()
    var showOnionDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showTexturePanel by remember { mutableStateOf(false) }
    var showShapeMenu by remember { mutableStateOf(false) }
    var shapeType by remember { mutableStateOf(ShapeType.RECTANGLE) }
    var textInput by remember { mutableStateOf("") }
    var showTextDialog by remember { mutableStateOf(false) }
    var pendingTextPosition by remember { mutableStateOf(Offset.Zero) }
    var lassoPoints by remember { mutableStateOf(listOf<Offset>()) }
    var canvasRevision by remember { mutableIntStateOf(0) }
    var referenceBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var referenceTransform by remember { mutableStateOf(ReferenceTransform()) }
    var showReferenceDialog by remember { mutableStateOf(false) }
    var referenceEditMode by remember { mutableStateOf(false) }
    var showFrameTools by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val recorder = remember { AudioRecorderController(context) }
    val audioPreview = remember { AudioPreviewController(context) }
    var isRecording by remember { mutableStateOf(false) }
    val recordPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && !isRecording) { recorder.start(); isRecording = true }
    }
    LaunchedEffect(project.audioPath) {
        waveform = project.audioPath?.let { AudioWaveformAnalyzer.analyze(it, 160) } ?: emptyList()
    }
    LaunchedEffect(frameIndex, rotoscopeFrames) {
        if (rotoscopeFrames.isNotEmpty()) referenceBitmap = rotoscopeFrames.getOrNull(frameIndex)?.let { it } ?: referenceBitmap
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Default) {
                val imported = runCatching {
                    RotoscopeSequenceImporter.import(
                        resolver = context.contentResolver,
                        uri = uri,
                        targetFps = project.fps,
                        maxFrames = rotoscopeMaxFrames,
                        targetWidth = project.canvasW,
                        targetHeight = project.canvasH
                    )
                }.getOrNull()
                val images = imported?.frames?.map { it.asImageBitmap() } ?: emptyList()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    rotoscopeFrames = images
                    images.forEachIndexed { index, _ ->
                        project.frames.add(Frame(tag = "ROTO ${index + 1}", durationFrames = 1))
                    }
                    canvasRevision++
                }
            }
        }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching {
                val bmp = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                referenceBitmap = bmp?.asImageBitmap()
                if (bmp != null) referenceTransform = ReferenceTransform(Offset(project.canvasW/2f, project.canvasH/2f), 1f, 0f, 0.45f, false)
            }
        }
    }
    var textSize by remember { mutableFloatStateOf(40f) }
    var showBrushPresets by remember { mutableStateOf(false) }
    var presetName by remember { mutableStateOf("") }
    var showAdvancedPanel by remember { mutableStateOf(false) }
    var motionTrailsEnabled by remember { mutableStateOf(false) }
    var trailRadius by remember { mutableIntStateOf(3) }
    var showBatchFrames by remember { mutableStateOf(false) }
    var batchStartFrame by remember { mutableIntStateOf(0) }
    var batchEndFrame by remember { mutableIntStateOf(0) }
    var waveform by remember { mutableStateOf<List<Float>>(emptyList()) }
    var showProTools by remember { mutableStateOf(false) }
    var rotoscopeFrames by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var rotoscopeMaxFrames by remember { mutableIntStateOf(60) }
    var chromaTolerance by remember { mutableIntStateOf(60) }

    // Selection and Transformation state
    val selectedStrokeIds = remember { mutableStateListOf<String>() }
    var isMarqueeSelecting by remember { mutableStateOf(false) }
    var marqueeStart by remember { mutableStateOf<Offset?>(null) }
    var marqueeCurrent by remember { mutableStateOf<Offset?>(null) }
    var activeTransformHandle by remember { mutableStateOf(TransformHandle.NONE) }
    var transformStartPoint by remember { mutableStateOf(Offset.Zero) }
    var initialStrokesSnapshot by remember { mutableStateOf<List<DrawStroke>>(emptyList()) }
    var clipboardStrokes by remember { mutableStateOf<List<DrawStroke>>(emptyList()) }
    var shapeStart by remember { mutableStateOf<Offset?>(null) }

    // Clear selection when changing frames or tools
    LaunchedEffect(frameIndex) {
        selectedStrokeIds.clear()
        isMarqueeSelecting = false
        marqueeStart = null
        marqueeCurrent = null
        activeTransformHandle = TransformHandle.NONE
    }

    LaunchedEffect(tool) {
        if (tool != ToolType.Select) {
            selectedStrokeIds.clear()
            isMarqueeSelecting = false
            marqueeStart = null
            marqueeCurrent = null
            activeTransformHandle = TransformHandle.NONE
        }
    }

    val selectedStrokes = remember(selectedStrokeIds.toList(), history.strokes.toList()) {
        history.strokes.filter { it.id in selectedStrokeIds }
    }
    val selectionBounds = remember(selectedStrokes) {
        computeBoundingBox(selectedStrokes)
    }

    if (showOnionDialog) {
        OnionSkinSettingsDialog(
            state = onionSkinState,
            onDismiss = { showOnionDialog = false }
        )
    }

    if (texturedBrush) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(selected = true, onClick = { onTexturedBrushChange(false) }, label = { Text("Textured Brush") })
            Text("Grain", color = White, modifier = Modifier.padding(start = 10.dp))
            Slider(textureAmount, onTextureAmountChange, valueRange = 0f..1f, modifier = Modifier.weight(1f))
        }
    } else {
        TextButton(onClick = { onTexturedBrushChange(true) }, modifier = Modifier.padding(horizontal = 8.dp)) { Text("Texture Brush") }
    }

    if (showColorPicker) {
        AdvancedColorPickerDialog(
            currentColor = color,
            recentColors = recentColors,
            customPalette = customPalette,
            onColorChanged = { newColor ->
                onColorChange(newColor)
            },
            onSaveToPalette = onSaveToPalette,
            onRemoveFromPalette = onRemoveFromPalette,
            onEyedropperClick = {
                onToolChange(ToolType.Eyedropper)
            },
            onDismiss = { showColorPicker = false }
        )
    }

    if (showBrushPresets) {
        AlertDialog(
            onDismissRequest = { showBrushPresets = false },
            title = { Text("Brush Presets") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = presetName,
                        onValueChange = { presetName = it },
                        label = { Text("Preset name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Current: ${size.toInt()} px", color = TextSecondary)
                    Text("Saved presets", fontWeight = FontWeight.SemiBold)
                    if (brushPresetStore.presets.isEmpty()) Text("No presets yet", color = TextSecondary)
                    brushPresetStore.presets.forEach { preset ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                onColorChange(preset.color)
                                onSizeChange(preset.width)
                                showBrushPresets = false
                            }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(24.dp).clip(CircleShape).background(preset.color))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(preset.name)
                                Text("${preset.width.toInt()} px", color = TextSecondary, fontSize = 12.sp)
                            }
                            IconButton(onClick = { brushPresetStore.remove(preset.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete preset")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = presetName.isNotBlank(), onClick = {
                    brushPresetStore.add(CustomBrushPreset(name = presetName.trim(), color = color, width = size, alpha = color.alpha))
                    presetName = ""
                }) { Text("Save current") }
            },
            dismissButton = { TextButton(onClick = { showBrushPresets = false }) { Text("Close") } }
        )
    }

    if (showTextDialog) {
        AlertDialog(
            onDismissRequest = { showTextDialog = false; textInput = "" },
            title = { Text("Add Text") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        label = { Text("Text") },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Size: ${textSize.toInt()}sp")
                    Slider(value = textSize, onValueChange = { textSize = it }, valueRange = 12f..160f)
                }
            },
            confirmButton = {
                TextButton(enabled = textInput.isNotBlank(), onClick = {
                    currentFrame.texts.add(
                        CanvasText(
                            text = textInput,
                            x = pendingTextPosition.x,
                            y = pendingTextPosition.y,
                            size = textSize,
                            color = color
                        )
                    )
                    showTextDialog = false
                    textInput = ""
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showTextDialog = false; textInput = "" }) { Text("Cancel") } }
        )
    }

    if (showShapeMenu) {
        AlertDialog(
            onDismissRequest = { showShapeMenu = false },
            title = { Text("Shape") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ShapeType.values().forEach { option ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                shapeType = option
                                onToolChange(ToolType.Shape)
                                showShapeMenu = false
                            }.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = shapeType == option, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(option.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showShapeMenu = false }) { Text("Close") } }
        )
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = { Text(project.name, color = White, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White)
                    }
                },
                actions = {
                    // Zoom In, Zoom Out, and Reset Controls
                    IconButton(onClick = { zoomPanState.zoomOut() }) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = White)
                    }
                    TextButton(onClick = { zoomPanState.reset() }) {
                        Text("${zoomPanState.zoomPercent}%", color = if (zoomPanState.zoom != 1f || zoomPanState.pan != Offset.Zero) PinkAccent else White, fontSize = 12.sp)
                    }
                    IconButton(onClick = { zoomPanState.zoomIn() }) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = White)
                    }
                    if (zoomPanState.zoom != 1f || zoomPanState.pan != Offset.Zero) {
                        IconButton(onClick = { zoomPanState.reset() }) {
                            Icon(Icons.Default.RestartAlt, contentDescription = "Reset View", tint = PinkAccent)
                        }
                    }

                    // Onion Skin toggle & settings in top bar
                    IconButton(onClick = { onionSkinState.toggle() }) {
                        Icon(
                            Icons.Default.Layers,
                            contentDescription = "Toggle Onion Skin",
                            tint = if (onionSkinState.enabled) PinkAccent else TextSecondary.copy(alpha = 0.5f)
                        )
                    }
                    IconButton(onClick = { showOnionDialog = true }) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Onion Skin Settings",
                            tint = if (onionSkinState.enabled) PinkAccent else TextSecondary.copy(alpha = 0.5f)
                        )
                    }

                    IconButton(
                        onClick = { history.undo() },
                        enabled = history.canUndo
                    ) {
                        Icon(
                            Icons.Default.Undo,
                            contentDescription = "Undo",
                            tint = if (history.canUndo) White else TextSecondary.copy(alpha = 0.35f)
                        )
                    }
                    IconButton(
                        onClick = { history.redo() },
                        enabled = history.canRedo
                    ) {
                        Icon(
                            Icons.Default.Redo,
                            contentDescription = "Redo",
                            tint = if (history.canRedo) White else TextSecondary.copy(alpha = 0.35f)
                        )
                    }
                    IconButton(onClick = onOpenTimeline) {
                        Icon(Icons.Default.ViewCarousel, contentDescription = "Timeline", tint = PinkAccent)
                    }
                    IconButton(onClick = onOpenLayers) {
                        Icon(Icons.Default.Layers, contentDescription = "Layers", tint = White)
                    }
                    if (workspaceVisibility.referenceWidget) {
                    IconButton(onClick = { imagePicker.launch("image/*") }
                    }) {
                        Icon(Icons.Default.Image, contentDescription = "Reference image", tint = if (referenceBitmap != null) PinkAccent else White)
                    }
                    IconButton(onClick = { showReferenceDialog = true }) {
                        Icon(Icons.Default.Tune, contentDescription = "Reference transform", tint = White)
                    }
                    FilterChip(
                        selected = referenceEditMode,
                        onClick = { if (referenceBitmap != null) referenceEditMode = !referenceEditMode },
                        label = { Text("Ref Edit") }
                    )
                    if (workspaceVisibility.frameToolsWidget) {
                    IconButton(onClick = { showFrameTools = true }
                    }) {
                        Icon(Icons.Default.Flag, contentDescription = "Frame tools", tint = White)
                    }
                    if (workspaceVisibility.audioWidget) {
                    IconButton(onClick = { showAudioDialog = true }
                    }) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice recording", tint = if (isRecording) PinkAccent else White)
                    }
                    if (workspaceVisibility.advancedWidget) {
                    IconButton(onClick = { showAdvancedPanel = true }
                    }) {
                        Icon(Icons.Default.Tune, contentDescription = "Advanced animation tools", tint = White)
                    }
                    if (workspaceVisibility.proToolsWidget) {
                    IconButton(onClick = { showProTools = true }
                    }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Pro tools", tint = PinkAccent)
                    }
                    IconButton(onClick = onOpenMore) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More tools / show-hide widgets", tint = PinkAccent)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
            }
        },
        bottomBar = {
            if (workspaceVisibility.timeline) {
            AnimationTimelineDock(
                project = project,
                currentFrameIndex = frameIndex,
                isPlaying = isPlaying,
                isLooping = isLooping,
                fps = fps,
                copiedFrame = copiedFrame,
                onFrameIndexChange = onFrameIndexChange,
                onPlayToggle = onPlayToggle,
                onLoopToggle = onLoopToggle,
                onFpsChange = onFpsChange,
                onAddBlankFrame = onAddBlankFrame,
                onInsertBefore = onInsertBefore,
                onInsertAfter = onInsertAfter,
                onDuplicateFrame = onDuplicateFrame,
                onCopyFrame = onCopyFrame,
                onPasteFrame = onPasteFrame,
                onDeleteFrame = onDeleteFrame,
                onMoveFrame = onMoveFrame
            )
            }
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Left Toolbar
            if (workspaceVisibility.leftToolbar) Column(
                modifier = Modifier
                    .width(56.dp)
                    .fillMaxHeight()
                    .background(PanelBackground)
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { onToolChange(ToolType.Brush) },
                    modifier = Modifier.background(
                        if (tool == ToolType.Brush) PanelBackground2 else Color.Transparent,
                        CircleShape
                    )
                ) {
                    Icon(Icons.Default.Brush, contentDescription = "Brush", tint = if (tool == ToolType.Brush) PinkAccent else White)
                }

                if (workspaceVisibility.brushPresetsWidget) {
                IconButton(
                    onClick = { showBrushPresets = true }
                },
                    modifier = Modifier.background(Color.Transparent, CircleShape)
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = "Brush presets", tint = White)
                }

                IconButton(
                    onClick = { onToolChange(ToolType.Eraser) },
                    modifier = Modifier.background(
                        if (tool == ToolType.Eraser) PanelBackground2 else Color.Transparent,
                        CircleShape
                    )
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = "Eraser", tint = if (tool == ToolType.Eraser) PinkAccent else White)
                }

                IconButton(
                    onClick = { onToolChange(ToolType.Select) },
                    modifier = Modifier.background(
                        if (tool == ToolType.Select) PanelBackground2 else Color.Transparent,
                        CircleShape
                    )
                ) {
                    Icon(
                        Icons.Default.HighlightAlt,
                        contentDescription = "Selection & Transform Tool",
                        tint = if (tool == ToolType.Select) PinkAccent else White
                    )
                }

                IconButton(
                    onClick = { onToolChange(ToolType.Lasso) },
                    modifier = Modifier.background(
                        if (tool == ToolType.Lasso) PanelBackground2 else Color.Transparent,
                        CircleShape
                    )
                ) {
                    Icon(Icons.Default.Gesture, contentDescription = "Lasso", tint = if (tool == ToolType.Lasso) PinkAccent else White)
                }

                IconButton(
                    onClick = { onToolChange(ToolType.Eyedropper) },
                    modifier = Modifier.background(
                        if (tool == ToolType.Eyedropper) PanelBackground2 else Color.Transparent,
                        CircleShape
                    )
                ) {
                    Icon(Icons.Default.Colorize, contentDescription = "Eyedropper", tint = if (tool == ToolType.Eyedropper) PinkAccent else White)
                }

                IconButton(
                    onClick = { onToolChange(ToolType.Pan) },
                    modifier = Modifier.background(
                        if (tool == ToolType.Pan) PanelBackground2 else Color.Transparent,
                        CircleShape
                    )
                ) {
                    Icon(Icons.Default.PanTool, contentDescription = "Pan", tint = if (tool == ToolType.Pan) PinkAccent else White)
                }

                IconButton(
                    onClick = { onToolChange(ToolType.Fill) },
                    modifier = Modifier.background(
                        if (tool == ToolType.Fill) PanelBackground2 else Color.Transparent,
                        CircleShape
                    )
                ) {
                    Icon(Icons.Default.FormatColorFill, contentDescription = "Fill", tint = if (tool == ToolType.Fill) PinkAccent else White)
                }

                IconButton(
                    onClick = { onToolChange(ToolType.Text) },
                    modifier = Modifier.background(
                        if (tool == ToolType.Text) PanelBackground2 else Color.Transparent,
                        CircleShape
                    )
                ) {
                    Icon(Icons.Default.TextFields, contentDescription = "Text", tint = if (tool == ToolType.Text) PinkAccent else White)
                }

                IconButton(
                    onClick = { onToolChange(ToolType.Arrow) },
                    modifier = Modifier.background(
                        if (tool == ToolType.Arrow) PanelBackground2 else Color.Transparent,
                        CircleShape
                    )
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Arrow", tint = if (tool == ToolType.Arrow) PinkAccent else White)
                }

                IconButton(
                    onClick = { showShapeMenu = true },
                    modifier = Modifier.background(
                        if (tool == ToolType.Shape) PanelBackground2 else Color.Transparent,
                        CircleShape
                    )
                ) {
                    Icon(Icons.Default.Category, contentDescription = "Shapes", tint = if (tool == ToolType.Shape) PinkAccent else White)
                }

                IconButton(
                    onClick = { history.undo() },
                    enabled = history.canUndo,
                    modifier = Modifier.background(Color.Transparent, CircleShape)
                ) {
                    Icon(
                        Icons.Default.Undo,
                        contentDescription = "Undo",
                        tint = if (history.canUndo) White else TextSecondary.copy(alpha = 0.35f)
                    )
                }

                IconButton(
                    onClick = { history.redo() },
                    enabled = history.canRedo,
                    modifier = Modifier.background(Color.Transparent, CircleShape)
                ) {
                    Icon(
                        Icons.Default.Redo,
                        contentDescription = "Redo",
                        tint = if (history.canRedo) White else TextSecondary.copy(alpha = 0.35f)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (workspaceVisibility.colorWidget) {
                // Active color indicator
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(2.dp, White, CircleShape)
                        .clickable {
                            showColorPicker = true
                        }
                )
                // Quick recent color mini-swatches
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    recentColors.take(3).forEach { rc ->
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(rc)
                                .border(
                                    1.dp,
                                    if (rc == color) PinkAccent else BorderSubtle,
                                    CircleShape
                                )
                                .clickable { onColorChange(rc) }
                        )
                    }
                }
            }

            // Interactive Canvas with Zoom, Pan, and Gesture Support
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(project.backgroundColor)
                    .pointerInput(referenceEditMode, referenceBitmap, zoomPanState.zoom) {
                        if (referenceEditMode && referenceBitmap != null) {
                            detectTransformGestures { _, panChange, zoomChange, rotationChange ->
                                if (!referenceTransform.locked) {
                                    referenceTransform = referenceTransform.copy(
                                        position = referenceTransform.position + panChange / zoomPanState.zoom.coerceAtLeast(0.01f),
                                        scale = (referenceTransform.scale * zoomChange).coerceIn(0.05f, 20f),
                                        rotationDegrees = referenceTransform.rotationDegrees + rotationChange
                                    )
                                    canvasRevision++
                                }
                            }
                        }
                    }
                    .pointerInput(tool, color, size, frameIndex, zoomPanState.zoom, zoomPanState.pan, canvasRevision) {
                        detectZoomPanOrDraw(
                            isPanTool = (tool == ToolType.Pan),
                            zoomPanState = zoomPanState,
                            onDrawStart = { canvasPoint, pressure ->
                                if (tool == ToolType.Eyedropper) {
                                    var sampled = project.backgroundColor
                                    val hitDistSq = 36f * 36f
                                    val strokes = currentFrame.strokes
                                    for (i in strokes.indices.reversed()) {
                                        val s = strokes[i]
                                        val hit = s.points.any { p ->
                                            val dx = p.x - canvasPoint.x
                                            val dy = p.y - canvasPoint.y
                                            (dx * dx + dy * dy) <= (hitDistSq + s.strokeWidth * s.strokeWidth)
                                        }
                                        if (hit && !s.isEraser) {
                                            sampled = s.color
                                            break
                                        }
                                    }
                                    onColorChange(sampled)
                                    onToolChange(ToolType.Brush)
                                } else if (tool == ToolType.Select) {
                                    val bounds = selectionBounds
                                    var handle = TransformHandle.NONE
                                    if (bounds != null && selectedStrokes.isNotEmpty()) {
                                        handle = hitTestHandle(bounds, canvasPoint, zoomPanState.zoom)
                                    }

                                    if (handle != TransformHandle.NONE) {
                                        activeTransformHandle = handle
                                        transformStartPoint = canvasPoint
                                        initialStrokesSnapshot = selectedStrokes.map { it.deepCopy() }
                                    } else {
                                        val tappedStroke = history.strokes.lastOrNull { stroke ->
                                            hitTestStroke(stroke, canvasPoint, 24f / zoomPanState.zoom)
                                        }
                                        if (tappedStroke != null) {
                                            selectedStrokeIds.clear()
                                            selectedStrokeIds.add(tappedStroke.id)
                                            activeTransformHandle = TransformHandle.BODY
                                            transformStartPoint = canvasPoint
                                            initialStrokesSnapshot = listOf(tappedStroke.deepCopy())
                                        } else {
                                            selectedStrokeIds.clear()
                                            isMarqueeSelecting = true
                                            marqueeStart = canvasPoint
                                            marqueeCurrent = canvasPoint
                                            activeTransformHandle = TransformHandle.NONE
                                            initialStrokesSnapshot = emptyList()
                                        }
                                    }
                                } else if (tool == ToolType.Fill) {
                                    val px = canvasPoint.x.roundToInt().coerceIn(0, project.canvasW - 1)
                                    val py = canvasPoint.y.roundToInt().coerceIn(0, project.canvasH - 1)
                                    currentFrame.fills.add(FillMark(px, py, color)); canvasRevision++
                                } else if (tool == ToolType.Lasso) {
                                    lassoPoints = listOf(canvasPoint)
                                } else if (tool == ToolType.Text) {
                                    pendingTextPosition = canvasPoint
                                    showTextDialog = true
                                } else if (tool == ToolType.Arrow || tool == ToolType.Shape) {
                                    shapeStart = canvasPoint
                                    currentDrawingPoints.clear()
                                    currentDrawingPoints.add(DrawPoint(canvasPoint.x, canvasPoint.y, pressure))
                                } else {
                                    currentDrawingPoints.clear()
                                    currentDrawingPoints.add(DrawPoint(canvasPoint.x, canvasPoint.y, pressure))
                                }
                            },
                            onDraw = { canvasPoint, pressure ->
                                if (tool == ToolType.Lasso) {
                                    lassoPoints = lassoPoints + canvasPoint
                                } else if (tool == ToolType.Select) {
                                    if (activeTransformHandle != TransformHandle.NONE && initialStrokesSnapshot.isNotEmpty()) {
                                        val initialBounds = computeBoundingBox(initialStrokesSnapshot)
                                        if (initialBounds != null) {
                                            val transformed = when (activeTransformHandle) {
                                                TransformHandle.BODY -> {
                                                    val delta = canvasPoint - transformStartPoint
                                                    transformTranslate(initialStrokesSnapshot, delta)
                                                }
                                                TransformHandle.ROTATE -> {
                                                    val center = initialBounds.center
                                                    val angleStart = kotlin.math.atan2(transformStartPoint.y - center.y, transformStartPoint.x - center.x)
                                                    val angleCurr = kotlin.math.atan2(canvasPoint.y - center.y, canvasPoint.x - center.x)
                                                    val angleDelta = angleCurr - angleStart
                                                    transformRotate(initialStrokesSnapshot, center, angleDelta)
                                                }
                                                TransformHandle.TOP_LEFT -> {
                                                    val pivot = initialBounds.bottomRight
                                                    val sx = (pivot.x - canvasPoint.x) / initialBounds.width
                                                    val sy = (pivot.y - canvasPoint.y) / initialBounds.height
                                                    transformScale(initialStrokesSnapshot, pivot, sx, sy)
                                                }
                                                TransformHandle.TOP_RIGHT -> {
                                                    val pivot = initialBounds.bottomLeft
                                                    val sx = (canvasPoint.x - pivot.x) / initialBounds.width
                                                    val sy = (pivot.y - canvasPoint.y) / initialBounds.height
                                                    transformScale(initialStrokesSnapshot, pivot, sx, sy)
                                                }
                                                TransformHandle.BOTTOM_LEFT -> {
                                                    val pivot = initialBounds.topRight
                                                    val sx = (pivot.x - canvasPoint.x) / initialBounds.width
                                                    val sy = (canvasPoint.y - pivot.y) / initialBounds.height
                                                    transformScale(initialStrokesSnapshot, pivot, sx, sy)
                                                }
                                                TransformHandle.BOTTOM_RIGHT -> {
                                                    val pivot = initialBounds.topLeft
                                                    val sx = (canvasPoint.x - pivot.x) / initialBounds.width
                                                    val sy = (canvasPoint.y - pivot.y) / initialBounds.height
                                                    transformScale(initialStrokesSnapshot, pivot, sx, sy)
                                                }
                                                TransformHandle.TOP_CENTER -> {
                                                    val pivot = initialBounds.bottomCenter
                                                    val sy = (pivot.y - canvasPoint.y) / initialBounds.height
                                                    transformScale(initialStrokesSnapshot, pivot, 1.0f, sy)
                                                }
                                                TransformHandle.BOTTOM_CENTER -> {
                                                    val pivot = initialBounds.topCenter
                                                    val sy = (canvasPoint.y - pivot.y) / initialBounds.height
                                                    transformScale(initialStrokesSnapshot, pivot, 1.0f, sy)
                                                }
                                                TransformHandle.CENTER_LEFT -> {
                                                    val pivot = initialBounds.centerRight
                                                    val sx = (pivot.x - canvasPoint.x) / initialBounds.width
                                                    transformScale(initialStrokesSnapshot, pivot, sx, 1.0f)
                                                }
                                                TransformHandle.CENTER_RIGHT -> {
                                                    val pivot = initialBounds.centerLeft
                                                    val sx = (canvasPoint.x - pivot.x) / initialBounds.width
                                                    transformScale(initialStrokesSnapshot, pivot, sx, 1.0f)
                                                }
                                                else -> initialStrokesSnapshot
                                            }
                                            val afterMap = transformed.associateBy { it.id }
                                            for (i in history.strokes.indices) {
                                                val updated = afterMap[history.strokes[i].id]
                                                if (updated != null) {
                                                    history.strokes[i] = updated
                                                }
                                            }
                                        }
                                    } else if (isMarqueeSelecting) {
                                        marqueeCurrent = canvasPoint
                                    }
                                } else if (tool == ToolType.Arrow || tool == ToolType.Shape) {
                                    val start = shapeStart
                                    if (start != null) {
                                        currentDrawingPoints.clear()
                                        currentDrawingPoints.add(DrawPoint(start.x, start.y, pressure))
                                        currentDrawingPoints.add(DrawPoint(canvasPoint.x, canvasPoint.y, pressure))
                                    }
                                } else if (tool != ToolType.Eyedropper) {
                                    currentDrawingPoints.add(DrawPoint(canvasPoint.x, canvasPoint.y, pressure))
                                }
                            },
                            onDrawEnd = {
                                if (tool == ToolType.Lasso) {
                                    val polygon = LassoSelection.close(lassoPoints.map { DrawPoint(it.x, it.y) })
                                    selectedStrokeIds.clear()
                                    selectedStrokeIds.addAll(currentFrame.strokes.filter { LassoSelection.strokeIntersects(it, polygon) }.map { it.id })
                                    lassoPoints = emptyList(); canvasRevision++
                                } else if (tool == ToolType.Select) {
                                    if (activeTransformHandle != TransformHandle.NONE && initialStrokesSnapshot.isNotEmpty()) {
                                        val currentStrokes = history.strokes.filter { it.id in selectedStrokeIds }
                                        history.replaceStrokes(before = initialStrokesSnapshot, after = currentStrokes)
                                        activeTransformHandle = TransformHandle.NONE
                                        initialStrokesSnapshot = emptyList()
                                    } else if (isMarqueeSelecting) {
                                        val start = marqueeStart
                                        val current = marqueeCurrent
                                        if (start != null && current != null) {
                                            val dist = (current - start).getDistance()
                                            if (dist > 8f / zoomPanState.zoom) {
                                                val minX = kotlin.math.min(start.x, current.x)
                                                val minY = kotlin.math.min(start.y, current.y)
                                                val maxX = kotlin.math.max(start.x, current.x)
                                                val maxY = kotlin.math.max(start.y, current.y)
                                                val marqueeBox = BoundingBox(minX, minY, maxX, maxY)
                                                val matched = history.strokes.filter { isStrokeInMarquee(it, marqueeBox) }
                                                selectedStrokeIds.clear()
                                                selectedStrokeIds.addAll(matched.map { it.id })
                                            } else {
                                                selectedStrokeIds.clear()
                                            }
                                        }
                                        isMarqueeSelecting = false
                                        marqueeStart = null
                                        marqueeCurrent = null
                                    }
                                } else if ((tool == ToolType.Arrow || tool == ToolType.Shape) && currentDrawingPoints.size >= 2) {
                                    val a = currentDrawingPoints.first()
                                    val b = currentDrawingPoints.last()
                                    val generated = if (tool == ToolType.Arrow) {
                                        arrowPoints(Offset(a.x, a.y), Offset(b.x, b.y))
                                    } else {
                                        shapePoints(shapeType, Offset(a.x, a.y), Offset(b.x, b.y))
                                    }
                                    history.addStroke(
                                        DrawStroke(
                                            points = generated.map { DrawPoint(it.x, it.y, 1f) },
                                            color = color,
                                            strokeWidth = size,
                                            alpha = color.alpha
                                        )
                                    )
                                    currentDrawingPoints.clear()
                                    shapeStart = null
                                } else if (tool != ToolType.Eyedropper && currentDrawingPoints.isNotEmpty()) {
                                    history.addStroke(
                                        DrawStroke(
                                            points = currentDrawingPoints.toList(),
                                            color = if (tool == ToolType.Eraser) project.backgroundColor else color,
                                            strokeWidth = size,
                                            alpha = color.alpha,
                                            isEraser = tool == ToolType.Eraser,
                                            layerIndex = selectedLayerIndex,
                                            textured = texturedBrush && tool == ToolType.Brush
                                        )
                                    )
                                    currentDrawingPoints.clear()
                                }
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Apply zoom and pan transformation to canvas rendering
                    translate(left = zoomPanState.pan.x, top = zoomPanState.pan.y) {
                        scale(scale = zoomPanState.zoom, pivot = Offset.Zero) {
                            // Grid
                            if (grid) {
                                val step = 40.dp.toPx()
                                for (x in 0 until (size.width / step).toInt()) {
                                    drawLine(
                                        color = Color.LightGray.copy(alpha = 0.4f),
                                        start = Offset(x * step, 0f),
                                        end = Offset(x * step, size.height),
                                        strokeWidth = 1f / zoomPanState.zoom
                                    )
                                }
                                for (y in 0 until (size.height / step).toInt()) {
                                    drawLine(
                                        color = Color.LightGray.copy(alpha = 0.4f),
                                        start = Offset(0f, y * step),
                                        end = Offset(size.width, y * step),
                                        strokeWidth = 1f / zoomPanState.zoom
                                    )
                                }
                            }

                            // Motion trails: persistent colored ghost strokes for trajectory reading.
                            if (motionTrailsEnabled) {
                                val all = project.frames.map { it.strokes.toList() }
                                MotionTrailEngine.samples(all, frameIndex, trailRadius).forEach { sample ->
                                    if (sample.stroke.points.size > 1) {
                                        val path = OpenSourceStrokeSmoother.build(OpenSourceDrawingEngine.smooth(sample.stroke.points.map { Offset(it.x, it.y) }))
                                        drawPath(path, sample.tint.copy(alpha = sample.alpha), style = Stroke(width = pressureAdjustedWidth(sample.stroke.strokeWidth, sample.stroke.points), cap = StrokeCap.Round, join = StrokeJoin.Round))
                                    }
                                }
                            }

                            // Onion Skinning (Previous and Next frames rendered underneath active frame)
                            if (onionSkinState.enabled) {
                                // Previous frames (from oldest ghost down to immediate previous)
                                for (dist in onionSkinState.prevFrames downTo 1) {
                                    val prevIdx = frameIndex - dist
                                    if (prevIdx in project.frames.indices) {
                                        val frame = project.frames[prevIdx]
                                        val frameAlpha = (onionSkinState.opacity * (1f - (dist - 1) * 0.22f)).coerceIn(0.06f, 1f)
                                        val ghostColor = if (onionSkinState.coloredTint) Color(0xFFEF4444) else null

                                        frame.strokes.forEach { stroke ->
                                            if (stroke.points.size > 1) {
                                                val path = OpenSourceStrokeSmoother.build(OpenSourceDrawingEngine.smooth(stroke.points.map { Offset(it.x, it.y) }))
                                                drawPath(
                                                    path = path,
                                                    color = (ghostColor ?: stroke.color).copy(alpha = frameAlpha),
                                                    style = Stroke(width = pressureAdjustedWidth(stroke.strokeWidth, stroke.points), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Next frames (from furthest forward down to immediate next)
                                for (dist in onionSkinState.nextFrames downTo 1) {
                                    val nextIdx = frameIndex + dist
                                    if (nextIdx in project.frames.indices) {
                                        val frame = project.frames[nextIdx]
                                        val frameAlpha = (onionSkinState.opacity * (1f - (dist - 1) * 0.22f)).coerceIn(0.06f, 1f)
                                        val ghostColor = if (onionSkinState.coloredTint) Color(0xFF10B981) else null

                                        frame.strokes.forEach { stroke ->
                                            if (stroke.points.size > 1) {
                                                val path = OpenSourceStrokeSmoother.build(OpenSourceDrawingEngine.smooth(stroke.points.map { Offset(it.x, it.y) }))
                                                drawPath(
                                                    path = path,
                                                    color = (ghostColor ?: stroke.color).copy(alpha = frameAlpha),
                                                    style = Stroke(width = pressureAdjustedWidth(stroke.strokeWidth, stroke.points), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Text objects (adapted from sameerasw/Canvas TextItem model)
                            currentFrame.texts.forEach { item ->
                                drawContext.canvas.nativeCanvas.drawText(
                                    item.text,
                                    item.x,
                                    item.y + item.size,
                                    android.graphics.Paint().apply {
                                        isAntiAlias = true
                                        color = item.color.toArgb()
                                        textSize = item.size
                                        typeface = android.graphics.Typeface.DEFAULT
                                    }
                                )
                            }

                            // Connected flood-fill engine: rasterize the current boundaries, flood-fill, then draw vectors over it.
                            if (currentFrame.fills.isNotEmpty()) {
                                val fillBitmap = android.graphics.Bitmap.createBitmap(project.canvasW, project.canvasH, android.graphics.Bitmap.Config.ARGB_8888)
                                val bc = AndroidCanvas(fillBitmap)
                                bc.drawColor(project.backgroundColor.toArgb())
                                val bp = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply { style = AndroidPaint.Style.STROKE; strokeCap = AndroidPaint.Cap.ROUND; strokeJoin = AndroidPaint.Join.ROUND }
                                currentFrame.strokes.forEach { s ->
                                    bp.color = if (s.isEraser) project.backgroundColor.toArgb() else s.color.copy(alpha = s.alpha).toArgb(); bp.strokeWidth = s.strokeWidth
                                    val path = android.graphics.Path(); s.points.firstOrNull()?.let { path.moveTo(it.x,it.y) }; s.points.drop(1).forEach { path.lineTo(it.x,it.y) }; bc.drawPath(path,bp)
                                }
                                currentFrame.fills.forEach { mark -> FloodFillEngine.fill(fillBitmap, project.canvasW, project.canvasH, mark.x, mark.y, mark.color.toArgb(), mark.tolerance) }
                                drawImage(fillBitmap.asImageBitmap())
                            }

                            // Optional reference image with move/scale/rotate/opacity transform.
                            referenceBitmap?.let { img ->
                                translate(referenceTransform.position.x, referenceTransform.position.y) {
                                    rotate(referenceTransform.rotationDegrees) {
                                        scale(referenceTransform.scale, referenceTransform.scale, Offset.Zero) {
                                            drawImage(img, topLeft = Offset(-img.width/2f, -img.height/2f), alpha = referenceTransform.opacity)
                                        }
                                    }
                                }
                            }

                            // Pro layer compositor: clipping/blend-aware raster composition.
                            val composedLayers = renderLayerComposite(project, currentFrame, textureAmount)
                            drawImage(composedLayers.asImageBitmap())
                            composedLayers.recycle()

                            // Active Frame Strokes selection overlays (content already composited above).
                            history.strokes.filter { it.id in selectedStrokeIds }.forEach { stroke ->
                                if (stroke.points.size > 1) {
                                    val path = OpenSourceStrokeSmoother.build(OpenSourceDrawingEngine.smooth(stroke.points.map { Offset(it.x, it.y) }))
                                    drawPath(path, PinkAccent.copy(alpha = 0.35f), style = Stroke(width = 3f, cap = StrokeCap.Round))
                                }
                            }

                            // Active dragging stroke preview
                            if (currentDrawingPoints.size > 1) {
                                val path = OpenSourceStrokeSmoother.build(OpenSourceDrawingEngine.smooth(currentDrawingPoints.map { Offset(it.x, it.y) }))
                                drawPath(
                                    path = path,
                                    color = if (tool == ToolType.Eraser) project.backgroundColor else color,
                                    style = Stroke(width = pressureAdjustedWidth(size, currentDrawingPoints), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            }

                            if (tool == ToolType.Lasso && lassoPoints.size > 1) {
                                val p = Path().apply { moveTo(lassoPoints.first().x, lassoPoints.first().y); lassoPoints.drop(1).forEach { lineTo(it.x,it.y) } }
                                drawPath(p, PinkAccent, style = Stroke(width = 3f))
                            }

                            // Visual Selection Boundary and Transform Handles
                            if (tool == ToolType.Select || tool == ToolType.Lasso) {
                                val b = selectionBounds
                                if (b != null && selectedStrokes.isNotEmpty()) {
                                    drawSelectionOverlay(
                                        bounds = b,
                                        zoom = zoomPanState.zoom,
                                        accentColor = PinkAccent
                                    )
                                }
                                val mStart = marqueeStart
                                val mCurr = marqueeCurrent
                                if (isMarqueeSelecting && mStart != null && mCurr != null) {
                                    drawMarqueeBox(
                                        start = mStart,
                                        current = mCurr,
                                        zoom = zoomPanState.zoom,
                                        accentColor = PinkAccent
                                    )
                                }
                            }
                        }
                    }
                }

                // Floating Action Bar for Selected Artwork
                if (tool == ToolType.Select && selectedStrokes.isNotEmpty() && selectionBounds != null) {
                    SelectionTransformBar(
                        selectedCount = selectedStrokes.size,
                        canPaste = clipboardStrokes.isNotEmpty(),
                        onFlipHorizontal = {
                            val bounds = selectionBounds
                            if (bounds != null) {
                                val before = selectedStrokes.map { it.deepCopy() }
                                val after = transformFlipHorizontal(before, bounds.center.x)
                                history.replaceStrokes(before, after)
                            }
                        },
                        onFlipVertical = {
                            val bounds = selectionBounds
                            if (bounds != null) {
                                val before = selectedStrokes.map { it.deepCopy() }
                                val after = transformFlipVertical(before, bounds.center.y)
                                history.replaceStrokes(before, after)
                            }
                        },
                        onDuplicate = {
                            val offset = Offset(24f / zoomPanState.zoom, 24f / zoomPanState.zoom)
                            val duplicated = selectedStrokes.map { stroke ->
                                val cloned = stroke.deepCopy()
                                val movedPoints = cloned.points.map { p -> DrawPoint(p.x + offset.x, p.y + offset.y) }
                                cloned.copy(points = movedPoints)
                            }
                            history.addStrokes(duplicated)
                            selectedStrokeIds.clear()
                            selectedStrokeIds.addAll(duplicated.map { it.id })
                        },
                        onCopy = {
                            clipboardStrokes = selectedStrokes.map { it.deepCopy() }
                        },
                        onPaste = {
                            if (clipboardStrokes.isNotEmpty()) {
                                val offset = Offset(32f / zoomPanState.zoom, 32f / zoomPanState.zoom)
                                val pasted = clipboardStrokes.map { stroke ->
                                    val cloned = stroke.deepCopy()
                                    val movedPoints = cloned.points.map { p -> DrawPoint(p.x + offset.x, p.y + offset.y) }
                                    cloned.copy(points = movedPoints)
                                }
                                history.addStrokes(pasted)
                                selectedStrokeIds.clear()
                                selectedStrokeIds.addAll(pasted.map { it.id })
                            }
                        },
                        onDelete = {
                            history.deleteStrokes(selectedStrokes)
                            selectedStrokeIds.clear()
                        },
                        onClearSelection = {
                            selectedStrokeIds.clear()
                        },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                    )
                }

                // Quick Paste floating badge when clipboard contains artwork and nothing is selected
                if (tool == ToolType.Select && selectedStrokes.isEmpty() && clipboardStrokes.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                val offset = Offset(32f / zoomPanState.zoom, 32f / zoomPanState.zoom)
                                val pasted = clipboardStrokes.map { stroke ->
                                    val cloned = stroke.deepCopy()
                                    val movedPoints = cloned.points.map { p -> DrawPoint(p.x + offset.x, p.y + offset.y) }
                                    cloned.copy(points = movedPoints)
                                }
                                history.addStrokes(pasted)
                                selectedStrokeIds.clear()
                                selectedStrokeIds.addAll(pasted.map { it.id })
                            }
                            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
                        color = PanelBackground,
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentPaste,
                                contentDescription = "Paste Clipboard Artwork",
                                tint = PinkAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "Paste (${clipboardStrokes.size} items)",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                }
                }
            }
        }
    }
    if (showAdvancedPanel) {
        AdvancedAnimationToolsDialog(
            project = project,
            frameIndex = frameIndex,
            motionTrailsEnabled = motionTrailsEnabled,
            trailRadius = trailRadius,
            waveform = waveform,
            onMotionTrails = { motionTrailsEnabled = it },
            onTrailRadius = { trailRadius = it },
            onOpenBatch = { showBatchFrames = true },
            onExportSheet = {
                val file = java.io.File(context.filesDir, "${project.name.replace("[^A-Za-z0-9_-]".toRegex(), "_")}-spritesheet.png")
                SpriteSheetExporter.export(project, file)
                android.widget.Toast.makeText(context, "Spritesheet exported", android.widget.Toast.LENGTH_SHORT).show()
            },
            onExportMp4 = {
                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Default) {
                    val tempVideo = java.io.File(context.cacheDir, "motioncanvas-video.mp4")
                    val finalVideo = java.io.File(context.filesDir, "${project.name.replace("[^A-Za-z0-9_-]".toRegex(), "_")}.mp4")
                    val frames = project.frames.flatMap { frame -> List(frame.durationFrames.coerceAtLeast(1)) { ProjectBitmapRenderer.render(project, frame) } }
                    val videoResult = Mp4VideoExporter.export(frames, project.fps, tempVideo)
                    if (!videoResult.isSuccess) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            android.widget.Toast.makeText(context, "MP4 export failed: ${videoResult.exceptionOrNull()?.message}", android.widget.Toast.LENGTH_LONG).show()
                        }
                        return@launch
                    }
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        if (project.audioTracks.any { it.enabled && it.clips.any { clip -> !clip.muted } }) {
                            Media3MultiTrackExporter.export(
                                context = context,
                                video = tempVideo,
                                tracks = project.audioTracks,
                                fps = project.fps,
                                output = finalVideo
                            ) { result ->
                                android.widget.Toast.makeText(context, if (result.isSuccess) "MP4 exported with mixed audio" else "Audio/video export failed: ${result.exceptionOrNull()?.message}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        } else {
                            tempVideo.copyTo(finalVideo, overwrite = true)
                            android.widget.Toast.makeText(context, "MP4 exported", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            onDismiss = { showAdvancedPanel = false }
        )
    }
    if (showBatchFrames) {
        BatchFrameDialog(
            project = project,
            frameIndex = frameIndex,
            onDismiss = { showBatchFrames = false },
            onBatchChanged = { canvasRevision++ }
        )
    }
    if (showReferenceDialog) {
        AlertDialog(
            onDismissRequest = { showReferenceDialog = false },
            containerColor = PanelBackground,
            title = { Text("Reference Transform", color = White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Use Ref Edit to drag, pinch/zoom and rotate the reference directly on the canvas.", color = TextSecondary)
                    Text("Opacity ${(referenceTransform.opacity * 100).toInt()}%", color = White)
                    Slider(
                        value = referenceTransform.opacity,
                        onValueChange = { referenceTransform = referenceTransform.copy(opacity = it); canvasRevision++ },
                        valueRange = 0.05f..1f
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Lock reference", color = White)
                        Switch(
                            checked = referenceTransform.locked,
                            onCheckedChange = {
                                referenceTransform = referenceTransform.copy(locked = it)
                                canvasRevision++
                            }
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            referenceTransform = ReferenceTransform(
                                Offset(project.canvasW / 2f, project.canvasH / 2f),
                                1f, 0f, referenceTransform.opacity, referenceTransform.locked
                            )
                            canvasRevision++
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Reset Transform") }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReferenceDialog = false }) { Text("Done", color = PinkAccent) }
            }
        )
    }
    if (showProTools) {
        ProToolsDialog(
            project = project, currentFrame = frameIndex, chromaTolerance = chromaTolerance,
            onChromaTolerance = { chromaTolerance = it },
            onImportVideo = { videoPicker.launch("video/*") },
            onApplyChroma = {
                referenceBitmap?.let { bmp ->
                    referenceBitmap = ChromaKeyEngine.removeColor(bmp.asAndroidBitmap(), android.graphics.Color.GREEN, chromaTolerance, 15).asImageBitmap()
                    canvasRevision++
                }
            },
            onBatchDuplicate = { BatchFrameOperations.duplicate(project.frames, setOf(frameIndex)); canvasRevision++ },
            onBatchDelete = { BatchFrameOperations.delete(project.frames, setOf(frameIndex)); canvasRevision++ },
            onSmudge = {
                val bmp = referenceBitmap?.asAndroidBitmap()
                if (bmp != null) { referenceBitmap = SmudgeEngine.smudge(bmp, project.canvasW/2, project.canvasH/2).asImageBitmap(); canvasRevision++ }
            },
            onDismiss = { showProTools = false }
        )
    }
    if (showAudioDialog) {
        AudioToolsDialog(
            project = project, currentFrame = frameIndex, isRecording = isRecording, waveform = waveform,
            onStartRecording = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    recorder.start(); isRecording = true
                } else { recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
            },
            onStopRecording = {
                val file = recorder.stop(); isRecording = false
                file?.let { val clip=AudioClip(java.util.UUID.randomUUID().toString(), it.absolutePath, frameIndex, maxOf(1, project.frames.size-frameIndex)); project.audioPath = it.absolutePath; project.audioClips.add(clip); if(project.audioTracks.isEmpty()) project.audioTracks.add(AudioTrack("track-1","Audio 1")); project.audioTracks[0].clips.add(clip) }
            },
            onPreview = { project.audioPath?.let { audioPreview.play(Uri.fromFile(java.io.File(it))) } },
            onStopPreview = { audioPreview.stop() },
            onDismiss = { showAudioDialog = false }
        )
    }
}


}

@Composable
fun OnionSkinSettingsDialog(
    state: OnionSkinState,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PanelBackground,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Onion Skin Settings", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = White)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Master Enable Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Enable Onion Skin", color = White, fontWeight = FontWeight.Medium)
                        Text(if (state.enabled) "Active on canvas" else "Hidden", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = state.enabled,
                        onCheckedChange = { state.enabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = White,
                            checkedTrackColor = PinkAccent
                        )
                    )
                }

                // Previous frames (0 to 5)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Past Frames (Previous)", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("${state.prevFrames} frames", color = Color(0xFFEF4444), fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        (0..5).forEach { count ->
                            Button(
                                onClick = { state.prevFrames = count },
                                modifier = Modifier.weight(1f).height(34.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (state.prevFrames == count) Color(0xFFEF4444) else PanelBackground2,
                                    contentColor = White
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("$count", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Next frames (0 to 5)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Future Frames (Next)", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("${state.nextFrames} frames", color = Color(0xFF10B981), fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        (0..5).forEach { count ->
                            Button(
                                onClick = { state.nextFrames = count },
                                modifier = Modifier.weight(1f).height(34.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (state.nextFrames == count) Color(0xFF10B981) else PanelBackground2,
                                    contentColor = White
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("$count", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Opacity Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Ghost Opacity", color = White, fontSize = 12.sp)
                        Text("${state.opacityPercent}%", color = PinkAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = state.opacity,
                        onValueChange = { state.opacity = it },
                        valueRange = 0.1f..0.9f,
                        colors = SliderDefaults.colors(thumbColor = PinkAccent, activeTrackColor = PinkAccent)
                    )
                }

                // Color Tint Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Color Coded Tints", color = White, fontSize = 12.sp)
                        Text("Red for past, green for future", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = state.coloredTint,
                        onCheckedChange = { state.coloredTint = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = White, checkedTrackColor = PinkAccent)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PinkAccent)
            ) {
                Text("Done", color = White)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)


@Composable
fun AdvancedEngineStatusWidget(
    state: AdvancedWorkspaceState,
    modifier: Modifier = Modifier
) {
    val active = buildList {
        if (state.perspectiveEnabled) add("Perspective")
        if (state.rulerEnabled) add("Ruler")
        if (state.bezierEnabled) add("Bezier")
        if (state.motionGuideEnabled) add("Motion Guide")
        if (state.cameraEnabled) add("Camera")
        if (state.ikEnabled) add("IK")
        if (state.smudgeEnabled) add("Smudge")
        if (state.liquifyEnabled) add("Liquify")
        if (state.particlesEnabled) add("Particles")
        if (state.magicWandEnabled) add("Magic Wand")
        if (state.autosaveEnabled) add("Autosave")
        if (state.brushDynamicsEnabled) add("Brush Dynamics")
        if (state.multiFrameTransformEnabled) add("Multi-frame Transform")
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = PanelBackground
    ) {
        Text(
            if (active.isEmpty()) "No advanced engines enabled"
            else "Active: ${active.joinToString(" • ")}",
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(10.dp)
        )
    }
}

@Composable
fun MoreToolsScreen(
    visibility: WorkspaceVisibility,
    onVisibilityChange: (WorkspaceVisibility) -> Unit,
    advancedState: AdvancedWorkspaceState = AdvancedWorkspaceState(),
    onAdvancedStateChange: (AdvancedWorkspaceState) -> Unit = {},
    onBack: () -> Unit
) {
    val rows = listOf(
        "Left toolbar" to visibility.leftToolbar,
        "Top toolbar / widgets" to visibility.topBar,
        "Timeline" to visibility.timeline,
        "Reference widget" to visibility.referenceWidget,
        "Frame tools widget" to visibility.frameToolsWidget,
        "Audio / recording widget" to visibility.audioWidget,
        "Advanced animation widget" to visibility.advancedWidget,
        "Pro tools widget" to visibility.proToolsWidget,
        "Brush presets widget" to visibility.brushPresetsWidget,
        "Color widget" to visibility.colorWidget
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("More • Workspace Widgets", color = White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White)
                    }
                },
                actions = {
                    TextButton(onClick = { onVisibilityChange(WorkspaceVisibility()) }) {
                        Text("Show all", color = PinkAccent)
                    }
                    TextButton(onClick = {
                        onVisibilityChange(WorkspaceVisibility(
                            leftToolbar = false,
                            topBar = true,
                            timeline = false,
                            referenceWidget = false,
                            frameToolsWidget = false,
                            audioWidget = false,
                            advancedWidget = false,
                            proToolsWidget = false,
                            brushPresetsWidget = false,
                            colorWidget = false
                        ))
                    }) {
                        Text("Minimal", color = White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        },
        containerColor = AppBackground
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Hide / unhide editor controls. Changes apply immediately when you return to the editor.",
                color = TextSecondary,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(12.dp))
            Text("Advanced engine switches", color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            val advancedRows = listOf(
                "Perspective guides" to advancedState.perspectiveEnabled,
                "Precision ruler" to advancedState.rulerEnabled,
                "Bezier path" to advancedState.bezierEnabled,
                "Motion guide" to advancedState.motionGuideEnabled,
                "2D camera" to advancedState.cameraEnabled,
                "IK controller" to advancedState.ikEnabled,
                "Smudge engine" to advancedState.smudgeEnabled,
                "Liquify" to advancedState.liquifyEnabled,
                "Particles" to advancedState.particlesEnabled,
                "Magic wand" to advancedState.magicWandEnabled,
                "Autosave" to advancedState.autosaveEnabled,
                "Brush dynamics" to advancedState.brushDynamicsEnabled,
                "Multi-frame transform" to advancedState.multiFrameTransformEnabled
            )
            advancedRows.forEach { (label, enabled) ->
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(PanelBackground).padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, color = White, modifier = Modifier.weight(1f))
                    Switch(
                        checked = enabled,
                        onCheckedChange = { v ->
                            onAdvancedStateChange(
                                when (label) {
                                    "Perspective guides" -> advancedState.copy(perspectiveEnabled = v)
                                    "Precision ruler" -> advancedState.copy(rulerEnabled = v)
                                    "Bezier path" -> advancedState.copy(bezierEnabled = v)
                                    "Motion guide" -> advancedState.copy(motionGuideEnabled = v)
                                    "2D camera" -> advancedState.copy(cameraEnabled = v)
                                    "IK controller" -> advancedState.copy(ikEnabled = v)
                                    "Smudge engine" -> advancedState.copy(smudgeEnabled = v)
                                    "Liquify" -> advancedState.copy(liquifyEnabled = v)
                                    "Particles" -> advancedState.copy(particlesEnabled = v)
                                    "Magic wand" -> advancedState.copy(magicWandEnabled = v)
                                    "Autosave" -> advancedState.copy(autosaveEnabled = v)
                                    "Brush dynamics" -> advancedState.copy(brushDynamicsEnabled = v)
                                    else -> advancedState.copy(multiFrameTransformEnabled = v)
                                }
                            )
                        }
                    )                }
            }

            rows.forEach { (label, enabled) ->
                val checked = enabled
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(PanelBackground).padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, color = White, modifier = Modifier.weight(1f))
                    Switch(
                        checked = checked,
                        onCheckedChange = { newValue ->
                            onVisibilityChange(
                                when (label) {
                                    "Left toolbar" -> visibility.copy(leftToolbar = newValue)
                                    "Top toolbar / widgets" -> visibility.copy(topBar = newValue)
                                    "Timeline" -> visibility.copy(timeline = newValue)
                                    "Reference widget" -> visibility.copy(referenceWidget = newValue)
                                    "Frame tools widget" -> visibility.copy(frameToolsWidget = newValue)
                                    "Audio / recording widget" -> visibility.copy(audioWidget = newValue)
                                    "Advanced animation widget" -> visibility.copy(advancedWidget = newValue)
                                    "Pro tools widget" -> visibility.copy(proToolsWidget = newValue)
                                    "Brush presets widget" -> visibility.copy(brushPresetsWidget = newValue)
                                    else -> visibility.copy(colorWidget = newValue)
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    onionSkinState: OnionSkinState,
    grid: Boolean,
    onToggleGrid: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = { Text("Project Settings", color = White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = PanelBackground)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Onion Skinning", color = White, fontWeight = FontWeight.Medium)
                            Text("Display translucent ghosts of adjacent frames", color = TextSecondary, fontSize = 12.sp)
                        }
                        Switch(
                            checked = onionSkinState.enabled,
                            onCheckedChange = { onionSkinState.enabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = White, checkedTrackColor = PinkAccent)
                        )
                    }

                    if (onionSkinState.enabled) {
                        // Previous Frames
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Previous frames (Past)", color = Color(0xFFEF4444), fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                (0..3).forEach { c ->
                                    Button(
                                        onClick = { onionSkinState.prevFrames = c },
                                        modifier = Modifier.size(width = 36.dp, height = 30.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (onionSkinState.prevFrames == c) Color(0xFFEF4444) else PanelBackground2,
                                            contentColor = White
                                        ),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("$c", fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        // Next Frames
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Next frames (Future)", color = Color(0xFF10B981), fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                (0..3).forEach { c ->
                                    Button(
                                        onClick = { onionSkinState.nextFrames = c },
                                        modifier = Modifier.size(width = 36.dp, height = 30.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (onionSkinState.nextFrames == c) Color(0xFF10B981) else PanelBackground2,
                                            contentColor = White
                                        ),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("$c", fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        // Opacity
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Ghost Opacity", color = White, fontSize = 12.sp)
                                Text("${onionSkinState.opacityPercent}%", color = PinkAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = onionSkinState.opacity,
                                onValueChange = { onionSkinState.opacity = it },
                                valueRange = 0.1f..0.9f,
                                colors = SliderDefaults.colors(thumbColor = PinkAccent, activeTrackColor = PinkAccent)
                            )
                        }
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = PanelBackground)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Alignment Grid", color = White, fontWeight = FontWeight.Medium)
                        Text("Overlay grid on canvas", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(checked = grid, onCheckedChange = { onToggleGrid() })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    project: Project,
    currentIndex: Int,
    isPlaying: Boolean,
    isLooping: Boolean,
    fps: Int,
    copiedFrame: Frame?,
    onSelectFrame: (Int) -> Unit,
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
    onMoveFrame: (Int, Int) -> Unit,
    onBack: () -> Unit
) {
    var showFpsMenu by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Animation Timeline", color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("${project.frames.size} frames • ${fps} FPS", color = TextSecondary, fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White)
                    }
                },
                actions = {
                    Button(
                        onClick = onAddBlankFrame,
                        colors = ButtonDefaults.buttonColors(containerColor = PinkAccent),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Frame", fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Player & Scrubber Deck
            Surface(
                color = PanelBackground,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mini preview screen
                        Box(
                            modifier = Modifier
                                .size(width = 80.dp, height = 50.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White)
                                .border(2.dp, PinkAccent, RoundedCornerShape(6.dp))
                        ) {
                            val activeF = project.frames.getOrNull(currentIndex)
                            if (activeF != null) {
                                FrameThumbnail(
                                    frame = activeF,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        // Playback & FPS controls
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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

                                    IconButton(
                                        onClick = onLoopToggle,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            if (isLooping) Icons.Default.Repeat else Icons.Default.RepeatOne,
                                            contentDescription = "Loop Mode",
                                            tint = if (isLooping) PinkAccent else TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Box {
                                        TextButton(
                                            onClick = { showFpsMenu = true },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("${fps} FPS", color = White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        DropdownMenu(
                                            expanded = showFpsMenu,
                                            onDismissRequest = { showFpsMenu = false }
                                        ) {
                                            listOf(6, 8, 12, 16, 24, 30, 60).forEach { rate ->
                                                DropdownMenuItem(
                                                    text = { Text("$rate FPS", color = if (rate == fps) PinkAccent else Color.Unspecified) },
                                                    onClick = {
                                                        onFpsChange(rate)
                                                        showFpsMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                val currentTimeSec = if (fps > 0) currentIndex.toFloat() / fps else 0f
                                Text(
                                    "Frame ${currentIndex + 1} / ${project.frames.size} (${String.format(java.util.Locale.US, "%.2f", currentTimeSec)}s)",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            // Scrubbing Slider
                            if (project.frames.size > 1) {
                                Slider(
                                    value = currentIndex.toFloat(),
                                    onValueChange = { onSelectFrame(it.toInt().coerceIn(0, project.frames.size - 1)) },
                                    valueRange = 0f..(project.frames.size - 1).toFloat(),
                                    steps = (project.frames.size - 2).coerceAtLeast(0),
                                    colors = SliderDefaults.colors(
                                        thumbColor = PinkAccent,
                                        activeTrackColor = PinkAccent,
                                        inactiveTrackColor = PanelBackground2
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Operation Chips Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimelineActionChip(
                            icon = Icons.Default.Add,
                            label = "+ Blank",
                            onClick = onAddBlankFrame
                        )
                        TimelineActionChip(
                            icon = Icons.Default.ArrowBack,
                            label = "Insert Before",
                            onClick = onInsertBefore
                        )
                        TimelineActionChip(
                            icon = Icons.Default.ArrowForward,
                            label = "Insert After",
                            onClick = onInsertAfter
                        )
                        TimelineActionChip(
                            icon = Icons.Default.ContentCopy,
                            label = "Duplicate",
                            onClick = onDuplicateFrame
                        )
                        TimelineActionChip(
                            icon = Icons.Default.CopyAll,
                            label = "Copy Frame",
                            onClick = onCopyFrame
                        )
                        TimelineActionChip(
                            icon = Icons.Default.ContentPaste,
                            label = "Paste Frame",
                            onClick = onPasteFrame,
                            enabled = copiedFrame != null
                        )
                        if (currentIndex > 0) {
                            TimelineActionChip(
                                icon = Icons.Default.ChevronLeft,
                                label = "Move Left",
                                onClick = { onMoveFrame(currentIndex, currentIndex - 1) }
                            )
                        }
                        if (currentIndex < project.frames.size - 1) {
                            TimelineActionChip(
                                icon = Icons.Default.ChevronRight,
                                label = "Move Right",
                                onClick = { onMoveFrame(currentIndex, currentIndex + 1) }
                            )
                        }
                        TimelineActionChip(
                            icon = Icons.Default.Delete,
                            label = "Delete",
                            onClick = onDeleteFrame,
                            enabled = project.frames.size > 1,
                            tint = if (project.frames.size > 1) Color(0xFFEF4444) else TextSecondary.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            // Multi-track audio timeline
            if (project.audioTracks.isNotEmpty()) {
                Surface(color = PanelBackground, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Audio Tracks", color = White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        project.audioTracks.forEachIndexed { trackIndex, track ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text(track.name, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(72.dp))
                                Box(Modifier.weight(1f).height(30.dp).clip(RoundedCornerShape(5.dp)).background(Color.Black.copy(alpha=.25f))) {
                                    track.clips.forEach { clip ->
                                        val left = (clip.startFrame.toFloat() / project.frames.size.coerceAtLeast(1)).coerceIn(0f, 1f)
                                        val width = (minOf(clip.durationFrames, clip.outFrame + 1).toFloat() / project.frames.size.coerceAtLeast(1)).coerceAtMost(1f)
                                        Box(Modifier.fillMaxHeight().fillMaxWidth(width).offset(x = (left * 1000).dp).padding(vertical=3.dp).clip(RoundedCornerShape(4.dp)).background(PinkAccent.copy(alpha=.55f))) {
                                            Text("Clip", color=White, fontSize=9.sp, modifier=Modifier.padding(horizontal=5.dp))
                                        }
                                    }
                                }
                                Text("${track.clips.size}", color=TextSecondary, fontSize=10.sp, modifier=Modifier.padding(start=6.dp))
                            }
                        }
                    }
                }
            }

            // Filmstrip Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 130.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(project.frames.size) { index ->
                    val frame = project.frames[index]
                    val isSelected = index == currentIndex

                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(PanelBackground)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) PinkAccent else BorderSubtle,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectFrame(index) }
                            .padding(6.dp)
                    ) {
                        // Thumbnail Preview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White)
                        ) {
                            FrameThumbnail(
                                frame = frame,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Frame badge
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (isSelected) PinkAccent else Color.Black.copy(alpha = 0.7f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    "#${index + 1}",
                                    color = White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Frame Card Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (index > 0) {
                                    IconButton(
                                        onClick = { onMoveFrame(index, index - 1) },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ChevronLeft,
                                            contentDescription = "Move Left",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                if (index < project.frames.size - 1) {
                                    IconButton(
                                        onClick = { onMoveFrame(index, index + 1) },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ChevronRight,
                                            contentDescription = "Move Right",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        val clone = frame.deepCopy()
                                        project.frames.add(index + 1, clone)
                                        onSelectFrame(index + 1)
                                    },
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Duplicate",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                if (project.frames.size > 1) {
                                    IconButton(
                                        onClick = {
                                            project.frames.removeAt(index)
                                            onSelectFrame(currentIndex.coerceAtMost(project.frames.size - 1))
                                        },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color = White
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        color = if (enabled) PanelBackground2 else PanelBackground2.copy(alpha = 0.4f),
        modifier = Modifier.height(28.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (enabled) tint else TextSecondary.copy(alpha = 0.3f),
                modifier = Modifier.size(14.dp)
            )
            Text(
                label,
                color = if (enabled) tint else TextSecondary.copy(alpha = 0.3f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayersScreen(
    layers: MutableList<Layer>,
    selectedLayer: Int,
    onSelectLayer: (Int) -> Unit,
    onAddLayer: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = { Text("Layers", color = White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White)
                    }
                },
                actions = {
                    IconButton(onClick = onAddLayer) {
                        Icon(Icons.Default.Add, contentDescription = "Add Layer", tint = PinkAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            layers.forEachIndexed { index, layer ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PanelBackground)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onSelectLayer(index) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Layers, contentDescription = null, tint = PinkAccent)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(layer.name, color = White, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Text(if (layer.clipToBelow) "CLIP" else "", color = PinkAccent, fontSize = 10.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(layer.clipToBelow, { layers[index] = layer.copy(clipToBelow = !layer.clipToBelow) }, label = { Text("Clip") })
                            Button(onClick = {
                                val modes = listOf("NORMAL", "MULTIPLY", "SCREEN", "OVERLAY", "ADD")
                                val next = modes[(modes.indexOf(layer.blendMode) + 1) % modes.size]
                                layers[index] = layer.copy(blendMode = next)
                            }) { Text(layer.blendMode) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdvancedAnimationToolsDialog(project: Project, frameIndex: Int, motionTrailsEnabled: Boolean, trailRadius: Int, waveform: List<Float>, onMotionTrails: (Boolean)->Unit, onTrailRadius:(Int)->Unit, onOpenBatch:()->Unit, onExportSheet:()->Unit, onExportMp4:()->Unit, onDismiss:()->Unit) {
    AlertDialog(onDismissRequest=onDismiss, containerColor=PanelBackground, title={Text("Advanced Animation Tools",color=White,fontWeight=FontWeight.Bold)}, text={
        Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){ Text("Motion trails",color=White); Switch(checked=motionTrailsEnabled,onCheckedChange=onMotionTrails) }
            Text("Trail frames: $trailRadius",color=TextSecondary,fontSize=12.sp)
            Slider(value=trailRadius.toFloat(),onValueChange={onTrailRadius(it.roundToInt().coerceIn(1,6))},valueRange=1f..6f,steps=4)
            HorizontalDivider()
            Button(onClick=onOpenBatch,modifier=Modifier.fillMaxWidth()){Text("Multi-frame operations")}
            OutlinedButton(onClick=onExportSheet,modifier=Modifier.fillMaxWidth()){Text("Export Sprite Sheet")}
            Button(onClick=onExportMp4,modifier=Modifier.fillMaxWidth()){Text(if(project.audioClips.isNotEmpty()) "Export MP4 + Audio" else "Export MP4")}
            Text("Current frame: ${frameIndex+1}/${project.frames.size}",color=TextSecondary,fontSize=11.sp)
            if(waveform.isNotEmpty()){ Text("Audio waveform",color=White,fontSize=12.sp); Canvas(Modifier.fillMaxWidth().height(50.dp)){ val step=size.width/waveform.size.coerceAtLeast(1); waveform.forEachIndexed{ i,v -> drawLine(PinkAccent,Offset(i*step,size.height/2-v*size.height/2),Offset(i*step,size.height/2+v*size.height/2),strokeWidth=2f) } } }
        }
    },confirmButton={TextButton(onClick=onDismiss){Text("Done",color=PinkAccent)}})
}

@Composable
private fun BatchFrameDialog(
    project: Project,
    frameIndex: Int,
    onDismiss: () -> Unit,
    onBatchChanged: () -> Unit
) {
    var start by remember { mutableIntStateOf(frameIndex.coerceAtLeast(0)) }
    var end by remember { mutableIntStateOf(frameIndex.coerceAtLeast(0)) }
    var target by remember { mutableIntStateOf((frameIndex + 1).coerceAtMost(project.frames.size)) }
    var duration by remember { mutableIntStateOf(project.frames.getOrNull(frameIndex)?.durationFrames ?: 1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PanelBackground,
        title = { Text("Multi-frame Operations", color = White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select a frame range, then duplicate, delete or move it.", color = TextSecondary)
                Text("Start: ${start + 1}", color = White)
                Slider(
                    value = start.toFloat(),
                    onValueChange = {
                        start = it.roundToInt().coerceIn(0, (project.frames.size - 1).coerceAtLeast(0))
                        if (end < start) end = start
                    },
                    valueRange = 0f..(project.frames.size - 1).coerceAtLeast(0).toFloat()
                )
                Text("End: ${end + 1}", color = White)
                Slider(
                    value = end.toFloat(),
                    onValueChange = {
                        end = it.roundToInt().coerceIn(start, (project.frames.size - 1).coerceAtLeast(start))
                    },
                    valueRange = start.toFloat()..(project.frames.size - 1).coerceAtLeast(start).toFloat()
                )
                Text("Move target: ${target + 1}", color = White)
                Slider(
                    value = target.toFloat(),
                    onValueChange = {
                        target = it.roundToInt().coerceIn(0, project.frames.size)
                    },
                    valueRange = 0f..project.frames.size.toFloat()
                )
                HorizontalDivider()
                Text("Current frame hold: $duration", color = White)
                Slider(
                    value = duration.toFloat(),
                    onValueChange = { duration = it.roundToInt().coerceIn(1, 12) },
                    valueRange = 1f..12f,
                    steps = 10
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            BatchFrameOperations.duplicate(project.frames, (start..end).toSet())
                            onBatchChanged()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Duplicate") }

                    OutlinedButton(
                        onClick = {
                            BatchFrameOperations.move(project.frames, start, end, target)
                            onBatchChanged()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Move") }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            BatchFrameOperations.delete(project.frames, (start..end).toSet())
                            onBatchChanged()
                            start = 0
                            end = 0
                        },
                        modifier = Modifier.weight(1f),
                        enabled = project.frames.size > 1
                    ) { Text("Delete Range") }

                    OutlinedButton(
                        onClick = {
                            project.frames.getOrNull(frameIndex)?.durationFrames = duration
                            onBatchChanged()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Apply Hold") }
                }
                Text(
                    "${project.frames.size} frames • range ${start + 1}-${end + 1}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", color = PinkAccent) }
        }
    )
}

@Composable
private fun ProToolsDialog(
    project: Project, currentFrame: Int, chromaTolerance: Int, onChromaTolerance: (Int)->Unit,
    onImportVideo: ()->Unit, onApplyChroma: ()->Unit, onBatchDuplicate: ()->Unit,
    onBatchDelete: ()->Unit, onSmudge: ()->Unit, onDismiss: ()->Unit
) {
    AlertDialog(onDismissRequest=onDismiss, containerColor=PanelBackground,
        title={Text("Pro Animation Tools", color=White, fontWeight=FontWeight.Bold)},
        text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("Frame ${currentFrame+1} • ${project.frames.size} frames", color=TextSecondary)
            Button(onClick=onImportVideo, modifier=Modifier.fillMaxWidth()){ Text("Import Video for Rotoscoping") }
            Text("Chroma-key tolerance: $chromaTolerance", color=White, fontSize=12.sp)
            Slider(value=chromaTolerance.toFloat(), onValueChange={onChromaTolerance(it.roundToInt())}, valueRange=5f..120f)
            OutlinedButton(onClick=onApplyChroma, modifier=Modifier.fillMaxWidth()){ Text("Remove Green Background") }
            OutlinedButton(onClick=onSmudge, modifier=Modifier.fillMaxWidth()){ Text("Apply Smudge to Reference") }
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick=onBatchDuplicate, modifier=Modifier.weight(1f)){ Text("Duplicate Frame") }
                OutlinedButton(onClick=onBatchDelete, modifier=Modifier.weight(1f)){ Text("Delete Frame") }
            }
            Text("Inspired by MIT/Apache open-source editor workflows; implementations are native Kotlin.", color=TextSecondary, fontSize=10.sp)
        }},
        confirmButton={TextButton(onClick=onDismiss){Text("Done", color=PinkAccent)}})
}

@Composable
private fun AudioToolsDialog(project: Project,currentFrame:Int,isRecording:Boolean,waveform:List<Float>,onStartRecording:()->Unit,onStopRecording:()->Unit,onPreview:()->Unit,onStopPreview:()->Unit,onDismiss:()->Unit){
    val clip = project.audioClips.firstOrNull()
    var inFrame by remember(clip?.id) { mutableIntStateOf(clip?.inFrame ?: 0) }
    var outFrame by remember(clip?.id) { mutableIntStateOf(clip?.outFrame?.takeIf { it != Int.MAX_VALUE } ?: maxOf(1, clip?.durationFrames ?: 1)) }
    var fadeIn by remember(clip?.id) { mutableIntStateOf(clip?.fadeInFrames ?: 0) }
    var fadeOut by remember(clip?.id) { mutableIntStateOf(clip?.fadeOutFrames ?: 0) }
    AlertDialog(onDismissRequest=onDismiss,containerColor=PanelBackground,title={Text("Audio Track",color=White,fontWeight=FontWeight.Bold)},text={
        Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
            Text(if(project.audioPath==null) "No audio clip attached" else "Clip attached",color=TextSecondary)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ Button(onClick=if(isRecording) onStopRecording else onStartRecording){Text(if(isRecording) "Stop" else "Record")} OutlinedButton(onClick=onPreview,enabled=project.audioPath!=null){Text("Preview")} OutlinedButton(onClick=onStopPreview){Text("Stop")}}
            Text("Starts at frame ${currentFrame+1}",color=TextSecondary,fontSize=12.sp)
            if(waveform.isNotEmpty()){Canvas(Modifier.fillMaxWidth().height(60.dp)){val step=size.width/waveform.size.coerceAtLeast(1);waveform.forEachIndexed{i,v->drawLine(PinkAccent,Offset(i*step,size.height/2-v*size.height/2),Offset(i*step,size.height/2+v*size.height/2),2f)}}}
            if(clip != null) {
                Text("Trim in: $inFrame • out: $outFrame", color=White, fontSize=12.sp)
                Slider(value=inFrame.toFloat(), onValueChange={inFrame=it.roundToInt().coerceIn(0, maxOf(0,outFrame-1))}, valueRange=0f..maxOf(1,clip.durationFrames-1).toFloat())
                Slider(value=outFrame.toFloat(), onValueChange={outFrame=it.roundToInt().coerceIn(inFrame+1, maxOf(inFrame+1,clip.durationFrames))}, valueRange=1f..maxOf(1,clip.durationFrames).toFloat())
                Text("Fade in: $fadeIn frames",color=TextSecondary,fontSize=12.sp)
                Slider(value=fadeIn.toFloat(),onValueChange={fadeIn=it.roundToInt().coerceIn(0,30)},valueRange=0f..30f)
                Text("Fade out: $fadeOut frames",color=TextSecondary,fontSize=12.sp)
                Slider(value=fadeOut.toFloat(),onValueChange={fadeOut=it.roundToInt().coerceIn(0,30)},valueRange=0f..30f)
                Text("At current frame gain: ${"%.2f".format(AudioTimelineEngine.volumeAt(clip,currentFrame))}",color=TextSecondary,fontSize=11.sp)
            }
        }
    },confirmButton={
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            TextButton(onClick=onDismiss){Text("Cancel",color=TextSecondary)}
            Button(onClick={
                if(project.audioClips.isNotEmpty()) { val old=project.audioClips[0]; project.audioClips[0]=old.copy(inFrame=inFrame,outFrame=outFrame,fadeInFrames=fadeIn,fadeOutFrames=fadeOut) }
                onDismiss()
            }){Text("Apply")}
        }
    })
}