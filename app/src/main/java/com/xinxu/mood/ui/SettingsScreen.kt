@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.xinxu.mood.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xinxu.mood.data.Settings

@Composable fun SettingsScreen(settings: Settings, count: Int, exerciseCount: Int, onSettings: (Settings) -> Unit, onLock: (Boolean) -> Unit, onReminder: (Boolean) -> Unit, onExport: () -> Unit, onImport: () -> Unit, onClear: () -> Unit) {
    var tag by rememberSaveable { mutableStateOf("") }; var clear by remember { mutableStateOf(false) }; val context = LocalContext.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(22.dp, 8.dp, 22.dp, 30.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageTitle("我的安静角落", "心绪日记 · 完全离线 · 由你掌握") }
        item { SoftCard { Text("你与自己相处的足迹", fontWeight = FontWeight.SemiBold); Text("$count 条心绪 · $exerciseCount 次练习"); Text("所有内容保存在当前设备。卸载或清除应用数据会移除记录，建议定期导出备份。", style = MaterialTheme.typography.bodySmall) } }
        item { SectionTitle("使用偏好") }
        item { SoftCard {
            Text("外观", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("system" to "跟随系统", "light" to "浅色", "dark" to "深色").forEach { (value, name) -> FilterChip(settings.theme == value, { onSettings(settings.copy(theme = value)) }, label = { Text(name) }) } }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column(Modifier.weight(1f)) { Text("应用锁"); Text("使用系统锁屏凭据，回到后台后锁定", style = MaterialTheme.typography.bodySmall) }; Switch(settings.locked, onLock) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column(Modifier.weight(1f)) { Text("每日温柔提醒"); Text("通知不会展示情绪或日记内容", style = MaterialTheme.typography.bodySmall) }; Switch(settings.reminder, onReminder) }
            OutlinedButton(onClick = { TimePickerDialog(context, { _, h, m -> onSettings(settings.copy(hour = h, minute = m)) }, settings.hour, settings.minute, true).show() }) { Text("提醒时间  %02d:%02d".format(settings.hour, settings.minute)) }
            Text("提醒为本机通知，可能因系统省电策略而延迟。", style = MaterialTheme.typography.bodySmall)
        } }
        item { SectionTitle("我的场景标签") }
        item { SoftCard {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { settings.tags.sorted().forEach { name -> InputChip(selected = false, onClick = { onSettings(settings.copy(tags = settings.tags - name)) }, label = { Text("$name ×") }) } }
            Text("点击标签移除；历史记录中的标签会保留。", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(tag, { if (it.length <= 12 && !it.contains('|') && !it.contains('\n')) tag = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("添加场景（最多 12 字）") })
            TextButton(enabled = tag.trim().isNotEmpty() && settings.tags.size < 30, onClick = { onSettings(settings.copy(tags = settings.tags + tag.trim())); tag = "" }) { Text("添加标签") }
        } }
        item { SectionTitle("数据由你掌握") }
        item { SoftCard {
            OutlinedButton(onClick = onExport, Modifier.fillMaxWidth()) { Text("导出密码加密备份") }
            OutlinedButton(onClick = onImport, Modifier.fillMaxWidth()) { Text("从备份恢复") }
            Text("备份采用 AES-GCM 加密。请记住密码；恢复会替换当前的心绪、练习记录和附件。", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { clear = true }) { Text("清空所有记录", color = MaterialTheme.colorScheme.error) }
        } }
        item { SoftCard { Text("关于心绪日记  1.1.0", fontWeight = FontWeight.SemiBold); Text("这是一个帮助你记录、观察和照顾情绪的个人工具。没有账号、广告或云端同步，应用不申请网络权限。", style = MaterialTheme.typography.bodyMedium); Text("练习与趋势用于自我观察，不提供疾病诊断。", style = MaterialTheme.typography.bodySmall) } }
    }
    if (clear) AlertDialog(onDismissRequest = { clear = false }, title = { Text("清空全部记录？") }, text = { Text("将永久删除 $count 条情绪记录和 $exerciseCount 次练习。此操作无法撤销，请先导出备份。") }, confirmButton = { TextButton(onClick = { clear = false; onClear() }) { Text("确认清空", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { clear = false }) { Text("取消") } })
}
