package io.github.bozkow.vibevisualizer.engine

import io.github.bozkow.vibevisualizer.core.S
import io.github.bozkow.vibevisualizer.core.Vibe
import io.github.bozkow.vibevisualizer.lyrics.SongMeta
import io.github.bozkow.vibevisualizer.media.Local
import io.github.bozkow.vibevisualizer.media.MediaApps
import io.github.bozkow.vibevisualizer.spotify.Spotify
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/** The next songs, got ready while this one plays: a few seconds into a song, the lyrics, Canvas and cover of the next
 *  ones are looked up, so they show the moment one starts. They come from your Spotify queue (the next five, in one
 *  request a song), the music app's queue (when the app shares it) or your own files' list. Skipping through songs
 *  asks for nothing new: only a song that keeps playing looks ahead, and what's already being got ready carries on.
 *  Settings → Lyrics → Get the next songs ready. */
object Preload {
    private const val AHEAD = 5   // from Spotify's queue: skipping a few songs still finds them ready
    private var job: Job? = null    // the wait and the queue request (a song change ends it)
    private var work: Job? = null   // the songs being got ready, one after another (a song change doesn't stop it)

    /** The next songs as found ahead (the Spotify app's song change starts its lookups at once when it's one of them). */
    private var next: List<SongMeta> = emptyList()

    /** A song started: in a few seconds (after its own lookups), find the next ones and get them ready. */
    fun songStarted() {
        job?.cancel()
        if (!S.b("aPreload")) return
        val tok = E.coverTok
        job = Vibe.scope.launch {
            delay(6000)
            if (tok != E.coverTok) return@launch
            val ms = when (E.src) {
                "spotify" -> Spotify.queueAhead(AHEAD)
                "media" -> listOfNotNull(MediaApps.nextSong())
                "file" -> listOfNotNull(Local.nextItem()?.let { (u, name) -> E.fileSong(name, Files.tags(u)) })
                else -> emptyList()
            }.filter { it.title.isNotEmpty() }
            if (tok != E.coverTok || ms.isEmpty()) return@launch
            next = ms
            // the nearest first, one song at a time (its lyrics, Canvas and cover together), so the sources aren't asked
            // for five songs at once; what's ready already (saved, or got ready from an earlier song) is skipped quickly
            work?.cancel()
            work = Vibe.scope.launch {
                for (m in ms) coroutineScope {
                    launch { runCatching { Lyr.preload(m) } }
                    launch { runCatching { CanvasVideo.preload(m) } }
                    if (m.cover.isNotEmpty()) launch { runCatching { Images.load(m.cover) } }
                }
            }
        }
    }

    /** The song the Spotify app just started, when it's one found ahead (same name, same length): with its Spotify
     *  details, so its lyrics and Canvas can start before Spotify's Web API names it. */
    fun nextFor(title: String, durMs: Double): SongMeta? =
        next.firstOrNull { it.id.isNotEmpty() && it.title.equals(title, ignoreCase = true) && (durMs <= 0 || abs(it.duration * 1000 - durMs) < 2500) }
}
