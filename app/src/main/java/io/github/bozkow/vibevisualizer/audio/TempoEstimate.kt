package io.github.bozkow.vibevisualizer.audio

import kotlin.math.*

/** Audio-only rhythm history. Fixed-time samples precede novelty detection, independent of UI FPS and bar gain. */
internal class TempoHistory {
    companion object { const val STEP_MS = 10.0; private const val CAP = 1600 }
    private val history = Array(3) { FloatArray(CAP) }
    private val previous = FloatArray(3)
    private val sampled = FloatArray(3)
    private val energy = FloatArray(3)
    private val limits = intArrayOf(50,250,2000,10000)
    private var last = Double.NaN
    private var next = 0.0
    private var count = 0
    private var head = 0

    fun reset() { last = Double.NaN; count = 0; head = 0 }

    fun add(t: Double, spectrum: FloatArray, rate: Int) {
        if (!t.isFinite() || rate <= 0) return
        energy.fill(0f)
        for (b in 0..2) {
            val from = (limits[b] * 2.0 * spectrum.size / rate).toInt().coerceIn(1,spectrum.lastIndex)
            val to = (limits[b+1] * 2.0 * spectrum.size / rate).toInt().coerceIn(from+1,spectrum.size)
            for (i in from until to) energy[b] += spectrum[i].coerceAtLeast(0f)
            energy[b] /= to-from
        }
        if (!last.isFinite() || t-last > 250 || t <= last) {
            reset(); last = t; next = t + STEP_MS
            energy.copyInto(previous); energy.copyInto(sampled)
            return
        }
        while (next <= t) {
            val k = ((next-last)/(t-last)).toFloat().coerceIn(0f,1f)
            for (b in 0..2) {
                val value = previous[b] + (energy[b]-previous[b])*k
                history[b][head] = (value-sampled[b]).coerceAtLeast(0f)
                sampled[b] = value
            }
            head = (head+1)%CAP; count = min(count+1,CAP); next += STEP_MS
        }
        energy.copyInto(previous); last = t
    }

    /** A small immutable copy for the worker; never correlate on the animation thread. */
    fun snapshot(): Array<FloatArray>? = if (count < 600) null else Array(3) { b ->
        FloatArray(count) { history[b][(head-count+it+CAP)%CAP] }
    }
}

/** Periodicity across several seconds, rather than the median distance between threshold crossings. */
internal fun tempoFromNovelty(bands: Array<FloatArray>): Float? {
    val n = bands.minOfOrNull { it.size } ?: return null
    if (n < 600) return null
    val correlations = Array(3) { DoubleArray(303) }
    val usable = BooleanArray(3)
    for (b in 0..2) {
        val x = bands[b]
        // A stopped/constant sound is not evidence for carrying a tempo from an old section.
        if (x.takeLast(300).maxOrNull()?.let { it < .01f } != false) continue
        val mean = x.sum().toDouble()/n
        val variance = x.sumOf { (it-mean).pow(2) }
        if (mean < .001 || variance < .01) continue
        usable[b] = true
        for (lag in 27..302) {
            var dot = 0.0; var aa = 0.0; var bb = 0.0
            for (i in lag until n) {
                val a = x[i]-mean; val c = x[i-lag]-mean
                dot += a*c; aa += a*a; bb += c*c
            }
            correlations[b][lag] = if (aa*bb > 1e-12) (dot/sqrt(aa*bb)).coerceAtLeast(0.0) else 0.0
        }
    }
    if (!usable.any { it }) return null
    // A snare on every second beat must not turn a clear kick pulse into half-time.
    val bassPeriodic = usable[0] && correlations[0].sliceArray(28..100).max() >= .5
    val weights = if (bassPeriodic) doubleArrayOf(.9,.07,.03) else doubleArrayOf(0.0,.7,.3)
    val corr = DoubleArray(303) { lag ->
        var sum = 0.0; var weight = 0.0
        for (b in 0..2) if (usable[b]) { sum += correlations[b][lag]*weights[b]; weight += weights[b] }
        if (weight > 0) sum/weight else 0.0
    }
    val peaks = (28..100).filter { corr[it] >= corr[it-1] && corr[it] > corr[it+1] && corr[it] >= .32 }
    if (peaks.isEmpty()) return null
    fun score(lag: Int) = corr[lag]*.7 + corr[lag*2]*.2 + corr[lag*3]*.1
    val best = peaks.maxOf { score(it) }
    // A whole beat repeats at two beats too: take the first equally supported fundamental.
    val lag = peaks.first { score(it) >= best*.72 }
    val a = corr[lag-1]; val b = corr[lag]; val c = corr[lag+1]
    val shift = if (abs(a-2*b+c)>1e-9) (.5*(a-c)/(a-2*b+c)).coerceIn(-.5,.5) else 0.0
    return (60000.0/((lag+shift)*TempoHistory.STEP_MS)).toFloat().takeIf { it in 59f..211f }?.coerceIn(60f,210f)
}

/** Require repeated agreement; never slowly sweep through unrelated tempos or carry one into another song. */
internal class TempoLock {
    private var candidate = 0f
    private var votes = 0
    var bpm: Float? = null; private set
    fun accept(measured: Float?): Float? {
        if (measured == null) { votes = 0; return bpm }
        if (abs(measured-candidate) <= max(2f,measured*.025f)) { candidate = (candidate+measured)*.5f; votes++ }
        else { candidate = measured; votes = 1 }
        if (votes >= 2 && (bpm == null || abs(candidate-bpm!!) >= 1f)) bpm = candidate
        return bpm
    }
}
