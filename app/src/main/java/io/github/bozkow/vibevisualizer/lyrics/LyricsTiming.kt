package io.github.bozkow.vibevisualizer.lyrics

import kotlin.math.max

/** The time a piece of text is sung over, in seconds. */
class TS(var t: Double, var end: Double) {
    /** A word joined from syllables (joinWords): each one's text and start, for Keep syllable pace. */
    var syl: List<Pair<String, Double>>? = null
}

/** Keep syllable pace (Spicy Lyrics Mobile 0.18): the letters of a joined word [tm], each syllable's ([counts] of them)
 *  spread over that syllable's own share of the word's time, so a long "per" and a quick "fect" keep their pace inside
 *  one word. [end0]: the word's end before its letters' trim, which comes off every syllable alike. */
fun sylLetterTimes(tm: TS, counts: List<Int>, end0: Double): List<TS> {
    val parts = tm.syl!!; val k = (tm.end - tm.t) / max(end0 - tm.t, .001)
    val out = ArrayList<TS>(counts.sum()); var at = tm.t
    parts.indices.forEach { i ->
        val to = if (i == parts.lastIndex) tm.end else (tm.t + (parts[i + 1].second - tm.t) * k).coerceIn(at, tm.end)
        val per = if (counts[i] > 0) (to - at) / counts[i] else 0.0
        repeat(counts[i]) { n -> out.add(TS(at + n * per, at + (n + 1) * per)) }
        at = to
    }
    return out
}

val UNSPACED = Regex("[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}\\p{IsThai}\\p{IsLao}\\p{IsKhmer}\\p{IsMyanmar}]")
val RTL = Regex("[\\u0590-\\u08FF\\uFB1D-\\uFDFF\\uFE70-\\uFEFF]")
private val DASH_END = Regex("[-\\u2010-\\u2015]$")

/** Join Split Words, ported from Spicy Lyrics (src/utils/Lyrics/Applyer/Utils/JoinSplitWords.ts, AGPL-3.0): the
 *  parts of a word sung in syllables become one word, unless they don't line up cleanly (a gap over 100 ms, an uneven
 *  pace, a space inside, a trailing dash, a script written without spaces). */
fun joinWords(ws: List<Word>, ts: List<TS>): Pair<List<Word>, List<TS>> {
    val out = ArrayList<Word>(); val tout = ArrayList<TS>()
    var i = 0
    fun ends(w: Word) = w.sp || w.text.lastOrNull()?.isWhitespace() == true
    while (i < ws.size) {
        var e = i
        while (!ends(ws[e]) && e + 1 < ws.size) e++
        val grp = ws.subList(i, e + 1); val rt = ts.subList(i, e + 1)
        val ok = grp.size > 1 && grp.all { it.text.trim().isNotEmpty() && it.text.trim().none { c -> c.isWhitespace() } } &&
            grp.dropLast(1).none { DASH_END.containsMatchIn(it.text) } && grp.none { UNSPACED.containsMatchIn(it.text) } &&
            rt.withIndex().all { (k, x) -> k == 0 || x.t - rt[k - 1].end <= .1 } && evenPace(grp, rt)
        if (ok) {
            out.add(Word(rt[0].t, null, grp.joinToString("") { it.text }, grp.last().sp, if (grp.any { it.rom != null }) grp.joinToString("") { it.rom ?: it.text } else null))
            tout.add(TS(rt[0].t, rt.maxOf { it.end }).also { x -> x.syl = grp.mapIndexed { k, w -> w.text to rt[k].t } })
        } else grp.forEachIndexed { k, w -> out.add(w); tout.add(rt[k]) }
        i = e + 1
    }
    return out to tout
}

private fun evenPace(grp: List<Word>, rt: List<TS>): Boolean {
    val du = rt.last().end - rt[0].t; val ch = grp.sumOf { it.text.length }
    if (du <= 0 || ch == 0) return false
    return rt.withIndex().all { (k, x) -> val tsr = (x.end - x.t) / du; val cs = grp[k].text.length.toDouble() / ch; tsr >= cs / 3 && tsr <= cs * 3 }
}

/** Where a line taller than the view sits (its top, px in the view): it moves a whole row at a time, so its rows rest
 *  whole, with the row being sung under the one before it (that one's last word is still being sung as the move starts)
 *  and as many rows to come as fit. It stops with its foot in view, and goes there once it's sung, translation included.
 *  The words being sung stay in view, even over a translation taller than the view. [view]: the view's height; [m]: kept
 *  clear at both ends; [h]: the line's height; [rowTop]: how far down it that row starts; [above]: the height of the row
 *  before; [ended]: sung to the end. (minOf and maxOf: a window too small to have room mustn't throw.) */
fun tallLineTop(view: Float, m: Float, h: Float, rowTop: Float, above: Float, ended: Boolean): Float {
    val foot = view - m - h
    if (ended) return foot
    return minOf(maxOf(m + above - rowTop, foot), m)
}

/** A row of a line's words as it's wrapped on screen: when its first word is sung, when the last word on the row before
 *  it starts (for the first row, its own start), and where its middle is and how tall it is (px down the line). */
class WordRow(val at: Double, val lastWord: Double, val y: Float, val h: Float)

/** How long (s) the scroll to the next row or line takes: the last word before it sets it off, the first word there ends
 *  it. Kept from crawling through a long held note or a pause (a slow scroll leaves rows cut in half the whole time, the
 *  owner's complaint on 0.41.6) and from snapping when the words run together. It still ends on the first word. */
fun scrollSecs(lastWord: Double, first: Double): Double = (first - lastWord).coerceIn(.35, .8)

/** The row the scroll should be on at [pos], -1 before the first: each is moved to in time to be there as its first word
 *  is sung. */
fun dueRow(rows: List<WordRow>, pos: Double): Int {
    var k = -1
    for (i in rows.indices) { val r = rows[i]; if (pos >= (if (i == 0) r.at else r.at - scrollSecs(r.lastWord, r.at))) k = i else break }
    return k
}

/** A pause this long (s) gets its "• • •", as in Spicy Lyrics (its getLyricsBetweenShow), however long it is; only the
 *  Focus style's cover takes their place (LyricsRenderer's COVER_BREAK). */
const val DOTS_FROM = 3.0

/** "• • •" lines for the intro and for instrumental gaps of [DOTS_FROM] seconds or more; shorter or trailing ones are
 *  dropped. (Up to 0.39.1 only from 5 s: a local file's 3–5 s pauses had none.) */
fun interludes(ls: List<Line>): List<Line> {
    val real = ls.filter { !it.dots }
    if (real.isEmpty()) return real
    val out = ArrayList<Line>()
    if ((real[0].t ?: 0.0) >= DOTS_FROM) out.add(Line(0.0, null, "", dots = true))
    var sungTo = 0.0   // a pause starts once nobody sings: a longer line before this one, or its background vocals, may go on
    ls.forEachIndexed { i, l ->
        out.add(l)
        if (!l.dots && (l.words != null || (l.end != null && l.end != 0.0))) {
            val own = lineEnd(l)!!.let { if (l.end == null && l.words!!.last().end == null) it + 1.2 else it }
            val end = maxOf(own, sungTo); sungTo = end
            val nx = ls.subList(i + 1, ls.size).find { !it.dots }
            if (nx != null && (nx.t ?: 0.0) - end >= DOTS_FROM) out.add(Line(end, null, "", dots = true))
        }
    }
    val res = ArrayList<Line>()
    out.forEachIndexed { i, l ->
        if (!l.dots) { res.add(l); return@forEachIndexed }
        val nx = out.subList(i + 1, out.size).find { !it.dots }
        if (nx == null || (nx.t ?: 0.0) - (l.t ?: 0.0) < DOTS_FROM || (res.isNotEmpty() && res.last().dots)) return@forEachIndexed
        res.add(Line(l.t, nx.t, "", dots = true))
    }
    return res
}

/** The end of a line: the latest of its own end and its words' and background vocals' ends; null when the source
 *  doesn't say (LRC): the line stays lit. */
fun lineEnd(l: Line?): Double? {
    if (l == null || l.dots) return null
    var e = l.end
    fun up(ws: List<Word>?) { ws?.forEach { w -> val x = w.end ?: w.t; if (e == null || x > e!!) e = x } }
    up(l.words); l.bg?.forEach { up(it.words) }
    return e
}

fun findIdx(L: List<Line>, t: Double): Int {
    var a = 0; var b = L.size - 1; var r = -1
    while (a <= b) { val m = (a + b) ushr 1; if ((L[m].t ?: Double.MAX_VALUE) <= t) { r = m; a = m + 1 } else b = m - 1 }
    return r
}

/** Auto timing check: lyrics found by song name can belong to a slightly different version (radio edit, sped up,
 *  remaster) that runs at another pace. When the lyrics' own length is known, or their last line comes after the song
 *  has ended, the lyrics timeline is stretched to fit this song. Returns the pace (1 = as they are). */
fun checkPace(r: LyricsResult, dur: Double): Double {
    if (!r.sync || dur <= 0) return 1.0
    val src0 = r.credit?.source ?: ""
    if (src0 in setOf("spicy_lyrics", "apple_music", "spotify", "file", "amll")) return 1.0   // matched by track id or your own file: exact
    val last = r.lines.lastOrNull { !it.dots && it.t != null }
    var src = r.srcDur?.takeIf { kotlin.math.abs(it - dur) > 1.5 }
    if (src == null && last != null && last.t!! > dur - .5) src = last.t!! + max(2.0, dur * .03)   // a line after the end can't be right
    return if (src != null && kotlin.math.abs(src - dur) / dur < .12) src / dur else 1.0
}
