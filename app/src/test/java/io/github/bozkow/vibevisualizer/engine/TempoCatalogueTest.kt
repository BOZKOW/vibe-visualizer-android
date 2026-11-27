package io.github.bozkow.vibevisualizer.engine

import io.github.bozkow.vibevisualizer.lyrics.SongMeta
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class TempoCatalogueTest {
    private val sid = "0123456789ABCDEFGHIJKL"
    private val id = "11111111-1111-1111-1111-111111111111"
    private val want = SongMeta(title="Night Drive",artist="Example Artist",duration=200.0)
    private val row = TempoTrack(id,"Night Drive",listOf("Example Artist"),200.0)
    @Test fun searchRequiresArtistFullVersionAndDuration() {
        assertEquals(listOf(row),matchingTempoTracks(want,listOf(row)))
        for (bad in listOf(row.copy(artists=listOf("Another Artist")),row.copy(title="Night Drive (Slowed)"),row.copy(title="Night Drive - Remix"),row.copy(duration=220.0)))
            assertTrue(matchingTempoTracks(want,listOf(bad)).isEmpty())
        assertTrue(matchingTempoTracks(want.copy(title="Night Drive (Slowed)"),listOf(row)).isEmpty())
        assertTrue(matchingTempoTracks(want.copy(artist=""),listOf(row)).isEmpty())
    }
    @Test fun missingLengthCannotChooseBetweenDifferentRecordings() {
        assertTrue(matchingTempoTracks(want.copy(duration=0.0),listOf(row,row.copy(id="other",duration=240.0))).isEmpty())
        assertEquals(listOf(row),matchingTempoTracks(want.copy(duration=0.0),listOf(row)))
    }
    @Test fun exactIdsNeverAcceptAnUnrelatedFirstFeature() {
        val wrong = TempoFeature("wrong","different","USAAA1234567",90.0)
        val right = TempoFeature(id,sid,"USAAA1234567",171.0)
        assertEquals(171.0,catalogueTempo(listOf(wrong,right),spotifyId=sid)!!,.001)
        assertNull(catalogueTempo(listOf(wrong),spotifyId=sid))
        assertNull(catalogueTempo(listOf(right)))
        assertEquals(171.0,catalogueTempo(listOf(wrong,right),ids=setOf(id))!!,.001)
    }
    @Test fun isrcDuplicatesMustAgreeInsteadOfChoosingHalfOrDoubleTime() {
        val a = TempoFeature("a",sid,"USAAA1234567",171.0)
        assertEquals(171.0,catalogueTempo(listOf(a,a.copy(id="b",bpm=171.02)),isrc="usaaa1234567")!!,.03)
        assertNull(catalogueTempo(listOf(a,a.copy(id="b",bpm=85.5)),isrc="USAAA1234567"))
    }
    @Test fun featureParserRejectsMissingInvalidAndOutOfRangeTempo() {
        val json = JSONObject("""{"content":[{"id":"$id","href":"https://open.spotify.com/track/$sid","tempo":171.01},
            {"tempo":0},{"tempo":900},{"tempo":null},{"id":"no-bpm"}]}""")
        val rows = tempoFeatures(json)
        assertEquals(1,rows.size)
        assertEquals(171.01,catalogueTempo(rows,spotifyId=sid)!!,.001)
        assertNull(catalogueTempo(tempoFeatures(JSONObject("""{"content":[{"href":"https://evil.invalid/track/$sid","tempo":120}]}""")),spotifyId=sid))
    }
    @Test fun trackParserReadsCatalogueShapeAndFiltersMalformedIds() {
        val json = JSONObject("""{"content":[{"id":"$id","trackTitle":"Night Drive","artists":[{"name":"Example Artist"}],"durationMs":200000},{"id":"bad"}]}""")
        assertEquals(listOf(row),tempoTracks(json))
    }
    @Test fun keysKeepRecordingVersionsDurationAndIdentifiersDistinct() {
        for (other in listOf(want.copy(title="Night Drive - Remix"),want.copy(duration=220.0),want.copy(id=sid),want.copy(isrc="USAAA1234567"),want.copy(artist="Another Artist")))
            assertNotEquals(tempoKey(want),tempoKey(other))
    }
    @Test fun onlyValidIdentifiersAreSentToIdentifierLookup() {
        assertEquals(sid,validTempoSpotifyId(sid))
        assertNull(validTempoSpotifyId("file:///song.mp3"))
        assertEquals("USAAA1234567",validTempoIsrc("usaaa1234567"))
        assertNull(validTempoIsrc("local-file"))
    }
}
