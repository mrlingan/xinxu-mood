package com.xinxu.mood.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File
import java.io.RandomAccessFile
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStore(private val context: Context) {
    val directory = File(context.filesDir, "memories").apply { mkdirs() }
    fun file(name: String): File { require(Attachments.validFile(name)); return File(directory, name) }
    suspend fun import(uri: Uri, requireLive: Boolean = false): Attachment = withContext(Dispatchers.IO) {
        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
        require(mime.startsWith("image/") || mime.startsWith("video/")) { "请选择图片或视频" }
        val id = UUID.randomUUID().toString(); val name = "$id.${if (mime.startsWith("video/")) "mp4" else "img"}"; val destination = file(name)
        var motion: File? = null
        try {
            requireNotNull(context.contentResolver.openInputStream(uri)).use { input -> destination.outputStream().use { out ->
                val buffer = ByteArray(65536); var total = 0L
                while (true) { val n = input.read(buffer); if (n < 0) break; total += n; require(total <= 200L * 1024 * 1024) { "单个附件请控制在 200 MB 内" }; out.write(buffer, 0, n) }
                require(total > 0) { "文件为空" }
            } }
            if (mime.startsWith("video/")) {
                require(!requireLive) { "请先选择实况照片原图" }; validateVideo(destination)
                Attachment(id, name, mime, "video")
            } else {
                val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                android.graphics.BitmapFactory.decodeFile(destination.path, bounds)
                require(bounds.outWidth > 0 && bounds.outHeight > 0) { "无法读取此图片，请换一个格式" }
                val offset = MotionPhoto.videoOffset(destination)
                if (offset != null) {
                    motion = file("${UUID.randomUUID()}.mp4")
                    RandomAccessFile(destination, "r").use { src -> src.seek(offset); motion!!.outputStream().use { out -> val buf = ByteArray(65536); while (true) { val n = src.read(buf); if (n < 0) break; out.write(buf, 0, n) } } }
                    if (runCatching { validateVideo(motion!!) }.isFailure) { motion!!.delete(); motion = null }
                }
                Attachment(id, name, mime, if (motion != null) "live" else "photo", motion?.name)
            }
        } catch (e: Exception) { destination.delete(); motion?.delete(); throw e }
    }
    fun cleanup(referenced: Set<String>, includeRecent: Boolean = false) {
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        directory.listFiles()?.filter { Attachments.validFile(it.name) && it.name !in referenced && (includeRecent || it.lastModified() < cutoff) }?.forEach { it.delete() }
    }
    fun validateVideo(file: File) { val retriever = MediaMetadataRetriever(); try { retriever.setDataSource(file.path); require(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) == "yes") { "无法读取此视频，请换一个格式" } } finally { retriever.release() } }
}
