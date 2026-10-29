@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.xinxu.mood.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xinxu.mood.data.*
import java.time.*

@Composable fun RecordsScreen(entries: List<MoodEntry>, onEntry: (MoodEntry) -> Unit) {
    var search by rememberSaveable { mutableStateOf("") }; var emotion by rememberSaveable { mutableStateOf<String?>(null) }; var tag by rememberSaveable { mutableStateOf<String?>(null) }; var day by rememberSaveable { mutableStateOf<String?>(null) }
    var monthText by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }; val month = YearMonth.parse(monthText)
    fun date(e: MoodEntry) = Instant.ofEpochMilli(e.occurredAt).atZone(ZoneId.systemDefault()).toLocalDate()
    val filtered = entries.filter { e -> (day == null || date(e).toString() == day) && (emotion == null || emotion in e.emotionMap()) && (tag == null || tag in e.tags.split("|")) && (search.isBlank() || listOf(e.note, e.thought, e.need, e.tags, e.emotions).any { it.contains(search, true) }) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(22.dp, 8.dp, 22.dp, 100.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageTitle("心绪足迹", "每一次记录，都是与自己的相遇") }
        item { OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text("搜索文字、情绪或场景") }, singleLine = true) }
        item { SoftCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { TextButton(onClick = { monthText = month.minusMonths(1).toString() }) { Text("‹") }; Text("${month.year}年 ${month.monthValue}月", Modifier.padding(top = 12.dp)); TextButton(onClick = { monthText = month.plusMonths(1).toString() }) { Text("›") } }
            Row { listOf("一", "二", "三", "四", "五", "六", "日").forEach { Box(Modifier.weight(1f), contentAlignment = androidx.compose.ui.Alignment.Center) { Text(it, style = MaterialTheme.typography.labelSmall) } } }
            val first = month.atDay(1).dayOfWeek.value - 1; val total = month.lengthOfMonth(); val dates = entries.groupingBy(::date).eachCount()
            (0 until (first + total + 6) / 7).forEach { week -> Row {
                (0..6).forEach { weekday -> val number = week * 7 + weekday - first + 1
                    Box(Modifier.weight(1f).height(42.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        if (number in 1..total) { val d = month.atDay(number); val selected = day == d.toString()
                            TextButton(onClick = { day = if (selected) null else d.toString() }, contentPadding = PaddingValues(0.dp)) { Text("$number${if (dates.containsKey(d)) "·" else ""}", color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary) }
                        }
                    }
                }
            } }
            if (day != null) TextButton(onClick = { day = null }) { Text("清除日期筛选 · $day") } else Text("带 · 的日期有记录，点击可筛选", style = MaterialTheme.typography.labelSmall)
        } }
        item { FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { FilterChip(emotion == null, { emotion = null }, label = { Text("全部情绪") }); emotionNames.forEach { name -> FilterChip(emotion == name, { emotion = if (emotion == name) null else name }, label = { Text(name) }) } } }
        val tags = entries.flatMap { it.tags.split("|") }.filter { it.isNotBlank() }.distinct()
        if (tags.isNotEmpty()) item { FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { tags.forEach { name -> FilterChip(tag == name, { tag = if (tag == name) null else name }, label = { Text(name) }) } } }
        item { Text("找到 ${filtered.size} 条记录", style = MaterialTheme.typography.labelMedium) }
        if (filtered.isEmpty()) item { EmptyCard("这里还没有匹配的记录。可以调整筛选，或记下此刻的感受。") }
        items(filtered, key = { it.id }) { EntryCard(it) { onEntry(it) } }
    }
}
