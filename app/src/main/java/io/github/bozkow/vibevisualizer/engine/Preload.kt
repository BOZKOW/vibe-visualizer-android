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
    private var at = 0   // how many of them Next in this app has gone past before Spotify named a song
    private var readFor = -1   // the song (E.coverTok) the list was read for

    /** Up next (Spicy Lyrics 6.5.0's card): the song that comes after this one, as read from the queue for this song;
     *  null while it isn't known. */
    fun upNext(): SongMeta? = if (readFor == E.coverTok) next.getOrNull(at) else null

    /** Next pressed in this app: the song found ahead that comes now (pressing again before Spotify answers gives the
     *  one after), or null. */
    fun skip(): SongMeta? = next.getOrNull(at)?.also { at++ }

    /** Spotify names the song that plays: the ones found ahead up to it are gone; one that wasn't among them (you
     *  picked another song, or the queue changed) makes the rest unsure, so they're forgotten until the next look. */
    fun arrived(id: String) {
        val i = next.indexOfFirst { it.id == id }
        if (i >= 0) { next = next.drop(i + 1); at = maxOf(0, at - i - 1) } else forget()
    }

    /** The queue changed (shuffle): what was found ahead no longer follows. */
    fun forget() { next = emptyList(); at = 0 }

    private suspend fun read(): List<SongMeta> = when (E.src) {
        "spotify" -> Spotify.queueAhead(AHEAD)
        "media" -> listOfNotNull(MediaApps.nextSong())
        "file" -> listOfNotNull(Local.nextItem()?.let { (u, name) -> E.fileSong(name, Files.tags(u)) })
        else -> emptyList()
    }.filter { it.title.isNotEmpty() }

    /** A song started: in a few seconds (after its own lookups), find the next ones and get them ready. */
    fun songStarted() {
        job?.cancel()
        val ready = S.b("aPreload"); val upNext = S.b("aUpNext")
        if (!ready && !upNext) return
        val tok = E.coverTok
        job = Vibe.scope.launch {
            delay(6000)
            if (tok != E.coverTok) return@launch
            val ms = read()
            if (tok != E.coverTok) return@launch
            if (upNext) launch { lastLook(tok) }
            if (ms.isEmpty() || !ready) { if (ms.isNotEmpty()) { next = ms; at = 0; readFor = tok }; return@launch }
            next = ms; at = 0; readFor = tok
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

    /** Up next shows in a song's last 30 s: the queue is read once more as they begin (one more request to Spotify),
     *  so a song you queued meanwhile is the one it names. A seek forwards counts; the wait checks every 2 s. */
    private suspend fun lastLook(tok: Int) {
        while (true) {
            delay(2000)
            if (tok != E.coverTok) return
            val d = E.curDur(); val left = d - E.curTime()
            if (d > 0 && left in 0.0..36.0) break
        }
        if (E.dj || (E.src == "spotify" && Spotify.repeat == "track")) return
        val ms = read()
        if (tok != E.coverTok || ms.isEmpty()) return
        val first = next.getOrNull(at)?.let { it.id to it.title }
        next = ms; at = 0; readFor = tok
        // a new song next: got ready too, as the ones read 6 s in were
        if (S.b("aPreload") && (ms[0].id to ms[0].title) != first) ms[0].let { m ->
            runCatching { Lyr.preload(m) }; runCatching { CanvasVideo.preload(m) }; if (m.cover.isNotEmpty()) runCatching { Images.load(m.cover) }
        }
    }

    /** The song the Spotify app just started, when it's one found ahead (same name, same length): with its Spotify
     *  details, so its lyrics and Canvas can start before Spotify's Web API names it. */
    fun nextFor(title: String, durMs: Double): SongMeta? =
        next.firstOrNull { it.id.isNotEmpty() && it.title.equals(title, ignoreCase = true) && (durMs <= 0 || abs(it.duration * 1000 - durMs) < 2500) }
}
