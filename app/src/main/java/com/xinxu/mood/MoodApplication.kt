package com.xinxu.mood
import android.app.Application
import androidx.room.Room
import com.xinxu.mood.data.*
class MoodApplication : Application() {
    val database by lazy { Room.databaseBuilder(this, MoodDatabase::class.java, "xinxu.db").addMigrations(MIGRATION_1_2).build() }
    val settings by lazy { SettingsRepository(this) }
}
