package io.github.bozkow.vibevisualizer.lyrics

import android.icu.text.Transliterator
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.text.Normalizer

/* ---- romanization the way Spicy Lyrics does it (src/utils/Lyrics/ProcessLyrics.ts, AGPL-3.0; 6.5.0's) ----
   A line's syllables are romanized together, with the line as context, and each gets its own part, so the romanized
   text stays timed with the singing: Japanese read a whole line at a time by kuromoji ([JapaneseLine]), Korean by
   sound across the line ([KoreanLine]), Chinese in pinyin (Android's own ICU), Cyrillic (Russian, or Ukrainian when
   the song has its letters) and Greek with tables. Romanization the lyrics already came with wins. A romanized line
   starts with a capital, and where the romaji ends a word the lyrics run together (Japanese, Chinese) it gets a gap. */
object Romanizer {
    private const val TAG = "VV-rom"
    private val jaLock = Mutex()
    @Volatile private var ja: com.atilika.kuromoji.ipadic.Tokenizer? = null
    private val han by lazy { runCatching { Transliterator.getInstance("Han-Latin") }.getOrNull() }

    // once a script is in the song, one of its characters is enough to romanize a syllable (Spicy Lyrics' Item tests)
    private val ITEM_JA = Regex("[\u3040-\u30FF\u4E00-\u9FFF\u3005\uFF66-\uFF9F]")
    private val ITEM_HAN = Regex("[\u4E00-\u9FFF\u3400-\u4DBF\uF900-\uFAFF]")
    private val UKRAINIAN = Regex("[ЄєІіЇїҐґ]")
    /** Script that reads as nothing may romanize to nothing (a っ sung as a stop, a kanji read by the kana after it). */
    private val RESIDUAL = Regex("[\u3040-\u30FF\u4E00-\u9FFF\uAC00-\uD7AF\u1100-\u11FF\u3130-\u318F\u0400-\u052F\u0370-\u03FF\u1F00-\u1FFF]")
    private val LETTER_OR_DIGIT = Regex("[\\p{L}\\p{N}]")

    /** What the song is written in, for the choices made per song (kanji are Japanese in a song with kana). */
    class Song(val japanese: Boolean, val ukrainian: Boolean)

    fun songOf(lines: List<Line>): Song {
        val all = lines.joinToString("\n") { l -> l.text + (l.bg?.joinToString("") { "\n" + it.text } ?: "") }
        return Song(JA.containsMatchIn(all), UKRAINIAN.containsMatchIn(all))
    }

    /** One unit's syllables romanized ([texts]), and where a word ends after one that runs on into the next ([gaps]). */
    class Unit(val texts: List<String>, val gaps: BooleanArray)

    /** Romanizes one unit: a line's (or its background vocal's) syllables, [partOfWord] true where a syllable runs
     *  straight on into the next one; a line without syllables is one. [reader] is kuromoji (null: not loaded, the old
     *  syllable by syllable spelling then), [pinyin] reads one Han character. */
    fun romanizeUnit(src: List<String>, partOfWord: List<Boolean>, song: Song, reader: JapaneseLine.Reader?, pinyin: (String) -> String): Unit {
        val texts = src.toMutableList(); val n = texts.size
        val gaps = BooleanArray(n)
        if (song.japanese) {
            if (texts.any { ITEM_JA.containsMatchIn(it) }) {
                val res = reader?.let { r -> runCatching { JapaneseLine.romanize(texts.zip(partOfWord), r) }.getOrNull() }
                for (i in 0 until n) {
                    val jp = ITEM_JA.containsMatchIn(texts[i])
                    if (res != null) {
                        gaps[i] = res[i].gapAfter
                        // Japanese punctuation (『』, 〜) in a syllable of its own is written out too
                        if (jp || res[i].text.isNotEmpty()) texts[i] = res[i].text
                    } else if (jp) reader?.word(texts[i])?.let { texts[i] = it }   // the line didn't tokenize: a syllable at a time
                }
            }
        } else if (texts.any { ITEM_HAN.containsMatchIn(it) }) {
            val orig = texts.toList()
            for (i in 0 until n) if (ITEM_HAN.containsMatchIn(texts[i])) texts[i] = chinese(texts[i], pinyin)
            // pinyin reads by character, so a Han syllable is a word of its own
            for (i in 0 until n) {
                val next = orig.getOrNull(i + 1) ?: continue
                if (!LETTER_OR_DIGIT.containsMatchIn(next)) continue
                if (orig[i].lastOrNull()?.let { isHan(it) } == true || isHan(next[0])) gaps[i] = true
            }
        }
        if (texts.any { KO.containsMatchIn(it) }) {
            // read across the whole unit, so a final consonant reads by the next block even in the next syllable
            val line = StringBuilder(); val starts = IntArray(n)
            for (i in 0 until n) { if (i > 0 && !partOfWord[i - 1]) line.append(' '); starts[i] = line.length; line.append(texts[i]) }
            val r = KoreanLine.charReadings(line.toString())
            for (i in 0 until n) if (KO.containsMatchIn(texts[i])) texts[i] = r.subList(starts[i], starts[i] + texts[i].length).joinToString("")
        }
        for (i in 0 until n) if (CYR.containsMatchIn(texts[i])) texts[i] = cyrillic(texts[i], song.ukrainian)
        for (i in 0 until n) if (GR.containsMatchIn(texts[i])) texts[i] = greek(texts[i])
        // the line starts with a capital when its first word is a romanized one (words in Latin letters stay as written)
        val first = src.indexOfFirst { t -> t.any { it.isLetter() } }
        if (first >= 0) {
            val letter = src[first].first { it.isLetter() }
            if (Character.UnicodeScript.of(letter.code) != Character.UnicodeScript.LATIN && texts[first] != src[first]) {
                val t = texts[first]; val k = t.indexOfFirst { it.isLetter() }
                if (k >= 0) texts[first] = t.substring(0, k) + t.substring(k, k + 1).uppercase() + t.substring(k + 1)
            }
        }
        return Unit(texts, gaps)
    }

    private fun isHan(c: Char) = ITEM_HAN.matches(c.toString())

    /** Pinyin, one reading per Han character, spaced from each other and from the words beside them. */
    fun chinese(t: String, pinyin: (String) -> String): String {
        val out = StringBuilder()
        for ((i, c) in t.withIndex()) {
            val prev = t.getOrNull(i - 1)
            if (prev != null && (isHan(c) || isHan(prev)) && LETTER_OR_DIGIT.matches(prev.toString()) && LETTER_OR_DIGIT.matches(c.toString())) out.append(' ')
            out.append(if (isHan(c)) pinyin(c.toString()).trim().ifEmpty { c.toString() } else c.toString())
        }
        return out.toString()
    }

    private fun partOfWord(w: Word) = !w.sp && w.text.lastOrNull()?.isWhitespace() != true

    /** Puts a unit's romanization on its syllables: one the lyrics came romanized keeps it, and if any did, the lyrics'
     *  own word gaps stay too. */
    private fun apply(ws: List<Word>, u: Unit) {
        val supplied = ws.any { it.rom != null && !it.romOwn }
        ws.forEachIndexed { i, w ->
            if (w.rom != null && !w.romOwn) return@forEachIndexed
            w.rom = null; w.romOwn = false; w.romSp = false
            val t = u.texts[i].replace(Regex("\\s+"), " ")
            if (t == w.text && !nonLatin(w.text)) return@forEachIndexed
            if (t.isBlank() && !RESIDUAL.containsMatchIn(w.text)) return@forEachIndexed
            w.rom = t.trim().ifEmpty { "" }; w.romOwn = true
            if (!supplied && i < ws.size - 1 && u.gaps[i] && partOfWord(w)) w.romSp = true
        }
    }

    private fun one(text: String, song: Song, reader: JapaneseLine.Reader?, pinyin: (String) -> String): String? =
        romanizeUnit(listOf(text), listOf(false), song, reader, pinyin).texts[0].replace(Regex("\\s+"), " ").trim().takeIf { it.isNotEmpty() && it != text }

    suspend fun romanizeLyrics(lines: List<Line>) = withContext(Dispatchers.Default) {
        val song = songOf(lines)
        fun needs(t: String) = nonLatin(t)
        val todo = lines.filter { l -> needs(l.text) || l.bg?.any { needs(it.text) } == true }
        if (todo.isEmpty()) return@withContext
        // kuromoji's dictionary, for a song with kana and kanji or kana to read
        val reader: JapaneseLine.Reader? = if (song.japanese) {
            val tk = withTimeoutOrNull(60_000) { japanese() }
            if (tk == null) { Log.w(TAG, "Japanese dictionary unavailable; Google fills in"); null } else JapaneseLine.kuromoji(tk) { t -> jaRomaji(tk, t) }
        } else null
        val py: (String) -> String = { c -> han?.transliterate(c) ?: c }
        fun unit(ws: List<Word>) = runCatching { apply(ws, romanizeUnit(ws.map { it.text }, ws.map(::partOfWord), song, reader, py)) }.onFailure { Log.w(TAG, "line: $it") }
        for (l in todo) {
            val ws = l.words
            if (!ws.isNullOrEmpty()) unit(ws)
            else if (l.apiRom == null && needs(l.text)) l.lineRom = runCatching { one(l.text, song, reader, py) }.getOrNull()
            l.bg?.forEach { b ->
                val bw = b.words
                if (!bw.isNullOrEmpty()) unit(bw)
                else if ((b.rom == null || b.romOwn) && needs(b.text)) { b.rom = runCatching { one(b.text, song, reader, py) }.getOrNull(); b.romOwn = b.rom != null }
            }
        }
        fun join(ws: List<Word>) = ws.joinToString("") { (it.rom ?: it.text) + if (it.sp || it.romSp) " " else "" }.replace(Regex("\\s+"), " ").trim()
        for (l in lines) {
            val ws = l.words
            if (!ws.isNullOrEmpty() && nonLatin(l.text) && ws.any { it.rom != null } && ws.none { nonLatin(it.text) && it.rom == null }) l.sylRom = join(ws)
            l.bg?.forEach { b -> val bw = b.words; if (bw != null && bw.any { it.rom != null } && (b.rom == null || b.romOwn)) { b.rom = join(bw); b.romOwn = true } }
        }
    }

    /** kuromoji's dictionary is big: built once, in the background, the first time a Japanese song plays. */
    private suspend fun japanese(): com.atilika.kuromoji.ipadic.Tokenizer? = jaLock.withLock {
        ja ?: runCatching { com.atilika.kuromoji.ipadic.Tokenizer() }.onFailure { Log.w(TAG, "kuromoji: $it") }.getOrNull().also { ja = it }
    }

    /** The spelling before 0.52.0, a syllable on its own: the fallback when a line doesn't tokenize. */
    private fun jaRomaji(tk: com.atilika.kuromoji.ipadic.Tokenizer, text: String): String {
        val parts = ArrayList<String>()
        var gem = false   // the last word ended in っ: it doubles this one's first consonant (だっ た → da tta)
        for (t in tk.tokenize(text)) {
            val s = t.surface
            if (s.isBlank()) continue
            val particle = t.partOfSpeechLevel1 == "助詞"
            val kana = t.reading?.takeIf { it != "*" } ?: s
            var r = when {
                particle && s == "は" -> "wa"; particle && s == "へ" -> "e"; particle && s == "を" -> "o"
                JA.containsMatchIn(s) || HAN.containsMatchIn(s) -> kanaToRomaji(kana)
                else -> s
            }
            if (gem && r.isNotEmpty() && r[0].isLetter() && r[0].lowercaseChar() !in "aeioun") r = (if (r.startsWith("ch")) "t" else r[0].toString()) + r
            gem = (JA.containsMatchIn(s) || HAN.containsMatchIn(s)) && (kana.endsWith('ッ') || kana.endsWith('っ'))
            parts.add(r)
        }
        return parts.joinToString(" ")   // kuroshiro's "spaced" mode
    }

    // ---- kana → Hepburn ----
    private val KANA = mapOf(
        "キャ" to "kya", "キュ" to "kyu", "キョ" to "kyo", "シャ" to "sha", "シュ" to "shu", "ショ" to "sho", "シェ" to "she",
        "チャ" to "cha", "チュ" to "chu", "チョ" to "cho", "チェ" to "che", "ニャ" to "nya", "ニュ" to "nyu", "ニョ" to "nyo",
        "ヒャ" to "hya", "ヒュ" to "hyu", "ヒョ" to "hyo", "ミャ" to "mya", "ミュ" to "myu", "ミョ" to "myo", "リャ" to "rya",
        "リュ" to "ryu", "リョ" to "ryo", "ギャ" to "gya", "ギュ" to "gyu", "ギョ" to "gyo", "ジャ" to "ja", "ジュ" to "ju",
        "ジョ" to "jo", "ジェ" to "je", "ヂャ" to "ja", "ヂュ" to "ju", "ヂョ" to "jo", "ビャ" to "bya", "ビュ" to "byu",
        "ビョ" to "byo", "ピャ" to "pya", "ピュ" to "pyu", "ピョ" to "pyo", "ファ" to "fa", "フィ" to "fi", "フェ" to "fe",
        "フォ" to "fo", "フュ" to "fyu", "ティ" to "ti", "ディ" to "di", "トゥ" to "tu", "ドゥ" to "du", "デュ" to "dyu",
        "ウィ" to "wi", "ウェ" to "we", "ウォ" to "wo", "ヴァ" to "va", "ヴィ" to "vi", "ヴェ" to "ve", "ヴォ" to "vo",
        "ツァ" to "tsa", "ツィ" to "tsi", "ツェ" to "tse", "ツォ" to "tso", "イェ" to "ye", "クァ" to "kwa", "グァ" to "gwa",
        "ア" to "a", "イ" to "i", "ウ" to "u", "エ" to "e", "オ" to "o", "カ" to "ka", "キ" to "ki", "ク" to "ku", "ケ" to "ke",
        "コ" to "ko", "サ" to "sa", "シ" to "shi", "ス" to "su", "セ" to "se", "ソ" to "so", "タ" to "ta", "チ" to "chi",
        "ツ" to "tsu", "テ" to "te", "ト" to "to", "ナ" to "na", "ニ" to "ni", "ヌ" to "nu", "ネ" to "ne", "ノ" to "no",
        "ハ" to "ha", "ヒ" to "hi", "フ" to "fu", "ヘ" to "he", "ホ" to "ho", "マ" to "ma", "ミ" to "mi", "ム" to "mu",
        "メ" to "me", "モ" to "mo", "ヤ" to "ya", "ユ" to "yu", "ヨ" to "yo", "ラ" to "ra", "リ" to "ri", "ル" to "ru",
        "レ" to "re", "ロ" to "ro", "ワ" to "wa", "ヰ" to "wi", "ヱ" to "we", "ヲ" to "o", "ン" to "n", "ガ" to "ga",
        "ギ" to "gi", "グ" to "gu", "ゲ" to "ge", "ゴ" to "go", "ザ" to "za", "ジ" to "ji", "ズ" to "zu", "ゼ" to "ze",
        "ゾ" to "zo", "ダ" to "da", "ヂ" to "ji", "ヅ" to "zu", "デ" to "de", "ド" to "do", "バ" to "ba", "ビ" to "bi",
        "ブ" to "bu", "ベ" to "be", "ボ" to "bo", "パ" to "pa", "ピ" to "pi", "プ" to "pu", "ペ" to "pe", "ポ" to "po",
        "ヴ" to "vu", "ァ" to "a", "ィ" to "i", "ゥ" to "u", "ェ" to "e", "ォ" to "o", "ャ" to "ya", "ュ" to "yu", "ョ" to "yo",
        "ヮ" to "wa", "ヵ" to "ka", "ヶ" to "ke",
    )

    fun kanaToRomaji(src: String): String {
        val k = buildString { for (c in src) append(if (c in 'ぁ'..'ゖ') c + 0x60 else c) }   // hiragana → katakana
        val out = StringBuilder(); var i = 0; var gem = false
        while (i < k.length) {
            val c = k[i]
            if (c == 'ッ') { gem = true; i++; continue }
            if (c == 'ー') { val v = out.lastOrNull { it in "aeiou" }; if (v != null) out.append(v); i++; continue }
            val two = if (i + 1 < k.length) KANA[k.substring(i, i + 2)] else null
            val r = two ?: KANA[c.toString()]
            if (r == null) { out.append(c); gem = false; i++; continue }
            if (gem) { out.append(if (r.startsWith("ch")) 't' else r[0]); gem = false }
            // ん before a vowel or y: n' keeps it readable (kon'ya)
            if (out.endsWith("n") && i > 0 && k[i - 1] == 'ン' && (r[0] in "aeiouy")) out.append('\'')
            out.append(r); i += if (two != null) 2 else 1
        }
        return out.toString()
    }

    // ---- Cyrillic ----
    private val CYR_MAP = mapOf(
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d", 'е' to "e", 'ё' to "yo", 'ж' to "zh", 'з' to "z", 'и' to "i",
        'й' to "y", 'к' to "k", 'л' to "l", 'м' to "m", 'н' to "n", 'о' to "o", 'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t",
        'у' to "u", 'ф' to "f", 'х' to "kh", 'ц' to "ts", 'ч' to "ch", 'ш' to "sh", 'щ' to "shch", 'ъ' to "", 'ы' to "y", 'ь' to "",
        'э' to "e", 'ю' to "yu", 'я' to "ya", 'є' to "ye", 'і' to "i", 'ї' to "yi", 'ґ' to "g", 'ў' to "u", 'ђ' to "dj", 'ј' to "j",
        'љ' to "lj", 'њ' to "nj", 'ћ' to "c", 'џ' to "dz", 'ѓ' to "gj", 'ќ' to "kj", 'ѕ' to "dz",
    )

    // Ukrainian (its national system): и is y, і is i, г is h, and є ї й ю я start a word as ye yi y yu ya, ie i i iu ia
    // inside one (Київ → Kyiv, Україна → Ukraina); зг is zgh, so it isn't read as zh
    private val UK_MAP = mapOf('и' to "y", 'і' to "i", 'г' to "h", 'ґ' to "g", 'х' to "kh", 'щ' to "shch", 'ь' to "", '\'' to "", 'ʼ' to "", '’' to "")
    private val UK_START = mapOf('є' to "ye", 'ї' to "yi", 'й' to "y", 'ю' to "yu", 'я' to "ya")
    private val UK_IN = mapOf('є' to "ie", 'ї' to "i", 'й' to "i", 'ю' to "iu", 'я' to "ia")

    /** Cyrillic in Latin letters: Russian (and the other languages' letters), or Ukrainian for a song with its letters. */
    fun cyrillic(t: String, ukrainian: Boolean = false): String = buildString {
        for ((i, c) in t.withIndex()) {
            val lo = c.lowercaseChar()
            val prev = t.getOrNull(i - 1)
            val r = if (!ukrainian) CYR_MAP[lo] else {
                val start = prev == null || !prev.isLetter() && prev !in "'ʼ’"
                when {
                    lo == 'г' && prev?.lowercaseChar() == 'з' -> "gh"
                    lo in UK_START -> if (start) UK_START[lo] else UK_IN[lo]
                    lo in UK_MAP && (lo !in "'ʼ’" || (prev?.let { CYR.matches(it.toString()) } == true)) -> UK_MAP[lo]
                    else -> CYR_MAP[lo]
                }
            }
            if (r == null) append(c)
            else if (c != lo && r.isNotEmpty()) {
                // a capital written as two letters is capitalized once (Щ → Shch), unless the word is all capitals
                val caps = t.getOrNull(i + 1)?.let { it.isUpperCase() } == true || (prev?.isUpperCase() == true && t.getOrNull(i + 1)?.isLetter() != true)
                append(if (caps) r.uppercase() else r[0].uppercaseChar() + r.substring(1))
            } else append(r)
        }
    }

    // ---- Greek: ELOT 743 style, with the common two-letter sounds ----
    private val GREEK = mapOf('α' to "a", 'β' to "v", 'γ' to "g", 'δ' to "d", 'ε' to "e", 'ζ' to "z", 'η' to "i", 'θ' to "th",
        'ι' to "i", 'κ' to "k", 'λ' to "l", 'μ' to "m", 'ν' to "n", 'ξ' to "x", 'ο' to "o", 'π' to "p", 'ρ' to "r", 'σ' to "s",
        'ς' to "s", 'τ' to "t", 'υ' to "y", 'φ' to "f", 'χ' to "ch", 'ψ' to "ps", 'ω' to "o")
    private val GR_PAIRS = listOf("ου" to "ou", "αι" to "ai", "ει" to "ei", "οι" to "oi", "μπ" to "b", "ντ" to "nt", "γκ" to "gk", "γγ" to "ng", "αυ" to "av", "ευ" to "ev")

    fun greek(src: String): String {
        val t = Normalizer.normalize(Normalizer.normalize(src, Normalizer.Form.NFD).replace(Regex("[\\u0300-\\u036f]"), ""), Normalizer.Form.NFC)
        val out = StringBuilder(); var i = 0
        while (i < t.length) {
            if (i + 1 < t.length) {
                val two = t.substring(i, i + 2); val lo = two.lowercase(); val pr = GR_PAIRS.find { it.first == lo }
                if (pr != null) { out.append(if (two[0] != lo[0]) pr.second[0].uppercaseChar() + pr.second.substring(1) else pr.second); i += 2; continue }
            }
            val c = t[i]; val l = c.lowercaseChar(); val r = GREEK[l]
            out.append(if (r == null) c.toString() else if (c != l) r[0].uppercaseChar() + r.substring(1) else r); i++
        }
        return out.toString()
    }
}
