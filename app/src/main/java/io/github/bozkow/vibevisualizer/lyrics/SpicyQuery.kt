package io.github.bozkow.vibevisualizer.lyrics

import java.net.URLEncoder

/**
 * What your server's `/api/lyrics` is asked for [m] (it asks Spicy Lyrics' API with its key): a Spotify song by its id;
 * a song from another app or your own file by its ISRC when it's known, otherwise by its title, lead artist, album and
 * length, for Spicy Lyrics' search (since 2026-11-05; the server takes only a match it calls exact, within 5 s of the
 * length). Null when there's nothing to ask with.
 */
fun spicyQuery(m: SongMeta): String? {
    fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    return when {
        m.id.isNotEmpty() -> "id=" + enc(m.id)
        m.isrc.isNotBlank() -> "id=" + enc(m.isrc.trim())
        m.title.isNotBlank() && (m.file.isBlank() || m.artist.isNotBlank()) -> buildString {
            append("title=").append(enc(m.title.trim()))
            spicySearchArtist(m.artist).takeIf { it.isNotEmpty() }?.let { append("&artist=").append(enc(it)) }
            m.album.trim().takeIf { it.isNotEmpty() }?.let { append("&album=").append(enc(it)) }
            if (m.duration > 0) append("&duration=").append(Math.round(m.duration))
        }
        else -> null
    }
}

/** The connection diagnostic tests the actual lookup metadata; only an idle app uses an explicitly labelled sample. */
internal fun spicyDiagnosticTarget(current: SongMeta?): SongMeta = current ?: SongMeta(
    id = "0VjIjW4GlUZAMYd2vXMi3b", title = "Blinding Lights", artist = "The Weeknd", duration = 200.0)

/** An ampersand may be part of a band's name; never turn Years & Years into a different artist. */
internal fun spicySearchArtist(artist: String): String = artist.split(Regex(",|\\s+(?:feat\\.?|ft\\.?)\\s+", RegexOption.IGNORE_CASE)).first().trim()
