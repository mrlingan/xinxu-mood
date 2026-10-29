package com.xinxu.mood.backup
import org.junit.Assert.*
import org.junit.Test
class BackupCryptoTest {
    private val password = "a-safe-test-password".toCharArray()
    @Test fun roundTripUnicodeAndFreshSalt() {
        val plain = "我的情绪\n今天有一点焦虑，也有期待。".toByteArray()
        val first = BackupCrypto.encrypt(plain, password); val second = BackupCrypto.encrypt(plain, password)
        assertArrayEquals(plain, BackupCrypto.decrypt(first, password)); assertFalse(first.contentEquals(second))
    }
    @Test fun wrongPasswordCannotDecrypt() {
        val encrypted = BackupCrypto.encrypt("private".toByteArray(), password)
        assertThrows(javax.crypto.AEADBadTagException::class.java) { BackupCrypto.decrypt(encrypted, "incorrect".toCharArray()) }
    }
    @Test fun corruptedPayloadCannotDecrypt() {
        val encrypted = BackupCrypto.encrypt("private".toByteArray(), password)
        encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 1).toByte()
        assertThrows(javax.crypto.AEADBadTagException::class.java) { BackupCrypto.decrypt(encrypted, password) }
    }
    @Test fun truncatedBackupRejected() { assertThrows(IllegalArgumentException::class.java) { BackupCrypto.decrypt(ByteArray(20), password) } }
}
