package com.xinxu.mood.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xinxu.mood.data.*
import com.xinxu.mood.R
import com.xinxu.mood.media.Attachments
import java.time.*
import java.time.format.DateTimeFormatter

fun displayTime(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MM月dd日 HH:mm"))
@Composable fun PageTitle(title: String, subtitle: String) { Column(Modifier.padding(top = 12.dp, bottom = 8.dp)) { Text(title, fontSize = 30.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp)); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) } }
@Composable fun SectionTitle(text: String) { Text(text, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) }
@Composable fun SoftCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) { Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content) } }
@Composable fun EmptyCard(text: String) { SoftCard { Text("慢慢来，每一种感受都值得被看见", fontWeight = FontWeight.Medium); Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable fun EntryCard(entry: MoodEntry, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer) { Art(moodArt[(entry.feeling - 1).coerceIn(0, 4)], modifier = Modifier.padding(8.dp).size(44.dp)) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(feelingNames[entry.feeling - 1], fontWeight = FontWeight.SemiBold); Text(displayTime(entry.occurredAt), style = MaterialTheme.typography.labelSmall) }
                Text(entry.emotionMap().entries.joinToString(" · ") { "${it.key} ${it.value}" }, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                if (entry.note.isNotBlank()) Text(entry.note, maxLines = 2, style = MaterialTheme.typography.bodyMedium)
                val media = remember(entry.media) { runCatching { Attachments.decode(entry.media) }.getOrDefault(emptyList()) }
                if (media.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { media.take(3).forEach { MediaThumbnail(it, Modifier.size(54.dp).then(Modifier)) }; if (media.size > 3) Text("+${media.size - 3}", style = MaterialTheme.typography.labelSmall) }
                if (entry.tags.isNotBlank()) Text(entry.tags.replace("|", "  ·  "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
@Composable fun LoadingScreen() { Surface(Modifier.fillMaxSize()) { Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() } } }
@Composable fun LockScreen(unlock: () -> Unit) { Surface(Modifier.fillMaxSize()) { Column(Modifier.safeDrawingPadding().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) { Art(R.drawable.xx_lock, modifier = Modifier.size(72.dp)); Spacer(Modifier.height(24.dp)); Text("属于你的安静角落", fontSize = 28.sp); Spacer(Modifier.height(12.dp)); Text("验证设备凭据，查看心绪记录"); Spacer(Modifier.height(28.dp)); Button(onClick = unlock) { Text("解锁日记") } } } }
@Composable fun PasswordDialog(importing: Boolean, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var password by remember { mutableStateOf("") }; var repeat by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (importing) "恢复加密备份" else "导出加密备份") }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (importing) "恢复会替换当前所有情绪、练习记录和附件。请先导出当前数据。文件和密码校验通过后才会替换。" else "备份包含全部情绪、练习记录和图片、实况、视频附件。密码至少 8 位，遗忘后无法找回。主题、提醒和自定义标签设置不包含在备份内。")
        OutlinedTextField(password, { password = it }, label = { Text("备份密码") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
        if (!importing) OutlinedTextField(repeat, { repeat = it }, label = { Text("再次输入密码") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
    } }, confirmButton = { TextButton(onClick = { onConfirm(password) }, enabled = password.length >= 8 && (importing || password == repeat)) { Text(if (importing) "选择文件并恢复" else "选择保存位置") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}
