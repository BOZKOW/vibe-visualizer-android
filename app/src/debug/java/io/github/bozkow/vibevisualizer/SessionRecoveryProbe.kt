package io.github.bozkow.vibevisualizer

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.SystemClock
import android.util.Log
import io.github.bozkow.vibevisualizer.core.Vibe
import io.github.bozkow.vibevisualizer.spotify.SessionFollower
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Device-only regression check: drop callbacks, then recover a surviving token's song, state and late artwork.
 * No audio is played and the real music app is never controlled. The temporary session is always released. */
object SessionRecoveryProbe {
    fun run(ctx: Context) = Vibe.scope.launch(Dispatchers.Main) {
        val session = MediaSession(ctx, "Vibe recovery probe")
        val follower = SessionFollower { it == ctx.packageName }
        var tracks = 0; var metadata = 0
        follower.onTrack = { tracks++ }; follower.onMeta = { metadata++ }
        fun song(artist: String, art: Bitmap? = null) = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, "Recovery probe")
            .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, 60_000)
            .apply { if (art != null) putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, art) }.build()
        fun dropCallbacks() {
            val c = SessionFollower::class.java.getDeclaredField("ctrl").apply { isAccessible = true }.get(follower) as MediaController
            val cb = SessionFollower::class.java.getDeclaredField("cb").apply { isAccessible = true }.get(follower) as MediaController.Callback
            c.unregisterCallback(cb)
        }
        try {
            session.setMetadata(song("First"))
            session.setPlaybackState(PlaybackState.Builder().setState(PlaybackState.STATE_PAUSED, 0, 0f).build())
            session.isActive = true
            delay(200)
            follower.refresh(ctx)
            check(follower.connected.value && tracks == 1) { "initial attachment did not publish the song" }
            dropCallbacks()
            session.setMetadata(song("Second")) // same title, different artist is a new song
            session.setPlaybackState(PlaybackState.Builder().setState(PlaybackState.STATE_PLAYING, 1200, 1f, SystemClock.elapsedRealtime()).build())
            delay(200)
            follower.refresh(ctx)
            check(follower.artist == "Second" && follower.playing && tracks == 2) { "stale same-token session was not recovered" }
            dropCallbacks()
            val art = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.RED) }
            session.setMetadata(song("Second", art))
            delay(200)
            val oldMeta = metadata
            follower.refresh(ctx, rebind = false)
            check(follower.cover()?.getPixel(0, 0) == android.graphics.Color.RED && metadata > oldMeta && tracks == 2) { "late artwork was lost or restarted the track" }
            follower.refresh(ctx, rebind = false)
            check(tracks == 2) { "unchanged snapshots restarted the track" }
            dropCallbacks()
            val atSeek = SystemClock.elapsedRealtime()
            val oldStateEvents = tracks
            session.setPlaybackState(PlaybackState.Builder().setState(PlaybackState.STATE_PLAYING, 9_000, 1f, atSeek).build())
            delay(100)
            follower.pollPosition()
            check(kotlin.math.abs(follower.positionMs()!! - (9_000 + SystemClock.elapsedRealtime() - atSeek)) < 100) { "missed seek did not follow the live Android position" }
            session.setPlaybackState(PlaybackState.Builder().setState(PlaybackState.STATE_PAUSED, 9_500, 0f).build())
            delay(100); follower.pollPosition()
            check(!follower.playing && follower.positionMs() == 9_500.0) { "missed pause did not stop the clock" }
            val atResume = SystemClock.elapsedRealtime()
            session.setPlaybackState(PlaybackState.Builder().setState(PlaybackState.STATE_PLAYING, 12_500, 1.5f, atResume).build())
            delay(100); follower.pollPosition()
            check(follower.playing && kotlin.math.abs(follower.positionMs()!! - (12_500 + (SystemClock.elapsedRealtime() - atResume) * 1.5)) < 100) { "missed resume/speed update did not follow Android" }
            check(tracks == oldStateEvents) { "position polling restarted the song" }
            Log.i("VV-position", "PASS: dropped seek, pause, resume and speed callbacks recovered from Android; no track restart")
            session.setMetadata(song("Third", art))
            session.setPlaybackState(PlaybackState.Builder().setState(PlaybackState.STATE_PLAYING, 500, 1f, SystemClock.elapsedRealtime()).build())
            delay(100); follower.pollPosition()
            check(follower.artist == "Third" && tracks == oldStateEvents + 1 && follower.positionMs()!! < 1_000) { "missed song-change metadata was not recovered with its position" }
            Log.i("VV-position", "PASS: backwards song change recovered metadata and position together")
            val beforeIds = tracks
            session.setMetadata(MediaMetadata.Builder(song("Third", art)).putString(MediaMetadata.METADATA_KEY_MEDIA_ID, "spotify:track:" + "A".repeat(22)).build())
            delay(100); follower.refresh(ctx, rebind = false)
            check(tracks == beforeIds) { "late ID restarted the same recording" }
            session.setMetadata(MediaMetadata.Builder(song("Third", art)).putString(MediaMetadata.METADATA_KEY_MEDIA_ID, "spotify:track:" + "B".repeat(22)).build())
            delay(100); follower.refresh(ctx, rebind = false)
            check(tracks == beforeIds + 1 && follower.spotifyId == "B".repeat(22)) { "different Spotify IDs with the same names were merged" }
            Log.i("VV-recording", "PASS: late ID refines current song; different known ID starts a new recording")
            val current = android.media.MediaDescription.Builder().setMediaId("probe-current").setTitle("Current fixture").build()
            val queued = android.media.MediaDescription.Builder().setMediaId("probe-next").setTitle("Next fixture").setSubtitle("Fixture artist").setIconBitmap(art).build()
            session.setQueue(listOf(MediaSession.QueueItem(current, 1), MediaSession.QueueItem(queued, 2)))
            session.setPlaybackState(PlaybackState.Builder().setState(PlaybackState.STATE_PAUSED, 1200, 0f).setActiveQueueItemId(1).build())
            delay(200)
            val next = follower.nextInQueue()
            check(next?.title == "Next fixture" && next.cover.startsWith("vv-artwork:")) { "queue artwork was discarded" }
            check(io.github.bozkow.vibevisualizer.engine.Images.cached("${next.cover}@128")?.getPixel(0, 0) == android.graphics.Color.RED) { "queue thumbnail was not retained" }
            val uriOnly = android.media.MediaDescription.Builder().setTitle("URI fixture").setIconUri(android.net.Uri.parse("spotify:image:fixture-image")).build()
            session.setQueue(listOf(MediaSession.QueueItem(current, 1), MediaSession.QueueItem(uriOnly, 2)))
            delay(200)
            check(follower.nextInQueue()?.cover == "https://i.scdn.co/image/fixture-image") { "Spotify image URI was lost" }
            Log.i("VV-queue", "PASS: queue bitmap retained and Spotify artwork URI preserved")
            Log.i("VV-recovery", "PASS: initial song, missed callbacks, artist change, playback state, late artwork, stable track")
        } catch (e: Exception) {
            Log.e("VV-recovery", "FAIL: ${e.message}", e)
        } finally {
            follower.stop(); session.isActive = false; session.release()
        }
    }
}
