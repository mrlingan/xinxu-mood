package com.xinxu.mood.ui

import androidx.compose.foundation.layout.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xinxu.mood.*
import com.xinxu.mood.R
import com.xinxu.mood.data.*
import kotlinx.coroutines.launch

@Composable fun MoodApp(vm: MoodViewModel, busy: Boolean, onExport: () -> Unit, onImport: () -> Unit, onLock: (Boolean) -> Unit, onReminder: (Boolean) -> Unit) {
    val greeting by vm.greeting.collectAsStateWithLifecycle()
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_START) vm.greet() }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    val pageState = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    val entries by vm.entries.collectAsStateWithLifecycle(); val exercises by vm.exercises.collectAsStateWithLifecycle(); val settings by vm.settings.collectAsStateWithLifecycle(); val message by vm.message.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }; var editingId by rememberSaveable { mutableStateOf<String?>(null) }; val editing = entries.find { it.id == editingId }; var editor by rememberSaveable { mutableStateOf(false) }; var exercise by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }; val scope = rememberCoroutineScope()
    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); vm.message.value = null } }
    androidx.activity.compose.BackHandler(busy) { }
    BackHandlerCompat(!busy && (editor || exercise != null)) { editor = false; exercise = null }
    Scaffold(containerColor = MaterialTheme.colorScheme.background, snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
        Surface(Modifier.navigationBarsPadding().padding(horizontal = 18.dp, vertical = 8.dp), shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 3.dp) {
            Row(Modifier.fillMaxWidth().padding(7.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                val names = listOf("今天", "记录", "趋势", "我的"); val icons = listOf(R.drawable.xx_home, R.drawable.xx_diary, R.drawable.xx_insights, R.drawable.xx_profile)
                names.forEachIndexed { i, name ->
                    val scale by animateFloatAsState(if (tab == i) 1.12f else 1f, spring(.75f, 430f), label = "导航回弹")
                    val color by animateColorAsState(if (tab == i) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, tween(220), label = "导航底色")
                    Surface(Modifier.weight(1f).softClick { if (!busy) tab = i }, shape = RoundedCornerShape(23.dp), color = color) {
                        Column(Modifier.padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) { Art(icons[i], modifier = Modifier.size(27.dp).graphicsLayer { scaleX = scale; scaleY = scale }); Spacer(Modifier.height(3.dp)); Text(name, style = MaterialTheme.typography.labelSmall, color = if (tab == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
    }, floatingActionButton = { if (tab == 1) FloatingActionButton(onClick = { if (!busy) { editingId = null; editor = true } }, containerColor = MaterialTheme.colorScheme.secondaryContainer) { Art(R.drawable.xx_plus, "记录此刻", Modifier.size(30.dp)) } }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            AnimatedContent(targetState = tab, transitionSpec = { (fadeIn(tween(240, 35)) + slideInHorizontally(spring(.92f, 430f)) { if (targetState > initialState) it / 14 else -it / 14 }).togetherWith(fadeOut(tween(120))) }, label = "页面轻移") { page ->
            pageState.SaveableStateProvider(page) { when (page) {
                0 -> HomeScreen(entries, exercises, greeting, onRecord = { editingId = null; editor = true }, onEntry = { editingId = it.id; editor = true }, onExercise = { exercise = it }, onAll = { tab = 1 })
                1 -> RecordsScreen(entries, onEntry = { editingId = it.id; editor = true })
                2 -> InsightsScreen(entries, exercises)
                3 -> SettingsScreen(settings, entries.size, exercises.size, onSettings = { value -> scope.launch { runCatching { vm.update(value) }.onFailure { vm.message.value = "设置保存失败" } } }, onLock, onReminder, onExport, onImport, onClear = { vm.clearRecords() })
            }
            } }

        }
    }
    if (busy) androidx.compose.ui.window.Dialog(onDismissRequest = {}, properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
        SoftCard { CircularProgressIndicator(); Text("正在安全处理备份，请稍候…") }
    }
    if (editor && (editingId == null || editing != null)) EntryEditor(editing, settings.tags, onDismiss = { editor = false }, onSave = { vm.save(it) { editor = false } }, onDelete = { entry -> scope.launch {
        runCatching { vm.dao.delete(entry) }.onSuccess {
            editor = false
            if (snackbar.showSnackbar("记录已删除", "撤销", duration = SnackbarDuration.Long) == SnackbarResult.ActionPerformed) vm.dao.save(entry)
        }.onFailure { vm.message.value = "删除失败，请重试" }
    } })
    exercise?.let { type -> ExerciseScreen(type, hapticsEnabled = settings.breathHaptics, breezeLevel = settings.breezeLevel, onLevelChange = { level -> scope.launch { vm.update(settings.copy(breezeLevel = level)) } }, onHapticsChange = { enabled -> scope.launch { runCatching { vm.update(settings.copy(breathHaptics = enabled)) }.onFailure { vm.message.value = "震动设置保存失败" } } }, onDismiss = { exercise = null }, onSave = { vm.saveExercise(it) { exercise = null } }) }
}
@Composable private fun BackHandlerCompat(enabled: Boolean, action: () -> Unit) { androidx.activity.compose.BackHandler(enabled, action) }
