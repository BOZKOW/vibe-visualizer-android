package io.github.bozkow.vibevisualizer.audio

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/** Track-level normalization, not a beat follower: learn the initial level, then adapt over seconds.
 * The spectrum remains free to express the music's dynamics. Each frequency range owns an instance.
 * Silence holds the level and quiet recordings have bounded gain; peaks use a soft output ceiling. */
class AutoGain {
    var level = .3f; private set
    private var sorted = FloatArray(0)
    private var initialized = false
    private var heardMs = 0.0

    /** The gain for this frame's bars [raw] (0 and up), [dt] ms after the last. */
    fun gain(raw: FloatArray, n: Int, dt: Double): Float {
        val l = topQuarter(raw, n)
        if (l > QUIET && dt > 0) {
            if (!initialized) { level = max(l, TARGET / MAX); initialized = true }
            val settling = heardMs < 1500.0
            val tau = if (l > level) { if (settling) 350.0 else RISE_MS } else { if (settling) 900.0 else FALL_MS }
            level += (l - level) * (1 - exp(-dt / tau)).toFloat()
            heardMs += dt
        }
        return min(MAX, TARGET / max(level, TARGET / MAX))
    }

    private fun topQuarter(raw: FloatArray, n: Int): Float {
        if (n <= 0) return 0f
        if (sorted.size < n) sorted = FloatArray(n)
        System.arraycopy(raw, 0, sorted, 0, n); sorted.sort(0, n)
        val k = max(1, n / 4); var s = 0f
        for (i in n - k until n) s += sorted[i]
        return s / k
    }

    companion object {
        /** Bounded and strictly increasing: loud passages retain detail instead of hitting a flat clip. */
        fun output(value: Float): Float = 1.1f * kotlin.math.tanh(value.coerceAtLeast(0f) / 1.1f)
        const val TARGET = .62f     // where that level comes to: the loudest bars a little higher, near the top
        const val MAX = 6f
        const val QUIET = .02f      // under this it's silence: the level holds
        const val RISE_MS = 6000.0
        const val FALL_MS = 8000.0
    }
}
