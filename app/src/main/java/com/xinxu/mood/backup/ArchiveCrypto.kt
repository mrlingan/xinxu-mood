package com.xinxu.mood.backup

import java.io.*
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Authenticated 64 KiB frames keep large video backups out of the Java heap.
 * A separately authenticated final frame binds the complete sequence and detects truncation.
 * Restore callers must stage the entire archive before making any data visible. */
object ArchiveCrypto {
    val magic = "XINXU002".toByteArray(Charsets.US_ASCII)
    const val LIMIT = 512L * 1024 * 1024
    private const val CHUNK = 65536
    private fun key(password: CharArray, salt: ByteArray): SecretKeySpec {
        require(password.size >= 8)
        val spec = PBEKeySpec(password, salt, 150_000, 256)
        return try { SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, "AES") } finally { spec.clearPassword() }
    }
    private fun cipher(mode: Int, key: SecretKeySpec, header: ByteArray, index: Int, end: Boolean, length: Int): Cipher {
        val nonce = header.copyOfRange(24, 32) + ByteBuffer.allocate(4).putInt(index).array()
        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(mode, key, GCMParameterSpec(128, nonce)); updateAAD(header)
            updateAAD(ByteBuffer.allocate(9).putInt(index).put(if (end) 1.toByte() else 0.toByte()).putInt(length).array())
        }
    }
    fun encrypt(input: InputStream, output: OutputStream, password: CharArray) {
        val random = SecureRandom(); val header = magic + ByteArray(16).also(random::nextBytes) + ByteArray(8).also(random::nextBytes)
        val key = key(password, header.copyOfRange(8, 24)); val out = DataOutputStream(output); out.write(header)
        val buffer = ByteArray(CHUNK); var index = 0; var total = 0L
        while (true) {
            var n = 0
            while (n < buffer.size) { val count = input.read(buffer, n, buffer.size - n); if (count < 0) break; if (count == 0) continue; n += count }
            if (n == 0) break
            total += n; require(total <= LIMIT) { "备份内容超过 512 MB，请减少附件后再导出" }
            val encrypted = cipher(Cipher.ENCRYPT_MODE, key, header, index++, false, n + 16).doFinal(buffer, 0, n)
            out.writeByte(0); out.writeInt(encrypted.size); out.write(encrypted)
        }
        val end = cipher(Cipher.ENCRYPT_MODE, key, header, index, true, 16).doFinal()
        out.writeByte(1); out.writeInt(16); out.write(end); out.flush()
    }
    fun decrypt(input: InputStream, output: OutputStream, password: CharArray) {
        val source = DataInputStream(input); val header = ByteArray(32); source.readFully(header)
        require(header.copyOfRange(0, 8).contentEquals(magic)) { "不是有效的心绪备份" }
        val key = key(password, header.copyOfRange(8, 24)); var index = 0; var total = 0L
        while (true) {
            val type = source.readUnsignedByte(); require(type in 0..1)
            val length = source.readInt(); require(if (type == 1) length == 16 else length in 17..CHUNK + 16) { "备份分块无效" }
            val block = ByteArray(length); source.readFully(block)
            val plain = cipher(Cipher.DECRYPT_MODE, key, header, index++, type == 1, length).doFinal(block)
            if (type == 1) { require(source.read() == -1) { "备份尾部无效" }; output.flush(); return }
            total += plain.size; require(total <= LIMIT) { "备份内容超过 512 MB" }; output.write(plain)
        }
    }
}
