package io.github.bozkow.vibevisualizer.lyrics

import org.junit.Assert.*
import org.junit.Test

class ParenthesizedVocalsTest {
    private fun result() = LyricsResult(listOf(Line(1.0,4.0,"Hello (yeah) world",listOf(
        Word(1.0,2.0,"Hello",true),Word(2.0,3.0,"(yeah)",true),Word(3.0,4.0,"world",false)))),true,"Other",Credit("other"))
    @Test fun movesTimedParenthesesWithoutChangingTheSource() {
        val source = result()
        val line = parenthesizedVocals(source).single()
        assertEquals("Hello world",line.text)
        assertEquals("Yeah",line.bg!!.single().text)
        assertTrue(line.bg!!.single().words!!.single().t >= 2.0)
        assertTrue(line.bg!!.single().words!!.single().end!! <= 3.0)
        assertEquals("Hello (yeah) world",source.lines.single().text)
        assertEquals(listOf("Hello","world"),line.words!!.map { it.text.trim() })
    }
    @Test fun preservesCommunityAndLocalStructure() {
        val source = result()
        source.credit = Credit("spicy_lyrics")
        assertSame(source.lines,parenthesizedVocals(source))
        source.credit = Credit("file")
        assertSame(source.lines,parenthesizedVocals(source))
    }
    @Test fun entirelyParenthesizedLineBecomesABackingGroup() {
        val source = LyricsResult(listOf(Line(1.0,2.0,"(yeah)",listOf(Word(1.0,2.0,"(yeah)",false)))),true,"Other",Credit("other"))
        val line = parenthesizedVocals(source).single()
        assertTrue(line.words!!.isEmpty())
        assertEquals("Yeah",line.bg!!.single().text)
    }
}
