package io.github.bozkow.vibevisualizer.lyrics

import androidx.compose.ui.graphics.drawscope.clipRect
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.bozkow.vibevisualizer.core.S
import io.github.bozkow.vibevisualizer.core.Vibe
import io.github.bozkow.vibevisualizer.core.now
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/** The lyrics on screen: laid out like the web app's DOM (lines, words, letters, background vocals, romanization and
 *  translation under them), with its classes (active, sung, distance, Focus mode) as states that glide like its CSS
 *  transitions, and the Spicy Lyrics word animation. Each line is drawn into its own GPU layer, so lines that don't
 *  change cost nothing but moving and blurring that layer. */
private val SPACES = Regex("\\s+")   // (kept: a new one for every lyric line added up)
private val OPEN_BRACKET = Regex("^\\s*[(\\[]")
private val CLOSE_BRACKET = Regex("[)\\]]\\s*$")
private val BRACKETED = Regex("^\\s*[(\\[][^()\\[\\]]*[)\\]]\\s*$")   // all of it in one pair of brackets ("(a) and (b)" isn't)

/** Background vocals' words as shown, with Background vocals in brackets [br] on or off: on, as they are (in brackets if
 *  they have none); off, without the one pair they may come in. */
internal fun bgBrackets(t: String, br: Boolean): String = when {
    br && !OPEN_BRACKET.containsMatchIn(t) -> "($t)"
    !br && BRACKETED.matches(t) -> t.replaceFirst(OPEN_BRACKET, "").replaceFirst(CLOSE_BRACKET, "").trim()
    else -> t
}

/** With the Focus style and Cover in long breaks: the cover takes a break's place from this long (s). Every other break
 *  has its dots, however long. */
const val COVER_BREAK = 5.0

class LyricsRenderer {
    /* ================= model ================= */
    inner class Lt(val ch: String, val ts: TS) { var dx = 0f; var w = 0f; var sp: Springs? = null }
    inner class Pc(val text: String, val ts: TS) {
        var x = 0f; var y = 0f; var w = 0f; var adv = 0f
        var letters: List<Lt>? = null
        var sp: Springs? = null
        var grad = -20.0
        var gR = 0.0; var gO = 0.0                    // glow radius (px at 40 px font) and opacity (%)
        var scale = Double.NaN; var yo = 0.0          // NaN: the resting pose from the CSS
        var slm = -50.0                               // refreshed simple mode: the soft edge
    }
    inner class Row(val kind: Int, val fs: Float, val lh: Float, val mt: Float, val weight: Int) {
        var pieces: List<Pc>? = null
        var text: String? = null
        var oneTs: TS? = null
        var layout: StaticLayout? = null
        var paint: TextPaint? = null
        var y = 0f; var h = 0f
        var fit = 1f   // smaller than its size when one word is wider than the row
        var spans: List<IntRange>? = null   // the pieces as whole words (wordSpans)
        var rowsW = FloatArray(0)
        val timed get() = pieces != null || oneTs != null
    }
    inner class LV(val i: Int, val l: Line) {
        val dots = l.dots
        /** A break the cover takes the place of (the Focus style's): no dots. */
        val longBreak get() = l.dots && S.xFocus && S.b("aRestCover") && (l.end ?: 0.0) - (l.t ?: 0.0) >= COVER_BREAK
        val opp = l.opp
        val rows = ArrayList<Row>()
        var mainTs: List<TS>? = null
        var top = 0f; var h = 0f; var hText = 0f; var bgH = 0f
        var a = false; var p = false; var dp = false; var sa = false; var dist = 0
        var fv = false; var fn = false; var fs = false; var fu = false
        val opacity = Tween(.51f); val blur = Tween(0f); val fy = Tween(0f); val fx = Tween(0f)
        val bgOpen = Tween(1f); val dotScale = Tween(0f); val dotOp = Tween(0f)
        val dotOff = Tween(0f)   // a break's dots: how far the lines after them still have to slide, their room having come or gone (tops)
        val pin = Tween(0f)      // held at the top while it's still sung and the lines after it scroll on (place: pinned)
        var activeAt = -1e9; var tapAt = -1e9
        var lineGlow: Spring? = null; var lineGrad = -20.0; var lineGlowV = 0.0
        val dotSp = if (l.dots) Array(3) { DotSprings() } else null
        val dotV = DoubleArray(12)
        var sweep: Sweep? = null
        var layer: GraphicsLayer? = null   // the line as seen: its blur and shadow, where it is
        var inner: GraphicsLayer? = null   // its picture (and how faint), so the blurred ends can draw it without that blur (drawPlain)
        var px = 0f; var py = 0f; var shown = false
        var dirty = true
        var recW = 0; var recH = 0
        var key = ""
        val rtl = RTL.containsMatchIn(l.text)
    }
    class Sweep(val xs: FloatArray, val ws: FloatArray, val lx: Array<FloatArray?>, val lw: Array<FloatArray?>, val fade: Float, val edges: DoubleArray)

    private var lines: List<Line> = emptyList()
    var sync = false; private set
    private var lv: List<LV> = emptyList()
    private var duet = false
    val tick = mutableLongStateOf(0L)
    var version by mutableIntStateOf(0); private set

    /* ================= metrics ================= */
    var W = 0f; private set
    var H = 0f; private set
    var drawnY = 0   // where the scroll put the lines on the last draw (px, whole), for the blurred ends to hold still among their pixels
    var bleedTop = 0f; var bleedBot = 0f; var bleedLeft = 0f   // how far outside the area they're drawn (LyricsPane's behind)
    private var areaH = 0f
    private var screenH = 0f; private var screenW = 0f
    private val FLING_FRICTION = 4.2f
    private var dens = 1f
    var F = 0f; private set
    private var padL = 0f; private var padR = 0f
    private var built = false; private var laidOut = false

    /* ================= state (the web app's lyricsTick / setActive) ================= */
    var li = -2; private set
    private var lsi = -2; private var lfi = -2; private var lfo = false; private var lfn = ""; private var lfs = ""
    private var lfnSet = emptySet<Int>(); private var lfsSet = emptySet<Int>()
    private var lAct = emptySet<Int>(); private var lActKey = ""; private var lOut = false
    var allsung by mutableStateOf(false); private set
    private val moving = HashSet<Int>()
    private var lastWT = 0.0

    // scrolling
    private val lyY = Tween(0f)
    private val lyG = Glide(0f); private var gliding = false   // where the lyrics sit while they glide instead (setLyY)
    var lyFree by mutableStateOf(false); private set
    /** A long break: nothing sung for COVER_BREAK or more around now, and the next line not due yet (the cover shows). */
    var rest by mutableStateOf(false); private set
    private var restAt = 0.0
    var covered = false   // the break's cover is fully over the lyrics on screen (set by the Lyrics screen)
    var lineAtTop: () -> Boolean = { false }   // the line being sung at the top, not the middle (the small window's setting)
    var softScroll: () -> Boolean = { false }  // on its side: the scroll glides, on the words' timing (scrollSecs)
    var quiet: () -> Boolean = { false }       // under Settings' blur: the lines' own blur and shadow rest (E.quiet)
    var pipScroll: () -> Boolean = { false }   // the small window: the scroll stays as it was up to 0.41.5, the owner's wish (pipSungRow, pipTallLineTop)
    private var stepMs = 550.0                 // how long the glide to where the lyrics are placed next takes
    private var lyFreeY = 0f; private var lyFreeAt = 0.0
    var dragged = false
    private var lyTall = false; private var lyTallAt = 0.0
    private val bandT = Tween(-1f); private val bandB = Tween(-1f)
    private var placeUntil = 0.0
    // untimed lyrics scroll slowly with the song
    private var stPos: Float? = null; private var stVel = 0f; private var stUser = 0.0; var stScroll = 0f; private set
    private var stLast = 0.0
    // new lyrics rise in; old ones fade out
    private var enterAt = -1e9; private var enterFrom = 0
    var leaving = false; private set
    private val leaveA = Tween(1f)
    var pressed = -1
    var creditsY by mutableStateOf(0f); private set    // where credits under the lyrics go (screen y inside the view)
    /** The credits under the last line are on screen (clear of the fade at the view's foot). */
    fun creditsShown() = H > 0 && creditsY < H - F * 2

    val isStatic get() = !sync && lines.isNotEmpty()
    val empty get() = lines.isEmpty()

    /* ================= paints ================= */
    private val tp = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
    private val tfCache = HashMap<String, Typeface>()
    private fun face(weight: Int): Typeface {
        val f = S.str("lfont"); val k = "$f$weight"
        return tfCache.getOrPut(k) {
            if (f == "system") Typeface.create(Typeface.DEFAULT, weight, false)
            else runCatching { Typeface.Builder(Vibe.app.assets, "fonts/inter.ttf").setFontVariationSettings(if (f == "spicy") "'wght' $weight, 'opsz' 32" else "'wght' $weight").build() }
                .getOrDefault(Typeface.create(Typeface.DEFAULT, weight, false))
        }
    }
    private fun paint(fs: Float, weight: Int): TextPaint { tp.typeface = face(weight); tp.textSize = fs; tp.shader = null; tp.clearShadowLayer(); return tp }

    /* ================= building (renderLyrics) ================= */

    fun set(ls: List<Line>, isSync: Boolean, quiet: Boolean) {
        linePos = -1.0
        release()
        lines = ls; sync = isSync
        duet = ls.any { it.opp }
        li = -2; lsi = -2; lfi = -2; lfo = false; lfn = ""; lfs = ""; lfnSet = emptySet(); lfsSet = emptySet(); lAct = emptySet(); lActKey = ""; lOut = false
        moving.clear(); lyTall = false; bandT.snap(-1f); bandB.snap(-1f); stPos = null
        if (!quiet) { stScroll = 0f; lyFree = false }
        lv = ls.mapIndexed { i, l -> LV(i, l) }
        built = false; laidOut = false
        leaving = false; leaveA.snap(1f)
        version++
        if (S.xFocus && sync && W > 0f && lines.isNotEmpty()) frame(now(), posNow, true)   // (placed before they're drawn: see setActive)
        bump()
    }

    fun restyle() { lv.forEach { it.rows.clear() }; built = false; laidOut = false; tfCache.clear(); bump() }
    fun relayout() { laidOut = false; lv.forEach { it.dirty = true }; li = -2; lActKey = "x"; bump() }

    fun leave() { if (lines.isNotEmpty() && !S.low) { leaving = true; leaveA.set(0f, now(), 350.0, Ease.ease) } }

    fun enter() { if (!S.low && !S.calm) { enterAt = now() + 16; enterFrom = max(0, li) } }   // (Motion sickness: no drift in)

    private fun release() { lv.forEach { d -> d.layer?.let { l -> releaser?.invoke(l) }; d.inner?.let { l -> releaser?.invoke(l) }; d.layer = null; d.inner = null } }
    var releaser: ((GraphicsLayer) -> Unit)? = null

    private fun build() {
        val romMode = S.romMode; val jm = S.joinMode; val join = jm != "off"; val br = S.b("lbgBr")
        for (d in lv) {
            d.rows.clear(); d.mainTs = null; d.sweep = null
            val l = d.l
            if (l.dots) continue
            val idx = d.i
            val main = Row(0, 1f, 1.18f, 0f, 700)
            val romOnly = romMode == "replace" && l.rom != null
            val ws = l.words
            if (ws != null && ws.isNotEmpty() && sync) {
                val nt = lines.getOrNull(idx + 1)?.t ?: Double.POSITIVE_INFINITY
                val ts = ws.mapIndexed { k, w -> TS(w.t, w.end ?: if (k + 1 < ws.size) ws[k + 1].t else min(w.t + 1.5, nt)) }
                val rw = if (romOnly) l.rom!!.split(SPACES) else null
                when {
                    !romOnly -> { val (jw, jt) = if (join) joinWords(ws, ts, jm) else ws to ts; main.pieces = jw.mapIndexed { k, w -> Pc(w.text + if (w.sp) " " else "", jt[k]) }; d.mainTs = jt }
                    l.romSyl && ws.any { it.rom != null } -> { val (jw, jt) = if (join) joinWords(ws, ts, jm) else ws to ts; main.pieces = jw.mapIndexed { k, w -> Pc((w.rom ?: w.text) + if (w.sp || w.romSp) " " else "", jt[k]) }; d.mainTs = jt }   // per-syllable romanization
                    rw != null && rw.size == ws.size -> { main.pieces = rw.mapIndexed { k, x -> Pc(x + if (k < rw.size - 1) " " else "", ts[k]) }; d.mainTs = ts }   // romanized words line up 1:1
                    else -> { main.text = l.rom; main.oneTs = TS(ws[0].t, min(ws.last().t + .8, nt)); d.mainTs = listOf(main.oneTs!!) }   // fill the whole romanized line
                }
                main.pieces?.let { ps -> splitLetters(ps) }
            } else main.text = if (romOnly) l.rom else l.text
            d.rows.add(main)
            l.bg?.forEach { b ->   // background vocals: their own smaller line, sung word by word on their own timing
                val bwds = b.words
                // romanized in place as its line is, or on its own when the line is in Latin letters (it stayed in its script
                // then: only a romanized line let its background vocals be)
                val romOn = romMode == "replace" && (l.rom != null || !nonLatin(l.text))
                val romW = romOn && bwds?.any { it.rom != null } == true
                val romB = romOn && b.rom != null && !romW
                val r = Row(1, .75f, 1.18f, .1f, 600)
                // the brackets go by Background vocals in brackets: on, as the lyrics have them (added where they have
                // none); off, none, even where the lyrics came with them (a TTML file's "(…)": they showed whatever the setting)
                val enclosed = BRACKETED.matches(b.text)
                if (sync && bwds != null && bwds.isNotEmpty() && !romB) {
                    val bts = bwds.mapIndexed { k, w -> TS(w.t, w.end ?: if (k + 1 < bwds.size) bwds[k + 1].t else w.t + 1) }
                    val brk = br && !OPEN_BRACKET.containsMatchIn(b.text); val strip = !br && enclosed; val n = bwds.size
                    r.pieces = bwds.mapIndexed { k, w ->
                        var t = if (romW && w.rom != null) w.rom!! else w.text
                        if (brk && k == 0) t = "($t"; if (brk && k == n - 1) t = t.trimEnd() + ")"
                        if (strip && k == 0) t = t.replaceFirst(OPEN_BRACKET, "")
                        if (strip && k == n - 1) t = t.replaceFirst(CLOSE_BRACKET, "")
                        Pc(t + if ((w.sp || (romW && w.romSp)) && t.isNotBlank()) " " else "", bts[k])   // (a bracket that was a part of its own leaves nothing)
                    }
                    splitLetters(r.pieces!!)
                } else {
                    val t = if (romB) b.rom!! else b.text
                    r.text = bgBrackets(t, br)
                }
                d.rows.add(r)
                // Romanization below: its own romanization right under it, word by word with it where it has words (kind 4:
                // small, tucked away and out with it); the line's romanization under the line never had them
                if (romMode == "below" && nonLatin(b.text) && (b.rom != null || bwds?.any { it.rom != null } == true)) {
                    val rRow = Row(4, .4f, 1.25f, .05f, 700)
                    if (sync && bwds != null && bwds.isNotEmpty() && bwds.any { it.rom != null }) {
                        val bts = bwds.mapIndexed { k, w -> TS(w.t, w.end ?: if (k + 1 < bwds.size) bwds[k + 1].t else w.t + 1) }
                        rRow.pieces = bwds.mapIndexed { k, w -> val t = w.rom ?: w.text; Pc(t + if ((w.sp || w.romSp) && t.isNotBlank()) " " else "", bts[k]) }
                    } else {
                        rRow.text = b.rom
                        val lt = l.t
                        if (sync && lt != null) rRow.oneTs = TS(lt, max(lt + .3, lineEnd(l) ?: lt + 2))
                    }
                    d.rows.add(rRow)
                }
                // its translation right under it, smaller and dimmer, tucked away and out with it (kind 4)
                b.tr?.takeIf { S.tr }?.let { tr ->
                    val tRow = Row(4, .4f, 1.18f, .05f, 900)
                    tRow.text = bgBrackets(tr, br)
                    if (sync) {
                        val bts = bwds?.takeIf { it.isNotEmpty() }?.let { ws -> TS(ws.first().t, ws.last().let { it.end ?: it.t + 1 }) }
                        val lt = l.t
                        tRow.oneTs = bts ?: lt?.let { TS(it, max(it + .3, lineEnd(l) ?: it + 2)) }
                    }
                    d.rows.add(tRow)
                }
            }
            // romanization and translation under the line fill in time with it: word by word when the romanized words
            // line up with the sung ones, otherwise across the whole line
            val lEnd = d.mainTs?.lastOrNull()?.end ?: lines.getOrNull(idx + 1)?.t ?: ((l.t ?: 0.0) + 4)
            fun timed(kind: Int, text: String, words: List<Pair<String, Boolean>>?) {
                // a translation as Spicy Lyric Translator draws one in Spicy Lyrics (0.46.2): extra bold, .45 of the line, a line
                // height of 1.18; the romanization as before
                val r = if (kind == 3) Row(kind, .45f, 1.18f, .22f, 900) else Row(kind, .46f, 1.3f, .22f, 700)
                val lw = l.words
                if (!sync || l.t == null) r.text = text
                else if (words != null && lw != null && words.size == lw.size) {
                    r.pieces = words.mapIndexed { k, (x, sp) -> val w = lw[k]; Pc(x + if (sp) " " else "", TS(w.t, w.end ?: if (k + 1 < lw.size) lw[k + 1].t else min(w.t + 1.5, lEnd))) }
                } else { r.text = text; r.oneTs = TS(l.t!!, max(l.t!! + .3, lEnd)) }
                d.rows.add(r)
            }
            val rom = l.rom
            if (rom != null && romMode == "below") {
                val lw = l.words
                timed(2, rom, if (l.romSyl && lw != null) lw.map { (it.rom ?: it.text) to (it.sp || it.romSp) } else rom.split(SPACES).filter { it.isNotEmpty() }.let { p -> p.mapIndexed { k, x -> x to (k < p.size - 1) } })
            }
            l.tr?.let { timed(3, it, null) }
        }
        built = true
    }

    /** Emphasize: a held word becomes a letter group; the letters share its time evenly and finish 250 ms early. */
    private fun splitLetters(ps: List<Pc>) {
        for (pc in ps) {
            val tm = pc.ts; val core = pc.text.trimEnd()
            val lets = graphemes(core)
            if (tm.end - tm.t < SL.MinLetterCapableDuration || lets.isEmpty() || RTL.containsMatchIn(core)) continue
            val end0 = tm.end
            tm.end = max(tm.t + .05, tm.end - SL.EmphasisEndTrim)
            val per = (tm.end - tm.t) / lets.size
            // Keep syllable pace: a joined word's syllables at their own speed (unless they don't split into these letters)
            val paced = tm.syl?.takeIf { it.size > 1 && S.b("ljoinPace") }?.let { ps ->
                ps.mapIndexed { i, p -> graphemes(if (i == ps.lastIndex) p.first.trimEnd() else p.first).size }.takeIf { it.sum() == lets.size }?.let { sylLetterTimes(tm, it, end0) }
            }
            pc.letters = lets.mapIndexed { i, ch -> Lt(ch, paced?.get(i) ?: TS(tm.t + i * per, tm.t + (i + 1) * per)) }
        }
    }

    private fun graphemes(s: String): List<String> {
        val out = ArrayList<String>(); val it = java.text.BreakIterator.getCharacterInstance(); it.setText(s)
        var a = it.first(); var b = it.next()
        while (b != java.text.BreakIterator.DONE) { out.add(s.substring(a, b)); a = b; b = it.next() }
        return out
    }

    /* ================= layout ================= */

    fun size(w: Float, h: Float, area: Float, scrW: Float, scrH: Float, density: Float, font: Float, pl: Float, pr: Float) {
        if (w == W && h == H && font == F && area == areaH && pl == padL && pr == padR) return
        // a new height (a notice's room, the controls hiding): the lines are placed again on the next frame, as after a line
        // change (they kept the old place until the next line: with a notice, sometimes moved, sometimes not, 0.46.1)
        if (h != H || area != areaH) lActKey = "x"
        W = w; H = h; areaH = area; screenW = scrW; screenH = scrH; dens = density; F = font; padL = pl; padR = pr
        laidOut = false; lv.forEach { it.dirty = true }; shadowCache.clear()
        bump()
    }

    // Bloom's lines are spaced by the text size; Spring's too with Increased line spacing, a little tighter (Spicy Lyrics 6.5.0)
    private fun linePad() = if (S.low || S.xFocus) .2f else if (S.lsimple) .32f else if (S.b("lspace")) .27f else .2f

    private fun layoutAll() {
        if (W <= 0 || F <= 0) return
        if (!built) build()
        val avail = W - padL - padR
        for (d in lv) {
            if (d.dots) { d.hText = F * 1.18f + 2 * F * .2f; continue }
            // the Focus style's duets stay on their own side, so two voices read as two columns even when they overlap
            val maxW = avail - if (duet && S.xFocus) avail * .22f else if (duet && S.lduet) avail * .14f else 0f
            var y = 0f; var bg = 0f
            for (r in d.rows) {
                val ps = r.pieces
                // a word wider than the row (a long held "Weeeeeng") makes its line smaller, instead of running off the screen
                r.fit = 1f
                val spans = ps?.let { pp -> r.spans ?: wordSpans(pp.map { it.text }).also { r.spans = it } }
                if (ps != null && spans != null) {
                    val p0 = paint(F * r.fs, r.weight)
                    val widest = spans.maxOfOrNull { s -> var w = 0f; for (k in s) w += p0.measureText(if (k == s.last) ps[k].text.trimEnd() else ps[k].text); w } ?: 0f
                    if (widest > maxW) r.fit = max(.4f, maxW / widest * .98f)
                }
                val fs = F * r.fs * r.fit; val lh = fs * r.lh
                r.y = y + F * r.mt
                if (ps != null && spans != null) {
                    val p = paint(fs, r.weight)
                    var x = 0f; var row = 0; val rowW = ArrayList<Float>()
                    for (s in spans) {   // a word sung in syllables moves to the next row whole
                        var wordW = 0f
                        for (k in s) { val pc = ps[k]; pc.adv = p.measureText(pc.text); pc.w = p.measureText(pc.text.trimEnd()); wordW += if (k == s.last) pc.w else pc.adv }
                        if (x > 0 && x + wordW > maxW) { rowW.add(x); row++; x = 0f }
                        for (k in s) {
                            val pc = ps[k]
                            if (x > 0 && x + pc.w > maxW) { rowW.add(x); row++; x = 0f }   // only one wider than a row at its smallest
                            pc.x = x; pc.y = row * lh; x += pc.adv
                            pc.letters?.let { lt -> var off = 0; for (c in lt) { c.dx = p.measureText(pc.text, 0, off); c.w = p.measureText(c.ch); off += c.ch.length } }
                        }
                    }
                    rowW.add(x)
                    r.rowsW = rowW.toFloatArray()
                    // right-aligned (the other singer) and right-to-left lines. A duet's lines wrap 14% short of the
                    // other side, as Spicy Lyrics pads them, but the other singer's still end at the right edge
                    for (pc in ps) {
                        val rw = r.rowsW[(pc.y / lh).roundToInt()]
                        if (d.rtl) pc.x = rw - pc.x - pc.adv + (pc.adv - pc.w)
                        if (d.opp) pc.x += avail - rw
                    }
                    r.h = (row + 1) * lh
                } else {
                    val tpp = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply { typeface = face(r.weight); textSize = fs }
                    val txt = r.text ?: ""
                    val sl = StaticLayout.Builder.obtain(txt, 0, txt.length, tpp, max(1, maxW.toInt()))
                        .setAlignment(if (d.opp) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL)
                        .setIncludePad(false).setLineSpacing(0f, 1f).build()
                    // set line height to the CSS one
                    val nat = if (sl.lineCount > 0) (sl.getLineBottom(0) - sl.getLineTop(0)).toFloat() else lh
                    val sl2 = if (abs(nat - lh) > .5f) StaticLayout.Builder.obtain(txt, 0, txt.length, tpp, max(1, maxW.toInt()))
                        .setAlignment(if (d.opp) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL)
                        .setIncludePad(false).setLineSpacing(lh - nat, 1f).build() else sl
                    r.layout = sl2; r.paint = tpp
                    r.h = max(lh, sl2.lineCount * lh)
                }
                y = r.y + r.h
                if (r.kind == 1 || r.kind == 4) bg += F * r.mt + r.h   // (a background vocal and its translation)
            }
            d.hText = y + 2 * F * linePad(); d.bgH = bg
        }
        laidOut = true
        lv.forEach { it.dirty = true }
    }

    /** Line tops: inactive breaks take no room, tucked background vocals neither. */
    private fun tops(now: Double) {
        var y = 0f
        for (d in lv) {
            d.h = when {
                d.dots -> {
                    // its room comes and goes at once for where the lyrics are placed; on screen the lines after it slide
                    // over (they jumped by its height in one frame: 133 px at a break's end on a phone on its side)
                    val nh = if (d.a && !d.longBreak) d.hText else 0f
                    if (nh != d.h && S.lsmooth && !S.low && !S.calm) { d.dotOff.snap(d.dotOff.value(now) + d.h - nh); d.dotOff.set(0f, now, 550.0, Ease.ease) }
                    nh
                }
                S.xBgPop && !isStatic && d.bgH > 0 -> d.hText - d.bgH * (1 - d.bgOpen.value(now))
                else -> d.hText
            }
            d.top = y; y += d.h
        }
        totalH = y
    }
    private var totalH = 0f

    /* ================= every frame ================= */

    /** Nothing is sung for COVER_BREAK or more around now (the intro, a break between lines, the end) and the next line
     *  is more than 2 s away. A line without its end known counts as sung until the next one, unless a break follows it. */
    private fun restAt(pos: Double): Boolean {
        val a = findIdx(lines, pos)
        var p = a; while (p >= 0 && lines[p].dots) p--
        var n = a + 1; while (n < lines.size && (lines[n].dots || lines[n].t == null)) n++
        val next = lines.getOrNull(n)?.t ?: durFn?.invoke()?.takeIf { it > 0 } ?: return false
        // the cover goes 2 s before the next line, so it's there when it's sung; the Focus style brings a line in only
        // just before it's sung, so the cover stays until then (it left an empty screen in between)
        if (pos >= next - (if (S.xFocus) .6 else 2.0)) return false
        var end = if (p < 0) 0.0 else lineEnd(lines[p]) ?: lines.getOrNull(p + 1)?.takeIf { it.dots }?.t ?: return false
        for (k in max(0, p - 6) until max(p, 0)) if (!lines[k].dots) lineEnd(lines[k])?.let { if (it > end) end = it }   // a duet's line still going
        return pos >= end && next - end >= COVER_BREAK
    }

    // the song's time as the lyrics go by it: never a moment back. The Spotify app's reports can put the clock a little back
    // just after a line has started (a correction); the lyrics went back to the line before for it and on again: they
    // jumped down and up, in every style (the owner's recording). A seek back is further, and goes back as ever
    private var linePos = -1.0; private var linePosAt = 0.0
    fun frame(t: Double, rawPos: Double, viewOn: Boolean) {
        val pos = if (rawPos < linePos && linePos - rawPos < .4 && t - linePosAt < 2000) linePos else rawPos
        linePos = pos; linePosAt = t
        posNow = pos
        if (lines.isEmpty() || W <= 0) { rest = false; return }
        if (!laidOut) layoutAll()
        val wasPlace = placeUntil > 0 && t < placeUntil
        if (wasPlace) place(t, pos)
        else if (lyTall && !lyFree && t - lyTallAt > (if (pipScroll() && !pipGlide()) 250 else 40)) { lyTallAt = t; place(t, pos) }   // (often: a row's move starts on time)
        if (!viewOn) return
        flingStep(t)
        if (!sync) { rest = false; staticScroll(t, pos); tops(t); bump(); return }
        val r = S.b("aRestCover") && S.xFocus && restAt(pos)   // one of Focus mode's (Settings greys it without)
        if (r != rest) { rest = r; restAt = t }
        if (rest && covered && !lyFree) return   // the cover's over them (upright): they rest until it goes
        if (lyFree && t - lyFreeAt > 4000) freeEnd(t, pos)
        val i = findIdx(lines, pos); val e = if (i >= 0) lineEnd(lines[i]) else null; val out = e != null && pos >= e
        val nx = lines.getOrNull(i + 1)
        var si = if (S.learly && nx?.t != null && pos >= nx.t!! - S.learlyMs / 1000.0) i + 1 else i
        // on its side: the last word on the line sets off the move to the next, its first word ends it
        // (a break's dots keep their own timing, and the Focus style brings each line in its own way)
        val paced = softScroll() && S.lsmooth && !S.xFocus
        val lastEnd = if (paced && i >= 0) lines[i].words?.lastOrNull()?.let { it.end ?: it.t } else null
        val nextAt = nx?.takeIf { !it.dots }?.t
        if (lastEnd != null && nextAt != null && pos >= scrollFrom(lastEnd, nextAt)) si = i + 1
        var fi = -2; var fo = false; var fn = ""; var fs = ""
        if (S.xFocus) {   // a line is due on screen 200 ms before it's sung, but the line before holds until it's sung to the end
            // a break (its dots) holds until the next line is due, as a line holds until it's sung (it went at once,
            // and the intro and short breaks were an empty screen)
            // a line sung to the end leaves at once and flies up, as on the desktop; the middle stays empty until the next is due
            fun en(k: Int): Double {
                val l = lines[k]; val nx = lines.getOrNull(k + 1)
                if (l.dots) return nx?.t?.minus(.2) ?: (l.t ?: 0.0)
                return lineEnd(l) ?: nx?.t ?: Double.POSITIVE_INFINITY
            }
            var f = findIdx(lines, pos + .2); while (f > 0 && en(f - 1) > pos) f--
            fi = f; fo = f >= 0 && pos >= en(f); si = max(f, 0)
            var k = max(f + 1, 0)
            val sbn = StringBuilder(); val sbs = StringBuilder()
            while (k < lines.size && lines[k].t != null && pos >= lines[k].t!! - .2) { if (!lines[k].dots) { sbn.append(k).append(','); if (pos >= lines[k].t!!) sbs.append(k).append(',') }; k++ }
            fn = sbn.toString(); fs = sbs.toString()
            if (fn != lfn) lfnSet = fn.split(',').filter { it.isNotEmpty() }.map { it.toInt() }.toSet()
            if (fs != lfs) lfsSet = fs.split(',').filter { it.isNotEmpty() }.map { it.toInt() }.toSet()
        }
        // every line still being sung is active, not just the newest: a duet line can start while another is going
        val ak = StringBuilder(); if (i >= 0 && !out) ak.append(i).append(',')
        var k = i - 1
        while (k >= 0 && k >= i - 6) { val l = lines[k]; if (!l.dots) { val ek = lineEnd(l); if (ek != null && pos >= l.t!! && pos < ek) ak.append(k).append(',') }; k-- }
        val aks = ak.toString()
        run { var n = lines.size - 1; while (n >= 0 && lines[n].dots) n--; val all = i > n || (i == n && (out || (e == null && pos >= (lines[n].t ?: 0.0) + 4))); if (all != allsung) allsung = all }
        if (si != lsi) {   // one line on takes the words' time; a seek, a break and the rest the usual half second
            val was = lines.getOrNull(si - 1)?.words?.lastOrNull()?.let { it.end ?: it.t }; val at = lines.getOrNull(si)?.takeIf { !it.dots }?.t
            stepMs = if (paced && si == lsi + 1 && was != null && at != null) scrollSecs(was, at) * 1000 else 550.0
        }
        if (i != li || out != lOut || fi != lfi || fo != lfo || fn != lfn || fs != lfs || aks != lActKey) {
            lOut = out; lsi = si; lfi = fi; lfo = fo; lfn = fn; lfs = fs
            lAct = aks.split(',').filter { it.isNotEmpty() }.map { it.toInt() }.toSet(); lActKey = aks
            setActive(i, t, pos)
        } else if (si != lsi) { lsi = si; place(t, pos) }
        wordsTick(t, pos)
        tops(t)
        bump()
    }

    private fun setActive(i: Int, t: Double, pos: Double) {
        li = i
        if (S.xBgPop) placeUntil = t + 650   // background vocals sliding in or out change the line's height: follow it
        val minAct = lAct.minOrNull() ?: Int.MAX_VALUE
        val focus = S.xFocus
        for (d in lv) {
            val j = d.i
            val a = lAct.contains(j) || (j == i && !lOut)
            val dp = j == i && lOut && !lAct.contains(j)
            val p = !a && (j < i || dp)
            val sa = j < i && lAct.isNotEmpty() && j > minAct
            val dist = abs(j - i)
            val fv = lfi != -2 && j == lfi && !lfo; val fn = lfi != -2 && lfnSet.contains(j); val fs = fn && lfsSet.contains(j)
            val fu = lfi != -2 && (j < lfi || (j == lfi && lfo))
            val key = "$a$p$dp$sa${if (a) 0 else min(dist, 6)}$fv$fn$fs$fu"
            if (a && !d.a) { d.activeAt = t; moving.add(j) }
            d.a = a; d.p = p; d.dp = dp; d.sa = sa; d.dist = dist; d.fv = fv; d.fn = fn; d.fs = fs; d.fu = fu
            // a new line in the Focus style starts where it belongs: from the default look they all showed at half strength and
            // flew off, whenever lyrics were set (a translation arriving, mid-song)
            if (key != d.key) { val fresh = d.key.isEmpty() && focus; d.key = key; d.dirty = true; targets(d, t, focus, fresh) }
        }
        place(t, pos)
    }

    /** What each line's classes ask for (opacity, blur, Focus mode's flight), set as transitions. */
    private fun targets(d: LV, t: Double, focus: Boolean, snap: Boolean = false) {
        val free = lyFree
        var op = when { d.a -> 1f; d.p -> .497f; else -> .51f }
        if (S.lminimal && d.p && !d.dp) op = if (S.xKeep && d.sa) .45f else 0f
        val lb = S.lblur && !S.low
        var bl = if (d.a || !lb) 0f else when { d.dp || d.dist <= 1 -> .075f; d.dist == 2 -> .1125f; d.dist == 3 -> .15f; d.dist == 4 -> .175f; else -> .2f }
        if (S.lminimal && S.xKeep && d.p && d.sa && !d.dp) bl = 0f
        var y = 0f; var x = 0f
        var dO = 200.0; var eO = Ease.opac; var dB = 350.0; var eB = Ease.easeOut; var dT = 800.0; var eT = Ease.eOut
        if (focus) {
            dO = 500.0; eO = Ease.ease; dB = 600.0; eB = Ease.ease
            if (d.fv) { op = 1f; bl = 0f }
            else if (d.fu) { op = 0f; bl = .35f; y = -.34f * screenH; if (duet) x = if (d.opp) .06f * screenW else -.06f * screenW; dO = 450.0; dB = 450.0; dT = 600.0; eT = Ease.fuMove }   // each singer's leave on their own side
            // the next line, coming into focus: as it'll look there, not dimmed and blurred on top of its words' own dimness
            // (it came in washed out and brightened once in place: "it should stay the same", 0.46.4)
            // (and it's there at full strength when its first word is sung: from nothing over half a second, starting .2 s
            // before it, it was still dim and blurred under its first words, sung and filled: "they should be white")
            else if (d.fn) { op = 1f; bl = 0f; dO = 180.0; eO = Ease.eOut; dB = 180.0; eB = Ease.eOut }
            else { op = 0f; bl = .35f; y = .6f * screenH; if (duet) x = if (d.opp) .06f * screenW else -.06f * screenW }
            if (d.dots && (!d.fv || d.longBreak)) op = 0f   // a break's dots only while it's the one in focus (a long one has none)
            if (S.low) bl = 0f   // Low performance mode: they move and fade, never blur
        }
        if (d.dots && !focus) op = if (d.a && !d.longBreak) 1f else 0f
        if (free) { bl = 0f; if (!d.a && !d.fv) op = .55f; y = 0f; x = 0f; dO = 300.0 }
        if (S.calm) { y = 0f; x = 0f }   // Motion sickness: the Focus style's lines fade in and out where they are, never fly
        d.opacity.set(op, t, if (d.dots) 140.0 else dO, eO)
        d.blur.set(bl, t, dB, eB)
        d.fy.set(y, t, dT, eT); d.fx.set(x, t, dT, eT)
        // (Low performance mode: no fades, no flights)
        if (snap || S.low) { d.opacity.snap(op); d.blur.snap(bl); d.fy.snap(y); d.fx.snap(x) }
        if (S.xBgPop) { if (S.low || S.calm) d.bgOpen.snap(if (d.a) 1f else 0f) else d.bgOpen.set(if (d.a) 1f else 0f, t, 500.0, Ease.eOut) } else d.bgOpen.snap(1f)
        if (d.dots) { if (S.low || S.calm) d.dotOp.snap(if (d.a && !d.longBreak) 1f else 0f) else d.dotOp.set(if (d.a && !d.longBreak) 1f else 0f, t, 140.0, Ease.linear) }
    }

    /* ---- where the lyrics sit (place / fitLine) ---- */
    private fun place(t: Double, pos: Double) {
        if (lv.isEmpty() || isStatic) return
        tops(t)
        val el = lv.getOrNull(max(if (lsi >= 0) lsi else li, 0)) ?: return
        // (Focus once took .74 of the area with the bars on, for the web app's bars over the lyrics; here they're under
        // the lyrics' area already, and it put the line near the top)
        val hh = H
        // the small window can keep the line being sung just under its top, the room going to the lines coming next
        // (Spicy Lyrics Mobile 0.17)
        var y = if (lineAtTop()) hh * .06f - el.top else hh * (if (S.xFocus) .44f else .38f) - (el.top + el.h / 2)   // Focus: in the middle
        // two lines sung at once (a duet, or lines whose times overlap, as some Spicy Lyrics syncs have): both in view,
        // the pair around the middle, or in the whole height when it's taller; taller than the view, the newer one whole
        // (in the Focus style the line in focus is the older one and the newer waits under it: only when it wouldn't fit there and
        // it's being sung too: for one just due, on a phone on its side, the line being sung slid up .2 s before its end, then again)
        val under = if (S.xFocus) lfsSet.maxOrNull()?.takeIf { it > el.i }?.let { k -> lv.firstOrNull { it.i == k } }?.takeIf { it.top + it.h + y > hh * .8f } else null
        // (not one just ending: a fast song's line often runs a moment past the next one's start, and the lyrics moved on
        // to the next, pulled back to show both, and moved on again as it ended: they jumped; it ends where it is)
        val older = lAct.minOrNull()?.takeIf { it < el.i && (pipScroll() || pos >= (el.l.t ?: 0.0)) }?.let { k -> lv.firstOrNull { it.i == k } }
            ?.takeIf { d -> (lineEnd(d.l) ?: Double.POSITIVE_INFINITY) - pos > .8 }
        val first = when { lineAtTop() -> null; S.xFocus -> under?.let { el }; else -> older }   // (a line on its way isn't sung yet: not two at once)
        val lower = under ?: el
        if (older != null && !S.xFocus && pinned(older, lower, hh, t, pos)) return
        pins(-1, 0f, t)
        if (first != null) {
            val top0 = first.top; val bot = lower.top + lower.h
            val span = bot - top0
            y = when {
                span <= hh * .64f -> hh * .46f - (top0 + bot) / 2   // around the middle
                span <= hh * .92f -> hh * .5f - (top0 + bot) / 2    // taller: both whole in the whole height
                // taller than the view: the two rows being sung (one in each) in view together, around the middle; when
                // even they're too far apart, the earlier one's near the top and the newer one below as far as there's
                // room; followed as the singing moves on
                else -> {
                    val a = top0 + sungRow(first, pos); val b = lower.top + sungRow(lower, pos)
                    if (b - a <= hh * .84f) hh * .5f - (a + b) / 2 else min(hh * .07f - top0, hh * .14f - a)
                }
            }
            lyTall = span > hh * .92f; lyTallAt = t
            // the pair is placed as a whole: fitting the newer line alone into view (as for one line) pushed the earlier
            // one out over the top; the clear band reaches over both
            band(max(0f, min(H * .14f, top0 + y - 2)), min(H, max(H * .8f, bot + y + 2)))
            setLyY(y, t)
            return
        }
        val top = if (lineAtTop()) hh * .06f else hh * .16f; if (el.top + y < top) y = top - el.top   // a tall line moves down so its top stays out of the fade
        setLyY(fitLine(el, y, pos), t)
    }

    /** Two lines sung at once that don't fit in view together with the lines between them (a duet's long line under the
     *  other singer's, a held backing, and in the small window nearly any two): each goes its own way. The one sung
     *  longest is held at the top while it lasts; the newer one, and the lines after it, scroll on under it as they would
     *  alone, and the lines in between fade out as they pass under it. When it ends it scrolls on up with the rest.
     *  When the held line is too tall to go whole with the newer one (the small window), only the row of it being sung
     *  shows at the top, following the singing down it. False when they'd fit in view anyway, or not even that fits. */
    private fun pinned(first: LV, lower: LV, hh: Float, t: Double, pos: Double): Boolean {
        if (!lineAtTop() && lower.top + lower.h - first.top <= hh * .92f) return false   // both in view, with what's between them (the line at the top: the older one's always above it)
        val small = pipScroll() || lineAtTop()
        val pt = if (small) max(4 * dens, hh * .03f) else hh * .1f
        val gap = F * (if (small) .12f else .35f)
        val whole = first.h + gap + lower.h <= hh * .96f - pt
        var rowTop = 0f; var rowH = first.h
        if (!whole) {
            rowH = F * (if (small) 1.2f else 1.35f)
            // the small window holds about three rows: the newer line may reach a little into the fade at its foot
            if (rowH + gap + lower.h > (if (small) hh * 1.08f else hh * .96f) - pt) {
                if (!small) return false
                // not even that: the newer line alone (the main voice goes on; the held one shows again when there's room)
                pins(-1, 0f, t)
                setLyY(fitLine(lower, if (lineAtTop()) hh * .06f - lower.top else hh * .38f - (lower.top + lower.h / 2), pos), t)
                return true
            }
            rowTop = (sungRow(first, pos) - rowH / 2).coerceIn(0f, max(0f, first.h - rowH))
        }
        var y = if (lineAtTop()) pt - lower.top else hh * .38f - (lower.top + lower.h / 2)   // the newer one, as it would be alone
        y = max(y, pt + rowH + gap - lower.top)                                            // under the one held at the top
        if (whole && lower.top + y + lower.h > hh * .96f) y = hh * .96f - lower.h - lower.top
        val by = pt - (first.top + rowTop + y)
        pinRowTop = if (whole) 0f else rowTop; pinRowH = if (whole) 0f else rowH
        pins(first.i, if (whole) max(0f, by) else by, t)
        lyTall = !whole; lyTallAt = t   // (a row at a time: placed again as the singing moves down it)
        band(max(0f, pt - 2), min(H, max(H * .8f, lower.top + y + lower.h + 2)))
        setLyY(y, t)
        return true
    }

    /** Line [i] held [by] px further down than the scroll puts it (none: -1); the rest let go, gliding with the scroll. */
    private var pinI = -1
    private var pinRowTop = 0f; private var pinRowH = 0f   // only this part of it shows (its row being sung), when it's too tall
    private fun pins(i: Int, by: Float, t: Double) {
        if (i < 0) { pinRowTop = 0f; pinRowH = 0f }
        pinI = if (by != 0f) i else -1
        val ms = if (S.lsmooth && !S.low && !S.calm) 550.0 else 0.0
        for (d in lv) { val to = if (d.i == i) by else 0f; if (d.pin.target != to) d.pin.set(to, t, ms, Ease.eOut) }
    }

    /** The line's rows as they're wrapped, each from when its first word is sung, in order down the line: the words, then
     *  background vocals that come after them (ones sung early would swing it down and back). A line without word timing
     *  shares its time between its rows. */
    private fun wordRows(d: LV): List<WordRow> {
        val out = ArrayList<WordRow>(); val pad = F * linePad(); var lastEnd = 0.0
        for (kind in 0..1) for (r in d.rows) { if (r.kind == kind) r.pieces?.forEach { pc ->
            val h = F * r.fs * r.fit * r.lh; val y = pad + r.y + pc.y + h / 2
            val cur = out.lastOrNull()
            if (cur == null || (y > cur.y && pc.ts.t > cur.at)) { out.add(WordRow(pc.ts.t, if (cur == null) pc.ts.t else lastEnd, y, h)); lastEnd = pc.ts.end }
            else if (y == cur.y) lastEnd = max(lastEnd, pc.ts.end)
        } }
        if (out.isNotEmpty()) return out
        val r = d.rows.firstOrNull { it.kind == 0 && it.pieces == null } ?: return out
        val t0 = d.l.t ?: return out; val e = lineEnd(d.l) ?: lines.getOrNull(d.i + 1)?.t ?: return out
        if (e <= t0) return out
        val n = max(r.layout?.lineCount ?: 1, 1); val rh = r.h / n
        for (k in 0 until n) { val at = t0 + (e - t0) * k / n; out.add(WordRow(at, at - .5, pad + r.y + rh * (k + .5f), rh)) }
        return out
    }

    /** How far down the line (px) the row the scroll is on has its middle. */
    private fun sungRow(d: LV, pos: Double): Float {
        if (pipScroll()) return pipSungRow(d, pos)
        val rows = wordRows(d); return rows.getOrNull(dueRow(rows, pos))?.y ?: 0f
    }

    /** The small window's: how far down the line (px) the words being sung are: its words and background vocals, not what's
     *  under them. A line without word timing goes by its own time. */
    private fun pipSungRow(d: LV, pos: Double): Float {
        var best: Float? = null; var bt = Double.NEGATIVE_INFINITY
        for (r in d.rows) { if (r.kind > 1) continue; r.pieces?.forEach { pc -> if (pc.ts.t <= pos && pc.ts.t > bt) { bt = pc.ts.t; best = F * linePad() + r.y + pc.y + F * r.fs * r.fit * r.lh / 2 } } }
        best?.let { return it }
        val r = d.rows.firstOrNull { it.kind == 0 && it.pieces == null } ?: return 0f
        val t0 = d.l.t ?: return 0f; val e = lineEnd(d.l) ?: lines.getOrNull(d.i + 1)?.t ?: return 0f
        return if (e <= t0) 0f else F * linePad() + r.y + r.h * ((pos - t0) / (e - t0)).coerceIn(0.0, 1.0).toFloat()
    }

    /** Where the line's last words are (px down it): what comes after them (a translation) is what's left to show. */
    private fun lastSung(d: LV): Float {
        var best = 0f
        for (r in d.rows) {
            if (r.kind > 1) continue
            val ps = r.pieces
            if (ps != null) ps.forEach { pc -> best = max(best, F * linePad() + r.y + pc.y + F * r.fs * r.fit * r.lh / 2) }
            else if (r.kind == 0) best = max(best, F * linePad() + r.y + r.h)
        }
        return best
    }

    private fun fitLine(el: LV, y: Float, pos: Double): Float {
        // (in a short view the fades stay small: 18 dp at each end of the small window hid nearly half of it)
        val pip = pipScroll()
        val h = el.h; val m = if (pip) max(18 * dens, H * .05f) else max(min(18 * dens, H * .07f), H * .05f); var t = el.top + y; lyTall = false
        if (lyFree || H <= 0 || (t >= H * .14f && t + h <= H * .8f)) { band(-1f, -1f); return y }   // it fits: the usual band
        if (h <= H - 2 * m) t = max(m, min(t, H - m - h))   // the whole line in view, as high as it needs to go
        else if (pip) {   // taller than everything, in the small window: it goes down with the singing
            lyTall = true; val e = lineEnd(el.l)
            t = pipTallLineTop(H, m, h, sungRow(el, pos), lastSung(el), e != null && pos >= e)
            band(m, H - m); return t - el.top
        }
        else {   // taller than everything: it moves on a row at a time, on the words' timing (on its side, and upright)
            lyTall = true; val e = lineEnd(el.l); val ended = e != null && pos >= e
            val rows = wordRows(el); val k = dueRow(rows, pos); val r = rows.getOrNull(max(k, 0))
            t = tallLineTop(H, m, h, r?.let { it.y - it.h / 2 } ?: 0f, rows.getOrNull(k - 1)?.h ?: 0f, ended)
            if (ended) stepMs = 550.0 else if (k >= 1 && r != null) stepMs = scrollSecs(r.lastEnd, r.at) * 1000
            band(m, H - m); return t - el.top
        }
        band(max(0f, min(H * .14f, t - 2)), min(H, max(H * .8f, t + h + 2)))
        return t - el.top
    }

    /** The clear band's edges ([a], [b]; -1: the usual, .14 and .8 of the height), moved there over 550 ms. Always between
     *  real edges: tweened from or to -1, an edge passed through 0 on its way (the bottom swept up the screen, dimming the
     *  lines being sung, then jumped back to .8): the "white highlight glitch" whenever a second line came in or one of
     *  two ended, in every style. */
    private fun band(a: Float, b: Float) {
        val n = now(); val ms = if (S.low || S.calm) 0.0 else 550.0
        if (bandT.value(n) < 0f) bandT.snap(H * .14f); if (bandB.value(n) < 0f) bandB.snap(H * .8f)
        bandT.set(if (a < 0f) H * .14f else a, n, ms, Ease.eOut); bandB.set(if (b < 0f) H * .8f else b, n, ms, Ease.eOut)
    }

    /** The small window with line-synced lyrics (no word timing): the scroll glides, aimed anew every frame, so a line
     *  taller than the window goes down with its fill smoothly and a new line comes in without a jolt (re-aimed every
     *  250 ms with a fresh ease each time it crept in steps: "jittery"). Word-synced lyrics keep 0.41.5's scroll there. */
    private fun pipGlide(): Boolean = pipScroll() && sync && S.lsmooth && !S.low && !S.calm &&
        lv.getOrNull(max(if (lsi >= 0) lsi else li, 0))?.l?.words == null

    private fun setLyY(y: Float, t: Double) {
        if (lyFree) return
        if (S.calm && !S.low) {   // Motion sickness: no scroll; a soft fade out and in around the move (draw)
            if (gliding) { lyY.snap(lyG.value(t)); gliding = false }
            if (abs(y - (calmTo ?: lyY.target)) < 1f) return
            if (calmTo == null) calmAt = t   // (a new place while fading out: the same fade, to the newer place)
            calmTo = y
            return
        }
        calmTo = null
        // on its side, and a tall line anywhere but the small window: they glide instead of snapping into place, on the
        // words' timing (frame, fitLine); it keeps its speed when the target moves on
        if (S.lsmooth && !S.low && ((!pipScroll() && ((softScroll() && !S.xFocus) || lyTall)) || pipGlide())) {   // (the Focus style's as upright)
            if (!gliding) { lyG.snap(lyY.value(t)); gliding = true }
            lyG.set(y, t, stepMs)
            return
        }
        if (gliding) { lyY.snap(lyG.value(t)); gliding = false }
        lyY.set(y, t, if (S.lsmooth && !S.low) 550.0 else 0.0, Ease.eOut)   // (Low performance mode: it jumps)
    }

    /* ---- scrolling through the lyrics by hand, as in Spicy Lyrics ---- */
    fun freeBy(dy: Float) { flingV = 0f; scrollBy(dy) }

    /** Moves the lyrics by [dy] px (a finger's move, or a fling's step); false when they're at an end and didn't move. */
    private fun scrollBy(dy: Float): Boolean {
        if (lines.isEmpty() || !sync) {
            if (!isStatic) return false
            stUser = now(); stPos = null
            val to = (stScroll - dy).coerceIn(0f, max(0f, staticMax())); val moved = to != stScroll; stScroll = to; bump(); return moved
        }
        val t = now()
        if (!lyFree) { lyFree = true; lyFreeY = if (gliding) lyG.value(t) else lyY.value(t); gliding = false; lv.forEach { targets(it, t, S.xFocus) } }
        val hh = H; val maxY = hh * .45f; val minY = min(maxY, hh * .45f - totalH)
        val to = (lyFreeY + dy).coerceIn(minY, maxY); val moved = to != lyFreeY
        lyFreeY = to
        lyY.snap(lyFreeY)
        lyFreeAt = t
        bump()
        return moved
    }

    /* the lyrics let go of with some speed carry on and slow down, as any list does (Compose's exponential decay: its
       friction of 4.2/s); they stopped dead the moment the finger lifted. Worked out from the time on each frame (the frame
       limit's), not a Compose animation */
    private var flingV = 0f; private var flingAt = 0.0; private var flingDone = 0f
    fun freeFling(v: Float) {
        flingV = if (abs(v) < 80f * dens || S.low) 0f else v
        flingAt = now(); flingDone = 0f
        bump()
    }
    /** A touch stops a fling under way; true if there was one. */
    fun stopFling(): Boolean { val was = flingV != 0f; flingV = 0f; return was }
    private fun flingStep(t: Double) {
        if (flingV == 0f) return
        val s = ((t - flingAt) / 1000.0).toFloat()
        val total = flingV / FLING_FRICTION * (1f - kotlin.math.exp(-FLING_FRICTION * s))
        val dy = total - flingDone; flingDone = total
        val v = flingV * kotlin.math.exp(-FLING_FRICTION * s)
        if (!scrollBy(dy) || abs(v) < 20f * dens) flingV = 0f
    }

    fun freeEnd(t: Double = now(), pos: Double? = null) {
        if (!lyFree) return
        lyFree = false; flingV = 0f
        lv.forEach { targets(it, t, S.xFocus) }
        lyY.snap(lyFreeY)
        if (pos != null) place(t, pos) else lActKey = "x"   // re-placed on the next frame
        bump()
    }

    /** The line under a tap (y inside the view), or -1. */
    fun lineAt(y: Float): Int {
        if (isStatic) return -1
        val yy = y - (if (gliding) lyG.v else lyY.v)
        lv.getOrNull(pinI)?.let { d -> val a = d.top + d.pin.v + (if (pinRowH > 0f) pinRowTop else 0f)
            if (d.i == pinI && yy >= a && yy < a + (if (pinRowH > 0f) pinRowH else d.h)) return d.i }   // (held at the top)
        for (d in lv) if (!d.dots && d.h > 0 && yy >= d.top && yy < d.top + d.h && d.opacity.v > .05f && d.i != pinI) return d.i
        return -1
    }

    fun tapped(i: Int) { lv.getOrNull(i)?.let { it.tapAt = now(); it.dirty = true; bump() } }

    // untimed lyrics scroll slowly by themselves: the top as the song starts, the end as it ends
    private fun staticMax() = totalH + H * .49f - H
    private fun staticScroll(t: Double, pos: Double) {
        val dt = min(.1, (t - (if (stLast == 0.0) t else stLast)) / 1000); stLast = t
        if (t - stUser < 4000) return
        val d = durFn?.invoke() ?: 0.0; val mx = staticMax(); if (d <= 0 || mx <= 0) return
        val target = (pos / d).coerceIn(0.0, 1.0).toFloat() * mx
        var p = stPos ?: stScroll.also { stVel = 0f }
        val gap = target - p
        if (abs(gap) < 1.5f && abs(stVel) < 20) { p = target; stVel = 0f }
        else { val w = 2 * Math.PI.toFloat() * .9f; stVel += (w * w * gap - 2 * w * stVel) * dt.toFloat(); p += stVel * dt.toFloat() }
        stPos = p; stScroll = p
    }
    var durFn: (() -> Double)? = null

    /* ---- the words (wordsTick) ---- */
    private fun wordsTick(t: Double, pos: Double) {
        val dt = min(.1, max(0.0, (t - (if (lastWT == 0.0) t else lastWT)) / 1000)); lastWT = t
        if (li >= 0) moving.add(li)
        lAct.forEach { moving.add(it) }
        val simple = S.low || S.lsimple || S.calm   // (Motion sickness: the words fill in place, no lift or bounce)
        val refreshed = S.lsimple && S.xRefSimple && !S.low && !reduceMotion()
        val it = moving.iterator()
        while (it.hasNext()) {
            val k = it.next(); val d = lv.getOrNull(k)
            if (d == null) { it.remove(); continue }
            // Low performance mode: a break's dots simply show, without their breathing and filling
            if (d.dots && (S.low || S.calm)) { val on = if (d.a && !d.longBreak) 1f else 0f; if (d.dotScale.v != on) { d.dotScale.snap(on); restDots(d) }; it.remove(); continue }
            d.dirty = true
            if (d.dots) {
                if (k > li) { restDots(d); it.remove(); continue }
                if (!animateDots(d, pos, dt) && k != li) it.remove()
                continue
            }
            if (k > li) { restLine(d); it.remove(); continue }   // seeking back: lines ahead go back to rest
            var m = false
            val main = d.rows.firstOrNull()
            if (refreshed && main?.pieces != null) {
                m = refreshedLine(d, main, pos)   // its extra rows fill simply (drawn from the clock)
                d.rows.forEach { r -> r.oneTs?.let { o -> oneGrad[r] = SL.fillAt(pos, o) } }
            } else if (simple) {
                for (r in d.rows) r.pieces?.forEach { pc -> pc.sp = null; pc.scale = 1.0; pc.yo = 0.0; pc.gO = 0.0; pc.grad = SL.fillAt(pos, pc.ts); pc.letters?.forEach { lt -> lt.sp = null } }
                if (main != null && main.pieces == null && main.oneTs == null) animateLineGlow(d, pos, dt)
                d.rows.forEach { r -> r.oneTs?.let { o -> oneGrad[r] = SL.fillAt(pos, o) } }
            } else {
                for (r in d.rows) r.pieces?.forEach { pc -> if (animateWord(pc, pos, dt)) m = true }
                d.rows.forEach { r -> r.oneTs?.let { o -> oneGrad[r] = SL.fillAt(pos, o) } }
                if (main != null && main.pieces == null && main.oneTs == null) m = animateLineGlow(d, pos, dt) || m   // line-synced: the line itself fills
            }
            if (t - d.activeAt < 600) m = true
            if (!m && k != li && !lAct.contains(k)) it.remove()   // settled; the words keep their final pose, as in Spicy Lyrics
        }
    }
    private val oneGrad = HashMap<Row, Double>()
    var reduceMotion: () -> Boolean = { false }

    private fun restLine(d: LV) {
        d.sweep = null; d.lineGlow = null; d.lineGrad = -20.0; d.lineGlowV = 0.0
        for (r in d.rows) { oneGrad.remove(r); r.pieces?.forEach { pc -> pc.sp = null; pc.scale = Double.NaN; pc.yo = 0.0; pc.grad = -20.0; pc.gO = 0.0; pc.gR = 0.0; pc.slm = -50.0; pc.letters?.forEach { it.sp = null } } }
        d.dirty = true
    }
    private fun restDots(d: LV) { val sp = d.dotSp ?: return; for (k in sp.indices) sp[k] = DotSprings(); d.dotV.fill(0.0); d.dirty = true }

    fun restAll() { moving.forEach { lv.getOrNull(it)?.let(::restLine) }; moving.clear(); lastWT = 0.0; li = -2; lsi = -2; lActKey = "x"; bump() }

    // per piece of text: values for drawing (scale, y in em, gradient, glow); letters keep theirs in their springs
    private val letterVals = HashMap<Lt, DoubleArray>()

    private fun animateWord(pc: Pc, pos: Double, dt: Double): Boolean {
        val w = pc.ts
        val state = SL.state(pos, w.t, w.end); val pct = SL.pct(pos, w.t, w.end); val lg = pc.letters != null
        val sp = pc.sp ?: Springs(SL.ScaleSpline, SL.YOffsetSpline).also { pc.sp = it }
        val q = when (state) { 1 -> pct; 0 -> 0.0; else -> 1.0 }
        sp.scale.goal(SL.ScaleSpline.at(q)); sp.y.goal(SL.YOffsetSpline.at(q)); sp.glow.goal(SL.GlowSpline.at(q))
        val cS = sp.scale.step(dt); val cY = sp.y.step(dt); val cG = sp.glow.step(dt)
        pc.scale = cS; pc.yo = cY; pc.grad = when (state) { 1 -> -20 + 120 * pct; 0 -> -20.0; else -> 100.0 }
        if (!lg) { pc.gR = 4 + 2 * cG; pc.gO = min(cG * 35, 100.0) } else { pc.gR = 0.0; pc.gO = 0.0 }
        var mv = !sp.asleep()
        val L_ = pc.letters ?: return mv
        var ai = -1; var ap = 0.0
        if (state == 1) for (i in L_.indices) if (SL.state(pos, L_[i].ts.t, L_[i].ts.end) == 1) { ai = i; ap = SL.pct(pos, L_[i].ts.t, L_[i].ts.end); break }
        for ((k, lt) in L_.withIndex()) {
            val ls = lt.sp ?: Springs(SL.LetterScaleSpline, SL.LetterYOffsetSpline).also { lt.sp = it }
            var sc: Double; var y: Double; var g: Double; val grad: Double
            if (state == 1) {
                sc = SL.LetterScaleSpline.at(0.0); y = SL.LetterYOffsetSpline.at(0.0); g = SL.GlowSpline.at(0.0)
                val lst = SL.state(pos, lt.ts.t, lt.ts.end)
                if (ai != -1) {
                    val bS = SL.LetterScaleSpline.at(ap); val bY = SL.LetterYOffsetSpline.at(ap); val bG = SL.GlowSpline.at(ap)
                    val rS = SL.LetterScaleSpline.at(0.0); val rY = SL.LetterYOffsetSpline.at(0.0); val rG = SL.GlowSpline.at(0.0)
                    val dist = abs(k - ai).toDouble(); val fo = max(0.0, 1 / (1 + dist.pow(2.8))); val gfo = max(0.0, 1 / (1 + dist * .9))
                    sc = rS + (bS - rS) * fo; y = rY + (bY - rY) * fo; g = rG + (bG - rG) * gfo
                }
                if (lst == 0) { sc = SL.LetterScaleSpline.at(0.0); y = SL.LetterYOffsetSpline.at(0.0); g = SL.GlowSpline.at(0.0) }
                else if (lst == 2 && ai == -1) g = SL.GlowSpline.at(.2)
                grad = if (lst == 0) -20.0 else if (lst == 2) 100.0 else if (k == ai) -20 + 120 * SL.easeSinOut(ap) else -20.0
            } else if (state == 0) { sc = SL.LetterScaleSpline.at(0.0); y = SL.LetterYOffsetSpline.at(0.0); g = SL.GlowSpline.at(0.0); grad = -20.0 }
            else { sc = SL.LetterScaleSpline.at(1.0); y = SL.LetterYOffsetSpline.at(1.0); g = SL.GlowSpline.at(1.0); grad = 100.0 }
            ls.scale.goal(sc); ls.y.goal(y); ls.glow.goal(g)
            val a = ls.scale.step(dt); val b = ls.y.step(dt); val c = ls.glow.step(dt)
            val v = letterVals.getOrPut(lt) { DoubleArray(5) }
            v[0] = a; v[1] = b * 2; v[2] = grad; v[3] = 4 + 12 * c; v[4] = c * SL.LetterGlowMultiplier_Opacity
            if (!ls.asleep()) mv = true
        }
        return mv
    }

    private fun animateLineGlow(d: LV, pos: Double, dt: Double): Boolean {   // lines without word timing: top-to-bottom fill and a glow
        val l = d.l; val t0 = l.t ?: return false
        val end = l.end ?: lines.getOrNull(d.i + 1)?.t ?: (t0 + 4)
        val state = SL.state(pos, t0, end); val pct = SL.pct(pos, t0, end)
        val g0 = d.lineGlow ?: Spring(SL.LineGlowSpline.at(0.0), 1.0, .5).also { d.lineGlow = it }
        g0.goal(if (state == 1) SL.LineGlowSpline.at(pct) else SL.LineGlowSpline.at(if (state == 2) 1.0 else 0.0))
        val g = g0.step(dt)
        d.lineGrad = when (state) { 1 -> pct * 100; 0 -> -20.0; else -> 100.0 }
        d.lineGlowV = g
        return !g0.canSleep()
    }

    private fun animateDots(d: LV, pos: Double, dt: Double): Boolean {   // the three dots each take a third of the break, ending 550 ms before the next line
        val l = d.l; val t0 = l.t ?: 0.0; val end = l.end ?: (t0 + 4)
        val total = max(.1, end - t0); val base = total / 3; val pad = SL.InterludeTimePadding / 3
        val e1 = max(0.0, base + pad); val e2 = max(e1, base * 2 + pad * 2); val e3 = max(e2, total + SL.InterludeTimePadding)
        val span = arrayOf(0.0 to e1, e1 to e2, e2 to e3)
        val ph = pos > end - SL.preHiddenDotLine
        d.dotScale.set(if (ph || !d.a) 0f else 1f, now(), if (ph || !d.a) 400.0 else 300.0, if (ph || !d.a) Ease.dotHide else Ease.ease)
        var mv = d.dotScale.moving(now())
        val sps = d.dotSp ?: return false
        for (k in 0 until 3) {
            val a = t0 + span[k].first; val b = t0 + span[k].second; val st = SL.state(pos, a, b); val pc = SL.pct(pos, a, b)
            val q = if (st == 1) pc else if (st == 0) 0.0 else 1.0
            val sp = sps[k]
            sp.scale.goal(SL.DotScaleSpline.at(q)); sp.y.goal(SL.DotYOffsetSpline.at(q)); sp.glow.goal(SL.DotGlowSpline.at(q)); sp.opacity.goal(SL.DotOpacitySpline.at(q))
            d.dotV[k * 4] = sp.scale.step(dt); d.dotV[k * 4 + 1] = sp.y.step(dt) / 1.3; d.dotV[k * 4 + 2] = sp.glow.step(dt); d.dotV[k * 4 + 3] = sp.opacity.step(dt).coerceIn(0.0, 1.0)
            if (!sp.asleep()) mv = true
        }
        return mv
    }

    private fun refreshedLine(d: LV, main: Row, pos: Double): Boolean {
        val ps = main.pieces ?: return false
        val sw = d.sweep ?: run {
            val n = ps.size; val xs = FloatArray(n); val ws = FloatArray(n); val lx = arrayOfNulls<FloatArray>(n); val lw = arrayOfNulls<FloatArray>(n)
            var x = 0f
            for ((k, pc) in ps.withIndex()) { xs[k] = x; ws[k] = pc.w; pc.letters?.let { ls -> lx[k] = FloatArray(ls.size) { ls[it].dx }; lw[k] = FloatArray(ls.size) { ls[it].w } }; x += pc.w }
            Sweep(xs, ws, lx, lw, F * RF.fade.toFloat(), DoubleArray(n) { Double.NEGATIVE_INFINITY }).also { d.sweep = it }
        }
        // the edge crosses each word in that word's time and waits where the last one ended through a gap
        val E_ = sw.edges; val half = sw.fade / 2; val last = ps.size - 1; var reached = Double.NEGATIVE_INFINITY
        for (i in ps.indices) {
            val w = ps[i].ts; val from = if (i == 0) sw.xs[i] - half else sw.xs[i]; val to = sw.xs[i] + sw.ws[i] + if (i == last) half else 0f
            var at = Double.NEGATIVE_INFINITY
            if (pos >= w.end) at = to.toDouble() else if (pos > w.t) at = from + (to - from) * (pos - w.t) / (w.end - w.t)
            E_[i] = at; if (at > reached) reached = at
        }
        var before = Double.NEGATIVE_INFINITY
        for (i in ps.indices) { val w = ps[i].ts; val at = E_[i]; if (pos >= w.end) E_[i] = reached else if (pos <= w.t) E_[i] = before; if (at > before) before = at }
        var cap = 1.0; var mv = false
        for ((j, pc) in ps.withIndex()) {
            val w = pc.ts
            cap = min(cap, RF.float(w.t, w.end, pos)); pc.yo = SL.YOffsetSpline.at(cap); pc.scale = 1.0; pc.gO = 0.0
            if (pc.letters == null) { pc.slm = RF.pos(E_[j], sw.xs[j].toDouble(), sw.ws[j].toDouble(), sw.fade.toDouble()); continue }
            val L_ = pc.letters!!; val n = L_.size; val wave = n in 2..7   // longer held words fill like any other word
            val st = RF.strength(w.end - w.t, j == last); val start = L_[0].ts.t
            for ((k, lt) in L_.withIndex()) {
                val g = RF.pos(E_[j], (sw.xs[j] + sw.lx[j]!![k]).toDouble(), sw.lw[j]!![k].toDouble(), sw.fade.toDouble())
                val v = letterVals.getOrPut(lt) { DoubleArray(7) }.let { if (it.size < 7) DoubleArray(7).also { n2 -> letterVals[lt] = n2 } else it }
                v[5] = g
                if (!wave) { v[0] = 1.0; v[1] = 0.0; v[6] = 0.0; v[3] = 0.0; v[4] = 0.0; continue }
                val at = pos - start - (st.du * RF.stagger / n) * k; val swv = RF.emph(at / st.du); val fa = (at + RF.lead) / (st.du * RF.stretch)
                val fl = if (fa > 0 && fa < 1) sin(fa * Math.PI) * RF.float else 0.0
                val lx = -swv * RF.spread * st.a * (n / 2.0 - k); val ly = -swv * RF.lift * st.a - fl
                v[0] = 1 + swv * RF.scale * st.a; v[1] = ly; v[6] = lx
                v[3] = min(RF.maxR, st.b * RF.radius) * 40   // in px at a 40 px font, like the others
                val lit = min(1.0, max(0.0, (g + 50) / 150)); v[4] = swv * st.b * (RF.dim + (1 - RF.dim) * lit) * 100
                if (!(swv == 0.0 && fl == 0.0)) mv = true
            }
            if (cap < 1) mv = true
        }
        return mv
    }

    /* ================= drawing ================= */

    // kept as the layer's own kind of effect: a new wrapper every frame (even around the same effect) counted as a new
    // effect, and every blurred line was blurred again each frame instead of reusing its finished picture
    private val shadowCache = HashMap<Int, androidx.compose.ui.graphics.RenderEffect>()
    private fun effect(blurPx: Float, shadow: Boolean): androidx.compose.ui.graphics.RenderEffect? {
        val q = (blurPx * 2).roundToInt()
        if (q <= 0 && !shadow) return null
        val key = q * 2 + if (shadow) 1 else 0
        return shadowCache.getOrPut(key) {
            if (shadow) {
                // drop-shadow(0 6px 16px rgba(0,0,0,.35)): a soft dark halo under the line being sung
                val r = 16f / 40f * F * .5f; val m = ColorMatrix(floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, .35f, 0f))
                val sh = RenderEffect.createOffsetEffect(0f, 6f / 40f * F, RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(m), RenderEffect.createBlurEffect(r, r, Shader.TileMode.DECAL)))
                RenderEffect.createBlendModeEffect(sh, RenderEffect.createOffsetEffect(0f, 0f), BlendMode.SRC_OVER).asComposeRenderEffect()
            } else RenderEffect.createBlurEffect(q / 2f, q / 2f, Shader.TileMode.DECAL).asComposeRenderEffect()
        }
    }

    // Motion sickness (S.calm): the place the lyrics go to next (setLyY), and when they started to fade out for it
    private var calmTo: Float? = null; private var calmAt = 0.0; private var calmInAt = -1e9
    /** How much the lyrics show (Motion sickness): out over 140 ms, put in their new place unseen, in over 260 ms. */
    private fun calmFade(t: Double): Float {
        val to = calmTo
        if (to != null) {
            val k = (t - calmAt) / 140
            if (k < 1) return (1 - .9 * Ease.ease(k)).toFloat()
            lyY.snap(to); calmTo = null; calmInAt = t
        }
        val k = (t - calmInAt) / 260
        return if (k >= 1) 1f else (.1 + .9 * Ease.eOut(k.coerceAtLeast(0.0))).toFloat()
    }

    fun draw(ds: DrawScope, obtain: () -> GraphicsLayer) {
        if (lines.isEmpty() || W <= 0) return
        if (!laidOut) { layoutAll(); tops(now()) }
        val t = now()
        val la = leaveA.value(t) * calmFade(t)
        val pad = ceil(F * .7f).toInt()
        val baseY = if (isStatic) (if (bleedTop > 0f) 12 * dens else H * .09f) - stScroll else if (gliding) lyG.value(t) else lyY.value(t)
        val st = isStatic
        drawnY = bleedTop.roundToInt() + baseY.roundToInt()
        // credits under the lyrics: right after the last line
        val lastBottom = lv.lastOrNull()?.let { it.top + it.h } ?: 0f
        val cy = baseY + lastBottom + F * .5f; if (abs(cy - creditsY) > .5f) creditsY = cy
        var slide = 0f   // from the breaks above: their room coming or going (tops)
        val hush = quiet()
        // the line held at the top (pinned): where it is, so the lines passing under it fade out there
        val held = if (st || lyFree) null else lv.getOrNull(pinI)?.takeIf { it.i == pinI && abs(it.pin.value(t)) > .5f }
        val clipRow = held != null && pinRowH > 0f
        val heldTop = held?.let { baseY + it.top + it.fy.value(t) + it.pin.value(t) + (if (clipRow) pinRowTop else 0f) } ?: 0f
        val heldBot = held?.let { heldTop + (if (clipRow) pinRowH else it.h) } ?: 0f
        for (d in lv) {
            d.shown = false
            val above = slide; if (d.dots) slide += d.dotOff.value(t)
            val op0 = if (st) 1f else d.opacity.value(t)
            val enterK = if (enterAt > 0 && !st) { val delay = min(abs(d.i - enterFrom), 12) * 45.0; Ease.eOut(((t - enterAt - delay) / 700).coerceIn(0.0, 1.0)).toFloat() } else 1f
            var op = op0 * la * (if (t - enterAt < 1300) enterK else 1f)
            val fy = if (st) 0f else d.fy.value(t) + (if (lyFree) 0f else d.pin.value(t)); val fx = if (st) 0f else d.fx.value(t)
            if (held != null && d !== held) {   // under the held line: gone over most of a line's height as it slides in under it
                val yt = baseY + d.top + above + fy
                if (yt < heldBot && yt + d.h > heldTop) op *= ((yt - heldBot) / (F * .8f) + 1f).coerceIn(0f, 1f)
            }
            if (op <= .004f || d.h <= 0f && !d.dots) continue
            val inView = d.top + above + fy + (if (enterK < 1f) 26 * dens * (1 - enterK) else 0f)
            val y = baseY + inView
            val hh = max(d.h, d.hText)
            if (y + hh + pad < -H * .1f - bleedTop || y - pad > H * 1.1f + bleedBot) continue
            val avail = W - padL - padR
            val w = (avail + 2 * pad).toInt(); val h = (hh + 2 * pad).toInt().coerceAtLeast(1)
            val inner = d.inner ?: obtain().also { d.inner = it; d.dirty = true; it.compositingStrategy = androidx.compose.ui.graphics.layer.CompositingStrategy.ModulateAlpha }
            val layer = d.layer ?: obtain().also { d.layer = it; d.recW = 0 }
            val sized = d.recW != w || d.recH != h
            if (d.dirty || sized || d.tapAt > t - 300) {
                inner.record(ds, ds.layoutDirection, IntSize(w, h)) { drawIntoCanvas { c -> c.nativeCanvas.translate(pad.toFloat(), pad.toFloat()); drawLine(c.nativeCanvas, d, t) } }
                d.dirty = d.a || moving.contains(d.i) || (S.xBgPop && d.bgOpen.moving(t)) || (d.dots && d.dotScale.moving(t))
            }
            if (sized) { layer.record(ds, ds.layoutDirection, IntSize(w, h)) { drawLayer(inner) }; d.recW = w; d.recH = h }
            // the scroll rounded once, for all of them: each line rounding its own sum stepped at its own moment, so the
            // lines shifted against each other by a pixel as they scrolled
            layer.topLeft = IntOffset((bleedLeft + padL + fx - pad).roundToInt(), bleedTop.roundToInt() + baseY.roundToInt() + (inView - pad).roundToInt())
            inner.alpha = op.coerceIn(0f, 1f)
            val blurPx = if (st || lyFree || hush) 0f else d.blur.value(t) * F
            val tapK = ((t - d.tapAt) / 260).coerceIn(0.0, 1.0).toFloat()
            val sc = if (tapK < 1f && !S.low && !S.calm) .97f + .03f * Ease.easeOut(tapK.toDouble()).toFloat() else 1f
            layer.scaleX = sc; layer.scaleY = sc
            layer.pivotOffset = androidx.compose.ui.geometry.Offset(if (d.opp) pad + avail else pad.toFloat(), h / 2f)
            val fx2 = effect(blurPx, d.a && !S.low && !st && !hush); if (layer.renderEffect !== fx2) layer.renderEffect = fx2
            if (d === held && clipRow) {   // too tall to go whole: only its row being sung
                val top = bleedTop + heldTop - F * .15f
                ds.clipRect(-W, top, 2 * W + bleedLeft * 2, top + pinRowH + F * .3f) { drawLayer(layer) }
            } else ds.drawLayer(layer)
            d.px = layer.topLeft.x.toFloat(); d.py = layer.topLeft.y.toFloat(); d.shown = true
        }
        if (enterAt > 0 && t - enterAt > 1300) enterAt = -1e9
    }

    /** The lines' pictures for the blurred copy past the lyrics' area (LyricsPane), after [draw]: without their own blur, as a
     *  blur drawn a second time under another scale is worked out again, for every line, every frame (the whole app stuttered
     *  behind a Canvas, whose every frame draws all this). Those wholly between [from] and [to] don't show in it. */
    fun drawPlain(ds: DrawScope, from: Float, to: Float) {
        for (d in lv) {
            if (!d.shown || (d.py >= from && d.py + d.recH <= to)) continue
            val l = d.inner ?: continue
            ds.translate(d.px, d.py) { drawLayer(l) }
        }
    }

    /** Masks for the lyrics area: the clear band (sharp between two fades), widened for a tall line. */
    fun bandTop(): Float { val v = bandT.value(now()); return if (v < 0) H * .14f else v }
    fun bandBottom(): Float { val v = bandB.value(now()); return if (v < 0) H * .8f else v }

    private fun white(a: Double) = ((a.coerceIn(0.0, 1.0) * 255).roundToInt() shl 24) or 0xFFFFFF
    private fun white(a: Float) = white(a.toDouble())

    private fun drawLine(c: Canvas, d: LV, t: Double) {
        val padT = F * linePad()
        if (d.dots) { drawDots(c, d, padT); return }
        if (pressed == d.i && !d.a && S.b("lhover")) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = white(.08) }
            c.drawRoundRect(-.3f * F, 0f, W - padL - padR + .3f * F, d.hText, .35f * F, .35f * F, p)
        }
        val st = isStatic
        val active = d.a && !st
        val refreshedOn = S.lsimple && S.xRefSimple && !S.low && !reduceMotion()
        val far = d.p && d.dist > 5
        val bgK = if (S.xBgPop && !st) d.bgOpen.value(t) else 1f
        var yShift = 0f
        for (r in d.rows) {
            val fs = F * r.fs * r.fit; val lh = fs * r.lh
            val tucks = r.kind == 1 || r.kind == 4   // a background vocal, or its translation
            if (tucks && bgK < 1f) {   // background vocals tucked away until the line is sung
                if (bgK <= .01f) { yShift -= F * r.mt + r.h; continue }
            }
            val ry = padT + r.y + yShift
            c.save()
            if (tucks && bgK < 1f) {
                val k = bgK
                c.clipRect(-F, ry - F * .1f, W + F, ry + r.h * k + F * .2f)
                c.translate(0f, -.6f * fs * (1 - k)); c.scale(.8f + .2f * k, .8f + .2f * k, if (d.opp) W - padL - padR else 0f, ry)
                yShift -= (F * r.mt + r.h) * (1 - k)
            }
            // the subs of the line being sung slide up and fade in (.5 s; the translation a little later)
            var subA = 1f
            if (active && r.kind >= 2) {
                val k = (((t - d.activeAt) - if (r.kind == 3) 60 else 0) / 500).coerceIn(0.0, 1.0)
                val e = Ease.eOut(k).toFloat(); subA = e; c.translate(0f, 14 * dens * (1 - e))
            }
            val ps = r.pieces
            if (ps != null) {
                val p = paint(fs, r.weight)
                // (a translation: filled .85 to .5 while its line is sung, about half bright otherwise, as Spicy Lyric Translator)
                val (aFill, aEnd) = when (r.kind) { 0 -> .85f to .5f; 1, 4 -> .6f to .3f; 2 -> 1f to .45f; else -> .85f to .5f }
                val restColor = when {
                    st -> 1f
                    r.kind == 2 -> if (active) 1f else .75f
                    r.kind == 3 -> .5f
                    r.kind == 4 -> .35f
                    d.p -> if (r.kind == 1) .6f else .85f
                    else -> if (r.kind == 1) .3f else .5f
                }
                // how this row moves: springs (Spicy Lyrics), refreshed simple mode, or a plain fill
                val mode = when { st -> 0; r.kind == 0 && refreshedOn -> 2; S.low || S.lsimple || S.calm -> 1; else -> 3 }
                val restScale = if (mode != 3 || far) 1.0 else .95
                val fillOn = active
                val glowOk = fillOn && (mode == 3 || mode == 2)
                for (pc in ps) {
                    val base = ry + pc.y + (lh - (p.descent() - p.ascent())) / 2 - p.ascent()
                    val springy = mode == 3 && !pc.scale.isNaN()
                    val sc = if (springy) pc.scale else if (mode == 2) 1.0 else restScale
                    val yo = if (springy || mode == 2) pc.yo else if (mode == 3 && !far) (if (pc.letters != null) .02 else .01) else 0.0
                    val lt = pc.letters
                    if (lt == null || mode == 0) {
                        val grad = if (mode == 1) SL.fillAt(posNow, pc.ts) else pc.grad
                        drawPiece(c, p, pc.text, pc.x, base, pc.w, fs, sc.toFloat(), yo.toFloat(),
                            if (fillOn && mode != 2) grad else null, if (fillOn) aFill else restColor, if (fillOn) aEnd else restColor,
                            if (glowOk && mode == 3) pc.gR else 0.0, if (glowOk && mode == 3) pc.gO else 0.0, subA, if (fillOn && mode == 2) pc.slm else null)
                    } else {
                        // a held word: each letter paints itself (the word's own fill and glow are off)
                        c.save(); c.translate(0f, (yo * fs).toFloat()); c.scale(sc.toFloat(), sc.toFloat(), pc.x + pc.w / 2, base - fs * .35f)
                        for (x in lt) {
                            val v = letterVals[x]
                            val live = v != null && (mode == 2 || (mode == 3 && x.sp != null))
                            val lsc = if (live) v!![0] else if (mode == 3 && !far) .95 else 1.0
                            val ly = if (live) v!![1] else if (mode == 3 && !far) .02 else 0.0
                            val lxo = if (live && mode == 2 && v!!.size > 6) v[6] else 0.0
                            val grad = if (mode == 1) SL.fillAt(posNow, x.ts) else v?.get(2) ?: -20.0
                            drawPiece(c, p, x.ch, pc.x + x.dx + (lxo * fs).toFloat(), base, x.w, fs, lsc.toFloat(), ly.toFloat(),
                                if (fillOn && mode != 2) grad else null, if (fillOn) aFill else restColor, if (fillOn) aEnd else restColor,
                                if (glowOk && live) v!![3] else 0.0, if (glowOk && live) v!![4] else 0.0, subA,
                                if (fillOn && mode == 2 && v != null && v.size > 5) v[5] else null)
                        }
                        c.restore()
                    }
                }
            } else {
                val sl = r.layout; val tpp = r.paint
                if (sl != null && tpp != null) {
                    tpp.clearShadowLayer(); tpp.shader = null
                    when {
                        st -> { tpp.color = white(1f); tpp.setShadowLayer(.2f * fs, 0f, .05f * fs, 0x55000000) }
                        active && r.kind == 0 && r.oneTs == null -> {   // line-synced: top-to-bottom fill
                            val g = d.lineGrad; val y0 = -.35f * fs; val y1 = r.h + .35f * fs; val hh = y1 - y0
                            tpp.color = white(1f)
                            tpp.shader = LinearGradient(0f, y0 + (hh * g / 100).toFloat(), 0f, y0 + (hh * (g + 20) / 100).toFloat(), white(.85f), white(.35f), Shader.TileMode.CLAMP)
                            val gl = d.lineGlowV; if (gl > .01 && !S.low) tpp.setShadowLayer(((4 + 8 * gl) / 40 * F * .87).toFloat(), 0f, 0f, white(gl * .5))
                        }
                        active && r.oneTs != null -> {   // a whole romanized line, or a translation, filled in time
                            val g = oneGrad[r] ?: -20.0; val (a1, a2) = when (r.kind) { 0 -> .85f to .5f; 2 -> 1f to .45f; 4 -> .6f to .3f; else -> .85f to .5f }
                            val w = sl.width.toFloat()
                            tpp.color = white(1f); tpp.shader = LinearGradient((w * g / 100).toFloat(), 0f, (w * (g + 20) / 100).toFloat(), 0f, white(a1 * subA), white(a2 * subA), Shader.TileMode.CLAMP)
                        }
                        else -> tpp.color = white(when (r.kind) {
                            0 -> if (active) .6f else if (d.p) .85f else .35f
                            1 -> if (active) .6f else if (d.p) .6f else .3f
                            2 -> if (active) 1f else .75f
                            4 -> .35f
                            else -> .5f
                        } * subA)
                    }
                    c.save(); c.translate(if (d.opp) W - padL - padR - sl.width else 0f, ry); sl.draw(c); c.restore()   // the other singer: at the right edge
                }
            }
            c.restore()
        }
    }
    var posNow = 0.0

    private fun drawPiece(c: Canvas, p: TextPaint, text: String, x: Float, base: Float, w: Float, fs: Float, scale: Float, yEm: Float,
                          grad: Double?, aFill: Float, aEnd: Float, glowR: Double, glowO: Double, alpha: Float, slm: Double?) {
        c.save()
        if (scale != 1f || yEm != 0f) { c.translate(0f, yEm * fs); c.scale(scale, scale, x + w / 2, base - fs * .35f) }
        p.shader = null; p.clearShadowLayer()
        when {
            slm != null -> {   // refreshed simple mode: one soft edge, the lit part on its left
                val fw = .55f * fs; val q = ((100 - slm) / 150).toFloat(); val ex = x + w + fw / 2 - (w + fw) * q
                p.color = white(1f); p.shader = LinearGradient(ex - fw / 2, 0f, ex + fw / 2, 0f, white(aFill * alpha), white(.4f * alpha), Shader.TileMode.CLAMP)
            }
            grad != null && aFill != aEnd -> {
                val x0 = x - .06f * fs; val gw = w + .12f * fs
                p.color = white(1f)
                p.shader = LinearGradient(x0 + (gw * grad / 100).toFloat(), 0f, x0 + (gw * (grad + 20) / 100).toFloat(), 0f, white(aFill * alpha), white(aEnd * alpha), Shader.TileMode.CLAMP)
            }
            else -> p.color = white(aFill * alpha)
        }
        if (glowO > .5) p.setShadowLayer(max(.5f, (glowR / 40 * F * .87).toFloat()), 0f, 0f, white(min(1.0, glowO / 100) * alpha))
        c.drawText(text, x, base, p)
        c.restore()
    }

    private fun drawDots(c: Canvas, d: LV, padT: Float) {
        val fs = F * 1.3f; val p = paint(fs, 700); val gap = .18f * F
        val dw = p.measureText("•"); val total = dw * 3 + gap * 2
        val avail = W - padL - padR
        val x0 = if (d.opp) avail - total else 0f
        val cy = padT + F * 1.18f / 2
        val gs = d.dotScale.value(now())
        if (gs <= .001f) return
        c.save(); c.scale(gs, gs, x0 + total / 2, cy)
        for (k in 0 until 3) {
            val sc = d.dotV[k * 4].takeIf { it > 0 } ?: .75; val y = d.dotV[k * 4 + 1]; val g = d.dotV[k * 4 + 2]; val o = d.dotV[k * 4 + 3].takeIf { it > 0 } ?: .35
            val x = x0 + k * (dw + gap)
            c.save(); c.translate(0f, (y * fs).toFloat()); c.scale(sc.toFloat(), sc.toFloat(), x + dw / 2, cy)
            p.color = white(.85 * o)
            if (g > .01) p.setShadowLayer(((4 + 6 * g) / 40 * F * .87).toFloat(), 0f, 0f, white(min(1.0, g * .9) * o)) else p.clearShadowLayer()
            c.drawText("•", x, cy - (p.ascent() + p.descent()) / 2, p)
            c.restore()
        }
        c.restore()
    }

    fun bump() { tick.longValue++ }
    fun lineCount() = lines.size
}
