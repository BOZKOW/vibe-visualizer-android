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

    @Test fun dotsFromThreeSeconds() {
        fun song(intro: Double, gap: Double) = listOf(Line(intro, intro + 2, "a"), Line(intro + 2 + gap, intro + 4 + gap, "b"))
        assertTrue("a 3 s intro has them", interludes(song(3.0, 1.0))[0].dots)
        assertTrue("a shorter one hasn't", !interludes(song(2.9, 1.0))[0].dots)
        val i = interludes(song(1.0, 3.5))
        assertEquals("a 3.5 s pause has them, between its two lines", listOf(false, true, false), i.map { it.dots })
        assertEquals("they last until the next line", 6.5, i[1].end ?: 0.0, .001)
        assertTrue("a 2.9 s pause hasn't", interludes(song(1.0, 2.9)).none { it.dots })
    }

    @Test fun aPauseStartsOnceNobodySings() {
        // a long line (1-20), a short one over it (2-3), the next at 25: the pause is from 20, not from 3
        val i = interludes(listOf(Line(1.0, 20.0, "long"), Line(2.0, 3.0, "short"), Line(25.0, 27.0, "next")))
        assertEquals(listOf(false, false, true, false), i.map { it.dots })
        assertEquals(20.0, i[2].t ?: 0.0, .001)
        // the next at 21: only 1 s after the long line ends, so no pause at all
        assertTrue(interludes(listOf(Line(1.0, 20.0, "long"), Line(2.0, 3.0, "short"), Line(21.0, 23.0, "next"))).none { it.dots })
    }

    @Test fun keepSyllablePace() {
        // "per" 0-2.6 s, "fect" 2.6-3.25 s; the letters' window ends .25 s early, which comes off both alike
        val ws = listOf(Word(0.0, 2.6, "per", false), Word(2.6, 3.25, "fect", true))
        val (_, jt) = joinWords(ws, ws.map { TS(it.t, it.end!!) })
        val tm = jt[0]; assertEquals(2, tm.syl?.size); val end0 = tm.end; tm.end = end0 - .25
        val ls = sylLetterTimes(tm, listOf(3, 4), end0)
        assertEquals(7, ls.size)
        assertEquals("the first syllable's three letters share 2.4 s", .8, ls[0].end - ls[0].t, .001)
        assertEquals("the second's four share .6 s", .15, ls[6].end - ls[6].t, .001)
        assertEquals("they meet", ls[2].end, ls[3].t, .001)
        assertEquals("the last ends with the window", 3.0, ls[6].end, .001)
    }

    @Test fun aGlideArrivesOnTimeAndKeepsItsSpeedWhenTheTargetMoves() {
        val g = Glide(0f); g.value(0.0); g.set(100f, 0.0, 1000.0)
        assertTrue("a soft start", g.value(50.0) < 10f)
        assertTrue("about there when it's due", g.value(1000.0) > 97f)
        assertEquals("it gets there", 100f, g.value(3000.0), .01f)
        val m = Glide(0f); m.value(0.0); m.set(100f, 0.0, 1000.0)
        val a = m.value(200.0); val b = m.value(216.0); m.set(200f, 216.0, 1000.0); val c = m.value(232.0)
        assertTrue("no jolt when the target moves on", c - b >= (b - a) * .95f)
        m.snap(5f); assertEquals("a snap is at once", 5f, m.value(5000.0), .001f)
    }

    @Test fun theLastWordSetsOffTheScrollAndTheFirstWordEndsIt() {
        assertEquals("from the last word to the first", .6, scrollSecs(11.4, 12.0), 1e-9)
        assertEquals("a long held note or a pause doesn't make it crawl", .8, scrollSecs(10.0, 15.0), 1e-9)
        assertEquals("words running together don't make it snap", .55, scrollSecs(11.95, 12.0), 1e-9)
        assertEquals("lines that overlap neither", .55, scrollSecs(12.0, 11.0), 1e-9)
        assertEquals("it waits for the last word to end", 11.7, scrollFrom(11.7, 12.0), 1e-9)
        assertEquals("through a long pause, until .8 s before", 14.2, scrollFrom(10.0, 15.0), 1e-9)
        assertEquals("a word held past the next line's start: no later than that", 12.0, scrollFrom(13.0, 12.0), 1e-9)
    }

    @Test fun eachRowIsDueInTimeForItsFirstWord() {
        val rows = listOf(WordRow(10.0, 10.0, 25f, 50f), WordRow(12.0, 11.4, 75f, 50f), WordRow(16.0, 13.0, 125f, 50f))
        assertEquals("not yet", -1, dueRow(rows, 9.9))
        assertEquals("the first as it's sung", 0, dueRow(rows, 10.0))
        assertEquals("it holds until the last word on the row", 0, dueRow(rows, 11.39))
        assertEquals("which sets off the next", 1, dueRow(rows, 11.4))
        assertEquals("it holds through a pause", 1, dueRow(rows, 15.19))
        assertEquals("and moves only in time to end on the first word", 2, dueRow(rows, 15.2))
        assertEquals("no rows", -1, dueRow(emptyList(), 5.0))
    }

    @Test fun aTallLineMovesAWholeRowAtATime() {
        val view = 300f; val m = 20f; val h = 600f; val foot = view - m - h   // rows of 50, 260 of room
        fun top(row: Int, ended: Boolean = false) = tallLineTop(view, m, h, row * 50f, if (row > 0) 50f else 0f, ended)
        assertEquals("it starts at its top", m, top(0), .01f)
        assertEquals("the second row needs no move: the first stays over it", m, top(1), .01f)
        assertEquals("then a row", m - 50f, top(2), .01f)
        assertEquals("and a row", m - 100f, top(3), .01f)
        assertEquals("it stops with its foot in view", foot, top(11), .01f)
        assertEquals("and stays there once it's sung", foot, top(3, ended = true), .01f)
        var was = m + 1
        for (row in 0..11) {
            val t = top(row); assertTrue("it only goes up", t <= was); was = t
            val at = t + row * 50f; assertTrue("the row being sung is whole in view: $at", at >= m && at + 50f <= view - m)
        }
    }

    @Test fun aTallLineInTheSmallWindowGoesDownWithTheSinging() {
        val view = 300f; val m = 20f   // 260 of room, for a line of 600 with its last words at 500 and a translation under them
        fun top(sung: Float, ended: Boolean = false) = pipTallLineTop(view, m, 600f, sung, 500f, ended)
        assertEquals("it starts at its top", m, top(0f), .5f)
        assertEquals("its foot is in view when the last words are sung", view - m - 600f, top(500f), .5f)
        assertEquals("and stays once it's sung", view - m - 600f, top(500f, ended = true), .5f)
        var was = m + 1
        for (sung in 0..500 step 25) {
            val t = top(sung.toFloat()); assertTrue("it only goes up", t <= was); was = t
            val at = t + sung; assertTrue("the words being sung stay in view: $at", at >= m && at <= view - m)
        }
        val at = pipTallLineTop(200f, m, 900f, 400f, 400f, false) + 400f   // 500 of translation under the last words
        assertTrue("$at", at >= m && at <= 200f - m)
        pipTallLineTop(10f, 20f, 100f, 50f, 80f, false); pipTallLineTop(0f, 0f, 0f, 0f, 0f, false)   // no room: it mustn't throw
    }

    @Test fun aTranslationTallerThanTheViewKeepsTheWordsInView() {
        val view = 200f; val m = 20f
        val at = tallLineTop(view, m, 900f, 400f, 50f, false) + 400f   // 500 of translation under the last words
        assertTrue("$at", at >= m && at <= view - m)
    }

    @Test fun aWindowWithNoRoomDoesntThrow() {
        tallLineTop(10f, 20f, 100f, 50f, 80f, false); tallLineTop(0f, 0f, 0f, 0f, 0f, false); tallLineTop(30f, 18f, 60f, 0f, 0f, false)
    }

    @Test fun aWordInSyllablesWrapsAsOne() {
        assertEquals("the parts of a word stay together", listOf(0..0, 1..3, 4..4), wordSpans(listOf("so ", "beau", "ti", "ful ", "song")))
        assertEquals("a row may end after a dash", listOf(0..0, 1..1, 2..2), wordSpans(listOf("la-", "la-", "la")))
        assertEquals("and in a script without spaces", listOf(0..0, 1..1, 2..2), wordSpans(listOf("春", "の", "風")))
        assertEquals("next to one too", listOf(0..0, 1..1), wordSpans(listOf("love", "春")))
        assertEquals("one word", listOf(0..1), wordSpans(listOf("per", "fect")))
        assertEquals("nothing", emptyList<IntRange>(), wordSpans(emptyList()))
    }

    @Test fun joins() {
        val ws = listOf(Word(0.0, .2, "beau", false), Word(.2, .4, "ti", false), Word(.4, .6, "ful", true))
        val ts = ws.map { TS(it.t, it.end!!) }
        val (jw, jt) = joinWords(ws, ts)
        assertEquals(1, jw.size); assertEquals("beautiful", jw[0].text); assertEquals(.6, jt[0].end, 1e-9)
    }
}
