package com.xinxu.mood.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.store by preferencesDataStore("settings")
data class Settings(val theme: String = "system", val locked: Boolean = false, val reminder: Boolean = false, val hour: Int = 21, val minute: Int = 0, val tags: Set<String> = defaultTags, val breathHaptics: Boolean = false, val breezeLevel: Int = 0)
class SettingsRepository(context: Context) {
    private val store = context.store
    private val theme = stringPreferencesKey("theme")
    private val locked = booleanPreferencesKey("locked")
    private val reminder = booleanPreferencesKey("reminder")
    private val hour = intPreferencesKey("hour")
    private val minute = intPreferencesKey("minute")
    private val tags = stringSetPreferencesKey("tags")
    private val breathHaptics = booleanPreferencesKey("breath_haptics_breeze_v1")
    private val breezeLevel = intPreferencesKey("breeze_level")
    private val quoteIndex = intPreferencesKey("quote_index")
    val flow = store.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map {
        Settings(it[theme] ?: "system", it[locked] ?: false, it[reminder] ?: false, it[hour] ?: 21, it[minute] ?: 0, it[tags] ?: defaultTags, it[breathHaptics] ?: false, (it[breezeLevel] ?: 0).coerceIn(0, 2))
    }
    suspend fun nextGreeting(): String {
        var index = 0
        store.edit { prefs -> index = ((prefs[quoteIndex] ?: -1) + 1) % greetings.size; prefs[quoteIndex] = index }
        return greetings[index]
    }
    suspend fun update(value: Settings) { store.edit { it[theme] = value.theme; it[locked] = value.locked; it[reminder] = value.reminder; it[hour] = value.hour; it[minute] = value.minute; it[tags] = value.tags; it[breathHaptics] = value.breathHaptics; it[breezeLevel] = value.breezeLevel.coerceIn(0, 2) } }
}

val greetings = listOf(
    "今天的你，也值得被温柔以待。",
    "先把肩膀放松，世界可以等一小会儿。",
    "不必每一天都发光，安静地生长也很好。",
    "心情像天气，云会慢慢走过。",
    "允许自己慢一点，花有自己的花期。",
    "一口呼吸，就是一个新的开始。",
    "不想说话的时候，这里也陪着你。",
    "把今天的小小欢喜，轻轻收进口袋。",
    "累了就歇一歇，你已经走了很远。",
    "此刻的感受，不需要一个完美的解释。",
    "照顾好自己，也是一件很了不起的小事。",
    "有些答案，可以留给明天的自己。",
    "为自己留一小块柔软的地方。",
    "今天只做一点点，也算向前走。",
    "难过可以被看见，快乐也可以被珍藏。",
    "愿你在忙碌的缝隙里，遇见一阵微风。"
)
