package com.xinxu.mood

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.xinxu.mood.backup.*
import com.xinxu.mood.data.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersistenceTest {
    private lateinit var db: MoodDatabase
    @Before fun setup() { db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), MoodDatabase::class.java).build() }
    @After fun close() { db.close() }
    @Test fun encryptedExportRestoreRetainsEveryField() = runBlocking {
        val entry = MoodEntry(feeling = 2, emotions = "焦虑:4|期待:2", tags = "工作|学习", note = "第一行\n第二行", thought = "我担心结果", need = "休息")
        val session = ExerciseSession(type = "情绪整理", seconds = 90, before = 4, after = 2, notes = "整理内容")
        db.dao().save(entry); db.dao().saveExercise(session)
        val password = "test-password".toCharArray()
        val encrypted = BackupCrypto.encrypt(BackupCodec.encode(db.dao().snapshot()), password)
        db.dao().replaceAll(emptyList(), emptyList())
        val restored = BackupCodec.decode(BackupCrypto.decrypt(encrypted, password))
        db.dao().replaceAll(restored.entries, restored.exercises)
        assertEquals(listOf(entry), db.dao().entries()); assertEquals(listOf(session), db.dao().exercises())
    }
    @Test fun failedReplacementRollsBackOriginalData() = runBlocking {
        val original = MoodEntry(note = "保留我"); db.dao().save(original)
        val duplicate = MoodEntry(id = "duplicate")
        try { db.dao().replaceAll(listOf(duplicate, duplicate), emptyList()); fail("Must reject duplicate primary keys") } catch (_: android.database.sqlite.SQLiteConstraintException) { }
        assertEquals(listOf(original), db.dao().entries())
    }
    @Test fun invalidBackupRejectedBeforeMutatingDatabase() = runBlocking {
        val original = MoodEntry(); db.dao().save(original)
        val invalid = BackupCodec.encode(Snapshot(listOf(MoodEntry(feeling = 8)), emptyList()))
        try { BackupCodec.decode(invalid); fail("Must reject invalid score") } catch (_: IllegalArgumentException) { }
        assertEquals(listOf(original), db.dao().entries())
    }
    @Test fun updatesDoNotDuplicateAndDeleteWorks() = runBlocking {
        val original = MoodEntry(note = "旧内容"); db.dao().save(original); val updated = original.copy(note = "新内容"); db.dao().save(updated)
        assertEquals(listOf(updated), db.dao().entries()); db.dao().delete(updated); assertTrue(db.dao().entries().isEmpty())
    }
    @Test fun diskDatabaseSurvivesReopening() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>(); val name = "persistence-test.db"; context.deleteDatabase(name)
        val original = MoodEntry(note = "重启后还在")
        Room.databaseBuilder(context, MoodDatabase::class.java, name).build().let { disk -> disk.dao().save(original); disk.close() }
        Room.databaseBuilder(context, MoodDatabase::class.java, name).build().let { disk -> assertEquals(listOf(original), disk.dao().entries()); disk.close() }
        context.deleteDatabase(name); Unit
    }
}
