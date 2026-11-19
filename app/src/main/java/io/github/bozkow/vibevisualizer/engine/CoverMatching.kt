package io.github.bozkow.vibevisualizer.engine

import io.github.bozkow.vibevisualizer.lyrics.Match
import io.github.bozkow.vibevisualizer.lyrics.SongMeta
import io.github.bozkow.vibevisualizer.lyrics.firstArtist

/** Search artwork must belong to this artist, song and (when supplied) album, not just a similar result. */
internal fun coverSearchMatch(want: SongMeta, title: String, artist: String, album: String): Boolean {
    val expected = Match.key(firstArtist(want.artist))
    return expected.isNotEmpty() && expected == Match.key(firstArtist(artist)) &&
        (Match.sameTitle(title, want.title) || Match.sameTitle(want.title, title)) &&
        (want.album.isBlank() || Match.key(album) == Match.key(want.album))
}
