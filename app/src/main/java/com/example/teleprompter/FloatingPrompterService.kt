// Ref: 02/07/2026 13:15
package com.example.teleprompter

import android.media.AudioManager
import android.media.ToneGenerator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.min

class FloatingPrompterService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var prompterView: ComposeView
    private lateinit var prompterParams: WindowManager.LayoutParams
    private lateinit var settingsView: ComposeView
    private lateinit var settingsParams: WindowManager.LayoutParams
    private var isSettingsOpen = false

    private val _fontSize = mutableFloatStateOf(32f)
    private val _scrollSpeed = mutableFloatStateOf(2f)
    private val _windowAlpha = mutableFloatStateOf(0.75f)
    private val _textColor = mutableStateOf(Color.White)
    private val _isMirrored = mutableStateOf(false)
    private val _isLoop = mutableStateOf(false)
    private val _countdownSeconds = mutableIntStateOf(0)
    private val _isPlaying = mutableStateOf(false)
    private val _scriptText = mutableStateOf("Loading script...")
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringExtra("SCRIPT_TEXT")?.let {
            if (it.isNotBlank()) {
                _scriptText.value = it
                serviceScope.launch {
                    _isPlaying.value = false
                    delay(50)
                    _isPlaying.value = true
                }
            }
        }

        val notification: Notification = NotificationCompat.Builder(this, "prompter_channel")
            .setContentTitle("Teleprompter Running")
            .setContentText("Settings Active")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        startForeground(1, notification)

        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        loadSettings()
        createNotificationChannel()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        setupPrompterWindow()
        windowManager.addView(prompterView, prompterParams)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel("prompter_channel", "Prompter Service", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun saveSettings() {
        val prefs = getSharedPreferences("prompter_prefs", MODE_PRIVATE)
        with(prefs.edit()) {
            putFloat("font_size", _fontSize.floatValue)
            putFloat("scroll_speed", _scrollSpeed.floatValue)
            putFloat("window_alpha", _windowAlpha.floatValue)
            putInt("text_color", _textColor.value.toArgb())
            putBoolean("is_mirrored", _isMirrored.value)
            putBoolean("is_loop", _isLoop.value)
            putInt("countdown_seconds", _countdownSeconds.intValue)
            apply()
        }
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("prompter_prefs", MODE_PRIVATE)
        _fontSize.floatValue = prefs.getFloat("font_size", 32f)
        _scrollSpeed.floatValue = prefs.getFloat("scroll_speed", 2f)
        _windowAlpha.floatValue = prefs.getFloat("window_alpha", 0.75f)
        val colorArgb = prefs.getInt("text_color", Color.White.toArgb())
        _textColor.value = Color(colorArgb)
        _isMirrored.value = prefs.getBoolean("is_mirrored", false)
        _isLoop.value = prefs.getBoolean("is_loop", false)
        _countdownSeconds.intValue = prefs.getInt("countdown_seconds", 0)
    }

    private fun setupPrompterWindow() {
        val displayMetrics = resources.displayMetrics
        val initialWidth = (displayMetrics.widthPixels * 0.85f).toInt()
        val initialHeight = (displayMetrics.heightPixels * 0.85f).toInt()

        prompterParams = WindowManager.LayoutParams(
            initialWidth, initialHeight,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 50
        }

        prompterView = ComposeView(this).apply {
            val lifecycleOwner = MyLifecycleOwner()
            lifecycleOwner.performRestore(null)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            this.setViewTreeLifecycleOwner(lifecycleOwner)
            this.setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                PrompterWindowContent(
                    script = _scriptText.value,
                    fontSize = _fontSize.floatValue,
                    scrollSpeed = _scrollSpeed.floatValue,
                    windowAlpha = _windowAlpha.floatValue,
                    textColor = _textColor.value,
                    isMirrored = _isMirrored.value,
                    isLoop = _isLoop.value,
                    countdownSeconds = _countdownSeconds.intValue,
                    isPlaying = _isPlaying.value,
                    onTogglePlay = { _isPlaying.value = !_isPlaying.value },
                    onScrollEnd = { _isPlaying.value = false },
                    onClose = {
                        val intent = Intent(this@FloatingPrompterService, MainActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        stopSelf()
                    },
                    onOpenSettings = { toggleSettingsWindow() },
                    onDrag = { x, y -> moveWindow(prompterParams, prompterView, x, y) },
                    onResize = { w, h -> resizeWindow(prompterParams, prompterView, w, h) },
                    onInteractionState = { isInteracting ->
                        val targetWindowAlpha = if (isInteracting) 0.5f else 1.0f
                        updateWindowAlpha(targetWindowAlpha)
                    }
                )
            }
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
    }

    private fun setupSettingsWindow() {
        val displayMetrics = resources.displayMetrics
        settingsParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            (displayMetrics.heightPixels * 0.55f).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            0,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
        }

        settingsView = ComposeView(this).apply {
            val lifecycleOwner = MyLifecycleOwner()
            lifecycleOwner.performRestore(null)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            this.setViewTreeLifecycleOwner(lifecycleOwner)
            this.setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                val savedFontSize = remember { _fontSize.floatValue }
                val savedSpeed = remember { _scrollSpeed.floatValue }
                val savedAlpha = remember { _windowAlpha.floatValue }
                val savedColor = remember { _textColor.value }
                val savedMirror = remember { _isMirrored.value }
                val savedLoop = remember { _isLoop.value }
                val savedCountdown = remember { _countdownSeconds.intValue }

                SettingsPanelContent(
                    currentFontSize = _fontSize.floatValue,
                    currentSpeed = _scrollSpeed.floatValue,
                    currentAlpha = _windowAlpha.floatValue,
                    currentColor = _textColor.value,
                    isMirrored = _isMirrored.value,
                    isLoop = _isLoop.value,
                    currentCountdown = _countdownSeconds.intValue,
                    onFontSizeChange = { _fontSize.floatValue = it },
                    onSpeedChange = { _scrollSpeed.floatValue = it },
                    onAlphaChange = { _windowAlpha.floatValue = it },
                    onColorChange = { _textColor.value = it },
                    onMirrorChange = { _isMirrored.value = it },
                    onLoopChange = { _isLoop.value = it },
                    onCountdownChange = { _countdownSeconds.intValue = it },
                    onSave = { saveSettings(); toggleSettingsWindow() },
                    onCancel = {
                        _fontSize.floatValue = savedFontSize
                        _scrollSpeed.floatValue = savedSpeed
                        _windowAlpha.floatValue = savedAlpha
                        _textColor.value = savedColor
                        _isMirrored.value = savedMirror
                        _isLoop.value = savedLoop
                        _countdownSeconds.intValue = savedCountdown
                        toggleSettingsWindow()
                    }
                )
            }
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
    }

    private fun toggleSettingsWindow() {
        if (isSettingsOpen) {
            windowManager.removeView(settingsView)
            isSettingsOpen = false
        } else {
            setupSettingsWindow()
            windowManager.addView(settingsView, settingsParams)
            isSettingsOpen = true
        }
    }

    private fun moveWindow(params: WindowManager.LayoutParams, view: View, deltaX: Float, deltaY: Float) {
        params.x += deltaX.toInt()
        params.y += deltaY.toInt()
        windowManager.updateViewLayout(view, params)
    }

    private fun resizeWindow(params: WindowManager.LayoutParams, view: View, deltaW: Float, deltaH: Float) {
        params.width = (params.width + deltaW.toInt()).coerceAtLeast(300)
        params.height = (params.height + deltaH.toInt()).coerceAtLeast(200)
        windowManager.updateViewLayout(view, params)
    }

    private fun updateWindowAlpha(alpha: Float) {
        prompterParams.alpha = alpha
        if (prompterView.isAttachedToWindow) windowManager.updateViewLayout(prompterView, prompterParams)
    }

    override fun onDestroy() {
        saveSettings()
        serviceScope.cancel()
        super.onDestroy()
        if (::prompterView.isInitialized) windowManager.removeView(prompterView)
        if (isSettingsOpen && ::settingsView.isInitialized) windowManager.removeView(settingsView)
    }
}

@Composable
fun PrompterWindowContent(
    script: String, fontSize: Float, scrollSpeed: Float, windowAlpha: Float, textColor: Color,
    isMirrored: Boolean, isLoop: Boolean, countdownSeconds: Int,
    isPlaying: Boolean, onTogglePlay: () -> Unit, onScrollEnd: () -> Unit, onClose: () -> Unit, onOpenSettings: () -> Unit,
    onDrag: (Float, Float) -> Unit, onResize: (Float, Float) -> Unit, onInteractionState: (Boolean) -> Unit
) {
    val scrollState = rememberScrollState()
    var unitHeightPx by remember { mutableIntStateOf(0) }

    val currentScrollSpeed by rememberUpdatedState(scrollSpeed)
    val currentIsLoop by rememberUpdatedState(isLoop)
    val currentUnitHeightPx by rememberUpdatedState(unitHeightPx)
    val currentOnScrollEnd by rememberUpdatedState(onScrollEnd)

    var isCountingDown by remember { mutableStateOf(false) }
    var countdownValue by remember { mutableIntStateOf(0) }
    var showAction by remember { mutableStateOf(false) }
    var flashOn by remember { mutableStateOf(true) }
    var sweepAngle by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(scrollState) {
        scrollState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) {
                onScrollEnd()
            }
        }
    }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect

        if (countdownSeconds > 0) {
            isCountingDown = true
            val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
            try {
                for (sec in countdownSeconds downTo 1) {
                    countdownValue = sec
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
                    val tickStartMs = System.currentTimeMillis()
                    while (System.currentTimeMillis() - tickStartMs < 1000) {
                        val elapsed = System.currentTimeMillis() - tickStartMs
                        sweepAngle = (elapsed / 1000f) * 360f
                        flashOn = elapsed < 200
                        delay(16)
                    }
                }
                sweepAngle = 0f
                showAction = true
                toneGenerator.startTone(ToneGenerator.TONE_DTMF_D, 300)
                delay(1000)
            } finally {
                toneGenerator.release()
                showAction = false
                isCountingDown = false
            }
        }

        while (true) {
            if (currentIsLoop) {
                if (currentUnitHeightPx > 0 && scrollState.value >= currentUnitHeightPx) {
                    scrollState.scrollTo(scrollState.value - currentUnitHeightPx)
                } else {
                    scrollState.scrollBy(currentScrollSpeed)
                }
            } else {
                if (scrollState.value >= scrollState.maxValue) {
                    currentOnScrollEnd()
                    break
                } else {
                    scrollState.scrollBy(currentScrollSpeed)
                }
            }
            delay(16)
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212).copy(alpha = windowAlpha)),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header (Never Mirrored)
                Row(
                    modifier = Modifier.fillMaxWidth().height(44.dp).background(Color(0xFF2C2C2C))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { onInteractionState(true) },
                                onDragEnd = { onInteractionState(false) },
                                onDragCancel = { onInteractionState(false) }
                            ) { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.x, dragAmount.y)
                            }
                        }.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Teleprompter", color = Color.LightGray, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Settings, "Settings", tint = Color.LightGray, modifier = Modifier.size(20.dp).clickable { onOpenSettings() })
                        Box(modifier = Modifier.size(24.dp).clickable { onTogglePlay() }, contentAlignment = Alignment.Center) {
                            if (isPlaying) {
                                Canvas(modifier = Modifier.matchParentSize()) {
                                    val barW = size.width * 0.35f
                                    drawRect(Color.White, topLeft = Offset(0f, 0f), size = Size(barW, size.height))
                                    drawRect(Color.White, topLeft = Offset(size.width - barW, 0f), size = Size(barW, size.height))
                                }
                            } else Icon(Icons.Default.PlayArrow, "Play", tint = Color.White, modifier = Modifier.matchParentSize())
                        }
                        Icon(Icons.Default.Close, "Close", tint = Color(0xFFFF5252), modifier = Modifier.size(24.dp).clickable { onClose() })
                    }
                }
                // Script Area (Mirrored if enabled)
                Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp)) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(scrollState)
                            .graphicsLayer {
                                if (isMirrored) {
                                    scaleX = -1f // Flip horizontally
                                }
                            }
                    ) {
                        ScriptUnit(
                            script = script,
                            fontSize = fontSize,
                            textColor = textColor,
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                unitHeightPx = coordinates.size.height
                            }
                        )
                        if (isLoop) {
                            ScriptUnit(script = script, fontSize = fontSize, textColor = textColor)
                        }
                    }
                }
            }
            // Resize Handle (Never Mirrored)
            Box(modifier = Modifier.align(Alignment.BottomEnd).size(40.dp).pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onInteractionState(true) },
                    onDragEnd = { onInteractionState(false) },
                    onDragCancel = { onInteractionState(false) }
                ) { change, dragAmount ->
                    change.consume()
                    onResize(dragAmount.x, dragAmount.y)
                }
            }) {
                Canvas(modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).size(20.dp)) {
                    drawArc(Color.White.copy(alpha = 0.5f), 0f, 90f, false, style = Stroke(width = 4.dp.toPx()))
                }
            }
            if (isCountingDown) {
                CountdownOverlay(
                    countdownValue = countdownValue,
                    sweepAngle = sweepAngle,
                    flashOn = flashOn,
                    showAction = showAction,
                    onCancel = currentOnScrollEnd,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun ScriptUnit(
    script: String,
    fontSize: Float,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = script,
            color = textColor,
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.4).sp
        )
        Spacer(modifier = Modifier.height((fontSize * 4).dp))
        Text(
            text = "END",
            color = Color.Red,
            fontSize = (fontSize * 1.8).sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height((fontSize * 4).dp))
    }
}

@Composable
private fun rememberAutoFitFontSize(
    text: String,
    maxWidthPx: Float,
    maxHeightPx: Float,
    startSizeSp: Float,
    fontFamily: FontFamily
): TextUnit {
    val textMeasurer = rememberTextMeasurer()
    return remember(text, maxWidthPx, maxHeightPx, startSizeSp) {
        var size = startSizeSp
        while (size > 8f) {
            val result = textMeasurer.measure(
                text = text,
                style = TextStyle(fontSize = size.sp, fontWeight = FontWeight.Bold, fontFamily = fontFamily)
            )
            if (result.size.width <= maxWidthPx && result.size.height <= maxHeightPx) break
            size -= 2f
        }
        size.sp
    }
}

@Composable
fun CountdownOverlay(
    countdownValue: Int,
    sweepAngle: Float,
    flashOn: Boolean,
    showAction: Boolean,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.88f))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    onCancel()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (showAction) {
            val actionWidthPx = with(density) { (maxWidth * 0.9f).toPx() }
            val actionHeightPx = with(density) { (maxHeight * 0.5f).toPx() }
            val actionFontSize = rememberAutoFitFontSize("ACTION", actionWidthPx, actionHeightPx, 400f, FontFamily.Serif)
            Text(
                text = "ACTION",
                color = Color.Red,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = actionFontSize,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            val ringDiameter = (if (maxWidth < maxHeight) maxWidth else maxHeight) * 0.7f
            Canvas(modifier = Modifier.size(ringDiameter)) {
                val radius = size.minDimension / 2f
                val ringCenter = center
                drawCircle(Color.White.copy(alpha = 0.6f), radius = radius, center = ringCenter, style = Stroke(width = 3.dp.toPx()))
                drawCircle(Color.White.copy(alpha = 0.6f), radius = radius * 0.6f, center = ringCenter, style = Stroke(width = 3.dp.toPx()))
                drawLine(Color.White.copy(alpha = 0.6f), Offset(ringCenter.x, ringCenter.y - radius), Offset(ringCenter.x, ringCenter.y + radius), strokeWidth = 2.dp.toPx())
                drawLine(Color.White.copy(alpha = 0.6f), Offset(ringCenter.x - radius, ringCenter.y), Offset(ringCenter.x + radius, ringCenter.y), strokeWidth = 2.dp.toPx())
                rotate(degrees = sweepAngle, pivot = ringCenter) {
                    drawLine(Color.Red, ringCenter, Offset(ringCenter.x, ringCenter.y - radius), strokeWidth = 4.dp.toPx())
                }
            }
            val innerDiameterPx = with(density) { (ringDiameter * 0.6f).toPx() }
            val numberFontSize = rememberAutoFitFontSize(countdownValue.toString(), innerDiameterPx, innerDiameterPx, 260f, FontFamily.Serif)
            Text(
                text = countdownValue.toString(),
                color = if (flashOn) Color.White else Color.White.copy(alpha = 0.35f),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = numberFontSize,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SettingsPanelContent(
    currentFontSize: Float,
    currentSpeed: Float,
    currentAlpha: Float,
    currentColor: Color,
    isMirrored: Boolean,
    isLoop: Boolean,
    currentCountdown: Int,
    onFontSizeChange: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onAlphaChange: (Float) -> Unit,
    onColorChange: (Color) -> Unit,
    onMirrorChange: (Boolean) -> Unit,
    onLoopChange: (Boolean) -> Unit,
    onCountdownChange: (Int) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val presetColors = listOf(Color.White, Color(0xFFFFEB3B), Color(0xFF00E676), Color(0xFF00E5FF), Color(0xFFEA80FC))

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(width = 40.dp, height = 4.dp).background(Color(0xFF555555), RoundedCornerShape(2.dp)))
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = Color.LightGray, fontSize = 15.sp)
                }
                Text("Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                TextButton(
                    onClick = onSave,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF00C853))
                ) {
                    Text("Save", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(color = Color(0xFF333333))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                HorizontalSliderRow("Font", currentFontSize, 12f..72f, currentFontSize.toInt().toString(), Color(0xFF00C853), onFontSizeChange)
                HorizontalSliderRow("Speed", currentSpeed, 1f..10f, currentSpeed.toInt().toString(), Color(0xFFFFAB00), onSpeedChange)
                HorizontalSliderRow("Opacity", currentAlpha, 0.1f..1.0f, "${(currentAlpha * 100).toInt()}%", Color(0xFF2979FF), onAlphaChange)

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color(0xFF333333))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Mirror", color = Color.LightGray, fontSize = 12.sp)
                        Switch(checked = isMirrored, onCheckedChange = onMirrorChange)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Loop", color = Color.LightGray, fontSize = 12.sp)
                        Switch(checked = isLoop, onCheckedChange = onLoopChange)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Countdown", color = Color.LightGray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        CountdownDropdown(currentCountdown, onCountdownChange)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color(0xFF333333))
                Spacer(modifier = Modifier.height(12.dp))

                Text("Text Color", color = Color.LightGray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    presetColors.forEach { color ->
                        ColorBubble(color, currentColor == color) { onColorChange(color) }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun CountdownDropdown(current: Int, onChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(0, 3, 5, 10)
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Color(0xFF2C2C2C), RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = if (current == 0) "Off" else "${current}s",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("▾", color = Color.LightGray, fontSize = 14.sp)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(if (option == 0) "Off" else "$option seconds") },
                    onClick = {
                        onChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ColorBubble(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(40.dp).background(color, CircleShape)
            .border(if (isSelected) 3.dp else 0.dp, if (isSelected) Color.White else Color.Transparent, CircleShape)
            .clickable { onClick() }
    )
}

@Composable
fun HorizontalSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    color: Color,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.LightGray, fontSize = 13.sp, modifier = Modifier.width(64.dp))
        Text(valueLabel, color = Color.White, fontSize = 13.sp, textAlign = TextAlign.End, modifier = Modifier.width(40.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
            colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color)
        )
    }
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.LightGray, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

class MyLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    fun handleLifecycleEvent(event: Lifecycle.Event) = lifecycleRegistry.handleLifecycleEvent(event)
    fun performRestore(savedState: android.os.Bundle?) = savedStateRegistryController.performRestore(savedState)
}