// Ref: 02/06//2026 17:58
package com.example.teleprompter

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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
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
    private val _windowAlpha = mutableFloatStateOf(1.0f)
    private val _textColor = mutableStateOf(Color.White)
    private val _isPlaying = mutableStateOf(false)
    private val _scriptText = mutableStateOf("Loading script...")

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringExtra("SCRIPT_TEXT")?.let {
            if (it.isNotBlank()) _scriptText.value = it
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
        createNotificationChannel()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        setupPrompterWindow()
        setupSettingsWindow()
        windowManager.addView(prompterView, prompterParams)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel("prompter_channel", "Prompter Service", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun setupPrompterWindow() {
        prompterParams = WindowManager.LayoutParams(
            800, 600,
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
                    isPlaying = _isPlaying.value,
                    onTogglePlay = { _isPlaying.value = !_isPlaying.value },
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
        settingsParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        settingsView = ComposeView(this).apply {
            val lifecycleOwner = MyLifecycleOwner()
            lifecycleOwner.performRestore(null)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            this.setViewTreeLifecycleOwner(lifecycleOwner)
            this.setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    SettingsPanelContent(
                        currentFontSize = _fontSize.floatValue,
                        currentSpeed = _scrollSpeed.floatValue,
                        currentAlpha = _windowAlpha.floatValue,
                        currentColor = _textColor.value,
                        onFontSizeChange = { _fontSize.floatValue = it },
                        onSpeedChange = { _scrollSpeed.floatValue = it },
                        onAlphaChange = { _windowAlpha.floatValue = it },
                        onColorChange = { _textColor.value = it },
                        onClose = { toggleSettingsWindow() },
                        onDrag = { x, y -> moveWindow(settingsParams, settingsView, x, y) }
                    )
                }
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
        super.onDestroy()
        if (::prompterView.isInitialized) windowManager.removeView(prompterView)
        if (isSettingsOpen && ::settingsView.isInitialized) windowManager.removeView(settingsView)
    }
}

@Composable
fun PrompterWindowContent(
    script: String, fontSize: Float, scrollSpeed: Float, windowAlpha: Float, textColor: Color,
    isPlaying: Boolean, onTogglePlay: () -> Unit, onClose: () -> Unit, onOpenSettings: () -> Unit,
    onDrag: (Float, Float) -> Unit, onResize: (Float, Float) -> Unit, onInteractionState: (Boolean) -> Unit
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(isPlaying, scrollSpeed) {
        if (isPlaying) {
            while (true) {
                scrollState.scrollBy(scrollSpeed)
                delay(16)
            }
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
                Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp)) {
                    Text(text = script, color = textColor, fontSize = fontSize.sp, lineHeight = (fontSize * 1.4).sp, modifier = Modifier.verticalScroll(scrollState))
                }
            }
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
        }
    }
}

@Composable
fun SettingsPanelContent(
    currentFontSize: Float,
    currentSpeed: Float,
    currentAlpha: Float,
    currentColor: Color,
    onFontSizeChange: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onAlphaChange: (Float) -> Unit,
    onColorChange: (Color) -> Unit,
    onClose: () -> Unit,
    onDrag: (Float, Float) -> Unit
) {
    val presetColors = listOf(Color.White, Color(0xFFFFEB3B), Color(0xFF00E676), Color(0xFF00E5FF), Color(0xFFEA80FC))
    val firstRowColors = presetColors.take(3)
    val secondRowColors = presetColors.drop(3)

    val configuration = LocalConfiguration.current
    val cardWidth = min(configuration.screenWidthDp * 0.9f, 420f).dp

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        elevation = CardDefaults.cardElevation(16.dp),
        modifier = Modifier.width(cardWidth)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF333333))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount.x, dragAmount.y)
                        }
                    }.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Settings", color = Color.White, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.Close, "Close", tint = Color.Gray, modifier = Modifier.clickable { onClose() })
            }

            Column(modifier = Modifier.padding(24.dp)) {
                Text("Text Color", color = Color.LightGray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    firstRowColors.forEach { color ->
                        ColorBubble(color, currentColor == color) { onColorChange(color) }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    secondRowColors.forEach { color ->
                        ColorBubble(color, currentColor == color) { onColorChange(color) }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(thickness = 6.dp, color = Color(0xFF333333))
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().height(500.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    VerticalControlItem("Font", currentFontSize.toInt().toString(), currentFontSize, onFontSizeChange, 12f..72f, Color(0xFF00C853))
                    VerticalControlItem("Opacity", "${(currentAlpha * 100).toInt()}%", currentAlpha, onAlphaChange, 0.1f..1.0f, Color(0xFF2979FF))
                    VerticalControlItem("Speed", currentSpeed.toInt().toString(), currentSpeed, onSpeedChange, 1f..10f, Color(0xFFFFAB00))
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowScope.VerticalControlItem(
    label: String, valueText: String, value: Float, onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>, color: Color
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxHeight().weight(1f)) {
        Text(label, color = Color.LightGray, fontSize = 12.sp)
        Text(valueText, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)

        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                interactionSource = interactionSource,
                modifier = Modifier
                    .graphicsLayer { rotationZ = -90f }
                    .requiredWidth(450.dp),
                colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color),
                thumb = {
                    Box(modifier = Modifier.size(width = 12.dp, height = 50.dp).background(color, RoundedCornerShape(2.dp)))
                }
            )
        }
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