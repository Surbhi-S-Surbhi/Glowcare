package com.glowcare.app.ui.camera

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.glowcare.app.R
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * Beauty camera: full-screen CameraX preview, filter carousel, beauty sliders,
 * photo capture with timer, gallery import, and real video recording via
 * VideoCapture<Recorder>. All processing happens on-device only.
 */
@Composable
fun CameraScreen(
    onOpenEditor: (Uri) -> Unit,
    onOpenVideoPreview: (Uri) -> Unit,
    onClose: () -> Unit,
    premium: Boolean = false,
    onUpgrade: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasPermission = it }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Camera state
    var frontCamera by remember { mutableStateOf(true) }
    var torchOn by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableIntStateOf(0) }
    var countdown by remember { mutableIntStateOf(0) }
    var capturing by remember { mutableStateOf(false) }
    var showBeautyPanel by remember { mutableStateOf(false) }
    var beauty by remember { mutableStateOf(FilterEngine.BeautySettings()) }
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    var filterIntensity by remember { mutableStateOf(0.5f) }
    var showFilters by remember { mutableStateOf(true) }

    // Photo / Video mode + recording state
    var videoMode by remember { mutableStateOf(false) }
    var micGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        micGranted = it
        if (!it) {
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.mic_denied_toast),
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }
    var isRecording by remember { mutableStateOf(false) }
    var recordingStarted by remember { mutableStateOf(false) }
    var stopRequested by remember { mutableStateOf(false) }
    var recordSeconds by remember { mutableIntStateOf(0) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var videoError by remember { mutableStateOf<String?>(null) }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    val recorder = remember {
        Recorder.Builder()
            .setQualitySelector(
                QualitySelector.from(Quality.HD, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD))
            )
            .build()
    }
    val videoCapture = remember { VideoCapture.withOutput(recorder) }
    val scope = rememberCoroutineScope()

    // Recording timer
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordSeconds = 0
            while (isRecording) {
                delay(1000)
                recordSeconds++
            }
        }
    }

    // Stop the recording safely if the user leaves the screen mid-record.
    DisposableEffect(Unit) {
        onDispose {
            runCatching { activeRecording?.stop() }
            activeRecording = null
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        CameraEditHandoff.filterIndex = 0
        CameraEditHandoff.filterIntensity = 0f
        CameraEditHandoff.beauty = FilterEngine.BeautySettings()
        uri?.let(onOpenEditor)
    }

    fun startRecording() {
        if (isRecording) return
        // Ask for the microphone the first time it's needed; recording proceeds
        // without audio if denied.
        if (!micGranted) {
            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        videoError = null
        recordingStarted = false
        stopRequested = false

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "GlowCare_${System.currentTimeMillis()}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/GlowCare")
        }
        val outputOptions = MediaStoreOutputOptions.Builder(
            context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(values).build()

        val pending = recorder.prepareRecording(context, outputOptions)
        if (micGranted) pending.withAudioEnabled()

        activeRecording = pending.start(
            ContextCompat.getMainExecutor(context)
        ) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    recordingStarted = true
                    // A very quick stop tap (before Start arrived) is honored now.
                    if (stopRequested) {
                        runCatching { activeRecording?.stop() }
                    }
                }
                is VideoRecordEvent.Finalize -> {
                    isRecording = false
                    activeRecording = null
                    if (!event.hasError()) {
                        onOpenVideoPreview(event.outputResults.outputUri)
                    } else {
                        // Clean up the partial file; recording may fail if another
                        // app holds the camera or storage is full.
                        runCatching {
                            context.contentResolver.delete(event.outputResults.outputUri, null, null)
                        }
                        videoError = context.getString(R.string.couldnt_play)
                    }
                }
                else -> Unit
            }
        }
        isRecording = true
    }

    fun stopRecording() {
        if (!isRecording) return
        if (!recordingStarted) {
            // Recording hasn't actually started yet; stop as soon as Start arrives.
            stopRequested = true
            return
        }
        runCatching { activeRecording?.stop() }
    }

    fun takePhoto() {
        if (capturing) return
        CameraEditHandoff.filterIndex = selectedFilterIndex
        CameraEditHandoff.filterIntensity = filterIntensity
        CameraEditHandoff.beauty = beauty
        if (timerSeconds > 0) {
            scope.launch {
                countdown = timerSeconds
                while (countdown > 0) {
                    delay(1000)
                    countdown--
                }
                captureNow(context, imageCapture, torchOn, onOpenEditor) { capturing = it }
            }
        } else {
            captureNow(context, imageCapture, torchOn, onOpenEditor) { capturing = it }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .background(Color.Black)
    ) {
        if (hasPermission) {
            key(frontCamera) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            val providerFuture = ProcessCameraProvider.getInstance(ctx)
                            providerFuture.addListener({
                                val provider = providerFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(surfaceProvider)
                                }
                                runCatching {
                                    provider.unbindAll()
                                    provider.bindToLifecycle(
                                        lifecycleOwner,
                                        if (frontCamera) CameraSelector.DEFAULT_FRONT_CAMERA
                                        else CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview, imageCapture, videoCapture
                                    )
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Column(
                Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("📷", style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.camera_permission_needed), color = Color.White,
                    style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.camera_permission_sub),
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    modifier = Modifier.clickable { permissionLauncher.launch(Manifest.permission.CAMERA) }
                ) {
                    Text(stringResource(R.string.allow_camera), Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        color = Color.Black)
                }
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.clickable {
                        galleryLauncher.launch(PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                ) {
                    Text(stringResource(R.string.import_gallery), Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        color = Color.White)
                }
            }
        }

        // ---- Top bar: back | flash | timer ----
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { stopRecording(); onClose() }) {
                Icon(Icons.Filled.Close, "Close camera", tint = Color.White)
            }
            Row {
                IconButton(onClick = { torchOn = !torchOn }) {
                    Icon(
                        if (torchOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                        "Flash", tint = if (torchOn) Color(0xFFFFD54F) else Color.White
                    )
                }
                IconButton(onClick = { timerSeconds = when (timerSeconds) { 0 -> 3; 3 -> 10; else -> 0 } }) {
                    Text(
                        if (timerSeconds == 0) "" else "$timerSeconds",
                        color = if (timerSeconds > 0) Color(0xFFFFD54F) else Color.Unspecified,
                        style = MaterialTheme.typography.labelLarge
                    )
                    if (timerSeconds == 0) Icon(Icons.Filled.Timer, "Timer", tint = Color.White)
                }
            }
        }

        // Countdown / recording indicator overlays
        if (countdown > 0) {
            Text(
                "$countdown",
                color = Color.White,
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        if (capturing) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
        }
        if (isRecording) {
            Row(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.FiberManualRecord, null, tint = Color(0xFFFF5252),
                    modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "%02d:%02d".format(recordSeconds / 60, recordSeconds % 60),
                    color = Color.White, style = MaterialTheme.typography.labelLarge
                )
            }
        }
        videoError?.let { msg ->
            Text(
                msg,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            )
        }

        // ---- Bottom controls ----
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.35f))
                .padding(vertical = 10.dp)
        ) {
            AnimatedVisibility(visible = showBeautyPanel) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    BeautySlider(stringResource(R.string.smooth), beauty.smooth) { beauty = beauty.copy(smooth = it) }
                    BeautySlider(stringResource(R.string.brighten), beauty.brighten) { beauty = beauty.copy(brighten = it) }
                    BeautySlider(stringResource(R.string.tone_cool_warm), (beauty.tone + 1f) / 2f) { beauty = beauty.copy(tone = it * 2f - 1f) }
                    BeautySlider(stringResource(R.string.blemish_soften), beauty.blemish) { beauty = beauty.copy(blemish = it) }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .clickable { beauty = FilterEngine.BeautySettings() }
                    ) {
                        Text(stringResource(R.string.reset_beauty), Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (showFilters) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    itemsIndexed(FilterEngine.FILTERS) { i, filter ->
                        val selected = i == selectedFilterIndex
                        val locked = FilterEngine.isFilterLocked(filter, premium)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier
                                    .size(56.dp)
                                    .border(
                                        2.dp,
                                        if (selected) Color.White else Color.White.copy(alpha = 0.3f),
                                        CircleShape
                                    )
                                    .background(gradientFor(filter), CircleShape)
                                    .clickable {
                                        if (locked) onUpgrade() else selectedFilterIndex = i
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (locked) "🔒" else "${(i + 1)}",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            Text(
                                if (locked) stringResource(R.string.premium_locked) else filter.name,
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        }
                    }
                }
                if (selectedFilterIndex != 0) {
                    Slider(
                        value = filterIntensity,
                        onValueChange = { filterIntensity = it },
                        valueRange = 0f..1f,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Text(
                        stringResource(R.string.intensity_fmt, (filterIntensity * 100).toInt()),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }

            // Photo | Video mode toggle above the shutter
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                ModePill(stringResource(R.string.photo), !videoMode, enabled = !isRecording) { videoMode = false }
                ModePill(stringResource(R.string.video), videoMode, enabled = !isRecording) { videoMode = true }
                if (videoMode && !micGranted) {
                    Icon(Icons.Filled.MicOff, "Microphone disabled — video will have no sound",
                        tint = Color(0xFFFFD54F), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.no_audio), color = Color(0xFFFFD54F),
                        style = MaterialTheme.typography.labelMedium)
                }
            }

            // Main row: gallery | shutter/record | flip
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (videoMode) {
                    Spacer(Modifier.width(48.dp))
                } else {
                    IconButton(onClick = {
                        galleryLauncher.launch(PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Icon(Icons.Filled.PhotoLibrary, "Import from gallery", tint = Color.White)
                    }
                }

                if (videoMode) {
                    val red = Color(0xFFFF3B30)
                    if (isRecording) {
                        Box(
                            Modifier
                                .size(74.dp)
                                .border(4.dp, Color.White, CircleShape)
                                .padding(6.dp)
                                .background(Color.White, CircleShape)
                                .clickable { stopRecording() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Stop, "Stop recording", tint = red,
                                modifier = Modifier.size(34.dp))
                        }
                    } else {
                        Box(
                            Modifier
                                .size(74.dp)
                                .border(4.dp, Color.White, CircleShape)
                                .padding(6.dp)
                                .background(red, CircleShape)
                                .clickable { startRecording() }
                        )
                    }
                } else {
                    Box(
                        Modifier
                            .size(74.dp)
                            .border(4.dp, Color.White, CircleShape)
                            .padding(6.dp)
                            .background(Color.White, CircleShape)
                            .clickable { takePhoto() }
                    )
                }

                IconButton(onClick = { if (!isRecording) frontCamera = !frontCamera }) {
                    Icon(Icons.Filled.FlipCameraAndroid, "Switch camera", tint = Color.White)
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SideControl(stringResource(R.string.beauty), Icons.Filled.AutoAwesome, !showBeautyPanel) {
                    showBeautyPanel = !showBeautyPanel; showFilters = false
                }
                SideControl(stringResource(R.string.filter), Icons.Filled.PhotoCamera, showFilters) {
                    showFilters = !showFilters; showBeautyPanel = false
                }
            }
        }
    }
}

@Composable
private fun ModePill(label: String, active: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (active) Color.White else Color.White.copy(alpha = 0.12f),
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick)
    ) {
        Text(
            label,
            Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
            color = if (active) Color.Black else Color.White,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

private fun gradientFor(filter: FilterEngine.Filter): androidx.compose.ui.graphics.Brush =
    androidx.compose.ui.graphics.Brush.linearGradient(
        listOf(Color(0xFFF9DEE3), Color(0xFFEBDDF3), Color(0xFFDCE9F0))
    )

@Composable
private fun SideControl(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
                        active: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (active) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.12f),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun BeautySlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Column {
        Text(label, color = Color.White, style = MaterialTheme.typography.labelMedium)
        Slider(value = value, onValueChange = onChange)
    }
}

private fun captureNow(
    context: android.content.Context,
    imageCapture: ImageCapture,
    torchOn: Boolean,
    onOpenEditor: (Uri) -> Unit,
    setCapturing: (Boolean) -> Unit
) {
    setCapturing(true)
    val file = File(context.cacheDir, "glowcare_${System.currentTimeMillis()}.jpg")
    val options = ImageCapture.OutputFileOptions.Builder(file).build()
    imageCapture.flashMode = if (torchOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
    imageCapture.takePicture(
        options,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                setCapturing(false)
                onOpenEditor(Uri.fromFile(file))
            }
            override fun onError(exception: ImageCaptureException) {
                setCapturing(false)
                android.widget.Toast.makeText(context, "Couldn't take photo: ${exception.message}",
                    android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    )
}
