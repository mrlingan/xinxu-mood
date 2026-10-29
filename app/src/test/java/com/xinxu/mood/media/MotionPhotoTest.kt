package com.xinxu.mood.media

import java.io.File
import java.nio.ByteBuffer
import org.junit.Assert.*
import org.junit.Test

class MotionPhotoTest {
    private val mp4 = ByteBuffer.allocate(64).putInt(24).put("ftypisom".toByteArray()).array()
    private fun offset(bytes: ByteArray): Long? { val file = File.createTempFile("motion-", ".jpg"); return try { file.writeBytes(bytes); MotionPhoto.videoOffset(file) } finally { file.delete() } }
    private fun jpeg(meta: String, size: Int = 1000): ByteArray = ByteArray(size).also { it[0] = 0xff.toByte(); it[1] = 0xd8.toByte(); meta.toByteArray().copyInto(it, 16) }
    @Test fun readsLegacyOffsetAndStandardContainerItem() {
        assertEquals(1000L, offset(jpeg("Camera:MicroVideoOffset=\"64\"") + mp4))
        assertEquals(1000L, offset(jpeg("<Container:Item Item:Mime=\"video/mp4\" Item:Length=\"64\"/>") + mp4))
    }
    @Test fun findsAppendedVideoAcrossScanBoundary() { assertEquals(65535L, offset(jpeg("", 65535) + mp4)) }
    @Test fun rejectsStillPhotoBogusOffsetAndMalformedVideo() {
        assertNull(offset(jpeg("")))
        assertNull(offset(jpeg("Camera:MicroVideoOffset=\"999999999999\"") + ByteArray(64)))
        assertNull(offset(jpeg("Camera:MicroVideoOffset=\"64\"") + mp4.copyOf().also { it[4] = 0 }))
    }
}
