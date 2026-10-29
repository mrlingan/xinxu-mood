package com.xinxu.mood.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.xinxu.mood.R

val moodArt = listOf(R.drawable.xx_mood_1, R.drawable.xx_mood_2, R.drawable.xx_mood_3, R.drawable.xx_mood_4, R.drawable.xx_mood_5)
val emotionArt = listOf(R.drawable.xx_happy, R.drawable.xx_calm, R.drawable.xx_sad, R.drawable.xx_anxious, R.drawable.xx_angry, R.drawable.xx_tired, R.drawable.xx_hope, R.drawable.xx_lonely)
@Composable fun Art(@DrawableRes id: Int, description: String? = null, modifier: Modifier = Modifier.size(28.dp)) {
    Image(painterResource(id), description, modifier)
}
fun Modifier.softClick(onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .96f else 1f, spring(dampingRatio = .85f, stiffness = 550f), label = "轻按回弹")
    this.scale(scale).clickable(interactionSource = interaction, indication = null, onClick = onClick)
}
