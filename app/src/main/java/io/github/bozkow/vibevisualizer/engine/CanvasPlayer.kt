package io.github.bozkow.vibevisualizer.engine

import android.graphics.Bitmap
import android.net.Uri
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import io.github.bozkow.vibevisualizer.core.Net
import io.github.bozkow.vibevisualizer.core.S
import io.github.bozkow.vibevisualizer.core.Server
import io.github.bozkow.vibevisualizer.core.Vibe
import io.github.bozkow.vibevisualizer.core.now
import io.github.bozkow.vibevisualizer.lyrics.SongMeta
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.net.URLEncoder
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import io.github.bozkow.vibevisualizer.core.wall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Spotify Canvas videos (from your server, or the endpoint set in Settings) and your own canvas video. While one
 *  plays, the layout makes room for it and the background is the video itself, blurred. */
@OptIn(UnstableApi::class)
object CanvasVideo {
    private var player: ExoPlayer? = null
    private var url: String? = null
    private var spicyId: String? = null   // the song whose Canvas came from Spicy Lyrics' Canvas service
    private var ready = false
    var active by mutableStateOf(false); private set     // a Canvas is loading or playing: its video needs a view to play in
    var cvmode by mutableStateOf(false); private set     // the Canvas layout (once its first frame is shown)
    var cvlive by mutableStateOf(false); private set     // the blurred Canvas behind everything
    var blurred: Bitmap? = null; private set
    var blurVer by mutableIntStateOf(0); private set
    private var view: WeakReference<TextureView>? = null
    private var lastCv = 0.0
    private val grabs = arrayOfNulls<Bitmap>(2); private var grabI = 0   // two small frames, taken in turn (no new bitmap each time)
    private var revealAt = 0.0

    private val listener = object : Player.Listener {
        override fun onRenderedFirstFrame() { ready = true; revealAt = now(); applyMode(); syncPlay() }
        override fun onPlayerError(error: PlaybackException) { if (url != null) {
            android.util.Log.w("VV-canvas", "can't play: ${error.errorCodeName}")
            spicyId?.let { id -> Vibe.scope.launch { SpicyCanvas.forget(id) } }   // remembered, but gone: looked up again next time
            clear(false)
        } }
    }

    // Canvas videos are kept on the phone (200 MB at most, the longest unplayed go first): a song played again, or the
    // next one found ahead, starts at once instead of downloading its video again
    private val cache by lazy { SimpleCache(java.io.File(Vibe.app.cacheDir, "canvas"), LeastRecentlyUsedCacheEvictor(200L shl 20), StandaloneDatabaseProvider(Vibe.app)) }
    private val cached by lazy { CacheDataSource.Factory().setCache(cache).setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true)) }
    private val ahead = LinkedHashMap<String, Pair<String, Boolean>>()   // the next songs' Canvas, found ahead: song → (video, from Spicy's service)
    private var askAt = 0L

    private fun ensure(): ExoPlayer = player ?: ExoPlayer.Builder(Vibe.app).setMediaSourceFactory(DefaultMediaSourceFactory(DefaultDataSource.Factory(Vibe.app, cached))).build().apply {
        volume = 0f; repeatMode = Player.REPEAT_MODE_ONE; addListener(listener)
        view?.get()?.let { setVideoTextureView(it) }
        player = this
    }

    fun bindView(tv: TextureView) {
        view = WeakReference(tv)
        player?.setVideoTextureView(tv)
    }

    /** A view going away: only lets go if it's the one the video plays in (another cover may have taken over). */
    fun unbind(tv: TextureView) {
        if (view?.get() !== tv) return
        view = null; player?.clearVideoTextureView(tv)
    }

    val hasVideo get() = url != null && ready

    fun clear(keepLayout: Boolean) {
        url = null; ready = false; active = false; spicyId = null
        player?.let { it.stop(); it.clearMediaItems() }
        blurred = null; cvlive = false
        if (!keepLayout) cvmode = false
    }

    private fun set(u: String) {
        clear(true)
        url = u; active = true
        ensure().apply { setMediaItem(MediaItem.fromUri(u)); prepare() }
    }

    fun setLocal(u: Uri) = set(u.toString())

    fun lookup(m: SongMeta, tok: Int) {
        Vibe.scope.launch {
            look(m, tok)
            if (tok == E.coverTok && url == null) cvmode = false   // this song has no Canvas: animate back
        }
    }

    private fun aheadKey(m: SongMeta) = m.id.ifEmpty { (m.artist + "|" + m.title).lowercase() }

    /** Your server first (when set), then Spicy Lyrics' Canvas service for Spotify songs. Found ahead: at once. Song
     *  changes in quick succession only look up the one you stop on. */
    private suspend fun look(m: SongMeta, tok: Int) {
        if (!S.canvasOn) return
        ahead.remove(aheadKey(m))?.let { (v, spicy) -> set(v); if (spicy) spicyId = m.id; return }
        val quick = wall() - askAt < 1200; askAt = wall()
        if (quick) { delay(650); if (tok != E.coverTok) return }
        val f = find(m) { tok != E.coverTok } ?: return
        if (tok == E.coverTok && f.first.isNotEmpty()) { set(f.first); if (f.second) spicyId = m.id }
    }

    /** The next song's Canvas, found ahead and its video downloaded, so it plays the moment the song starts. */
    suspend fun preload(m: SongMeta) {
        if (!S.canvasOn) return
        val k = aheadKey(m); if (ahead.containsKey(k)) return
        val f = find(m)?.takeIf { it.first.isNotEmpty() } ?: return   // none, or couldn't tell: looked up as usual then
        ahead[k] = f; while (ahead.size > 12) ahead.remove(ahead.keys.first())
        if (f.first.startsWith("http")) withContext(Dispatchers.IO) { runCatching { CacheWriter(cached.createDataSource(), DataSpec(Uri.parse(f.first)), null, null).cache() } }
    }

    /** The song's Canvas: (video, from Spicy Lyrics' service), ("", false) when there's none, null when it couldn't be
     *  told (or [gone]: another song plays). Your server's lookup is tried once more after 1.5 s when it fails (as
     *  Spicy Lyrics Mobile 0.14.4 does). */
    private suspend fun find(m: SongMeta, gone: () -> Boolean = { false }): Pair<String, Boolean>? {
        val tpl = Vibe.cfg.canvasUrl.trim()
        var failed = false
        if (tpl.isNotEmpty()) {
            var r = askServer(m, tpl)
            if (r == null) { delay(1500); if (gone()) return null; r = askServer(m, tpl) }
            if (gone()) return null
            if (!r.isNullOrEmpty()) return r to false
            failed = r == null
        }
        if (!S.b("cvSpicy") || m.id.isEmpty() || m.ep) return if (failed) null else "" to false
        val v = SpicyCanvas.find(m.id)
        if (gone()) return null
        return if (v != null) v to true else if (failed) null else "" to false
    }

    /** Your server's Canvas for the song: the video, "" when it has none, null when the lookup failed. */
    private suspend fun askServer(m: SongMeta, tpl: String): String? {
        var u = Regex("\\{(\\w+)\\}").replace(tpl) { r ->
            URLEncoder.encode(when (r.groupValues[1]) { "id" -> m.id; "uri" -> m.uri; "title" -> m.title; "artist" -> m.artist; else -> "" }, "UTF-8")
        }
        if (u.startsWith("/")) u = Server.url(u) ?: return ""
        if (m.id.isEmpty() && tpl.contains("{id}")) return ""
        try {
            val r = Net.fetch(u, "canvas")
            if (r.code == 404) return ""
            if (!r.ok) throw IllegalStateException(r.json()?.optString("error")?.ifEmpty { null } ?: "HTTP ${r.code}")
            val ct = r.header("content-type") ?: ""
            val v = when {
                ct.startsWith("video/") -> u
                ct.contains("json") -> r.json()?.let { j -> listOf("url", "canvasUrl", "video", "canvas").firstNotNullOfOrNull { k -> j.optString(k).ifEmpty { null } } }
                else -> r.text.trim()
            }
            return v ?: ""
        } catch (e: kotlinx.coroutines.CancellationException) { throw e
        } catch (e: Exception) { android.util.Log.w("VV-canvas", "lookup: ${e.message}"); return null }   // no Canvas, quietly
    }

    fun applyMode() {
        val has = hasVideo
        cvmode = S.canvasOn && has
        cvlive = S.canvasOn && has && !S.low
        if (!S.canvasOn) player?.pause() else syncPlay()
    }

    fun syncPlay() {
        val p = player ?: return
        if (url == null || !S.canvasOn) return
        if ((E.playing() || E.src == "none") && E.visible && !E.ambient) p.play() else p.pause()
    }

    fun onBackground() { player?.pause() }

    /** The blurred Canvas: 12 times a second (it's a blur: more looks the same and costs a copy from the GPU each
     *  time), a small frame of the video, blurred, as the background. */
    fun frame(t: Double) {
        if (!cvlive || t - lastCv < 80 || !E.visible) return
        lastCv = t
        val tv = view?.get() ?: return
        if (!tv.isAvailable) return
        val bm = grabs[grabI] ?: Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888).also { grabs[grabI] = it }
        if (runCatching { tv.getBitmap(bm) }.isFailure) return
        grabI = 1 - grabI
        Visuals.blurBitmap(bm, S.bgblur * .025, Visuals.bgSat(), Visuals.bgBri())
        blurred = bm; blurVer++
    }

    /** How far the Canvas has faded in (0-1), for the layout change. */
    fun reveal(t: Double) = if (!hasVideo) 0f else ((t - revealAt) / 800).coerceIn(0.0, 1.0).toFloat()

    fun release() { player?.release(); player = null }
}
