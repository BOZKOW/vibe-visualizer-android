package io.github.bozkow.vibevisualizer.lyrics

/* ---- Chinese read by word, as Spicy Lyrics Mobile 0.20 reads it (its PinyinRomanizer, AGPL-3.0, with the `pinyin` 4.0.0
   package's dictionaries, MIT, Copyright (c) 闲耘: app/src/main/resources/romanization/) ----
   Each run of Han characters is read by word, the longest known word first, so a character with several readings takes
   the one its word uses (行走 → xíng zǒu, 长大 → zhǎng dà); a character in no known word takes its most common reading. The
   readings run across the whole line, so a word sung over two syllables keeps its reading. */
object ChineseLine {
    private const val LONGEST_WORD = 9

    private fun resource(name: String) = ChineseLine::class.java.classLoader?.getResourceAsStream(name)?.bufferedReader(Charsets.UTF_8)

    /** Character → its most common reading (null: the dictionary couldn't be read). */
    private val readings: Map<Int, String>? by lazy {
        runCatching {
            val r = resource("romanization/pinyin.txt") ?: return@runCatching null
            HashMap<Int, String>(48000).also { m ->
                r.useLines { lines -> for (line in lines) { if (line.isEmpty()) continue; val c = line.codePointAt(0); m[c] = line.substring(Character.charCount(c)) } }
            }
        }.getOrNull()
    }

    /** Word → its readings, or null when every character keeps its own. */
    private val words: Map<String, List<String>?> by lazy {
        runCatching {
            val r = resource("romanization/pinyin-phrases.txt") ?: return@runCatching emptyMap()
            HashMap<String, List<String>?>(48000).also { m ->
                r.useLines { lines -> for (line in lines) { if (line.isEmpty()) continue; val tab = line.indexOf('\t')
                    if (tab < 0) m[line] = null else m[line.substring(0, tab)] = line.substring(tab + 1).split(' ') } }
            }
        }.getOrDefault(emptyMap())
    }

    /** The dictionary is there (it's in the app; this is false only if it couldn't be read). */
    fun available() = readings != null

    /** One reading per character of [text]: Han characters in pinyin, everything else as written. */
    fun charReadings(text: String): List<String> {
        val dict = readings ?: return text.map(Char::toString)
        val out = text.mapTo(ArrayList(text.length)) { dict[it.code] ?: it.toString() }
        var at = 0
        while (at < text.length) {
            if (dict[text[at].code] == null) { at++; continue }
            var end = at
            while (end < text.length && dict[text[end].code] != null) end++
            // the longest known word starting here, then on from its end
            while (at < end) {
                var len = minOf(LONGEST_WORD, end - at)
                while (len > 1 && text.substring(at, at + len) !in words) len--
                if (len > 1) words[text.substring(at, at + len)]?.forEachIndexed { i, r -> out[at + i] = r }
                at += len
            }
        }
        return out
    }
}
