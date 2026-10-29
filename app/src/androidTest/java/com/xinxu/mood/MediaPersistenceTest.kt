package com.xinxu.mood

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.xinxu.mood.backup.*
import com.xinxu.mood.data.*
import com.xinxu.mood.media.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.*
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class MediaPersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @Test fun versionOneDatabaseUpgradesWithoutLosingDiary(): Unit = runBlocking {
        val name = "migration-test.db"; context.deleteDatabase(name)
        val file = context.getDatabasePath(name); file.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            old.execSQL("CREATE TABLE entries (id TEXT NOT NULL PRIMARY KEY, occurredAt INTEGER NOT NULL, feeling INTEGER NOT NULL, emotions TEXT NOT NULL, tags TEXT NOT NULL, note TEXT NOT NULL, thought TEXT NOT NULL, need TEXT NOT NULL, updatedAt INTEGER NOT NULL)")
            old.execSQL("CREATE INDEX index_entries_occurredAt ON entries(occurredAt)")
            old.execSQL("CREATE TABLE exercises (id TEXT NOT NULL PRIMARY KEY, type TEXT NOT NULL, finishedAt INTEGER NOT NULL, seconds INTEGER NOT NULL, before INTEGER NOT NULL, after INTEGER NOT NULL, notes TEXT NOT NULL)")
            old.execSQL("INSERT INTO entries VALUES('old',1,3,'平静:3','工作','旧版日记','想法','需要',2)")
            old.version = 1
        }
        val db = Room.databaseBuilder(context, MoodDatabase::class.java, name).addMigrations(MIGRATION_1_2).build()
        try { val entry = db.dao().entries().single(); assertEquals("旧版日记", entry.note); assertEquals("[]", entry.media); assertEquals("想法", entry.thought) } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun encryptedMediaBackupRestoresEveryByteAndRejectsTamperingAtomically(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, MoodDatabase::class.java).build()
        val store = MediaStore(context); val originalFile = store.file("${UUID.randomUUID()}.img"); val bytes = ByteArray(155000) { (it * 17).toByte() }; originalFile.writeBytes(bytes)
        val attachment = Attachment(UUID.randomUUID().toString(), originalFile.name, "image/jpeg", "photo")
        val original = MoodEntry(note = "带附件", media = Attachments.encode(listOf(attachment))); db.dao().save(original)
        val output = ByteArrayOutputStream(); BackupArchive(context).export(db.dao().snapshot(), output, "password-123".toCharArray())
        try {
            val encrypted = output.toByteArray(); val broken = encrypted.copyOf().also { it[it.size - 4] = (it[it.size - 4].toInt() xor 1).toByte() }
            try { BackupArchive(context).restore(ByteArrayInputStream(broken), "password-123".toCharArray(), db.dao()); fail("Must reject tampering") } catch (_: javax.crypto.AEADBadTagException) { }
            assertEquals(original, db.dao().entries().single())
            db.dao().replaceAll(emptyList(), emptyList()); originalFile.delete()
            BackupArchive(context).restore(ByteArrayInputStream(encrypted), "password-123".toCharArray(), db.dao())
            val restored = db.dao().entries().single(); assertEquals(original.note, restored.note)
            val restoredItem = Attachments.decode(restored.media).single(); assertNotEquals(originalFile.name, restoredItem.file); assertArrayEquals(bytes, store.file(restoredItem.file).readBytes()); store.file(restoredItem.file).delete()
        } finally { originalFile.delete(); db.close() }
    }
    @Test fun oldBackupStillRestoresAndAttachmentPathsCannotEscape(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, MoodDatabase::class.java).build()
        try {
            val original = MoodEntry(note = "兼容旧备份")
            val json = org.json.JSONObject(BackupCodec.encode(Snapshot(listOf(original), emptyList())).toString(Charsets.UTF_8)).put("version", 1)
            json.getJSONArray("entries").getJSONObject(0).remove("media")
            val encrypted = BackupCrypto.encrypt(json.toString().toByteArray(), "password-123".toCharArray())
            BackupArchive(context).restore(ByteArrayInputStream(encrypted), "password-123".toCharArray(), db.dao()); assertEquals(original, db.dao().entries().single())
            try { Attachments.decode("[{\"id\":\"${UUID.randomUUID()}\",\"file\":\"../../bad.img\",\"mime\":\"image/jpeg\",\"kind\":\"photo\"}]"); fail("Must reject path traversal") } catch (_: IllegalArgumentException) { }
        } finally { db.close() }
    }
    @Test fun realMotionPhotoImportsOfflineAndKeepsPlayableVideo(): Unit = runBlocking {
        val fixture = File(context.cacheDir, "fixture-motion.jpg")
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().context.assets.open("motion-photo.jpg").use { source -> fixture.outputStream().use { source.copyTo(it) } }
        val store = MediaStore(context)
        try {
            val item = store.import(android.net.Uri.fromFile(fixture), requireLive = true)
            assertEquals("live", item.kind); assertNotNull(item.motion)
            store.validateVideo(store.file(item.motion!!))
            val photo = android.graphics.BitmapFactory.decodeFile(store.file(item.file).path); assertNotNull(photo); photo.recycle()
            item.files.forEach { store.file(it).delete() }
        } finally { fixture.delete() }
    }
    @Test fun newBreezeRequiresOptInAndGreetingsRotate(): Unit = runBlocking {
        val repository = SettingsRepository(context)
        assertFalse(Settings().breathHaptics)
        val first = repository.nextGreeting(); val second = repository.nextGreeting(); assertNotEquals(first, second); assertTrue(first in greetings); assertTrue(second in greetings)
        repository.update(Settings(breathHaptics = true, breezeLevel = 1)); assertTrue(repository.flow.first().breathHaptics)
        repository.update(Settings()); assertFalse(repository.flow.first().breathHaptics)
    }
}
