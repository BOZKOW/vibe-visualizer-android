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
 *  · … --es files content://media/external/file/1,content://… (play these files one after another: Up next) · … --es seek 150 (s)
 *  · … --es debugInfo 1 (Copy debug info in logcat, tag VV-dbg) · … --es orientation landscape|portrait|auto */
class DebugReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val set = i.getStringExtra("set"); val to = i.getStringExtra("to")
        val page = i.getStringExtra("page"); val sheet = i.getStringExtra("sheet")
        Handler(Looper.getMainLooper()).post {
            i.getStringExtra("creditsPreview")?.let(CreditsPreview::show)
            if (i.hasExtra("coverRecovery")) CoverRecoveryProbe.run()
            if (i.hasExtra("sessionRecovery")) SessionRecoveryProbe.run(c.applicationContext)
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
            if (i.hasExtra("debugInfo")) android.util.Log.i("VV-dbg", Lyr.debugInfo())
            i.getStringExtra("canvasLocal")?.let { u ->
                if (u == "off") { CreditsPreview.stopCanvas(); io.github.bozkow.vibevisualizer.engine.CanvasVideo.clear(false) }
                else io.github.bozkow.vibevisualizer.engine.CanvasVideo.setLocal(android.net.Uri.parse(u))
            }
            if (i.hasExtra("canvasMove")) Handler(Looper.getMainLooper()).postDelayed({ CreditsPreview.moveCanvas() }, 1200)
            i.getStringExtra("pipAt")?.toDoubleOrNull()?.let { E.clockOverride = it; E.poke() }
            if (i.hasExtra("pip")) E.activity?.enterPictureInPictureMode(android.app.PictureInPictureParams.Builder()
                .setAspectRatio(android.util.Rational(16, 9)).build())
            i.getStringExtra("orientation")?.let { o -> E.activity?.requestedOrientation = when (o) {
                "landscape" -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                "portrait" -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                else -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            } }
            if (page != null) { if (page == "close") E.settingsPage = null else E.openSettings(page) }
            if (sheet != null) E.sheet = if (sheet == "close") null else sheet
        }
    }
}
