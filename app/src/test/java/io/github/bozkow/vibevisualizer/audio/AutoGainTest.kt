package io.github.bozkow.vibevisualizer.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoGainTest {
    private fun bars(v: Float, n: Int = 64) = FloatArray(n) { v }
    private fun run(g: AutoGain, raw: FloatArray, ms: Int): Float { var x = 0f; repeat(ms / 16) { x = g.gain(raw, raw.size, 16.0) }; return x }

    @Test fun loudMusicComesToTheTarget() {
        val g = AutoGain(); val gain = run(g, bars(.5f), 3000)
        assertEquals(AutoGain.TARGET, .5f * gain, .02f)
    }

    @Test fun quietMusicIsTurnedUpAtMostFourTimes() {
        val g = AutoGain(); val gain = run(g, bars(.05f), 30000)
        assertEquals(AutoGain.MAX, gain, .01f)
    }

    @Test fun oneSpikeDoesNotTurnEverythingDown() {
        val g = AutoGain(); run(g, bars(.3f), 5000)
        val before = g.gain(bars(.3f), 64, 16.0)
        val spike = bars(.3f).also { it[10] = 1.5f }
        repeat(6) { g.gain(spike, 64, 16.0) }   // ~100 ms
        val after = g.gain(bars(.3f), 64, 16.0)
        assertTrue("gain $before -> $after", after > before * .9f)
    }

    @Test fun silenceHoldsTheLevel() {
        val g = AutoGain(); val loud = run(g, bars(.5f), 3000)
        val afterSilence = run(g, bars(0f), 10000)
        assertEquals(loud, afterSilence, .01f)
    }

    @Test fun aLouderPartTakesOverInUnderASecond() {
        val g = AutoGain(); run(g, bars(.2f), 5000)
        run(g, bars(.6f), 900)
        assertTrue(g.level > .5f)
    }
}
