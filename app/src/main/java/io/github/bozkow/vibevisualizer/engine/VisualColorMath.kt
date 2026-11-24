package io.github.bozkow.vibevisualizer.engine

import kotlin.math.pow
import kotlin.math.roundToInt

/** Saturate, clamp, then dim: the reference's CSS stages must not amplify out-of-gamut channels after dimming. */
internal object BackgroundGrade {
    const val CONTRAST = 1.06f
    fun channel(value: Float, luma: Float, saturation: Float, brightness: Float): Float =
        (((luma + (value - luma) * saturation - .5f) * CONTRAST + .5f).coerceIn(0f, 1f) * brightness).coerceIn(0f, 1f)
}

/** Minimum adjustment towards neutral light/dark ink. Original hue stays visible; no replacement white palette. */
internal object BarContrast {
    private fun linear(v: Double) = if (v <= .04045) v / 12.92 else ((v + .055) / 1.055).pow(2.4)
    fun luminance(rgb: Int): Double = .2126 * linear((rgb shr 16 and 255) / 255.0) +
        .7152 * linear((rgb shr 8 and 255) / 255.0) + .0722 * linear((rgb and 255) / 255.0)
    private fun ratio(a: Double, b: Double) = (maxOf(a, b) + .05) / (minOf(a, b) + .05)
    fun adjust(rgb: Int, background: Double, minimum: Double = 2.2): Int {
        val bg = background.coerceIn(0.0, 1.0)
        if (ratio(luminance(rgb), bg) >= minimum) return rgb
        val ink = if (bg > .35) 0 else 255
        fun mix(k: Double): Int {
            fun ch(shift: Int) = ((rgb shr shift and 255) * (1 - k) + ink * k).roundToInt().coerceIn(0, 255)
            return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
        }
        var lo = 0.0; var hi = 1.0
        repeat(10) { val mid = (lo + hi) / 2; if (ratio(luminance(mix(mid)), bg) >= minimum) hi = mid else lo = mid }
        return mix(hi)
    }
}

internal data class VideoPalette(val colors: List<IntArray>, val luminance: Double)
