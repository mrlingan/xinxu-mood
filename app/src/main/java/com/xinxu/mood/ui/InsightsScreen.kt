package com.xinxu.mood.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xinxu.mood.data.*
import java.time.*

@Composable fun InsightsScreen(entries: List<MoodEntry>, exercises: List<ExerciseSession>) {
    var days by rememberSaveable { mutableIntStateOf(7) }; val daily = Insights.daily(entries, days); val start = LocalDate.now().minusDays(days.toLong() - 1)
    fun inWindow(time: Long): Boolean { val date = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDate(); return !date.isBefore(start) && !date.isAfter(LocalDate.now()) }
    val relevant = entries.filter { inWindow(it.occurredAt) }; val sessions = exercises.filter { inWindow(it.finishedAt) }
    val color = MaterialTheme.colorScheme.primary; val grid = MaterialTheme.colorScheme.outlineVariant
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(22.dp, 8.dp, 22.dp, 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageTitle("慢慢读懂自己", "看见变化，也允许起伏") }
        item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { listOf(7, 30).forEach { FilterChip(days == it, { days = it }, label = { Text("近 $it 天") }) } } }
        item { SoftCard { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text("${relevant.size}", fontSize = 32.sp, fontWeight = FontWeight.Bold); Text("次心绪记录") }; Column { Text("${daily.count { it.count > 0 }}", fontSize = 32.sp, fontWeight = FontWeight.Bold); Text("天留下足迹") }; Column { Text("${sessions.size}", fontSize = 32.sp, fontWeight = FontWeight.Bold); Text("次调节练习") } } } }
        item { SoftCard {
            Text("整体感受变化", fontWeight = FontWeight.SemiBold)
            Text("5 较舒服 ↑     1 较难受 ↓", style = MaterialTheme.typography.labelSmall)
            Canvas(Modifier.fillMaxWidth().height(160.dp).padding(vertical = 12.dp, horizontal = 8.dp)) {
                val width = size.width; val height = size.height
                (0..4).forEach { drawLine(grid, Offset(0f, it * height / 4), Offset(width, it * height / 4), 1.dp.toPx()) }
                fun point(i: Int, score: Float) = Offset(i * width / (days - 1), (5 - score) * height / 4)
                daily.forEachIndexed { i, d -> d.average?.let { score ->
                    val p = point(i, score)
                    if (i > 0) daily[i - 1].average?.let { previous -> drawLine(color, point(i - 1, previous), p, 3.dp.toPx(), StrokeCap.Round) }
                    drawCircle(color, if (days == 7) 5.dp.toPx() else 3.dp.toPx(), p)
                } }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(start.toString(), style = MaterialTheme.typography.labelSmall); Text(LocalDate.now().toString(), style = MaterialTheme.typography.labelSmall) }
            Text("每天按记录的整体感受取平均。未记录的日期留空，不连接跨日缺口。", style = MaterialTheme.typography.bodySmall)
            if (relevant.isEmpty()) Text("记录后，这里会出现你的变化轨迹。", color = color)
        } }
        item { SoftCard {
            Text("这段时间的情绪", fontWeight = FontWeight.SemiBold)
            val counts = relevant.flatMap { it.emotionMap().keys }.groupingBy { it }.eachCount().toList().sortedByDescending { it.second }
            if (counts.isEmpty()) Text("还没有足够的记录")
            counts.forEach { (name, count) -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(name); Text("$count 次") }; LinearProgressIndicator(progress = { count.toFloat() / relevant.size.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth()) }
            Text("一条记录可包含多种情绪，次数可能超过记录总数。", style = MaterialTheme.typography.bodySmall)
        } }
        item { SoftCard { Text("经常出现的场景", fontWeight = FontWeight.SemiBold)
            val tags = relevant.flatMap { it.tags.split("|").filter(String::isNotBlank) }.groupingBy { it }.eachCount().toList().sortedByDescending { it.second }.take(5)
            if (tags.isEmpty()) Text("记录时添加场景标签，可在这里回顾。")
            tags.forEach { (tag, count) -> Text("$tag  ·  $count 次") }
            Text("出现次数描述记录中的关联，不代表情绪变化的原因。", style = MaterialTheme.typography.bodySmall)
        } }
        item { SectionTitle("最近的练习") }
        if (sessions.isEmpty()) item { EmptyCard("给自己一分钟，也是一种照顾。") }
        sessions.take(10).forEach { session -> item { SoftCard { Text(session.type, fontWeight = FontWeight.SemiBold); Text("${displayTime(session.finishedAt)} · ${session.seconds} 秒", style = MaterialTheme.typography.bodySmall); Text("自评强度：${session.before} → ${session.after}"); if (session.notes.isNotBlank()) Text(session.notes); Text("这只是你在练习前后的主观记录。", style = MaterialTheme.typography.labelSmall) } } }
    }
}
