package io.github.bozkow.vibevisualizer.ui

import org.junit.Assert.*
import org.junit.Test

class CanvasRenderSizeTest {
    @Test fun softScenesStayWithinTheirBudgetOnHighResolutionPhones() {
        for ((w,h) in listOf(1080f to 2340f, 1440f to 3120f, 3120f to 1440f)) {
            val (tw,th) = canvasTextureSize(w,h,384)
            assertTrue(maxOf(tw,th) <= 384)
            assertTrue(tw * th < w * h / 20)
            assertEquals(w/h, tw.toFloat()/th, .01f)
        }
    }
    @Test fun smallWindowsAreNeverUpscaledAndZeroSizesStaySafe() {
        assertEquals(128 to 72, canvasTextureSize(128f,72f,384))
        assertEquals(1 to 1, canvasTextureSize(0f,0f,96))
    }
    @Test fun displayCoordinatesRemainTheSameWhenSamplingReducedScenes() {
        val (w,h) = canvasTextureSize(1080f,2340f,384)
        val normalizedX = .72f; val normalizedY = .87f
        assertEquals(1080f * normalizedX, w * normalizedX * (1080f/w), .001f)
        assertEquals(2340f * normalizedY, h * normalizedY * (2340f/h), .001f)
    }
}
