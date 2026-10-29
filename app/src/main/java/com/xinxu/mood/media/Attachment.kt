package com.xinxu.mood.media

import org.json.JSONArray
import org.json.JSONObject

data class Attachment(val id: String, val file: String, val mime: String, val kind: String, val motion: String? = null) {
    val files: List<String> get() = listOfNotNull(file, motion)
}
object Attachments {
    private val safeFile = Regex("[a-f0-9-]{36}\\.(?:img|mp4)")
    fun validFile(name: String) = safeFile.matches(name)
    fun encode(items: List<Attachment>): String = JSONArray().apply { items.forEach { item -> put(JSONObject().apply {
        put("id", item.id); put("file", item.file); put("mime", item.mime); put("kind", item.kind); if (item.motion != null) put("motion", item.motion)
    }) } }.toString()
    fun decode(text: String): List<Attachment> {
        val array = JSONArray(text); require(array.length() <= 9) { "每条记录最多 9 个附件" }
        return (0 until array.length()).map { i -> array.getJSONObject(i).let {
            Attachment(it.getString("id"), it.getString("file"), it.getString("mime"), it.getString("kind"), it.optString("motion").takeIf(String::isNotBlank))
        } }.also { items ->
            require(items.map { it.id }.distinct().size == items.size)
            items.forEach { require(it.id.length == 36 && it.files.all(::validFile) && it.mime.length <= 100 && it.kind in listOf("photo", "video", "live") && (it.kind != "live" || it.motion != null)) { "附件信息无效" } }
        }
    }
}
