package io.github.bozkow.vibevisualizer.ui

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.bozkow.vibevisualizer.core.S
import io.github.bozkow.vibevisualizer.core.now
import io.github.bozkow.vibevisualizer.engine.Background
import io.github.bozkow.vibevisualizer.engine.CanvasVideo
import io.github.bozkow.vibevisualizer.engine.E
import io.github.bozkow.vibevisualizer.engine.Lyr
import io.github.bozkow.vibevisualizer.engine.Visuals
import io.github.bozkow.vibevisualizer.ui.theme.Bg
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** The background: the cover, alive (a liquid warp of it, as Spicy Lyrics' Kawarp; or drifting layers), a still
 *  image or a color. While a Canvas plays, the player draws it blurred over this ([CanvasBlur]), which then rests. */
@Composable
fun BackgroundLayer(modifier: Modifier = Modifier) {
    val style = S.bgStyle
    val cvlive = CanvasVideo.cvlive
    val cvA by animateFloatAsState(if (cvlive) 1f else 0f, tween(800), label = "cv")
    // recorded as it's drawn, so what's frosted upright can show it too (FrostBack)
    val seen = rememberGraphicsLayer()
    DisposableEffect(seen) { Frost.bg = seen; onDispose { if (Frost.bg === seen) Frost.bg = null } }
    val low = S.low   // (no frost there)
    Box(modifier.fillMaxSize().drawWithContent { if (low) drawContent() else { seen.record { this@drawWithContent.drawContent() }; drawLayer(seen) } }.background(Bg)) {
        if (cvA < 1f) Box(Modifier.fillMaxSize()) {
            when {
                style == "dynamic" && !S.low -> if (Build.VERSION.SDK_INT >= 33) LiquidBackground() else LayersBackground()
                style == "dynamic" -> BakedBackground()
                style == "artist" || style == "cover" -> StillBackground()
                style == "color" -> {   // a new pick fades in
                    val c by androidx.compose.animation.animateColorAsState(Visuals.plainColor(), tween(400), label = "bgcolor")
                    Box(Modifier.fillMaxSize().background(c))
                }
                else -> {}
            }
        }
    }
}

/** The baked background as is (Low performance mode): the blurred cover, stretched, no motion. */
@Composable
private fun BakedBackground() {
    Visuals.bakeVer
    val bm = Visuals.baked ?: return
    val img = remember(bm) { bm.asImageBitmap() }
    Canvas(Modifier.fillMaxSize()) {
        val s = maxOf(size.width, size.height)
        drawImage(img, IntOffset.Zero, IntSize(bm.width, bm.height), IntOffset(((size.width - s) / 2).toInt(), ((size.height - s) / 2).toInt()), IntSize(s.toInt(), s.toInt()), filterQuality = FilterQuality.High)
    }
}

/* ---- Kawarp-style liquid warp (API 33+): the blurred cover, slowly flowing, moving with the music ---- */
private const val LIQUID = """
uniform shader img;
uniform shader prev;
uniform float2 res;
uniform float t;
uniform float mixv;
uniform float sat;
uniform float dim;
uniform float pulse;
half4 main(float2 fc) {
    float2 uv = fc / res;
    float aspect = res.x / res.y;
    float2 p = uv - 0.5;
    if (aspect > 1.0) { p.y /= aspect; } else { p.x *= aspect; }
    float a = t * 0.045;
    p = float2(p.x * cos(a) - p.y * sin(a), p.x * sin(a) + p.y * cos(a));
    p *= 0.82 - pulse * 0.05;
    float2 q = p;
    q += 0.055 * float2(sin(p.y * 3.1 + t * 0.7) + sin(p.y * 1.7 - t * 0.4), cos(p.x * 2.9 - t * 0.6) + cos(p.x * 1.3 + t * 0.5));
    q += 0.05 * float2(sin((p.x + p.y) * 4.3 + t * 0.9), cos((p.x - p.y) * 3.7 - t * 1.1));
    q += 0.025 * float2(sin(length(p) * 9.0 - t * 1.3), cos(length(p) * 7.0 + t));
    float2 st = clamp(q + 0.5, 0.0, 1.0) * 128.0;
    half4 c = mix(prev.eval(st), img.eval(st), mixv);
    half l = dot(c.rgb, half3(0.299, 0.587, 0.114));
    c.rgb = mix(half3(l), c.rgb, sat);
    c.rgb *= (1.0 - dim);
    c.rgb += pulse * 0.035;
    c.rgb += (fract(sin(dot(fc, float2(12.9898, 78.233))) * 43758.5453) - 0.5) / 255.0;
    return half4(c.rgb, 1.0);
}
"""

@RequiresApi(33)
@Composable
private fun LiquidBackground() {
    Visuals.bakeVer
    val src = Visuals.kwSrc ?: return
    val prev = Visuals.kwPrev ?: src
    val shader = remember { RuntimeShader(LIQUID) }
    fun bs(b: Bitmap) = BitmapShader(b, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply { filterMode = BitmapShader.FILTER_MODE_LINEAR }
    val imgS = remember(src) { bs(src) }
    val prevS = remember(prev) { bs(prev) }
    val brush = remember(shader) { ShaderBrush(shader) }
    // drawn small (320 px on its long side) and stretched with smooth filtering, as Spicy Lyrics Mobile and the web's
    // Kawarp draw it: the picture is a blur anyway, and the shader runs for about 1/50 of the screen's pixels
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val fw = constraints.maxWidth; val fh = constraints.maxHeight
        if (fw <= 0 || fh <= 0) return@BoxWithConstraints
        val k = 320f / maxOf(fw, fh)
        val sw = (fw * k).roundToInt().coerceAtLeast(1); val sh = (fh * k).roundToInt().coerceAtLeast(1)
        Canvas(Modifier.layout { m, _ -> val p = m.measure(Constraints.fixed(sw, sh)); layout(fw, fh) { p.place(0, 0) } }
            .graphicsLayer { transformOrigin = TransformOrigin(0f, 0f); scaleX = fw / sw.toFloat(); scaleY = fh / sh.toFloat(); compositingStrategy = CompositingStrategy.Offscreen }) {
            E.bgTick.longValue
            shader.setInputShader("img", imgS); shader.setInputShader("prev", prevS)
            shader.setFloatUniform("res", size.width, size.height)
            shader.setFloatUniform("t", Background.kwTime.toFloat() * 2.2f)
            shader.setFloatUniform("mixv", Visuals.kwFade(now()))
            shader.setFloatUniform("sat", Visuals.bgSat().toFloat())   // vivid (CoverColors), the same as the blurred Canvas
            shader.setFloatUniform("dim", (1f - Visuals.bgBri().toFloat()).coerceIn(0f, .85f))
            shader.setFloatUniform("pulse", Background.pulse.coerceIn(0f, 1f))
            drawRect(brush)
        }
    }
}

/** API 31-32: the web app's own fallback, three soft layers of the blurred cover that drift, spin and swell with the bass. */
private val BGL = arrayOf(floatArrayOf(1f, 1f, .06f, .05f, 1.25f, 1f), floatArrayOf(.7f, -1f, .22f, .18f, .95f, .7f), floatArrayOf(1.25f, 1f, .24f, .22f, .8f, .55f))

@Composable
private fun LayersBackground() {
    Visuals.bakeVer
    val sp = Visuals.sprites.map { it?.asImageBitmap() }
    val prev = Visuals.spritesPrev.map { it?.asImageBitmap() }
    Canvas(Modifier.fillMaxSize()) {
        E.bgTick.longValue
        val w = size.width; val h = size.height; val pulse = Background.pulse; val D = hypot(w, h) * (1 + pulse * .18f); val t = Background.bgT.toFloat(); val W2 = 6.2832f / 40
        fun layers(imgs: List<androidx.compose.ui.graphics.ImageBitmap?>, alpha: Float) {
            imgs.forEachIndexed { i, img ->
                img ?: return@forEachIndexed
                val l = BGL[i]; val s = D * l[4]
                val cx = w / 2 + cos(t * .11f * (i + 1) + i * 2) * w * l[2]; val cy = h / 2 + sin(t * .09f * (i + 1) + i) * h * l[3]
                withTransform({ translate(cx, cy); rotate((t * W2 * l[0] * l[1] + i * 2.1f) * 57.2958f, Offset.Zero) }) {
                    drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), IntOffset((-s / 2).toInt(), (-s / 2).toInt()), IntSize(s.toInt(), s.toInt()), alpha = l[5] * alpha, filterQuality = FilterQuality.High)
                }
            }
        }
        val fade = Visuals.fadeProgress(now())
        layers(sp, 1f)
        if (fade < 1f && prev.any { it != null }) layers(prev, 1 - fade)
        if (pulse > .02f) drawRect(Color.White.copy(alpha = min(.12f, pulse * .1f)))
    }
}

/** Settings → Background: Artist image or Cover art, blurred, with brightness and saturation. */
@Composable
private fun StillBackground() {
    val bm = Background.still ?: return
    val img = remember(bm) { bm.asImageBitmap() }
    val m = remember(S.bri, S.sat) { ColorMatrix().apply { setToSaturation(Visuals.bgSat().toFloat()) }.also { cm -> val b = Visuals.bgBri().toFloat(); cm.timesAssign(ColorMatrix(floatArrayOf(b, 0f, 0f, 0f, 0f, 0f, b, 0f, 0f, 0f, 0f, 0f, b, 0f, 0f, 0f, 0f, 0f, 1f, 0f))) } }
    // (Low performance mode, no blur: a tiny copy stretched over the screen is as soft)
    val low = S.low
    val tiny = remember(bm, low) { if (low) Bitmap.createScaledBitmap(bm, 32, 32, true).asImageBitmap() else null }
    if (tiny != null) Image(tiny, null, Modifier.fillMaxSize().scale(1.12f), contentScale = ContentScale.Crop, colorFilter = ColorFilter.colorMatrix(m), filterQuality = FilterQuality.Low)
    else Image(img, null, Modifier.fillMaxSize().scale(1.12f).blur((S.bgblur * .6f).dp), contentScale = ContentScale.Crop, colorFilter = ColorFilter.colorMatrix(m))
}

/** The Canvas video, blurred, as the background behind everything: [vid] (the video as the player draws it, recorded)
 *  drawn a second time on the GPU, 96 px along the screen's longer side, blurred there and stretched over the screen,
 *  then shaded. Nothing comes back from the GPU: until 0.35.0 a small frame was copied back 12 times a second, and
 *  each copy held up the frame being drawn, so the lyrics stuttered (the more the video moved, the worse). [vidSize]: how
 *  big [vid]'s recording is, as state: it's recorded after this is drawn, so this draws again once it's there (coming back
 *  from the small window it had nothing to draw and nothing asked again, so the background stayed dark). [wide]: on its
 *  side, blurred more. [stage]: Spicy Lyrics' stage over it (a phone on its side): its grade, flat at brightness .65 for the
 *  default slider, no vignette, and the video 1.3 times bigger, so its edges stay out of the blur. */
@Composable
fun CanvasBlur(vid: GraphicsLayer, vidSize: () -> IntSize, wide: Boolean = false, stage: Boolean = false) {
    val cvA by animateFloatAsState(if (CanvasVideo.cvlive) 1f else 0f, tween(800), label = "cv")
    if (cvA <= 0f) return
    val quiet = E.quiet
    val vs = vidSize()   // (read here, not as it draws: there it never asked again, seen in a log of each draw)
    val small = rememberGraphicsLayer()
    // [small] on a picture of its own, drawn from there by all that show it: drawn straight by each frosted button and the bar,
    // with their own clips, its blur was worked out again for each of them, for every frame of the video
    val done = rememberGraphicsLayer()
    DisposableEffect(done) { Frost.video = done; onDispose { if (Frost.video === done) Frost.video = null } }
    val cf = remember(S.bri, S.sat, stage) { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(Visuals.bgSat().toFloat()) }.also { cm -> val b = if (stage) .65f * S.bri / 70f else Visuals.bgBri().toFloat(); cm.timesAssign(ColorMatrix(floatArrayOf(b, 0f, 0f, 0f, 0f, 0f, b, 0f, 0f, 0f, 0f, 0f, b, 0f, 0f, 0f, 0f, 0f, 1f, 0f))) }) }
    Canvas(Modifier.fillMaxSize().onSizeChanged { Frost.screen = it }) {
        val lw = vs.width; val lh = vs.height
        if (lw > 0 && lh > 0) {
            val k = 96f / maxOf(size.width, size.height)
            val sw = maxOf(1, (size.width * k).roundToInt()); val sh = maxOf(1, (size.height * k).roundToInt())
            // as soft as before: the background's Blur as the old three box passes over a 48-px frame, with that frame's
            // own softness, here in 96-px pixels (a blur radius r is a sigma of .577 r + .5)
            val rf = S.bgblur * .025
            val sigma = 2f * kotlin.math.sqrt(.36 + rf * (rf + 1)).toFloat()
            val r = (((if (wide) sigma * 2f else sigma) - .5f) / .57735f).coerceAtLeast(.5f)
            small.renderEffect = if (quiet) null else BlurEffect(r, r, TileMode.Clamp)
            small.colorFilter = cf
            small.alpha = cvA
            small.record(IntSize(sw, sh)) {
                // the video covering the screen (upright it's the whole screen already; side by side, the left side's)
                val sc = maxOf(sw / lw.toFloat(), sh / lh.toFloat()) * (if (stage) 1.3f else 1f)
                translate((sw - lw * sc) / 2f, (sh - lh * sc) / 2f) { scale(sc, sc, pivot = Offset.Zero) { drawLayer(vid) } }
            }
            done.compositingStrategy = androidx.compose.ui.graphics.layer.CompositingStrategy.Offscreen
            done.record(IntSize(sw, sh)) { drawLayer(small) }
            scale(size.width / sw, size.height / sh, pivot = Offset.Zero) { drawLayer(done) }
        }
        if (!stage) drawRect(Brush.radialGradient(listOf(Color.Black.copy(alpha = .15f * cvA), Color.Black.copy(alpha = .55f * cvA)), Offset(size.width * .7f, size.height * .5f), maxOf(size.width, size.height) * .8f))
    }
}

/** The seek bar, the buttons and the notifications lie right on the cover, the video or the lyrics, so on the player they're
 *  frosted instead of see-through. No blur is run for them: they show part of pictures that are blurred already. Upright that's
 *  what's really behind them: the background, the Canvas's blurred copy and the lyrics' blurred ends (a crop of the cover had
 *  nothing to do with what was under it, and a blurred picture of its own for them took fast video from 21% to 39% janky).
 *  On its side the Canvas's copy, or the cover's, made once a song. */
val LocalFrost = staticCompositionLocalOf { false }

object Frost {
    var video by mutableStateOf<GraphicsLayer?>(null)   // the Canvas, blurred ([CanvasBlur])
    var screen = IntSize.Zero                           // the size that's drawn at
    var cover by mutableStateOf(Rect.Zero)              // where the cover is, in the window
    var upright by mutableStateOf(false)                // the phone's held upright: frost shows what's behind it
    var bg: GraphicsLayer? = null                       // the background as it's drawn
    var lyrics: GraphicsLayer? = null                   // the lyrics' blurred ends (LyricsPane), when they're drawn: an eighth of the size...
    var lyricsY = 0f                                    // ...and how far up they start
    var gradeW = 0f; var gradeH = 0f; var gradeTop = 0f; var gradeShade = 0f   // the grade below, for what's frosted to show too
}

/** The Canvas screen's grade, over any background, so every song's screen looks the same as one with a Canvas: darker to the
 *  edges, and at the top and the foot behind what's there ([shade]: how much of that, 0 to 1). Plain gradients: next to nothing to
 *  draw, and no blur. [top]: where the top's text ends, px. */
fun DrawScope.screenGrade(w: Float, h: Float, top: Float, shade: Float, k: Float = 1f) {
    drawRect(Brush.radialGradient(listOf(Color.Black.copy(alpha = .15f * k), Color.Black.copy(alpha = .55f * k)), Offset(w * .7f, h * .5f), maxOf(w, h) * .8f), size = androidx.compose.ui.geometry.Size(w, h))
    if (shade > 0f) {
        val tf = (top / h).coerceIn(.02f, .45f); val a = .45f * shade * k
        drawRect(Brush.verticalGradient(0f to Color.Black.copy(alpha = a), tf to Color.Transparent, .55f to Color.Transparent, 1f to Color.Black.copy(alpha = a), startY = 0f, endY = h), size = androidx.compose.ui.geometry.Size(w, h))
    }
}

/** Draws what a frosted thing at [at] (in the window) shows, as a DrawScope call. */
@Composable
fun frostPainter(): DrawScope.(Offset) -> Unit {
    Visuals.bakeVer
    val src = Visuals.kwSrc
    val img = remember(src) { src?.asImageBitmap() }
    val still = Background.still
    val stillImg = remember(still) { still?.asImageBitmap() }
    return remember(img, stillImg) { { at -> drawFrost(img, stillImg, at) } }
}

private fun DrawScope.drawFrost(img: ImageBitmap?, stillImg: ImageBitmap?, at: Offset) {
    drawRect(Color(0xFF18181E))   // never see-through, whatever's there yet
    if (Frost.upright) {
        translate(-at.x, -at.y) {
            val c = Frost.cover
            when (S.bgStyle) {
                // (a still background's picture itself: drawn through its own blur, that would be worked out again for each of these)
                "artist", "cover" -> if (stillImg != null) drawImage(stillImg, IntOffset.Zero, IntSize(stillImg.width, stillImg.height), IntOffset.Zero, IntSize((c.right + c.left).roundToInt().coerceAtLeast(1), (c.bottom + c.top).roundToInt().coerceAtLeast(1)), filterQuality = FilterQuality.Low)
                "color" -> drawRect(Visuals.plainColor())
                else -> Frost.bg?.let { drawLayer(it) }
            }
            val v = Frost.video; val scr = Frost.screen
            if (v != null && v.size.width > 0 && scr.width > 0) scale(scr.width / v.size.width.toFloat(), scr.height / v.size.height.toFloat(), pivot = Offset.Zero) { drawLayer(v) }
            if (Frost.gradeW > 0f) screenGrade(Frost.gradeW, Frost.gradeH, Frost.gradeTop, Frost.gradeShade)   // (what's behind it is graded)
            Frost.lyrics?.let { l -> if (l.size.width > 0) translate(0f, Frost.lyricsY) { scale(8f, 8f, pivot = Offset.Zero) { drawLayer(l) } } }
        }
        return
    }
    val v = Frost.video; val scr = Frost.screen
    if (v != null && v.size.width > 0 && scr.width > 0) {
        translate(-at.x, -at.y) { scale(scr.width / v.size.width.toFloat(), scr.height / v.size.height.toFloat(), pivot = Offset.Zero) { drawLayer(v) } }
    } else if (img != null) {
        val c = Frost.cover; val iw = img.width; val ih = img.height
        val k = iw / (if (c.width > 0f) c.width else maxOf(size.width, 1f))
        val sw = (size.width * k).roundToInt().coerceIn(1, iw); val sh = (size.height * k).roundToInt().coerceIn(1, ih)
        val sx = ((at.x - c.left) * k).roundToInt().coerceIn(0, iw - sw); val sy = ((at.y - c.top) * k).roundToInt().coerceIn(0, ih - sh)
        drawImage(img, IntOffset(sx, sy), IntSize(sw, sh), IntOffset.Zero, IntSize(size.width.roundToInt() + 1, size.height.roundToInt() + 1), filterQuality = FilterQuality.Low)
    }
    drawRect(Color.Black.copy(alpha = .35f))   // the white on it keeps reading on a bright cover
}

/* ---- Liquid glass (0.56.0, an experiment the owner asked for: "iOS-like"; Settings → Experiments → Liquid glass) ----
   The frost behind a piece drawn through a lens: near its rim the blurred picture behind is read from further in, so it
   bends and magnifies there like the edge of a thick glass, with red and blue read a hair apart for the faint rainbow
   real glass has at its edge. Android 13 or newer (an AGSL RuntimeShader as the layer's RenderEffect); the shader runs
   only on the piece's own small layer. Off in Low performance mode and with Motion sickness. */
private const val LENS = """
uniform shader content;
uniform float2 size;
uniform float radius;
uniform float rim;
half4 main(float2 p) {
    float2 c = size * 0.5;
    float2 q = abs(p - c) - (c - radius);
    float d = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
    float t = clamp(1.0 + d / rim, 0.0, 1.0);
    float bend = t * t;
    float2 dir = p - c;
    float2 s = c + dir * (1.0 - 0.32 * bend);
    float2 off = dir * 0.03 * bend;
    half4 g = content.eval(s);
    half r = content.eval(s + off).r;
    half b = content.eval(s - off).b;
    return half4(r, g.g, b, g.a);
}
"""

/** Liquid glass is on (and can be): the experiment, Android 13+, not in Low performance mode or with Motion sickness. */
fun liquidOn(): Boolean = Build.VERSION.SDK_INT >= 33 && S.b("xLiquid") && !S.low && !S.calm

/** The frost behind a round button ([radius] null: an oval) or a notice (its corners' [radius]), through the lens when
 *  Liquid glass is on; plain frost otherwise. */
fun Modifier.liquidFrost(radius: Dp? = null, inset: Dp = 0.dp): Modifier = composed {
    if (!liquidOn()) return@composed Modifier.frosted(inset, oval = radius == null)
    if (!LocalFrost.current) return@composed Modifier
    var at by remember { mutableStateOf(Offset.Zero) }
    val paint = frostPainter()
    val layer = androidx.compose.ui.graphics.rememberGraphicsLayer()
    val shader = remember { if (Build.VERSION.SDK_INT >= 33) RuntimeShader(LENS) else null }
    val made = remember { floatArrayOf(-1f, -1f) }
    Modifier.onGloballyPositioned { at = it.positionInWindow() }.drawBehind {
        val sh = shader ?: return@drawBehind
        if (Build.VERSION.SDK_INT < 33) return@drawBehind
        val w = size.width; val h = size.height
        if (w < 1f || h < 1f) return@drawBehind
        // the effect is made again only when the size changes (its numbers are the piece's shape)
        if (made[0] != w || made[1] != h) {
            made[0] = w; made[1] = h
            val r = radius?.toPx()?.coerceAtMost(minOf(w, h) / 2) ?: (minOf(w, h) / 2)
            sh.setFloatUniform("size", w, h); sh.setFloatUniform("radius", r)
            sh.setFloatUniform("rim", minOf(minOf(w, h) * .32f, 22.dp.toPx()))
            layer.renderEffect = android.graphics.RenderEffect.createRuntimeShaderEffect(sh, "content").asComposeRenderEffect()
        }
        layer.record(IntSize(w.roundToInt(), h.roundToInt())) { paint(at) }
        if (radius == null) {
            val i = inset.toPx()
            val clip = Path().apply { addOval(Rect(i, i, w - i, h - i)) }
            clipPath(clip) { drawLayer(layer) }
        } else drawLayer(layer)
    }
}

/** Liquid glass's highlight: the light catching the rim brightest at the top left, and a soft glow inside it there. */
fun DrawScope.liquidShine(shape: androidx.compose.ui.graphics.Shape) {
    val o = shape.createOutline(size, layoutDirection, this)
    drawOutline(o, Brush.linearGradient(0f to Color.White.copy(alpha = .75f), .35f to Color.White.copy(alpha = .12f), .7f to Color.Transparent, 1f to Color.White.copy(alpha = .25f),
        start = Offset.Zero, end = Offset(size.width, size.height)), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
    drawOutline(o, Brush.radialGradient(0f to Color.White.copy(alpha = .14f), 1f to Color.Transparent, center = Offset(size.width * .25f, size.height * .15f), radius = maxOf(size.width, size.height) * .6f))
}

/** Frost behind a round button or a notice, on the player. [inset]: when the button is drawn smaller than it's laid out.
 *  [oval] false: the thing is clipped to its own shape already. */
fun Modifier.frosted(inset: Dp = 0.dp, oval: Boolean = true): Modifier = composed {
    if (!LocalFrost.current) return@composed Modifier
    var at by remember { mutableStateOf(Offset.Zero) }
    val paint = frostPainter()
    val clip = remember { Path() }
    Modifier.onGloballyPositioned { at = it.positionInWindow() }.drawBehind {
        if (!oval) paint(at)
        else {
            val i = inset.toPx()
            clip.reset(); clip.addOval(Rect(i, i, size.width - i, size.height - i))
            clipPath(clip) { paint(at) }
        }
    }
}
