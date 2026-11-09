package io.github.bozkow.vibevisualizer.audio

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/** Auto sensitivity: how much the bars are turned up so the music fills them, loud or quiet.
 *
 *  It follows the louder bars together (the mean of the top quarter), not the single loudest bar of a frame: one hit
 *  turned everything down for seconds. It rises in about a third of a second and falls over about four, and it holds
 *  through silence (an intro, a pause, the gap between songs), so the next sound isn't met with a huge boost; quiet
 *  music is turned up at most [MAX] times. */
class AutoGain {
    var level = .3f; private set
    private var sorted = FloatArray(0)

    /** The gain for this frame's bars [raw] (0 and up), [dt] ms after the last. */
    fun gain(raw: FloatArray, n: Int, dt: Double): Float {
        val l = topQuarter(raw, n)
        if (l > QUIET) level += (l - level) * (1 - exp(-dt / (if (l > level) RISE_MS else FALL_MS))).toFloat()
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
        const val TARGET = .62f     // where that level comes to: the loudest bars a little higher, near the top
        const val MAX = 4f
        const val QUIET = .02f      // under this it's silence: the level holds
        const val RISE_MS = 300.0
        const val FALL_MS = 4000.0
    }
}
