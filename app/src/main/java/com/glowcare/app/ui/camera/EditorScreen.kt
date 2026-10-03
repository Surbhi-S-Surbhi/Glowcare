package com.glowcare.app.ui.camera

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.glowcare.app.R
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface

/** Carries camera-applied filter/beauty choices into the editor. */
object CameraEditHandoff {
    var filterIndex: Int = 0
    var filterIntensity: Float = 0.5f
    var beauty: FilterEngine.BeautySettings = FilterEngine.BeautySettings()
}

/** Cap for the editor's working bitmap so filter processing stays memory-safe. */
private const val MAX_EDIT_DIMENSION = 2048

/** Cap for the saved export — high quality, still memory-safe (~67MP budget). */
private const val MAX_SAVE_DIMENSION = 4096

/** Application context accessor for the export renderer (set in GlowCareApp). */
object GlowCareAppContextHolder {
    @Volatile var appContext: android.content.Context? = null
}

/**
 * Photo editor: filters, beauty, and manual adjustments — all processed locally.
 * Nothing leaves the device unless the user explicitly taps Share.
 */
@Composable
fun EditorScreen(
    imageUri: Uri,
    isFromCamera: Boolean,
    onDone: () -> Unit,
    premium: Boolean = false,
    onUpgrade: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var tab by remember { mutableIntStateOf(0) } // 0 filters, 1 beauty, 2 adjust
    var filterIndex by remember { mutableIntStateOf(CameraEditHandoff.filterIndex) }
    var filterIntensity by remember { mutableStateOf(CameraEditHandoff.filterIntensity) }
    var beauty by remember { mutableStateOf(CameraEditHandoff.beauty) }
    var adj by remember { mutableStateOf(FilterEngine.Adjustments()) }
    var rotation by remember { mutableIntStateOf(0) }
    var showBefore by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf<Uri?>(null) }

    val original by produceState<Bitmap?>(initialValue = null, imageUri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                // Decode bounded: full-size camera photos (12-50MP) would otherwise
                // blow the heap during filtering. ~2048px keeps quality high while
                // making the whole pipeline memory-safe on every device.
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(imageUri)?.use {
                    BitmapFactory.decodeStream(it, null, bounds)
                }
                var sample = 1
                var maxSide = maxOf(bounds.outWidth, bounds.outHeight)
                while (maxSide / sample > MAX_EDIT_DIMENSION) sample *= 2
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                context.contentResolver.openInputStream(imageUri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, opts)
                }?.let { bmp ->
                    // Respect EXIF orientation for gallery imports.
                    val exif = context.contentResolver.openInputStream(imageUri)?.use {
                        ExifInterface(it)
                    }
                    val orientation = exif?.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) ?: 0
                    val degrees = when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }
                    if (degrees != 0f) {
                        val m = Matrix().apply { postRotate(degrees) }
                        Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
                    } else bmp
                }
            }.getOrNull()
        }
    }

    val previewBitmap by produceState<Bitmap?>(
        initialValue = null,
        original, filterIndex, filterIntensity, beauty, adj, rotation
    ) {
        val src = original ?: return@produceState
        value = withContext(Dispatchers.IO) {
            // Belt-and-braces: every stage already fails safe on its own; if the
            // chain still fails (e.g. rotation on a huge bitmap), show the
            // original photo rather than crashing.
            runCatching {
                var bmp = src
                if (rotation != 0) {
                    val m = Matrix().apply { postRotate(rotation.toFloat()) }
                    bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
                }
                bmp = FilterEngine.applyBeauty(bmp, beauty)
                bmp = FilterEngine.applyFilter(bmp, FilterEngine.FILTERS[filterIndex], filterIntensity)
                FilterEngine.applyAdjustments(bmp, adj)
            }.getOrDefault(src)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .background(Color(0xFF121014))
    ) {
        // Top bar
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDone) {
                Icon(Icons.Filled.Close, "Discard", tint = Color.White)
            }
            Text(stringResource(R.string.edit_photo), color = Color.White,
                style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (!showBefore) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clickable { showBefore = true }
                ) {
                    Text(stringResource(R.string.before), Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Color.White)
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clickable { showBefore = false }
                ) {
                    Text(stringResource(R.string.after), Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Color.Black)
                }
            }
        }

        // Image (shrinks so controls and actions always fit on any screen)
        Box(Modifier.weight(0.58f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val shown = if (showBefore) original else previewBitmap
            if (shown == null) {
                CircularProgressIndicator(color = Color.White)
            } else {
                Image(
                    bitmap = shown.asImageBitmap(),
                    contentDescription = "Photo preview",
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Tabs
        TabRow(
            selectedTabIndex = tab,
            containerColor = Color(0xFF1C181C),
            contentColor = Color.White
        ) {
            Tab(selected = tab == 0, onClick = { tab = 0 },
                text = { Text(stringResource(R.string.filters), color = Color.White) })
            Tab(selected = tab == 1, onClick = { tab = 1 },
                text = { Text(stringResource(R.string.beauty), color = Color.White) })
            Tab(selected = tab == 2, onClick = { tab = 2 },
                text = { Text(stringResource(R.string.adjust), color = Color.White) })
        }

        // Controls
        Column(
            Modifier
                .fillMaxWidth()
                .weight(0.42f)
                .verticalScroll(rememberScrollState())
        ) {
            when (tab) {
                0 -> {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(12.dp)) {
                        itemsIndexed(FilterEngine.FILTERS) { i, filter ->
                            val selected = i == filterIndex
                            val locked = FilterEngine.isFilterLocked(filter, premium)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    Modifier
                                        .size(54.dp)
                                        .background(
                                            androidx.compose.ui.graphics.Brush.linearGradient(
                                                listOf(Color(0xFFF9DEE3), Color(0xFFEBDDF3))
                                            ),
                                            CircleShape
                                        )
                                        .clickable {
                                            if (locked) onUpgrade()
                                            else { filterIndex = i; filterIntensity = 0.5f }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        if (locked) "🔒" else if (selected) "✓" else filter.name.take(1),
                                        color = Color(0xFF5A2430)
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    if (locked) stringResource(R.string.premium_locked) else filter.name,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    if (filterIndex != 0) {
                        Text(stringResource(R.string.intensity_fmt, (filterIntensity * 100).toInt()),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 16.dp))
                        Slider(value = filterIntensity, onValueChange = { filterIntensity = it },
                            modifier = Modifier.padding(horizontal = 16.dp))
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clickable { filterIndex = 0; filterIntensity = 0f }
                    ) {
                        Text(stringResource(R.string.off_reset), Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            color = Color.White)
                    }
                }
                1 -> {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        EditorSlider(stringResource(R.string.smooth), beauty.smooth) { beauty = beauty.copy(smooth = it) }
                        EditorSlider(stringResource(R.string.brighten), beauty.brighten) { beauty = beauty.copy(brighten = it) }
                        EditorSlider(stringResource(R.string.tone_cool_warm), (beauty.tone + 1f) / 2f) {
                            beauty = beauty.copy(tone = it * 2f - 1f)
                        }
                        Row {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier.clickable { beauty = FilterEngine.BeautySettings() }
                            ) {
                                Text(stringResource(R.string.reset_beauty), Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    color = Color.White)
                            }
                            Spacer(Modifier.width(12.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier.clickable { rotation = (rotation + 90) % 360 }
                            ) {
                                Text(stringResource(R.string.rotate_90), Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    color = Color.White)
                            }
                        }
                    }
                }
                2 -> {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        EditorSlider(stringResource(R.string.brightness), adj.brightness) { adj = adj.copy(brightness = it) }
                        EditorSlider(stringResource(R.string.contrast), adj.contrast) { adj = adj.copy(contrast = it) }
                        EditorSlider(stringResource(R.string.saturation), adj.saturation) { adj = adj.copy(saturation = it) }
                        EditorSlider(stringResource(R.string.warmth), adj.warmth) { adj = adj.copy(warmth = it) }
                        EditorSlider(stringResource(R.string.highlights), adj.highlights) { adj = adj.copy(highlights = it) }
                        EditorSlider(stringResource(R.string.shadows), adj.shadows) { adj = adj.copy(shadows = it) }
                        EditorSlider(stringResource(R.string.sharpness), (adj.sharpness * 2f) - 1f) { adj = adj.copy(sharpness = (it + 1f) / 2f) }
                        EditorSlider(stringResource(R.string.blur), adj.blur) { adj = adj.copy(blur = (it + 1f) / 2f) }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.clickable { adj = FilterEngine.Adjustments(); rotation = 0 }
                        ) {
                            Text(stringResource(R.string.reset_all), Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                color = Color.White)
                        }
                    }
                }
            }
        }

        // Bottom actions: save / share
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF1C181C))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            EditorAction(stringResource(R.string.save), Icons.Filled.Check, enabled = !saving) {
                if (saving) return@EditorAction
                saving = true
                scope.launch {
                    val uri = withContext(Dispatchers.IO) {
                        // Re-render at higher resolution than the live preview so
                        // the saved file keeps quality; bounded to stay memory-safe.
                        val hi = renderAtQuality(imageUri, adj, beauty, filterIndex,
                            filterIntensity, rotation)
                        saveToGallery(context, hi ?: previewBitmap ?: return@withContext null)
                    }
                    saving = false
                    saved = uri
                }
            }
            EditorAction(stringResource(R.string.share), Icons.Filled.IosShare, enabled = saved != null || previewBitmap != null) {
                val bmp = previewBitmap ?: return@EditorAction
                saving = true
                scope.launch {
                    val uri = withContext(Dispatchers.IO) {
                        saved ?: saveToGallery(context, bmp)
                    }
                    saving = false
                    if (uri != null) {
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "image/jpeg"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(share, "Share photo"))
                    }
                }
            }
            EditorAction(stringResource(R.string.edit_again), Icons.Filled.Refresh, enabled = true) {
                // Start a fresh editing pass on the SAME photo: reset all edits,
                // keep the image loaded, and land on the Filters tab.
                adj = FilterEngine.Adjustments()
                beauty = FilterEngine.BeautySettings()
                rotation = 0
                filterIndex = 0
                filterIntensity = 0.5f
                showBefore = false
                saved = null
                tab = 0
                android.widget.Toast.makeText(context, context.getString(R.string.editing_again), android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        LaunchedEffect(saved) {
            if (saved != null) {
                android.widget.Toast.makeText(context, context.getString(R.string.saved_gallery_toast),
                    android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private fun pct(v: Float) = "${(v * 100).toInt()}%"

/** Slider for a -1..1 adjustment; shows the current percentage in its label. */
@Composable
private fun EditorSlider(label: String, current: Float, onChange: (Float) -> Unit) {
    var value by remember { mutableStateOf(((current + 1f) / 2f).coerceIn(0f, 1f)) }
    Column {
        Text("$label ${pct(current)}", color = Color.White,
            style = MaterialTheme.typography.labelMedium)
        Slider(
            value = value,
            onValueChange = {
                value = it
                onChange(it * 2f - 1f)
            }
        )
    }
}

@Composable
private fun EditorAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
                         enabled: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (enabled) Color.White else Color.White.copy(alpha = 0.2f),
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.Black, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = Color.Black, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * Renders the current edit at up to [MAX_SAVE_DIMENSION] px (vs the 2048px live
 * preview) so the exported image keeps quality, while the working bitmap stays
 * within a safe memory budget. Falls back to the preview bitmap on any failure.
 */
internal fun renderAtQuality(
    uri: Uri,
    adj: FilterEngine.Adjustments,
    beauty: FilterEngine.BeautySettings,
    filterIndex: Int,
    filterIntensity: Float,
    rotationDegrees: Int
): Bitmap? = runCatching {
    val context = GlowCareAppContextHolder.appContext ?: return@runCatching null
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, opts)
    }
    var sample = 1
    var maxSide = maxOf(opts.outWidth, opts.outHeight)
    while (maxSide / sample > MAX_SAVE_DIMENSION) sample *= 2
    val decode = BitmapFactory.Options().apply { inSampleSize = sample }
    var bmp = context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, decode)
    } ?: return@runCatching null
    if (rotationDegrees != 0) {
        val m = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }
    bmp = FilterEngine.applyBeauty(bmp, beauty)
    bmp = FilterEngine.applyFilter(bmp, FilterEngine.FILTERS[filterIndex], filterIntensity)
    FilterEngine.applyAdjustments(bmp, adj)
}.getOrNull()

private fun saveToGallery(context: android.content.Context, bitmap: Bitmap): Uri? {
    return runCatching {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "GlowCare_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GlowCare")
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return null
        context.contentResolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        uri
    }.getOrNull()
}
