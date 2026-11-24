package io.github.bozkow.vibevisualizer.lyrics

import io.github.bozkow.vibevisualizer.engine.BackgroundGrade
import io.github.bozkow.vibevisualizer.engine.BarContrast
import io.github.bozkow.vibevisualizer.spotify.fullArtworkAddress
import org.junit.Assert.*
import org.junit.Test

class VisualColorMathTest {
    @Test fun `out of gamut saturation is clamped before dimming`() {
        assertEquals(.65f, BackgroundGrade.channel(.9f, .3f, 2.5f, .65f), .00001f)
        assertEquals(0f, BackgroundGrade.channel(.1f, .5f, 2.5f, .65f), .00001f)
        val gray = BackgroundGrade.channel(.5f, .5f, 2.5f, .65f)
        assertEquals(.325f, gray, .00001f)
    }
    @Test fun `bars gain light on dark backgrounds and darken on white`() {
        val dark = 0xFF202024.toInt(); val light = 0xFFEEEEFF.toInt()
        assertTrue(BarContrast.luminance(BarContrast.adjust(dark, .02)) > BarContrast.luminance(dark))
        assertTrue(BarContrast.luminance(BarContrast.adjust(light, .9)) < BarContrast.luminance(light))
        val blue = 0xFF4070C0.toInt()
        assertEquals(blue, BarContrast.adjust(blue, 0.0))
        for (bg in listOf(0.0, .02, .2, .5, .9, 1.0)) for (color in listOf(dark, light, blue)) {
            val l = BarContrast.luminance(BarContrast.adjust(color, bg))
            assertTrue((maxOf(bg, l) + .05) / (minOf(bg, l) + .05) >= 2.19)
        }
    }
    @Test fun `artwork resolution upgrade preserves album identity and leaves unknown URLs alone`() {
        assertEquals("https://i.scdn.co/image/ab67616d0000b273abcd", fullArtworkAddress("https://i.scdn.co/image/ab67616d00001e02abcd"))
        val custom = "https://example.com/00001e02abcd"
        assertEquals(custom, fullArtworkAddress(custom))
    }
}
