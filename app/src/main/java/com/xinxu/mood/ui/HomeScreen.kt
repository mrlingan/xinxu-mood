package com.xinxu.mood.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xinxu.mood.R
import com.xinxu.mood.data.*
import java.time.*
import java.time.format.DateTimeFormatter

@Composable fun HomeScreen(entries: List<MoodEntry>, exercises: List<ExerciseSession>, greeting: String, onRecord: () -> Unit, onEntry: (MoodEntry) -> Unit, onExercise: (String) -> Unit, onAll: () -> Unit) {
    val today = LocalDate.now(); val count = entries.count { Instant.ofEpochMilli(it.occurredAt).atZone(ZoneId.systemDefault()).toLocalDate() == today }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(22.dp, 8.dp, 22.dp, 90.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageTitle("给心情，一点空间", today.format(DateTimeFormatter.ofPattern("M月d日 · EEEE", java.util.Locale.CHINESE))) }
        item { Surface(shape = RoundedCornerShape(30.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Art(R.drawable.xx_spark, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("今日的小小来信", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
                Art(R.drawable.xx_friend, modifier = Modifier.fillMaxWidth().height(155.dp))
                Text(greeting, fontSize = 20.sp, lineHeight = 29.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(18.dp))
                Button(onClick = onRecord, modifier = Modifier.fillMaxWidth().height(50.dp)) { Art(R.drawable.xx_diary, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(8.dp)); Text("记录此刻") }
                Spacer(Modifier.height(10.dp)); Text(if (count == 0) "留一句话，也可以留一张喜欢的照片" else "今天已收藏 $count 次心绪", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        } }
        item { SectionTitle("先让自己缓一缓") }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(Triple("呼吸练习", "像微风一样\n慢慢呼吸", R.drawable.xx_breath), Triple("情绪整理", "理一理想法\n看见需要", R.drawable.xx_sort), Triple("短暂停顿", "回到此刻\n感受周围", R.drawable.xx_pause)).forEach { (name, hint, art) ->
                Surface(modifier = Modifier.weight(1f).softClick { onExercise(name) }, shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) { Art(art, modifier = Modifier.size(38.dp)); Text(name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium); Text(hint, style = MaterialTheme.typography.bodySmall, lineHeight = 21.sp) }
                }
            }
        } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { SectionTitle("最近的心绪"); TextButton(onClick = onAll) { Text("查看全部"); Art(R.drawable.xx_arrow, modifier = Modifier.size(20.dp)) } } }
        if (entries.isEmpty()) item { EmptyCard("试着用一句话，留下今天的感受。记录只保存在你的设备里。") }
        items(entries.take(3), key = { it.id }) { EntryCard(it) { onEntry(it) } }
        if (exercises.isNotEmpty()) item { Text("你已为自己留出 ${exercises.size} 次练习时间", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
