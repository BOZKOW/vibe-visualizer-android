package io.github.bozkow.vibevisualizer.engine

import io.github.bozkow.vibevisualizer.core.S
import io.github.bozkow.vibevisualizer.core.now

/** Shared visual choreography only; never changes the media clock, lookup or word timing. */
internal object PlayerTransitions {
    const val PRIMARY_MS = 600
    private var until = -1e9
    fun songChanged() { until = now() + PRIMARY_MS }
    fun coverChanged() { until = maxOf(until, now() + PRIMARY_MS) }
    fun secondaryDelay(): Double = if (S.low || E.reduceMotion()) 0.0 else remaining(now(), until)
    internal fun remaining(at: Double, deadline: Double) = (deadline - at).coerceIn(0.0, PRIMARY_MS.toDouble())
}
