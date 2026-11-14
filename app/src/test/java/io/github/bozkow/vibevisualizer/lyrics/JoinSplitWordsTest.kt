package io.github.bozkow.vibevisualizer.lyrics

import org.junit.Assert.assertEquals
import org.junit.Test

/** Join Split Words' four choices, as Spicy Lyrics 6.5.0 has them. */
class JoinSplitWordsTest {
    private fun w(t: Double, end: Double, text: String, sp: Boolean = false) = Word(t, end, text, sp)
    private fun join(ws: List<Word>, mode: String) = joinWords(ws, ws.map { TS(it.t, it.end!!) }, mode).first.map { it.text }

    @Test fun aDashOrASlashEndsAWord() {
        val ws = listOf(w(0.0, .3, "tea"), w(.3, .6, "ry-"), w(.6, 1.0, "eyed", true))
        assertEquals(listOf("teary-", "eyed"), join(ws, "full"))
        val ah = listOf(w(0.0, .3, "Ah-"), w(.3, .6, "ah-"), w(.6, .9, "ah", true))
        assertEquals(listOf("Ah-", "ah-", "ah"), join(ah, "full"))
        val sl = listOf(w(0.0, .3, "and"), w(.3, .6, "/or", true))
        assertEquals(listOf("and", "/or"), join(sl, "full"))
    }

    @Test fun offLeavesEverySyllable() {
        val ws = listOf(w(0.0, .2, "beau"), w(.2, .4, "ti"), w(.4, .6, "ful", true))
        assertEquals(listOf("beau", "ti", "ful"), join(ws, "off"))
        assertEquals(listOf("beautiful"), join(ws, "steady"))
    }

    @Test fun steadyNeedsNoGapAndAnEvenPace() {
        val gap = listOf(w(0.0, .2, "beau"), w(.5, .7, "ti"), w(.7, .9, "ful", true))   // 300 ms gap
        assertEquals(listOf("beau", "ti", "ful"), join(gap, "steady"))
        assertEquals(listOf("beautiful"), join(gap, "full"))
        val uneven = listOf(w(0.0, 3.0, "o"), w(3.0, 3.05, "kay", true))
        assertEquals(listOf("o", "kay"), join(uneven, "steady"))
    }

    @Test fun heldJoinsOnlyWordsDrawnLetterByLetter() {
        val quick = listOf(w(0.0, .2, "beau"), w(.2, .4, "ti"), w(.4, .6, "ful", true))
        assertEquals(listOf("beau", "ti", "ful"), join(quick, "held"))
        val held = listOf(w(0.0, .6, "for"), w(.6, 1.8, "ever", true))
        assertEquals(listOf("forever"), join(held, "held"))
    }

    @Test fun scriptsWithoutSpacesStaySplit() {
        val ws = listOf(w(0.0, .3, "君"), w(.3, .6, "の", true))
        assertEquals(listOf("君", "の"), join(ws, "full"))
    }
}
