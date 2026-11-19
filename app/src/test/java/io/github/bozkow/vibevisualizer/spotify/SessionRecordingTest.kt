package io.github.bozkow.vibevisualizer.spotify

import org.junit.Assert.*
import org.junit.Test

class SessionRecordingTest {
    @Test fun knownDifferentIdsAreNewSongsEvenWithIdenticalNames() {
        assertTrue(sessionRecordingChanged("one", "two", "Album", "Album", 180_000, 180_000))
        assertFalse(sessionRecordingChanged("one", "one", "", "Album", 180_000, 180_010))
    }
    @Test fun lateIdsAndIncompleteMetadataDoNotRestartTheSong() {
        assertFalse(sessionRecordingChanged("", "one", "", "Album", 0, 180_000))
        assertFalse(sessionRecordingChanged("one", "", "Album", "", 180_000, 0))
    }
    @Test fun withoutIdsDifferentKnownReleasesOrLengthsStillCount() {
        assertTrue(sessionRecordingChanged("", "", "Single", "Album", 180_000, 180_000))
        assertTrue(sessionRecordingChanged("", "", "Album", "Album", 180_000, 200_000))
        assertFalse(sessionRecordingChanged("", "", "Album", "album", 180_000, 180_020))
    }
}
