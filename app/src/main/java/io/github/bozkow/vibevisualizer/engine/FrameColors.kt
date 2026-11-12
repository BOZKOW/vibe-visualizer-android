package io.github.bozkow.vibevisualizer.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.TextureView
import io.github.bozkow.vibevisualizer.core.S
import io.github.bozkow.vibevisualizer.core.Vibe
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Colours from a video that's playing (the Canvas, an animated cover) for the bars and the app's accent. A small frame is
 *  copied back now and then, never all the time: copying from the GPU holds up the frame being drawn (0.35.0 did it 12 times
 *  a second, and the lyrics stuttered). The first looks come quickly (many videos fade in from black, and black frames don't
 *  count), then one every 5 s while it moves, so the bars follow a video whose colours change; the last three lit frames
 *  count together. [set] gets them all side by side, or null for none (the cover's colours again). */
class FrameColors(private val view: () -> TextureView?, private val moving: () -> Boolean, private val set: (Bitmap?) -> Unit) {
    private var job: Job? = null
    val running get() = job?.isActive == true

    /** Looks again from the start (what was found stays until something new is). Not in Low performance mode. */
    fun start() {
        job?.cancel()
        if (S.low) return
        job = Vibe.scope.launch {
            val w = 48; val h = 85; val frames = ArrayDeque<Bitmap>()
            var n = 0
            while (true) {
                // quick to take the video's colours (a quarter second in, then twice more as it fades in from black), then
                // every 1.5 s while it moves: the bars follow its cuts (every 5 s, three frames mixed, they lagged and blended)
                delay(when (n++) { 0 -> 250L; 1 -> 500L; 2 -> 750L; else -> 1500L })
                if (n > 3 && !moving()) continue   // held still (paused, or not on screen): nothing new to see
                val f = view()?.takeIf { it.isAvailable }?.getBitmap(w, h) ?: continue
                if (!litEnough(f)) continue
                frames.addLast(f); while (frames.size > 1) frames.removeFirst()   // (what's on screen now, not mixed with before)
                val all = Bitmap.createBitmap(w * frames.size, h, Bitmap.Config.ARGB_8888)
                Canvas(all).let { c -> frames.forEachIndexed { i, b -> c.drawBitmap(b, (i * w).toFloat(), 0f, null) } }
                set(all)
            }
        }
    }

    /** Stops looking; [clear]: and lets its colours go. */
    fun stop(clear: Boolean = true) { job?.cancel(); job = null; if (clear) set(null) }

    private fun litEnough(b: Bitmap): Boolean {
        val px = IntArray(b.width * b.height); b.getPixels(px, 0, b.width, 0, 0, b.width, b.height)
        return px.sumOf { p -> ((p shr 16 and 255) + (p shr 8 and 255) + (p and 255)).toLong() } / (3.0 * 255 * px.size) > .04
    }
}
