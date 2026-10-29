package com.xinxu.mood

import android.Manifest
import android.app.KeyguardManager
import android.content.Intent
import android.os.*
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.activity.compose.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.xinxu.mood.backup.*
import com.xinxu.mood.ui.*
import kotlinx.coroutines.*
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private var locked by mutableStateOf(true)
    private var external = false
    fun beginMediaSelection() { external = true }
    fun endMediaSelection() { external = false }
    override fun onStop() { super.onStop(); if (!external && !isChangingConfigurations) locked = true }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: MoodViewModel = viewModel()
            val settings by vm.settings.collectAsStateWithLifecycle()
            val ready by vm.loaded.collectAsStateWithLifecycle()
            LaunchedEffect(settings.locked) {
                if (settings.locked) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE) else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
            var password by remember { mutableStateOf("") }
            var backupMode by remember { mutableStateOf<String?>(null) }
            var busy by remember { mutableStateOf(false) }
            val credential = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> external = false; if (result.resultCode == RESULT_OK) locked = false }
            val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> external = false; lifecycleScope.launch { vm.update(settings.copy(reminder = granted)); if (!granted) vm.message.value = "未开启通知权限，提醒暂未启用" } }
            fun fail(error: Throwable) { vm.message.value = if (error is javax.crypto.AEADBadTagException) "密码不正确，或备份文件已损坏" else "操作失败：${error.message ?: "请重试"}" }
            val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
                external = false
                if (uri != null) lifecycleScope.launch {
                    busy = true
                    runCatching { withContext(Dispatchers.IO) {
                        requireNotNull(contentResolver.openOutputStream(uri)).use { BackupArchive(applicationContext).export(vm.dao.snapshot(), it, password.toCharArray()) }
                    } }.onSuccess { vm.message.value = "加密备份已导出，请妥善保存密码" }.onFailure(::fail)
                    password = ""; busy = false
                } else password = ""
            }
            val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                external = false
                if (uri != null) lifecycleScope.launch {
                    busy = true
                    runCatching { withContext(Dispatchers.IO) {
                        requireNotNull(contentResolver.openInputStream(uri)).use { BackupArchive(applicationContext).restore(it, password.toCharArray(), vm.dao) }
                    } }.onSuccess { vm.message.value = "恢复完成" }.onFailure(::fail)
                    password = ""; busy = false
                } else password = ""
            }
            val dark = settings.theme == "dark" || (settings.theme == "system" && androidx.compose.foundation.isSystemInDarkTheme())
            SideEffect {
                androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            XinXuTheme(settings.theme) {
                if (!ready) LoadingScreen() else if (settings.locked && locked) LockScreen {
                    val manager = getSystemService(KeyguardManager::class.java)
                    val intent = manager.createConfirmDeviceCredentialIntent("解锁心绪日记", "使用设备凭据查看个人记录")
                    if (intent != null) { external = true; credential.launch(intent) } else { vm.message.value = "请先在系统设置中启用设备锁屏" }
                } else MoodApp(vm, busy,
                    onExport = { backupMode = "export" }, onImport = { backupMode = "import" },
                    onLock = { enabled ->
                        val manager = getSystemService(KeyguardManager::class.java)
                        if (enabled && !manager.isDeviceSecure) vm.message.value = "请先设置系统锁屏密码"
                        else lifecycleScope.launch { vm.update(settings.copy(locked = enabled)); locked = false }
                    },
                    onReminder = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) { external = true; permission.launch(Manifest.permission.POST_NOTIFICATIONS) }
                        else lifecycleScope.launch { vm.update(settings.copy(reminder = enabled)) }
                    }
                )
                backupMode?.let { mode -> PasswordDialog(mode == "import", onDismiss = { backupMode = null }) { value ->
                    password = value; backupMode = null; external = true
                    if (mode == "export") export.launch("心绪备份-${LocalDate.now()}.xinxu") else import.launch(arrayOf("*/*"))
                } }
            }
        }
    }
}
