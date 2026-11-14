package io.github.bozkow.vibevisualizer.lyrics

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Romanization as Spicy Lyrics 6.5.0 does it: Japanese a whole line at a time, Korean by sound, Ukrainian's own
 *  spelling, capitals and word gaps. kuromoji is a plain Java library, so the real dictionary runs here. */
class RomanizerTest {
    companion object {
        private val tk by lazy { com.atilika.kuromoji.ipadic.Tokenizer() }
        private val reader by lazy { JapaneseLine.kuromoji(tk) }
    }

    private fun ja(vararg parts: Pair<String, Boolean>) = JapaneseLine.romanize(parts.toList(), reader)
    private fun jaText(line: String) = ja(line to false).single().text

    @Test fun aWordSplitOverSyllablesKeepsItsReading() {
        // 一|緒 read alone would be "ichi" and "o"; as a word it is issho, the っ's consonant staying on the first
        assertEquals(listOf("is", "sho"), ja("一" to true, "緒" to false).map { it.text })
        assertEquals(listOf("damat", "te"), ja("黙っ" to true, "て" to false).map { it.text })
    }

    @Test fun wordsAndEndingsAsRomanizedLyricsWriteThem() {
        assertEquals("kimi no namae wo yonde ita", jaText("君の名前を呼んでいた"))
        assertEquals("watashi wa", jaText("私は"))
        assertEquals("koohii", jaText("コーヒー"))
        assertEquals("kyou", jaText("今日"))
        assertEquals("a", jaText("あっ"))
    }

    @Test fun gapsBetweenWordsTheLyricsRunTogether() {
        val r = ja("君" to true, "の" to true, "名" to true, "前" to false)
        assertEquals(listOf("kimi", "no", "na", "mae"), r.map { it.text })
        assertEquals(listOf(true, true, false, false), r.map { it.gapAfter })
    }

    @Test fun longVowelInASyllableOfItsOwn() {
        assertEquals(listOf("se", "e"), ja("せ" to true, "ー" to false).map { it.text })
    }

    @Test fun koreanBySound() {
        assertEquals("saranghae", KoreanLine.text("사랑해"))
        assertEquals("gachi", KoreanLine.text("같이"))
        assertEquals("joa", KoreanLine.text("좋아"))
        assertEquals("silla", KoreanLine.text("신라"))
        // across syllables the final still reads by the next block
        val song = Romanizer.Song(japanese = false, ukrainian = false)
        val u = Romanizer.romanizeUnit(listOf("먹", "는"), listOf(true, false), song, null) { it }
        assertEquals(listOf("Meong", "neun"), u.texts)
    }

    @Test fun cyrillicRussianAndUkrainian() {
        assertEquals("Privet", Romanizer.cyrillic("Привет"))
        assertEquals("Shchuka", Romanizer.cyrillic("Щука"))
        assertEquals("Kyiv", Romanizer.cyrillic("Київ", ukrainian = true))
        assertEquals("Ukraina", Romanizer.cyrillic("Україна", ukrainian = true))
        assertEquals("Yizhak", Romanizer.cyrillic("Їжак", ukrainian = true))
        assertEquals("ruka i noha", Romanizer.cyrillic("рука і нога", ukrainian = true))
    }

    @Test fun chineseSpacedWithGapsAfterHan() {
        val py = mapOf("我" to "wǒ", "爱" to "ài", "你" to "nǐ")
        val song = Romanizer.Song(japanese = false, ukrainian = false)
        val u = Romanizer.romanizeUnit(listOf("我爱", "你"), listOf(true, false), song, null) { py[it] ?: it }
        assertEquals(listOf("Wǒ ài", "nǐ"), u.texts)
        assertTrue(u.gaps[0])
    }

    @Test fun aLineStartsWithACapitalButLatinWordsStayAsWritten() {
        val song = Romanizer.Song(japanese = false, ukrainian = false)
        assertEquals(listOf("Sarang", "hae"), Romanizer.romanizeUnit(listOf("사랑", "해"), listOf(true, false), song, null) { it }.texts)
        assertEquals(listOf("oh", "sarang"), Romanizer.romanizeUnit(listOf("oh", "사랑"), listOf(false, false), song, null) { it }.texts)
    }

    @Test fun syllablesGetTheirRomajiAndTheLineItsGaps() {
        val ws = listOf(Word(0.0, .5, "君", false), Word(.5, 1.0, "の", false), Word(1.0, 1.5, "名", false), Word(1.5, 2.0, "前", false))
        val l = Line(0.0, 2.0, "君の名前", ws)
        runBlocking { Romanizer.romanizeLyrics(listOf(l)) }
        assertEquals(listOf("Kimi", "no", "na", "mae"), ws.map { it.rom })
        assertEquals(listOf(true, true, false, false), ws.map { it.romSp })
        assertEquals("Kimi no namae", l.sylRom)
        // the app's own romanization is never saved with the lyrics, so a better one can replace it
        assertFalse(ws[0].toJson().has("r"))
    }

    @Test fun romanizationTheLyricsCameWithIsKept() {
        val ws = listOf(Word(0.0, .5, "君", false, "KIMI"), Word(.5, 1.0, "の", false))
        val l = Line(0.0, 1.0, "君の", ws)
        runBlocking { Romanizer.romanizeLyrics(listOf(l)) }
        assertEquals("KIMI", ws[0].rom)
        assertFalse(ws[0].romSp)   // the lyrics' own gaps stay when some of it came romanized
        assertTrue(ws[0].toJson().has("r"))
    }
}
