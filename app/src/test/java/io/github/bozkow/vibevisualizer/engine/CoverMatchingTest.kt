package io.github.bozkow.vibevisualizer.engine

import io.github.bozkow.vibevisualizer.lyrics.SongMeta
import org.junit.Assert.*
import org.junit.Test

class CoverMatchingTest {
    private val song = SongMeta(title = "Home", artist = "Artist", album = "The Album")
    @Test fun rejectsTributesAndDifferentAlbumsEvenWithTheSameSongName() {
        assertFalse(coverSearchMatch(song, "Home", "Artist Tribute Band", "The Album"))
        assertFalse(coverSearchMatch(song, "Home", "Artist", "Greatest Hits"))
        assertFalse(coverSearchMatch(song, "Other Song", "Artist", "The Album"))
        assertTrue(coverSearchMatch(song, "Home", "Artist", "The Album"))
    }
    @Test fun missingArtistCannotAuthorizeAnUnrelatedSearchResult() {
        assertFalse(coverSearchMatch(song.copy(artist = ""), "Home", "Someone Else", "The Album"))
        assertTrue(coverSearchMatch(song.copy(album = ""), "Home", "Artist", "A Release"))
    }
}
