package io.github.bozkow.vibevisualizer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import io.github.bozkow.vibevisualizer.core.Defaults
import io.github.bozkow.vibevisualizer.core.S
import io.github.bozkow.vibevisualizer.engine.E

/** Debug builds only (app/src/debug): a test on the emulator sets any setting and opens any Settings page or pop-up
 *  directly, as Settings would, without finding them on screen:
 *  adb shell am broadcast -n io.github.bozkow.vibevisualizer/.DebugReceiver --es set lstyle --es to focus
 *  … --es page lyrics (or close) · … --es sheet timing (or close) · … --es year 2024 · … --es dj 1 (Spotify's DJ starts talking) */
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
            if (page != null) { if (page == "close") E.settingsPage = null else E.openSettings(page) }
            if (sheet != null) E.sheet = if (sheet == "close") null else sheet
        }
    }
}
