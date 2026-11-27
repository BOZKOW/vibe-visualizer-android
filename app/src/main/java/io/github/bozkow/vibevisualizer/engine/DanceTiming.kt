package io.github.bozkow.vibevisualizer.engine

import kotlin.math.roundToInt

internal const val DANCE_LOOP_BEATS = 8
internal fun danceLoopMs(bpm: Int) = DANCE_LOOP_BEATS * 60000.0 / bpm.coerceIn(30,300)

/** Eight beats per loop. Only WebP animation duration fields change; encoded pixels/alpha remain untouched. */
internal fun retimeDance(source: ByteArray, bpm: Int): ByteArray {
    require(source.size >= 12 && String(source, 0, 4, Charsets.US_ASCII) == "RIFF" && String(source, 8, 4, Charsets.US_ASCII) == "WEBP")
    fun uint(at: Int, count: Int): Int {
        var n = 0L
        for (i in 0 until count) n = n or ((source[at+i].toLong() and 255) shl (8*i))
        require(n <= Int.MAX_VALUE)
        return n.toInt()
    }
    val frames = ArrayList<Pair<Int,Int>>()
    var at = 12
    while (at + 8 <= source.size) {
        val length = uint(at+4,4)
        require(length <= source.size-at-8)
        if (String(source,at,4,Charsets.US_ASCII) == "ANMF") {
            require(length >= 16)
            frames.add(at+20 to uint(at+20,3).coerceAtLeast(1))
        }
        at += 8 + length + (length and 1)
    }
    require(frames.isNotEmpty())
    val total = frames.sumOf { it.second }.toDouble()
    val duration = danceLoopMs(bpm)
    val result = source.copyOf()
    var elapsed = 0; var emitted = 0
    for ((offset,time) in frames) {
        elapsed += time
        val end = (elapsed/total*duration).roundToInt()
        val value = (end-emitted).coerceIn(10,0xffffff)
        for (i in 0..2) result[offset+i] = (value shr (8*i)).toByte()
        emitted += value
    }
    return result
}
