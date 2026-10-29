package com.xinxu.mood.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable fun BreezeDialog(onDismiss: () -> Unit, canDismiss: Boolean = true, content: @Composable (close: () -> Unit) -> Unit) {
    var visible by remember { mutableStateOf(false) }; val scope = rememberCoroutineScope()
    val close: () -> Unit = { if (canDismiss) scope.launch { visible = false; delay(220); onDismiss() } }
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        LaunchedEffect(Unit) { visible = true }
        Box(Modifier.fillMaxSize()) {
            AnimatedVisibility(visible, enter = fadeIn(tween(220)) + slideInVertically(spring(dampingRatio = .92f, stiffness = 380f)) { it / 10 }, exit = fadeOut(tween(180)) + slideOutVertically(tween(200)) { it / 12 }) { content(close) }
        }
    }
}
