package io.github.bozkow.vibevisualizer.spotify

import io.github.bozkow.vibevisualizer.engine.coverSearchMatch
import io.github.bozkow.vibevisualizer.lyrics.SongMeta

/** A name-search result is usable only for the requested recording, never just because it ranked first. */
internal fun spotifyRecordingMatch(want: SongMeta, title: String, artist: String, album: String, durationMs: Long, isrc: String): Boolean =
    coverSearchMatch(want, title, artist, album) &&
        (want.duration <= 0 || durationMs > 0 && kotlin.math.abs(want.duration * 1000 - durationMs) <= 1_500) &&
        (want.isrc.isBlank() || want.isrc.equals(isrc, ignoreCase = true))
