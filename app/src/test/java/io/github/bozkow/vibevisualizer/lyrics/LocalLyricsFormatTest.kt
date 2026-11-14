package io.github.bozkow.vibevisualizer.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Your own lyrics files as they're written: hyphenated repeats stay joined, and background vocals' brackets go by the setting. */
class LocalLyricsFormatTest {
    private fun shown(l: Line) = l.words!!.joinToString("") { it.text + if (it.sp) " " else "" }.trim()

    @Test fun hyphenatedSyllablesWithNoSpacesStayJoined() {
        // a line written with no spaces at all: its words get one each, but "la-" "la-" "la" is one word
        val p = parseTTML("""<tt xmlns="http://www.w3.org/ns/ttml"><body><div><p begin="1.0" end="3.0"><span begin="1.0" end="1.5">la-</span><span begin="1.5" end="2.0">la-</span><span begin="2.0" end="3.0">la</span></p></div></body></tt>""")
        assertEquals("la-la-la", shown(p.lines[0]))
        assertEquals("la-la-la", p.lines[0].text)
    }

    @Test fun spacesTheFileHasAreKept() {
        val p = parseTTML("""<tt xmlns="http://www.w3.org/ns/ttml"><body><div><p begin="1.0" end="4.0"><span begin="1.0" end="1.5">Oh</span> <span begin="1.5" end="2.0">la-</span><span begin="2.0" end="2.5">la-</span><span begin="2.5" end="3.0">la</span> <span begin="3.0" end="4.0">yeah</span></p></div></body></tt>""")
        assertEquals("Oh la-la-la yeah", shown(p.lines[0]))
    }

    @Test fun wordsWithNoSpacesStillGetThem() {
        val p = parseTTML("""<tt xmlns="http://www.w3.org/ns/ttml"><body><div><p begin="1.0" end="3.0"><span begin="1.0" end="2.0">Hello</span><span begin="2.0" end="3.0">world</span></p></div></body></tt>""")
        assertEquals("Hello world", shown(p.lines[0]))
    }

    @Test fun japaneseSyllablesWithNoSpacesStayGlued() {
        // syllables of a script written without spaces get none, so 君の名前 isn't shown (or romanized) as 君 の 名 前
        val p = parseTTML("""<tt xmlns="http://www.w3.org/ns/ttml"><body><div><p begin="1.0" end="3.0"><span begin="1.0" end="1.5">君</span><span begin="1.5" end="2.0">の</span><span begin="2.0" end="2.5">名</span><span begin="2.5" end="3.0">前</span></p></div></body></tt>""")
        assertEquals("君の名前", shown(p.lines[0]))
        // a Latin word beside them still gets its space
        val q = parseTTML("""<tt xmlns="http://www.w3.org/ns/ttml"><body><div><p begin="1.0" end="3.0"><span begin="1.0" end="1.5">Oh</span><span begin="1.5" end="2.0">君</span><span begin="2.0" end="3.0">の</span></p></div></body></tt>""")
        assertEquals("Oh 君の", shown(q.lines[0]))
        // the long-vowel mark is written without spaces too (Unicode puts it in no script)
        val r = parseTTML("""<tt xmlns="http://www.w3.org/ns/ttml"><body><div><p begin="1.0" end="3.0"><span begin="1.0" end="1.5">コー</span><span begin="1.5" end="2.0">ヒー</span><span begin="2.0" end="2.5">せ</span><span begin="2.5" end="3.0">ー</span></p></div></body></tt>""")
        assertEquals("コーヒーせー", shown(r.lines[0]))
    }

    @Test fun joinsNextOnHyphens() {
        assertTrue(joinsNext("la-", "la"))
        assertTrue(joinsNext("la", "-la"))
        assertFalse(joinsNext("la", "la"))
        assertFalse(joinsNext("la-", null))
    }

    @Test fun backgroundBracketsFollowTheSetting() {
        assertEquals("backing keeps going on", bgBrackets("(backing keeps going on)", br = false))
        assertEquals("(backing keeps going on)", bgBrackets("(backing keeps going on)", br = true))
        assertEquals("(ooh)", bgBrackets("ooh", br = true))
        assertEquals("ooh", bgBrackets("ooh", br = false))
        assertEquals("(a) and (b)", bgBrackets("(a) and (b)", br = false))   // not one pair: left as written
        assertEquals("ooh", bgBrackets("[ooh]", br = false))
    }
}
