package io.github.bozkow.vibevisualizer.lyrics

import io.github.bozkow.vibevisualizer.audio.Analyser
import io.github.bozkow.vibevisualizer.audio.AutoGain
import io.github.bozkow.vibevisualizer.audio.FrequencyBands
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class FrequencyBandsTest {
    @Test fun `bass mids and treble respond to their own test tones at different sample rates`() {
        for (rate in listOf(44100, 48000, 96000)) for (n in listOf(24, 48, 128)) {
            val bands = FrequencyBands(n, rate)
            for ((wanted, hz) in listOf(100, 1000, 8000).withIndex()) {
                val data = FloatArray(1024)
                Analyser().compute(FloatArray(2048) { sin(2 * PI * hz * it / rate).toFloat() * .3f }, 0f, data)
                val peakBin = data.indices.maxBy { data[it] }
                val peakBar = (0 until n).firstOrNull { i -> peakBin >= bands.edges[i] && peakBin < maxOf(bands.edges[i] + 1, bands.edges[i + 1]) }
                assertNotNull("tone $hz Hz at $rate Hz with $n bars", peakBar)
                assertTrue(peakBar!! >= bands.ranges[wanted] && peakBar < bands.ranges[wanted + 1])
            }
        }
    }
    @Test fun `loud bass does not reduce another ranges automatic gain`() {
        val gains = Array(3) { AutoGain() }
        val mids = FloatArray(12) { .1f }
        val before = gains[1].gain(mids, mids.size, 16.7)
        repeat(120) { gains[0].gain(FloatArray(12) { 1f }, 12, 16.7) }
        val after = gains[1].gain(mids, mids.size, 0.0)
        assertEquals(before, after, .00001f)
        assertTrue(gains[0].gain(FloatArray(12) { 1f }, 12, 0.0) < after)
    }
    @Test fun `all ranges stay inside the spectrum even on low sample rate recordings`() {
        val bands = FrequencyBands(48, 16000)
        assertTrue(bands.edges.all { it in 1..1023 })
        assertTrue((1 until bands.edges.size).all { bands.edges[it - 1] <= bands.edges[it] })
    }
}
