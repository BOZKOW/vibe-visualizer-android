package io.github.bozkow.vibevisualizer.ui

import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import kotlin.math.max
import kotlin.math.min
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.composed
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.bozkow.vibevisualizer.core.S
import io.github.bozkow.vibevisualizer.engine.E

/* ---- Frosted glass, as the seek bar's track and the player's notices have it: clear white over what's under it, with
   a light edge. Never blurred live: what's under it is blurred already. The Glass design puts it on every card,
   button and switch; the player's round buttons have it in both designs. ---- */
val GlassFill = Color.White.copy(alpha = .12f)
val GlassEdge = Color.White.copy(alpha = .22f)

/** Frosted glass on a card or panel: the glass over a little shade, so white text stays readable on a bright cover.
 *  strong: more shade, for panels over busy content. */
fun Modifier.glass(shape: Shape, strong: Boolean = false): Modifier {
    return this.clip(shape)
        .background(Color(0xFF101018).copy(alpha = if (strong) .5f else .22f), shape)
        .background(GlassFill, shape)
        .border(1.dp, GlassEdge, shape)
}

/** A frosted button (Glass, the app's one design since 0.50.0). */
@Composable
fun VTonalButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, shape: Shape = ButtonDefaults.filledTonalShape, content: @Composable RowScope.() -> Unit) {
    androidx.compose.material3.FilledTonalButton(onClick, modifier, enabled, shape = shape, border = BorderStroke(1.dp, if (enabled) GlassEdge else Color.White.copy(alpha = .08f)),
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = GlassFill, contentColor = Color.White, disabledContainerColor = Color.White.copy(alpha = .04f), disabledContentColor = Color.White.copy(alpha = .38f)), content = content)
}

/** The main button: frosted glass tinted with the cover's colour (still the one to press). */
@Composable
fun VButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    val acc = MaterialTheme.colorScheme.primary
    androidx.compose.material3.Button(onClick, modifier, enabled, border = BorderStroke(1.dp, if (enabled) Color.White.copy(alpha = .34f) else Color.White.copy(alpha = .08f)),
        colors = ButtonDefaults.buttonColors(containerColor = acc.copy(alpha = .34f), contentColor = Color.White, disabledContainerColor = Color.White.copy(alpha = .04f), disabledContentColor = Color.White.copy(alpha = .38f)), content = content)
}

/** A fainter frosted button. */
@Composable
fun VOutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    androidx.compose.material3.OutlinedButton(onClick, modifier, enabled, border = BorderStroke(1.dp, if (enabled) GlassEdge else Color.White.copy(alpha = .08f)),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White.copy(alpha = .05f), contentColor = Color.White, disabledContentColor = Color.White.copy(alpha = .38f)), content = content)
}

/** A choice's card (one of several to pick from): frosted glass, the chosen one tinted with the cover's colour and with
 *  a brighter edge. */
@Composable
@Suppress("UNUSED_PARAMETER")
fun Modifier.choiceCard(on: Boolean, shape: Shape, off: Float = .06f): Modifier {
    val acc = MaterialTheme.colorScheme
    return this.clip(shape).background(if (on) acc.primary.copy(alpha = .26f) else GlassFill.copy(alpha = .08f))
        .border(1.dp, if (on) Color.White.copy(alpha = .36f) else GlassEdge, shape)
}

/** A slider's colours: its track frosted glass, filled with the cover's colour as far as it goes. */
@Composable
fun glassSlider(): SliderColors {
    val acc = MaterialTheme.colorScheme.primary
    return SliderDefaults.colors(thumbColor = acc, activeTrackColor = acc, inactiveTrackColor = Color.White.copy(alpha = .16f), activeTickColor = Color.White.copy(alpha = .5f),
        inactiveTickColor = acc, disabledInactiveTrackColor = Color.White.copy(alpha = .05f))
}

/** A switch: off, frosted glass; on, the cover's colour (on and off told apart at a glance). */
@Composable
fun VSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    androidx.compose.material3.Switch(checked, onCheckedChange, modifier, enabled = enabled, colors = SwitchDefaults.colors(
        uncheckedTrackColor = GlassFill, uncheckedBorderColor = Color.White.copy(alpha = .34f), uncheckedThumbColor = Color.White.copy(alpha = .7f),
        disabledUncheckedTrackColor = Color.White.copy(alpha = .04f), disabledUncheckedBorderColor = Color.White.copy(alpha = .12f)))
}

/** Glass: a scrolling list fades out at its edges instead of being cut off in a hard line where it scrolls under
 *  a header or a search field. top / bottom: how far the fade reaches; [clear]: how far down from its top nothing shows
 *  (a list that goes on under the header over it: from there it fades in). */
fun Modifier.fadeEdges(top: Dp = 28.dp, bottom: Dp = 0.dp, clear: Dp = 0.dp): Modifier = this
    .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val h = size.height; if (h <= 0f) return@drawWithContent
        val c = (clear.toPx() / h).coerceIn(0f, .6f)
        val t = (c + top.toPx() / h).coerceIn(c, .7f); val b = (bottom.toPx() / h).coerceIn(0f, .45f).coerceAtMost(1f - t)
        drawRect(Brush.verticalGradient(0f to Color.Transparent, c to Color.Transparent, t to Color.Black, (1 - b) to Color.Black, 1f to if (b > 0f) Color.Transparent else Color.Black),
            blendMode = androidx.compose.ui.graphics.BlendMode.DstIn)
    }

/** Room for the status bar / the navigation bar, the same whether it shows or is hidden (Settings → Phone): hiding the
 *  bars, or a swipe bringing them back for a moment, doesn't stretch or jump the layout. */
@Composable fun Modifier.statusBarsSpace(): Modifier = windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility)
@Composable fun Modifier.navBarsSpace(): Modifier = windowInsetsPadding(WindowInsets.navigationBarsIgnoringVisibility)

/** What lies under a floating island (the Settings list, all of Settings under a notice), recorded as it's drawn so the
 *  island can show it blurred behind its glass ([glassIsland]). */
class Backdrop(internal val layer: androidx.compose.ui.graphics.layer.GraphicsLayer) { internal var at = Offset.Zero }

@Composable
fun rememberBackdrop(): Backdrop { val l = androidx.compose.ui.graphics.rememberGraphicsLayer(); return remember(l) { Backdrop(l) } }

/** Records what this draws for the islands over it ([b]; null, or [on] false while no island shows: nothing recorded). */
fun Modifier.backdropSource(b: Backdrop?, on: Boolean = true): Modifier = if (b == null || !on) this else this
    .onGloballyPositioned { b.at = it.positionInRoot() }
    .drawWithContent { b.layer.record { this@drawWithContent.drawContent() }; drawLayer(b.layer) }

/** Frosted glass for a floating island (Settings' header islands, notices over Settings): what's under it ([b]) blurred,
 *  a tint so its words read on anything ([tint]), Glass's fill, a light edge, a soft highlight along its top and a shadow
 *  under it. [k]: how much of an island it is (0: none, as the header rests). Without [b], the tint alone. The blur is
 *  worked out at a quarter of the size, as Settings' own (a full-size one each frame stuttered). */
@Composable
fun Modifier.glassIsland(b: Backdrop?, k: Float = 1f, shape: Shape = RoundedCornerShape(28.dp), tint: Float = .42f): Modifier {
    val blur = androidx.compose.ui.graphics.rememberGraphicsLayer()
    val at = remember { floatArrayOf(0f, 0f) }
    val fx = remember { androidx.compose.ui.graphics.BlurEffect(6f, 6f, androidx.compose.ui.graphics.TileMode.Clamp) }
    if (k <= 0f) return this
    return this.shadow(10.dp * k, shape, clip = false, ambientColor = Color.Black, spotColor = Color.Black)
        .onGloballyPositioned { val p = it.positionInRoot(); at[0] = p.x; at[1] = p.y }
        .clip(shape)
        .drawBehind {
            if (b != null && b.layer.size.width > 0) {
                val f = 4f; val sw = (size.width / f).toInt() + 2; val sh = (size.height / f).toInt() + 2
                blur.renderEffect = fx; blur.alpha = k
                blur.record(androidx.compose.ui.unit.IntSize(sw, sh)) {
                    scale(1f / f, 1f / f, pivot = Offset.Zero) { translate(b.at.x - at[0], b.at.y - at[1]) { drawLayer(b.layer) } }
                }
                scale(f, f, pivot = Offset.Zero) { drawLayer(blur) }
            }
            drawRect(Color(0xFF0E0E14).copy(alpha = (if (b != null) tint else .8f) * k))
            drawRect(GlassFill.copy(alpha = GlassFill.alpha * k))
            drawRect(Brush.verticalGradient(0f to Color.White.copy(alpha = .1f * k), .45f to Color.Transparent))   // light along its top edge
        }
        .border(1.dp, GlassEdge.copy(alpha = GlassEdge.alpha * k), shape)
}

/** A round button's symbol on an island: frosted as the island's ([k]: how much of an island it is). */
fun Modifier.glassChip(k: Float): Modifier = if (k <= 0f) this else this
    .background(GlassFill.copy(alpha = GlassFill.alpha * k), androidx.compose.foundation.shape.CircleShape)
    .border(1.dp, GlassEdge.copy(alpha = GlassEdge.alpha * k), androidx.compose.foundation.shape.CircleShape)

/** A click only a quick tap makes. Held past the long-press time, it's let go without doing anything: an accidental touch
 *  (the phone gripped, a palm or a resting thumb lifted off a button) is a long one, and it skipped or paused the song.
 *  [onHold]: what holding it does instead, if anything (with its buzz). */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
fun Modifier.quickClick(src: androidx.compose.foundation.interaction.MutableInteractionSource?, enabled: Boolean = true,
                        indication: androidx.compose.foundation.Indication? = null, onHold: (() -> Unit)? = null, onClick: () -> Unit): Modifier =
    this.combinedClickable(src, indication, enabled = enabled, role = Role.Button, onLongClick = onHold ?: {}, hapticFeedbackEnabled = false,
        onClick = onClick)

/** A press you can feel: the thing shrinks a little while it's held and springs back (no motion with reduced motion). */
@Composable
fun Modifier.pressScale(src: androidx.compose.foundation.interaction.InteractionSource, to: Float = .9f): Modifier {
    val pressed by src.collectIsPressedAsState()
    val calm = S.low || io.github.bozkow.vibevisualizer.engine.E.reduceMotion()
    val k by androidx.compose.animation.core.animateFloatAsState(if (pressed && !calm) to else 1f,
        androidx.compose.animation.core.spring(.5f, androidx.compose.animation.core.Spring.StiffnessMediumLow), label = "press")
    return this.graphicsLayer { scaleX = k; scaleY = k }
}

/** Scrolling that reaches [bleedStart] and [bleedEnd] past its own box (out to the screen's sides, 0.46.1: only where
 *  nothing else is there; next to a cover or an icon give 0, so the text never slides over it), fading out over [fadeStart]
 *  and [fadeEnd] there. At rest what scrolls starts where it would without it, its first letter whole; it goes there and
 *  back ([pingPong]), its last letter coming to rest whole at the end. */
fun Modifier.edgeMarquee(bleedStart: Dp, bleedEnd: Dp, fadeStart: Dp, fadeEnd: Dp, iterations: Int, onRead: ((Long) -> Unit)? = null): Modifier = this
    .layout { m, c ->
        val l = bleedStart.roundToPx(); val r = bleedEnd.roundToPx()
        val p = m.measure(c.copy(minWidth = 0, maxWidth = if (c.hasBoundedWidth) c.maxWidth + l + r else c.maxWidth))
        val w = if (c.hasBoundedWidth) c.maxWidth else p.width
        layout(w, p.height) { p.place(-l, 0) }
    }
    .pingPong(iterations, endGap = bleedEnd, fadeStart = fadeStart, fadeEnd = fadeEnd, onRead = onRead)   // (it rests inside the margin it may slide through)
    .padding(start = bleedStart)

/** Text too long for its room slides left until its end shows ([endGap] short of the edge), waits, and slides back to
 *  where it started, again and again ([iterations] times; 0: it stays at its start). Not round and off the side as a
 *  marquee: the start and the end are always on screen, one after the other. Text that fits stays put. The sides fade
 *  ([fadeStart], [fadeEnd]) only where there's more text past them: at rest the start is whole and the end fades; at
 *  the end it's the other way round (a fade that stayed put dimmed the first letters, and sat over what was beside it). */
fun Modifier.pingPong(iterations: Int, endGap: Dp = 0.dp, delayMillis: Int = 2500, fadeStart: Dp = 0.dp, fadeEnd: Dp = 0.dp,
                      onRead: ((Long) -> Unit)? = null): Modifier = composed {
    var travel by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val dens = androidx.compose.ui.platform.LocalDensity.current
    val calm = S.low || io.github.bozkow.vibevisualizer.engine.E.reduceMotion()
    val on = travel > 0 && iterations > 0 && !calm
    // a short way at about 36 dp a second; past 120 dp slower, 20 dp a second (a long name or notice flew by), with a
    // gentle ease in and out (a sine's: its fastest is only half again its average)
    val ms = if (travel <= 0) 0L else with(dens) { val near = 120.dp.toPx(); ((min(travel.toFloat(), near) / 36.dp.toPx() + max(0f, travel - near) / 20.dp.toPx()) * 1000).toLong().coerceIn(700L, 30000L) }
    val start = remember(travel, iterations, calm) { android.os.SystemClock.uptimeMillis() }   // (starts over for new text or room)
    LaunchedEffect(on, ms) { onRead?.invoke(if (on) delayMillis + ms + 1500L else 0L) }   // ([onRead]: how long until its end has been read)
    // where it is, worked out from the time on the app's own frames (E.drawTick, at the frame limit): an animation of
    // its own ran at the screen's rate (120 Hz) the whole time a long name slid, whatever the limit
    val offset = {
        if (!on) 0f else {
            io.github.bozkow.vibevisualizer.engine.E.drawTick.longValue
            val cycle = delayMillis + ms + 1800 + ms
            val e = android.os.SystemClock.uptimeMillis() - start
            if (iterations != Int.MAX_VALUE && e / cycle >= iterations) 0f else {
                val k = e % cycle
                val f = when {
                    k < delayMillis -> 0f
                    k < delayMillis + ms -> SLIDE_EASE.transform((k - delayMillis).toFloat() / ms)
                    k < delayMillis + ms + 1800 -> 1f
                    else -> 1f - SLIDE_EASE.transform((k - delayMillis - ms - 1800).toFloat() / ms)
                }
                -travel * f
            }
        }
    }
    val fades = fadeStart > 0.dp || fadeEnd > 0.dp
    this.then(if (!fades) Modifier else Modifier.graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }.drawWithContent {
            drawContent()
            val w = size.width; if (travel <= 0 || w <= 0f) return@drawWithContent
            val moved = (-offset()).coerceIn(0f, travel.toFloat()); val left = travel - moved
            // each fade as wide as there's text hidden past it, up to its full width
            val a = (min(moved, fadeStart.toPx()) / w).coerceIn(0f, .45f); val b = (min(left, fadeEnd.toPx()) / w).coerceIn(0f, .45f)
            if (a <= 0f && b <= 0f) return@drawWithContent
            drawRect(Brush.horizontalGradient(0f to (if (a > 0f) Color.Transparent else Color.Black), a to Color.Black, (1 - b) to Color.Black, 1f to (if (b > 0f) Color.Transparent else Color.Black)),
                blendMode = androidx.compose.ui.graphics.BlendMode.DstIn)
        })
        .clipToBounds().layout { m, c ->
            val p = m.measure(c.copy(minWidth = 0, maxWidth = androidx.compose.ui.unit.Constraints.Infinity))
            val w = (if (c.hasBoundedWidth) p.width.coerceAtMost(c.maxWidth) else p.width).coerceAtLeast(c.minWidth)
            val t = if (p.width > w) p.width - w + endGap.roundToPx() else 0
            if (t != travel) travel = t
            // moved by the layer only: a frame of sliding redraws it, nothing is measured or placed again
            layout(w, p.height) { p.placeWithLayer(0, 0) { translationX = offset() } }
        }
}
private val SLIDE_EASE = androidx.compose.animation.core.CubicBezierEasing(.37f, 0f, .63f, 1f)

/** Out past the side margins by [by] on each side (the bars, to the screen's edges: 0.46.1). */
fun Modifier.bleedX(by: Dp): Modifier = this.layout { m, c ->
    val px = by.roundToPx()
    if (!c.hasBoundedWidth) return@layout m.measure(c).let { p -> layout(p.width, p.height) { p.place(0, 0) } }
    val p = m.measure(c.copy(minWidth = c.minWidth + 2 * px, maxWidth = c.maxWidth + 2 * px))
    layout(c.maxWidth, p.height) { p.place(-px, 0) }
}

/** Fades the left and right edges (a long title scrolling through), [start] / [end] wide. */
fun Modifier.fadeSides(start: Dp, end: Dp): Modifier = this
    .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val w = size.width; if (w <= 0f) return@drawWithContent
        val a = (start.toPx() / w).coerceIn(0f, .4f); val b = (end.toPx() / w).coerceIn(0f, .4f)
        drawRect(Brush.horizontalGradient(0f to Color.Transparent, a to Color.Black, (1 - b) to Color.Black, 1f to Color.Transparent),
            blendMode = androidx.compose.ui.graphics.BlendMode.DstIn)
    }

/** An icon from an SVG path (24×24 unless said otherwise). */
fun svgIcon(d: String, size: Float = 24f): ImageVector = ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = size, viewportHeight = size)
    .addPath(PathParser().parsePathString(d).toNodes(), fill = SolidColor(Color.White)).build()

object VIcons {
    val spotify = svgIcon("M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0 -20zm4.6 14.4a.6 .6 0 0 1 -.9 .2c-2.4 -1.5 -5.4 -1.8 -8.9 -1a.6 .6 0 1 1 -.3 -1.2c3.9 -.9 7.2 -.5 9.9 1.1 .3 .2 .4 .6 .2 .9zm1.2 -2.7a.8 .8 0 0 1 -1.1 .3c-2.7 -1.7 -6.9 -2.2 -10.1 -1.2a.8 .8 0 1 1 -.5 -1.5c3.7 -1.1 8.3 -.6 11.4 1.3 .4 .3 .5 .8 .3 1.1zm.1 -2.8C14.7 9 9.4 8.8 6.3 9.7a1 1 0 1 1 -.6 -1.9c3.5 -1.1 9.4 -.9 13.1 1.3a1 1 0 0 1 -.9 1.8z")
    val lyrics = svgIcon("M4 6h16v2H4zm0 5h12v2H4zm0 5h16v2H4z")
    val bars = svgIcon("M4 14h3v6H4zm6.5 -6h3v12h-3zM17 4h3v16h-3z")
    val both = svgIcon("M4 5h16v2H4zm0 4h10v2H4zM4 17h2.5v3H4zm4.5 -3H11v6H8.5zm4.5 1.5h2.5V20H13zm4.5 -3.5H20v8h-2.5z")
    val sliders = svgIcon("M4 7h10v2H4zm12 0h4v2h-4zM4 15h4v2H4zm6 0h10v2H10zM14 5h2v6h-2zM8 13h2v6H8z")
    val bolt = svgIcon("M13 2L4 14h6l-1 8 9 -12h-6l1 -8z")
    val resync = svgIcon("M12 4V1L8 5l4 4V6a6 6 0 0 1 6 6 6 6 0 0 1 -.7 2.8l1.46 1.46A8 8 0 0 0 20 12a8 8 0 0 0 -8 -8zm0 14a6 6 0 0 1 -6 -6c0 -1 .25 -1.97 .7 -2.8L5.24 7.74A8 8 0 0 0 4 12a8 8 0 0 0 8 8v3l4 -4 -4 -4v3z")
    val translate = svgIcon("M12.87 15.07l-2.54 -2.51 .03 -.03A17.52 17.52 0 0 0 14.07 6H17V4h-7V2H8v2H1v2h11.17C11.5 7.92 10.44 9.75 9 11.35 8.07 10.32 7.3 9.19 6.69 8h-2c.73 1.63 1.73 3.17 2.98 4.56l-5.09 5.02L4 19l5 -5 3.11 3.11 .76 -2.04zM18.5 10h-2L12 22h2l1.12 -3h4.75L21 22h2l-4.5 -12zm-2.62 7l1.62 -4.33L19.12 17h-3.24z")
    val heart = svgIcon("M16.5 3A6 6 0 0 0 12 5.1 6 6 0 0 0 7.5 3 5.5 5.5 0 0 0 2 8.5c0 3.8 3.4 6.9 8.6 11.5l1.4 1.4 1.4 -1.3C18.6 15.4 22 12.3 22 8.5A5.5 5.5 0 0 0 16.5 3zm-4.4 15.6l-.1 .1 -.1 -.1C7.1 14.2 4 11.4 4 8.5 4 6.5 5.5 5 7.5 5c1.5 0 3 1 3.6 2.4h1.8C13.5 6 15 5 16.5 5c2 0 3.5 1.5 3.5 3.5 0 2.9 -3.1 5.7 -7.9 10.1z")
    val heartOn = svgIcon("M12 21.4l-1.4 -1.4C5.4 15.4 2 12.3 2 8.5A5.5 5.5 0 0 1 7.5 3 6 6 0 0 1 12 5.1 6 6 0 0 1 16.5 3 5.5 5.5 0 0 1 22 8.5c0 3.8 -3.4 6.9 -8.6 11.5z")
    val devices = svgIcon("M4 6h18V4H4a2 2 0 0 0 -2 2v11H0v3h14v-3H4V6zm19 2h-6a1 1 0 0 0 -1 1v10a1 1 0 0 0 1 1h6a1 1 0 0 0 1 -1V9a1 1 0 0 0 -1 -1zm-1 9h-4v-7h4v7z")
}

/** The logo: the web app's gradient tile with five bars. */
@Composable
fun AppLogo(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val s = this.size.width / 64f
        drawRoundRect(Brush.linearGradient(listOf(Color(0xFF8B7CFF), Color(0xFFFF5FA2), Color(0xFF36C9FF)), Offset.Zero, Offset(this.size.width, this.size.height)), cornerRadius = CornerRadius(16 * s))
        drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = .28f), Color.Transparent), 0f, this.size.height / 2), cornerRadius = CornerRadius(16 * s))
        val bars = listOf(floatArrayOf(9.5f, 13f, 20f, .78f), floatArrayOf(19f, 19f, 22f, .9f), floatArrayOf(28.75f, 25f, 26f, 1f), floatArrayOf(38.5f, 19f, 22f, .9f), floatArrayOf(48f, 13f, 20f, .78f))
        bars.forEach { b -> drawRoundRect(Color.White.copy(alpha = b[3]), Offset(b[0] * s, b[1] * s), Size(6.5f * s, b[2] * s), CornerRadius(3.25f * s)) }
    }
}

@Composable
fun SectionLabel(t: String, modifier: Modifier = Modifier) {
    Text(t.uppercase(), modifier.padding(top = 16.dp, bottom = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color.White.copy(alpha = .45f))
}

@Composable
fun Hint(t: String, modifier: Modifier = Modifier) { Text(t, modifier.padding(vertical = 4.dp), fontSize = 13.sp, lineHeight = 18.sp, color = Color.White.copy(alpha = .6f)) }

@Composable
fun HSpace(w: Dp) = Spacer(Modifier.width(w))

fun openUrl(url: String) {
    val a = E.activity ?: return
    runCatching {
        CustomTabsIntent.Builder().setShowTitle(true).setColorScheme(CustomTabsIntent.COLOR_SCHEME_DARK)
            .setDefaultColorSchemeParams(CustomTabColorSchemeParams.Builder().setToolbarColor(0xFF07070C.toInt()).build()).build().launchUrl(a, Uri.parse(url))
    }.onFailure { runCatching { a.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }
}

val pillShape = RoundedCornerShape(50)
val cardShape = RoundedCornerShape(24.dp)
fun circle() = CircleShape
@Suppress("unused") private val keepTheme = MaterialTheme

/** What the camera cutout takes, per side: the larger of the room Android reports as safe and where the hole
 *  actually is, with room beside it (16 dp), on the left and right (a phone on its side). Some phones report less
 *  than the hole takes there. The top and bottom are Android's (the status bar covers the hole in portrait). */
@Composable
fun cutoutInsets(): androidx.compose.foundation.layout.WindowInsets {
    val d = androidx.compose.ui.platform.LocalDensity.current
    val dir = androidx.compose.ui.platform.LocalLayoutDirection.current
    val base = WindowInsets.displayCutout   // read here: a rotation recomposes this
    var l = base.getLeft(d, dir); val t = base.getTop(d); var r = base.getRight(d, dir); val b = base.getBottom(d)
    val v = androidx.compose.ui.platform.LocalView.current
    val w = v.width; val h = v.height
    if (w > 0 && h > 0) {
        val air = with(d) { 16.dp.roundToPx() }   // clear of the hole, not just beside it
        v.rootWindowInsets?.displayCutout?.boundingRects?.forEach { rc ->
            val toL = rc.left; val toR = w - rc.right
            // inside the room Android keeps at the top or the bottom (a hole in a corner of a phone held upright): that
            // room clears it already, so nothing at the side
            if (rc.bottom <= t + 1 || rc.top >= h - b - 1) return@forEach
            if (minOf(toL, toR) > minOf(rc.top, h - rc.bottom)) return@forEach   // at the top or bottom
            if (toL <= toR) l = maxOf(l, rc.right + air) else r = maxOf(r, w - rc.left + air)
        }
    }
    return androidx.compose.foundation.layout.WindowInsets(l, t, r, b)
}

/** Whether the top of the phone, with its camera, is on the left. Goes by how the phone is held, not what it reports about
 *  the hole; turning it right over changes nothing else the app is told, so this listens to the display. */
@Composable
fun cameraOnLeft(): Boolean {
    val v = androidx.compose.ui.platform.LocalView.current
    var rot by remember { androidx.compose.runtime.mutableIntStateOf(v.display?.rotation ?: 0) }
    androidx.compose.runtime.DisposableEffect(v) {
        val dm = v.context.getSystemService(android.hardware.display.DisplayManager::class.java)
        val l = object : android.hardware.display.DisplayManager.DisplayListener {
            override fun onDisplayAdded(id: Int) {}
            override fun onDisplayRemoved(id: Int) {}
            override fun onDisplayChanged(id: Int) { v.display?.let { rot = it.rotation } }
        }
        dm.registerDisplayListener(l, android.os.Handler(android.os.Looper.getMainLooper()))
        onDispose { dm.unregisterDisplayListener(l) }
    }
    return rot == android.view.Surface.ROTATION_90
}

/** The cutout as Android reports it and where it is, for the debug info. */
fun cutoutNote(v: android.view.View?): String {
    val c = v?.rootWindowInsets?.displayCutout ?: return "none"
    return "safe L${c.safeInsetLeft} T${c.safeInsetTop} R${c.safeInsetRight} B${c.safeInsetBottom} · holes " + c.boundingRects.joinToString { "${it.left},${it.top}-${it.right},${it.bottom}" } + " · window ${v.width}×${v.height}"
}

/** An eased ramp 0..1 for a mask (smoothstep): it leaves and arrives flat, so its ends don't show as edges, in [SOFT_STEPS] stops. */
const val SOFT_STEPS = 6
fun softStep(s: Float): Float { val x = s.coerceIn(0f, 1f); return x * x * (3 - 2 * x) }

/** A blur that costs little, for a layer over the whole screen: what's under it is drawn into a picture an eighth of
 *  the size, blurred there and stretched back up (a heavy blur looks the same, with ~60 times fewer pixels to blur;
 *  blurring the full screen every frame made Settings stutter). [k] 0..1 fades it in over the sharp picture, [dim]
 *  darkens it. */
@Composable
fun Modifier.cheapBlur(k: Float, radius: Dp, dim: Float = 0f): Modifier {
    val layer = androidx.compose.ui.graphics.rememberGraphicsLayer()
    val r = with(androidx.compose.ui.platform.LocalDensity.current) { radius.toPx() } / 8f
    if (k <= 0f) return this
    return this.drawWithContent {
        if (k < 1f) drawContent()   // the sharp picture under it while it fades in
        val f = 8f
        val sw = (size.width / f).toInt().coerceAtLeast(1); val sh = (size.height / f).toInt().coerceAtLeast(1)
        layer.record(androidx.compose.ui.unit.IntSize(sw, sh)) {
            scale(1f / f, 1f / f, pivot = Offset.Zero) { this@drawWithContent.drawContent() }
        }
        val rr = (r * k).coerceAtLeast(.5f)
        layer.renderEffect = androidx.compose.ui.graphics.BlurEffect(rr, rr, androidx.compose.ui.graphics.TileMode.Clamp)
        layer.alpha = k
        scale(f, f, pivot = Offset.Zero) { drawLayer(layer) }
        if (dim > 0f) drawRect(Color.Black.copy(alpha = dim * k))
    }
}

/** A row that squishes a little while pressed (and ripples), clipped to [shape]. */
@Composable
fun Modifier.squishClick(shape: Shape = RoundedCornerShape(16.dp), to: Float = .975f, onClick: () -> Unit): Modifier {
    val src = remember { MutableInteractionSource() }
    return this.pressScale(src, to).clip(shape).clickable(src, androidx.compose.material3.ripple(), onClick = onClick)
}

/** Settings' cards slide in one after another when a page opens ([i]: the card's place); cards that come later
 *  (scrolled to) just show. */
object Stagger { var shownAt = 0L }

@Composable
fun Modifier.staggerIn(i: Int): Modifier {
    val fresh = remember { android.os.SystemClock.uptimeMillis() - Stagger.shownAt < 450 && !Pop.calm }
    val k = remember { androidx.compose.animation.core.Animatable(if (fresh) 0f else 1f) }
    if (fresh) androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(i.coerceAtMost(8) * 45L)
        k.animateTo(1f, androidx.compose.animation.core.tween(380, easing = androidx.compose.animation.core.FastOutSlowInEasing))
    }
    return this.graphicsLayer { alpha = k.value; translationY = (1 - k.value) * 18.dp.toPx() }
}
