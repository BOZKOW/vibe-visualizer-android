package io.github.bozkow.vibevisualizer.lyrics

import io.github.bozkow.vibevisualizer.lyrics.blend.BlendDonors
import io.github.bozkow.vibevisualizer.lyrics.blend.BlendSyllable
import io.github.bozkow.vibevisualizer.lyrics.blend.BlendText

/** Presentation-only extraction. Community syncs and local TTML keep their authored vocal structure. */
internal fun parenthesizedVocals(result: LyricsResult): List<Line> {
    val credit = result.credit
    if (!result.sync || result.prov == "local" || credit?.source == "file" ||
        credit?.source == "spicy_lyrics" || credit?.uploader != null || credit?.maker != null) return result.lines
    return result.lines.map { line ->
        val words = line.words
        if (words.isNullOrEmpty() || '(' !in line.text || ')' !in line.text) return@map line
        val syllables = words.mapIndexed { i, w -> BlendSyllable(w.text,w.t,w.end ?: words.getOrNull(i+1)?.t ?: line.end ?: w.t+1,!w.sp) }
        val (lead,background) = BlendDonors.splitBackground(syllables,setOf('('))
        if (background.isEmpty()) return@map line
        fun timed(parts: List<BlendSyllable>) = parts.map { Word(it.start,it.end,it.text,!it.partOfWord) }
        val backing = background.map { Bg(BlendText.syllablesText(it.syllables),null,timed(it.syllables)) }
        Line(line.t,line.end,BlendText.syllablesText(lead),timed(lead),line.bg.orEmpty()+backing,
            line.opp,line.dots,line.apiRom,line.apiTr,line.apiTrLang)
    }
}
