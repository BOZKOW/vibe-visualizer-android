package io.github.bozkow.vibevisualizer.lyrics

import io.github.bozkow.vibevisualizer.core.Net
import io.github.bozkow.vibevisualizer.core.NetError
import org.json.JSONArray
import java.net.URLEncoder

/** Translation and romanization from Google Translate's free web endpoint (unofficial), as in the web app. */
object Google {
    private const val GT = "https://translate.googleapis.com/translate_a/single?sl=auto&dt=t"

    class Res(val tr: List<String?>, val rom: List<String?>, val lang: String?)
    private class Chunk(val tr: List<String>?, val rom: List<String>?, val lang: String?)

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    private suspend fun chunk(lines: List<String>, tl: String): Chunk {
        // Google's free endpoint answers 500 for some requests (romanization asked with a longer run of Latin-script
        // lines, into some languages): the same once more under its other client name, then without its romanization
        // (the app romanizes the main scripts itself), then in halves
        var r = ask(lines, tl, "dict-chrome-ex", true)
        if (r.code >= 500) r = ask(lines, tl, "gtx", true)
        if (r.code >= 500) r = ask(lines, tl, "gtx", false)
        if (r.code >= 500 && lines.size > 1) {
            val h = lines.size / 2
            val a = chunk(lines.subList(0, h), tl); val b = chunk(lines.subList(h, lines.size), tl)
            fun <T> join(x: List<T>?, y: List<T>?) = if (x != null && y != null) x + y else null
            return Chunk(join(a.tr, b.tr), join(a.rom, b.rom), a.lang ?: b.lang)
        }
        if (!r.ok) throw NetError(if (r.code == 429) "Google is limiting requests right now, try again later" else "HTTP ${r.code}")
        val d = r.jsonArray() ?: throw NetError("unexpected answer")
        val segs = d.optJSONArray(0) ?: JSONArray()
        fun split(s: String) = s.split(Regex("\\s*\\|\\s*")).map { it.trim() }
        val trText = StringBuilder(); val rm = ArrayList<String>()
        for (i in 0 until segs.length()) {
            val x = segs.optJSONArray(i) ?: continue
            if (!x.isNull(0)) trText.append(x.optString(0))
            if (x.length() > 3 && !x.isNull(3)) x.optString(3).takeIf { it.isNotEmpty() }?.let { rm.add(it) }
        }
        val tr = split(trText.toString())
        val rom = if (rm.isNotEmpty()) split(rm.joinToString(" ")) else emptyList()
        return Chunk(tr.takeIf { it.size == lines.size }, rom.takeIf { it.size == lines.size }, d.optString(2).takeIf { it.isNotEmpty() && it != "null" })
    }

    private suspend fun ask(lines: List<String>, tl: String, client: String, rom: Boolean) =
        Net.fetch(GT + "&client=" + client + (if (rom) "&dt=rm" else "") + "&tl=" + enc(tl) + "&q=" + enc(lines.joinToString("\n|\n")), "google")

    suspend fun all(lines: List<String>, tl: String): Res {
        val tr = ArrayList<String?>(); val rom = ArrayList<String?>(); var lang: String? = null
        var i = 0
        while (i < lines.size) {   // chunks that keep the request URL a sensible size
            var j = i; var len = 0
            while (j < lines.size && (j == i || len + enc(lines[j]).length < 3500)) { len += enc(lines[j]).length + 9; j++ }
            val part = lines.subList(i, j)
            var r = chunk(part, tl)
            if ((r.tr == null || r.rom == null) && part.size > 1) {   // separators got mangled: one line at a time
                val one = part.map { chunk(listOf(it), tl) }
                r = Chunk(one.map { it.tr?.get(0) ?: "" }, one.map { it.rom?.get(0) ?: "" }, r.lang)
            }
            lang = lang ?: r.lang
            part.indices.forEach { k -> tr.add(r.tr?.getOrNull(k)?.ifEmpty { null }); rom.add(r.rom?.getOrNull(k)?.ifEmpty { null }) }
            i = j
        }
        return Res(tr, rom, lang)
    }
}
