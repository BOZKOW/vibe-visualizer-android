package io.github.bozkow.vibevisualizer.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParseTest {
    private fun res(n: String) = javaClass.classLoader!!.getResource(n)!!.readText()

    @Test fun ttml() {
        val p = fromText(res("test.ttml"))
        assertTrue(p.sync)
        assertEquals(8, p.lines.size)
        val l0 = p.lines[0]
        assertEquals(6.0, l0.t!!, 1e-9)
        assertEquals("Hello from the visualizer", l0.text)
        assertEquals(4, l0.words!!.size)
        assertTrue(l0.words!![0].sp)
        val l1 = p.lines[1]
        assertNotNull(l1.bg)
        assertEquals("(lights up)", l1.bg!![0].text)
        assertTrue("second singer on the right", p.lines[2].opp)
        assertEquals(listOf("Test Writer", "Second Writer"), p.writers)
    }

    @Test fun lrc() {
        val l = parseLRC("[00:01.00]One\n[00:03.50]\n[00:05.25][00:07.00]Two")
        assertEquals(4, l.size)
        assertTrue(l[1].dots)
        assertEquals(5.25, l[2].t!!, 1e-9)
    }

    @Test fun lrcWordTiming() {
        val l = parseLRC("[00:12.00]<00:12.00>Hello <00:12.50>wor<00:12.80>ld<00:13.20>\n[00:14.00]<00:14.00>Next<00:14.40> one")
        assertEquals(2, l.size)
        assertEquals("Hello world", l[0].text)
        val w = l[0].words!!
        assertEquals(listOf("Hello", "wor", "ld"), w.map { it.text })
        assertTrue("a space after Hello", w[0].sp)
        assertTrue("a syllable runs into the next", !w[1].sp)
        assertEquals(12.8, w[1].end!!, 1e-9)
        assertEquals(13.2, l[0].end!!, 1e-9)
        assertTrue("a space before the next word counts for the one before", l[1].words!![0].sp)
    }

    @Test fun lrcTranslationUnder() {
        val l = parseLRC("[00:01.00]Ich liebe dich\n[00:01.00]I love you\n[00:03.00]Next")
        assertEquals(2, l.size)
        assertEquals("Ich liebe dich", l[0].text)
        assertEquals("I love you", l[0].apiTr)
    }

    @Test fun ttmlForgiving() {
        val p = parseTTML("\uFEFF  <tt xmlns=\"http://www.w3.org/ns/ttml\"><body><div><p begin=\"1.0\" end=\"2.0\"><span begin=\"1.0\" end=\"0.5\">A</span>&nbsp;<span begin=\"1.5\" end=\"2.0\">B</span></p></div></body></tt>")
        assertEquals(1, p.lines.size)
        assertEquals(null, p.lines[0].words!![0].end)
    }

    @Test fun interludesAndKeys() {
        val p = fromText(res("test.ttml"))
        val i = interludes(p.lines)
        assertTrue("an intro break", i[0].dots)
        assertTrue("a break before 26 s", i.any { it.dots && (it.t ?: 0.0) in 18.0..19.0 })
        assertTrue(nameKeys("Vibe Tester - Test Song.ttml").contains("n:vibe tester|test song"))
        assertTrue(songKeys(SongMeta(title = "Test Song", artist = "Vibe Tester", file = "Vibe Tester - Test Song.wav")).contains("n:vibe tester|test song"))
    }

    @Test fun joins() {
        val ws = listOf(Word(0.0, .2, "beau", false), Word(.2, .4, "ti", false), Word(.4, .6, "ful", true))
        val ts = ws.map { TS(it.t, it.end!!) }
        val (jw, jt) = joinWords(ws, ts)
        assertEquals(1, jw.size); assertEquals("beautiful", jw[0].text); assertEquals(.6, jt[0].end, 1e-9)
    }
}
