package io.github.bozkow.vibevisualizer.lyrics

private val ROM_WORD = Regex("\\S+\\s*")

/** A Japanese timing unit may read as several Latin words. Expose their spaces to the row wrapper instead of
 * treating the entire reading as one oversized word. Keep all text and the unit's start/end; with no finer word
 * timings available, its continuous fill is divided by character position inside that same interval. */
internal fun romanizedWordPieces(text: String, time: TS): List<Pair<String, TS>> {
    val words = ROM_WORD.findAll(text).toList()
    if (words.size <= 1) return listOf(text to time)
    val coreEnd = text.trimEnd().length
    val count = text.codePointCount(0, coreEnd).coerceAtLeast(1)
    val duration = (time.end - time.t).coerceAtLeast(0.0)
    fun at(index: Int) = time.t + duration * text.codePointCount(0, index.coerceAtMost(coreEnd)) / count
    return words.mapIndexed { i, word ->
        val from = if (i == 0) 0 else word.range.first
        val to = if (i == words.lastIndex) text.length else words[i + 1].range.first
        text.substring(from, to) to TS(at(from), if (i == words.lastIndex) time.end else at(to))
    }
}
