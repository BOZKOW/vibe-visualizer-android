package io.github.bozkow.vibevisualizer.audio

import kotlin.math.floor
import kotlin.math.pow

/** Three independent logarithmic ranges: 40–250 Hz, 250 Hz–4 kHz, 4–16 kHz. Bins always fit the actual Nyquist limit. */
internal class FrequencyBands(val count: Int, sampleRate: Int, private val fftSize: Int = 2048) {
    val edges = IntArray(count + 1)
    val ranges = IntArray(4) { it * count / 3 }
    init {
        val maxBin = fftSize / 2 - 1
        val binHz = sampleRate.coerceAtLeast(1).toDouble() / fftSize
        val limits = doubleArrayOf(40.0, 250.0, 4000.0, minOf(16000.0, maxBin * binHz))
        for (band in 0..2) {
            val from = ranges[band]; val until = ranges[band + 1]
            val hi = limits[band + 1].coerceAtMost(maxBin * binHz).coerceAtLeast(binHz)
            val lo = limits[band].coerceAtMost(hi).coerceAtLeast(binHz)
            for (i in from..until) {
                val frac = if (until > from) (i - from).toDouble() / (until - from) else 0.0
                edges[i] = floor(lo * (hi / lo).pow(frac) / binHz).toInt().coerceIn(1, maxBin)
            }
        }
    }
    /** Overlapping crossover gains, applied to untouched spectral bins. No artificial trough at a range boundary. */
    fun gainAt(index: Int, gains: FloatArray): Float {
        fun center(band: Int) = (ranges[band] + ranges[band + 1] - 1) * .5f
        if (index <= center(0)) return gains[0]
        if (index >= center(2)) return gains[2]
        val band = if (index < center(1)) 0 else 1
        val x = ((index - center(band)) / (center(band + 1) - center(band))).coerceIn(0f, 1f)
        val k = x * x * (3f - 2f * x)
        // Interpolate amplification in dB, so a quiet range cannot create an abrupt tall shelf beside it.
        return kotlin.math.exp(kotlin.math.ln(gains[band].coerceAtLeast(.001f)) * (1f - k) +
            kotlin.math.ln(gains[band + 1].coerceAtLeast(.001f)) * k)
    }

}
