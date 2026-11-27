package io.github.bozkow.vibevisualizer.engine

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import io.github.bozkow.vibevisualizer.core.Net
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

/** Creator-hosted opt-in artwork, decoded natively: no browser, audio track or bundled external artwork. */
internal object DanceAnimation {
    private const val URL = "https://cdn.jsdelivr.net/gh/guyutongxue/shigure-ui-dance-css@608001527f287176c8d4b7f3b6cc5dbf6ea61059/part1-small.webp"
    private var source: ByteArray? = null
    private val lock = Mutex()
    private val variants = LinkedHashMap<Int, AnimatedImageDrawable>()
    suspend fun load(bpm: Int): AnimatedImageDrawable? = lock.withLock {
        variants[bpm]?.let { return@withLock it }
        val bytes = source ?: runCatching { Net.fetch(URL).takeIf { it.ok }?.body }.getOrNull()?.also { source = it } ?: return@withLock null
        val image = withContext(Dispatchers.Default) {
            runCatching {
                ImageDecoder.decodeDrawable(ImageDecoder.createSource(ByteBuffer.wrap(retimeDance(bytes, bpm)))) { dec, info, _ ->
                    val max = maxOf(info.size.width, info.size.height)
                    dec.setTargetSize((info.size.width * 384 / max).coerceAtLeast(1), (info.size.height * 384 / max).coerceAtLeast(1))
                } as? AnimatedImageDrawable
            }.getOrNull()
        } ?: return@withLock null
        image.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
        variants[bpm] = image
        if (variants.size > 3) variants.remove(variants.keys.first())
        image
    }
}
