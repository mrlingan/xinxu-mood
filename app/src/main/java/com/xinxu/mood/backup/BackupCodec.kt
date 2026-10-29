package com.xinxu.mood.backup

import com.xinxu.mood.data.*
import com.xinxu.mood.media.Attachments
import org.json.JSONArray
import org.json.JSONObject

object BackupCodec {
    fun encode(snapshot: Snapshot): ByteArray {
        val entries = JSONArray(); snapshot.entries.forEach { e -> entries.put(JSONObject().apply {
            put("id", e.id); put("time", e.occurredAt); put("feeling", e.feeling); put("emotions", e.emotions); put("tags", e.tags); put("note", e.note); put("thought", e.thought); put("need", e.need); put("updated", e.updatedAt); put("media", org.json.JSONArray(e.media))
        }) }
        val sessions = JSONArray(); snapshot.exercises.forEach { e -> sessions.put(JSONObject().apply {
            put("id", e.id); put("type", e.type); put("time", e.finishedAt); put("seconds", e.seconds); put("before", e.before); put("after", e.after); put("notes", e.notes)
        }) }
        return JSONObject().put("version", 2).put("entries", entries).put("exercises", sessions).toString().toByteArray(Charsets.UTF_8)
    }
    fun decode(bytes: ByteArray): Snapshot {
        val root = JSONObject(bytes.toString(Charsets.UTF_8)); require(root.getInt("version") in 1..2) { "备份版本不受支持" }
        val es = root.getJSONArray("entries"); val xs = root.getJSONArray("exercises")
        require(es.length() <= 50000 && xs.length() <= 50000) { "备份记录过多" }
        val entries = (0 until es.length()).map { i -> es.getJSONObject(i).let {
            MoodEntry(it.getString("id"), it.getLong("time"), it.getInt("feeling"), it.getString("emotions"), it.getString("tags"), it.getString("note"), it.getString("thought"), it.getString("need"), it.getLong("updated"), it.optJSONArray("media")?.toString() ?: "[]")
        } }
        val sessions = (0 until xs.length()).map { i -> xs.getJSONObject(i).let {
            ExerciseSession(it.getString("id"), it.getString("type"), it.getLong("time"), it.getInt("seconds"), it.getInt("before"), it.getInt("after"), it.getString("notes"))
        } }
        require(entries.map { it.id }.distinct().size == entries.size && sessions.map { it.id }.distinct().size == sessions.size) { "备份包含重复记录" }
        entries.forEach { e -> require(e.id.length in 1..100 && e.occurredAt in 0..253402214400000L && e.updatedAt >= 0 && e.feeling in 1..5 && e.emotions.isNotBlank() && e.emotionMap().isNotEmpty() && e.emotions.split("|").size == e.emotionMap().size && e.emotionMap().all { it.key in emotionNames && it.value in 1..5 } && e.note.length <= 10000 && e.thought.length <= 10000 && e.need.length <= 10000 && e.tags.length <= 5000) { "备份中的情绪记录无效" } }
        sessions.forEach { require(it.id.length in 1..100 && it.type in listOf("呼吸练习", "情绪整理", "短暂停顿") && it.finishedAt in 0..253402214400000L && it.seconds in 0..86400 && it.before in 1..5 && it.after in 1..5 && it.notes.length <= 40000) { "备份中的练习记录无效" } }
        entries.forEach { Attachments.decode(it.media) }
        return Snapshot(entries, sessions)
    }
}
