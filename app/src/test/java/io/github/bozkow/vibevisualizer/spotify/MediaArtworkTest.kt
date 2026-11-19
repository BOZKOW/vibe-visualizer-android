package io.github.bozkow.vibevisualizer.spotify

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaArtworkTest {
    @Test fun spotifyImageIdentifiersBecomeArtworkUrls() {
        assertEquals("https://i.scdn.co/image/cover-id", mediaArtworkAddress("spotify:image:cover-id"))
    }
    @Test fun localAndWebArtworkAreKeptButTrackUrisAreNotImages() {
        for (uri in listOf("content://music/42", "file:///cover.jpg", "https://example.test/cover.jpg", "android.resource://music/cover"))
            assertEquals(uri, mediaArtworkAddress(uri))
        assertEquals("", mediaArtworkAddress("spotify:track:track-id"))
        assertEquals("", mediaArtworkAddress(null))
    }
}
