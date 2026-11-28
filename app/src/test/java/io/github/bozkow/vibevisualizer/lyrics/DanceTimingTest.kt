package io.github.bozkow.vibevisualizer.lyrics

import io.github.bozkow.vibevisualizer.engine.retimeDance
import org.junit.Assert.*
import org.junit.Test

class DanceTimingTest {
    private val durationOffsets = listOf(32,60,88)
    private fun clip(): ByteArray = ByteArray(96) { 0x45 }.apply {
        "RIFF".toByteArray().copyInto(this,0); "WEBP".toByteArray().copyInto(this,8)
        for ((i,start) in listOf(12,40,68).withIndex()) {
            "ANMF".toByteArray().copyInto(this,start)
            for (k in 0..3) this[start+4+k] = (20 shr (8*k)).toByte()
            val offset = durationOffsets[i]
            for (k in 0..2) this[offset+k] = ((40*(i+1)) shr (8*k)).toByte()
        }
    }
    private fun duration(data: ByteArray) = durationOffsets.sumOf { at ->
        (data[at].toInt() and 255) + ((data[at+1].toInt() and 255) shl 8) + ((data[at+2].toInt() and 255) shl 16)
    }
    @Test fun automaticCadenceFollowsFasterSubdivisionsForLowCatalogTempos() {
        assertEquals(230,io.github.bozkow.vibevisualizer.engine.dancePlaybackBpm(115,0f))
        assertEquals(256,io.github.bozkow.vibevisualizer.engine.dancePlaybackBpm(128,0f))
        assertEquals(296,io.github.bozkow.vibevisualizer.engine.dancePlaybackBpm(148,0f))
        assertEquals(168,io.github.bozkow.vibevisualizer.engine.dancePlaybackBpm(168,0f))
        assertEquals(115,io.github.bozkow.vibevisualizer.engine.dancePlaybackBpm(115,1f))
        assertEquals(230,io.github.bozkow.vibevisualizer.engine.dancePlaybackBpm(115,2f))
    }
    @Test fun animationLoopMatchesEightBeatsAtDifferentTempos() {
        for (bpm in listOf(15,30,60,90,120,160,210,250,300,600)) assertEquals(480000.0/bpm,duration(retimeDance(clip(),bpm)).toDouble(),1.0)
    }
    @Test fun changingTempoPreservesEncodedFramesAlphaAndTheDownloadedOriginal() {
        val source = clip(); val backup = source.copyOf(); val timed = retimeDance(source,160)
        assertArrayEquals(backup,source)
        val timingBytes = durationOffsets.flatMap { listOf(it,it+1,it+2) }.toSet()
        for (i in source.indices) if (i !in timingBytes) assertEquals("pixel/container byte $i",source[i],timed[i])
    }
    @Test fun fullSequencesKeepTheOriginalExcerptPlaybackRate() {
        assertEquals(8000.0,duration(retimeDance(clip(),120,120.0)).toDouble(),1.0)
    }
    @Test(expected = IllegalArgumentException::class) fun corruptAnimationIsRejected() { retimeDance(ByteArray(16),120) }
}
