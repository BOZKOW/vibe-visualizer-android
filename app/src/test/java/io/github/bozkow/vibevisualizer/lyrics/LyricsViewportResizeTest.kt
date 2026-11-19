package io.github.bozkow.vibevisualizer.lyrics

import org.junit.Assert.*
import org.junit.Test

class LyricsViewportResizeTest {
    @Test fun heightAnimationKeepsTextLayoutButWidthAndFontChangesInvalidateIt() {
        val view = LyricsRenderer()
        val layout = LyricsRenderer::class.java.getDeclaredField("laidOut").apply { isAccessible = true }
        fun size(width: Float = 300f, height: Float = 500f, font: Float = 30f) = view.size(width, height, height, 360f, 780f, 1f, font, 24f, 24f)
        size(); layout.setBoolean(view, true)
        repeat(30) { size(height = 500f + it) }
        assertTrue("Height-only movement must reuse measured lyric rows", layout.getBoolean(view))
        size(width = 320f); assertFalse(layout.getBoolean(view))
        layout.setBoolean(view, true); size(width = 320f, font = 36f); assertFalse(layout.getBoolean(view))
    }
}
