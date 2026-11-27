package io.github.bozkow.vibevisualizer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import io.github.bozkow.vibevisualizer.core.S
import io.github.bozkow.vibevisualizer.engine.E
import io.github.bozkow.vibevisualizer.engine.danceLoopMs
import io.github.bozkow.vibevisualizer.engine.DanceAnimation
import io.github.bozkow.vibevisualizer.engine.SongTempo
import io.github.bozkow.vibevisualizer.engine.Lyr
import io.github.bozkow.vibevisualizer.engine.tempoKey
import io.github.bozkow.vibevisualizer.lyrics.SongMeta
import io.github.bozkow.vibevisualizer.spotify.Spotify
import io.github.bozkow.vibevisualizer.core.Net
import io.github.bozkow.vibevisualizer.core.now
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

internal fun danceEnabled() = E.wide && S.b("xDance") && !S.low && !E.reduceMotion() && !E.inPip && !E.ambient

internal fun currentDanceSong(): SongMeta = sequenceOf(Lyr.curSong(), Spotify.meta.takeIf { E.src == "spotify" }).filterNotNull()
    .firstOrNull { it.title == E.title && it.artist == E.artist }
    ?: SongMeta(title = E.title, artist = E.artist, album = E.album, duration = E.curDur())

private object DancePace {
    private val prefs by lazy { io.github.bozkow.vibevisualizer.core.Vibe.app.getSharedPreferences("dance-pace", android.content.Context.MODE_PRIVATE) }
    var revision by mutableIntStateOf(0)
    private fun key(m: SongMeta) = io.github.bozkow.vibevisualizer.lyrics.Match.key(m.title) + "|" + io.github.bozkow.vibevisualizer.lyrics.Match.key(io.github.bozkow.vibevisualizer.lyrics.firstArtist(m.artist))
    fun get(m: SongMeta): Float {
        revision
        return prefs.getFloat(key(m), 1f).takeIf { it == .5f || it == 1f || it == 2f } ?: 1f
    }
    fun set(m: SongMeta, value: Float) {
        if (value == 1f) prefs.edit().remove(key(m)).apply() else prefs.edit().putFloat(key(m), value).apply()
        revision++
    }
}

@Composable
internal fun DanceSpeedSettings() {
    val meta = currentDanceSong()
    if (E.src == "none" || meta.title.isBlank()) { Hint("Play a song to set its dance speed."); return }
    val pace = DancePace.get(meta)
    Hint("Dance speed · " + meta.title)
    Buttons(*listOf(.5f to "½×", 1f to "1×", 2f to "2×").map { (value, label) ->
        (if (pace == value) "✓ $label" else label) to { DancePace.set(meta, value) }
    }.toTypedArray())
}

private object DancePresence {
    var track by mutableIntStateOf(-1)
    var matched by mutableStateOf(false)
}

@Composable
internal fun danceProgress(): Float {
    val shown = danceEnabled() && DancePresence.track == E.trackVer && DancePresence.matched
    return androidx.compose.animation.core.animateFloatAsState(
        if (shown) 1f else 0f, androidx.compose.animation.core.tween(if (S.low || E.reduceMotion()) 0 else 450), label = "danceSpace").value
}

@Composable
fun BarsView(modifier: Modifier = Modifier) {
    SpectrumBars(if (danceEnabled()) modifier.padding(end = 88.dp * danceProgress()) else modifier)
}

@Composable
internal fun DanceCorner(modifier: Modifier) {
    val track = E.trackVer
    val meta = currentDanceSong()
    val key = tempoKey(meta)
    var bpm by remember(track) { mutableStateOf<Int?>(null) }
    val visible = E.playingState && E.visible && !E.quiet && E.settingsPage == null && E.sheet == null
    val network = Net.online.value
    val canLookup = E.visible && !E.quiet && E.settingsPage == null
    LaunchedEffect(track, key, network, canLookup) {
        if (!canLookup) return@LaunchedEffect
        delay(350)   // don't query songs that are only skipped past
        // Recording details can arrive again during the same song. Hold its confirmed pace while
        // refining the lookup so a metadata refresh cannot interrupt the animation.
        SongTempo.find(meta, network)?.roundToInt()?.coerceIn(30,300)?.let { bpm = it }
    }
    DisposableEffect(Unit) { onDispose { DancePresence.matched = false } }
    val progress = danceProgress()
    val pace = DancePace.get(meta)
    val prepared by produceState<Pair<Int, android.graphics.drawable.AnimatedImageDrawable>?>(null, bpm, pace, E.quiet, E.visible) {
        val speed = bpm?.let { (it * pace).roundToInt().coerceIn(15,600) } ?: return@produceState
        if (E.visible && !E.quiet) value = DanceAnimation.load(speed)?.let { speed to it }
    }
    var image by remember { mutableStateOf<android.graphics.drawable.AnimatedImageDrawable?>(null) }
    var imageBpm by remember { mutableIntStateOf(120) }
    var startedAt by remember { mutableDoubleStateOf(0.0) }
    SideEffect { DancePresence.track = track; DancePresence.matched = bpm != null && image != null }
    val active = visible && progress > .001f
    LaunchedEffect(prepared, visible, track) {
        val (speed,next) = prepared ?: return@LaunchedEffect
        if (image != null && active && image !== next) {
            val cycle = danceLoopMs(imageBpm)
            delay((cycle - ((now()-startedAt) % cycle)).toLong().coerceAtLeast(1))
        }
        image = next; imageBpm = speed
    }
    val drawable = image ?: return
    DisposableEffect(drawable, active) {
        if (active) { startedAt = now(); drawable.start() } else drawable.stop()
        onDispose { drawable.stop() }
    }
    Canvas(modifier.graphicsLayer { translationX = (size.width + 24.dp.toPx()) * (1f-progress) }.clipToBounds()) {
        // Union of non-transparent pixels across ALL frames of the pinned 932×720 source.
        // Position the visible figure, not the large transparent animation canvas, next to the bezel.
        val scale = minOf(size.width / (416f/932*drawable.intrinsicWidth), size.height / (580f/720*drawable.intrinsicHeight))
        val w = drawable.intrinsicWidth * scale; val h = drawable.intrinsicHeight * scale
        val left = size.width - w * (686f/932)
        val top = size.height - h * (679f/720)
        drawable.setBounds(left.toInt(), top.toInt(), (left+w).toInt(), (top+h).toInt())
        drawable.draw(drawContext.canvas.nativeCanvas)
    }
}
