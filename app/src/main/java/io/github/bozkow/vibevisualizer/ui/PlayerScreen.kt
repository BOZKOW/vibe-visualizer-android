package io.github.bozkow.vibevisualizer.ui

import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import android.graphics.Matrix
import android.view.TextureView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import kotlinx.coroutines.launch
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.SizeTransform
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.zIndex
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.Canvas
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.YoutubeSearchedFor
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.foundation.border
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import kotlin.math.roundToInt
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.bozkow.vibevisualizer.core.S
import io.github.bozkow.vibevisualizer.audio.Audio
import io.github.bozkow.vibevisualizer.core.Server
import io.github.bozkow.vibevisualizer.core.clockLeft
import io.github.bozkow.vibevisualizer.core.fmtTime
import io.github.bozkow.vibevisualizer.core.wall
import io.github.bozkow.vibevisualizer.engine.AnimatedCover
import io.github.bozkow.vibevisualizer.engine.CanvasVideo
import io.github.bozkow.vibevisualizer.engine.E
import io.github.bozkow.vibevisualizer.engine.FilePicker
import io.github.bozkow.vibevisualizer.engine.Images
import io.github.bozkow.vibevisualizer.engine.Lyr
import io.github.bozkow.vibevisualizer.engine.Preload
import io.github.bozkow.vibevisualizer.engine.Updates
import io.github.bozkow.vibevisualizer.engine.Visuals
import io.github.bozkow.vibevisualizer.media.Local
import io.github.bozkow.vibevisualizer.spotify.Spotify

private val textShadow = Shadow(Color.Black.copy(alpha = .45f), androidx.compose.ui.geometry.Offset(0f, 2f), 14f)
private val dim = Color.White.copy(alpha = .68f)

/** The player, laid out as a music app: Now Playing (cover, title, controls), full-screen Lyrics and Visualizer with
 *  compact controls, or side by side on a wide screen (landscape, tablets, a foldable open). */
@Composable
fun PlayerScreen() {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // side by side: a big screen held wide (a tablet, an open foldable on its side), or a phone on its side; held
        // upright they get the phone's layout, no wider than 600 dp
        val wide = (maxWidth >= 600.dp && maxWidth >= maxHeight) || (maxWidth > maxHeight && maxHeight < 520.dp)
        if (E.wide != wide) E.wide = wide
        // clear of the bars and of the camera hole (where it really is: on its side some phones report less room)
        val ins = WindowInsets.statusBarsIgnoringVisibility.union(WindowInsets.navigationBarsIgnoringVisibility).union(cutoutInsets())
        val dn = androidx.compose.ui.platform.LocalDensity.current; val ldir = androidx.compose.ui.platform.LocalLayoutDirection.current
        val insL = with(dn) { ins.getLeft(dn, ldir).toDp() }
        // a phone on its side: Spicy Lyrics' stage for the Canvas
        val stage = if (wide && maxHeight < 560.dp) LandscapeStage((maxWidth - insL - with(dn) { ins.getRight(dn, ldir).toDp() }).value, (maxHeight - with(dn) { (ins.getTop(dn) + ins.getBottom(dn)).toDp() }).value) else null
        ScreenGrade(wide, stage != null)
        if (CanvasVideo.active) CanvasBackdrop(wide, centred = wide && maxHeight >= 560.dp && soloWide(), stage = stage, stageStart = insL)
        AmbientBlack()
        val pad = Modifier.fillMaxSize().windowInsetsPadding(ins.only(WindowInsetsSides.Vertical + WindowInsetsSides.Horizontal))
        // Ambient mode: its own screen, as a phone's always-on display
        if (E.ambient) { Box(pad) { AmbientScreen() }; return@BoxWithConstraints }
        androidx.compose.runtime.SideEffect { Frost.upright = !wide }
        when {
            E.src == "none" -> Box(pad) { EmptyState() }
            // a phone on its side, edge to edge: only the camera's side (and a navigation bar there) keeps its room; the
            // cover and the lyrics run under the status bar and down to the foot, what's on them kept clear of the bars
            wide && maxHeight < 560.dp -> Box(Modifier.fillMaxSize().windowInsetsPadding(ins.only(WindowInsetsSides.Horizontal))) { WideLayout() }
            wide -> Box(pad) { WideLayout() }
            // one screen: the cover, the lyrics and the bars together (a song without lyrics: the big cover)
            else -> Box(pad.onGloballyPositioned { Frost.cover = it.boundsInWindow() }, contentAlignment = Alignment.TopCenter) {
                // frosted as on its side: the lyrics run behind all of this now (no cover here: its blurred copy stands for the screen)
                androidx.compose.runtime.CompositionLocalProvider(LocalFrost provides !S.low) { Box(Modifier.widthIn(max = 600.dp).fillMaxSize()) { LyricsScreen() } }
            }
        }
    }
}

/* ================= Now Playing ================= */
@Composable
private fun NowPlaying(noLyricsNote: Boolean = false) {
    val hidden = E.uiHidden
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // while music plays untouched, the controls slide away (as in Spicy Lyrics Mobile): the cover, the song and the
        // seek bar stay; a tap brings the rest back
        TopBar(floatNotice = true)
        Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp).ambientOut(), contentAlignment = Alignment.Center) {
            // upright it grows to Apple's tall shape while a tall animated cover plays (Spicy Lyrics Mobile 0.19.0)
            if (!(CanvasVideo.cvmode && S.b("cvNoArt"))) CoverArt(Modifier.aspectRatio(bigCoverShape(), matchHeightConstraintsFirst = true).canvasShrink(), corner = 22.dp, tall = true)
        }
        if (S.view == "both") ExtraBars(Modifier.fillMaxWidth().height(64.dp).padding(bottom = 6.dp))   // (48 dp tall until 0.46.1; edge to edge in 0.46.2 only, the owner preferred it inside the margins)
        SongInfo(big = true, note = noLyricsNote)
        AnimatedVisibility(!hidden, enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(), exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()) {
            Column { Transport(big = true); if (S.b("volshow")) VolumeRow(); BottomBar() }
        }
        if (hidden) Spacer(Modifier.height(16.dp))
    }
}

/* ================= Lyrics ================= */
@Composable
private fun LyricsScreen() {
    // a song without lyrics (or still looking for them after one without): the big cover, the song, the seek bar,
    // controls that slide away, with a short note under the song
    Crossfade(noLyrics(), label = "nolyrics") { big -> if (big) NowPlaying(noLyricsNote = true) else LyricsWithLyrics() }
}

/** The Lyrics screen upright (0.53.0, after Spicy Lyrics Mobile, the owner's pick): the song at the top, right under the
 *  status bar, no top line (a big cover, its name and artist beside it, translate / romanize and the heart at its end), the
 *  lyrics under it, then the bars, the seek bar and the credits, and the controls when they're woken. Notices come first,
 *  above the song, pushing it down while they show. With the
 *  Focus style's Cover in long breaks, the cover itself grows from its place into the lyrics' and goes back as the next
 *  line comes ([MovingCover]); the name stays. Without the cover (Settings → Player screen → Cover at the top): the name
 *  alone up there, and a break's cover fades in over the lyrics as before. */
@Composable
private fun LyricsWithLyrics() {
    val hidden = E.uiHidden
    val withCover = S.b("aLyrCover")
    var slot by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }   // the cover's place in the song at the top (in the root)
    var area by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }   // the lyrics' area (in the root)
    var origin by remember { mutableStateOf(Offset.Zero) }
    var nameAt by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }   // the name centred at the top in a break (in the root)
    // a long break: the cover in the lyrics' place, gone 2 s before the next line (not while you scroll them)
    val rest = restCover()
    val rk by animateFloatAsState(if (rest) 1f else 0f, if (S.low || E.reduceMotion()) tween(0) else spring(.85f, if (rest) Spring.StiffnessVeryLow else Spring.StiffnessLow), label = "rest")
    val over = rk >= .99f
    androidx.compose.runtime.SideEffect { Lyr.view.covered = over }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { Lyr.view.covered = false } }
    Box(Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInRoot() }) {
        Column(Modifier.fillMaxSize()) {
            // no top line here (the owner's pick: the song as high as Spicy Lyrics has it); notices still come first
            TopBar(Modifier.padding(horizontal = 24.dp), floatNotice = true, line = false, upNext = false, report = false)
            // (the song at the top is where the top's shade and the Canvas's blur reach down to, as the top line was and the
            // controls are: the owner's ask)
            Box(Modifier.reportTop().padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp), contentAlignment = Alignment.Center) {
                TitleRow(big = false, bleed = true, cover = withCover, header = true, coverSlot = { slot = it }, textAway = if (withCover) rk else 0f)
                // while the cover is out in a long break, the name and artist centred at the top (the owner's pick: beside an
                // empty spot they looked off), crossfading with the ones beside the cover
                if (withCover && rk > .01f) Column(Modifier.fillMaxWidth().onGloballyPositioned { nameAt = it.boundsInRoot() }.graphicsLayer { alpha = rk.coerceIn(0f, 1f) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.runtime.CompositionLocalProvider(LocalSlideGroup provides remember { SlideGroup() }) {
                        val k = when (S.str("aTitle")) { "s" -> .85f; "l" -> 1.18f; else -> 1f }
                        Scrolling(E.title, 24f.times(k).sp, FontWeight.Bold, Color.White, (-.3).sp)
                        val by = E.byLine()
                        if (by.isNotEmpty()) Scrolling(by, 16f.times(k).sp, FontWeight.SemiBold, dim)
                    }
                    if (S.b("aUpNext")) UpNextLine(center = true)
                }
            }
            var areaH by remember { mutableFloatStateOf(0f) }
            Box(Modifier.weight(1f).fillMaxWidth().zIndex(-1f).onSizeChanged { areaH = it.height.toFloat() }.onGloballyPositioned { area = it.boundsInRoot() }) {   // (under all the rest: the lyrics run behind it)
                LyricsPane(Modifier.fillMaxSize().graphicsLayer { alpha = (1f - rk).coerceIn(0f, 1f) }, areaHeightPx = areaH, padStart = 24f, padEnd = 24f, problems = false, behind = true)
                if (!withCover && rk > .01f) Box(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp).ambientOut()
                    .graphicsLayer { alpha = rk.coerceIn(0f, 1f); val s = .9f + .1f * rk; scaleX = s; scaleY = s }, contentAlignment = Alignment.Center) {
                    CoverArt(Modifier.aspectRatio(1f, matchHeightConstraintsFirst = true).canvasShrink(), corner = 22.dp)
                }
            }
            // (lined up with the seek bar and the lyrics, 24 dp in: they reached 12 dp further out on both sides, the owner's pick)
            if (S.view == "both") ExtraBars(Modifier.fillMaxWidth().height(if (hidden) 80.dp else 56.dp).padding(horizontal = 24.dp))
            Column(Modifier.padding(horizontal = 24.dp)) { SongInfo(big = false, lyrics = true, name = false, restCover = rest) }
            AnimatedVisibility(!hidden, enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(), exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()) {
                Column(Modifier.padding(horizontal = 24.dp)) { Transport(big = false, mid = true); BottomBar(big = false, mid = true, lyricsToggles = true) }
            }
            if (hidden) Spacer(Modifier.height(16.dp))
        }
        if (withCover && slot.width > 0f) MovingCover(slot, area, rk, origin, nameAt.bottom)
    }
}

/** The cover of the song at the top, drawn over the screen at its place there ([slot]); in a long break ([k] going to 1) it
 *  grows into the middle of the lyrics' [area], as big as the break's cover was, and flows back as the next line comes
 *  (the owner: "make the cover get bigger, the text would stay"). Positions are in the root; [origin] is this layer's. */
@Composable
private fun MovingCover(slot: androidx.compose.ui.geometry.Rect, area: androidx.compose.ui.geometry.Rect, k0: Float, origin: Offset, nameBottom: Float = 0f) {
    val dens = androidx.compose.ui.platform.LocalDensity.current
    val cvk by animateFloatAsState(if (CanvasVideo.cvmode && S.b("cvShrink")) .56f else 1f, if (S.low || E.reduceMotion()) tween(0) else spring(.85f, Spring.StiffnessVeryLow), label = "cvcover")
    val k = k0.coerceIn(0f, 1f)
    // big, from just under the name centred at the top down to the lyrics' foot, as wide as the margins allow (only the
    // lyrics' area, under the whole song's place, made it look small: the owner)
    val padH = with(dens) { 24.dp.toPx() }; val padV = with(dens) { 12.dp.toPx() }
    // (clear of the bars under it too: their tallest reach above their own room)
    val top = (if (nameBottom > 0f) nameBottom else area.top) + padV; val bottom = area.bottom - with(dens) { 28.dp.toPx() }
    val bigSide = (minOf(area.width - 2 * padH, bottom - top) * cvk).coerceAtLeast(slot.width)
    fun mix(a: Float, b: Float) = a + (b - a) * k
    val side = mix(slot.width, bigSide)
    val x = mix(slot.left, area.center.x - bigSide / 2); val y = mix(slot.top, (top + bottom) / 2 - bigSide / 2)
    val shape = RoundedCornerShape(mix(10f, 22f).dp)
    Box(Modifier.offset { androidx.compose.ui.unit.IntOffset((x - origin.x).roundToInt(), (y - origin.y).roundToInt()) }.size(with(dens) { side.toDp() })
        .then(if (k > .01f) Modifier.ambientOut() else Modifier)) {
        // small, a soft shadow of its own; big, the big cover's (and its animated cover, if the album has one)
        CoverArt(Modifier.fillMaxSize().then(if (k < .99f) Modifier.shadow(mix(8f, 24f).dp, shape, ambientColor = Color.Black, spotColor = Color.Black) else Modifier), corner = mix(10f, 22f).dp, small = k < .99f)
    }
}

/** The cover's size in the song at the top of the Lyrics screen: about a third of the width, as Spicy Lyrics Mobile has it
 *  (115 dp on a 360 dp phone), less on a short screen. */
@Composable
private fun headerCoverSize(): Dp {
    val c = androidx.compose.ui.platform.LocalConfiguration.current
    return minOf(c.screenWidthDp * .32f, c.screenHeightDp * .16f).coerceIn(64f, 128f).dp
}

/* ================= Visualizer ================= */

/* ================= side by side ================= */
@Composable
private fun WideLayout() {
    BoxWithConstraints(Modifier.fillMaxSize()) { if (maxHeight < 560.dp) LandscapePhone() else Tablet() }
}

/** A tablet or an open foldable: Now Playing on the left, the lyrics (or the bars) on the right. */
@Composable
private fun Tablet() {
    val solo = soloWide()   // no lyrics: Now Playing alone, the cover big
    Row(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.Center) {
        Column(Modifier.fillMaxHeight().then(if (solo) Modifier.widthIn(max = 560.dp) else Modifier.weight(.42f).widthIn(max = 480.dp)), horizontalAlignment = Alignment.CenterHorizontally) {
            TopBar(Modifier.ambientOut(), floatNotice = true)
            Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp).ambientOut(), contentAlignment = Alignment.Center) {
                if (!(CanvasVideo.cvmode && S.b("cvNoArt"))) CoverArt(Modifier.aspectRatio(1f, matchHeightConstraintsFirst = true).canvasShrink(), corner = 22.dp)
            }
            if (solo && S.view == "both") ExtraBars(Modifier.fillMaxWidth().height(56.dp).padding(bottom = 6.dp))
            TitleRow(big = true)
            if (solo) NoLyricsNote()
            SeekRow()
            Column(Modifier.ambientOut()) {
                Transport(big = true)
                if (S.b("volshow")) VolumeRow()
                BottomBar()
            }
        }
        if (!solo) {
            Spacer(Modifier.width(32.dp))
            RightPane(Modifier.fillMaxHeight().weight(.58f))
        }
    }
}

/** A phone on its side: all that isn't lyrics on the left (where it plays, what's wrong, the cover or the Canvas, the
 *  song, the seek bar and the controls, which come and go as in portrait), the lyrics (or the bars) with the whole
 *  height on the right. */
@Composable
private fun LandscapePhone() = LandscapeSplit()

private val showCtl = expandVertically(expandFrom = Alignment.Top) + fadeIn()
private val hideCtl = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()

/** A phone on its side: while a Canvas plays, as on the desktop (the video over the left half, the cover smaller and
 *  the song centred over it, the lyrics on the right); otherwise the cover as tall as the screen on the left. */
@Composable
private fun LandscapeSplit() {
    androidx.compose.runtime.CompositionLocalProvider(LocalFrost provides !S.low) {   // (Low performance mode: see-through)
        Crossfade(CanvasVideo.cvmode, label = "landcv") { cv -> if (cv) LandscapeCanvas() else LandscapeCover() }
    }
}

/** With a Canvas, Spicy Lyrics': the video takes the cover's place (the backdrop), so none is drawn, and the song, the seek bar
 *  and the controls are docked at its foot like a video player's, stacked from the bottom, the name a caption (CanvasImmersive
 *  in Spicy Lyrics, CoverStage in Spicy Lyrics Mobile). The lyrics take the rest, from over the video's soft edge. */
@Composable
private fun LandscapeCanvas() {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val st = LandscapeStage(maxWidth.value, maxHeight.value)
        val start = if (cameraOnLeft()) 48.dp else 20.dp   // (as the cover's, without a Canvas)
        // (the song in its big size, but the seek bar and the buttons at the stage's width: wider, as on 0.45.1, looked worse to the owner)
        val dockW = maxOf(st.solid.dp - start, 220.dp)
        Row(Modifier.fillMaxSize().padding(start = start, top = 8.dp, bottom = 8.dp)) {
            Column(Modifier.width(dockW).fillMaxHeight().ambientOut().windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility.union(WindowInsets.navigationBarsIgnoringVisibility).only(WindowInsetsSides.Vertical))) {
                TopBar(side = true)
                Spacer(Modifier.weight(1f))
                TitleRow(big = true, toggles = !barsInstead())
                SeekRow()
                SideControls()
            }
            val gap = maxOf(st.lyricsStart.dp - start - dockW, 8.dp)
            Spacer(Modifier.width(gap))
            SideColumn(Modifier.weight(1f), endGap = maxOf(st.lyricsEndGap.dp, 8.dp), startRoom = gap * .8f)
        }
    }
}

/** Without a Canvas: the cover as tall as the screen on the left, and over it where it plays (and what's wrong) at its
 *  head, the song in its big size, the seek bar and the controls at its foot, each on a soft shade so they read on any
 *  cover. When the controls hide, the cover has its middle back. */
@Composable
private fun LandscapeCover() {
  BoxWithConstraints(Modifier.fillMaxSize()) {
    // the cover's size goes by the screen: the height there is (8 dp clear above and below), but no more than 40% of the
    // width, so the lyrics, the bars and the credits keep most of it (as tall as the screen, it took nearly half a wide
    // phone: "too large"); never so small its buttons get cramped. Centred in the height
    val dn = androidx.compose.ui.platform.LocalDensity.current
    val side = minOf(maxHeight - 16.dp, maxOf(maxWidth * .4f, 280.dp)).coerceAtLeast(160.dp)
    val gapY = (maxHeight - side) / 2
    // only what of the status and navigation bars lies over the cover keeps its words clear of them
    val headIn = (with(dn) { WindowInsets.statusBarsIgnoringVisibility.getTop(dn).toDp() } - gapY).coerceAtLeast(0.dp)
    val footIn = (with(dn) { WindowInsets.navigationBarsIgnoringVisibility.getBottom(dn).toDp() } - gapY).coerceAtLeast(0.dp)
    // further in on the camera's side: the owner's phone still had the cover in the hole with the room it reports
    Row(Modifier.fillMaxSize().padding(start = if (cameraOnLeft()) 48.dp else 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(side).ambientOut().onGloballyPositioned { Frost.cover = it.boundsInWindow() }) {
            CoverArt(Modifier.fillMaxSize(), corner = 18.dp)
            var headH by remember { mutableFloatStateOf(0f) }
            var footH by remember { mutableFloatStateOf(0f) }
            // drawn behind both, reaching past them and fading out over the cover (nothing at the head: no shade there);
            // the same corners as the cover's (Settings → Player screen → Cover corners)
            Column(Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp * (S.i("aCoverR").coerceIn(0, 40) / 22f))).drawBehind {
                // (eased at their ends: from clear straight to dark, the edge showed as a line across a bright cover)
                val down = 36.dp.toPx(); val up = 72.dp.toPx()
                val h0 = headH / (headH + down); val f = up / (footH + up)
                if (headH > 0f) drawRect(Brush.verticalGradient(0f to Color.Black.copy(alpha = .6f), h0 to Color.Black.copy(alpha = .35f), h0 + (1 - h0) * .5f to Color.Black.copy(alpha = .1f), 1f to Color.Transparent, endY = headH + down),
                    size = androidx.compose.ui.geometry.Size(size.width, headH + down))
                val y = size.height - footH - up
                drawRect(Brush.verticalGradient(0f to Color.Transparent, f * .35f to Color.Black.copy(alpha = .06f), f * .7f to Color.Black.copy(alpha = .22f), f to Color.Black.copy(alpha = .38f), 1f to Color.Black.copy(alpha = .72f), startY = y, endY = size.height),
                    topLeft = Offset(0f, y), size = androidx.compose.ui.geometry.Size(size.width, footH + up))
            }) {
                TopBar(Modifier.onSizeChanged { headH = it.height.toFloat() }.padding(top = headIn).padding(horizontal = 10.dp), side = true)
                Spacer(Modifier.weight(1f))
                Column(Modifier.fillMaxWidth().onSizeChanged { footH = it.height.toFloat() }.padding(bottom = footIn).padding(start = 18.dp, end = 18.dp, bottom = 6.dp)) {
                    TitleRow(big = true, toggles = !barsInstead())
                    SeekRow()
                    SideControls()
                }
            }
        }
        Spacer(Modifier.width(20.dp))
        SideColumn(Modifier.weight(1f).padding(vertical = 8.dp), endGap = 20.dp, startRoom = 16.dp)
    }
  }
}

/** On its side, the bars take the lyrics' place: the Visualizer, or a song without lyrics. */
private fun barsInstead() = E.screen == "viz" || noLyrics()

/** The controls on its side, in two rows, each its full size (side by side in one row they were too small to hit);
 *  they hide after a few seconds. */
@Composable
private fun SideControls() {
    AnimatedVisibility(!E.uiHidden, enter = showCtl, exit = hideCtl) {
        Column(Modifier.fillMaxWidth()) { Transport(big = false); BottomBar(big = false) }
    }
}

/** The right side on its side: the lyrics with the whole height (duets and background vocals have room), or the bars,
 *  then the small bars and the credits under them. All the rest is on the left. */
@Composable
private fun SideColumn(modifier: Modifier, endGap: Dp = 20.dp, startRoom: Dp = 40.dp, lyricsEnd: Dp = 6.dp) {
    // the column runs to the screen's right side, its lyrics blurring out there; the lines, the bars and the credits stay
    // [endGap] in from it, as until 0.47.0 (0.48.0 ran the bars to the edge, faded there: too far right, clipped); the
    // lyrics' blur reaches no further left than [startRoom] (it lay over the cover's edge); the credits clear of the
    // navigation bar
    Column(modifier.fillMaxHeight()) {
        val bars = barsInstead()
        if (bars && E.screen != "viz") NoLyricsNote(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility.only(WindowInsetsSides.Top)).padding(end = endGap))
        var areaH by remember { mutableFloatStateOf(0f) }
        Box(Modifier.weight(1f).fillMaxWidth().zIndex(-1f).onSizeChanged { areaH = it.height.toFloat() }) {
            if (bars) { BarsView(Modifier.fillMaxSize().padding(top = 8.dp, bottom = 8.dp, end = endGap)); AwayNote(Modifier.align(Alignment.Center)) }
            // the lyrics nearer the side than the bars ([lyricsEnd]): the other singer's lines, set at the right, end close to
            // the screen's edge (28 dp in, they sat towards the middle: "move the duet vocals more to the right")
            else LyricsPane(Modifier.fillMaxSize().padding(end = lyricsEnd), areaHeightPx = areaH, padStart = 4f, padEnd = 6f, problems = false, behind = true, startRoom = startRoom)
        }
        if (!bars && S.view == "both") ExtraBars(Modifier.fillMaxWidth().padding(end = endGap).height(34.dp))
        Box(Modifier.windowInsetsPadding(WindowInsets.navigationBarsIgnoringVisibility.only(WindowInsetsSides.Bottom)).padding(end = endGap)) { SongInfo(big = false, lyrics = !bars, name = false, seek = false) }
    }
}

/* ================= Ambient mode ================= */

/** Fades out (slowly) while Ambient mode dims the screen, back at once on a touch. */
@Composable
private fun Modifier.ambientOut(): Modifier {
    val k by animateFloatAsState(if (E.ambient) 0f else 1f, tween(if (E.ambient) 1500 else 300), label = "ambout")
    return graphicsLayer { alpha = k }
}

/** Ambient mode's black, over the background and the Canvas, under the lyrics and the song. */
@Composable
private fun AmbientBlack() {
    // black, or the player behind dimmed right down (Settings → Player screen → Ambient mode → Background)
    val k by animateFloatAsState(if (E.ambient) (if (S.str("aAmbBg") == "dim") .78f else 1f) else 0f, tween(if (E.ambient) 1500 else 300), label = "ambblack")
    if (k > 0f) Box(Modifier.fillMaxSize().graphicsLayer { alpha = k }.background(Color.Black))
}

@Composable
private fun RightPane(modifier: Modifier) {
    Column(modifier) {
        var areaH by remember { mutableFloatStateOf(0f) }
        // the Visualizer, or a song without lyrics: the bars (and why there are no lyrics, at the top)
        val bars = E.screen == "viz" || noLyrics()
        if (bars && E.screen != "viz") NoLyricsNote(Modifier.fillMaxWidth())
        Box(Modifier.weight(1f).fillMaxWidth().onSizeChanged { areaH = it.height.toFloat() }) {
            if (bars) { BarsView(Modifier.fillMaxSize().padding(vertical = 16.dp)); AwayNote(Modifier.align(Alignment.Center)) }
            else LyricsPane(Modifier.fillMaxSize(), areaHeightPx = areaH, padStart = 8f, padEnd = 16f, problems = false)
        }
        if (!bars && S.view == "both") ExtraBars(Modifier.fillMaxWidth().height(64.dp))
        LyricsCredits()
    }
}

/* ================= nothing playing yet ================= */
@Composable
private fun EmptyState() {
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        TopBar(home = true)
        Spacer(Modifier.weight(1f))
        AppLogo(96.dp)
        Spacer(Modifier.height(20.dp))
        Text("Nothing playing", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        Text("Play a song from your phone, connect Spotify, or play something in YouTube Music, SoundCloud or another music app: the app follows it, with its cover and lyrics.",
            Modifier.padding(top = 8.dp).widthIn(max = 420.dp), fontSize = 15.sp, color = dim, textAlign = TextAlign.Center)
        Spacer(Modifier.height(22.dp))
        VButton(onClick = { E.haptic(); FilePicker.pickAudio() }, Modifier.fillMaxWidth().widthIn(max = 360.dp).height(52.dp)) { Text("Play songs from your phone", fontSize = 15.sp) }
        Spacer(Modifier.height(10.dp))
        VTonalButton(onClick = { E.haptic(); if (Spotify.connected) E.setSource("spotify") else E.spLogin() }, Modifier.fillMaxWidth().widthIn(max = 360.dp).height(52.dp)) {
            Icon(VIcons.spotify, null, Modifier.size(20.dp)); HSpace(8.dp); Text(if (Spotify.connected) "Use Spotify" else "Connect Spotify", fontSize = 15.sp)
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { E.wizard = true }) { Text("Setup guide") }
            TextButton(onClick = { E.openSettings() }) { Text("Settings") }
            TextButton(onClick = { E.openSettings("help") }) { Text("What’s new") }
        }
    }
}

/* ================= parts ================= */

/** The top of every screen: where the music plays from at the left and on what device at the right, and under them the
 *  space for what's wrong. Always there, also with the controls away. */
@Composable
private fun TopBar(modifier: Modifier = Modifier, home: Boolean = false, side: Boolean = false, floatNotice: Boolean = false, line: Boolean = true,
                   upNext: Boolean = true, noticeGap: Boolean = true, report: Boolean = true) {
    E.sec.intValue
    // where it plays from and on, short enough to stand either side of the camera whole (with "PLAYING FROM …" and
    // "PLAYING ON …" both only ever showed their first words, crawling); an icon says which is which
    val app = io.github.bozkow.vibevisualizer.media.MediaApps.appName().uppercase()
    // your own files playing with Spotify connected: the line says so and is the way back (a tap: Spotify again, 0.46.0)
    val back = E.src == "file" && Spotify.connected
    val toSpotify: (() -> Unit)? = if (back) ({ E.setSource("spotify") }) else null
    val top = when (E.src) { "spotify" -> "SPOTIFY"; "file" -> if (back) "THIS PHONE · ⇄ SPOTIFY" else "THIS PHONE"; "media" -> app; else -> "VIBE VISUALIZER" }
    val topIcon = when (E.src) { "spotify" -> VIcons.spotify; "file" -> Icons.Rounded.PhoneAndroid; "media" -> Icons.Rounded.MusicNote; else -> null }
    val sd = Spotify.device
    val dev = when (E.src) { "spotify" -> sd?.name ?: if (Spotify.meta == null) "NOTHING PLAYING" else ""; "file" -> Local.position(); else -> "" }.uppercase()
    val devIcon = when (E.src) { "spotify" -> sd?.let { deviceIcon(it.type) }; "file" -> Icons.AutoMirrored.Rounded.QueueMusic; else -> null }
    Column(modifier.fillMaxWidth().then(if (report) Modifier.reportTop() else Modifier)) {
        // the status bar hidden: the line goes up where it was, either side of the camera, and takes no room here
        // where it plays from and on: gone since 0.55.0 (the owner: "remove the playing from and on completely"); only the
        // home screen keeps its line (the app's name). Notices stay
        val showLine = home && S.b("aTopLine")
        if (side && showLine) {
            // on its side, one line with room for the words: "PLAYING FROM SPOTIFY ON <DEVICE>"
            val one = when (E.src) {
                "spotify" -> "PLAYING FROM SPOTIFY" + (if (sd != null) " ON $dev" else if (dev.isNotEmpty()) " · $dev" else "")
                "file" -> "PLAYING ON THIS PHONE" + (if (dev.isNotEmpty()) " · $dev" else "") + (if (back) " · ⇄ SPOTIFY" else "")
                "media" -> "PLAYING FROM $app"
                else -> top
            }
            // the words "PLAYING FROM / ON" only where they fit whole: over a Canvas (the controls' narrow panel) they made the
            // line slide; without them it's the upright line's "THIS PHONE · SONG 1 OF 2", which slides only if even that's long
            val short = when (E.src) {
                "spotify" -> "SPOTIFY" + (if (dev.isNotEmpty()) " · $dev" else "")
                "file" -> "THIS PHONE" + (if (dev.isNotEmpty()) " · $dev" else "") + (if (back) " · ⇄ SPOTIFY" else "")
                "media" -> app
                else -> top
            }
            androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth().height(26.dp).then(if (toSpotify != null) Modifier.clickable { E.haptic(); toSpotify() } else Modifier), contentAlignment = Alignment.Center) {
                val tm = androidx.compose.ui.text.rememberTextMeasurer()
                val style = androidx.compose.material3.LocalTextStyle.current.merge(TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
                val room = constraints.maxWidth - with(androidx.compose.ui.platform.LocalDensity.current) { 4.dp.roundToPx() }   // (whole, it sits in the middle with no padding)
                val fits = remember(one, room, style) { tm.measure(one, style, maxLines = 1, softWrap = false).size.width <= room }
                AnimatedContent(if (fits) one else short, Modifier.fillMaxWidth(), contentAlignment = Alignment.Center, transitionSpec = { (fadeIn(tween(260)) + slideInVertically { it / 2 }) togetherWith fadeOut(tween(160)) }, label = "hdone") { t ->
                    TopText(t, 11.sp, FontWeight.Bold, dim, 1.sp)
                }
            }
        } else if (showLine && line) TopLine(top, topIcon, dev, devIcon, Modifier, beside = S.b("aHideStatus"), onTopClick = toSpotify)
        // [floatNotice]: the notice floats in a room of a fixed height (NoticeGap): the lyrics and the cover move down by the same
        // amount whatever its size, one line or two (0.46.1; at its own height a taller notice pushed them further, and on the
        // lyrics screen in 0.45.4 it took no room at all while the cover screen made room for it)
        if (floatNotice) {
            Box(Modifier.fillMaxWidth().zIndex(1f).layout { m, c -> val p = m.measure(c.copy(minHeight = 0)); layout(p.width, 0) { p.place(0, 0) } }) { NoticeSpace(home, row = side, upNext = upNext) }
            if (noticeGap) NoticeGap(home, upNext)   // ([noticeGap] false: on its side they float over the cover's top edge, the owner's pick)
        }
        else NoticeSpace(home, row = side, upNext = upNext)
    }
}

/** The room a notice gets on the upright screens while one shows: always the same, so everything under it moves alike. */
@Composable
private fun NoticeGap(home: Boolean, upNext: Boolean = true) {
    val on = E.notices(home).any { upNext || it.id != "upnext" }
    val h by androidx.compose.animation.core.animateDpAsState(if (on) 60.dp else 0.dp,
        if (S.low || E.reduceMotion()) tween(0) else spring(.85f, Spring.StiffnessMediumLow), label = "noticegap")
    Spacer(Modifier.height(h))
}

/** Where the top's text (and what's wrong under it) ends on screen, in px: the Canvas is blurred down to there. */
private object TopArea { var bottom by mutableFloatStateOf(0f) }
private fun Modifier.reportTop() = onGloballyPositioned { TopArea.bottom = it.positionInWindow().y + it.size.height }

/** Where it plays from at the left, the device at the right, each in the middle of its side. Beside the camera:
 *  drawn up in the hidden status bar's place, level with the camera and clear of it at both sides. */
@Composable
private fun TopLine(top: String, topIcon: ImageVector?, dev: String, devIcon: ImageVector?, modifier: Modifier, beside: Boolean = false, onTopClick: (() -> Unit)? = null) {
    val view = androidx.compose.ui.platform.LocalView.current
    val dens = androidx.compose.ui.platform.LocalDensity.current
    var at by remember { mutableStateOf<Offset?>(null) }            // where this row starts in the window
    var w by remember { mutableFloatStateOf(0f) }
    var cut by remember { mutableStateOf<android.graphics.Rect?>(null) }   // the camera hole above the row
    val e = with(dens) { 8.dp.toPx() }      // up there it reaches a little further out (the corners are clear at that height)
    val gap = with(dens) { 16.dp.toPx() }   // room to the camera
    val x0 = (at?.x ?: 0f) - (if (beside) e else 0f)
    val ww = w + (if (beside) 2 * e else 0f)
    val c = cut?.takeIf { beside && it.right > x0 && it.left < x0 + ww }
    // where each text goes (px from the row's start, and how wide): either side of a camera in the middle; with the
    // camera near a corner, both share the side that has room; without one, a half each
    val minSide = with(dens) { 90.dp.toPx() }
    val half = gap / 2
    val spots = if (c == null) floatArrayOf(0f, ww / 2 - half, ww / 2 + half, ww / 2 - half) else {
        val l1 = c.left - x0 - gap; val r0 = c.right - x0 + gap
        when {
            l1 >= minSide && ww - r0 >= minSide -> floatArrayOf(0f, l1, r0, ww - r0)
            // a corner: the side with room, shared (or all of it when there's no device to name)
            l1 < minSide -> if (dev.isEmpty()) floatArrayOf(r0, ww - r0, ww, 0f) else { val m = (r0 + ww) / 2; floatArrayOf(r0, m - half - r0, m + half, ww - m - half) }
            else -> if (dev.isEmpty()) floatArrayOf(0f, l1, l1, 0f) else { val m = l1 / 2; floatArrayOf(0f, m - half, m + half, l1 - m - half) }
        }
    }
    val lx = spots[0]; val rx = spots[2]
    // the outer ends reach out to the screen's sides when a text starts at the row's start or ends at its end (no camera there)
    val winW = view.width.toFloat()
    val outerL = if (at != null && lx == 0f) with(dens) { x0.coerceAtLeast(0f).toDp() } else 0.dp
    val outerR = if (at != null && kotlin.math.abs(rx + spots[3] - ww) < 1f && winW > 0f) with(dens) { (winW - x0 - ww).coerceAtLeast(0f).toDp() } else 0.dp
    val lw = with(dens) { spots[1].coerceAtLeast(1f).toDp() }
    val rw = with(dens) { spots[3].coerceAtLeast(1f).toDp() }
    Box(modifier.fillMaxWidth()
        .onGloballyPositioned { lc ->
            val p = lc.positionInWindow(); at = p; w = lc.size.width.toFloat()
            if (beside) cut = view.rootWindowInsets?.displayCutout?.boundingRects?.firstOrNull { it.top <= 0 && it.bottom <= p.y + 1 && it.width() < lc.size.width }
        }
        .then(if (beside) Modifier.layout { m, cs ->
            val ex = e.roundToInt()
            val pl = m.measure(cs.copy(minWidth = cs.maxWidth + 2 * ex, maxWidth = cs.maxWidth + 2 * ex, minHeight = 0))
            val y0 = at?.y ?: 0f
            val cy = cut?.exactCenterY() ?: (y0 / 2)   // level with the camera; without one, the middle of the bar's place
            layout(cs.maxWidth, 0) { pl.place(-ex, (cy - y0 - pl.height / 2f).roundToInt()) }
        } else Modifier.height(32.dp))
        .graphicsLayer { alpha = if (at == null) 0f else 1f }) {
        // each in the middle of its side
        AnimatedContent(top to topIcon, Modifier.align(Alignment.CenterStart).offset { androidx.compose.ui.unit.IntOffset(lx.roundToInt(), 0) }.width(lw).then(if (onTopClick != null) Modifier.quickClick(null, indication = androidx.compose.material3.ripple()) { E.haptic(); onTopClick() } else Modifier), contentAlignment = Alignment.Center, transitionSpec = { (fadeIn(tween(260)) + slideInVertically { it / 2 }) togetherWith fadeOut(tween(160)) }, label = "hdtop") { (t, ic) ->
            TopText(t, 11.sp, FontWeight.Bold, dim, 1.sp, icon = ic, bleedL = outerL)
        }
        AnimatedContent(dev to devIcon, Modifier.align(Alignment.CenterStart).offset { androidx.compose.ui.unit.IntOffset(rx.roundToInt(), 0) }.width(rw), contentAlignment = Alignment.Center, transitionSpec = { (fadeIn(tween(260)) + slideInVertically { it / 2 }) togetherWith fadeOut(tween(160)) }, label = "hdsub") { (t, ic) ->
            if (t.isNotEmpty()) TopText(t, 11.sp, FontWeight.Bold, dim, 1.sp, icon = ic, bleedR = outerR)
        }
    }
}

/** A text of the top line, in the middle of its side; too long for it, it slides on and on, as a long song name does,
 *  fading out at both ends of its side. At rest its first letter is whole, just after the fade (or it ends in "…" with
 *  Scroll long names off). */
@Composable
private fun TopText(text: String, size: androidx.compose.ui.unit.TextUnit, weight: FontWeight, color: Color, spacing: androidx.compose.ui.unit.TextUnit = 0.sp,
                    icon: ImageVector? = null, align: Alignment = Alignment.Center, bleedL: Dp = 0.dp, bleedR: Dp = 0.dp) {
    var boxW by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var rowW by remember(text) { androidx.compose.runtime.mutableIntStateOf(0) }
    val over = boxW > 0 && rowW > boxW
    Box(Modifier.fillMaxWidth().onSizeChanged { boxW = it.width }, contentAlignment = align) {
        if (!S.b("aMarquee") || S.low || S.calm) Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { Icon(icon, null, Modifier.size(12.dp), tint = color); Spacer(Modifier.width(5.dp)) }
            Text(text, Modifier.weight(1f, fill = false), fontSize = size, fontWeight = weight, letterSpacing = spacing, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
        } else {
            // too long: the icon goes along with the words, and at the line's outer ends ([bleedL], [bleedR]: nothing else
            // there, no camera) the scrolling reaches out to the screen's side, fading out (0.46.1)
            val iter = if (E.settingsPage == null) Int.MAX_VALUE else 0
            val m = if (over) Modifier.edgeMarquee(bleedL, bleedR, if (bleedL > 0.dp) 12.dp else 10.dp, if (bleedR > 0.dp) 16.dp else 14.dp, iter)
                .then(if (bleedL > 0.dp) Modifier else Modifier.padding(start = 10.dp))
                else Modifier.pingPong(iter)
            Box(m) {
                Row(Modifier.onSizeChanged { rowW = it.width }, verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) { Icon(icon, null, Modifier.size(12.dp), tint = color); Spacer(Modifier.width(5.dp)) }
                    Text(text, fontSize = size, fontWeight = weight, letterSpacing = spacing, color = color, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

/** Under the top line: what's wrong (or that the phone is warm), while it lasts, even with the controls away. One at a
 *  time: several take turns (4.5 s each), a new one first, with a dot for each, and you can swipe between them, after which it
 *  stays on the one you left it on. Only a hard error has a button. */
@Composable
private fun NoticeSpace(home: Boolean, row: Boolean = false, upNext: Boolean = true) {
    // ([upNext] false: the upright Lyrics screen, where Up next has its line in the song at the top instead)
    val all = E.notices(home).let { l -> if (upNext) l else l.filter { it.id != "upnext" } }
    val ids = all.map { it.id }
    var turn by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    // on and on while there's more than one (a change in the list doesn't start them over)…
    val many = all.size > 1
    var byHand by remember { mutableStateOf(false) }   // swiped: no more turns by themselves, until a new one comes
    var dir by remember { androidx.compose.runtime.mutableIntStateOf(0) }   // which way the last swipe went (1: on to the next)
    // (each turn 4.5 s, or as long as its words need to slide to their end and be read there)
    val nowAll by androidx.compose.runtime.rememberUpdatedState(all)
    LaunchedEffect(many, byHand) {
        while (many && !byHand) {
            val t0 = android.os.SystemClock.uptimeMillis(); kotlinx.coroutines.delay(4500)
            while (true) {
                val cur = nowAll.getOrNull(((turn % nowAll.size.coerceAtLeast(1)) + nowAll.size.coerceAtLeast(1)) % nowAll.size.coerceAtLeast(1))
                val left = E.readNeed(cur) - (android.os.SystemClock.uptimeMillis() - t0); if (left <= 0) break; kotlinx.coroutines.delay(left)
            }
            turn++
        }
    }
    // …and a new one shows first
    val seen = remember { HashSet<String>() }
    LaunchedEffect(ids) { if (ids.any { it !in seen && it != "upnext" }) { turn = 0; byHand = false; dir = 0 }; seen.clear(); seen.addAll(ids) }   // (Up next waits its turn)
    val k = if (all.isEmpty()) 0 else ((turn % all.size) + all.size) % all.size
    val swipe = remember { Animatable(0f) }; val scope = androidx.compose.runtime.rememberCoroutineScope()
    val p = all.getOrNull(k)
    val last = remember { arrayOfNulls<E.Prob>(1) }   // still shown while it slides away
    if (p != null) last[0] = p
    // Up next is there but quiet ("kinda hidden while also being visible"): one line, its pill and words faded back
    val quiet by animateFloatAsState(if (p?.k == "next") .7f else 1f, tween(300), label = "quiet")
    AnimatedVisibility(p != null, enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(), exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()) {
        Column(Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally) {
            // on frosted glass, as the seek bar's track: a soft white pill with a light edge (no blur of its own: what's
            // under it is the blurred background already); it grows and shrinks to each notice
            val shape = RoundedCornerShape(if (row) 14.dp else 18.dp)
            // tapped: its detail screen (what's wrong, what to do, OK), growing out of it; pressed, it gives a little
            val press = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            Box(Modifier.padding(top = if (row) 2.dp else 6.dp, bottom = 4.dp).padding(horizontal = 8.dp).widthIn(max = 560.dp).pressScale(press, .96f)
                .graphicsLayer { alpha = quiet }
                .shadow(8.dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = .4f), spotColor = Color.Black.copy(alpha = .5f))
                .clip(shape).liquidFrost(radius = if (row) 14.dp else 18.dp)
                .then(if (S.low) Modifier.background(Color(0xFF101018).copy(alpha = .7f)) else Modifier)   // (no frost: it's over the lyrics)
                .drawBehind { glassBody(0f) }.border(1.dp, GlassRim, shape)
                .then(if (!many) Modifier else Modifier.pointerInput(Unit) {
                    // byHand already at the start: a turn by itself under the finger made the swipe land back on the same one
                    detectHorizontalDragGestures(onDragStart = { byHand = true }, onDragEnd = {
                        scope.launch {
                            val far = kotlin.math.abs(swipe.value) > size.width * .18f
                            if (far) { dir = if (swipe.value < 0) 1 else -1; turn += dir; byHand = true; swipe.snapTo(0f) }
                            else swipe.animateTo(0f, spring(.8f, Spring.StiffnessMediumLow))
                        }
                    }, onDragCancel = { scope.launch { swipe.animateTo(0f) } }) { ch, dx -> ch.consume(); scope.launch { swipe.snapTo(swipe.value + dx) } }
                })
                .quietTouch().clickable(press, androidx.compose.material3.ripple()) { last[0]?.let { E.haptic(); E.noticeOpen = it } }
                .animateContentSize(if (Pop.calm) tween(0) else spring(.8f, Spring.StiffnessMediumLow))) {
                // a different one cross-fades in; a countdown in the same one just counts
                AnimatedContent(last[0], Modifier.graphicsLayer { translationX = swipe.value; alpha = 1f - (kotlin.math.abs(swipe.value) / size.width.coerceAtLeast(1f)).coerceAtMost(1f) * .8f }, contentKey = { it?.let { q -> q.id + q.t } }, contentAlignment = Alignment.Center, transitionSpec = {
                    if (Pop.calm) fadeIn(tween(200)) togetherWith fadeOut(tween(120))
                    else if (dir != 0) (fadeIn(tween(200)) + androidx.compose.animation.slideInHorizontally(tween(240)) { w -> dir * w }) togetherWith (fadeOut(tween(160)) + androidx.compose.animation.slideOutHorizontally(tween(240)) { w -> -dir * w })
                    else (fadeIn(tween(260)) + slideInVertically(spring(.7f, Spring.StiffnessMediumLow)) { it / 2 } + scaleIn(spring(.6f, Spring.StiffnessMediumLow), initialScale = .92f)) togetherWith (fadeOut(tween(160)) + scaleOut(tween(160), targetScale = .96f))
                }, label = "notice") { q ->
                    // words alone, in the middle of the glass: OK and what fixes it are on the screen tapping opens
                    val one = row || q?.k == "next"
                    Row(Modifier.padding(start = if (one) 8.dp else 12.dp, end = 16.dp, top = if (one) 4.dp else 8.dp, bottom = if (one) 4.dp else 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (q != null) NoticeIcon(q.k, if (one) 22.dp else 28.dp, q.img)
                        Column(Modifier.weight(1f, fill = false).padding(start = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) { if (q != null) NoticeLines(q, one) }
                    }
                }
            }
            if (all.size > 1) Row(Modifier.padding(bottom = if (row) 2.dp else 4.dp)) {
                all.indices.forEach { i ->
                    val on by animateFloatAsState(if (i == k) 1f else 0f, tween(300), label = "dot")
                    Box(Modifier.padding(horizontal = 3.dp).size(width = 5.dp + 7.dp * on, height = 5.dp).clip(CircleShape).background(Color.White.copy(alpha = .3f + .6f * on)))
                }
            }
        }
    }
}

/** What kind of notice it is at a glance (Material 3 Expressive): broken (red), waiting (yellow) or good news (white), its
 *  icon on a soft round tile of its colour. */
@Composable
fun NoticeIcon(k: String, size: Dp = 28.dp, img: String? = null) {
    if (k == "next") { NoticeCover(img, size); return }   // Up next: the song's cover
    val (icon, tint) = when (k) {
        "err" -> Icons.Rounded.ErrorOutline to Color(0xFFFFB4AB)
        "ok" -> Icons.Rounded.CheckCircle to Color.White
        else -> Icons.Rounded.HourglassTop to Color(0xFFFFD27A)
    }
    Box(Modifier.size(size).clip(CircleShape).background(tint.copy(alpha = .16f)).border(1.dp, tint.copy(alpha = .28f), CircleShape), contentAlignment = Alignment.Center) {   // (glass: its light edge)
        Icon(icon, null, Modifier.size(size * .62f), tint = tint)
    }
}

/** What's wrong, in red (broken) or yellow (waiting), or good news in white: words alone (OK, and what fixes it, are
 *  on its detail screen). On its side, where the room is the left column's: one text of two lines at most. */
@Composable
fun NoticeLines(p: E.Prob, row: Boolean = false, live: Boolean = false) {
    val tint = when (p.k) { "err" -> Color(0xFFFFB4AB); "ok", "next" -> Color.White; else -> Color(0xFFFFD27A) }   // red broken, white good news (and Up next), yellow waiting
    val act: @Composable () -> Unit = {}
    if (p.k == "next") {   // Up next: "UP NEXT  Song · Artist" in one quiet line, beside the song's cover
        val words = androidx.compose.ui.text.buildAnnotatedString {
            withStyle(androidx.compose.ui.text.SpanStyle(color = dim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)) { append("UP NEXT  ") }
            withStyle(androidx.compose.ui.text.SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) { append(p.t) }
            if (p.s.isNotEmpty()) withStyle(androidx.compose.ui.text.SpanStyle(color = dim, fontSize = 12.sp)) { append("  ·  " + p.s) }
        }
        if (S.b("aMarquee") && !S.low && !S.calm) NoticeText(words, 13.sp, live = live)
        else Text(words, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        return
    }
    // each line scrolls across the pill when it's too long, instead of wrapping (0.46.1); with Scroll long names off, or in Low
    // performance mode, they wrap as before
    if (S.b("aMarquee") && !S.low && !S.calm) {
        if (row) NoticeText(androidx.compose.ui.text.buildAnnotatedString {
            withStyle(androidx.compose.ui.text.SpanStyle(color = tint, fontWeight = FontWeight.Bold)) { append(p.t) }
            if (p.s.isNotEmpty()) withStyle(androidx.compose.ui.text.SpanStyle(color = dim, fontSize = 12.sp)) { append("  ·  " + p.s) }
        }, 13.sp, live = live)
        else {
            NoticeText(androidx.compose.ui.text.AnnotatedString(p.t), 13.sp, FontWeight.Bold, tint, live)
            if (p.s.isNotEmpty()) NoticeText(androidx.compose.ui.text.AnnotatedString(p.s), 12.sp, color = dim, live = live)
        }
        return
    }
    if (row) Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        // one text: the title whole first, the detail after it cut short if the room runs out
        Text(androidx.compose.ui.text.buildAnnotatedString {
            withStyle(androidx.compose.ui.text.SpanStyle(color = tint, fontWeight = FontWeight.Bold)) { append(p.t) }
            if (p.s.isNotEmpty()) withStyle(androidx.compose.ui.text.SpanStyle(color = dim, fontSize = 12.sp)) { append("  ·  " + p.s) }
        }, Modifier.weight(1f, fill = false), fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        act()
    } else {
        Text(p.t, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = tint, maxLines = if (p.s.isEmpty()) 2 else 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        if (p.s.isNotEmpty()) Text(p.s, fontSize = 12.sp, color = dim, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        act()
    }
}

/** One line of a notice: too long for the pill, it scrolls across it, out into the pill's padding and fading there. */
@Composable
private fun NoticeText(text: androidx.compose.ui.text.AnnotatedString, size: androidx.compose.ui.unit.TextUnit, weight: FontWeight? = null, color: Color = Color.Unspecified, live: Boolean = false) {
    var maxW by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var textW by remember(text) { androidx.compose.runtime.mutableIntStateOf(0) }
    val over = maxW > 0 && textW > maxW
    // ([live]: in Settings' header, on top: it slides there too; the player's rest under Settings)
    val iter = if (live || E.settingsPage == null) Int.MAX_VALUE else 0
    val key = text.text
    Text(text, Modifier.layout { m, c -> if (c.hasBoundedWidth && c.maxWidth != maxW) maxW = c.maxWidth; val pl = m.measure(c); layout(pl.width, pl.height) { pl.place(0, 0) } }
        .then(if (over) Modifier.edgeMarquee(0.dp, 16.dp, 12.dp, 12.dp, iter, onRead = { ms -> E.readMs[key] = ms }) else Modifier.pingPong(iter)),   // (not into the icon at its start)
        fontSize = size, fontWeight = weight, color = color, maxLines = 1, softWrap = false, textAlign = TextAlign.Center, onTextLayout = { textW = it.size.width })
}

@Composable
private fun ExtraBars(modifier: Modifier) {
    AnimatedVisibility(!Audio.away, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) { BarsView(modifier) }
}

/** Where the bars were on the Visualizer: why they're gone (the music plays elsewhere), and how they could hear it. */
@Composable
private fun AwayNote(modifier: Modifier) {
    AnimatedVisibility(Audio.away, modifier, enter = fadeIn(tween(600)), exit = fadeOut()) {
        val where = when (E.src) { "spotify" -> Spotify.device?.name ?: "another device"; else -> "another device" }
        Column(Modifier.widthIn(max = 360.dp).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Playing on $where", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("The bars only hear this phone. Pick the microphone if the music plays in the room.", Modifier.padding(top = 4.dp),
                fontSize = 13.sp, color = dim, textAlign = TextAlign.Center)
            TextButton(onClick = { E.haptic(); E.openSheet("audio") }) { Text("What the bars hear") }
        }
    }
}

/** The foot of every portrait screen: the song, the seek bar and where the lyrics are from. They stay when the
 *  controls slide away. */
@Composable
private fun SongInfo(big: Boolean, note: Boolean = false, toggles: Boolean = false, lyrics: Boolean = false, name: Boolean = true, restCover: Boolean = false, seek: Boolean = true) {
    Column(Modifier.fillMaxWidth()) {
        if (name) TitleRow(big = big, toggles = toggles, bleed = true, cover = lyrics && S.b("aLyrCover") && !restCover)
        else if (toggles && (Lyr.needTr || Lyr.needRom)) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (Lyr.needTr) SmallToggle(VIcons.translate, "Translate lyrics", S.tr) { toggleTr() }
            if (Lyr.needRom) SmallToggle(null, "Romanize lyrics", S.romMode != "off", text = "あa") { toggleRom() }
        }
        if (note) NoLyricsNote(Modifier.fillMaxWidth())
        if (seek) { Spacer(Modifier.height(if (big) 10.dp else 6.dp)); SeekRow() }
        val c = Lyr.credit
        // on the Lyrics screen, Refined credits under the last line say it already while they're on screen (a break's
        // cover over the lyrics hides them; with the Canvas and no cover, or on its side, the lyrics stay and so do they)
        val v = Lyr.view
        val under by remember { derivedStateOf { v.creditsShown() } }
        val refined = lyrics && S.credPos == "lyrics" && S.b("xRefCred") && !restCover &&
            (if (S.xFocus && !v.isStatic) v.allsung else v.allsung || v.isStatic || under)
        AnimatedVisibility(c != null && Lyr.lines > 0 && !refined, enter = fadeIn(tween(600)) + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            if (c != null) CreditsLine(c, Modifier.fillMaxWidth().padding(bottom = 4.dp))
        }
    }
}

@Composable
private fun SmallToggle(icon: ImageVector?, desc: String, on: Boolean, text: String? = null, onClick: () -> Unit) {
    val bg by androidx.compose.animation.animateColorAsState(if (on) Color.White else GlassFill, tween(220), label = "tglbg")   // off: frosted glass
    val fg by androidx.compose.animation.animateColorAsState(if (on) Color(0xFF111116) else Color.White, tween(220), label = "tglfg")
    val pop = remember { Animatable(1f) }
    LaunchedEffect(on) { if (!S.low && !E.reduceMotion() && pop.value == 1f) { pop.snapTo(.82f); pop.animateTo(1f, spring(.45f, Spring.StiffnessMedium)) } }
    // like the seek bar: tapped while the controls are hidden, it only does its job (they stay away)
    IconButton(onClick = { E.haptic(); onClick() }, Modifier.quietTouch().graphicsLayer { scaleX = pop.value; scaleY = pop.value }.frosted(inset = 4.dp)
        .semantics { contentDescription = desc; selected = on },
        colors = IconButtonDefaults.iconButtonColors(containerColor = bg, contentColor = fg)) {
        if (icon != null) Icon(icon, null, Modifier.size(20.dp)) else Text(text ?: "", fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MoreButton() {
    IconButton(onClick = { E.haptic(); E.openSettings() }, Modifier.semantics { contentDescription = "Settings" }) { Icon(Icons.Rounded.MoreVert, null, tint = Color.White) }
}

@Composable
private fun TitleRow(big: Boolean, center: Boolean = false, toggles: Boolean = false, like: Boolean = !center, bleed: Boolean = false, cover: Boolean = false,
                     header: Boolean = false, coverSlot: ((androidx.compose.ui.geometry.Rect) -> Unit)? = null, textAway: Float = 0f) {
    val ver = E.trackVer
    // [cover]: the Lyrics screen upright, where the big cover only shows in long breaks: a small one beside the name, so
    // it's always in sight; it steps aside while the big one shows
    val ck by animateFloatAsState(if (cover) 1f else 0f, if (S.low || E.reduceMotion()) tween(0) else spring(.85f, Spring.StiffnessMediumLow), label = "smallcover")
    // [bleed] (upright, in the 24 dp margins): long names scroll out to the screen's sides, fading out there; on the right
    // only when nothing's beside them (translate, romanize, the heart); on the left not past the small cover
    val busyRight = (toggles && (Lyr.needTr || Lyr.needRom)) || (E.src == "spotify" && like && S.b("aLike"))
    val bl = if (bleed && ck < .01f) 24.dp else 0.dp; val br = if (bleed && !busyRight) 24.dp else 0.dp
    val a = remember { Animatable(1f) }
    LaunchedEffect(ver) { if (!S.low && !E.reduceMotion()) { a.snapTo(0f); a.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) } }
    Row(Modifier.fillMaxWidth().graphicsLayer { alpha = a.value; translationY = (1 - a.value) * 10f * density }, verticalAlignment = Alignment.CenterVertically) {
        if (ck > .01f) {
            // [header]: the Lyrics screen's song at the top, its big cover drawn by MovingCover at the place kept here
            if (header && coverSlot != null) Box(Modifier.size(headerCoverSize() * ck).onGloballyPositioned { coverSlot(it.boundsInRoot()) })
            else Box(Modifier.size(52.dp * ck).graphicsLayer { alpha = ck.coerceIn(0f, 1f) }) { CoverArt(Modifier.fillMaxSize(), corner = 10.dp, small = true) }
            Spacer(Modifier.width((if (header) 16.dp else 12.dp) * ck))
        }
        // [header]: the buttons on the name's line and the heart on the artist's (the owner's sketch), so the artist and Up next
        // have the column's whole width; the name and the artist still slide in step
        if (header) {
            Column(Modifier.weight(1f).graphicsLayer { alpha = (1f - textAway).coerceIn(0f, 1f) }) {
                androidx.compose.runtime.CompositionLocalProvider(LocalSlideGroup provides remember { SlideGroup() }) {
                    val k = when (S.str("aTitle")) { "s" -> .85f; "l" -> 1.18f; else -> 1f }   // Settings → Player screen → Song name size
                    // (they slide out right up to the cover's edge, fading there: from the column's own edge they faded 16 dp short
                    // of it, the owner's screenshot)
                    val hl = if (ck > .01f) 16.dp * ck else bl
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { Scrolling(E.title, 24f.times(k).sp, FontWeight.Bold, Color.White, (-.3).sp, hl, if (toggles && (Lyr.needTr || Lyr.needRom)) 0.dp else br) }
                        if (toggles) {
                            if (Lyr.needTr) SmallToggle(VIcons.translate, "Translate lyrics", S.tr) { toggleTr() }
                            if (Lyr.needRom) SmallToggle(null, "Romanize lyrics", S.romMode != "off", text = "あa") { toggleRom() }
                        }
                    }
                    val by = E.byLine()
                    val heart = E.src == "spotify" && like && S.b("aLike")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { if (by.isNotEmpty()) Scrolling(by, 16f.times(k).sp, FontWeight.SemiBold, dim, bleedL = hl, bleedR = if (heart) 0.dp else br) }
                        if (heart) LikeButton()
                    }
                }
                if (S.b("aUpNext")) UpNextLine()
            }
            return@Row
        }
        // the name and the artist slide in step (Spicy Lyrics 6.5.0's TextScroller): they leave together, the shorter waits
        // at its end, and they come back together
        Column(Modifier.weight(1f), horizontalAlignment = if (center) Alignment.CenterHorizontally else Alignment.Start) {
            androidx.compose.runtime.CompositionLocalProvider(LocalSlideGroup provides remember { SlideGroup() }) {
                val k = when (S.str("aTitle")) { "s" -> .85f; "l" -> 1.18f; else -> 1f }   // Settings → Player screen → Song name size
                Scrolling(E.title, (if (big) 27f else 19f).times(k).sp, FontWeight.Bold, Color.White, (-.3).sp, bl, br)
                val by = E.byLine()
                if (by.isNotEmpty()) Scrolling(by, (if (big) 19f else 15f).times(k).sp, FontWeight.Medium, dim, bleedL = bl, bleedR = br)
            }
        }
        if (toggles) {   // the Lyrics screen: translate and romanize, when the lyrics need them
            if (Lyr.needTr) SmallToggle(VIcons.translate, "Translate lyrics", S.tr) { toggleTr() }
            if (Lyr.needRom) SmallToggle(null, "Romanize lyrics", S.romMode != "off", text = "あa") { toggleRom() }
        }
        if (E.src == "spotify" && like && S.b("aLike")) LikeButton()
    }
}

/** A name that slides to its end and back when it's too long for its row, fading out at both sides (it rests under Settings). */
@Composable
private fun Scrolling(text: String, size: androidx.compose.ui.unit.TextUnit, weight: FontWeight, color: Color, spacing: androidx.compose.ui.unit.TextUnit = 0.sp,
                      bleedL: Dp = 0.dp, bleedR: Dp = 0.dp) {
    var boxW by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var textW by remember(text) { androidx.compose.runtime.mutableIntStateOf(0) }
    val over = boxW > 0 && textW > boxW
    if (!S.b("aMarquee") || S.low || S.calm) {   // Settings → Player screen → Scroll long names, off (or Low performance mode, or Motion sickness): it ends in "…"
        Text(text, style = TextStyle(shadow = textShadow), fontSize = size, fontWeight = weight, letterSpacing = spacing, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
        return
    }
    // too long: it slides within its own row, or out to the screen's side where [bleedL] / [bleedR] say nothing else is
    // there; never over what's beside it (it slid 20 dp over the small cover, and in landscape past the cover's edge).
    // Each side fades while there's more of the name past it; at rest it starts where it always does, its first letter
    // whole. A name that fits isn't moved (a centred one would sit off centre)
    val iter = if (E.settingsPage == null) Int.MAX_VALUE else 0
    val scroll = if (over) Modifier.edgeMarquee(bleedL, bleedR, 14.dp, if (bleedR > 0.dp) 18.dp else 24.dp, iter)
        else Modifier.pingPong(iter)
    Box(Modifier.onSizeChanged { boxW = it.width }) {
        Text(text, scroll, style = TextStyle(shadow = textShadow),
            fontSize = size, fontWeight = weight, letterSpacing = spacing, color = color, maxLines = 1, onTextLayout = { textW = it.size.width })
    }
}

/** Up next in the song at the top of the upright Lyrics screen (the owner's pick: there nothing moves for it): a small line
 *  under the artist, the next song's cover and name, in a song's last 30 s. Its room is always kept, empty otherwise, so the
 *  name doesn't shift when it comes. */
@Composable
private fun UpNextLine(center: Boolean = false) {
    val nx = Preload.shown?.takeIf { !E.inPip && !E.ambient }
    var last by remember { mutableStateOf<io.github.bozkow.vibevisualizer.lyrics.SongMeta?>(null) }
    if (nx != null) last = nx
    Box(Modifier.fillMaxWidth().height(24.dp).padding(top = 4.dp), contentAlignment = if (center) Alignment.Center else Alignment.CenterStart) {
        androidx.compose.animation.AnimatedVisibility(nx != null, enter = fadeIn(tween(400)), exit = fadeOut(tween(250))) {
            val m = last ?: return@AnimatedVisibility
            Row(verticalAlignment = Alignment.CenterVertically) {
                NoticeCover(m.cover.ifEmpty { null }, 18.dp)
                Spacer(Modifier.width(6.dp))
                androidx.compose.runtime.CompositionLocalProvider(LocalSlideGroup provides remember { SlideGroup() }) {
                    Scrolling("Up next: " + m.title, 13.sp, FontWeight.SemiBold, Color.White.copy(alpha = .75f))
                }
            }
        }
    }
}

/** A song's cover as a notice's icon (Up next), rounded; a note while it loads or when the song has none. */
@Composable
fun NoticeCover(url: String?, size: Dp) {
    val bm by produceState(url?.let { Images.cached("$it@128") }, url) { value = url?.let { Images.load(it, 128) } }
    Box(Modifier.size(size).clip(RoundedCornerShape(size * .22f)).background(GlassFill).border(1.dp, GlassEdge, RoundedCornerShape(size * .22f)), contentAlignment = Alignment.Center) {
        val b = bm
        if (b != null) Image(b.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(Icons.Rounded.MusicNote, null, Modifier.size(size * .6f), tint = Color.White.copy(alpha = .7f))
    }
}

@Composable
private fun LikeButton() {
    Spotify.version
    val liked = Spotify.liked == true
    val pop = remember { Animatable(1f) }
    LaunchedEffect(liked) { if (liked && !S.low && !E.reduceMotion()) { pop.snapTo(.6f); pop.animateTo(1f, spring(.35f, Spring.StiffnessMedium)) } }
    // the icon's edge lines up with the title's right edge (the touch area reaches past it)
    val ok = Spotify.meta?.ep == false
    Box(Modifier.offset(x = 12.dp).size(48.dp).clip(CircleShape).quickClick(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, ok, androidx.compose.material3.ripple()) { E.haptic(); Spotify.toggleLike() }
        .semantics { contentDescription = if (liked) "Remove from Liked Songs" else "Save to Liked Songs" }, contentAlignment = Alignment.Center) {
        Icon(if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null, Modifier.size(28.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value },
            tint = (if (liked) Color(0xFF1ED760) else Color.White).let { if (ok) it else it.copy(alpha = .38f) })
    }
}

/** Shuffle, previous, play / pause (its shape morphs), next, repeat. */
@Composable
private fun Transport(big: Boolean, mid: Boolean = false) {
    Spotify.version
    val sp = E.src == "spotify"
    val main = if (big) 56.dp else if (mid) 50.dp else 42.dp
    Row(Modifier.fillMaxWidth().padding(vertical = if (big) 6.dp else 0.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        if (sp) CtlIcon(Icons.Rounded.Shuffle, "Shuffle", size = 24.dp, enabled = Spotify.spState != null && "toggling_shuffle" !in Spotify.disallows, on = Spotify.shuffle, dot = true) { Spotify.toggleShuffle() }
        else CtlIcon(if (Local.muted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp, if (Local.muted) "Unmute" else "Mute", enabled = E.src == "file") { Local.muted = !Local.muted }
        CtlIcon(Icons.Rounded.FastRewind, "Previous", size = main, enabled = E.src != "none", nudge = -1) { E.prev() }
        PlayButton(if (big) 76.dp else if (mid) 68.dp else 58.dp)
        CtlIcon(Icons.Rounded.FastForward, "Next", size = main, enabled = E.canNext(), nudge = 1) { E.next() }
        if (sp) {
            val r = Spotify.repeat
            CtlIcon(if (r == "track") Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, "Repeat: " + when (r) { "context" -> "all"; "track" -> "this song"; else -> "off" },
                size = 24.dp, enabled = Spotify.spState != null, on = r != "off", dot = true) { Spotify.cycleRepeat() }
        } else Box(Modifier.size(48.dp))
    }
}

/** The music app's bottom row: five buttons you pick (Settings → Player screen → Buttons under the controls; by
 *  default where it plays · the bars · Lyrics timing · Resync · Look again), then Settings, always last: it's the way into them. */
@Composable
private fun BottomBar(big: Boolean = true, mid: Boolean = false, lyricsToggles: Boolean = false) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
    // six that fit with room between them (a narrow phone, or the largest display size), and round when the row gets less
    // height than they'd like (on its side: the large song name, a notice of two lines and the controls together)
    val d = minOf(if (big) 54.dp else if (mid) 50.dp else 44.dp, (maxWidth - 40.dp) / 6, (maxHeight - (if (big) 10.dp else 6.dp)).coerceAtLeast(0.dp))
    Row(Modifier.fillMaxWidth().padding(top = if (big) 6.dp else 2.dp, bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        // (Lyrics and Visualizer were screens of their own until 0.38.0: a slot saved with Lyrics turns the bars on and
        // off now, one with the Visualizer opens Lyrics timing, so the old default row has no button twice)
        val picks = (1..5).map { i -> S.str("aBar$i").let { k -> when (k) { "lyrics" -> "bars"; "viz" -> "timing"; else -> k } } }
        // [lyricsToggles]: the upright Lyrics screen, whose song at the top has no room for them (the owner's pick, as Spicy
        // Lyrics Mobile has them): translate and romanize first when the lyrics need them, the last picked ones giving way,
        // so the row keeps its six
        val first = if (!lyricsToggles) emptyList() else listOfNotNull("tr".takeIf { Lyr.needTr && "tr" !in picks }, "rom".takeIf { Lyr.needRom && "rom" !in picks })
        for (k in (first + picks).take(5)) BarButton(k, d)
        BarIcon(Icons.Rounded.Settings, "Settings", false, d) { E.openSettings() }
    }
    }
}

/** One of the bottom row's buttons you pick (the keys of BAR_CHOICES in Settings). */
@Composable
private fun BarButton(k: String, d: Dp) {
    val sp = E.src == "spotify"
    when (k) {
        "where" -> if (sp) BarIcon(Icons.Rounded.Cast, Spotify.device?.let { "Playing on ${it.name}" } ?: "Choose where Spotify plays", false, d) { E.openSheet("devices") }
            else BarIcon(Icons.Rounded.GraphicEq, "What the bars hear", false, d) { E.openSheet("audio") }
        "audio" -> BarIcon(Icons.Rounded.GraphicEq, "What the bars hear", false, d) { E.openSheet("audio") }
        "bars" -> BarIcon(VIcons.bars, "The bars", S.view == "both", d) { S.set("view", if (S.view == "both") "lyrics" else "both") }
        "timing" -> BarIcon(Icons.Rounded.Timer, "Lyrics timing", false, d) { E.openSheet("timing") }
        "tr" -> BarIcon(VIcons.translate, "Translate lyrics", S.tr, d) { toggleTr() }
        "rom" -> BarIcon(null, "Romanize lyrics", S.romMode != "off", d, text = "あa") { toggleRom() }
        "canvas" -> BarIcon(Icons.Rounded.Movie, "Canvas", S.canvasOn, d) { S.set("canvasOn", !S.canvasOn) }
        "like" -> {
            Spotify.version
            val liked = sp && Spotify.liked == true
            BarIcon(if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, if (liked) "Remove from Liked Songs" else "Save to Liked Songs", liked, d,
                enabled = sp && Spotify.meta?.ep == false) { Spotify.toggleLike() }
        }
        "addlyr" -> BarIcon(Icons.AutoMirrored.Rounded.NoteAdd, "Add a lyrics file for this song", false, d) { FilePicker.lyrics?.invoke(true) }
        "songs" -> BarIcon(Icons.Rounded.LibraryMusic, "Play songs from your phone", false, d) { FilePicker.pickAudio() }
        "again" -> BarIcon(Icons.Rounded.YoutubeSearchedFor, "Look up the lyrics again", false, d) { Lyr.clearSong() }
        "rotate" -> {
            // tap: turned on its side (or back upright), and it stays so whatever the phone's tilt; hold: follows the phone again
            val act = androidx.activity.compose.LocalActivity.current
            val side = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            BarIcon(Icons.Rounded.ScreenRotation, if (rotLock) "Rotate the screen (hold: follow the phone)" else "Rotate the screen", rotLock, d,
                onLongClick = { act?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED; rotLock = false }) {
                act?.requestedOrientation = if (side) android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                rotLock = act != null
            }
        }
        "none" -> Spacer(Modifier.size(d))
        else -> BarIcon(VIcons.resync, "Resync the lyrics", false, d) { Lyr.resync() }
    }
}

/** The rotate button turned the screen and holds it there (until it's held, or the app starts again). */
private var rotLock by mutableStateOf(false)

/** A round outlined button (filled softly while it's the screen you're on). */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun BarIcon(icon: ImageVector?, desc: String, on: Boolean, d: Dp = 54.dp, text: String? = null, enabled: Boolean = true, onLongClick: (() -> Unit)? = null, onClick: () -> Unit) {
    val src = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val k by animateFloatAsState(if (on) 1f else 0f, tween(260), label = "baron")   // the highlight fades between screens
    val a by animateFloatAsState(if (enabled) 1f else .4f, tween(220), label = "barok")
    // on: filled white with a dark icon, as the translate and romanize buttons, so on and off can't be mistaken
    val fg = androidx.compose.ui.graphics.lerp(Color.White, Color(0xFF111116), k)
    // off: the glass (0.55.0: the cover's colour, a sheen and a rim that catches the light, over the frost behind it)
    // (Liquid glass, the experiment: what's behind through a lens, clear)
    Box(Modifier.size(d).alpha(a).pressScale(src).clip(CircleShape).liquidFrost().drawBehind { glassBody(0f, 1f - k) }.background(Color.White.copy(alpha = .9f * k))
        .border(1.dp, GlassRim, CircleShape)
        .quickClick(src, enabled, androidx.compose.material3.ripple(), onHold = onLongClick?.let { h -> { E.haptic(); h() } }) { E.haptic(); onClick() }
        .semantics { contentDescription = desc; selected = on }, contentAlignment = Alignment.Center) {
        if (icon != null) Icon(icon, null, Modifier.size(d * .42f), tint = fg)
        else Text(text ?: "", fontSize = (d.value * .3f).sp, fontWeight = FontWeight.Bold, color = fg)
    }
}

@Composable
private fun CtlIcon(icon: ImageVector, desc: String, size: Dp = 24.dp, enabled: Boolean = true, on: Boolean = false, nudge: Int = 0, dot: Boolean = false, onClick: () -> Unit) {
    val src = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val tint by androidx.compose.animation.animateColorAsState(if (on) MaterialThemeAccent() else Color.White, tween(220), label = "ctlon")   // shuffle / repeat light up softly
    // [dot]: a toggle (shuffle, repeat) has a small dot under it while it's on, popping in and shrinking away
    val dk by animateFloatAsState(if (dot && on) 1f else 0f, if (Pop.calm) tween(0) else spring(.5f, Spring.StiffnessMedium), label = "ctldot")
    // previous / next ([nudge] -1 / 1): the arrows slide out the way you skip, fading, and spring back in from the other side
    val k = remember { Animatable(0f) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    // (a round button as IconButton is, but only a quick tap counts: quickClick)
    Box(Modifier.size(maxOf(48.dp, size + 12.dp)).pressScale(src, .86f).clip(CircleShape)
        .quickClick(src, enabled, androidx.compose.material3.ripple()) {
            E.haptic(); onClick()
            if (nudge != 0 && !Pop.calm) scope.launch { k.snapTo(0f); k.animateTo(1f, tween(130, easing = androidx.compose.animation.core.FastOutLinearInEasing)); k.snapTo(-1f); k.animateTo(0f, spring(.62f, 520f)) }
        }
        .semantics { contentDescription = desc; if (dot) selected = on }, contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(size).graphicsLayer { val x = k.value; translationX = x * nudge * size.toPx() * .38f; alpha = 1f - kotlin.math.abs(x) * .9f }
            .drawWithContent { drawContent(); if (dk > .01f) drawCircle(tint, 2.2.dp.toPx() * dk.coerceAtMost(1.2f), Offset(this.size.width / 2, this.size.height + 5.dp.toPx()), alpha = dk.coerceIn(0f, 1f)) }, tint = if (enabled) tint else Color.White.copy(alpha = .3f))
    }
}

@Composable private fun MaterialThemeAccent() = androidx.compose.material3.MaterialTheme.colorScheme.primary

/** Play / pause: big and round; while music plays it morphs into a soft cookie shape (Material 3 Expressive). */
@Composable
private fun PlayButton(size: Dp) {
    val playing = E.playingState
    val pop = remember { Animatable(1f) }
    LaunchedEffect(playing) { if (!S.low && !E.reduceMotion()) { pop.snapTo(.86f); pop.animateTo(1f, spring(.45f, Spring.StiffnessMedium)) } }
    val src = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Box(Modifier.size(size).pressScale(src, .88f).clip(CircleShape).quickClick(src, indication = androidx.compose.material3.ripple()) { E.haptic(); E.toggle() }.semantics { contentDescription = if (playing) "Pause" else "Play" }, contentAlignment = Alignment.Center) {
        // play and pause swap with a quick scale and fade
        // play and pause swap with a quarter turn, a springy scale and a fade
        AnimatedContent(playing, transitionSpec = {
            if (Pop.calm) fadeIn(tween(120)) togetherWith fadeOut(tween(100))
            else (fadeIn(tween(140)) + scaleIn(spring(.55f, 500f), initialScale = .45f)) togetherWith (fadeOut(tween(110)) + scaleOut(tween(150), targetScale = .45f))
        }, label = "playicon") { p ->
            val turn by transition.animateFloat(transitionSpec = { spring(.6f, 420f) }, label = "playturn") { st ->
                if (Pop.calm) 0f else when (st) { androidx.compose.animation.EnterExitState.PreEnter -> -90f; androidx.compose.animation.EnterExitState.PostExit -> 90f; else -> 0f }
            }
            Icon(if (p) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, Modifier.size(size).graphicsLayer { scaleX = pop.value; scaleY = pop.value; rotationZ = turn }, tint = Color.White)
        }
    }
}

/** The seek bar: a wave while music plays (as Android's own media controls), the time it would jump to while dragging. */
@Composable
private fun SeekRow(inline: Boolean = false) {
    var drag by remember { mutableStateOf<Float?>(null) }
    // where the song is, worked out as the bar draws, on the app's own frames (E.drawTick): smooth, and never a frame of its
    // own (its 260 ms glide towards a target that moved every 240 ms never ended, and kept the app drawing at 120 Hz)   // under Settings' blur it steps (a glide re-blurs the screen every frame)
    // read by the bar as it draws: the glide repaints the bar alone instead of rebuilding this whole row every frame
    val frac = { drag ?: if (E.seeking) E.seekFrac else { E.drawTick.longValue; val d = E.curDur(); if (d > 0) (E.curTime() / d).toFloat().coerceIn(0f, 1f) else 0f } }
    val d0 = drag   // while dragging, the times and the bubble follow the finger
    E.timeText   // the row itself is rebuilt with the time ticker (once a second or so): the times and a new song's length
    val dur = E.curDur()
    val thick = when (S.str("aSeek")) { "thin" -> 6.dp; "thick" -> 18.dp; else -> 12.dp }   // Settings → Player screen → Seek bar
    // Scrub preview (Experiments): while you drag, a bubble above your finger with the time you'd jump to, and the
    // times say the same; off, they show where the song really is until you let go
    val scrub = S.b("xScrub")
    // frosted: it lies right on the cover, the video or the lyrics
    var at by remember { mutableStateOf(Offset.Zero) }
    val paintFrost = if (LocalFrost.current) frostPainter() else null
    val under: (androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit)? = if (paintFrost != null) ({ paintFrost(this, at) }) else null
    val bar = @Composable { m: Modifier ->
        Box(m) {
            ThickSeek(frac, thick = thick, onDrag = { f -> drag = f; E.seekDrag(true) }, onDone = { f -> if (drag == null) E.seekDrag(true); drag = null; E.seekFraction(f); E.seekDrag(false) }, enabled = E.src != "none" && dur > 0, under = under, modifier = Modifier.quietTouch().then(if (under != null) Modifier.onGloballyPositioned { at = it.positionInWindow() } else Modifier))
            if (scrub) ScrubTip(d0 != null, d0 ?: 0f, fmtTime((d0 ?: 0f) * dur))
        }
    }
    // the time moves on with the app's time ticker (a few times a second), not every frame
    val now = @Composable { Text(fmtTime(if (d0 != null && scrub) d0 * dur else E.curTime()), fontSize = 14.sp, color = if (d0 != null && scrub) Color.White else dim, fontWeight = FontWeight.Medium) }
    val end = @Composable { Text(if (d0 != null && scrub) "-" + fmtTime(dur - d0 * dur) else fmtTime(dur), fontSize = 14.sp, color = dim, fontWeight = FontWeight.Medium) }
    // inline: the times on either side of the bar (under a cover in landscape)
    if (inline) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { now(); Spacer(Modifier.width(12.dp)); bar(Modifier.weight(1f)); Spacer(Modifier.width(12.dp)); end() }
    else Column(Modifier.fillMaxWidth()) {
        bar(Modifier)
        Row(Modifier.fillMaxWidth()) { now(); Spacer(Modifier.weight(1f)); end() }
    }
}

/** Scrub preview (Spicy Lyrics 6.4.50): while a bar is dragged, a small bubble above the finger with what letting go
 *  would set (the time on the seek bar, the level on the volume), kept inside the bar's width. Takes no room. */
@Composable
private fun ScrubTip(show: Boolean, f: Float, text: String, inset: Dp = 0.dp) {
    val k by animateFloatAsState(if (show) 1f else 0f, tween(if (show) 140 else 220), label = "scrubtip")
    val last = remember { object { var f = 0f; var t = "" } }
    if (show) { last.f = f; last.t = text }
    if (k <= 0f) return
    Box(Modifier.fillMaxWidth().layout { m, c ->
        val p = m.measure(c.copy(minWidth = 0, minHeight = 0))
        layout(c.maxWidth, 0) {
            val ins = inset.roundToPx(); val track = (c.maxWidth - 2 * ins).coerceAtLeast(1)
            val x = (ins + last.f * track - p.width / 2f).roundToInt().coerceIn(0, maxOf(0, c.maxWidth - p.width))
            p.place(x, -p.height - 4.dp.roundToPx())
        }
    }) {
        Text(last.t, Modifier.graphicsLayer { alpha = k; val s = .8f + .2f * k; scaleX = s; scaleY = s; transformOrigin = TransformOrigin(.5f, 1f); translationY = (1 - k) * 6.dp.toPx() }
            .clip(RoundedCornerShape(10.dp)).background(Color.White).padding(horizontal = 10.dp, vertical = 4.dp),
            color = Color(0xFF111116), fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun VolumeRow() {
    val sp = E.src == "spotify"
    val dev = Spotify.device
    val canVol = !sp || (dev != null && dev.supportsVolume && dev.volume != null)
    val v = if (sp) (dev?.volume ?: 0) else S.i("volume")
    val src = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val dragged by src.collectIsDraggedAsState(); val pressed by src.collectIsPressedAsState()
    Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.AutoMirrored.Rounded.VolumeDown, null, Modifier.size(20.dp), tint = Color.White.copy(alpha = if (canVol) .7f else .3f))
        Box(Modifier.weight(1f).padding(horizontal = 6.dp)) {
            Slider(value = v / 100f, onValueChange = { f -> val x = (f * 100).toInt(); if (sp) Spotify.setVolume(x) else S.set("volume", x); E.poke() }, enabled = canVol, interactionSource = src,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = if (sp) "Spotify volume" else "Volume" },
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = .22f), disabledInactiveTrackColor = Color.White.copy(alpha = .1f)))
            if (S.b("xScrub")) ScrubTip(canVol && (dragged || pressed), v / 100f, "$v%", inset = 10.dp)   // the level it sets
        }
        Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, Modifier.size(20.dp), tint = Color.White.copy(alpha = if (canVol) .7f else .3f))
    }
}

@Composable
private fun LyricsCredits() {
    val c = Lyr.credit
    // in the corner, unless Refined credits are under the lyrics; in Focus mode those only come at the end, so the corner
    // ones show until then (where the lyrics are from is always on screen)
    val show = c != null && Lyr.lines > 0 && (S.credPos == "corner" || !S.b("xRefCred") || (S.xFocus && Lyr.lsync && !Lyr.view.allsung))
    AnimatedVisibility(show, enter = fadeIn(tween(600)), exit = fadeOut()) {
        if (c != null) CornerCredits(c, Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp), alignEnd = S.credPos == "lyrics" && S.b("lcredLast") && Lyr.L.lastOrNull { !it.dots }?.opp == true)
    }
}

/* ================= cover and Canvas ================= */

/** While a Canvas plays, the cover glides to a smaller size in the middle, so more of the Canvas shows (as the web app's
 *  cover shrinks to the middle); it grows back when there's none. Drawn smaller, not laid out again, so nothing jumps. */
@Composable
private fun Modifier.canvasShrink(): Modifier {
    val k by animateFloatAsState(if (CanvasVideo.cvmode && S.b("cvShrink")) .56f else 1f, if (S.low || E.reduceMotion()) tween(0) else spring(.85f, Spring.StiffnessVeryLow), label = "cvcover")
    return graphicsLayer { scaleX = k; scaleY = k }
}

/** The cover, in its frame (the Canvas plays behind the player, not in here). */
@Composable
fun CoverArt(modifier: Modifier, corner: Dp, small: Boolean = false, tall: Boolean = false) {
    val shape = RoundedCornerShape(corner * (S.i("aCoverR").coerceIn(0, 40) / 22f))   // Settings → Player screen → Cover corners (22: as designed)
    Box(modifier.then(if (small) Modifier else Modifier.shadow(24.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)).clip(shape).background(Color.White.copy(alpha = .08f))
        .then(if (S.b("aCoverLine")) Modifier.border(1.dp, Color.White.copy(alpha = .22f), shape) else Modifier)) {   // Settings → Player screen → Cover outline
        Cover(Modifier.fillMaxSize())
        if (!small) MotionCoverLayer(tall)
    }
}

/** Over the still cover: the album's animated cover from Apple Music, where it has one (it fades in on its first frame;
 *  none, or it fails, and the still cover is simply what shows). Never beside a Canvas: one video at a time. [tall]: a
 *  cover that grows taller for Apple's tall (3:4) stream, where the record has one (Spicy Lyrics Mobile 0.19.0). */
@Composable
private fun MotionCoverLayer(tall: Boolean = false) {
    val on = motionCoverOn()
    val artist = E.artist; val album = E.album; val title = E.title
    val url by produceState<String?>(null, on, artist, album, title, tall) {
        value = null; if (on) value = if (tall) AnimatedCover.findTall(artist, album, title) else AnimatedCover.find(artist, album, title)
    }
    val u = url ?: return
    val a by animateFloatAsState(if (AnimatedCover.ready) 1f else 0f, tween(600), label = "motioncover")
    AndroidView({ ctx -> TextureView(ctx) }, Modifier.fillMaxSize().graphicsLayer { alpha = a }, update = { AnimatedCover.attach(it, u) }, onRelease = { AnimatedCover.detach(it) })
}

private fun motionCoverOn() = S.b("aMotionCover") && !S.low && !CanvasVideo.active && !E.dj && E.src != "none" && E.title != "Nothing playing"

/** How wide the upright big cover is for its height: 1 (square), or Apple's tall shape while its tall stream plays. */
@Composable
private fun bigCoverShape(): Float {
    val tall = motionCoverOn() && AnimatedCover.ready && AnimatedCover.aspect < .9f
    val k by animateFloatAsState(if (tall) AnimatedCover.aspect.coerceIn(.6f, 1f) else 1f,
        if (S.low || E.reduceMotion()) tween(0) else spring(.85f, Spring.StiffnessLow), label = "tallcover")
    return k
}

/** The cover: a new one condenses out of a blur while the old one melts away (swells, sinks and blurs). */
@Composable
fun Cover(modifier: Modifier = Modifier) {
    val ver = Visuals.coverVer
    var cur by remember { mutableStateOf(Visuals.cover?.asImageBitmap()) }
    var prev by remember { mutableStateOf<ImageBitmap?>(null) }
    val k = remember { Animatable(1f) }
    val calm = S.low || E.reduceMotion() || Visuals.quietVer == ver
    LaunchedEffect(ver) {
        val nb = Visuals.cover?.asImageBitmap()
        if (nb === cur) return@LaunchedEffect
        prev = if (calm) null else cur; cur = nb
        k.snapTo(0f); k.animateTo(1f, tween(if (calm) 1 else 1100, easing = FastOutSlowInEasing)); prev = null
    }
    val p = k.value
    Box(modifier, contentAlignment = Alignment.Center) {
        if (cur == null && prev == null) Icon(VIcons.bars, null, Modifier.fillMaxSize(.36f), tint = Color.White.copy(alpha = .35f))
        prev?.let { Image(it, null, Modifier.fillMaxSize().graphicsLayer { alpha = 1 - p; scaleX = 1 + .14f * p; scaleY = 1 + .2f * p; translationY = size.height * .07f * p }.blur((22 * p).dp), contentScale = ContentScale.Crop) }
        cur?.let {
            val sc = if (p < .6f) .9f + (1.015f - .9f) * (p / .6f) else 1.015f - .015f * ((p - .6f) / .4f)
            Image(it, "Album cover", Modifier.fillMaxSize().graphicsLayer { scaleX = sc; scaleY = sc; alpha = if (prev == null) 1f else (p * 1.6f).coerceAtMost(1f) }.blur((16 * (1 - (p / .6f).coerceAtMost(1f))).dp), contentScale = ContentScale.Crop)
        }
    }
}

/** Every song's screen as one with a Canvas (the owner's wish): over the background without one, the blurred Canvas's grade. On a
 *  phone on its side, as the stage's backdrop there: flat, brightness .65 at the default slider. A colour you picked stays as it is. */
@Composable
private fun ScreenGrade(wide: Boolean, stage: Boolean) {
    val behindLyrics = !wide && E.screen == "lyrics" && !noLyrics()
    val behind = S.str("cvBehind")
    val dark = behind == "dark" || behind == "both" || (behind == "blur" && S.low)
    // as much of the shade as the Canvas's, which is part of its video: faint behind the lyrics
    val shade = if (!dark || wide) 0f else if (behindLyrics) S.i("cvLyrA").coerceIn(0, 80) / 100f else 1f
    val top = TopArea.bottom + with(androidx.compose.ui.platform.LocalDensity.current) { 56.dp.toPx() }
    val k by animateFloatAsState(if (CanvasVideo.cvlive || S.bgStyle == "color") 0f else 1f, tween(800), label = "grade")
    androidx.compose.runtime.SideEffect { Frost.gradeTop = top; Frost.gradeShade = shade }
    Canvas(Modifier.fillMaxSize().onSizeChanged { Frost.gradeW = it.width.toFloat(); Frost.gradeH = it.height.toFloat() }) {
        if (k <= 0f) return@Canvas
        if (stage) drawRect(Color.Black.copy(alpha = (1f - .65f * S.bri / 70f / Visuals.bgBri().toFloat()).coerceIn(0f, 1f) * k))
        else screenGrade(size.width, size.height, top, shade, k)
    }
}

/** The Canvas is the background: the video fills the screen behind the player and fades into the blurred Canvas toward
 *  the bottom, so the controls stay readable. Behind the lyrics it's dimmed right down, so the words stay sharp. On a
 *  wide screen it fills the left side and fades out to the right, as in the web app. The view is there from the start:
 *  the video's first frame (which reveals it) can only be drawn into one. */
@Composable
private fun CanvasBackdrop(wide: Boolean, centred: Boolean, stage: LandscapeStage? = null, stageStart: Dp = 0.dp) {
    val behindLyrics = !wide && E.screen == "lyrics" && !noLyrics()
    val a by animateFloatAsState(if (!CanvasVideo.cvmode) 0f else if (behindLyrics) S.i("cvLyrA").coerceIn(0, 80) / 100f else 1f, if (stage != null) tween(900, easing = androidx.compose.animation.core.CubicBezierEasing(.23f, 1f, .32f, 1f)) else tween(800), label = "canvas")
    // Spicy Lyrics' stage: a veil that deepens while the controls show
    val ctl by animateFloatAsState(if (E.uiHidden) 0f else 1f, if (S.low) tween(0) else tween(300), label = "cvctl")
    // side by side without lyrics, the video glides to the middle and fades out on both sides (moved, not laid out
    // again: the video's crop is set for its size)
    val c by animateFloatAsState(if (centred) 1f else 0f, if (S.low || E.reduceMotion()) tween(0) else tween(700, easing = FastOutSlowInEasing), label = "cvcentre")
    // behind the top bar, the song and the controls: the dark gradient, the Canvas blurred there, both or nothing
    val behind = S.str("cvBehind")
    val dark = behind == "dark" || behind == "both" || (behind == "blur" && S.low)   // Low performance: no blur, the gradient instead
    // only while the video shows (not while it loads, nor once it's gone): the whole Canvas blurred already, nothing to add
    val frost = (behind == "blur" || behind == "both") && !S.low && !S.b("cvBlur") && CanvasVideo.cvmode && !wide && !E.quiet   // side by side nothing sits on the video (the desktop's)
    // the blur reaches up to the song while the controls show, and sinks with them when they hide
    val sink by animateFloatAsState(if (E.uiHidden) 1f else 0f, tween(600, easing = FastOutSlowInEasing), label = "cvsink")
    val from = when { wide -> .52f; behindLyrics -> .6f; else -> .5f } + .12f * sink
    val d = androidx.compose.ui.platform.LocalDensity.current
    val topPx = TopArea.bottom; val rampPx = with(d) { 28.dp.toPx() }; val softPx = with(d) { 96.dp.toPx() }
    val r = with(d) { (S.i("cvBlurAmt").coerceIn(5, 100) / 100f * 48f).dp.toPx() }
    // the top's blur, shade and softening behind the top line (always there, on its side over the Canvas side). Two
    // sizes: just behind the line, or down past what's wrong under it (it follows the error space as it opens and closes)
    val topK = 1f

    // the video, recorded as it's drawn, so the background can draw it again blurred (on the GPU, see CanvasBlur)
    val vid = rememberGraphicsLayer()
    var vidSize by remember { mutableStateOf(IntSize.Zero) }
    CanvasBlur(vid, { vidSize }, wide, stage != null)
    // on its side the sharp video first blurs where it is, then fades into the blurred background (straight from sharp to the
    // blurred copy, which sits elsewhere, the two showed as an edge)
    val sideR = with(d) { 28.dp.toPx() }
    val soft = wide && stage == null && !S.low && c <= 0f && !E.quiet
    val stageW = stage?.let { stageStart + it.width.dp }; val featherPx = stage?.let { with(d) { it.feather.dp.toPx() } } ?: 0f
    // (.75: translucent, so the blurred backdrop carries through the video and the two read as one surface; Low performance mode has
    // no such backdrop, and it was the cover's colours that came through)
    Box((if (stageW != null) Modifier.fillMaxHeight().width(stageW) else if (wide) Modifier.fillMaxHeight().fillMaxWidth(.56f) else Modifier.fillMaxSize()).alpha(maxOf(a, .01f) * (if (stage != null && !S.low) .75f else 1f))
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen; if (wide) translationX = size.width * (.22f / .56f) * c }.drawWithContent {
            drawContent()
            // as dark at the top (behind the status bar and the top bar) as behind the controls at the bottom
            val tf = ((topPx + rampPx * 2) / size.height).coerceIn(.02f, .45f)
            val shade = Brush.verticalGradient(0f to Color.Black.copy(alpha = .45f * topK), tf to Color.Transparent, .55f to Color.Transparent, 1f to Color.Black.copy(alpha = .45f))
            if (stage != null) {
                // a black veil at 1 - b is the picture at brightness b, without a filter pass per frame; darker to the foot for the
                // dock (eased: a short straight ramp showed as a band; over .75, as all of this is); and a soft edge only toward
                // the lyrics and over the top, on eased stops: a straight ramp leaves a line where it reaches full strength
                val b = 1f - .45f * ctl
                drawRect(Color.Black.copy(alpha = 1f - .8f * (.45f + .55f * b)))
                drawRect(Brush.verticalGradient(0f to Color.Transparent, .45f to Color.Transparent, .6f to Color.Black.copy(alpha = .05f), .72f to Color.Black.copy(alpha = .15f), .84f to Color.Black.copy(alpha = .25f), 1f to Color.Black.copy(alpha = .35f)))
                val w = size.width; fun x(at: Float) = ((w - featherPx * at) / w).coerceIn(0f, 1f)
                drawRect(Brush.horizontalGradient(0f to Color.Black, x(1f) to Color.Black, x(.72f) to Color.Black.copy(alpha = .85f), x(.38f) to Color.Black.copy(alpha = .36f), x(.18f) to Color.Black.copy(alpha = .1f), 1f to Color.Transparent), blendMode = BlendMode.DstIn)
                drawRect(Brush.verticalGradient(0f to Color.Transparent, .09f to Color.Black.copy(alpha = .28f), .18f to Color.Black), blendMode = BlendMode.DstIn)
            } else if (wide) {
                // side by side: the shade fades out with the video (drawn after the fade it stopped in a hard line at the
                // edge), over a long, eased fade into the blurred Canvas behind
                // the desktop's: darker at the top and the foot, for the text over it
                drawRect(Brush.verticalGradient(0f to Color.Black.copy(alpha = .4f), .22f to Color.Transparent, .52f to Color.Transparent, 1f to Color.Black.copy(alpha = .7f)))
                // where the fade starts: the sharp video keeps most of its side (from .3 it took nearly half, the owner's complaint on
                // 0.43.0), a little later in the middle
                val s = .6f + .1f * c
                drawRect(Brush.horizontalGradient(0f to Color.Black, *Array(6) { i -> (s + (1 - s) * EASE_AT[i]) to Color.Black.copy(alpha = EASE_A[i]) }), blendMode = BlendMode.DstIn)
                // in the middle, the same fade on the left
                if (c > 0f) drawRect(Brush.horizontalGradient(*Array(6) { k -> val j = 5 - k; .3f * (1 - EASE_AT[j]) to Color.Black.copy(alpha = 1 - c * (1 - EASE_A[j])) }, 1f to Color.Black), blendMode = BlendMode.DstIn)
            } else {
                // the video softens into the blurred Canvas at the top too, as it does (longer) toward the controls
                val fade = arrayOf(0f to Color.Black.copy(alpha = 1 - .65f * topK), .08f to Color.Black.copy(alpha = 1 - .3f * topK), .2f to Color.Black, .45f to Color.Black, .58f to Color.Black.copy(alpha = .85f), .74f to Color.Black.copy(alpha = .5f), .88f to Color.Black.copy(alpha = .18f), 1f to Color.Transparent)
                drawRect(Brush.verticalGradient(*fade), blendMode = BlendMode.DstIn)
                if (dark) drawRect(shade)
            }
        }) {
        Box(Modifier.fillMaxSize().graphicsLayer { renderEffect = if (size.height < 1f || size.width < 1f) null else if (frost) frostBands(size.height, r, topPx / size.height, softPx / size.height, from, topK) else if (soft) sideBlur(size.width, sideR) else null }
            .drawWithContent {
                vid.record { this@drawWithContent.drawContent() }; drawLayer(vid)
                if (vid.size != vidSize) vidSize = vid.size
            }) { CanvasVideoView() }
    }
}

/** The Canvas frosted where the controls are: a blurred copy of it over the sharp one, shown through a mask that's opaque at
 *  the top (down to [top0], where the top's text ends; [topK] of it, fading while nothing's there), clear in the middle, and
 *  opaque again from a little below [from] (under the song, the seek bar and the controls). Long, eased ends, so the blur
 *  melts into the video: a short straight ramp showed where it stopped. One blur pass on the GPU. */
private fun frostBands(h: Float, r: Float, top0: Float, ramp0: Float, from: Float, topK: Float): androidx.compose.ui.graphics.RenderEffect {
    // never a range upside down (a crash in 0.29.0: a layer 0 px tall made the ramp 84 times the height)
    val ramp = ramp0.coerceIn(.001f, .25f)
    val low0 = from - .08f; val low1 = minOf(from + .14f, .99f)
    val top = top0.coerceIn(0f, maxOf(0f, low0 - ramp - .02f))
    val blur = android.graphics.RenderEffect.createBlurEffect(r, r, android.graphics.Shader.TileMode.CLAMP)
    fun black(a: Float) = android.graphics.Color.argb((a.coerceIn(0f, 1f) * 255).toInt(), 0, 0, 0)
    val up = topK.coerceIn(0f, 1f)
    val cols = ArrayList<Int>(); val at = ArrayList<Float>()
    cols.add(black(up)); at.add(0f)
    for (k in 0..SOFT_STEPS) { val s = k / SOFT_STEPS.toFloat(); cols.add(black(up * (1 - softStep(s)))); at.add(top + ramp * s) }
    for (k in 0..SOFT_STEPS) { val s = k / SOFT_STEPS.toFloat(); cols.add(black(softStep(s))); at.add(low0 + (low1 - low0) * s) }
    cols.add(black(1f)); at.add(1f)
    val mask = android.graphics.RenderEffect.createShaderEffect(android.graphics.LinearGradient(0f, 0f, 0f, maxOf(h, 1f), cols.toIntArray(), at.toFloatArray(), android.graphics.Shader.TileMode.CLAMP))
    val frosted = android.graphics.RenderEffect.createBlendModeEffect(blur, mask, android.graphics.BlendMode.DST_IN)
    return android.graphics.RenderEffect.createBlendModeEffect(android.graphics.RenderEffect.createOffsetEffect(0f, 0f), frosted, android.graphics.BlendMode.SRC_OVER).asComposeRenderEffect()
}

/** On its side: the video, blurred more and more to the right, one blur pass on the GPU as [frostBands]. */
private fun sideBlur(w: Float, r: Float): androidx.compose.ui.graphics.RenderEffect {
    val blur = android.graphics.RenderEffect.createBlurEffect(r, r, android.graphics.Shader.TileMode.CLAMP)
    val on = android.graphics.Color.BLACK; val off = android.graphics.Color.TRANSPARENT
    val from = .58f; val to = .92f   // sharp up to [from], eased: the transition stays, in the last part of the video
    val cols = IntArray(SOFT_STEPS + 3); val at = FloatArray(SOFT_STEPS + 3)
    cols[0] = off; at[0] = 0f
    for (k in 0..SOFT_STEPS) { val u = k / SOFT_STEPS.toFloat(); cols[k + 1] = android.graphics.Color.argb((softStep(u) * 255).toInt(), 0, 0, 0); at[k + 1] = from + (to - from) * u }
    cols[SOFT_STEPS + 2] = on; at[SOFT_STEPS + 2] = 1f
    val mask = android.graphics.RenderEffect.createShaderEffect(android.graphics.LinearGradient(0f, 0f, w, 0f, cols, at, android.graphics.Shader.TileMode.CLAMP))
    val frosted = android.graphics.RenderEffect.createBlendModeEffect(blur, mask, android.graphics.BlendMode.DST_IN)
    return android.graphics.RenderEffect.createBlendModeEffect(android.graphics.RenderEffect.createOffsetEffect(0f, 0f), frosted, android.graphics.BlendMode.SRC_OVER).asComposeRenderEffect()
}

private val EASE_AT = floatArrayOf(0f, .214f, .429f, .629f, .814f, 1f)   // the eased fade's steps (where, how opaque)
private val EASE_A = floatArrayOf(1f, .9f, .66f, .38f, .14f, 0f)

/** The Canvas video filling its box: sharp, or (Blur the Canvas) drawn at a quarter of the size, blurred there and
 *  stretched back, which is cheap, unlike a full-screen blur every frame. */
@Composable
private fun CanvasVideoView() {
    if (S.b("cvBlur") && !S.low) BoxWithConstraints(Modifier.fillMaxSize()) {
        val fw = constraints.maxWidth; val fh = constraints.maxHeight
        val sw = (fw / 4).coerceAtLeast(1); val sh = (fh / 4).coerceAtLeast(1)
        val r = S.i("cvBlurAmt").coerceIn(5, 100) / 100f * 14f   // in quarter-size pixels: up to ~56 on the screen
        Box(Modifier.layout { m, _ -> val p = m.measure(Constraints.fixed(sw, sh)); layout(fw, fh) { p.place(0, 0) } }
            .graphicsLayer { transformOrigin = TransformOrigin(0f, 0f); scaleX = fw / sw.toFloat(); scaleY = fh / sh.toFloat(); renderEffect = BlurEffect(r, r, TileMode.Clamp); compositingStrategy = CompositingStrategy.Offscreen }) {
            CanvasView()
        }
    } else CanvasView()
}

/** Picture-in-picture: the Canvas behind the lyrics there too, darkened so they stay readable. */
@Composable
fun PipCanvas() {
    if (!CanvasVideo.active) return
    val a by animateFloatAsState(if (CanvasVideo.cvmode) 1f else 0f, tween(800), label = "pipcanvas")
    Box(Modifier.fillMaxSize().alpha(maxOf(a, .01f)).drawWithContent { drawContent(); drawRect(Color.Black.copy(alpha = .45f)) }) { CanvasVideoView() }
}

@Composable
private fun CanvasView() {
    // fitted again whenever its size changes: turned on its side, it kept the upright size's numbers and the video stretched
    AndroidView({ ctx -> TextureView(ctx).also { tv -> CanvasVideo.bindView(tv); tv.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> coverFit(tv) } } }, Modifier.fillMaxSize(), onRelease = { CanvasVideo.unbind(it) })
}

/** A long break on the upright Lyrics screen: the cover takes the lyrics' place (not while you scroll them, nor while a
 *  Canvas plays with the cover hidden). */
private fun restCover() = Lyr.view.rest && !Lyr.view.lyFree && !(CanvasVideo.cvmode && S.b("cvNoArt"))

/** The lookup is done and found no lyrics: the cover takes their place (what's wrong, if anything, is at the top). */
private fun noLyrics() = Lyr.empty && Lyr.lines == 0

/** Side by side with no lyrics (and not the Visualizer): the song alone, in the middle. */
private fun soloWide() = noLyrics() && E.screen != "viz"

/** Under the big cover: why there are no lyrics, and the one thing to do about it. */
@Composable
private fun NoLyricsNote(modifier: Modifier = Modifier) {
    val m = Lyr.msg?.takeIf { !it.err } ?: return   // lyrics that couldn't load are an error: at the top
    // still looking: as where the lyrics go (the skeleton's label and a line the light runs along, or the spinner)
    if (m.kind == "load") return Row(modifier.padding(top = 2.dp).heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
        if (S.b("xSkeleton")) {
            Text("Loading lyrics", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = .5f))
            Spacer(Modifier.width(12.dp))
            SkeletonBar(Modifier.weight(1f).height(10.dp))
        } else {
            androidx.compose.material3.LoadingIndicator(Modifier.size(24.dp), color = Color.White.copy(alpha = .7f))
            Spacer(Modifier.width(10.dp))
            Text(m.msg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = dim, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    val act = m.actions.firstOrNull { it.pri } ?: m.actions.firstOrNull()
    Row(modifier.padding(top = 2.dp).heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(VIcons.lyrics, null, Modifier.size(16.dp), tint = dim)
        Spacer(Modifier.width(8.dp))
        Text(m.msg, Modifier.weight(1f, fill = false), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = dim, maxLines = 1, overflow = TextOverflow.Ellipsis)
        act?.let { a -> TextButton(onClick = { E.haptic(); a.fn?.invoke() }) { Text(a.label, fontSize = 13.sp) } }
    }
}

/** object-fit: cover for the TextureView (Canvas videos are 9:16), and object-position 50% 40%, as Spicy Lyrics': faces sit in
 *  the upper half of most, and a centred crop cuts them off. */
private fun coverFit(tv: TextureView) {
    val vw = 720f; val vh = 1280f
    val w = tv.width.toFloat(); val h = tv.height.toFloat(); if (w <= 0 || h <= 0) return
    val s = maxOf(w / vw, h / vh); val m = Matrix(); m.setScale(vw * s / w, vh * s / h, w / 2, h * .4f); tv.setTransform(m)
}

private var lastRom = "replace"
fun toggleTr() {
    val n = io.github.bozkow.vibevisualizer.core.LANGS.find { it.first == S.trTo }?.second ?: S.trTo
    @Suppress("UNUSED_VARIABLE") val unused = n
    S.set("tr", !S.tr)
}
fun toggleRom() {
    if (S.romMode != "off") lastRom = S.romMode
    val on = S.romMode == "off"; S.set("romMode", if (on) lastRom else "off")
}

@Suppress("unused") private val keep = listOf(Server, ColumnScope::class)
