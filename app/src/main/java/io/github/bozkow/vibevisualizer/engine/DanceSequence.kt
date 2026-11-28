package io.github.bozkow.vibevisualizer.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.BlendMode
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.ensureActive

/** Prepare complete, transparent frames once off main. Playback uses one native animation across every join. */
internal object DanceSequence {
    private const val WIDTH = 512
    private const val HEIGHT = 396
    private data class Part(val frames: List<ByteArray>, val first: Bitmap, val last: Bitmap)
    private fun uint(bytes: ByteArray, at: Int, count: Int): Int {
        var value = 0
        repeat(count) { value = value or ((bytes[at+it].toInt() and 255) shl (it*8)) }
        return value
    }
    private fun put(bytes: ByteArray, at: Int, value: Int, count: Int) {
        repeat(count) { bytes[at+it] = (value ushr (it*8)).toByte() }
    }
    private fun chunk(name: String, payload: ByteArray): ByteArray {
        val result = ByteArray(8+payload.size+(payload.size and 1))
        name.toByteArray(Charsets.US_ASCII).copyInto(result)
        put(result,4,payload.size,4); payload.copyInto(result,8)
        return result
    }
    private fun container(chunks: List<ByteArray>): ByteArray {
        val payload = ByteArrayOutputStream()
        payload.write("WEBP".toByteArray(Charsets.US_ASCII))
        chunks.forEach { payload.write(it) }
        return chunk("RIFF",payload.toByteArray())
    }
    private fun header(width: Int, height: Int, animated: Boolean): ByteArray = chunk("VP8X",ByteArray(10).also {
        it[0] = (if (animated) 0x12 else 0x10).toByte()
        put(it,4,width-1,3); put(it,7,height-1,3)
    })
    private fun chunks(bytes: ByteArray): List<Pair<String,ByteArray>> {
        require(bytes.size >= 12 && String(bytes,0,4,Charsets.US_ASCII)=="RIFF" && String(bytes,8,4,Charsets.US_ASCII)=="WEBP")
        val result = ArrayList<Pair<String,ByteArray>>()
        var at = 12
        while (at+8 <= bytes.size) {
            val length = uint(bytes,at+4,4)
            require(length >= 0 && length <= bytes.size-at-8)
            result.add(String(bytes,at,4,Charsets.US_ASCII) to bytes.copyOfRange(at+8,at+8+length))
            at += 8+length+(length and 1)
        }
        return result
    }
    private fun frame(bitmap: Bitmap, duration: Int): ByteArray {
        val encoded = ByteArrayOutputStream()
        check(bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS,30,encoded))
        val pixels = chunks(encoded.toByteArray()).filter { it.first in setOf("ALPH","VP8 ","VP8L") }
        require(pixels.isNotEmpty())
        val out = ByteArrayOutputStream()
        out.write(ByteArray(16).also {
            put(it,6,bitmap.width-1,3); put(it,9,bitmap.height-1,3); put(it,12,duration,3)
            it[15] = 2 // replace the entire canvas, including transparent pixels
        })
        pixels.forEach { (type,data) -> out.write(chunk(type,data)) }
        return chunk("ANMF",out.toByteArray())
    }
    private suspend fun prepare(bytes: ByteArray): Part {
        val source = chunks(bytes)
        val dimensions = source.first { it.first=="VP8X" }.second
        val width = uint(dimensions,4,3)+1; val height = uint(dimensions,7,3)+1
        require(width in 1..2048 && height in 1..2048)
        val bitmap = Bitmap.createBitmap(WIDTH,HEIGHT,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val clear = Paint().apply { blendMode = BlendMode.CLEAR }
        val frames = ArrayList<ByteArray>()
        var first: Bitmap? = null
        var last: Bitmap? = null
        val raw = source.filter { it.first=="ANMF" }
        for ((index,item) in raw.withIndex()) {
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            val data = item.second
            require(data.size >= 16)
            val x = uint(data,0,3)*2; val y = uint(data,3,3)*2
            val w = uint(data,6,3)+1; val h = uint(data,9,3)+1
            require(x+w <= width && y+h <= height)
            val flags = data[15].toInt()
            val still = container(listOf(header(w,h,false),data.copyOfRange(16,data.size)))
            val patch = BitmapFactory.decodeByteArray(still,0,still.size) ?: error("Unreadable dance frame")
            val rect = RectF(x.toFloat()/width*WIDTH,y.toFloat()/height*HEIGHT,(x+w).toFloat()/width*WIDTH,(y+h).toFloat()/height*HEIGHT)
            if (flags and 2 != 0) canvas.drawRect(rect,clear)
            canvas.drawBitmap(patch,null,rect,paint); patch.recycle()
            // The original part 1 ends with a 1.8-second theme-switch hold. Continuous dancing skips that hold.
            val duration = uint(data,12,3).coerceAtLeast(1).let { if (index==raw.lastIndex) it.coerceAtMost(120) else it }
            frames.add(frame(bitmap,duration))
            if (first==null) first = bitmap.copy(Bitmap.Config.ARGB_8888,false)
            if (index==raw.lastIndex) last = bitmap.copy(Bitmap.Config.ARGB_8888,false)
            if (flags and 1 != 0) canvas.drawRect(rect,clear)
        }
        bitmap.recycle()
        return Part(frames,requireNotNull(first),requireNotNull(last))
    }
    suspend fun build(sources: List<ByteArray>): ByteArray {
        val parts = sources.map { prepare(it) }
        val output = ArrayList<ByteArray>()
        val blend = Bitmap.createBitmap(WIDTH,HEIGHT,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(blend)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        try {
            parts.forEachIndexed { index,part ->
                output.addAll(part.frames)
                val next = parts[(index+1)%parts.size]
                for (step in 1..3) {
                    canvas.drawColor(android.graphics.Color.TRANSPARENT,BlendMode.CLEAR)
                    paint.blendMode = BlendMode.SRC; paint.alpha = 255-step*255/4
                    canvas.drawBitmap(part.last,0f,0f,paint)
                    paint.blendMode = BlendMode.PLUS; paint.alpha = step*255/4
                    canvas.drawBitmap(next.first,0f,0f,paint)
                    output.add(frame(blend,40))
                }
            }
            return container(listOf(header(WIDTH,HEIGHT,true),chunk("ANIM",ByteArray(6)))+output)
        } finally {
            blend.recycle()
            parts.forEach { it.first.recycle(); it.last.recycle() }
        }
    }
}
