package io.github.bozkow.vibevisualizer.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpicyQueryTest {
    @Test fun `a Spotify song is asked for by its id`() {
        assertEquals("id=4PTG3Z6ehGkBFwjybzWkR8", spicyQuery(SongMeta(id = "4PTG3Z6ehGkBFwjybzWkR8", title = "Never Gonna Give You Up", isrc = "GBARL9300135")))
    }

    @Test fun `a song from elsewhere with an ISRC is asked for by it`() {
        assertEquals("id=GBARL9300135", spicyQuery(SongMeta(title = "Never Gonna Give You Up", isrc = " GBARL9300135 ")))
    }

    @Test fun `a song from elsewhere without one is asked for by name and length`() {
        assertEquals("title=Never+Gonna+Give+You+Up&artist=Rick+Astley&album=Whenever+You+Need+Somebody&duration=214",
            spicyQuery(SongMeta(title = "Never Gonna Give You Up", artist = "Rick Astley, Someone Else", album = "Whenever You Need Somebody", duration = 213.573)))
        assertEquals("title=Song", spicyQuery(SongMeta(title = " Song ")))
    }

    @Test fun `nothing to ask with`() {
        assertNull(spicyQuery(SongMeta()))
        assertNull(spicyQuery(SongMeta(artist = "Someone")))
    }
    @Test fun `connection diagnostic uses current local file instead of the sample track`() {
        val current = SongMeta(title = "A different song", artist = "Another artist", file = "local.flac", duration = 171.0)
        assertEquals(current, spicyDiagnosticTarget(current))
        assertEquals("title=A+different+song&artist=Another+artist&duration=171", spicyQuery(spicyDiagnosticTarget(current)))
        assertEquals("Blinding Lights", spicyDiagnosticTarget(null).title)
    }

    @Test fun `ampersand band names are preserved in local searches`() {
        assertEquals("title=Example&artist=Years+%26+Years&duration=200", spicyQuery(SongMeta(title = "Example", artist = "Years & Years", duration = 200.0, file = "example.flac")))
    }

}
