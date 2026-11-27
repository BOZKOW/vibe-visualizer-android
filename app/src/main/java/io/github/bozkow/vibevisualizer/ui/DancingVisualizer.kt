package io.github.bozkow.vibevisualizer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import io.github.bozkow.vibevisualizer.core.S
import io.github.bozkow.vibevisualizer.engine.E
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

@Composable
fun BarsView(modifier: Modifier = Modifier) {
    SpectrumBars(if (danceEnabled()) modifier.padding(end = 88.dp) else modifier)
}

@Composable
internal fun DanceCorner(modifier: Modifier) {
    val track = E.trackVer
    // Lyrics metadata can still be the previous song during a change; never query it for this title.
    val meta = sequenceOf(Lyr.curSong(), Spotify.meta.takeIf { E.src == "spotify" }).filterNotNull()
        .firstOrNull { it.title == E.title && it.artist == E.artist }
        ?: SongMeta(title = E.title, artist = E.artist, album = E.album, duration = E.curDur())
    val key = tempoKey(meta)
    var bpm by remember(track, key) { mutableStateOf<Int?>(null) }
    val visible = E.playingState && E.visible && !E.quiet && E.settingsPage == null && E.sheet == null
    val network = Net.online.value
    val canLookup = E.visible && !E.quiet && E.settingsPage == null
    LaunchedEffect(track, key, network, canLookup) {
        if (!canLookup) return@LaunchedEffect
        delay(350)   // don't query songs that are only skipped past
        bpm = SongTempo.find(meta, network)?.roundToInt()?.coerceIn(30,300)
    }
    val prepared by produceState<Pair<Int, android.graphics.drawable.AnimatedImageDrawable>?>(null, bpm, E.quiet, E.visible) {
        val speed = bpm ?: 120   // a still preview while listening; never animate at a guessed tempo
        if (E.visible && !E.quiet) value = DanceAnimation.load(speed)?.let { speed to it }
    }
    var image by remember(track) { mutableStateOf<android.graphics.drawable.AnimatedImageDrawable?>(null) }
    var imageBpm by remember(track) { mutableIntStateOf(120) }
    var startedAt by remember(track) { mutableDoubleStateOf(0.0) }
    var lockedImage by remember(track) { mutableStateOf(false) }
    val active = visible && bpm != null && lockedImage
    LaunchedEffect(prepared, visible, track) {
        val (speed,next) = prepared ?: return@LaunchedEffect
        if (image != null && active && image !== next) {
            val cycle = 240000.0 / imageBpm
            delay((cycle - ((now()-startedAt) % cycle)).toLong().coerceAtLeast(1))
        }
        image = next; imageBpm = speed
        lockedImage = bpm != null && speed == bpm
    }
    val drawable = image ?: return
    DisposableEffect(drawable, active) {
        if (active) { startedAt = now(); drawable.start() } else drawable.stop()
        onDispose { drawable.stop() }
    }
    Canvas(modifier.clipToBounds()) {
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
