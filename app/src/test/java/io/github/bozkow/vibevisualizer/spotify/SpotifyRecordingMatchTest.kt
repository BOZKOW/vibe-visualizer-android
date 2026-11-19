package io.github.bozkow.vibevisualizer.spotify

import io.github.bozkow.vibevisualizer.lyrics.SongMeta
import org.junit.Assert.*
import org.junit.Test

class SpotifyRecordingMatchTest {
    private val song = SongMeta(title = "Home", artist = "Artist", album = "Album", duration = 180.0)
    @Test fun rejectsSameNamedDifferentVersions() {
        assertFalse(spotifyRecordingMatch(song, "Home", "Artist", "Album", 210_000, ""))
        assertFalse(spotifyRecordingMatch(song, "Home", "Artist", "Other Album", 180_000, ""))
        assertTrue(spotifyRecordingMatch(song, "Home", "Artist", "Album", 180_300, ""))
    }
    @Test fun knownRecordingCodeMustMatch() {
        assertFalse(spotifyRecordingMatch(song.copy(isrc = "CODE"), "Home", "Artist", "Album", 180_000, "OTHER"))
        assertTrue(spotifyRecordingMatch(song.copy(isrc = "CODE"), "Home", "Artist", "Album", 180_000, "code"))
    }
}
