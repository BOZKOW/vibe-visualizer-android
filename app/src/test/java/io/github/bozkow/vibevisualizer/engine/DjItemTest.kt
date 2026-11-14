package io.github.bozkow.vibevisualizer.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The DJ talking, as the Spotify app names it: "Up next" (Následuje…) by "DJ X", a few seconds long. */
class DjItemTest {
    @Test fun theDjsVoice() {
        assertTrue(isDjItem("DJ X", 5_000))
        assertTrue(isDjItem("dj x", 0))          // length not known yet
        assertTrue(isDjItem(" DJ Livi ", 12_000))
    }

    @Test fun songsAreNot() {
        assertFalse(isDjItem("DJ X", 185_000))  // a real song by an artist of that name
        assertFalse(isDjItem("DJ Snake", 5_000))
        assertFalse(isDjItem("Spotify", 5_000))
        assertFalse(isDjItem("", 5_000))
    }
}
