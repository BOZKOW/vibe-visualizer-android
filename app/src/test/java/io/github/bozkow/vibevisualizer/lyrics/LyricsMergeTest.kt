package io.github.bozkow.vibevisualizer.lyrics

import org.junit.Assert.*
import org.junit.Test

class LyricsMergeTest {
    private val texts = listOf("Walking down the river road", "Counting every stone I know", "Hey hey the water is cold",
        "I will carry you safely home", "Walking down the river road", "Counting every stone I know",
        "Somewhere the lanterns glow", "I will carry you safely home")
    private fun base() = LyricsResult(texts.mapIndexed { i, t -> Line(10.0 + i * 5, 14.0 + i * 5, t, apiTr = "Translation $i") },
        true, "Preferred", Credit("spicy_lyrics", uploader = Person("Uploader", null, "https://example.com/u", null),
            maker = Person("Maker", null, "https://example.com/m", null)), prov = "spicy")
    private fun donor(content: List<String> = texts) = LyricsResult(content.mapIndexed { i, t ->
        val a = 10.12 + i * 5
        val ws = t.split(" "); val step = 3.6 / ws.size
        Line(a, a + 3.6, t, ws.mapIndexed { k, w -> Word(a + k * step, a + (k + 1) * step, w, k != ws.lastIndex) })
    }, true, "Timing donor", Credit("netease"), prov = "netease")

    @Test fun oldRepeatedMergeCreditsAreFlattenedWhenLoaded() {
        val credit = Credit("apple", via = "Spicy Lyrics + KuGou · Spicy Lyrics · merged timing + KuGou · merged timing")
        assertEquals("Spicy Lyrics + KuGou", credit.via)
        assertNull(Credit("apple",via="merged timing").via)
    }

    @Test fun matchingTimingKeepsPreferredWordsTranslationAndAttribution() {
        val b = base()
        val r = LyricsMerge.merge(b, listOf(donor()), "Nobody", "Made up")
        assertNotNull(r); r!!
        assertEquals(texts, r.lines.map { it.text })
        assertTrue(r.lines.all { !it.words.isNullOrEmpty() })
        assertEquals("spicy", r.prov)
        assertEquals("spicy_lyrics", r.credit!!.source)
        assertEquals("Maker", r.credit!!.maker!!.username)
        assertEquals("https://example.com/u", r.credit!!.uploader!!.url)
        assertTrue(r.credit!!.via!!.contains("Timing donor"))
        assertEquals("Translation 0", r.lines.first().apiTr)
        assertEquals("Translation 4", r.lines[4].apiTr)
        assertTrue(b.lines.all { it.words == null })
        r.lines.flatMap { it.words.orEmpty() }.forEach { assertTrue(it.end!! >= it.t) }
    }

    @Test fun aSpicyTimingDonorKeepsItsRequiredLinkedCredits() {
        val d = donor().also { it.label = "Spicy Lyrics"; it.credit = base().credit; it.prov = "spicy" }
        val b = base().also { it.label = "Preferred catalogue"; it.credit = Credit("lrclib"); it.prov = "lrclib" }
        val r = LyricsMerge.merge(b, listOf(d), "Nobody", "Made up")!!
        assertEquals("lrclib", r.prov)
        assertEquals("spicy_lyrics", r.credit!!.source)
        assertEquals("Maker", r.credit!!.maker!!.username)
        assertEquals("https://example.com/u", r.credit!!.uploader!!.url)
        assertTrue(r.credit!!.via!!.contains("Preferred catalogue"))
    }

    @Test fun differentRecordingOrMissingWordTimingIsNotMerged() {
        assertNull(LyricsMerge.merge(base(), listOf(donor(List(8) { "Completely different words on another recording" })), "Nobody", "Made up"))
        assertNull(LyricsMerge.merge(base(), listOf(base()), "Nobody", "Made up"))
        assertNull(LyricsMerge.merge(donor(), listOf(base()), "Nobody", "Made up"))
    }

    @Test fun cachedOrEarlyAnswersCanBeReplacedByHigherPriorityUntilLookupEnds() {
        assertTrue(lyricsCandidateBeats(1, 0, 1, 4))
        assertFalse(lyricsCandidateBeats(1, 4, 1, 0))
        assertFalse(lyricsCandidateBeats(0, 0, 1, 4))
        assertTrue(lyricsCandidateBeats(2, 4, 1, 0)) // optional word-timing preference
        assertTrue(lyricsCandidateBeats(1, 0, null, 99))
        assertFalse(lyricsCandidateBeats(1, 0, 1, 0))
    }
}
