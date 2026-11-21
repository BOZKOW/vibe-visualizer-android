// Adapted from Spicy Lyrics Mobile by TheX24, AGPL-3.0.
// upstream f005aaa33a51d1fedfab8af15fce9f929dc41407 (2026-11-21).
package io.github.bozkow.vibevisualizer.lyrics.blend

/*
 * The shape blends work in: the Spicy Lyrics API's JSON document (Content -> Lead/Background ->
 * Syllables), in seconds. Lines and groups are compared by identity, so they are plain classes
 * rather than data classes.
 */

/** One timed syllable. [partOfWord]: it runs on into the next one. */
data class BlendSyllable(
    val text: String,
    val start: Double,
    val end: Double,
    val partOfWord: Boolean,
    /** An onset nobody measured: shared out of a lump by character count. */
    val guess: Boolean = false,
)

/** A lead or background vocal group. */
class BlendGroup(val syllables: List<BlendSyllable>, val start: Double?, val end: Double?)

class BlendLine(
    /** The line's own text, or null for one spelled by its lead syllables. */
    var text: String?,
    var start: Double?,
    var end: Double?,
    var lead: BlendGroup? = null,
    var background: List<BlendGroup> = emptyList(),
    val agent: String? = null,
    val oppositeAligned: Boolean = false,
    /** Cut out of a donor's syllable stream rather than paired with one of its lines. */
    val recut: Boolean = false,
) {
    /** A copy with the vocal parts and times taken off, keeping what identifies the line. */
    fun bare(text: String? = this.text) = BlendLine(text, null, null, agent = agent, oppositeAligned = oppositeAligned)
}

class BlendDoc(val lines: List<BlendLine>, val songwriters: List<String> = emptyList())

enum class BlendQuality(val rank: Int) { NONE(0), STATIC(1), LINE(2), SYLLABLE(3) }
