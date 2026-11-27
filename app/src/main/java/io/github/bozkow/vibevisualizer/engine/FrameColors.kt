package io.github.bozkow.vibevisualizer.engine

import android.graphics.Bitmap
import android.view.TextureView
import android.view.PixelCopy
import android.view.Surface
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import io.github.bozkow.vibevisualizer.core.S
import io.github.bozkow.vibevisualizer.core.Vibe
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Tiny asynchronous video snapshots for palette/contrast, never per-frame. Each sample reflects the current scene,
 *  including a genuinely dark cut. Palette analysis runs off main; extraction pauses when the video is hidden or still. */
internal class FrameColors(private val view: () -> TextureView?, private val moving: () -> Boolean, private val set: (VideoPalette?) -> Unit) {
    private var job: Job? = null
    val running get() = job?.isActive == true

    /** Looks again from the start (what was found stays until something new is). Not in Low performance mode. */
    fun start() {
        job?.cancel()
        if (S.low) return
        job = Vibe.scope.launch {
            val w = 48; val h = 85
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            var sampled = false
            var nextSample = io.github.bozkow.vibevisualizer.core.now() + 200.0
            while (true) {
                delay((nextSample - io.github.bozkow.vibevisualizer.core.now()).toLong().coerceAtLeast(1L))
                nextSample = io.github.bozkow.vibevisualizer.core.now() + 200.0
                // Five color samples per second; a cancelled/hidden player makes no readback requests
                if ((!moving() && sampled) || E.quiet || !E.visible || S.low || E.ambient) continue
                val f = copyFrame(bitmap) ?: continue
                // Only the tiny GPU readback runs on main; colour extraction and luminance run together off it.
                val palette = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { Visuals.barsFrom(f) } ?: continue
                set(palette)
                sampled = true
            }
        }
    }

    /** Stops looking; [clear]: and lets its colours go. */
    fun stop(clear: Boolean = true) { job?.cancel(); job = null; if (clear) set(null) }

    /** PixelCopy waits for the decoder/GPU asynchronously. TextureView.getBitmap synchronously stalls the UI thread. */
    private suspend fun copyFrame(bitmap: Bitmap): Bitmap? {
        val texture = view()?.takeIf { it.isAvailable }?.surfaceTexture ?: return null
        return suspendCancellableCoroutine { continuation ->
            val surface = Surface(texture)
            try {
                PixelCopy.request(surface, bitmap, { result ->
                    surface.release()
                    if (io.github.bozkow.vibevisualizer.BuildConfig.BUILD_TYPE == "debug")
                        android.util.Log.i("VV-palette", "Async frame copy: $result")
                    if (continuation.isActive) continuation.resume(if (result == PixelCopy.SUCCESS) bitmap else null)
                }, Handler(Looper.getMainLooper()))
            } catch (_: IllegalArgumentException) {
                surface.release()
                if (continuation.isActive) continuation.resume(null)
            }
        }
    }

}
