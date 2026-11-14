package io.github.bozkow.vibevisualizer.lyrics

/* ---- Japanese romanization a whole line at a time, ported from Spicy Lyrics 6.5.0 (src/utils/Lyrics/RomanizeJapaneseLine.ts,
   by TheX24 for Spikerko/spicy-lyrics, AGPL-3.0) ----
   kuromoji reads the whole line, so kanji get their reading in context and a word split over several syllables keeps it
   (一|緒 is "is|sho", not "ichi|itoguchi"); the line's reading is then shared back out to the syllables it spans. The
   spelling is the one romanized lyric pages use: long vowels as written (kyou, koohii), particles as said (wa, e, wo), and
   a gap between words with endings kept on their word (kimi no namae wo yonde ita). */
object JapaneseLine {
    /** One syllable's romaji, and whether a word ends after it. */
    data class Rom(val text: String, val gapAfter: Boolean)

    /** A token as the dictionary reads it: [reading] in katakana, particles as said; [start] in the line. */
    class Token(var surface: String, var reading: String, val pos: String, val posDetail: String, var start: Int = 0)

    /** Reads a line into tokens (kuromoji in the app; anything that covers the line in tests); [word] romanizes one
     *  syllable alone, for a line that can't be read whole. */
    fun interface Reader { fun read(text: String): List<Token>; fun word(text: String): String? = null }

    private val PARTICLES = mapOf("は" to "ワ", "へ" to "エ", "を" to "ヲ")
    private val INFLECTING = setOf("動詞", "形容詞", "助動詞")
    private val VOWEL_KANA = mapOf("ア" to "a", "イ" to "i", "ウ" to "u", "エ" to "e", "オ" to "o")
    private val OPENS = Regex("^[「『（(“‘〈《【]$")
    private val RUBY = Regex("[(（]([ぁ-ゖァ-ーー]+)[)）]")
    private val LETTER_OR_DIGIT = Regex("[\\p{L}\\p{N}]")

    fun isKanaChar(c: Char) = c in '぀'..'ヿ'
    fun isKanji(c: Char) = c in '一'..'鿏' || c in '豈'..'﫿' || c in '㐀'..'䶿' || c == '々'
    fun isJapaneseChar(c: Char) = isKanaChar(c) || isKanji(c)
    fun hasJapanese(s: String) = s.any(::isJapaneseChar)
    fun toKatakana(s: String) = buildString { for (c in s) append(if (c in 'ぁ'..'ゖ') c + 0x60 else c) }
    /** Half-width katakana (ｿﾝﾅ) as the dictionary knows it (ソンナ), one for one, so every syllable keeps its length. */
    fun widen(s: String) = buildString { for (c in s) { val w = if (c in '｡'..'ﾟ') java.text.Normalizer.normalize(c.toString(), java.text.Normalizer.Form.NFKC) else c.toString(); append(if (w.length == 1) w else c.toString()) } }

    /** Hepburn as written (ー as the vowel again: koohii). */
    fun romanize(kana: String): String = Romanizer.kanaToRomaji(kana).trim()

    /** A kuromoji tokenizer as a [Reader]. */
    fun kuromoji(tk: com.atilika.kuromoji.ipadic.Tokenizer, alone: ((String) -> String)? = null) = object : Reader {
        override fun word(text: String) = alone?.invoke(text)
        override fun read(text: String) = tk.tokenize(text).map { t ->
            val s = t.surface; val r = t.reading?.takeIf { it.isNotEmpty() && it != "*" }
            Token(s, when {
                t.partOfSpeechLevel1 == "助詞" && PARTICLES.containsKey(s) -> PARTICLES.getValue(s)
                !hasJapanese(s) -> s
                r != null -> toKatakana(r)
                s.all(::isKanaChar) -> toKatakana(s)
                else -> s
            }, t.partOfSpeechLevel1 ?: "", t.partOfSpeechLevel2 ?: "")
        }
    }

    private fun tokenize(reader: Reader, text: String): List<Token> {
        val tokens = reader.read(text).toMutableList()
        // a verb or adjective ending in っ takes the next token in (言っ|て → itte)
        var i = 0
        while (i + 1 < tokens.size) {
            val t = tokens[i]
            if ((t.pos == "動詞" || t.pos == "形容詞") && t.surface.length > 1 && (t.surface.endsWith('っ') || t.surface.endsWith('ッ'))) {
                t.surface += tokens[i + 1].surface; t.reading += tokens[i + 1].reading; tokens.removeAt(i + 1)
            } else i++
        }
        var at = 0
        for (t in tokens) { t.start = at; at += t.surface.length }
        require(tokens.joinToString("") { it.surface } == text) { "tokens don't cover the line" }
        return tokens
    }

    /** A lone vowel that holds the sound before it (うるせ|え → urusee). */
    private fun stretches(t: Token, prev: Token?): Boolean {
        val v = VOWEL_KANA[toKatakana(t.surface)] ?: return false
        if (prev == null || !hasJapanese(prev.surface)) return false
        return romanize(prev.reading).takeLast(1) == v
    }

    /** Endings that stay on the word before (yonde, ita, tomodachisan); a copula after a noun stays its own word. */
    private fun attaches(t: Token, prev: Token?): Boolean =
        (t.pos == "助動詞" && prev != null && prev.pos in INFLECTING) ||
            (t.pos == "助詞" && t.posDetail == "接続助詞") || t.posDetail == "接尾" ||
            (prev != null && (t.surface == "ん" || t.surface == "ン")) ||
            (prev != null && t.surface.isNotEmpty() && t.surface.all { it in "ーｰ〜～" }) ||
            stretches(t, prev)

    /** A reading in sounds: small kana, ー, ン and ッ stay with the kana before them (セン|リ|ガン, イッ|ポ). */
    private fun morae(reading: String): List<String> {
        val out = ArrayList<String>()
        for (c in reading) if (out.isEmpty() || c !in "ャュョァィゥェォヮーンッ") out.add(c.toString()) else out[out.size - 1] += c
        return out
    }

    /** A reading shared between characters by mora, the extra going to the last (名前 → ナ|マエ). */
    private fun share(reading: String, count: Int): List<String> {
        val parts = morae(reading)
        fun cut(x: Double) = if (parts.size < count) Math.round(x).toInt() else Math.floor(x).toInt()
        return List(count) { i -> parts.subList(cut(i.toDouble() * parts.size / count).coerceIn(0, parts.size), cut((i + 1).toDouble() * parts.size / count).coerceIn(0, parts.size)).joinToString("") }
    }

    /** One kana reading per character of a word: kana read as themselves, each run of kanji takes the reading between
     *  them (走り出す → ハシ|リ|ダ|ス). */
    private fun charReadings(surface: String, reading: String): List<String> {
        val runs = Regex("[぀-ヿ]+|[^぀-ヿ]+").findAll(surface).map { it.value }.toList()
        val pattern = Regex("^" + runs.joinToString("") { r -> if (isKanaChar(r[0])) "(" + Regex.escape(toKatakana(r)) + ")" else "(.+?)" } + "$")
        val m = pattern.find(reading) ?: return share(reading, surface.length)
        return runs.flatMapIndexed { i, r -> if (isKanaChar(r[0])) toKatakana(r).map { it.toString() } else share(m.groupValues[i + 1], r.length) }
    }

    /** Kanji read by the kana in brackets after them (傾(かぶ)けば): the kanji and brackets read nothing, the kana themselves. */
    private fun rubyReadings(text: String, tokens: List<Token>): Map<Int, String> {
        val out = HashMap<Int, String>()
        for (m in RUBY.findAll(text)) {
            val open = m.range.first
            val word = tokens.lastOrNull { it.start + it.surface.length == open } ?: continue
            var kanji = 0
            while (kanji < word.surface.length && isKanji(word.surface[word.surface.length - 1 - kanji])) kanji++
            if (kanji == 0) continue
            for (i in open - kanji until open) out[i] = ""
            val close = m.range.last
            for (i in open..close) out[i] = if (i == open || i == close) "" else toKatakana(text[i].toString())
        }
        return out
    }

    /** Romanizes one line's syllables ([parts]: their text, and whether each runs straight on into the next one, part of
     *  the same word) with the whole line as the dictionary's context: one [Rom] per syllable. */
    fun romanize(parts: List<Pair<String, Boolean>>, reader: Reader): List<Rom> {
        // syllables of different words are spaced apart, so the dictionary sees the lyrics' word breaks; not between two
        // Japanese ones (many lyrics put a space after every syllable, and the dictionary would read kanji one by one)
        val text = StringBuilder(); val owner = ArrayList<Int>(); val startsAt = IntArray(parts.size)
        parts.forEachIndexed { i, (t, _) ->
            val prev = parts.getOrNull(i - 1)
            if (prev != null && !prev.second && !(prev.first.lastOrNull()?.let(::isJapaneseChar) == true && t.firstOrNull()?.let(::isJapaneseChar) == true)) {
                text.append(' '); owner.add(-1)
            }
            startsAt[i] = text.length
            text.append(widen(t)); repeat(t.length) { owner.add(i) }
        }
        val line = text.toString()
        val tokens = tokenize(reader, line)
        val ruby = rubyReadings(line, tokens)
        class Piece(val syl: Int, val kana: String, val newWord: Boolean)
        val kana = List(parts.size) { ArrayList<Pair<String, Boolean>>() }
        val startsWord = BooleanArray(parts.size)
        var lastReading: String? = null
        tokens.forEachIndexed { ti, tok ->
            val newWord = ti > 0 && LETTER_OR_DIGIT.containsMatchIn(tok.surface) && !attaches(tok, tokens[ti - 1])   // punctuation never starts a word
            if (newWord) { val syl = startsAt.indexOf(tok.start); if (syl > 0) startsWord[syl] = true }
            var readings: List<String> = when {
                // a 々 the dictionary didn't read repeats the kanji before it (燦々 → san san)
                tok.surface.all { it == '々' } && tok.reading.firstOrNull()?.let(::isKanaChar) != true && lastReading != null -> tok.surface.map { lastReading!! }
                hasJapanese(tok.surface) -> charReadings(tok.surface, tok.reading)
                else -> tok.surface.map { it.toString() }
            }
            readings = readings.mapIndexed { i, r -> ruby[tok.start + i] ?: r }
            lastReading = readings.lastOrNull { it.isNotEmpty() } ?: lastReading
            val bySyl = sortedMapOf<Int, String>()
            readings.forEachIndexed { i, r -> val s = owner.getOrElse(tok.start + i) { -1 }; if (s >= 0) bySyl[s] = (bySyl[s] ?: "") + r }
            var first = true
            for ((s, piece) in bySyl) { if (piece.isNotBlank()) kana[s].add(piece to (first && newWord)); first = false }
        }
        val pieces = kana.flatMapIndexed { s, l -> l.map { (p, nw) -> Piece(s, p, nw) } }
        class Out(val text: String, val newWord: Boolean, val opens: Boolean)
        val romaji = List(parts.size) { ArrayList<Out>() }
        var lastVowel = ""
        pieces.forEachIndexed { k, p ->
            // a ー at a syllable's start holds the vowel before it (セ|ー → se|e)
            val marks = p.kana.takeWhile { it == 'ー' || it == 'ｰ' }.length
            val body = p.kana.drop(marks)
            var out = when {
                body == "ヲ" -> "wo"
                !(body.endsWith('っ') || body.endsWith('ッ')) -> romanize(body)
                else -> {   // a っ ending a syllable keeps the consonant it doubles there (黙っ|て → damat|te); before a vowel or the end, nothing
                    val next = pieces.getOrNull(k + 1)?.let { romanize(it.kana) } ?: ""
                    val c = next.firstOrNull()?.lowercaseChar()
                    val doubled = when { next.startsWith("ch") -> "t"; c != null && c in 'a'..'z' && c !in "aeioun" -> c.toString(); else -> "" }
                    romanize(body.trimEnd('っ', 'ッ')) + doubled
                }
            }
            if (marks > 0) out = lastVowel.repeat(marks) + out
            out = out.replace('『', '"').replace('』', '"').replace('〜', '~')
            Regex("[aeiou](?=[^a-z]*$)", RegexOption.IGNORE_CASE).find(out)?.let { lastVowel = it.value.lowercase() }
            romaji[p.syl].add(Out(out, p.newWord, OPENS.matches(p.kana.trim())))
        }
        return List(parts.size) { i ->
            val kept = romaji[i].filter { it.text.isNotEmpty() }
            val joined = kept.mapIndexed { j, e -> if (j > 0 && e.newWord && !kept[j - 1].opens) " " + e.text else e.text }.joinToString("").trim()
            if (joined.isEmpty()) Rom("", false)
            else {
                var n = i + 1
                while (n < parts.size && !startsWord[n] && romaji[n].all { it.text.isEmpty() }) n++
                Rom(joined, n < parts.size && startsWord[n] && kept.lastOrNull()?.opens != true)
            }
        }
    }
}
