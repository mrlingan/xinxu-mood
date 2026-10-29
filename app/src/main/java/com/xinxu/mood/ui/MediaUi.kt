package com.xinxu.mood.ui

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.view.View
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.xinxu.mood.MainActivity
import com.xinxu.mood.R
import com.xinxu.mood.media.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun Context.host(): MainActivity? = when (this) { is MainActivity -> this; is ContextWrapper -> baseContext.host(); else -> null }

@Composable fun MediaEditor(value: String, onChange: (String) -> Unit, onBusy: (Boolean) -> Unit) {
    val context = LocalContext.current; val store = remember { MediaStore(context.applicationContext) }; val scope = rememberCoroutineScope()
    val attachments = remember(value) { Attachments.decode(value) }
    var busy by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }
    var pending by rememberSaveable { mutableStateOf<String?>(null) }; var pairPrompt by rememberSaveable { mutableStateOf(false) }
    var preview by remember { mutableStateOf<Attachment?>(null) }
    fun add(item: Attachment) { if (attachments.size < 9) onChange(Attachments.encode(attachments + item)) }
    fun importing(work: suspend () -> Unit) { scope.launch { busy = true; onBusy(true); try { work() } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; error = e.message ?: "导入失败，请重试" } finally { busy = false; onBusy(false) } } }
    val pair = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        context.host()?.endMediaSelection()
        if (uri != null && pending != null) importing {
            val photo = Attachments.decode(pending!!).single()
            val video = store.import(uri)
            require(video.kind == "video") { "请选择配对的短视频" }
            val duration = withContext(Dispatchers.IO) { MediaMetadataRetriever().let { r -> try { r.setDataSource(store.file(video.file).path); r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: Long.MAX_VALUE } finally { r.release() } } }
            if (duration > 15_000) { store.file(video.file).delete(); error = "实况视频请控制在 15 秒内，可通过视频入口添加完整视频"; pairPrompt = true }
            else { add(photo.copy(kind = "live", motion = video.file)); pending = null }
        } else if (pending != null) pairPrompt = true
    }
    val photos = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        context.host()?.endMediaSelection()
        if (uris.isNotEmpty()) importing {
            val added = mutableListOf<Attachment>(); var failure: String? = null
            for (uri in uris.take(9 - attachments.size)) { try { added += store.import(uri) } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; failure = e.message } }
            onChange(Attachments.encode(attachments + added))
            if (uris.size > 9 - attachments.size) error = "每条记录最多保留 9 个附件"
            else if (failure != null) error = "部分文件未能导入：$failure"
        }
    }
    val live = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        context.host()?.endMediaSelection()
        if (uri != null) importing { val item = store.import(uri, requireLive = true); if (item.kind == "live") add(item) else { pending = Attachments.encode(listOf(item)); pairPrompt = true } }
    }
    val video = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        context.host()?.endMediaSelection()
        if (uris.isNotEmpty()) importing {
            val added = mutableListOf<Attachment>(); var failure: String? = null
            for (uri in uris.take(9 - attachments.size)) { try { added += store.import(uri) } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; failure = e.message } }
            onChange(Attachments.encode(attachments + added)); if (failure != null) error = "部分视频未能导入：$failure" else if (uris.size > 9 - attachments.size) error = "每条记录最多保留 9 个附件"
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Triple("图片", R.drawable.xx_photo, { context.host()?.beginMediaSelection(); photos.launch(arrayOf("image/*")) }), Triple("实况", R.drawable.xx_live, { context.host()?.beginMediaSelection(); live.launch(arrayOf("image/*")) }), Triple("视频", R.drawable.xx_video, { context.host()?.beginMediaSelection(); video.launch(arrayOf("video/*")) })).forEach { (name, icon, action) ->
                OutlinedButton(onClick = action, enabled = !busy && attachments.size < 9, modifier = Modifier.weight(1f), contentPadding = PaddingValues(10.dp)) { Art(icon, modifier = Modifier.size(23.dp)); Spacer(Modifier.width(5.dp)); Text(name) }
            }
        }
        if (busy) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("正在把回忆收进本机…", style = MaterialTheme.typography.bodySmall) }
        if (attachments.isNotEmpty()) LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(attachments, key = { it.id }) { item -> Box(Modifier.size(116.dp)) {
                MediaThumbnail(item, Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).softClick { preview = item })
                Surface(Modifier.align(Alignment.TopEnd).padding(4.dp), shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface.copy(alpha = .95f)) { IconButton(onClick = { onChange(Attachments.encode(attachments - item)) }, enabled = !busy, modifier = Modifier.size(30.dp)) { Art(R.drawable.xx_close, "移除附件", Modifier.size(20.dp)) } }
            } }
        }
        Text("留住一张照片，或一小段有声音的回忆。${attachments.size}/9", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
    if (pairPrompt) AlertDialog(onDismissRequest = { pairPrompt = false; pending = null }, title = { Text("让照片动起来") }, text = { Text("这张原图中没有读到可播放的动态部分。可以选择与它配对的短视频，或先保存为普通照片。") }, confirmButton = { TextButton(onClick = { pairPrompt = false; context.host()?.beginMediaSelection(); pair.launch(arrayOf("video/*")) }) { Text("选择配对视频") } }, dismissButton = { TextButton(onClick = { pending?.let { add(Attachments.decode(it).single()) }; pending = null; pairPrompt = false }) { Text("作为照片添加") } })
    preview?.let { MediaPreview(it) { preview = null } }
}

@Composable fun MediaThumbnail(item: Attachment, modifier: Modifier = Modifier, large: Boolean = false) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(null, item.file, large) {
        value = withContext(Dispatchers.IO) { runCatching {
            val file = MediaStore(context.applicationContext).file(item.file)
            if (item.kind == "video") MediaMetadataRetriever().let { r -> try { r.setDataSource(file.path); if (android.os.Build.VERSION.SDK_INT >= 27) r.getScaledFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 512, 512) else r.getFrameAtTime(0) } finally { r.release() } }
            else { val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }; BitmapFactory.decodeFile(file.path, options); val target = if (large) 1800 else 400; var sample = 1; while (maxOf(options.outWidth, options.outHeight) / sample > target) sample *= 2; BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample }) }
        }.getOrNull() }
    }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap!!.asImageBitmap(), "${if (item.kind == "live") "实况照片" else if (item.kind == "video") "视频" else "照片"}预览", Modifier.fillMaxSize(), contentScale = if (large) ContentScale.Fit else ContentScale.Crop)
        else Art(R.drawable.xx_gallery, "预览暂不可用", Modifier.size(36.dp))
        if (!large && item.kind != "photo") Surface(Modifier.align(Alignment.BottomStart).padding(6.dp), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .9f)) { Row(Modifier.padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) { Art(if (item.kind == "live") R.drawable.xx_live else R.drawable.xx_play, modifier = Modifier.size(16.dp)); Text(if (item.kind == "live") "实况" else "视频", style = MaterialTheme.typography.labelSmall) } }
    }
}

@Composable fun MediaPreview(item: Attachment, onDismiss: () -> Unit) {
    val context = LocalContext.current; val store = remember { MediaStore(context.applicationContext) }
    var playing by remember { mutableStateOf(false) }; var player by remember { mutableStateOf<VideoView?>(null) }; var error by remember { mutableStateOf(false) }; var zoom by remember { mutableFloatStateOf(1f) }
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(owner) { val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_PAUSE) { playing = false; player?.pause() } }; owner.lifecycle.addObserver(observer); onDispose { owner.lifecycle.removeObserver(observer); player?.stopPlayback(); player = null } }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.safeDrawingPadding()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(if (item.kind == "live") "实况回忆" else if (item.kind == "video") "视频回忆" else "照片回忆", Modifier.padding(20.dp), style = MaterialTheme.typography.titleMedium); IconButton(onClick = onDismiss) { Art(R.drawable.xx_close, "关闭预览") } }
                Box(Modifier.weight(1f).fillMaxWidth().then(if (item.kind == "live") Modifier.pointerInput(Unit) { detectTapGestures(onPress = { error = false; playing = true; tryAwaitRelease(); playing = false }) } else Modifier), contentAlignment = Alignment.Center) {
                    if (item.kind != "photo") AndroidView(factory = { ctx -> VideoView(ctx).apply {
                        player = this; setVideoPath(store.file(item.motion ?: item.file).path)
                        setOnPreparedListener { it.isLooping = item.kind == "live"; if (playing) start() }
                        setOnCompletionListener { playing = false }
                        setOnErrorListener { _, _, _ -> error = true; playing = false; true }
                    } }, update = { view -> view.visibility = if (playing) View.VISIBLE else View.INVISIBLE; if (playing && !view.isPlaying) view.start() else if (!playing) { view.pause(); if (item.kind == "live") view.seekTo(0) } }, modifier = Modifier.fillMaxSize())
                    if (!playing) MediaThumbnail(item, Modifier.fillMaxSize().graphicsLayer { scaleX = zoom; scaleY = zoom }.then(if (item.kind == "photo") Modifier.pointerInput(Unit) { detectTransformGestures { _, _, factor, _ -> zoom = (zoom * factor).coerceIn(1f, 4f) } } else Modifier), large = true)
                    if (item.kind == "video") FilledTonalButton(onClick = { playing = !playing; error = false }, modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp)) { Art(R.drawable.xx_play); Spacer(Modifier.width(8.dp)); Text(if (playing) "暂停" else "播放视频") }
                }
                Text(if (error) "这段视频暂时无法播放，请尝试其他格式" else when (item.kind) { "live" -> "长按照片，让这一刻动起来"; "video" -> "回忆只在本机播放"; else -> "双指缩放，看看那些小小的细节" }, Modifier.align(Alignment.CenterHorizontally).padding(24.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
