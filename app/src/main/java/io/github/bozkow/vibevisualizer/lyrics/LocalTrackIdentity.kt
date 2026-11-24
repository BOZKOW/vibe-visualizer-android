package io.github.bozkow.vibevisualizer.lyrics

/** Embedded tags are authoritative. A missing artist is not permission to split a real title at its version dash. */
internal fun localTrackIdentity(file: String, title: String?, artist: String?, album: String?, duration: Double, year: String = ""): SongMeta {
    fun known(s: String?) = s?.trim()?.takeUnless { it.isBlank() || it.equals("<unknown>", true) }
    val filename = TrackNameCleaner.clean(file, "")
    val taggedTitle = known(title)
    val taggedArtist = known(artist)?.takeUnless { it.lowercase() in setOf("unknown", "unknown artist") }
    val cleaned = if (taggedTitle != null) {
        if (taggedArtist != null) TrackNameCleaner.clean(taggedTitle, taggedArtist)
        else TrackNameCleaner.Names(taggedTitle, if (Match.key(filename.title) == Match.key(taggedTitle)) filename.artist else "")
    } else TrackNameCleaner.clean(file, taggedArtist ?: "")
    return SongMeta(title = cleaned.title, artist = cleaned.artist, album = known(album).orEmpty(),
        duration = duration.takeIf { it.isFinite() && it > 0 } ?: 0.0, file = file, year = year)
}

/** Online cache identity is stricter than the owner's manually associated TTML/LRC keys. */
internal fun onlineLyricsIdentity(m: SongMeta): String = if (m.file.isNotBlank())
    listOf("local-v2", m.uri, m.file, m.title, m.artist, m.album, m.isrc, m.duration.toString()).joinToString("|")
else listOf(m.id, m.artist, m.title, Math.round(m.duration).toString()).joinToString("|")
