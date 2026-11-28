package io.github.bozkow.vibevisualizer.audio

import kotlin.math.exp
import kotlin.math.max

/** Detect rising audio attacks only. This detector never derives or changes a song's BPM. */
internal class BeatHits {
    private var previous = 0f
    private var averageRise = .005f
    private var lastTime = Double.NaN
    private var armed = true
    var hitAt = Double.NEGATIVE_INFINITY; private set
    fun reset() { previous = 0f; averageRise = .005f; lastTime = Double.NaN; armed = true; hitAt = Double.NEGATIVE_INFINITY }
    fun add(time: Double, level: Float): Boolean {
        if (!time.isFinite() || !level.isFinite()) return false
        val dt = if (lastTime.isFinite()) (time-lastTime).coerceIn(1.0,100.0) else 16.7
        lastTime = time
        val rise = (level-previous).coerceAtLeast(0f)
        previous = level
        val threshold = max(.012f,averageRise*2.8f)
        val hit = armed && level > .03f && rise > threshold && time-hitAt >= 180
        if (hit) { hitAt = time; armed = false }
        if (rise < max(.002f,averageRise*.6f)) armed = true
        averageRise += (rise-averageRise)*(1-exp(-dt/800)).toFloat()
        return hit
    }
}
