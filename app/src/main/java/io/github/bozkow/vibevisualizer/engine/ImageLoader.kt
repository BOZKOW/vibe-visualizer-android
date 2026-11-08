package io.github.bozkow.vibevisualizer.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.net.Uri
import android.util.LruCache
import io.github.bozkow.vibevisualizer.core.Net
import io.github.bozkow.vibevisualizer.core.Vibe
import java.nio.ByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Cover art, artist photos, avatars: downloaded once (OkHttp's disk cache), decoded small enough for the screen. */
object Images {
    private val mem = object : LruCache<String, Bitmap>(48 * 1024 * 1024) { override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount }

    fun cached(url: String): Bitmap? = mem.get(url)

    /** Why the last picture didn't load (for the debug info). */
    var lastError = ""; private set

    suspend fun load(url: String, max: Int = 1000, fresh: Boolean = false): Bitmap? {
        if (url.isEmpty()) { lastError = "no address"; return null }
        if (!fresh) mem.get("$url@$max")?.let { return it }
        val bytes: ByteArray = if (url.startsWith("content:") || url.startsWith("file:") || url.startsWith("android.resource:"))
            withContext(Dispatchers.IO) { runCatching { Vibe.app.contentResolver.openInputStream(Uri.parse(url))?.use { it.readBytes() } }.getOrNull() } ?: run { lastError = "couldn’t open it"; return null }
        else try {
            val r = Net.fetch(url, noCache = fresh)
            if (!r.ok) { lastError = "HTTP ${r.code}"; return null }
            r.body
        } catch (e: Exception) { lastError = "not reached (" + (e.message ?: e.javaClass.simpleName) + ")"; return null }
        val bm = decode(bytes, max) ?: run { lastError = "not a picture it can read (${bytes.size} bytes)"; return null }
        mem.put("$url@$max", bm)
        return bm
    }

    suspend fun decode(bytes: ByteArray, max: Int = 1000): Bitmap? = withContext(Dispatchers.Default) {
        runCatching {
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
            var s = 1; while (o.outWidth / (s * 2) >= max && o.outHeight / (s * 2) >= max) s *= 2
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = s; inPreferredConfig = Bitmap.Config.ARGB_8888 })
                // on to the graphics card now, in the background: else it went there in the first frame that drew it, and the
                // song change stuttered
                ?.also { it.prepareToDraw() }
        }.getOrNull()
    }

    private val moving = LruCache<String, AnimatedImageDrawable>(4)   // each keeps its file in memory: at most two show at once
    private val stills = LruCache<String, Boolean>(64)                // looked at, and they don't move

    fun cachedMoving(url: String, max: Int): AnimatedImageDrawable? = moving.get("$url@$max")

    /** A picture's moving version if it has one (GIF, animated WebP, whatever the address says), already running; null for
     *  a still one. Android decodes and draws its frames off the main thread, and only while it's being drawn. */
    suspend fun loadMoving(url: String, max: Int): AnimatedImageDrawable? {
        val key = "$url@$max"
        moving.get(key)?.let { return it }
        if (stills.get(key) != null) return null
        val r = try { Net.fetch(movingUrl(url)) } catch (e: Exception) { return null }
        if (!r.ok) return null
        val d = withContext(Dispatchers.Default) {
            runCatching {
                ImageDecoder.decodeDrawable(ImageDecoder.createSource(ByteBuffer.wrap(r.body))) { dec, info, _ ->
                    val w = info.size.width; val h = info.size.height; val big = maxOf(w, h)
                    if (big > max) dec.setTargetSize((w * max / big).coerceAtLeast(1), (h * max / big).coerceAtLeast(1))
                } as? AnimatedImageDrawable
            }.getOrNull()
        }
        if (d == null) { stills.put(key, true); return null }
        d.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
        d.start()
        moving.put(key, d)
        return d
    }
}

/** Discord starts the name of a picture that can move with a_, and serves it still unless it's asked for the GIF. */
fun movingUrl(url: String): String {
    val m = Regex("""^(https://cdn\.discordapp\.com/.*/a_[^/?]+)\.(?:png|jpe?g|webp)(\?.*)?$""").find(url) ?: return url
    return m.groupValues[1] + ".gif" + m.groupValues[2]
}
