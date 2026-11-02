package io.github.bozkow.vibevisualizer.engine

import io.github.bozkow.vibevisualizer.lyrics.Line
import io.github.bozkow.vibevisualizer.lyrics.Word
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random
import kotlin.math.abs

/** The desync check's math, on made-up singing: words at uneven times, as sung, and heard [shift] s after the lyrics say. */
class DesyncScoreTest {
    private val rnd = Random(7)
    private val lines: List<Line> = run {
        var t = 2.0
        List(16) {
            val ws = ArrayList<Word>()
            repeat(5) { val len = .2 + rnd.nextDouble() * .5; ws.add(Word(t, t + len, "w", true)); t += len + .05 + rnd.nextDouble() * .2 }
            t += 1.0 + rnd.nextDouble() * 2
            Line(ws[0].t, ws.last().end, "w w w w w", words = ws)
        }
    }
    private val ts = DoubleArray(1200) { it * .05 }

    private fun heard(shift: Double): Pair<FloatArray, FloatArray> {
        val starts = lines.flatMap { l -> l.words!!.map { it.t + shift } }
        val spans = lines.flatMap { l -> l.words!!.map { (it.t + shift) to (it.end!! + shift) } }
        val fl = FloatArray(ts.size) { i -> if (starts.any { abs(it - ts[i]) < .03 }) 1f else 0f }
        val v = FloatArray(ts.size) { i -> if (spans.any { ts[i] >= it.first && ts[i] < it.second }) 1f else 0f }
        return fl to v
    }

    @Test fun inStepSaysSo() {
        val (fl, v) = heard(0.0)
        assertEquals(DesyncScore.IN_SYNC, DesyncScore.score(ts, fl, v, lines, 0.0).kind)
    }

    @Test fun aSecondLateIsFound() {
        val (fl, v) = heard(1.0)
        val r = DesyncScore.score(ts, fl, v, lines, 0.0)
        assertEquals(DesyncScore.OFF, r.kind)
        assertTrue("found ${r.off}", abs(r.off - 1.0) <= .1)
    }

    @Test fun theSoundsOwnDelayIsNotADesync() {
        val (fl, v) = heard(.05)
        assertEquals(DesyncScore.IN_SYNC, DesyncScore.score(ts, fl, v, lines, .05).kind)
    }

    @Test fun silenceIsNoMatch() {
        assertEquals(DesyncScore.NO_MATCH, DesyncScore.score(ts, FloatArray(ts.size), FloatArray(ts.size), lines, 0.0).kind)
    }

    @Test fun tooFewWordsWaits() {
        assertEquals(DesyncScore.TOO_LITTLE, DesyncScore.score(ts, FloatArray(ts.size), FloatArray(ts.size), lines.take(1), 0.0).kind)
        assertEquals(DesyncScore.TOO_LITTLE, DesyncScore.score(DoubleArray(0), FloatArray(0), FloatArray(0), lines, 0.0).kind)
    }
}
