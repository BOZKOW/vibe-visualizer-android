package io.github.bozkow.vibevisualizer.ui

import kotlin.math.roundToInt

/** Pixel budget for soft textures, independent of phone resolution. Sharp video never uses this path. */
internal fun canvasTextureScale(width: Float, height: Float, longestEdge: Int): Float =
    (longestEdge / maxOf(width, height).coerceAtLeast(1f)).coerceAtMost(1f)

internal fun canvasTextureSize(width: Float, height: Float, longestEdge: Int): Pair<Int, Int> {
    val k = canvasTextureScale(width, height, longestEdge)
    return (width * k).roundToInt().coerceAtLeast(1) to (height * k).roundToInt().coerceAtLeast(1)
}
