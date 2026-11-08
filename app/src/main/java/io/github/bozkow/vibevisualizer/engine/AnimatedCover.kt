// The search is ported from Spicy Lyrics Mobile by TheX24 (github.com/spicylyrics/mobile, AGPL-3.0): MotionCoverFinder
// and the player settings of MotionCover.
package io.github.bozkow.vibevisualizer.engine

import android.graphics.Matrix
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import io.github.bozkow.vibevisualizer.core.Net
import io.github.bozkow.vibevisualizer.core.Net.await
import io.github.bozkow.vibevisualizer.core.Vibe
import io.github.bozkow.vibevisualizer.core.wall
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.lang.ref.WeakReference
import java.net.URLEncoder
import kotlin.math.max

/** What finding an animated cover reads, without Android or the network. */
internal object MotionSearch {
    data class AlbumHit(val url: String, val name: String, val artist: String, val explicit: Boolean = false)

    private val AMBIENT_VIDEO = Regex("<amp-ambient-video[^>]+src=\"([^\"]+\\.m3u8)\"")
    private val TALL_VIDEO = Regex("\"motionDetailTall\":\\{.{0,3000}?\"video\":\"([^\"]+\\.m3u8)\"")
    private val ALBUM_LINK = Regex("music\\.apple\\.com/[a-z]{2}/album/[^/\"?]+/(\\d+)")
    private val EDITION = Regex("\\s*(?:[(\\[][^()\\[\\]]*(?:deluxe|edition|version|remaster|expanded|anniversary|special|bonus|explicit|reissue)[^()\\[\\]]*[)\\]]" +
        "|-\\s*(?:single|ep|deluxe\\b.*|.*\\bedition\\b.*|.*\\bversion\\b.*))\\s*$", RegexOption.IGNORE_CASE)

    /** One per record, by its lead artist; null when there's nothing to search. */
    fun key(artist: String, album: String, title: String): String? {
        val name = album.ifBlank { title }
        return if (artist.isBlank() || name.isBlank()) null else akey(artist) + "|" + akey(name)
    }

    fun akey(s: String) = s.lowercase().filter(Char::isLetterOrDigit)

    /** The first of several artists: the record is under its lead. */
    fun leadArtist(artists: String) = artists.split(", ").first().split(Regex("\\s+(?:feat\\.?|ft\\.?|featuring)\\s+", RegexOption.IGNORE_CASE)).first().trim()

    /** The same name allowing for editions and suffixes, either way round. */
    fun loose(a: String, b: String): Boolean {
        val ka = akey(a); val kb = akey(b)
        if (ka.isEmpty() || kb.isEmpty()) return false
        return ka == kb || (ka.length >= 4 && ka in kb) || (kb.length >= 4 && kb in ka)
    }

    /** The record's name without its edition ("(Deluxe)", "- Single"). */
    fun plain(name: String): String { var o = name; repeat(2) { o = EDITION.replace(o, "") }; return o.trim().ifEmpty { name } }

    /** The album pages worth opening, best first. The artist counts as much as the name (album names are far from unique,
     *  and a loose match would hang another artist's video on this record); among equal names the explicit edition goes
     *  first, the one Apple usually animates. */
    fun rankAlbums(hits: List<AlbumHit>, artist: String, name: String): List<String> =
        hits.asSequence().filter { it.url.isNotEmpty() && loose(name, it.name) && loose(artist, it.artist) }
            .sortedWith(compareBy<AlbumHit> { when { akey(it.name) == akey(name) -> 0; akey(plain(it.name)) == akey(plain(name)) -> 1; else -> 2 } }.thenBy { !it.explicit })
            .distinctBy { albumId(it.url) }.map { it.url }.toList()

    /** iTunes' answer (a song stands for the album it's on). */
    fun parseAlbums(json: String): List<AlbumHit> = runCatching {
        val a = JSONObject(json).getJSONArray("results")
        (0 until a.length()).mapNotNull { i ->
            val o = a.getJSONObject(i)
            if (o.optString("wrapperType") !in setOf("collection", "track")) return@mapNotNull null
            AlbumHit(o.optString("collectionViewUrl").substringBefore('?'), o.optString("collectionName"), o.optString("artistName"), o.optString("collectionExplicitness") == "explicit")
        }
    }.getOrDefault(emptyList())

    fun albumIds(html: String) = ALBUM_LINK.findAll(html).map { it.groupValues[1] }.distinct().take(12).toList()
    fun albumId(url: String) = url.substringAfterLast('/')
    fun ambientVideo(html: String) = AMBIENT_VIDEO.find(html)?.groupValues?.get(1)

    /** The record's tall (3:4) stream, in the page's data: its own player only shows the square one (Mobile 0.19.0). */
    fun tallVideo(html: String) = TALL_VIDEO.find(html)?.groupValues?.get(1)

    /** A record as remembered: when, its square stream ("" none) and its tall one ("" none, null: from before they were read). */
    data class Entry(val at: Long, val url: String, val tall: String?)

    fun parseEntry(v: String): Entry { val p = v.split('|'); return Entry(p[0].toLongOrNull() ?: 0L, p.getOrElse(1) { "" }, p.getOrNull(2)) }
    fun storeEntry(e: Entry) = "${e.at}|${e.url}" + (e.tall?.let { "|$it" } ?: "")
}

/** Apple Music's animated covers: the looping video on an album's page, played over the still cover where an album has
 *  one. No account and no key: iTunes' public search says which album it is, and the album's public music.apple.com page
 *  has the stream. That page's format is Apple's and undocumented, so everything here is best effort: any failure ends
 *  with no video, and the still cover stays. It's only asked for and streamed while a big cover is on screen, and never
 *  beside a Canvas (one video at a time). Settings → Player screen → Animated covers. */
@OptIn(UnstableApi::class)
object AnimatedCover {
    private const val MISS_MS = 30L * 24 * 3600 * 1000   // an album with none is asked about again after a month
    private const val UA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
    private val prefs by lazy { Vibe.app.getSharedPreferences("motioncovers", android.content.Context.MODE_PRIVATE) }   // key → "when|video|tall" (MotionSearch.Entry)
    private val http by lazy { Net.http.newBuilder().cache(null).build() }   // album pages are big: not for the small answers' cache

    /** The record's stream, or null: none, can't tell now, or another song came first (the caller's cancelled then). */
    suspend fun find(artists: String, album: String, title: String): String? = lookup(artists, album, title, tall = false)?.url?.ifEmpty { null }

    /** For a cover taller than it's wide: the record's tall (3:4) stream where Apple has one, else its square one. */
    suspend fun findTall(artists: String, album: String, title: String): String? =
        lookup(artists, album, title, tall = true)?.let { it.tall?.ifEmpty { null } ?: it.url.ifEmpty { null } }

    private suspend fun lookup(artists: String, album: String, title: String, tall: Boolean): MotionSearch.Entry? {
        val artist = MotionSearch.leadArtist(artists)
        val key = MotionSearch.key(artist, album, title) ?: return null
        val known = prefs.getString(key, null)?.let(MotionSearch::parseEntry)
        if (known != null) {
            if (known.url.isNotEmpty() && (known.tall != null || !tall)) return known
            if (known.url.isEmpty() && wall() - known.at < MISS_MS) return known
        }
        delay(800)   // skipping through songs: only the one you stop on is asked about
        var failed = false
        suspend fun get(u: String): String? = try {
            val req = Request.Builder().url(u).header("User-Agent", UA).header("Accept-Language", "en-US,en;q=0.9").build()
            http.newCall(req).await().use { r -> if (r.isSuccessful) withContext(Dispatchers.IO) { r.body.string() } else { failed = true; null } }
        } catch (e: CancellationException) { throw e } catch (e: Exception) { failed = true; null }
        fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
        fun tidy(s: String) = s.split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ")
        suspend fun itunes(term: String, entity: String) = get("https://itunes.apple.com/search?term=${enc(term)}&entity=$entity&limit=10")?.let(MotionSearch::parseAlbums).orEmpty()
        val name = album.ifBlank { title }
        val single = album.isBlank() || MotionSearch.loose(MotionSearch.plain(album), MotionSearch.plain(title))
        val terms = listOfNotNull("$artist $name", "$artist ${MotionSearch.plain(name)}", "$artist $title".takeIf { single && title.isNotBlank() }).map(::tidy).distinct()
        // iTunes' search first, it's quick: the song too, as Apple often animates one edition and a song names the one it's
        // on. Its index runs years behind for some records, so the web search follows
        val stages = listOf<suspend () -> List<MotionSearch.AlbumHit>>(
            { (if (title.isNotBlank()) itunes(tidy("$artist $title"), "song") else emptyList()) + terms.flatMap { itunes(it, "album") } },
            { terms.flatMap { t ->
                val ids = get("https://music.apple.com/us/search?term=${enc(t)}")?.let(MotionSearch::albumIds).orEmpty()
                if (ids.isEmpty()) emptyList() else get("https://itunes.apple.com/lookup?id=${ids.joinToString(",")}")?.let(MotionSearch::parseAlbums).orEmpty()
            } },
        )
        val tried = HashSet<String>(); var found: Pair<String, String?>? = null
        run search@{
            for (stage in stages) for (u in MotionSearch.rankAlbums(stage(), artist, name)) {
                if (!tried.add(MotionSearch.albumId(u))) continue
                val page = get(u) ?: continue
                val square = MotionSearch.ambientVideo(page) ?: continue
                found = square to MotionSearch.tallVideo(page); return@search
            }
        }
        // a failed request isn't "none": asked again next time (a record already known keeps its stream meanwhile)
        if (found == null && failed) return known?.takeIf { it.url.isNotEmpty() }
        val entry = when {
            found != null -> MotionSearch.Entry(wall(), found!!.first, found!!.second.orEmpty())
            known != null && known.url.isNotEmpty() -> MotionSearch.Entry(known.at, known.url, "")   // remembered from before tall ones were read
            else -> MotionSearch.Entry(wall(), "", "")
        }
        prefs.edit().putString(key, MotionSearch.storeEntry(entry)).apply(); trim()
        return entry
    }

    private fun trim() {
        val all = prefs.all; if (all.size <= 800) return
        val e = prefs.edit()
        all.entries.sortedBy { (it.value as? String)?.substringBefore('|')?.toLongOrNull() ?: 0L }.take(all.size - 700).forEach { e.remove(it.key) }
        e.apply()
    }

    /* ---- the video: one player, playing only while a cover shows it ---- */
    var ready by mutableStateOf(false); private set     // its first frame is drawn: it fades in over the still cover
    var aspect by mutableStateOf(1f); private set       // width / height of what plays: under 1 for a tall stream
    private var player: ExoPlayer? = null
    private var view: WeakReference<TextureView>? = null
    private var cur: String? = null
    private var vw = 1f; private var vh = 1f
    private val fit = android.view.View.OnLayoutChangeListener { v, _, _, _, _, _, _, _, _ -> fit(v as TextureView) }
    private val cache by lazy { SimpleCache(File(Vibe.app.cacheDir, "motion"), LeastRecentlyUsedCacheEvictor(150L shl 20), CanvasVideo.cacheDb) }   // a loop is a few MB

    private val listener = object : Player.Listener {
        override fun onRenderedFirstFrame() { ready = true }
        override fun onVideoSizeChanged(s: VideoSize) {
            if (s.width > 0 && s.height > 0) { vw = s.width.toFloat(); vh = s.height.toFloat(); aspect = vw * s.pixelWidthHeightRatio / vh; view?.get()?.let(::fit) }
        }
        override fun onPlayerError(error: PlaybackException) {   // remembered, but gone: looked up again next time; the still cover stays
            android.util.Log.w("VV-cover", "can't play: ${error.errorCodeName}")
            val bad = cur ?: return; ready = false; cur = null
            prefs.all.filterValues { (it as? String)?.split('|')?.drop(1)?.contains(bad) == true }.keys.forEach { prefs.edit().remove(it).apply() }
        }
    }

    private fun ensure(): ExoPlayer = player ?: run {
        val up = DefaultHttpDataSource.Factory().setUserAgent(UA).setAllowCrossProtocolRedirects(true)
        val src = CacheDataSource.Factory().setCache(cache).setUpstreamDataSourceFactory(up).setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        ExoPlayer.Builder(Vibe.app).setMediaSourceFactory(HlsMediaSource.Factory(src)).build().apply {
            volume = 0f; repeatMode = Player.REPEAT_MODE_ONE   // never asks for audio focus: the music is left alone
            // Apple offers up to 1080² (1080 × 1440 tall) at 10 Mbit/s: one steady rendition below that keeps a loop to a few MB
            // and caches cleanly
            trackSelectionParameters = trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                .setMaxVideoSize(1080, 1440).setMaxVideoBitrate(3_600_000).setForceHighestSupportedBitrate(true).build()
            addListener(listener); player = this
        }
    }

    /** A cover shows [url] in [tv] (another cover may have been showing it). */
    fun attach(tv: TextureView, url: String) {
        val p = ensure()
        if (view?.get() !== tv) { view?.get()?.removeOnLayoutChangeListener(fit); view = WeakReference(tv); ready = false; p.setVideoTextureView(tv); tv.addOnLayoutChangeListener(fit) }
        if (url != cur) { cur = url; ready = false; aspect = 1f; p.setMediaItem(MediaItem.fromUri(url)); p.prepare() }
        sync()
    }

    /** That cover is going away: only lets go if it's the one the video plays in. */
    fun detach(tv: TextureView) {
        if (view?.get() !== tv) return
        tv.removeOnLayoutChangeListener(fit); view = null; ready = false
        player?.clearVideoTextureView(tv); player?.pause()
    }

    /** It moves while the music plays, on screen; otherwise it holds its frame. */
    fun sync() {
        val p = player ?: return
        if (view?.get() != null && cur != null && E.visible && !E.ambient && E.playing()) p.play() else p.pause()
    }

    /** object-fit: cover (square, or 3:4 in a cover grown to it). */
    private fun fit(tv: TextureView) {
        val w = tv.width.toFloat(); val h = tv.height.toFloat(); if (w <= 0 || h <= 0) return
        val s = max(w / vw, h / vh); tv.setTransform(Matrix().apply { setScale(vw * s / w, vh * s / h, w / 2, h / 2) })
    }

    fun release() { player?.release(); player = null; cur = null; ready = false }
}
