package io.github.bozkow.vibevisualizer

import android.os.Handler
import android.os.Looper
import android.util.Log
import io.github.bozkow.vibevisualizer.core.S
import io.github.bozkow.vibevisualizer.engine.E
import io.github.bozkow.vibevisualizer.engine.Lyr
import io.github.bozkow.vibevisualizer.lyrics.Bg
import io.github.bozkow.vibevisualizer.lyrics.Credit
import io.github.bozkow.vibevisualizer.lyrics.Line
import io.github.bozkow.vibevisualizer.lyrics.LyricsResult
import io.github.bozkow.vibevisualizer.lyrics.Person
import io.github.bozkow.vibevisualizer.lyrics.Word

/** Silent, uncached UI fixture. Restores source, lyrics, clock and settings with --es creditsPreview off. */
object CreditsPreview {
    private val fixtureNote = E.Snack("Backdrop preview", "err", sub = "Live cover and title behind the glass")
    private var restore: (() -> Unit)? = null
    private var canvasRestore: (() -> Unit)? = null
    fun stopCanvas() { canvasRestore?.invoke(); canvasRestore = null }

    /** Fake only Vibe's follower state while a muted local Canvas is previewed. No MediaSession,
     * audio focus or command to another player. Never replace a real followed session. */
    fun moveCanvas() {
        val follower = io.github.bozkow.vibevisualizer.media.MediaApps
        if (canvasRestore != null || E.src != "file" || follower.pkg.isNotEmpty()) return
        val field = io.github.bozkow.vibevisualizer.spotify.SessionFollower::class.java.getDeclaredField("state").apply { isAccessible = true }
        val duration = io.github.bozkow.vibevisualizer.spotify.SessionFollower::class.java.getDeclaredField("durMs").apply { isAccessible = true }
        val oldState = field.get(follower); val oldDur = follower.durMs; val oldSrc = E.src; val hide = S.str("uiHide")
        val fake = android.media.session.PlaybackState.Builder().setState(android.media.session.PlaybackState.STATE_PLAYING, 1000, 1f).build()
        canvasRestore = {
            if (field.get(follower) === fake) { field.set(follower, oldState); duration.setLong(follower, oldDur) }
            if (E.src == "media") E.src = oldSrc
            S.set("uiHide", hide)
        }
        field.set(follower, fake); duration.setLong(follower, 180000L); E.src = "media"; S.set("uiHide", "never")
        io.github.bozkow.vibevisualizer.engine.CanvasVideo.syncPlay(); E.poke()
    }
    private fun apply(r: LyricsResult) = Lyr::class.java.getDeclaredMethod("apply", LyricsResult::class.java)
        .apply { isAccessible = true }.invoke(Lyr, r)
    fun show(mode: String) {
        if (mode == "off") { stopCanvas(); restore?.invoke(); restore = null; return }
        if (restore == null) {
            val src = E.src; val title = E.title; val artist = E.artist; val clock = E.clockOverride
            val cover = io.github.bozkow.vibevisualizer.engine.Visuals.cover
            val shown = io.github.bozkow.vibevisualizer.engine.Preload::class.java.getDeclaredField("shown\$delegate").apply { isAccessible = true }
                .get(io.github.bozkow.vibevisualizer.engine.Preload) as androidx.compose.runtime.MutableState<io.github.bozkow.vibevisualizer.lyrics.SongMeta?>
            val oldShown = shown.value
            val old = LyricsResult(Lyr.L, Lyr.lsync, Lyr.label, Lyr.credit)
            val bools = listOf("xFocus", "xRefCred", "aRestCover", "aLyrCover", "aMotionCover").associateWith { S.b(it) }
            val strings = listOf("credPos", "view", "romMode", "uiHide").associateWith { S.str(it) }
            restore = {
                bools.forEach { (k, v) -> S.set(k, v) }; strings.forEach { (k, v) -> S.set(k, v) }
                E.notes.remove(fixtureNote); shown.value = oldShown
                E.src = src; E.title = title; E.artist = artist; E.clockOverride = clock
                io.github.bozkow.vibevisualizer.engine.Visuals.setCover(cover, quiet = true)
                if (old.lines.isEmpty()) Lyr.clear("") else apply(old)
                E.poke()
            }
        }
        if (mode == "controlsHidden") { S.set("uiHide", "idle"); E.uiHidden = true; return }
        if (mode == "controlsShown") { E.poke(); return }
        if (mode == "empty") { Lyr.clear("No lyrics for this song", kind = "none"); E.poke(); return }
        if (mode == "notice") { E.snack(fixtureNote); E.poke(); return }
        if (mode.startsWith("next")) {
            val state = io.github.bozkow.vibevisualizer.engine.Preload::class.java.getDeclaredField("shown\$delegate").apply { isAccessible = true }
                .get(io.github.bozkow.vibevisualizer.engine.Preload) as androidx.compose.runtime.MutableState<io.github.bozkow.vibevisualizer.lyrics.SongMeta?>
            state.value = io.github.bozkow.vibevisualizer.lyrics.SongMeta(title = "Next fixture song", artist = "Fixture artist", cover =
                io.github.bozkow.vibevisualizer.engine.Images.retainCover("fixture-next", testCover(mode == "nextA")))
            E.poke(); return
        }
        val travel = mode.startsWith("travel") || mode.startsWith("swap")
        if (travel) {
            Log.i("VV-motion", "mode=$mode low=${S.low} calm=${S.calm} anim=${S.str("anim")} system=${android.provider.Settings.Global.getFloat(io.github.bozkow.vibevisualizer.core.Vibe.app.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f)}")
            if (mode != "travelstart") {
                E.clockOverride = if (mode == "travelout") 6.0 else 15.6
                if (mode.startsWith("swap")) {
                    E.title = if (mode == "swapA") "First preview song" else "Second preview song"
                    E.artist = if (mode == "swapA") "First preview artist" else "Second preview artist"
                    E.trackVer++
                    io.github.bozkow.vibevisualizer.engine.Visuals.setCover(testCover(mode == "swapA"))
                }
                E.poke(); return
            }
            S.set("aRestCover", true); S.set("aLyrCover", true); S.set("aMotionCover", false)
            io.github.bozkow.vibevisualizer.engine.Visuals.setCover(testCover(true))
        }
        E.src = "file"; E.title = "Credits preview"; E.artist = "Test fixture"; E.settingsPage = null
        S.set("xFocus", mode.startsWith("focus") || travel); S.set("xRefCred", true); S.set("credPos", "lyrics"); S.set("view", "lyrics")
        val roman = mode.startsWith("rom")
        if (roman) S.set("romMode", if (mode == "rombelow") "below" else if (mode == "romoff") "off" else "replace")
        val static = mode == "static"
        val original = "君の名前を呼んでいた"
        val reading = "Kimi no namae wo yonde ita"
        val lines = if (mode.startsWith("pipbg")) {
            val backing = "We keep on singing through the night together until the morning brings us home again".split(" ")
            val words = backing.mapIndexed { n, word -> Word(3.0 + n, 4.0 + n, word, true) }
            listOf(Line(0.0, 26.0, "Stay here with me", listOf(Word(0.0, 26.0, "Stay here with me", false)),
                listOf(Bg(backing.joinToString(" "), null, words))))
        } else if (mode.startsWith("duet")) listOf(Line(6.0, 8.0, "Upcoming singer", opp = mode == "duetright"))
            else if (travel) listOf(Line(0.0, 2.0, "Before the break"), Line(16.0, 18.0, "After the break"))
            else if (roman) listOf(Line(0.0, 6.0, original, listOf(Word(0.0, 6.0, original, false, reading)), apiRom = reading))
            else if (static) (1..20).map { Line(null, null, "Fixture lyric line $it") }
            else (0..2).map { Line(it * 2.0, it * 2.0 + 1.5, "Fixture lyric line ${it + 1}") }
        E.clockOverride = if (mode.startsWith("pipbg")) 11.0 else if (mode.startsWith("duet")) 2.0 else if (mode.endsWith("end")) 7.0 else 1.0
        apply(LyricsResult(lines, !static, "Spicy Lyrics", Credit("spicy_lyrics", writers = listOf("Fixture Writer"),
            maker = Person("fixture-maker", null, null, null), uploader = Person("fixture-uploader", null, null, null))))
        E.poke()
        Handler(Looper.getMainLooper()).postDelayed({
            if (roman) {
                val ls = Lyr.view.javaClass.getDeclaredField("lv").apply { isAccessible = true }.get(Lyr.view) as List<*>
                val rs = ls.first()!!.javaClass.getDeclaredField("rows").apply { isAccessible = true }.get(ls.first()) as List<*>
                rs.forEach { r -> val row = r!!; val type = row.javaClass
                    val fit = type.getDeclaredField("fit").apply { isAccessible = true }.getFloat(row)
                    val widths = type.getDeclaredField("rowsW").apply { isAccessible = true }.get(row) as FloatArray
                    Log.i("VV-romwrap", "$mode fit=$fit rows=${widths.size}")
                }
            }
            Log.i("VV-credits", "$mode: all=${Lyr.view.allsung} y=${Lyr.view.creditsY} h=${Lyr.view.H} visible=${Lyr.view.creditsShown()}")
        }, 1600)
    }
    private fun testCover(first: Boolean): android.graphics.Bitmap = android.graphics.Bitmap.createBitmap(320, 320, android.graphics.Bitmap.Config.ARGB_8888).apply {
        eraseColor(if (first) 0xFF234C90.toInt() else 0xFF963865.toInt())
        val canvas = android.graphics.Canvas(this)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
        canvas.drawCircle(if (first) 100f else 220f, 100f, 55f, paint)
        canvas.drawRect(45f, 225f, 275f, 265f, paint)
    }

}
