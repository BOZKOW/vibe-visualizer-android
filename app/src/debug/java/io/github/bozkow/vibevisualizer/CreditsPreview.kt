package io.github.bozkow.vibevisualizer

import android.os.Handler
import android.os.Looper
import android.util.Log
import io.github.bozkow.vibevisualizer.core.S
import io.github.bozkow.vibevisualizer.engine.E
import io.github.bozkow.vibevisualizer.engine.Lyr
import io.github.bozkow.vibevisualizer.lyrics.Credit
import io.github.bozkow.vibevisualizer.lyrics.Line
import io.github.bozkow.vibevisualizer.lyrics.LyricsResult
import io.github.bozkow.vibevisualizer.lyrics.Person
import io.github.bozkow.vibevisualizer.lyrics.Word

/** Silent, uncached UI fixture. Restores source, lyrics, clock and settings with --es creditsPreview off. */
object CreditsPreview {
    private var restore: (() -> Unit)? = null
    private fun apply(r: LyricsResult) = Lyr::class.java.getDeclaredMethod("apply", LyricsResult::class.java)
        .apply { isAccessible = true }.invoke(Lyr, r)
    fun show(mode: String) {
        if (mode == "off") { restore?.invoke(); restore = null; return }
        if (restore == null) {
            val src = E.src; val title = E.title; val artist = E.artist; val clock = E.clockOverride
            val old = LyricsResult(Lyr.L, Lyr.lsync, Lyr.label, Lyr.credit)
            val bools = listOf("xFocus", "xRefCred", "aRestCover").associateWith { S.b(it) }
            val strings = listOf("credPos", "view", "romMode").associateWith { S.str(it) }
            restore = {
                bools.forEach { (k, v) -> S.set(k, v) }; strings.forEach { (k, v) -> S.set(k, v) }
                E.src = src; E.title = title; E.artist = artist; E.clockOverride = clock
                if (old.lines.isEmpty()) Lyr.clear("") else apply(old)
            }
        }
        E.src = "file"; E.title = "Credits preview"; E.artist = "Test fixture"; E.settingsPage = null
        S.set("xFocus", mode.startsWith("focus")); S.set("xRefCred", true); S.set("credPos", "lyrics"); S.set("view", "lyrics")
        val roman = mode.startsWith("rom")
        if (roman) S.set("romMode", if (mode == "rombelow") "below" else if (mode == "romoff") "off" else "replace")
        val static = mode == "static"
        val original = "君の名前を呼んでいた"
        val reading = "Kimi no namae wo yonde ita"
        val lines = if (roman) listOf(Line(0.0, 6.0, original, listOf(Word(0.0, 6.0, original, false, reading)), apiRom = reading))
            else if (static) (1..20).map { Line(null, null, "Fixture lyric line $it") }
            else (0..2).map { Line(it * 2.0, it * 2.0 + 1.5, "Fixture lyric line ${it + 1}") }
        E.clockOverride = if (mode.endsWith("end")) 7.0 else 1.0
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
}
