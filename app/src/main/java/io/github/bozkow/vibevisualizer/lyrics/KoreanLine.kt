package io.github.bozkow.vibevisualizer.lyrics

/* ---- Korean romanization by sound, ported from Spicy Lyrics 6.5.0 (src/utils/Lyrics/RomanizeKorean.ts, by TheX24 for
   Spikerko/spicy-lyrics, AGPL-3.0) ----
   Revised Romanization as it's said rather than letter by letter (사랑해 → saranghae, 좋아 → joa, 신라 → silla). Each
   block splits into its letters, and a final consonant reads by what follows it, longest rule first: final + next
   initial + next vowel, then final + next initial, then alone. When the final's rule covers the next initial, that
   initial isn't read again. The rules run over the whole line, so a final reads by the next block even when the two are
   sung in different syllables (먹|는 → meong|neun). */
object KoreanLine {
    private val CHO = arrayOf("g", "kk", "n", "d", "tt", "r", "m", "b", "pp", "s", "ss", "", "j", "jj", "ch", "k", "t", "p", "h")
    private val JUNG = arrayOf("a", "ae", "ya", "yae", "eo", "e", "yeo", "ye", "o", "wa", "wae", "oe", "yo", "u", "wo", "we", "wi", "yu", "eu", "ui", "i")

    // finals alone and before the next block's initial (and vowel); ㅇ is the silent initial, so "ᆨᄋ" is ㄱ before a
    // vowel; a final ㅎ there is silent (좋아 → joa), before ㄱ, ㄷ or ㅈ it aspirates them (좋다 → jota)
    private val JONG = mapOf(
        "ᆨ" to "k", "ᆨᄋ" to "g", "ᆨᄂ" to "ngn", "ᆨᄅ" to "ngn", "ᆨᄆ" to "ngm", "ᆨᄒ" to "kh",
        "ᆩ" to "kk", "ᆩᄋ" to "kg", "ᆩᄂ" to "ngn", "ᆩᄅ" to "ngn", "ᆩᄆ" to "ngm", "ᆩᄒ" to "kh",
        "ᆪ" to "k", "ᆪᄋ" to "ks", "ᆪᄂ" to "ngn", "ᆪᄅ" to "ngn", "ᆪᄆ" to "ngm", "ᆪᄒ" to "kch",
        "ᆫ" to "n", "ᆫᄅ" to "ll",
        "ᆬ" to "n", "ᆬᄋ" to "nj", "ᆬᄂ" to "nn", "ᆬᄅ" to "nn", "ᆬᄆ" to "nm", "ᆬᄒ" to "nch",
        "ᆭ" to "n", "ᆭᄋ" to "n", "ᆭᄅ" to "nn", "ᆭᄀ" to "nk", "ᆭᄃ" to "nt", "ᆭᄌ" to "nch",
        "ᆮ" to "t", "ᆮᄋ" to "d", "ᆮᄂ" to "nn", "ᆮᄅ" to "nn", "ᆮᄆ" to "nm", "ᆮᄒ" to "th",
        "ᆯ" to "l", "ᆯᄋ" to "r", "ᆯᄂ" to "ll",
        "ᆰ" to "k", "ᆰᄋ" to "lg", "ᆰᄂ" to "ngn", "ᆰᄅ" to "ngn", "ᆰᄆ" to "ngm", "ᆰᄒ" to "lkh",
        "ᆱ" to "m", "ᆱᄋ" to "lm", "ᆱᄂ" to "mn", "ᆱᄅ" to "mn", "ᆱᄆ" to "mm", "ᆱᄒ" to "lmh",
        "ᆲ" to "p", "ᆲᄋ" to "lb", "ᆲᄂ" to "mn", "ᆲᄅ" to "mn", "ᆲᄆ" to "mm", "ᆲᄒ" to "lph",
        "ᆳ" to "t", "ᆳᄋ" to "ls", "ᆳᄂ" to "nn", "ᆳᄅ" to "nn", "ᆳᄆ" to "nm", "ᆳᄒ" to "lsh",
        "ᆴ" to "t", "ᆴᄋ" to "lt", "ᆴᄂ" to "nn", "ᆴᄅ" to "nn", "ᆴᄆ" to "nm", "ᆴᄒ" to "lth",
        "ᆵ" to "p", "ᆵᄋ" to "lp", "ᆵᄂ" to "mn", "ᆵᄅ" to "mn", "ᆵᄆ" to "mm", "ᆵᄒ" to "lph",
        "ᆶ" to "l", "ᆶᄋ" to "r", "ᆶᄂ" to "ll", "ᆶᄅ" to "ll", "ᆶᄆ" to "lm", "ᆶᄒ" to "lh", "ᆶᄀ" to "lk", "ᆶᄃ" to "lt", "ᆶᄌ" to "lch",
        "ᆷ" to "m", "ᆷᄅ" to "mn",
        "ᆸ" to "p", "ᆸᄋ" to "b", "ᆸᄂ" to "mn", "ᆸᄅ" to "mn", "ᆸᄆ" to "mm", "ᆸᄒ" to "ph",
        "ᆹ" to "p", "ᆹᄋ" to "ps", "ᆹᄂ" to "mn", "ᆹᄅ" to "mn", "ᆹᄆ" to "mm", "ᆹᄒ" to "psh",
        "ᆺ" to "t", "ᆺᄋ" to "s", "ᆺᄂ" to "nn", "ᆺᄅ" to "nn", "ᆺᄆ" to "nm", "ᆺᄒ" to "sh",
        "ᆻ" to "t", "ᆻᄋ" to "ss", "ᆻᄂ" to "nn", "ᆻᄅ" to "nn", "ᆻᄆ" to "nm", "ᆻᄒ" to "th",
        "ᆼ" to "ng",
        "ᆽ" to "t", "ᆽᄋ" to "j", "ᆽᄂ" to "nn", "ᆽᄅ" to "nn", "ᆽᄆ" to "nm", "ᆽᄒ" to "ch",
        "ᆾ" to "t", "ᆾᄋ" to "ch", "ᆾᄂ" to "nn", "ᆾᄅ" to "nn", "ᆾᄆ" to "nm", "ᆾᄒ" to "ch",
        "ᆿ" to "k", "ᆿᄋ" to "k", "ᆿᄂ" to "ngn", "ᆿᄅ" to "ngn", "ᆿᄆ" to "ngm", "ᆿᄒ" to "kh",
        "ᇀ" to "t", "ᇀᄋ" to "t", "ᇀᄂ" to "nn", "ᇀᄅ" to "nn", "ᇀᄆ" to "nm", "ᇀᄒ" to "th",
        "ᇁ" to "p", "ᇁᄋ" to "p", "ᇁᄂ" to "mn", "ᇁᄅ" to "mn", "ᇁᄆ" to "mm", "ᇁᄒ" to "ph",
        "ᇂ" to "t", "ᇂᄋ" to "", "ᇂᄀ" to "k", "ᇂᄃ" to "t", "ᇂᄌ" to "ch", "ᇂᄂ" to "nn", "ᇂᄅ" to "nn", "ᇂᄆ" to "mm", "ᇂᄒ" to "t",
        // ㄷ and ㅌ before 이 (and ㄷ before 히/혀) soften to j and ch: 같이 → gachi, 굳이 → guji, 닫혀 → dachyeo
        "ᆮ이" to "j", "ᇀ이" to "ch", "ᆮ히" to "ch", "ᆮ혀" to "ch", "ᆴ이" to "lch",
    )

    /** A Hangul block as its initial, vowel and (if any) final letters; anything else as itself. */
    private fun segment(c: Char): String {
        val x = c.code - 0xAC00
        if (x !in 0..11171) return c.toString()
        val cho = (x / 588 + 0x1100).toChar(); val jung = ((x % 588) / 28 + 0x1161).toChar()
        return if (x % 28 != 0) "$cho$jung${(x % 28 + 0x11A7).toChar()}" else "$cho$jung"
    }

    // the consonant a joined final hands to the next block, as it's sung there
    private val ONSET = Regex("(ch|ss|kk|tt|pp|jj|[a-z])$")

    /** One romanization per character of [text]: Hangul blocks read by sound with the blocks around them, anything else
     *  as written. Where a final and the next initial are read together, the next block's consonant starts that block,
     *  so each block reads as it's sung (같|이 → ga|chi, 먹|는 → meong|neun, 없|어 → eop|seo). */
    fun charReadings(text: String): List<String> {
        val blocks = text.map(::segment)
        var carried: String? = null
        return blocks.mapIndexed { i, b ->
            if (b.length < 2) { carried = null; return@mapIndexed b }
            val next = blocks.getOrElse(i + 1) { "" }
            var piece = carried ?: CHO.getOrNull(b[0].code - 0x1100) ?: b[0].toString()
            carried = null
            piece += JUNG.getOrNull(b[1].code - 0x1161) ?: b[1].toString()
            if (b.length == 3) {
                val last = b[2]
                val joined = if (next.length >= 2) JONG["$last${next[0]}${next[1]}"] ?: JONG["$last${next[0]}"] else null
                if (joined != null) {
                    val c = ONSET.find(joined)?.value ?: ""
                    carried = c
                    piece += joined.substring(0, joined.length - c.length)
                } else piece += JONG[last.toString()] ?: last.toString()
            }
            piece
        }
    }

    fun text(t: String) = charReadings(t).joinToString("")
}
