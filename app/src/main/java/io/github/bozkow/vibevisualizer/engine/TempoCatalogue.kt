package io.github.bozkow.vibevisualizer.engine

import io.github.bozkow.vibevisualizer.lyrics.Match
import io.github.bozkow.vibevisualizer.lyrics.SongMeta
import io.github.bozkow.vibevisualizer.lyrics.firstArtist
import org.json.JSONObject
import kotlin.math.abs

internal data class TempoTrack(val id: String, val title: String, val artists: List<String>, val duration: Double)
internal data class TempoFeature(val id: String, val spotifyId: String, val isrc: String, val bpm: Double)
private val spotifyTempoId = Regex("[A-Za-z0-9]{22}")
private val tempoIsrc = Regex("[A-Za-z]{2}[A-Za-z0-9]{3}[0-9]{7}")
internal fun validTempoSpotifyId(id: String) = id.takeIf(spotifyTempoId::matches)
internal fun validTempoIsrc(id: String) = id.uppercase().takeIf(tempoIsrc::matches)
internal fun tempoKey(m: SongMeta) = listOf(m.id,m.isrc,Match.key(m.title),Match.key(m.artist),m.duration.toInt().toString()).joinToString("|")

internal fun tempoTracks(json: JSONObject): List<TempoTrack> {
    val rows = json.optJSONArray("content") ?: return emptyList()
    return (0 until rows.length()).mapNotNull { i -> rows.optJSONObject(i)?.let { r ->
        val artists = r.optJSONArray("artists")
        TempoTrack(r.optString("id"),r.optString("trackTitle"), (0 until (artists?.length() ?: 0)).mapNotNull { artists?.optJSONObject(it)?.optString("name") },r.optDouble("durationMs",0.0)/1000)
    } }.filter { it.id.matches(Regex("[a-fA-F0-9-]{36}")) }
}

internal fun matchingTempoTracks(m: SongMeta, rows: List<TempoTrack>): List<TempoTrack> {
    if (m.title.isBlank() || m.artist.isBlank()) return emptyList()
    val artists = setOf(Match.key(m.artist),Match.key(firstArtist(m.artist)))
    val matches = rows.filter { r ->
        Match.key(r.title) == Match.key(m.title) && r.artists.any { Match.key(it) in artists } &&
            r.duration.isFinite() && r.duration > 0 && (m.duration <= 0 || abs(r.duration-m.duration) <= 2.0)
    }
    // Without length, identically named but different recordings are ambiguous.
    if (m.duration <= 0 && matches.isNotEmpty() && matches.maxOf { it.duration }-matches.minOf { it.duration } > 2) return emptyList()
    return matches.distinctBy { it.id }.take(3)
}

internal fun tempoFeatures(json: JSONObject): List<TempoFeature> {
    val rows = json.optJSONArray("content") ?: return emptyList()
    return (0 until rows.length()).mapNotNull { i -> rows.optJSONObject(i)?.let { r ->
        val href = r.optString("href")
        val sid = Regex("https://open\\.spotify\\.com/track/([A-Za-z0-9]{22})(?:[/?].*)?").matchEntire(href)?.groupValues?.get(1).orEmpty()
        TempoFeature(r.optString("id"),sid,r.optString("isrc").uppercase(),r.optDouble("tempo",Double.NaN))
    } }.filter { it.bpm.isFinite() && it.bpm in 30.0..300.0 }
}

/** Only the requested recording can supply BPM. Conflicting duplicate analyses are not averaged into a guess. */
internal fun catalogueTempo(rows: List<TempoFeature>, spotifyId: String? = null, isrc: String? = null, ids: Set<String> = emptySet()): Double? {
    val matching = rows.filter { r -> when {
        spotifyId != null -> r.spotifyId == spotifyId
        isrc != null -> r.isrc.equals(isrc,ignoreCase = true)
        ids.isNotEmpty() -> r.id in ids
        else -> false
    } }.map { it.bpm }.sorted()
    if (matching.isEmpty() || matching.last()-matching.first() > 1.0) return null
    return matching[matching.size/2]
}
