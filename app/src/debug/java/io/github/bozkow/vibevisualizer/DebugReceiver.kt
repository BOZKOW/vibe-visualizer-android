package io.github.bozkow.vibevisualizer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.launch
import io.github.bozkow.vibevisualizer.core.Defaults
import io.github.bozkow.vibevisualizer.core.S
import io.github.bozkow.vibevisualizer.engine.E
import io.github.bozkow.vibevisualizer.engine.Lyr
import io.github.bozkow.vibevisualizer.lyrics.Credit
import io.github.bozkow.vibevisualizer.lyrics.Person

/** Debug builds only (app/src/debug): a test on the emulator sets any setting and opens any Settings page or pop-up
 *  directly, as Settings would, without finding them on screen:
 *  adb shell am broadcast -n io.github.bozkow.vibevisualizer/.DebugReceiver --es set lstyle --es to focus
 *  … --es page lyrics (or close) · … --es sheet timing (or close) · … --es year 2024 · … --es dj 1 (Spotify's DJ starts talking) · … --es canvas 1 (look up a Canvas for this song)
 *  · … --es files content://media/external/file/1,content://… (play these files one after another: Up next) · … --es seek 150 (s) */
class DebugReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val set = i.getStringExtra("set"); val to = i.getStringExtra("to")
        val page = i.getStringExtra("page"); val sheet = i.getStringExtra("sheet")
        Handler(Looper.getMainLooper()).post {
            if (set != null && to != null) Defaults.S[set]?.let { d ->
                S.set(set, when (d) { is Boolean -> to == "true"; is Int -> to.toInt(); else -> to })
            }
            i.getStringExtra("year")?.let { E.year = it }
            if (i.hasExtra("dj")) E.onSpDj()
            i.getStringExtra("toast")?.let { E.toast(it, "warn") }
            // --es canvas 1: the Canvas is looked up for the song that plays, as for a Spotify song (your files never ask);
            // with Vibe.cfg.canvasUrl an https video, that video is its Canvas (tests "Animated cover before Canvas")
            if (i.hasExtra("canvas")) io.github.bozkow.vibevisualizer.engine.CanvasVideo.lookup(
                io.github.bozkow.vibevisualizer.lyrics.SongMeta(title = E.title, artist = E.artist, album = E.album), E.coverTok)
            // the lyrics' credit names a maker with this picture, and an uploader with --es uploader (Lyr's setter is private)
            i.getStringExtra("avatar")?.let { a ->
                val up = i.getStringExtra("uploader")?.let { Person("uploader", null, null, it) }
                Lyr::class.java.getDeclaredMethod("setCredit", Credit::class.java).apply { isAccessible = true }
                    .invoke(Lyr, Credit("spicy_lyrics", uploader = up, maker = Person("maker", null, null, a)))
            }
            i.getStringExtra("files")?.let { f ->
                val us = f.split(',').map { android.net.Uri.parse(it.trim()) }
                io.github.bozkow.vibevisualizer.core.Vibe.scope.launch {
                    val r = io.github.bozkow.vibevisualizer.engine.Files.sort(us)
                    android.util.Log.i("VV-dbg", "files: ${us.size} given, ${r.audio.size} audio, ${r.lyrics.size} lyrics")
                    E.openFiles(us)
                }
            }
            i.getStringExtra("seek")?.toDoubleOrNull()?.let { io.github.bozkow.vibevisualizer.media.Local.seek((it * 1000).toLong()) }
            if (page != null) { if (page == "close") E.settingsPage = null else E.openSettings(page) }
            if (sheet != null) E.sheet = if (sheet == "close") null else sheet
        }
    }
}
