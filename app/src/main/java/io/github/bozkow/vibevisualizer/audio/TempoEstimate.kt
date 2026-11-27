package io.github.bozkow.vibevisualizer.audio

/** A robust local estimate, independent of the background animation's narrower speed clamp. */
internal fun tempoFromOnsets(gaps: List<Double>): Float? {
    val valid = gaps.filter { it.isFinite() && it in 240.0..1500.0 }.sorted()
    if (valid.size < 6) return null
    val center = (valid[(valid.size-1)/2] + valid[valid.size/2]) / 2
    return (60000.0 / center).toFloat().coerceIn(60f,210f)
}
