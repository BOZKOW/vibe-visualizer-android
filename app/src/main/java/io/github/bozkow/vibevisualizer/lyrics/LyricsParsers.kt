package io.github.bozkow.vibevisualizer.lyrics

import org.json.JSONArray
import org.json.JSONObject
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

class Parsed(val lines: List<Line>, val sync: Boolean, val writers: List<String> = emptyList())

private val LRC_LINE = Regex("""^((?:\[\d+:\d+(?:[.:]\d+)?]\s*)+)(.*)$""")
private val LRC_TIME = Regex("""\[(\d+):(\d+(?:[.:]\d+)?)]""")
private val LRC_WORD_TIME = Regex("""<(\d+):(\d+(?:[.:]\d+)?)>""")
private fun lrcSec(m: String, s: String) = m.toDouble() * 60 + s.replace(':', '.').toDouble()

/** Enhanced LRC's word timing ("<00:12.34>Hello <00:12.80>world<00:13.20>", as LRCLIB and Unison sometimes have it):
 *  each word (or syllable) runs to the next stamp. Null without word stamps, or with text before the first one. */
private fun lrcWords(s: String): List<Word>? {
    val st = LRC_WORD_TIME.findAll(s).toList()
    if (st.isEmpty() || s.substring(0, st[0].range.first).isNotBlank()) return null
    val out = ArrayList<Word>()
    for ((i, m) in st.withIndex()) {
        val nx = st.getOrNull(i + 1)
        val seg = s.substring(m.range.last + 1, nx?.range?.first ?: s.length)
        if (seg.firstOrNull()?.isWhitespace() == true) out.lastOrNull()?.sp = true   // " world": a space after the word before
        val txt = seg.trim(); if (txt.isEmpty()) continue
        val t = lrcSec(m.groupValues[1], m.groupValues[2])
        out.add(Word(t, nx?.let { lrcSec(it.groupValues[1], it.groupValues[2]) }?.takeIf { it > t }, txt, seg.lastOrNull()?.isWhitespace() == true))
    }
    return out.takeIf { it.isNotEmpty() }
}

fun parseLRC(t: String): List<Line> {
    val out = ArrayList<Line>()
    for (ln in t.split(Regex("\r?\n"))) {
        val m = LRC_LINE.find(ln) ?: continue
        val txt = m.groupValues[2].replace(LRC_WORD_TIME, "").replace(Regex("\\s+"), " ").trim()   // empty timed lines mark instrumental breaks
        val times = LRC_TIME.findAll(m.groupValues[1]).toList()
        // word stamps only make sense for a line sung once (a line on several stamps would get the first one's times)
        val ws = if (txt.isNotEmpty() && times.size == 1) lrcWords(m.groupValues[2]) else null
        for (tm in times) {
            val s = lrcSec(tm.groupValues[1], tm.groupValues[2])
            out.add(if (txt.isEmpty()) Line(s, null, "", dots = true) else if (ws != null) Line(s, ws.last().end, txt, words = ws) else Line(s, null, txt))
        }
    }
    out.sortBy { it.t }
    // a line with its translation right under it on the same stamp (two lines, one time; Spicy Lyrics Mobile 0.16.0):
    // the second is the translation, not a line of its own
    val merged = ArrayList<Line>(out.size)
    var i = 0
    while (i < out.size) {
        val a = out[i]; val b = out.getOrNull(i + 1)
        if (b != null && !a.dots && !b.dots && a.t == b.t && out.getOrNull(i + 2)?.t != a.t) { a.apiTr = b.text; merged.add(a); i += 2 }
        else { merged.add(a); i++ }
    }
    return merged
}

fun ttSec(x: String): Double {
    val s = x.trim()
    if (s.endsWith("s")) return s.dropLast(1).toDoubleOrNull() ?: 0.0
    return s.split(':').fold(0.0) { a, v -> a * 60 + (v.toDoubleOrNull() ?: 0.0) }
}

private fun Node.local() = nodeName.substringAfter(':')
private fun Element.attr(n: String): String? = if (hasAttribute(n)) getAttribute(n) else null
private fun Element.childElements(): List<Element> = (0 until childNodes.length).mapNotNull { childNodes.item(it) as? Element }

private fun allByLocal(root: Node, name: String, out: MutableList<Element> = ArrayList()): MutableList<Element> {
    val ch = root.childNodes
    for (i in 0 until ch.length) {
        val n = ch.item(i)
        if (n is Element) { if (n.local() == name) out.add(n); allByLocal(n, name, out) }
    }
    return out
}

fun parseTTML(x0: String): Parsed {
    val f = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = false; isValidating = false }
    // a file starting with a byte-order mark or spaces, or using &nbsp; (HTML's, not XML's), reads too (Spicy Lyrics Mobile 0.16.1)
    val x = x0.trimStart('\uFEFF', ' ', '\t', '\r', '\n').replace("&nbsp;", "&#160;")
    val d = try { f.newDocumentBuilder().parse(ByteArrayInputStream(x.toByteArray(Charsets.UTF_8))) }
    catch (e: Exception) { throw IllegalArgumentException("The TTML file is not valid XML.") }
    val out = ArrayList<Line>()
    // duets: who sings each line (ttm:agent on the line or its block). The first singer stays left, other singers go right
    val people = allByLocal(d, "agent").filter { (it.attr("type") ?: "person") == "person" }.mapNotNull { it.attr("xml:id") ?: it.attr("id") }
    fun agentOf(p: Element): String = p.attr("ttm:agent") ?: (p.parentNode as? Element)?.attr("ttm:agent") ?: ""
    var main = people.firstOrNull() ?: ""
    fun role(n: Node): String = (n as? Element)?.let { it.attr("ttm:role") ?: it.attr("role") } ?: ""
    fun isBg(n: Node) = role(n).contains("x-bg")
    fun isSide(n: Node) = Regex("x-translation|x-roman").containsMatchIn(role(n))   // translation / romanization spans (AMLL TTML) aren't sung text
    fun take(par: Node, words: MutableList<Word>) {
        val ch = par.childNodes
        for (i in 0 until ch.length) {
            val n = ch.item(i)
            if (isBg(n) || isSide(n)) continue
            if (n is Element && n.hasAttribute("begin")) {
                // a word with no end, or one before its start, runs to the next word (it keeps a length)
                val b = ttSec(n.getAttribute("begin"))
                words.add(Word(b, n.attr("end")?.let { ttSec(it) }?.takeIf { it > b }, n.textContent, false))
            } else if (n.nodeType == Node.TEXT_NODE && n.nodeValue.any { it.isWhitespace() } && words.isNotEmpty()) words.last().sp = true
        }
    }
    fun fix(ws: MutableList<Word>): MutableList<Word> {
        if (ws.isNotEmpty() && ws.none { it.sp || it.text.lastOrNull()?.isWhitespace() == true }) ws.forEach { it.sp = true }
        return ws
    }
    // a word-synced file (its timing="Word", or lines with several timed words): a line of one word ("Oh") is
    // word-synced too, not a line with no words
    val root = d.documentElement
    val timing = (0 until root.attributes.length).map { root.attributes.item(it) }.firstOrNull { it.nodeName.endsWith("timing") }?.nodeValue?.lowercase()
    val wordDoc = timing == "word" || allByLocal(d, "p").any { p -> p.childElements().count { it.hasAttribute("begin") && !isBg(it) && !isSide(it) } > 1 }
    for (p in allByLocal(d, "p")) {
        val b = p.attr("begin") ?: continue
        val ag = agentOf(p); if (main.isEmpty() && ag.isNotEmpty()) main = ag
        val words = ArrayList<Word>(); take(p, words); fix(words)
        val bg = p.childElements().filter { isBg(it) }.map { n ->
            val ws = ArrayList<Word>(); take(n, ws); fix(ws)
            val t = StringBuilder(); val ch = n.childNodes
            for (i in 0 until ch.length) if (!isSide(ch.item(i))) t.append(ch.item(i).textContent)
            Bg(t.toString().replace(Regex("\\s+"), " ").trim(), null, ws.ifEmpty { null })
        }.filter { it.text.isNotEmpty() }
        val sb = StringBuilder(); val ch = p.childNodes
        for (i in 0 until ch.length) { val n = ch.item(i); if (!isBg(n) && !isSide(n)) sb.append(n.textContent) }
        val text = sb.toString().replace(Regex("\\s+"), " ").trim()
        if (text.isEmpty()) continue
        fun side(r: String): Pair<String, String>? {
            val n = p.childElements().find { Regex(r).containsMatchIn(role(it)) } ?: return null
            return n.textContent.replace(Regex("\\s+"), " ").trim() to (n.attr("xml:lang") ?: "").split('-')[0]
        }
        val trS = side("x-translation"); val roS = side("x-roman")
        val opp = ag.isNotEmpty() && ag != main && (people.isEmpty() || ag in people)
        out.add(Line(ttSec(b), p.attr("end")?.let { ttSec(it) }, text, if (words.size > 1 || (words.size == 1 && wordDoc)) words else null, bg.ifEmpty { null }, opp,
            apiTr = trS?.first?.ifEmpty { null }, apiTrLang = trS?.first?.takeIf { it.isNotEmpty() }?.let { trS.second.ifEmpty { "zh" } },
            apiRom = roS?.first?.ifEmpty { null }))
    }
    out.sortBy { it.t }
    val writers = allByLocal(d, "songwriter").map { it.textContent.trim() }.filter { it.isNotEmpty() }   // credits
    return Parsed(out, true, writers)
}

fun fromText(t: String): Parsed {
    if (Regex("<tt[\\s>]", RegexOption.IGNORE_CASE).containsMatchIn(t)) return parseTTML(t)
    if (Regex("\\[\\d+:\\d+").containsMatchIn(t)) { val l = parseLRC(t); if (l.isNotEmpty()) return Parsed(l, true) }
    return Parsed(t.split(Regex("\r?\n")).map { it.trim() }.filter { it.isNotEmpty() }.map { Line(null, null, it) }, false)
}

/* Spicy Lyrics API (https://developers.spicylyrics.org): Body.Type is the sync type (Syllable / Line / Static) */
private fun JSONObject.dbl(k: String): Double? = if (has(k) && !isNull(k)) optDouble(k).takeIf { !it.isNaN() } else null
private fun JSONObject.str(k: String): String? = if (has(k) && !isNull(k)) optString(k).takeIf { it.isNotEmpty() } else null

fun parseSpicy(j: JSONObject, dur: Double?): LyricsResult {
    val b = j.optJSONObject("Body") ?: j
    val type = b.optString("Type", "Static").ifEmpty { "Static" }
    fun sylls(o: JSONObject?): List<Word>? {
        val a = o?.optJSONArray("Syllables") ?: return null
        if (a.length() == 0) return null
        return (0 until a.length()).map { i -> val x = a.getJSONObject(i)
            Word(x.optDouble("StartTime"), x.dbl("EndTime"), if (x.isNull("Text")) "" else x.optString("Text"), !x.optBoolean("IsPartOfWord"), x.str("TransliteratedText")) }
    }
    fun textOf(o: JSONObject?, ws: List<Word>?): String =
        (o?.str("Text") ?: ws?.joinToString("") { it.text + if (it.sp) " " else "" } ?: "").trim()
    val content: JSONArray = b.optJSONArray("Content") ?: b.optJSONArray("Lines") ?: JSONArray()   // Static lyrics may come as Lines[{Text}]
    var lines = (0 until content.length()).map { i ->
        val it = content.getJSONObject(i)
        val lead = it.optJSONObject("Lead") ?: it
        val ws = sylls(lead)
        val bgA = it.optJSONArray("Background")
        val bg = (0 until (bgA?.length() ?: 0)).map { k -> val bo = bgA!!.getJSONObject(k); val bw = sylls(bo); Bg(textOf(bo, bw), bo.str("TransliteratedText"), bw) }.filter { x -> x.text.isNotEmpty() }
        Line(lead.dbl("StartTime") ?: it.dbl("StartTime"), lead.dbl("EndTime") ?: it.dbl("EndTime"),
            if (it.has("Text") && !it.isNull("Text")) it.optString("Text").trim() else textOf(lead, ws),
            if (type == "Syllable") ws else null, bg, it.optBoolean("OppositeAligned"),
            apiRom = lead.str("TransliteratedText") ?: it.str("TransliteratedText"), apiTr = lead.str("TranslatedText") ?: it.str("TranslatedText"))
    }.filter { it.text.isNotEmpty() }
    val sync = type != "Static" && lines.isNotEmpty() && lines.all { it.t != null }
    if (sync) {
        val mx = lines.maxOf { it.end ?: it.t!! }
        if (mx > 10000 || (dur != null && dur > 0 && mx > dur * 3)) {   // milliseconds -> seconds
            val k = { v: Double? -> v?.div(1000) }
            lines.forEach { l -> l.t = k(l.t); l.end = k(l.end); l.words?.forEach { w -> w.t /= 1000; w.end = k(w.end) }
                l.bg?.forEach { bb -> bb.words?.forEach { w -> w.t /= 1000; w.end = k(w.end) } } }
        }
        lines = lines.sortedBy { it.t }
    } else lines.forEach { l -> l.t = null; l.words = null; l.bg = l.bg?.map { Bg(it.text, it.rom, null) } }
    val ua = b.optJSONObject("UploadAttribution")
    val sw = b.optJSONArray("SongWriters")
    return LyricsResult(lines, sync, "Spicy Lyrics", Credit(b.str("source"), uploader = personFrom(ua?.optJSONObject("Uploader")),
        maker = personFrom(ua?.optJSONObject("Maker")), writers = sw?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList()))
}
