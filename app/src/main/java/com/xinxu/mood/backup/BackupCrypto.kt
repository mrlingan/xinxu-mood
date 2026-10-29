package com.xinxu.mood.backup

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupCrypto {
    private val magic = "XINXU001".toByteArray(Charsets.US_ASCII)
    private fun key(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, 150_000, 256)
        return try { SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, "AES") } finally { spec.clearPassword() }
    }
    fun encrypt(plain: ByteArray, password: CharArray): ByteArray {
        require(password.size >= 8) { "密码至少需要 8 位" }
        val random = SecureRandom(); val salt = ByteArray(16).also(random::nextBytes); val nonce = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(password, salt), GCMParameterSpec(128, nonce)); cipher.updateAAD(magic)
        return magic + salt + nonce + cipher.doFinal(plain)
    }
    fun decrypt(bytes: ByteArray, password: CharArray): ByteArray {
        require(bytes.size in 52..16_777_216 && bytes.copyOfRange(0, 8).contentEquals(magic)) { "不是有效的心绪备份文件" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(password, bytes.copyOfRange(8, 24)), GCMParameterSpec(128, bytes.copyOfRange(24, 36))); cipher.updateAAD(magic)
        return cipher.doFinal(bytes.copyOfRange(36, bytes.size))
    }
}
