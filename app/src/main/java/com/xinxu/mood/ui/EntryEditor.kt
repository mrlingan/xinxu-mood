@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.xinxu.mood.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xinxu.mood.data.*
import com.xinxu.mood.R
import androidx.compose.ui.Alignment
import java.time.*

@Composable fun EntryEditor(entry: MoodEntry?, tags: Set<String>, onDismiss: () -> Unit, onSave: (MoodEntry) -> Unit, onDelete: (MoodEntry) -> Unit) {
    val draftId = rememberSaveable { java.util.UUID.randomUUID().toString() }
    var feeling by rememberSaveable { mutableIntStateOf(entry?.feeling ?: 3) }
    var emotionString by rememberSaveable { mutableStateOf(entry?.emotions ?: "平静:3") }
    val emotionMap = MoodEntry(emotions = emotionString).emotionMap()
    var selectedTags by rememberSaveable { mutableStateOf(entry?.tags ?: "") }
    var note by rememberSaveable { mutableStateOf(entry?.note ?: "") }; var thought by rememberSaveable { mutableStateOf(entry?.thought ?: "") }; var need by rememberSaveable { mutableStateOf(entry?.need ?: "") }
    var media by rememberSaveable { mutableStateOf(entry?.media ?: "[]") }
    var importing by remember { mutableStateOf(false) }
    var time by rememberSaveable { mutableLongStateOf(entry?.occurredAt ?: System.currentTimeMillis()) }
    var delete by remember { mutableStateOf(false) }; var saving by remember { mutableStateOf(false) }
    val context = LocalContext.current
    fun updateEmotion(name: String, strength: Int?) { val map = emotionMap.toMutableMap(); if (strength == null) map.remove(name) else map[name] = strength; emotionString = map.entries.joinToString("|") { "${it.key}:${it.value}" } }
    BreezeDialog(onDismiss = onDismiss, canDismiss = !importing) { close ->
        Scaffold(modifier = Modifier.fillMaxSize(), topBar = { TopAppBar(title = { Text(if (entry == null) "记录此刻" else "心绪详情") }, navigationIcon = { IconButton(onClick = close, enabled = !importing) { Art(R.drawable.xx_close, "关闭") } }, actions = { TextButton(onClick = { saving = true; onSave((entry ?: MoodEntry(id = draftId)).copy(occurredAt = time, feeling = feeling, emotions = emotionString, tags = selectedTags, note = note.trim(), thought = thought.trim(), need = need.trim(), updatedAt = System.currentTimeMillis(), media = media)); saving = false }, enabled = emotionMap.isNotEmpty() && !saving && !importing) { Text("保存") } }) }) { padding ->
            Column(Modifier.padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("整体感觉", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) { feelingNames.forEachIndexed { i, name ->
                    Surface(Modifier.weight(1f).softClick { feeling = i + 1 }, shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp), color = if (feeling == i + 1) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface) {
                        Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) { Art(moodArt[i], modifier = Modifier.size(42.dp)); Spacer(Modifier.height(6.dp)); Text(name, style = MaterialTheme.typography.labelSmall) }
                    }
                } }
                SectionTitle("有哪些情绪？可多选")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { emotionNames.forEachIndexed { i, name -> FilterChip(selected = name in emotionMap, onClick = { updateEmotion(name, if (name in emotionMap) null else 3) }, label = { Text(name) }, leadingIcon = { Art(emotionArt[i], modifier = Modifier.size(22.dp)) }) } }
                emotionMap.forEach { (name, value) -> Column { Text("$name · 强度 $value / 5", style = MaterialTheme.typography.bodyMedium); Slider(value.toFloat(), onValueChange = { updateEmotion(name, it.toInt()) }, valueRange = 1f..5f, steps = 3) } }
                Text("强度表示感受有多强烈，与整体舒服程度分别记录。", style = MaterialTheme.typography.bodySmall)
                SectionTitle("发生在什么时候？")
                OutlinedButton(onClick = {
                    val current = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault())
                    DatePickerDialog(context, { _, y, m, d ->
                        TimePickerDialog(context, { _, h, minute -> time = LocalDate.of(y, m + 1, d).atTime(h, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli().coerceAtMost(System.currentTimeMillis()) }, current.hour, current.minute, true).show()
                    }, current.year, current.monthValue - 1, current.dayOfMonth).apply { datePicker.maxDate = System.currentTimeMillis() }.show()
                }) { Text(displayTime(time)) }
                SectionTitle("与什么有关？")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { (tags + selectedTags.split("|").filter { it.isNotBlank() }).sorted().forEach { tag ->
                    val selected = selectedTags.split("|").filter { it.isNotBlank() }.toSet()
                    FilterChip(selected = tag in selected, onClick = { selectedTags = (if (tag in selected) selected - tag else selected + tag).joinToString("|") }, label = { Text(tag) })
                } }
                SectionTitle("收藏这一刻")
                MediaEditor(media, onChange = { media = it }, onBusy = { importing = it })
                OutlinedTextField(note, { if (it.length <= 10000) note = it }, modifier = Modifier.fillMaxWidth(), label = { Text("发生了什么？（选填）") }, minLines = 3)
                OutlinedTextField(thought, { if (it.length <= 10000) thought = it }, modifier = Modifier.fillMaxWidth(), label = { Text("当时有什么想法？（选填）") }, minLines = 2)
                OutlinedTextField(need, { if (it.length <= 10000) need = it }, modifier = Modifier.fillMaxWidth(), label = { Text("现在需要什么？（选填）") }, minLines = 2)
                Text("文字仅保存在本机，给自己一点诚实的空间。", style = MaterialTheme.typography.bodySmall)
                if (entry != null) TextButton(onClick = { delete = true }) { Text("删除这条记录", color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.height(24.dp))
            }
        }
        if (delete && entry != null) AlertDialog(onDismissRequest = { delete = false }, title = { Text("删除这条心绪？") }, text = { Text("删除后可以在短时间内撤销。") }, confirmButton = { TextButton(onClick = { delete = false; onDelete(entry) }) { Text("删除") } }, dismissButton = { TextButton(onClick = { delete = false }) { Text("取消") } })
    }
}
