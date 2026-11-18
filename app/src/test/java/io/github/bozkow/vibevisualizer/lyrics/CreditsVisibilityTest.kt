package io.github.bozkow.vibevisualizer.lyrics

import org.junit.Assert.*
import org.junit.Test

class CreditsVisibilityTest {
    @Test fun fallbackStaysForOffscreenClippedOrFadedCredits() {
        assertFalse(creditsReadable(700f, 120f, 600f, 1f))
        assertFalse(creditsReadable(-200f, 120f, 600f, 1f))
        assertFalse(creditsReadable(550f, 120f, 600f, 1f))
        assertFalse(creditsReadable(200f, 120f, 600f, 0f))
        assertFalse(creditsReadable(0f, 0f, 600f, 1f))
    }
    @Test fun normalAndFocusCreditsBecomeReadable() {
        assertTrue(creditsReadable(400f, 120f, 600f, .55f))
        assertTrue(creditsReadable(240f, 120f, 600f, 1f))
    }
    @Test fun finalScrollFitsMeasuredFooterWithoutMovingAnAlreadyFittingBlock() {
        assertEquals(-460f, creditsEndTarget(-200f, 900f, 140f, 600f, 20f), .01f)
        assertEquals(-500f, creditsEndTarget(-500f, 900f, 140f, 600f, 20f), .01f)
        assertEquals(-200f, creditsEndTarget(-200f, 900f, 0f, 600f, 20f), .01f)
    }
}
