package io.github.bozkow.vibevisualizer.ui

/** Match Compose's duration scale to the same animation preference used by the player engine. */
internal fun playerMotionScale(low: Boolean, preference: String, systemScale: Float): Float = when {
    low || preference == "off" -> 0f
    preference == "on" -> 1f
    else -> systemScale
}

/** The moving cover keeps one measured surface; only its compositor transform changes during travel. */
internal data class CoverTravelPose(val x: Float, val y: Float, val scale: Float, val corner: Float, val shadow: Float)

internal fun coverTravelPose(startX: Float, startY: Float, startSide: Float,
    endX: Float, endY: Float, endSide: Float, surfaceSide: Float, progress: Float): CoverTravelPose {
    val k = progress.coerceIn(0f, 1f)
    fun mix(a: Float, b: Float) = a + (b - a) * k
    return CoverTravelPose(mix(startX, endX), mix(startY, endY), mix(startSide, endSide) / surfaceSide.coerceAtLeast(1f),
        mix(10f, 22f), mix(8f, 24f))
}
