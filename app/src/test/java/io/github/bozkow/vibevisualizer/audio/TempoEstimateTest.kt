package io.github.bozkow.vibevisualizer.audio

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*
import kotlin.random.Random

class TempoEstimateTest {
    // Synthetic spectra include sustained bass, eighth-note hats, a backbeat, missing kicks,
    // amplitude changes and frame jitter. Feed the actual fixed-time history, not ideal beat intervals.
    private fun recording(bpm: Double, fps: Int = 60, gain: Float = 1f, missing: Boolean = false): Float? {
        val history = TempoHistory()
        val bins = FloatArray(1024)
        val random = Random(42)
        var t = 0.0
        while (t < 16000) {
            val beat = t*bpm/60000
            val phase = beat-floor(beat)
            val kick = if (missing && floor(beat).toInt()%7 == 4) 0.0 else exp(-phase*18)
            val hatPhase = beat*2-floor(beat*2)
            val hat = exp(-hatPhase*22)
            val snare = if (floor(beat).toInt()%2==1) exp(-phase*25) else 0.0
            for (i in bins.indices) bins[i] = gain * when (i) {
                in 2..10 -> (45 + 95*kick).toFloat()
                in 11..85 -> (10 + 70*snare).toFloat()
                in 86..426 -> (5 + 100*hat).toFloat()
                else -> 0f
            }
            history.add(t,bins,48000)
            t += 1000.0/fps * (.85+random.nextDouble()*.3)
        }
        return tempoFromNovelty(history.snapshot()!!)
    }
    @Test fun detectsBeatInsteadOfBassThresholdOrEighthNoteHats() {
        val results = listOf(60.0,75.0,90.0,100.0,120.0,128.0,150.0,180.0,200.0,210.0).map { it to recording(it) }
        assertTrue(results.toString(), results.all { (expected,actual) -> actual != null && abs(expected-actual) < 3.0 })
    }
    @Test fun independentOfFrameRateVolumeAndMissedKicks() {
        for (fps in listOf(30,60,120)) for (gain in listOf(.1f,1f,3f))
            assertEquals("$fps fps, gain $gain",128.0,recording(128.0,fps,gain,true)!!.toDouble(),3.0)
    }
    @Test fun decodedAudioThroughTheRealFftKeepsItsTempo() {
        for (bpm in listOf(75.0,128.0,180.0)) {
            val analyser = Analyser()
            val history = TempoHistory()
            val pcm = FloatArray(2048); val spectrum = FloatArray(1024)
            for (frame in 0 until 960) {
                val t = frame/60.0
                for (i in pcm.indices) {
                    val at = (t + (i-2048)/48000.0).coerceAtLeast(0.0)
                    val beat = at*bpm/60
                    val kick = exp(-(beat-floor(beat))*14)
                    val hat = exp(-(beat*2-floor(beat*2))*24)
                    pcm[i] = (.3*sin(2*PI*85*at)*kick + .1*sin(2*PI*6000*at)*hat).toFloat()
                }
                analyser.compute(pcm,.35f,spectrum)
                history.add(t*1000,spectrum,48000)
            }
            assertEquals("PCM $bpm BPM",bpm,tempoFromNovelty(history.snapshot()!!)!!.toDouble(),3.0)
        }
    }
    @Test fun silenceConstantTonesAndUnrelatedNoiseAreNotTempo() {
        val random = Random(9)
        assertNull(tempoFromNovelty(Array(3) { FloatArray(640) }))
        assertNull(tempoFromNovelty(Array(3) { FloatArray(640) { 3f } }))
        assertNull(tempoFromNovelty(Array(3) { FloatArray(640) { random.nextFloat() } }))
        val history = TempoHistory()
        for (i in 0..700) history.add(i*25.0,FloatArray(1024) { 100f },48000)
        assertNull(tempoFromNovelty(history.snapshot()!!))
    }
    @Test fun discontinuityNeedsFreshAudioAndNewTrackDoesNotCarryOldTempo() {
        val history = TempoHistory()
        for (i in 0..300) history.add(i*25.0,FloatArray(1024),48000)
        assertNotNull(history.snapshot())
        history.add(10000.0,FloatArray(1024),48000)
        assertNull(history.snapshot())
        val lock = TempoLock()
        assertNull(lock.accept(128f))
        assertEquals(128f,lock.accept(128f)!!,.1f)
        assertEquals(128f,lock.accept(190f)!!,.1f)
        assertEquals(128f,lock.accept(128f)!!,.1f)
        assertNull(TempoLock().bpm)
    }
}
