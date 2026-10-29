package com.xinxu.mood.haptics

import android.content.Context
import android.os.Build
import android.media.AudioAttributes
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class BreathingVibration(context: Context) {
    @Suppress("DEPRECATION")
    private val vibrator = if (Build.VERSION.SDK_INT >= 31) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        context.getSystemService(Vibrator::class.java)
    }

    val supported: Boolean = vibrator?.let { it.hasVibrator() && it.hasAmplitudeControl() } == true

    fun start(elapsedMs: Long, durationMs: Long, level: Int = 0) {
        if (!supported) return // Never substitute constant-strength vibration.
        val pattern = BreathingWaveform.remaining(elapsedMs, durationMs, level)
        if (pattern.timings.isNotEmpty()) {
            // All amplitudes stay above zero. One finite effect avoids gaps between phases
            // and ends at the session deadline even if UI updates are delayed.
            val effect = VibrationEffect.createWaveform(pattern.timings, pattern.amplitudes, -1)
            // Classify interactive guidance as touch feedback, so notification intensity
            // does not amplify a barely perceptible breath into a strong alert.
            if (Build.VERSION.SDK_INT >= 33) vibrator?.vibrate(effect, VibrationAttributes.Builder().setUsage(VibrationAttributes.USAGE_TOUCH).build())
            else @Suppress("DEPRECATION") vibrator?.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).build())
        }
    }

    fun stop() { vibrator?.cancel() }
}
