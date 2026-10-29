package com.xinxu.mood.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.*
import org.junit.Test

class ArchiveCryptoTest {
    private val password = "gentle-breeze".toCharArray()
    private fun encrypt(data: ByteArray): ByteArray = ByteArrayOutputStream().also { ArchiveCrypto.encrypt(ByteArrayInputStream(data), it, password) }.toByteArray()
    private fun decrypt(data: ByteArray, pass: CharArray = password): ByteArray = ByteArrayOutputStream().also { ArchiveCrypto.decrypt(ByteArrayInputStream(data), it, pass) }.toByteArray()
    @Test fun multiFrameArchiveRoundTripsIncludingPartialFinalChunk() {
        val data = ByteArray(65536 * 3 + 397) { (it * 31).toByte() }; val encrypted = encrypt(data)
        assertArrayEquals(data, decrypt(encrypted)); assertFalse(encrypted.contentEquals(encrypt(data)))
    }
    @Test fun rejectsWrongPasswordAndChangedFrame() {
        val encrypted = encrypt(ByteArray(70000) { 42 })
        listOf(encrypted.copyOf().also { it[100] = (it[100].toInt() xor 1).toByte() }, encrypted).forEachIndexed { i, bytes ->
            try { decrypt(bytes, if (i == 1) "wrong-pass".toCharArray() else password); fail("Must reject unauthenticated archive") } catch (_: javax.crypto.AEADBadTagException) { }
        }
    }
    @Test fun rejectsTruncationAtFrameBoundaryAndTrailingData() {
        val encrypted = encrypt(ByteArray(65536) { 9 })
        listOf(encrypted.copyOf(32 + 5 + 65536 + 16), encrypted + byteArrayOf(0), encrypted.copyOf(encrypted.size - 1)).forEach {
            try { decrypt(it); fail("Must require authenticated end and EOF") } catch (_: java.io.IOException) { } catch (_: IllegalArgumentException) { }
        }
    }
    @Test fun rejectsFrameReordering() {
        val encrypted = encrypt(ByteArray(65536 * 2) { (it / 65536).toByte() })
        val size = 5 + 65536 + 16
        val reordered = encrypted.copyOfRange(0, 32) + encrypted.copyOfRange(32 + size, 32 + size * 2) + encrypted.copyOfRange(32, 32 + size) + encrypted.copyOfRange(32 + size * 2, encrypted.size)
        try { decrypt(reordered); fail("Must bind frame order") } catch (_: javax.crypto.AEADBadTagException) { }
    }
}
