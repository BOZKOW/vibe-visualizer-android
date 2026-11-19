package io.github.bozkow.vibevisualizer

import android.graphics.Bitmap
import android.util.Log
import io.github.bozkow.vibevisualizer.core.Vibe
import io.github.bozkow.vibevisualizer.engine.Background
import io.github.bozkow.vibevisualizer.engine.E
import io.github.bozkow.vibevisualizer.engine.Visuals
import io.github.bozkow.vibevisualizer.core.S
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/** Exercise a real suspended image decode, then let player artwork win before it completes. No playback or network. */
object CoverRecoveryProbe {
    fun run() = Vibe.scope.launch {
        val tok = E.coverTok
        val old = Visuals.cover; val oldStatus = E.status
        val loaded = E::class.java.getDeclaredField("loadedCoverTok").apply { isAccessible = true }
        val oldLoaded = loaded.getInt(E)
        val file = java.io.File.createTempFile("cover-race-", ".png", Vibe.app.cacheDir)
        try {
            val fallback = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.RED) }
            file.outputStream().use { fallback.compress(Bitmap.CompressFormat.PNG, 100, it) }
            var nativeArrived = false
            val pending = async(start = CoroutineStart.UNDISPATCHED) {
                E.setCover(file.toURI().toString(), "Fixture fallback", tok, canApply = { !nativeArrived })
            }
            check(!pending.isCompleted) { "fixture did not suspend the real image load" }
            val native = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.GRAY) }
            nativeArrived = true
            check(E.setCoverBitmap(native, "Fixture native artwork", tok, quiet = true))
            check(!pending.await() && Visuals.cover === native) { "late fallback replaced the player's artwork" }
            Log.i("VV-cover", "PASS: native monochrome bitmap retained; late fallback decode cannot overwrite it")
        } catch (e: Exception) {
            Log.e("VV-cover", "FAIL: ${e.message}", e)
        } finally {
            file.delete()
            if (E.coverTok == tok) {
                Visuals.setCover(old, quiet = true); E.status = oldStatus; loaded.setInt(E, oldLoaded)
                if (S.bgStyle != "dynamic") Background.applyStill()
            }
        }
    }
}
