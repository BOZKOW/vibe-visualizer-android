package io.github.bozkow.vibevisualizer.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Finding an album's animated cover, on the shapes Apple's answers have. */
class MotionSearchTest {
    private fun hit(id: Int, name: String, artist: String, explicit: Boolean = false) =
        MotionSearch.AlbumHit("https://music.apple.com/us/album/x/$id", name, artist, explicit)

    @Test fun anotherArtistsAlbumIsNotOpened() {
        // "NF NO NAME" also returns Jack White's "No Name": its video mustn't hang on this record
        val r = MotionSearch.rankAlbums(listOf(hit(1, "No Name", "Jack White"), hit(2, "NO NAME", "NF")), "NF", "NO NAME")
        assertEquals(listOf("https://music.apple.com/us/album/x/2"), r)
    }

    @Test fun theExactNameFirstThenTheExplicitEdition() {
        val r = MotionSearch.rankAlbums(listOf(hit(1, "Album (Deluxe Edition)", "Band"), hit(2, "Album", "Band"), hit(3, "Album", "Band", explicit = true)), "Band", "Album")
        assertEquals(listOf("3", "2", "1"), r.map { MotionSearch.albumId(it) })
    }

    @Test fun editionsComeOff() {
        assertEquals("Album", MotionSearch.plain("Album (Deluxe Edition)"))
        assertEquals("Song", MotionSearch.plain("Song - Single"))
        assertEquals("Remain in Light", MotionSearch.plain("Remain in Light"))
        assertTrue(MotionSearch.loose("The Tortured Poets Department", "THE TORTURED POETS DEPARTMENT: THE ANTHOLOGY"))
    }

    @Test fun theLeadArtist() {
        assertEquals("Taylor Swift", MotionSearch.leadArtist("Taylor Swift, Post Malone"))
        assertEquals("Drake", MotionSearch.leadArtist("Drake feat. Rihanna"))
        assertEquals(MotionSearch.key("Taylor Swift", "1989", "Style"), MotionSearch.key("taylor swift!", "1989", "Blank Space"))
        assertNull(MotionSearch.key("", "1989", "Style"))
    }

    @Test fun itunesAnswers() {
        val r = MotionSearch.parseAlbums("""{"results":[
            {"wrapperType":"collection","collectionViewUrl":"https://music.apple.com/us/album/a/11?uo=4","collectionName":"A","artistName":"B","collectionExplicitness":"explicit"},
            {"wrapperType":"track","collectionViewUrl":"https://music.apple.com/us/album/c/22?i=1","collectionName":"C","artistName":"D"},
            {"wrapperType":"artist","artistName":"E"}]}""")
        assertEquals(listOf("11", "22"), r.map { MotionSearch.albumId(it.url) })
        assertTrue(r[0].explicit)
        assertTrue(MotionSearch.parseAlbums("not json").isEmpty())
    }

    @Test fun theVideoOnAnAlbumPage() {
        val page = """<div><amp-ambient-video class="x" data-testid="video" src="https://mvod.itunes.apple.com/itunes-assets/a/b/P123_Anull_video_gr698_sdr_1080x1080-.m3u8" poster="p.jpg"></amp-ambient-video></div>"""
        assertTrue(MotionSearch.ambientVideo(page)!!.endsWith("1080x1080-.m3u8"))
        assertNull(MotionSearch.ambientVideo("<html>no video here</html>"))
        assertEquals(listOf("123", "456"), MotionSearch.albumIds("""href="https://music.apple.com/us/album/a/123" x music.apple.com/us/album/b/456?i=1 music.apple.com/us/album/a/123"""))
    }

    @Test fun theTallStreamComesFromThePageDataPastItsPreviewFrame() {
        // Spicy Lyrics Mobile 0.19.0's test page
        val page = """"videoArtwork":{"dictionary":{"motionDetailSquare":{"previewFrame":{"height":3840},"video":"https://a/square.m3u8"}}},""" +
            """"tallVideoArtwork":{"dictionary":{"motionDetailTall":{"previewFrame":{"height":2732},"video":"https://a/tall.m3u8"}}}"""
        assertEquals("https://a/tall.m3u8", MotionSearch.tallVideo(page))
        assertNull(MotionSearch.tallVideo(""""videoArtwork":{"dictionary":{"motionDetailSquare":{"video":"https://a/square.m3u8"}}}"""))
    }

    @Test fun aRememberedRecordKeepsItsStreamsAndOldOnesReadTheSame() {
        val e = MotionSearch.Entry(123L, "https://a/square.m3u8", "https://a/tall.m3u8")
        assertEquals(e, MotionSearch.parseEntry(MotionSearch.storeEntry(e)))
        assertEquals(MotionSearch.Entry(5L, "https://a/s.m3u8", ""), MotionSearch.parseEntry(MotionSearch.storeEntry(MotionSearch.Entry(5L, "https://a/s.m3u8", ""))))
        // saved before tall streams were read: the tall one is unknown, so it's looked for once more
        assertEquals(MotionSearch.Entry(7L, "https://a/s.m3u8", null), MotionSearch.parseEntry("7|https://a/s.m3u8"))
        assertEquals(MotionSearch.Entry(9L, "", null), MotionSearch.parseEntry("9|"))
    }
}
