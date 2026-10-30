package io.github.bozkow.vibevisualizer.engine

import io.github.bozkow.vibevisualizer.core.S
import io.github.bozkow.vibevisualizer.core.Vibe
import io.github.bozkow.vibevisualizer.lyrics.SongMeta
import io.github.bozkow.vibevisualizer.media.Local
import io.github.bozkow.vibevisualizer.media.MediaApps
import io.github.bozkow.vibevisualizer.spotify.Spotify
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/** The next song, got ready while this one plays: a few seconds into a song, the next one's lyrics, Canvas and cover are
 *  looked up, so they show the moment it starts. The next song comes from your Spotify queue (one request a song), the
 *  music app's queue (when the app shares it) or your own files' list. Skipping through songs asks for nothing: only a
 *  song that keeps playing looks ahead. Settings → Lyrics → Get the next song ready. */
object Preload {
    private var job: Job? = null

    /** The next song as found ahead (the Spotify app's song change starts its lookups at once when it's this one). */
    var next: SongMeta? = null; private set

    /** A song started: in a few seconds (after its own lookups), find the next one and get it ready. */
    fun songStarted() {
        job?.cancel()
        if (!S.b("aPreload")) return
        val tok = E.coverTok
        job = Vibe.scope.launch {
            delay(6000)
            if (tok != E.coverTok) return@launch
            val m = when (E.src) {
                "spotify" -> Spotify.queueNext()
                "media" -> MediaApps.nextSong()
                "file" -> Local.nextItem()?.let { (u, name) -> E.fileSong(name, Files.tags(u)) }
                else -> null
            } ?: return@launch
            if (tok != E.coverTok || m.title.isEmpty()) return@launch
            next = m
            launch { runCatching { Lyr.preload(m) } }
            launch { runCatching { CanvasVideo.preload(m) } }
            if (m.cover.isNotEmpty()) launch { runCatching { Images.load(m.cover) } }
        }
    }

    /** The song the Spotify app just started, when it's the one found ahead (same name, same length): with its Spotify
     *  details, so its lyrics and Canvas can start before Spotify's Web API names it. */
    fun nextFor(title: String, durMs: Double): SongMeta? =
        next?.takeIf { it.id.isNotEmpty() && it.title.equals(title, ignoreCase = true) && (durMs <= 0 || abs(it.duration * 1000 - durMs) < 2500) }
}
