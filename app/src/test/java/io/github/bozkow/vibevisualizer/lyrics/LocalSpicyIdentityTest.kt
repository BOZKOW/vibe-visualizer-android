package io.github.bozkow.vibevisualizer.lyrics

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class LocalSpicyIdentityTest {
    private val id = "4PTG3Z6ehGkBFwjybzWkR8"
    private fun track(title: String = "Example - Slow Mix", artist: String = "Example Artist", duration: Int = 200000) =
        JSONObject().put("id", id).put("type", "track").put("name", title).put("duration", duration)
            .put("artists", org.json.JSONArray().put(JSONObject().put("name", artist)))
    private val want = SongMeta(title = "Example - Slow Mix", artist = "Example Artist", duration = 200.0, file = "local.mp3")

    @Test fun `tagged version title with missing artist is never split into fake artist and title`() {
        val m = localTrackIdentity("unknown.flac", "Example - Slow Mix", "<unknown>", "Album", 200.0)
        assertEquals("Example - Slow Mix", m.title); assertEquals("", m.artist)
        assertNull(spicyQuery(m))
    }
    @Test fun `filename fallback can recover artist but valid tags win`() {
        val m = localTrackIdentity("03 - Example Artist - Example (Remix).mp3", null, null, null, 200.0)
        assertEquals("Example Artist", m.artist); assertEquals("Example (Remix)", m.title)
        val tagged = localTrackIdentity("Wrong - File.mp3", "Right - Live", "Actual Artist", "Album", 201.0)
        assertEquals("Right - Live", tagged.title); assertEquals("Actual Artist", tagged.artist)
    }
    @Test fun `same name files albums and versions have independent online caches`() {
        val a = want.copy(uri = "content://files/1", album = "A")
        assertNotEquals(onlineLyricsIdentity(a), onlineLyricsIdentity(a.copy(uri = "content://files/2")))
        assertNotEquals(onlineLyricsIdentity(a), onlineLyricsIdentity(a.copy(album = "B")))
        assertNotEquals(onlineLyricsIdentity(a), onlineLyricsIdentity(a.copy(title = "Example")))
        assertNotEquals(onlineLyricsIdentity(a), onlineLyricsIdentity(a.copy(duration = 240.0)))
    }
    @Test fun `an exact flag cannot excuse a wrong artist version or duration`() {
        assertTrue(SpicyRecording.matches(want, id, track()))
        assertFalse(SpicyRecording.matches(want, id, track(artist = "Unrelated Artist")))
        assertFalse(SpicyRecording.matches(want, id, track(title = "Example")))
        assertFalse(SpicyRecording.matches(want, id, track(title = "Example - Live")))
        assertFalse(SpicyRecording.matches(want, id, track(duration = 205000)))
        assertFalse(SpicyRecording.matches(want.copy(artist = ""), id, track()))
        assertFalse(SpicyRecording.matches(want, id, track().put("id", "another ID")))
    }
    @Test fun `public embed metadata is parsed without using lyric text as song identity`() {
        val doc = JSONObject().put("props", JSONObject().put("pageProps", JSONObject().put("state", JSONObject().put("data", JSONObject().put("entity", track())))))
        val html = "<html><script id=\"__NEXT_DATA__\" type=\"application/json\">$doc</script></html>"
        assertTrue(SpicyRecording.matches(want, id, SpicyRecording.entity(html)!!))
        assertNull(SpicyRecording.entity("<html>unavailable</html>"))
        assertEquals(id, SpicyRecording.responseId(JSONObject().put("Body", JSONObject().put("ids", JSONObject().put("spotify", id)))))
        assertNull(SpicyRecording.responseId(JSONObject().put("Body", JSONObject().put("Content", "a repeated unrelated song"))))
    }
    @Test fun `local lyric timing must fit the actual file`() {
        val bad = LyricsResult(listOf(Line(5.0, 280.0, "Example")), true, "", null)
        assertFalse(SpicyRecording.fitsDuration(bad, 200.0))
        assertTrue(SpicyRecording.fitsDuration(bad, 300.0))
        assertTrue(SpicyRecording.fitsDuration(LyricsResult(listOf(Line(null, null, "Example")), false, "", null), 200.0))
    }
    @Test fun `a real title or album named Unknown is not missing metadata`() {
        val song = localTrackIdentity("01.flac", "Unknown", "Example Artist", "Unknown", 200.0)
        assertEquals("Unknown", song.title); assertEquals("Unknown", song.album)
    }

}
