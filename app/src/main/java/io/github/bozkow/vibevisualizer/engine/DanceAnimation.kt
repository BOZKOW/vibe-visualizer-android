package io.github.bozkow.vibevisualizer.engine

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import io.github.bozkow.vibevisualizer.core.Net
import io.github.bozkow.vibevisualizer.core.Vibe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/** Creator-hosted opt-in artwork. Every part and join is prepared before native playback starts. */
internal object DanceAnimation {
    data class Clip(val image: AnimatedImageDrawable, val durationMs: Double)
    private const val BASE = "https://cdn.jsdelivr.net/gh/guyutongxue/shigure-ui-dance-css@608001527f287176c8d4b7f3b6cc5dbf6ea61059/"
    private val lock = Mutex()
    private val variants = LinkedHashMap<String,Clip>()
    private val sequences = HashMap<String,ByteArray>()
    suspend fun load(bpm: Int, selection: String): Clip? = lock.withLock {
        val mode = selection.takeIf { it in setOf("all","1","2") } ?: "all"
        val key = "$mode/$bpm"
        variants[key]?.let { return@withLock it }
        withContext(Dispatchers.Default) {
            try {
                val bytes = sequences[mode] ?: run {
                    val file = File(Vibe.app.cacheDir,"dance-sequence-v2-$mode.webp")
                    val cached = if (file.exists()) file.readBytes() else null
                    val prepared = cached ?: run {
                        val parts = if (mode=="all") listOf("1","2") else listOf(mode)
                        val sources = parts.map { part ->
                            val response = Net.fetch(BASE+"part$part.webp")
                            check(response.ok)
                            response.body
                        }
                        DanceSequence.build(sources).also { result ->
                            val temp = File(file.path+".tmp")
                            temp.writeBytes(result); temp.renameTo(file)
                        }
                    }
                    sequences[mode] = prepared
                    prepared
                }
                // Preserve the tempo scaling of the original 2.36-second excerpt across the full sequence.
                val timed = retimeDance(bytes,bpm,2360.0)
                val image = ImageDecoder.decodeDrawable(ImageDecoder.createSource(ByteBuffer.wrap(timed))) { decoder, info, _ ->
                    val max = maxOf(info.size.width,info.size.height)
                    if (max>512) decoder.setTargetSize(info.size.width*512/max,info.size.height*512/max)
                } as? AnimatedImageDrawable ?: return@withContext null
                image.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                val clip = Clip(image, danceDurationMs(timed).toDouble())
                variants[key] = clip
                if (variants.size>3) variants.remove(variants.keys.first())
                clip
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) {
                sequences.remove(mode)
                File(Vibe.app.cacheDir,"dance-sequence-v2-$mode.webp").delete()
                null
            }
        }
    }
}
