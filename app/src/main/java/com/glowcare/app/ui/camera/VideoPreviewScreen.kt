package com.glowcare.app.ui.camera

import android.content.Intent
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.glowcare.app.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Video preview shown right after a recording finishes. The video is already
 * saved to MediaStore (visible in Gallery). Retake/Delete remove it; Share
 * hands the exact file to the user's chosen app — only on explicit action.
 */
@Composable
fun VideoPreviewScreen(videoUri: Uri, onDone: () -> Unit) {
    val context = LocalContext.current
    var playing by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    val videoViewRef = remember { mutableStateOf<VideoView?>(null) }

    // State changes drive the actual player.
    androidx.compose.runtime.LaunchedEffect(playing) {
        val view = videoViewRef.value ?: return@LaunchedEffect
        if (playing) {
            if (finished) { view.seekTo(0); finished = false }
            view.start()
        } else {
            view.pause()
        }
    }

    fun deleteVideo() {
        runCatching { context.contentResolver.delete(videoUri, null, null) }
        android.widget.Toast.makeText(context, context.getString(R.string.video_deleted), android.widget.Toast.LENGTH_SHORT).show()
    }

    fun shareVideo() {
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, videoUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(share, "Share video"))
    }

    Column(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .background(Color(0xFF121014))
    ) {
        // Header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, start = 20.dp, end = 20.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.your_video), color = Color.White, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.saved_to_gallery), color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelMedium)
        }

        // Player
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        videoViewRef.value = this
                        setVideoURI(videoUri)
                        val controller = MediaController(ctx)
                        controller.setAnchorView(this)
                        setMediaController(controller)
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            start()
                            playing = true
                        }
                        setOnCompletionListener {
                            playing = false
                            finished = true
                        }
                        setOnErrorListener { _, _, _ ->
                            android.widget.Toast.makeText(
                                ctx, ctx.getString(R.string.couldnt_play), android.widget.Toast.LENGTH_SHORT
                            ).show()
                            true
                        }
                    }
                },
                onRelease = {
                    it.stopPlayback()
                    videoViewRef.value = null
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Controls
        Column(Modifier.fillMaxWidth().background(Color(0xFF1C181C)).padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                VideoAction(stringResource(R.string.play_pause), if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow) {
                    finished = false
                    playing = !playing
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                VideoAction(stringResource(R.string.retake), Icons.Filled.Refresh) {
                    deleteVideo()
                    onDone()
                }
                VideoAction(stringResource(R.string.save_ok), Icons.Filled.Check) {
                    android.widget.Toast.makeText(context, context.getString(R.string.already_saved),
                        android.widget.Toast.LENGTH_SHORT).show()
                }
                VideoAction(stringResource(R.string.share), Icons.Filled.IosShare) { shareVideo() }
                VideoAction(stringResource(R.string.delete), Icons.Filled.Delete) {
                    deleteVideo()
                    onDone()
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.video_note),
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun VideoAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
                        onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.12f),
            modifier = Modifier.clickable(onClick = onClick)
        ) {
            Icon(icon, label, tint = Color.White,
                modifier = Modifier.padding(14.dp).size(22.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.labelMedium)
    }
}
