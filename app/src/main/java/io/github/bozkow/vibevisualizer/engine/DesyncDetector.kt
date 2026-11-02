package io.github.bozkow.vibevisualizer.engine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.bozkow.vibevisualizer.audio.Audio
import io.github.bozkow.vibevisualizer.core.S
import io.github.bozkow.vibevisualizer.core.Vibe
import io.github.bozkow.vibevisualizer.core.now
import io.github.bozkow.vibevisualizer.lyrics.Line
import io.github.bozkow.vibevisualizer.lyrics.lineEnd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sqrt

/* Desync detection: listens to the music and checks that the singing lines up with the lyrics. Singing starts show up
   as sudden rises in the voice range (300-3400 Hz); they're compared with where the lyrics expect words to start (or
   lines, without word timing), trying every shift from -3 to +3 s. Only a clear, repeated answer counts. Needs real
   sound (phone sound, Vibe Bridge, the microphone, a file); Spotify's ambient bars carry none. */
object Desync {
    private val prev = FloatArray(1024)
    private var ft = 0.0; private var acc = 0f; private var lv = 0f; private var ln = 0; private var bt = 0.0
    private val T = ArrayList<Double>(); private val Fl = ArrayList<Float>(); private val V = ArrayList<Float>()
    private var lastT: Double? = null; private var off = 0
    private val est = ArrayList<Double>()
    private var fixes = 0; private var dismissed = 0; private var asked = false; private var ign = false; private var next = 0.0; private var lastFix = 0.0
    var stat by mutableStateOf(""); private set

    private fun st(m: String) { if (m != stat) stat = m }
    private var busy = false; private var gen = 0   // the check under way, and what it was asked about
    fun reset(all: Boolean = false) { gen++; T.clear(); Fl.clear(); V.clear(); lastT = null; est.clear(); if (all) { lastFix = 0.0; fixes = 0; asked = false } }
    fun song() { reset(); lastFix = 0.0; fixes = 0; dismissed = 0; asked = false; ign = false; Lyr.lAdj = 0.0 }

    fun tick(t: Double) {
        if (S.str("lDesync") == "off") { st(""); return }
        if (!Lyr.lsync || Lyr.L.isEmpty() || ign || !E.playing()) { lastT = null; st(if (ign) "Ignored for this song." else ""); return }
        if (!Audio.realAudio) { lastT = null; st("Waiting for real sound (phone sound, Vibe Bridge, the microphone or a file)."); return }
        val pos = Lyr.lyrT()
        if ((lastT != null && abs(pos - lastT!!) > .35) || off != S.loff) { reset(); off = S.loff }   // a seek, a jump or a new offset: start over
        lastT = pos
        if (t - ft < 15) return; ft = t   // rises measured at most ~60 times a second, so they're the same size at any frame rate
        val bw = (Audio.sampleRate() / 2.0) / 1024; val a = (300 / bw).roundToInt(); val b = (3400 / bw).roundToInt()
        var fl = 0f; var l = 0f; val data = Audio.data
        for (k in a until min(b, 1024)) { val d = data[k] - prev[k]; if (d > 0) fl += d; l += data[k] }
        System.arraycopy(data, 0, prev, 0, 1024)
        if (fl > acc) acc = fl; lv += l; ln++
        if (t - bt < 50) return; bt = t   // one value every 50 ms: the strongest rise in it, and the average level
        T.add(pos); Fl.add(acc); V.add(lv / max(1, ln)); acc = 0f; lv = 0f; ln = 0
        while (T.isNotEmpty() && pos - T[0] > 60) { T.removeAt(0); Fl.removeAt(0); V.removeAt(0) }
        if (T.size < 400) st("Listening… (${(T.size / 20.0).roundToInt()} of 20 s)")
        else if (t > next) { next = t + 5000; analyze() }
    }

    /** The math runs off the main thread: about 290,000 steps, every 5 s, was a stutter in the lyrics each time. Only
     *  what it answers comes back here; a seek or a new song in the meantime ([gen]) drops the answer. */
    private fun analyze() {
        if (busy) return
        busy = true
        val g = gen; val ts = T.toDoubleArray(); val fl = Fl.toFloatArray(); val vs = V.toFloatArray(); val ls = Lyr.L; val lag = Audio.lag()
        Vibe.scope.launch {
            val r = try { withContext(Dispatchers.Default) { DesyncScore.score(ts, fl, vs, ls, lag) } } finally { busy = false }
            if (g == gen && !ign) take(r)
        }
    }

    private fun take(r: DesyncScore.Result) {
        when (r.kind) {
            DesyncScore.TOO_LITTLE -> { st("Listening… (too little singing so far)"); return }
            DesyncScore.NO_MATCH -> { est.clear(); st("Listening… (no clear match yet)"); return }
            DesyncScore.IN_SYNC -> { est.clear(); st("In sync ✓"); return }
        }
        est.add(r.off); if (est.size > 3) est.removeAt(0)
        if (est.size < 3) { st("Checking a possible desync…"); return }
        val lo = est.min(); val hi = est.max(); if (hi - lo > .1) { est.removeAt(0); return }   // three checks in a row (15 s) must agree
        found((lo + hi) / 2)
    }

    private fun found(e: Double) {   // e > 0: the singing comes e seconds after the lyrics' time, so the lyrics are ahead
        est.clear()
        if (lastFix != 0.0 && sign(e) == -sign(lastFix) && abs(e + lastFix) < max(.25, abs(lastFix) * .4)) {   // it wants the last fix undone: that fix was wrong
            Lyr.lAdj += lastFix; lastFix = 0.0; ign = true; reset(); Lyr.view.restAll()
            st("The last fix didn’t fit after all, so it was undone. This song is left as it is."); return
        }
        val txt = "about " + String.format(java.util.Locale.getDefault(), "%.1f", abs(e)) + " s " + (if (e > 0) "ahead of" else "behind") + " the singing"
        val fix = { Lyr.lAdj -= e; lastFix = e; fixes++; reset(); Lyr.view.restAll(); st("Fixed: moved " + (if (e > 0) "back " else "ahead ") + String.format(java.util.Locale.getDefault(), "%.1f", abs(e)) + " s.") }
        if (fixes >= 4) return   // something odd with this song: stop adjusting
        if (S.str("lDesync") == "auto" || fixes > 0) { fix(); E.toast("Lyrics moved " + String.format(java.util.Locale.getDefault(), "%.1f", abs(e)) + " s to match the singing", "ok"); return }   // once you've said yes, later fixes for the song just happen
        if (asked) return
        asked = true; st("Lyrics look $txt.")
        E.snack(E.Snack("Lyrics look " + String.format(java.util.Locale.getDefault(), "%.1f", abs(e)) + " s " + (if (e > 0) "ahead" else "behind"), "warn", E.Act("Fix") { fix() }, long = true,
            onDismiss = { dismissed++; if (dismissed >= 2) ign = true else { asked = false; next = now() + 60000 } }))
    }
}

/** Does the singing line up with the lyrics? The math of Desync, on what it collected: touches nothing but its arguments,
 *  so it can run off the main thread. [T] is where the lyrics were (s), [Fl] the rises in the voice range, [V] its level. */
internal object DesyncScore {
    const val TOO_LITTLE = 0; const val NO_MATCH = 1; const val IN_SYNC = 2; const val OFF = 3
    class Result(val kind: Int, val off: Double = 0.0)

    fun score(T: DoubleArray, Fl: FloatArray, V: FloatArray, L: List<Line>, lag: Double): Result {
        // two things have to line up: singing starts with word (or line) starts, and singing with the parts where the
        // lyrics say someone sings, so the pauses between lines count too (a steady rhythm alone can't fool it)
        val n = T.size; if (n == 0) return Result(TOO_LITTLE)
        val t0 = T[0]; val t1 = T[n - 1]; val on = ArrayList<Double>(); val sp = ArrayList<DoubleArray>()
        for ((i, l) in L.withIndex()) {
            if (l.dots || l.t == null) continue
            val ws = l.words
            if (!ws.isNullOrEmpty()) for (w in ws) { on.add(w.t); sp.add(doubleArrayOf(w.t, w.end ?: (w.t + .3))) }
            else { on.add(l.t!!); sp.add(doubleArrayOf(l.t!!, lineEnd(l) ?: L.getOrNull(i + 1)?.t ?: (l.t!! + 4))) }
        }
        if (on.count { it > t0 + 3 && it < t1 - 3 } < 12) return Result(TOO_LITTLE)
        on.sort(); sp.sortBy { it[0] }
        fun near(x: Double): Double {
            var lo = 0; var hi = on.size; while (lo < hi) { val m = (lo + hi) ushr 1; if (on[m] < x) lo = m + 1 else hi = m }
            return if ((lo < on.size && on[lo] - x < .06) || (lo > 0 && x - on[lo - 1] < .06)) 1.0 else 0.0
        }
        fun inside(x: Double): Double {
            var lo = 0; var hi = sp.size; while (lo < hi) { val m = (lo + hi) ushr 1; if (sp[m][0] <= x) lo = m + 1 else hi = m }
            var k = lo - 1; while (k >= 0 && k >= lo - 4) { if (x < sp[k][1]) return 1.0; k-- }
            return 0.0
        }
        // the level, without slow loudness changes (a louder chorus isn't singing starting)
        val Vd = DoubleArray(n) { i -> var s = 0.0; var c = 0; for (k in max(0, i - 30)..min(n - 1, i + 30)) { s += V[k]; c++ }; V[i] - s / c }
        val Fd = DoubleArray(n) { Fl[it].toDouble() }
        fun pear(X: DoubleArray, f: (Double) -> Double, e: Double): Double {
            var sx = 0.0; var sy = 0.0; var sxx = 0.0; var syy = 0.0; var sxy = 0.0; val c = n.toDouble()
            for (i in 0 until n) { val x = X[i]; val y = f(T[i] - e); sx += x; sy += y; sxx += x * x; syy += y * y; sxy += x * y }
            val vx = sxx - sx * sx / c; val vy = syy - sy * sy / c
            return if (vx > 0 && vy > 0) (sxy - sx * sy / c) / sqrt(vx * vy) else 0.0
        }
        val lags = DoubleArray(121) { (it - 60) * .05 }; val sc = DoubleArray(121) { k -> pear(Fd, ::near, lags[k]) + pear(Vd, ::inside, lags[k]) }
        var bi = 0; for (k in 1 until sc.size) if (sc[k] > sc[bi]) bi = k
        val best = sc[bi]; val e = lags[bi]; var second = -1.0
        for (k in sc.indices) if (abs(lags[k] - e) > .2 && sc[k] > second) second = sc[k]
        if (best < .12 || best - second < .02) return Result(NO_MATCH)
        // the sound reaches the app a little after it plays: that delay isn't a desync. Only a clear, real offset counts:
        // 0.4 s+ with word timing, 1 s+ with line timing, and only when it fits clearly better than the lyrics as they are
        val z = (60 + (lag / .05).roundToInt()).coerceIn(0, sc.size - 1); val ec = e - lag
        val words = L.any { !it.words.isNullOrEmpty() }; val mn = if (words) .4 else 1.0
        if (abs(ec) < mn || best - sc[z] < .15 || best - second < .05) return Result(IN_SYNC)
        return Result(OFF, ec)
    }
}
