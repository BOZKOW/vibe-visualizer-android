package io.github.bozkow.vibevisualizer.audio

import org.junit.Assert.*
import org.junit.Test

class BeatHitsTest {
    @Test fun attacksTriggerOnceAndSustainedSoundDoesNotRepeat() {
        val detector = BeatHits()
        var hits = 0
        for (i in 0 until 400) {
            val level = if ((i/25)%2==0) .02f else .3f
            if (detector.add(i*16.0,level)) hits++
        }
        assertEquals(8,hits)
    }
    @Test fun silenceAndSlowCrescendosDoNotBecomeBeats() {
        val detector = BeatHits()
        for (i in 0 until 300) assertFalse(detector.add(i*16.0,i*.001f))
        detector.reset()
        for (i in 0 until 100) assertFalse(detector.add(i*16.0,0f))
    }
    @Test fun closelySpacedNoiseCannotRetriggerAndResetDropsOldHit() {
        val detector = BeatHits()
        assertTrue(detector.add(0.0,.4f))
        detector.add(40.0,0f)
        assertFalse(detector.add(80.0,.4f))
        detector.reset()
        assertEquals(Double.NEGATIVE_INFINITY,detector.hitAt,0.0)
        assertFalse(detector.add(100.0,Float.NaN))
    }
}
