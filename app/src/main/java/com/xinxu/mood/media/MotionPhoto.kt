package com.xinxu.mood.media

import java.io.File
import java.io.RandomAccessFile

/** Reads standard MicroVideoOffset / Container XMP, plus JPEG files with appended MP4. */
object MotionPhoto {
    fun videoOffset(file: File): Long? = RandomAccessFile(file, "r").use { input ->
        val size = input.length(); if (size < 32) return null
        val header = ByteArray(minOf(size, 2_097_152).toInt()); input.readFully(header)
        val xmp = header.toString(Charsets.ISO_8859_1)
        val offset = Regex("(?:MicroVideoOffset|MotionPhotoVideoOffset)\\s*=\\s*[\"'](\\d+)[\"']").find(xmp)?.groupValues?.get(1)?.toLongOrNull()
        if (offset != null && offset in 16 until size && isMp4(input, size - offset)) return size - offset
        val item = Regex("<[^>]*Item\\b[^>]*>").findAll(xmp).map { it.value }.firstOrNull { it.contains("video/mp4") }
        val length = item?.let { Regex("(?:Item:)?Length\\s*=\\s*[\"'](\\d+)[\"']").find(it)?.groupValues?.get(1)?.toLongOrNull() }
        if (length != null && length in 16 until size && isMp4(input, size - length)) return size - length
        // Some manufacturers omit standard XMP. Only use an appended video after JPEG data.
        val jpeg = header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte()
        if (!jpeg) return null
        input.seek(2); var position = 2L; val chunk = ByteArray(65536 + 8); var overlap = 0
        while (position < size) {
            input.seek(position)
            val count = input.read(chunk, overlap, 65536); if (count < 0) break
            val used = count + overlap; val base = position - overlap
            for (i in 4 until used - 4) {
                if (chunk[i] == 102.toByte() && chunk[i + 1] == 116.toByte() && chunk[i + 2] == 121.toByte() && chunk[i + 3] == 112.toByte()) {
                    val start = base + i - 4
                    if (start > 100 && isMp4(input, start)) return start
                }
            }
            overlap = minOf(8, used); chunk.copyInto(chunk, 0, used - overlap, used); position += count
        }
        null
    }
    private fun isMp4(input: RandomAccessFile, start: Long): Boolean {
        if (start < 0 || start + 16 > input.length()) return false
        input.seek(start); val boxSize = input.readInt().toLong() and 0xffffffffL
        val type = ByteArray(4); input.readFully(type)
        val brand = ByteArray(4); input.readFully(brand)
        return type.toString(Charsets.US_ASCII) == "ftyp" && boxSize in 16..(input.length() - start) && brand.toString(Charsets.US_ASCII).trim() in listOf("isom", "mp42", "mp41", "iso2", "avc1", "qt", "M4V", "3gp5")
    }
}
