package com.xinxu.mood.haptics

import org.junit.Assert.*
import org.junit.Test

class BreathingWaveformTest {
    @Test fun envelopeRisesAndFallsLinearlyWithoutOffSegments() {
        assertEquals(1, BreathingWaveform.amplitudeAt(0))
        assertEquals(2, BreathingWaveform.amplitudeAt(2000))
        assertEquals(3, BreathingWaveform.amplitudeAt(4000))
        assertEquals(2, BreathingWaveform.amplitudeAt(7000))
        assertEquals(1, BreathingWaveform.amplitudeAt(10000))
        val pattern = BreathingWaveform.remaining(0, 180000)
        assertTrue(pattern.amplitudes.all { it in 1..3 })
        assertEquals(180000L, pattern.timings.sum())
        assertEquals(pattern.timings.size, pattern.amplitudes.size)
    }
    @Test fun strengthNeverExceedsGentlePeaks() {
        listOf(3, 6, 10).forEachIndexed { level, peak ->
            assertEquals(peak, BreathingWaveform.amplitudeAt(4000, level))
            assertTrue(BreathingWaveform.remaining(0, 60000, level).amplitudes.all { it in 1..peak })
        }
    }
    @Test fun resumedPatternStaysInPhaseAndEndsAtDeadline() {
        val pattern = BreathingWaveform.remaining(7343, 60000)
        assertEquals(BreathingWaveform.amplitudeAt(7343), pattern.amplitudes.first())
        assertEquals(60000L - 7343, pattern.timings.sum())
        assertTrue(pattern.timings.all { it in 1..50 })
    }
    @Test fun cycleJoinsHaveNoAmplitudeJump() {
        assertTrue(kotlin.math.abs(BreathingWaveform.amplitudeAt(3950) - BreathingWaveform.amplitudeAt(4000)) <= 2)
        assertTrue(kotlin.math.abs(BreathingWaveform.amplitudeAt(9950) - BreathingWaveform.amplitudeAt(10000)) <= 2)
    }
    @Test fun finishedSessionHasNoWaveform() {
        assertTrue(BreathingWaveform.remaining(60000, 60000).timings.isEmpty())
        assertTrue(BreathingWaveform.remaining(65000, 60000).amplitudes.isEmpty())
    }
}
