package io.github.bozkow.vibevisualizer.lyrics

import io.github.bozkow.vibevisualizer.lyrics.blend.*

/** Preserve the preferred source's words, translation and attribution; borrow only a safely matched clock.
 * Matching/re-cutting and donor validation are adapted from Spicy Lyrics Mobile (AGPL-3.0).
 */
object LyricsMerge {
    fun needsTiming(r: LyricsResult) = r.lines.any { !it.dots && it.words.isNullOrEmpty() }

    fun merge(base: LyricsResult, donors: List<LyricsResult>, title: String, artist: String): LyricsResult? {
        if (!needsTiming(base)) return null
        val usable = donors.filter { it !== base && it.lines.any { l -> !l.words.isNullOrEmpty() } }
        val shaped = usable.mapNotNull { r -> BlendDonors.shape(document(r), title, artist)?.let { BlendDonor(it, r.label, r.prov ?: r.label) } }
        val first = shaped.firstOrNull() ?: return null
        val outcome = LyricsBlender.blended(document(base), base.label, base.prov ?: base.label, first, shaped.getOrNull(1)) ?: return null
        if (outcome.via == null || outcome.quality != BlendQuality.SYLLABLE) return null
        val lines = outcome.doc.lines.map { line ->
            val text = BlendText.lineText(line)
            val old = base.lines.firstOrNull { !it.dots && it.text == text && it.t == line.start }
                ?: base.lines.filter { !it.dots && it.text == text }.minByOrNull { kotlin.math.abs((it.t ?: 0.0) - (line.start ?: 0.0)) }
            Line(line.start, line.end, text, line.lead?.let(::words),
                line.background.map { b -> Bg(BlendText.syllablesText(b.syllables), null, words(b)) }.takeIf { it.isNotEmpty() } ?: old?.bg,
                line.oppositeAligned, apiRom = old?.apiRom, apiTr = old?.apiTr, apiTrLang = old?.apiTrLang)
        }
        // A Spicy timing donor still requires its linked maker/uploader, even when another source supplies words.
        val c = if (base.credit?.source == "spicy_lyrics") base.credit else usable.firstOrNull {
            it.credit?.source == "spicy_lyrics" && outcome.via.contains(it.label)
        }?.credit ?: base.credit
        val credit = Credit(c?.source, c?.name, c?.uploader, c?.maker,
            (c?.writers.orEmpty() + outcome.doc.songwriters).distinct(),
            listOfNotNull(c?.via, outcome.via).distinct().joinToString(" · "))
        return LyricsResult(lines, true, "${base.label} · merged timing", credit, base.srcDur, base.prov)
    }

    private fun words(g: BlendGroup) = g.syllables.map { Word(it.start, it.end, it.text, !it.partOfWord) }
    private fun group(words: List<Word>, start: Double?, end: Double?): BlendGroup? = words.takeIf { it.isNotEmpty() }?.let { ws ->
        BlendGroup(ws.mapIndexed { i, w -> BlendSyllable(w.text, w.t,
            w.end ?: ws.getOrNull(i + 1)?.t ?: end ?: w.t + 1, i < ws.lastIndex && !w.sp) }, start, end)
    }
    private fun document(r: LyricsResult) = BlendDoc(r.lines.filter { !it.dots }.map { l ->
        BlendLine(l.text, l.t, l.end, l.words?.let { group(it, l.t, l.end) },
            l.bg.orEmpty().mapNotNull { b -> b.words?.let { group(it, it.firstOrNull()?.t, it.lastOrNull()?.end) } }, oppositeAligned = l.opp)
    }, r.credit?.writers.orEmpty())
}

/** Shared policy for cached, early and final answers. Timing preference is supplied by the caller. */
fun lyricsCandidateBeats(rank: Int, priority: Int, currentRank: Int?, currentPriority: Int): Boolean =
    currentRank == null || rank > currentRank || (rank == currentRank && priority < currentPriority)
