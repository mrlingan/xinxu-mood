package com.xinxu.mood.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "entries", indices = [Index("occurredAt")])
data class MoodEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val occurredAt: Long = System.currentTimeMillis(),
    val feeling: Int = 3,
    val emotions: String = "平静:3",
    val tags: String = "",
    val note: String = "",
    val thought: String = "",
    val need: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "'[]'") val media: String = "[]"
)
@Entity(tableName = "exercises")
data class ExerciseSession(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val type: String,
    val finishedAt: Long = System.currentTimeMillis(),
    val seconds: Int,
    val before: Int,
    val after: Int,
    val notes: String = ""
)
@Dao
interface MoodDao {
    @Query("SELECT * FROM entries ORDER BY occurredAt DESC") fun observeEntries(): Flow<List<MoodEntry>>
    @Query("SELECT * FROM exercises ORDER BY finishedAt DESC") fun observeExercises(): Flow<List<ExerciseSession>>
    @Query("SELECT * FROM entries ORDER BY occurredAt DESC") suspend fun entries(): List<MoodEntry>
    @Query("SELECT * FROM exercises ORDER BY finishedAt DESC") suspend fun exercises(): List<ExerciseSession>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(entry: MoodEntry)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveExercise(session: ExerciseSession)
    @Delete suspend fun delete(entry: MoodEntry)
    @Query("DELETE FROM entries") suspend fun clearEntries()
    @Query("DELETE FROM exercises") suspend fun clearExercises()
    @Insert suspend fun insertEntries(entries: List<MoodEntry>)
    @Insert suspend fun insertExercises(sessions: List<ExerciseSession>)
    @Transaction suspend fun replaceAll(entries: List<MoodEntry>, sessions: List<ExerciseSession>) {
        clearEntries(); clearExercises(); insertEntries(entries); insertExercises(sessions)
    }
    @Transaction suspend fun snapshot(): Snapshot = Snapshot(entries(), exercises())
}
data class Snapshot(val entries: List<MoodEntry>, val exercises: List<ExerciseSession>)
@Database(entities = [MoodEntry::class, ExerciseSession::class], version = 2, exportSchema = true)
abstract class MoodDatabase : RoomDatabase() { abstract fun dao(): MoodDao }

val emotionNames = listOf("开心", "平静", "难过", "焦虑", "生气", "疲惫", "期待", "孤独")
val defaultTags = setOf("工作", "学习", "家庭", "关系", "健康", "独处", "休息")
val feelingNames = listOf("很难受", "较难受", "一般", "较舒服", "很舒服")
fun MoodEntry.emotionMap(): Map<String, Int> = emotions.split("|").mapNotNull {
    val pair = it.split(":"); if (pair.size == 2) pair[1].toIntOrNull()?.let { v -> pair[0] to v } else null
}.toMap()

val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) { db.execSQL("ALTER TABLE entries ADD COLUMN media TEXT NOT NULL DEFAULT '[]'") }
}
