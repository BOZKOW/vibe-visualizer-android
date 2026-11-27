package io.github.bozkow.vibevisualizer.audio

import org.junit.Assert.*
import org.junit.Test

class TempoEstimateTest {
    @Test fun slowMediumAndFastRecordingsKeepTheirActualTempo() {
        for (bpm in listOf(60f,90f,120f,180f,200f))
            assertEquals(bpm,tempoFromOnsets(List(8) { 60000.0/bpm })!!,.01f)
    }
    @Test fun oneMissedOrExtraBeatDoesNotChangeTheEstimate() {
        assertEquals(120f,tempoFromOnsets(listOf(500.0,497.0,503.0,500.0,500.0,1000.0,250.0,500.0))!!,.01f)
    }
    @Test fun weakOrInvalidAudioDoesNotInventATempo() {
        assertNull(tempoFromOnsets(listOf(500.0,500.0)))
        assertNull(tempoFromOnsets(List(8) { Double.NaN }))
        assertNull(tempoFromOnsets(List(8) { 0.0 }))
    }
}
