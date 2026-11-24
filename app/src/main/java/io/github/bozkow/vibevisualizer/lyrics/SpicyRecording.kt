package io.github.bozkow.vibevisualizer.lyrics

import org.json.JSONObject
import kotlin.math.abs

/** Verify the ID returned by the helper's search, not just its upstream `exact` label. No lyric text is a song title. */
internal object SpicyRecording {
    private val idPattern = Regex("[A-Za-z0-9]{22}")
    private val nextData = Regex("""<script\b[^>]*\bid=["']__NEXT_DATA__["'][^>]*>(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
    fun responseId(json: JSONObject): String? = (json.optJSONObject("Body") ?: json).let { b -> b.optJSONObject("ids")?.optString("spotify").orEmpty().ifBlank { b.optString("id") } }
        .removePrefix("spotify:track:").takeIf(idPattern::matches)

    fun entity(html: String): JSONObject? = nextData.find(html)?.groupValues?.get(1)?.let { data ->
        runCatching { JSONObject(data).getJSONObject("props").getJSONObject("pageProps").getJSONObject("state")
            .getJSONObject("data").getJSONObject("entity") }.getOrNull()
    }

    fun matches(want: SongMeta, id: String, track: JSONObject): Boolean {
        if (track.optString("id") != id || track.optString("type") != "track") return false
        val title = track.optString("name").ifBlank { track.optString("title") }
        // Require the complete version name in BOTH directions; a remix/live/edit is another recording.
        if (Match.key(title) != Match.key(want.title)) return false
        if (want.artist.isBlank()) return false
        val artists = track.optJSONArray("artists") ?: return false
        val acceptedArtists = setOf(Match.key(want.artist), Match.key(spicySearchArtist(want.artist)), Match.key(firstArtist(want.artist)))
        if ((0 until artists.length()).none { Match.key(artists.optJSONObject(it)?.optString("name").orEmpty()) in acceptedArtists }) return false
        val duration = track.optDouble("duration", 0.0) / 1000.0
        if (want.duration > 0 && (!duration.isFinite() || duration <= 0 || abs(want.duration - duration) > 2.0)) return false
        return true
    }

    /** A response with timing outside the file cannot be its synchronized lyrics. Short intros/outros are valid. */
    fun fitsDuration(result: LyricsResult, duration: Double): Boolean = duration <= 0 || !result.sync ||
        result.lines.all { l -> listOfNotNull(l.t, l.end).all { it.isFinite() && it >= 0 && it <= duration + 3 } &&
            l.words.orEmpty().all { it.t.isFinite() && it.t >= 0 && it.t <= duration + 3 && (it.end == null || it.end!!.isFinite() && it.end!! <= duration + 3) } }
}
