package io.github.bozkow.vibevisualizer.lyrics

import org.junit.Assert.*
import org.junit.Test

class RomanizedWrappingTest {
    @Test fun aWholeJapaneseReadingProvidesIndependentWordBreaks() {
        val text = "Kimi no namae wo yonde ita"
        val pieces = romanizedWordPieces(text, TS(2.0, 8.0))
        assertEquals(text, pieces.joinToString("") { it.first })
        assertEquals(listOf("Kimi ", "no ", "namae ", "wo ", "yonde ", "ita"), pieces.map { it.first })
        assertEquals(pieces.indices.map { it..it }, wordSpans(pieces.map { it.first }))
        assertEquals(2.0, pieces.first().second.t, .0001)
        assertEquals(8.0, pieces.last().second.end, .0001)
        pieces.zipWithNext().forEach { (a, b) -> assertEquals(a.second.end, b.second.t, .0001) }
    }
    @Test fun existingTimedSyllablesAreNotSplitOrRetimed() {
        val time = TS(1.0, 1.5)
        val a = romanizedWordPieces("is", time).single()
        assertEquals("is", a.first); assertSame(time, a.second)
        assertEquals(listOf(0..1), wordSpans(listOf("is", "sho ")))
    }
    @Test fun spacesAndUnicodeTextSurviveWithoutExtendingTheTimingUnit() {
        val text = "  Tōkyō  de  "
        val pieces = romanizedWordPieces(text, TS(3.0, 5.0))
        assertEquals(text, pieces.joinToString("") { it.first })
        assertEquals(3.0, pieces.first().second.t, .0001)
        assertEquals(5.0, pieces.last().second.end, .0001)
        assertTrue(pieces.all { it.second.t <= it.second.end && it.second.t >= 3.0 && it.second.end <= 5.0 })
    }
}
