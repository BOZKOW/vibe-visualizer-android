package io.github.bozkow.vibevisualizer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import io.github.bozkow.vibevisualizer.core.S
import io.github.bozkow.vibevisualizer.engine.E
import io.github.bozkow.vibevisualizer.engine.DanceAnimation
import io.github.bozkow.vibevisualizer.audio.Audio
import io.github.bozkow.vibevisualizer.core.now
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.math.abs

@Composable
fun BarsView(modifier: Modifier = Modifier) {
    if (!E.wide || !S.b("xDance") || Audio.away || S.low || E.reduceMotion() || E.inPip || E.ambient) {
        SpectrumBars(modifier); return
    }
    BoxWithConstraints(modifier) {
        val danceWidth = minOf(if (E.wide) 112.dp else 88.dp, maxWidth * .28f)
        val danceHeight = minOf(maxHeight, 112.dp)
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.Bottom) {
            SpectrumBars(Modifier.weight(1f).fillMaxHeight())
            ShigureDance(Modifier.width(danceWidth).height(danceHeight))
        }
    }
}

@Composable
private fun ShigureDance(modifier: Modifier) {
    var bpm by remember { mutableIntStateOf(120) }
    var smoothed by remember { mutableFloatStateOf(120f) }
    val active = E.playingState && E.visible && !E.quiet && E.settingsPage == null && E.sheet == null
    LaunchedEffect(active) {
        while (active) {
            delay(1000)
            val measured = Audio.tempoBpm() ?: continue
            smoothed += (measured - smoothed) * .35f
            val candidate = ((smoothed / 5).roundToInt() * 5).coerceIn(60,210)
            if (abs(candidate - bpm) >= 10) bpm += (candidate - bpm).coerceIn(-10,10)
        }
    }
    val prepared by produceState<android.graphics.drawable.AnimatedImageDrawable?>(null, bpm, E.quiet, E.visible) {
        if (E.visible && !E.quiet) value = DanceAnimation.load(bpm)
    }
    var image by remember { mutableStateOf<android.graphics.drawable.AnimatedImageDrawable?>(null) }
    var imageBpm by remember { mutableIntStateOf(120) }
    var startedAt by remember { mutableDoubleStateOf(0.0) }
    LaunchedEffect(prepared, active) {
        val next = prepared ?: return@LaunchedEffect
        if (image != null && active && image !== next) {
            val cycle = 240000.0 / imageBpm
            delay((cycle - ((now()-startedAt) % cycle)).toLong().coerceAtLeast(1))
        }
        image = next; imageBpm = bpm
    }
    val drawable = image ?: return
    DisposableEffect(drawable, active) {
        if (active) { startedAt = now(); drawable.start() } else drawable.stop()
        onDispose { drawable.stop() }
    }
    Canvas(modifier.clipToBounds()) {
        val scale = minOf(size.width / drawable.intrinsicWidth, size.height / drawable.intrinsicHeight)
        val w = drawable.intrinsicWidth * scale; val h = drawable.intrinsicHeight * scale
        drawable.setBounds(((size.width - w) / 2).toInt(), (size.height - h).toInt(), ((size.width + w) / 2).toInt(), size.height.toInt())
        drawable.draw(drawContext.canvas.nativeCanvas)
    }
}
