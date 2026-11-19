package io.github.bozkow.vibevisualizer.ui

import org.junit.Assert.*
import org.junit.Test

class PlayerMotionTest {
    @Test fun preferenceOverridesSystemOnlyWhenRequested() {
        assertEquals(1f, playerMotionScale(false, "on", 0f), 0f)
        assertEquals(0f, playerMotionScale(false, "off", 1f), 0f)
        assertEquals(.5f, playerMotionScale(false, "system", .5f), 0f)
        assertEquals(0f, playerMotionScale(true, "on", 1f), 0f)
    }
    @Test fun travelKeepsTheSameSurfaceAndExactEndpoints() {
        val start = coverTravelPose(24f, 48f, 112f, 48f, 240f, 312f, 312f, 0f)
        val end = coverTravelPose(24f, 48f, 112f, 48f, 240f, 312f, 312f, 1f)
        assertEquals(112f, start.scale * 312f, .0001f)
        assertEquals(312f, end.scale * 312f, .0001f)
        assertEquals(24f, start.x, 0f); assertEquals(48f, start.y, 0f)
        assertEquals(48f, end.x, 0f); assertEquals(240f, end.y, 0f)
        assertEquals(10f, start.corner, 0f); assertEquals(22f, end.corner, 0f)
    }
    @Test fun fractionalTravelIsNotRoundedToWholePixels() {
        val p = coverTravelPose(0f, 0f, 100f, 1f, 2f, 200f, 200f, .25f)
        assertEquals(.25f, p.x, 0f); assertEquals(.5f, p.y, 0f)
        assertEquals(125f, p.scale * 200f, .0001f)
    }
}
