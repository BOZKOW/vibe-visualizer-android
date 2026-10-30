package io.github.bozkow.vibevisualizer.media

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import io.github.bozkow.vibevisualizer.core.Vibe
import io.github.bozkow.vibevisualizer.engine.E

/** A delay for each player app (Spicy Lyrics Mobile 0.17's player delay): for a player whose position runs early or
 *  late on every song, kept for that app (Spotify, YouTube Music, Poweramp…, or this app's own player) and added to
 *  yours, the output's and the song's. Positive shows the lyrics earlier, as the others. */
object PlayerDelays {
    private val prefs by lazy { Vibe.app.getSharedPreferences("playerdelay", Context.MODE_PRIVATE) }
    var ver by mutableIntStateOf(0); private set   // a delay changed (the timing sheet reads it again)
    private var lastKey: String? = null; private var last = 0

    /** The app playing now: Spotify (on this phone or elsewhere), another music app by its package, or your files. */
    fun key(): String? = when (E.src) {
        "spotify" -> "spotify"
        "media" -> MediaApps.pkg.takeIf { it.isNotEmpty() }?.let { "app:$it" }
        "file" -> "file"
        else -> null
    }

    /** Its name, for the timing sheet. */
    fun name(): String = when (E.src) { "spotify" -> "Spotify"; "media" -> MediaApps.appName().ifEmpty { "This music app" }; "file" -> "This app’s player"; else -> "" }

    /** The delay for the app playing now (ms); read every frame, so kept until the app changes. */
    fun ms(): Int {
        val k = key()
        if (k != lastKey) { lastKey = k; last = if (k == null) 0 else prefs.getInt(k, 0) }
        return last
    }

    fun set(v: Int) {
        val k = key() ?: return
        last = v.coerceIn(-5000, 5000); lastKey = k
        prefs.edit().apply { if (last == 0) remove(k) else putInt(k, last) }.apply()
        ver++
    }
}
