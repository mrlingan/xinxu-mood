package com.xinxu.mood

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xinxu.mood.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.xinxu.mood.media.*

class MoodViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MoodApplication
    val dao = app.database.dao()
    val entries = dao.observeEntries().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val exercises = dao.observeExercises().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val settings = MutableStateFlow(Settings())
    val greeting = MutableStateFlow(greetings.first())
    fun greet() = viewModelScope.launch { runCatching { app.settings.nextGreeting() }.onSuccess { greeting.value = it } }
    val loaded = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)
    init { viewModelScope.launch { app.settings.flow.collect { settings.value = it; loaded.value = true } } }
    init { viewModelScope.launch(Dispatchers.IO) { runCatching { val referenced = dao.entries().flatMap { Attachments.decode(it.media) }.flatMap { it.files }.toSet(); MediaStore(app).cleanup(referenced) } } }
    fun clearRecords() = viewModelScope.launch {
        runCatching { dao.replaceAll(emptyList(), emptyList()); withContext(Dispatchers.IO) { MediaStore(app).cleanup(emptySet(), includeRecent = true) } }.onSuccess { message.value = "记录已清空" }.onFailure { message.value = "清空失败，请重试" }
    }
    fun save(entry: MoodEntry, done: () -> Unit) = viewModelScope.launch { runCatching { dao.save(entry) }.onSuccess { done(); message.value = "已保存，感谢你照顾此刻的自己" }.onFailure { message.value = "保存失败，请重试" } }
    fun saveExercise(session: ExerciseSession, done: () -> Unit) = viewModelScope.launch { runCatching { dao.saveExercise(session) }.onSuccess { done(); message.value = "练习已记录" }.onFailure { message.value = "保存失败，请重试" } }
    suspend fun update(value: Settings) { app.settings.update(value); ReminderReceiver.schedule(app, value) }
}
