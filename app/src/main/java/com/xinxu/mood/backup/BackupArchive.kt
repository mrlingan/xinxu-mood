package com.xinxu.mood.backup

import android.content.Context
import com.xinxu.mood.data.*
import com.xinxu.mood.media.*
import java.io.*
import java.util.UUID
import java.util.zip.*

class BackupArchive(private val context: Context) {
    private val media = MediaStore(context)
    fun export(snapshot: Snapshot, output: OutputStream, password: CharArray) {
        val zip = File.createTempFile("backup-", ".zip", context.cacheDir)
        try {
            val metadata = BackupCodec.encode(snapshot); require(metadata.size <= 16_777_216) { "记录内容过多" }
            val names = snapshot.entries.flatMap { Attachments.decode(it.media) }.flatMap { it.files }.toSet()
            require(names.sumOf { media.file(it).length() } + metadata.size < ArchiveCrypto.LIMIT - 1_048_576) { "备份内容超过 512 MB，请减少附件后再导出" }
            ZipOutputStream(zip.outputStream().buffered()).use { out ->
                out.setLevel(Deflater.NO_COMPRESSION)
                out.putNextEntry(ZipEntry("records.json")); out.write(metadata); out.closeEntry()
                names.forEach { name ->
                    val file = media.file(name); require(file.isFile) { "附件缺失，无法完成备份" }
                    out.putNextEntry(ZipEntry("media/$name")); file.inputStream().use { it.copyTo(out, 65536) }; out.closeEntry()
                }
            }
            zip.inputStream().buffered().use { ArchiveCrypto.encrypt(it, output, password) }
        } finally { zip.delete(); password.fill('\u0000') }
    }
    suspend fun restore(input: InputStream, password: CharArray, dao: MoodDao) {
        val zip = File.createTempFile("restore-", ".zip", context.cacheDir)
        val staging = File(context.cacheDir, "restore-${UUID.randomUUID()}").apply { mkdirs() }
        val created = mutableListOf<File>()
        try {
            val source = BufferedInputStream(input); source.mark(8); val signature = ByteArray(8); DataInputStream(source).readFully(signature); source.reset()
            if (!signature.contentEquals(ArchiveCrypto.magic)) {
                val bytes = readLimited(source, 16_777_216)
                val snapshot = BackupCodec.decode(BackupCrypto.decrypt(bytes, password))
                require(snapshot.entries.all { Attachments.decode(it.media).isEmpty() }) { "附件备份格式不完整" }
                dao.replaceAll(snapshot.entries, snapshot.exercises); return
            }
            zip.outputStream().buffered().use { ArchiveCrypto.decrypt(source, it, password) }
            // No state is changed until every frame and every ZIP entry has passed validation.
            val snapshot: Snapshot
            val renamed = mutableMapOf<String, String>(); val seen = mutableSetOf<String>()
            ZipInputStream(zip.inputStream().buffered()).use { archive ->
                require(archive.nextEntry?.name == "records.json") { "备份缺少记录" }
                snapshot = BackupCodec.decode(readLimited(archive, 16_777_216)); archive.closeEntry()
                val expected = snapshot.entries.flatMap { Attachments.decode(it.media) }.flatMap { it.files }.toSet()
                var total = 0L
                while (true) {
                    val entry = archive.nextEntry ?: break
                    require(!entry.isDirectory && entry.name.startsWith("media/")) { "备份中的附件路径无效" }
                    val name = entry.name.removePrefix("media/")
                    require(name in expected && Attachments.validFile(name) && seen.add(name)) { "备份包含无效或重复附件" }
                    val replacement = "${UUID.randomUUID()}.${name.substringAfterLast('.')}"; renamed[name] = replacement
                    val file = File(staging, replacement)
                    file.outputStream().use { out ->
                        val buffer = ByteArray(65536); var size = 0L
                        while (true) { val n = archive.read(buffer); if (n < 0) break; size += n; total += n; require(size <= 200L * 1024 * 1024 && total <= ArchiveCrypto.LIMIT) { "附件体积超出限制" }; out.write(buffer, 0, n) }
                        require(size > 0) { "附件为空" }
                    }
                    archive.closeEntry()
                }
                require(seen == expected) { "备份中的附件不完整" }
            }
            val entries = snapshot.entries.map { entry -> entry.copy(media = Attachments.encode(Attachments.decode(entry.media).map { item -> item.copy(file = renamed.getValue(item.file), motion = item.motion?.let { renamed.getValue(it) }) })) }
            renamed.values.forEach { name -> val destination = media.file(name); check(File(staging, name).renameTo(destination)) { "无法保存附件，请检查剩余空间" }; created += destination }
            try { dao.replaceAll(entries, snapshot.exercises) } catch (e: Throwable) { created.forEach { it.delete() }; throw e }
        } catch (e: Throwable) { created.forEach { it.delete() }; throw e }
        finally { zip.delete(); staging.deleteRecursively(); password.fill('\u0000') }
    }
    private fun readLimited(input: InputStream, max: Int): ByteArray {
        val output = ByteArrayOutputStream(); val buffer = ByteArray(8192)
        while (true) { val n = input.read(buffer); if (n < 0) break; require(output.size() + n <= max) { "记录文件过大" }; output.write(buffer, 0, n) }
        return output.toByteArray()
    }
}
