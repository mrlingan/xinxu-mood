package com.xinxu.mood.haptics

import kotlin.math.roundToInt

/** A continuous envelope: four seconds rising, six seconds falling, with no off segment. */
object BreathingWaveform {
    private const val MIN_AMPLITUDE = 1
    private val peaks = intArrayOf(3, 6, 10)
    private const val SAMPLE_MS = 50L

    fun progressAt(elapsedMs: Long): Float {
        val phase = Math.floorMod(elapsedMs, 10_000L)
        return if (phase < 4_000L) phase / 4_000f else (10_000L - phase) / 6_000f
    }

    fun amplitudeAt(elapsedMs: Long, level: Int = 0): Int =
        (MIN_AMPLITUDE + (peaks[level.coerceIn(0, 2)] - MIN_AMPLITUDE) * progressAt(elapsedMs)).roundToInt()

    data class Pattern(val timings: LongArray, val amplitudes: IntArray)

    /** Finite pattern, aligned to the session clock even when resumed mid-breath. */
    fun remaining(elapsedMs: Long, durationMs: Long, level: Int = 0): Pattern {
        require(durationMs in 1..180_000L)
        val offset = elapsedMs.coerceIn(0, durationMs)
        val remaining = durationMs - offset
        val count = ((remaining + SAMPLE_MS - 1) / SAMPLE_MS).toInt()
        return Pattern(
            LongArray(count) { i -> minOf(SAMPLE_MS, remaining - i * SAMPLE_MS) },
            IntArray(count) { i -> amplitudeAt(offset + i * SAMPLE_MS, level) }
        )
    }
}
