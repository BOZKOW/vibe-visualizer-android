package io.github.bozkow.vibevisualizer.engine

import android.os.PowerManager
import io.github.bozkow.vibevisualizer.core.S
import io.github.bozkow.vibevisualizer.core.Vibe
import io.github.bozkow.vibevisualizer.core.now

/** Offers Low performance mode for slow rendering or low battery. */
object Perf {
    private var prev = 0.0; private var t0 = 0.0; private var n = 0; private var slow = 0; private var secs = 0; private var bad = 0; private var sum = 0.0
    private var snooze = 0.0; private var asked = false

    fun tick(t: Double) {
        if (t - battAt > 60000 && E.visible) { battAt = t; E.batteryCheck() }   // low battery: once a minute is plenty
        val gap = t - prev; prev = t
        if (S.low || !S.b("perfAsk") || asked || !E.visible || !E.playing() || t < snooze || gap > 1000) { t0 = t; n = 0; slow = 0; secs = 0; bad = 0; sum = 0.0; return }   // only judged while a song plays on screen
        n++; if (gap > 50) slow++   // a frame that took 3+ refreshes at 60 Hz
        if (t - t0 < 1000) return
        val fps = n * 1000 / (t - t0); t0 = t
        secs++; sum += fps; if (fps < 30 || slow > n * .25) bad++
        n = 0; slow = 0
        if (secs >= 12) { if (bad >= 9) suggest("slow", sum / secs); secs = 0; bad = 0; sum = 0.0 }   // most of the last 12 seconds
    }

    private var battAt = 0.0

    /* ---- Power saving: Low performance mode follows it ---- */
    private val pp by lazy { Vibe.app.getSharedPreferences("perf", android.content.Context.MODE_PRIVATE) }

    /** The phone's Power saving came on or went off (or the app starts, and compares with how it was): on, Low
     *  performance mode turns on too; off, it turns off again if it came on that way. Only the change counts, so
     *  turning Low performance mode off by hand while Power saving is on keeps it off. */
    fun saverCheck(force: Boolean = false) {
        val on = runCatching { Vibe.app.getSystemService(PowerManager::class.java)?.isPowerSaveMode }.getOrNull() ?: return
        val was = pp.getBoolean("saver", false)
        pp.edit().putBoolean("saver", on).apply()
        if (!S.b("aSaverLow") || (on == was && !force)) return
        if (on && !S.low) {
            S.set("low", true); pp.edit().putBoolean("auto", true).apply()
            E.toast("Power saving is on, so Low performance mode is too", "ok")
        } else if (!on && pp.getBoolean("auto", false)) {
            pp.edit().putBoolean("auto", false).apply()
            if (S.low) { S.set("low", false); E.toast("Power saving is off, so Low performance mode is off again", "ok") }
        }
    }

    /** Low performance mode changed by hand: it isn't Power saving's to turn off any more. */
    fun lowByHand() { pp.edit().putBoolean("auto", false).apply() }

    fun suggest(why: String, v: Double, force: Boolean = false) {
        if (asked || S.low || !S.b("perfAsk") || (!force && now() < snooze)) return
        asked = true
        val m = when (why) {
            "battery" -> "Battery at ${Math.round(v * 100)}% · Low performance mode uses less"
            "saver" -> "Battery saver is on · Low performance mode uses less"
            else -> "Running slowly (${Math.round(v)} fps) · Low performance mode helps"
        }
        // swiping it away asks again in 20 minutes at the earliest; Settings → Performance turns the question off
        E.snack(E.Snack(m, "warn", E.Act("Turn on") { S.set("low", true) }, long = true, onDismiss = { snooze = now() + 20 * 60000; asked = false }))
    }
}
