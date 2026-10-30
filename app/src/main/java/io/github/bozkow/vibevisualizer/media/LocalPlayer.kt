package io.github.bozkow.vibevisualizer.media

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import io.github.bozkow.vibevisualizer.audio.FileTap
import io.github.bozkow.vibevisualizer.core.S
import io.github.bozkow.vibevisualizer.core.Vibe
import io.github.bozkow.vibevisualizer.engine.E
import io.github.bozkow.vibevisualizer.engine.Files
import kotlinx.coroutines.launch
import kotlin.math.pow

/** Playing your own files (the player lives in [PlaybackService]; everything runs in one process). */
object Local {
    private var player: ExoPlayer? = null
    private var controller: MediaController? = null
    private var pending: List<Uri>? = null
    private var names = HashMap<String, String>()
    private var lastDur = 0.0
    private var curUri: String? = null
    var muted = false
        set(v) { field = v; applyVolume() }

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) { E.onFileState() }
        override fun onPlaybackStateChanged(state: Int) { E.onFileState() }
        override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) FileTap.nextItem(lastDur)
            val u = item?.localConfiguration?.uri ?: return
            if (u.toString() == curUri && reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) return   // only its tags changed
            curUri = u.toString()
            E.onFileItem(u, names[u.toString()] ?: Files.name(u))
        }
        override fun onPlayerError(error: PlaybackException) { E.toast("This song can’t be played (its format may not be supported)", "warn") }
    }

    fun attach(p: ExoPlayer) {
        player = p; p.addListener(listener); applyVolume()
        pending?.let { pending = null; start(it) }
    }

    fun detach() { player?.removeListener(listener); player = null; controller = null }

    /** Starts the playback service (if it isn't running yet) and plays the files, in the order given. */
    fun open(uris: List<Uri>) {
        uris.forEach { u -> runCatching { Vibe.app.contentResolver.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
        if (player != null) { start(uris); return }
        pending = uris
        if (controller == null) {
            val tok = SessionToken(Vibe.app, ComponentName(Vibe.app, PlaybackService::class.java))
            val f = MediaController.Builder(Vibe.app, tok).buildAsync()
            f.addListener({ controller = runCatching { f.get() }.getOrNull() }, { Vibe.scope.launch { it.run() } })
        }
    }

    private fun start(uris: List<Uri>) {
        val p = player ?: return
        names.clear(); curUri = null
        val items = uris.map { u -> val n = Files.name(u); names[u.toString()] = n
            MediaItem.Builder().setUri(u).setMediaId(u.toString()).setMediaMetadata(MediaMetadata.Builder().setTitle(Files.clean(n)).build()).build() }
        p.setMediaItems(items); p.prepare(); p.play()
    }

    /** The song's tags and cover go to the notification and the lock screen. */
    fun setMeta(title: String, artist: String, picture: ByteArray?) {
        val p = player ?: return
        val i = p.currentMediaItemIndex; val it = p.currentMediaItem ?: return
        val md = MediaMetadata.Builder().setTitle(title).setArtist(artist).apply { if (picture != null) setArtworkData(picture, MediaMetadata.PICTURE_TYPE_FRONT_COVER) }.build()
        if (i >= 0 && i < p.mediaItemCount) p.replaceMediaItem(i, it.buildUpon().setMediaMetadata(md).build())
    }

    fun playing() = player?.isPlaying == true
    /** "Song 2 of 5" while a queue plays, nothing for one song. */
    fun position(): String { val p = player ?: return ""; val n = p.mediaItemCount; return if (n > 1) "Song ${p.currentMediaItemIndex + 1} of $n" else "" }
    fun loaded() = (player?.mediaItemCount ?: 0) > 0
    // the player's position only moves when it reports (about 4 times a second), so the lyrics and the bars stepped at
    // 4 fps: between reports it runs on from the last one with the clock, and a report that agrees doesn't move it back
    private var rawPos = Long.MIN_VALUE; private var rawAt = 0.0; private var lastEst = 0.0
    fun positionMs(): Double {
        val p = player ?: return 0.0
        val raw = p.currentPosition
        val now = android.os.SystemClock.elapsedRealtimeNanos() / 1e6
        if (raw != rawPos) { rawPos = raw; rawAt = now }
        if (!p.isPlaying) { lastEst = raw.toDouble(); return lastEst }
        var est = raw + (now - rawAt) * p.playbackParameters.speed
        val d = durationMs(); if (d > 0) est = minOf(est, d)
        // no small steps back (a report landing a little behind the guess); a seek or a new song still jumps
        if (est < lastEst && lastEst - est < 150) est = lastEst
        lastEst = est
        return est
    }
    fun durationMs(): Double = player?.duration?.takeIf { it > 0 }?.toDouble() ?: 0.0
    fun hasNext() = player?.hasNextMediaItem() == true
    /** The file that plays next (in shuffle and repeat too), and its name. */
    fun nextItem(): Pair<Uri, String>? {
        val p = player ?: return null
        val i = p.nextMediaItemIndex; if (i == androidx.media3.common.C.INDEX_UNSET) return null
        val u = p.getMediaItemAt(i).localConfiguration?.uri ?: return null
        return u to (names[u.toString()] ?: Files.name(u))
    }

    fun toggle() {
        val p = player ?: return
        if (p.isPlaying) p.pause() else { if (p.playbackState == Player.STATE_ENDED) p.seekTo(0); p.play() }
    }
    fun pause() { player?.pause() }
    fun seek(ms: Long) { player?.seekTo(ms) }
    fun previous() { val p = player ?: return; if (p.currentPosition > 3000 || !p.hasPreviousMediaItem()) p.seekTo(0) else p.seekToPreviousMediaItem() }
    fun next() { player?.let { if (it.hasNextMediaItem()) it.seekToNextMediaItem() } }
    fun applyVolume() { player?.volume = if (muted) 0f else (S.i("volume") / 100.0).pow(2).toFloat() }
    fun tickSession() { lastDur = durationMs() }
}
