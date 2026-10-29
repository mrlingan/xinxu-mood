@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.xinxu.mood.ui

import android.os.SystemClock
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.xinxu.mood.haptics.BreathingVibration
import com.xinxu.mood.haptics.BreathingWaveform
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xinxu.mood.data.ExerciseSession
import kotlinx.coroutines.delay

@Composable fun ExerciseScreen(type: String, hapticsEnabled: Boolean, breezeLevel: Int, onLevelChange: (Int) -> Unit, onHapticsChange: (Boolean) -> Unit, onDismiss: () -> Unit, onSave: (ExerciseSession) -> Unit) {
    val sessionId = rememberSaveable { java.util.UUID.randomUUID().toString() }
    var before by rememberSaveable { mutableIntStateOf(3) }; var after by rememberSaveable { mutableIntStateOf(3) }; var stage by rememberSaveable { mutableIntStateOf(0) }
    var duration by rememberSaveable { mutableIntStateOf(60) }; var start by rememberSaveable { mutableLongStateOf(0L) }; var elapsed by rememberSaveable { mutableIntStateOf(0) }
    var event by rememberSaveable { mutableStateOf("") }; var thought by rememberSaveable { mutableStateOf("") }; var feeling by rememberSaveable { mutableStateOf("") }; var next by rememberSaveable { mutableStateOf("") }
    var breathingScale by remember { mutableFloatStateOf(.65f) }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val lifecycleState by lifecycle.lifecycle.currentStateFlow.collectAsState()
    val context = LocalContext.current
    val vibration = remember(context) { BreathingVibration(context) }
    BreezeDialog(onDismiss = onDismiss) { close ->
        DisposableEffect(lifecycle, lifecycleState, type, stage, hapticsEnabled, breezeLevel, start, duration) {
            // Cancel synchronously on pause, including screen-off and app switching.
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP || event == Lifecycle.Event.ON_DESTROY) vibration.stop()
            }
            lifecycle.lifecycle.addObserver(observer)
            if (type == "呼吸练习" && stage == 1 && hapticsEnabled && lifecycleState.isAtLeast(Lifecycle.State.RESUMED)) {
                vibration.start((SystemClock.elapsedRealtime() - start).coerceAtLeast(0), duration * 1000L, breezeLevel)
            }
            onDispose {
                lifecycle.lifecycle.removeObserver(observer)
                vibration.stop()
            }
        }
        LaunchedEffect(stage, lifecycleState) {
            if (stage != 1 || !lifecycleState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) return@LaunchedEffect
            while (true) {
                val elapsedMs = (SystemClock.elapsedRealtime() - start).coerceAtLeast(0)
                elapsed = (elapsedMs / 1000).toInt().coerceIn(0, 86400)
                if (type == "呼吸练习") {
                    if (elapsed >= duration) { stage = 2; break }
                    breathingScale = .65f + .35f * BreathingWaveform.progressAt(elapsedMs)
                }
                delay(if (type == "呼吸练习") 50 else 200)
            }
        }
        Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = { TopAppBar(title = { Text(type) }, navigationIcon = { TextButton(onClick = close) { Text("关闭") } }) }) { padding ->
            Column(Modifier.padding(padding).imePadding().fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                if (stage == 0) {
                    PageTitle("给自己一点时间", "无需达到任何目标，按舒服的节奏来")
                    Text("此刻，想调节的情绪有多强烈？ $before / 5")
                    Slider(before.toFloat(), { before = it.toInt() }, valueRange = 1f..5f, steps = 3)
                    if (type == "呼吸练习") { Text("选择练习时长"); Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { listOf(60, 120, 180).forEach { FilterChip(duration == it, { duration = it }, label = { Text("${it / 60} 分钟") }) } }; Text("跟随圆形舒适地吸气与呼气，无需屏息；感到不适时可以随时结束。"); BreathHapticsToggle(hapticsEnabled, vibration.supported, breezeLevel, onHapticsChange, onLevelChange) }
                    else Text(if (type == "情绪整理") "把事件、想法、感受和下一步分开写下来。所有内容都可以跳过。" else "暂时停一停，用感官留意身边的事物。无需急着改变心情。")
                    Button(onClick = { start = SystemClock.elapsedRealtime(); elapsed = 0; stage = 1 }, modifier = Modifier.fillMaxWidth()) { Text("开始") }
                } else if (stage == 1) {
                    if (type == "呼吸练习") {
                        val inhale = elapsed % 10 < 4
                        val scale = breathingScale
                        Box(Modifier.fillMaxWidth().height(290.dp), contentAlignment = Alignment.Center) { Box(Modifier.size(220.dp).scale(scale).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) { Text(if (inhale) "缓缓吸气" else "慢慢呼气", fontSize = 25.sp, color = MaterialTheme.colorScheme.primary) } }
                        Text("剩余 ${(duration - elapsed).coerceAtLeast(0)} 秒", Modifier.align(Alignment.CenterHorizontally))
                        BreathHapticsToggle(hapticsEnabled, vibration.supported, breezeLevel, onHapticsChange, onLevelChange)
                    } else if (type == "情绪整理") {
                        Text("把心里的线，慢慢理清", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                        listOf(Triple("发生了什么？", event, { v: String -> event = v }), Triple("我当时怎么想？", thought, { v: String -> thought = v }), Triple("我感受到什么？", feeling, { v: String -> feeling = v }), Triple("下一步能做什么小事？", next, { v: String -> next = v })).forEach { (label, value, change) -> OutlinedTextField(value, { if (it.length <= 10000) change(it) }, Modifier.fillMaxWidth(), label = { Text(label) }, minLines = 3) }
                    } else {
                        Text("回到此时，此地", fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                        listOf("5 · 找到五样你看见的东西", "4 · 感受四种可以触摸的质地", "3 · 留意三种周围的声音", "2 · 留意两种气味，或想起熟悉的气味", "1 · 说一句此刻想对自己说的话").forEach { SoftCard { Text(it) } }
                        Text("可以睁着眼睛，跳过任何不适合你的步骤。")
                    }
                    Button(onClick = { stage = 2 }, Modifier.fillMaxWidth()) { Text("结束练习，看看此刻") }
                } else {
                    PageTitle("谢谢你陪了自己一会儿", "无论心情有没有变化，这段时间都属于你")
                    Text("现在，这份情绪的强度是？ $after / 5")
                    Slider(after.toFloat(), { after = it.toInt() }, valueRange = 1f..5f, steps = 3)
                    Text("练习前 $before → 练习后 $after，仅记录你的主观感受。")
                    Button(onClick = { onSave(ExerciseSession(id = sessionId, type = type, seconds = elapsed.coerceAtMost(86400), before = before, after = after, notes = if (type == "情绪整理") "事件：$event\n想法：$thought\n感受：$feeling\n下一步：$next" else "")) }, modifier = Modifier.fillMaxWidth()) { Text("保存这次练习") }
                    TextButton(onClick = close) { Text("直接离开") }
                }
            }
        }
    }
}

@Composable private fun BreathHapticsToggle(enabled: Boolean, supported: Boolean, level: Int, onChange: (Boolean) -> Unit, onLevelChange: (Int) -> Unit) {
    SoftCard {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("微风轻振")
            Text(if (supported) "默认关闭 · 随呼吸轻轻起伏" else "此设备不支持轻柔振幅调节", style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = enabled && supported, onCheckedChange = onChange, enabled = supported)
    }
    if (enabled && supported) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("羽毛", "微风", "轻拂").forEachIndexed { i, label -> FilterChip(level == i, { onLevelChange(i) }, label = { Text(label) }) } }
        Text("从羽毛档开始，按手中的感觉选择。", style = MaterialTheme.typography.bodySmall)
    }
    }
}
