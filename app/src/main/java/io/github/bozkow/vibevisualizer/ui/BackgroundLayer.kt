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
    DisposableEffect(seen) { Frost.bg = seen; Frost.bgReady = false; onDispose { if (Frost.bg === seen) { Frost.bg = null; Frost.bgReady = false } } }
    val low = S.low   // (no frost there)
    val cacheStill = !low && !cvlive && (style == "artist" || style == "cover")
    seen.compositingStrategy = if (cacheStill) androidx.compose.ui.graphics.layer.CompositingStrategy.Offscreen else androidx.compose.ui.graphics.layer.CompositingStrategy.Auto
    Box(modifier.fillMaxSize().then(if (cacheStill) Modifier.graphicsLayer() else Modifier).drawWithContent {
        if (low) drawContent() else {
            seen.record { this@drawWithContent.drawContent() }
            if (Frost.bg === seen) Frost.bgReady = seen.size.width > 0
            drawLayer(seen)
        }
    }.background(Bg)) {
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
    half l = dot(c.rgb, half3(0.213, 0.715, 0.072));
    c.rgb = clamp((mix(half3(l), c.rgb, sat) - 0.5) * 1.06 + 0.5, 0.0, 1.0);
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
    val grade = remember(S.bri, S.sat) { backgroundGradeEffect(Visuals.bgSat().toFloat(), Visuals.bgBri().toFloat()) }
    // (Low performance mode, no blur: a tiny copy stretched over the screen is as soft)
    val low = S.low
    val tiny = remember(bm, low, S.bri, S.sat) {
        if (low) Bitmap.createScaledBitmap(bm, 32, 32, true).copy(Bitmap.Config.ARGB_8888, true).also {
            Visuals.blurBitmap(it, 0.0, Visuals.bgSat(), Visuals.bgBri())
        }.asImageBitmap() else null
    }
    if (tiny != null) Image(tiny, null, Modifier.fillMaxSize().scale(1.12f), contentScale = ContentScale.Crop, filterQuality = FilterQuality.Low)
    else Image(img, null, Modifier.fillMaxSize().scale(1.12f).graphicsLayer { renderEffect = grade }.blur((S.bgblur * .6f).dp), contentScale = ContentScale.Crop)
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
    val grade = remember(S.bri, S.sat) { backgroundGradeNative(Visuals.bgSat().toFloat(), Visuals.bgBri().toFloat()) }
    val radius = backgroundBlurRadius(S.bgblur, wide)
    val sourceBlurred = lyricsCanvasBlur()
    val blur = remember(radius, quiet, grade, sourceBlurred) {
        if (quiet || sourceBlurred) grade.asComposeRenderEffect() else android.graphics.RenderEffect.createChainEffect(grade,
            android.graphics.RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)).asComposeRenderEffect()
    }
    Canvas(Modifier.fillMaxSize().onSizeChanged { Frost.screen = it }) {
        val lw = vs.width; val lh = vs.height
        if (lw > 0 && lh > 0) {
            val k = 96f / maxOf(size.width, size.height)
            val sw = maxOf(1, (size.width * k).roundToInt()); val sh = maxOf(1, (size.height * k).roundToInt())
            // as soft as before: the background's Blur as the old three box passes over a 48-px frame, with that frame's
            // own softness, here in 96-px pixels (a blur radius r is a sigma of .577 r + .5)
            small.renderEffect = blur
            small.colorFilter = null
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
    var coverSharp by mutableStateOf<GraphicsLayer?>(null) // same recorded landscape cover for clear Liquid refraction
    var coverReady by mutableStateOf(false)
    var coverBlurReady by mutableStateOf(false)
    var coverBlur by mutableStateOf<GraphicsLayer?>(null) // landscape cover, including its transition and motion video
    var videoScene by mutableStateOf<GraphicsLayer?>(null)              // finished displayed Canvas/background composite, after blur and shading
    var videoSceneBlur by mutableStateOf<GraphicsLayer?>(null)          // one finished blur of the same crop for regular frost
    var canvasAlreadyBlurred by mutableStateOf(false)
    var sceneReady by mutableStateOf(false)
    var sceneBlurReady by mutableStateOf(false)
    var sceneTextureSize by mutableStateOf(IntSize.Zero)
    var sceneBlurTextureSize by mutableStateOf(IntSize.Zero)
    var sceneSize by mutableStateOf(IntSize.Zero)
    var videoAt by mutableStateOf(Offset.Zero)
    var screen = IntSize.Zero                           // the size that's drawn at
    var cover by mutableStateOf(Rect.Zero)              // where the cover is, in the window
    var upright by mutableStateOf(false)                // the phone's held upright: frost shows what's behind it
    var bg by mutableStateOf<GraphicsLayer?>(null)
    var bgReady by mutableStateOf(false)                       // the background as it's drawn
    var lyrics by mutableStateOf<GraphicsLayer?>(null)                   // the lyrics' blurred ends (LyricsPane), when they're drawn: an eighth of the size...
    var lyricsY by mutableStateOf(0f)                                    // ...and how far up they start
    var gradeW by mutableStateOf(0f); var gradeH by mutableStateOf(0f); var gradeTop by mutableStateOf(0f); var gradeShade by mutableStateOf(0f)   // the grade below, for what's frosted to show too
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

/** Record the actual landscape cover once; all controls reuse its finished GPU blur. */
fun Modifier.liveCoverFrost(): Modifier = composed {
    if (S.low) return@composed Modifier
    val picture = rememberGraphicsLayer()
    val liquid = liquidOn()
    picture.compositingStrategy = if (liquid) androidx.compose.ui.graphics.layer.CompositingStrategy.Offscreen else androidx.compose.ui.graphics.layer.CompositingStrategy.Auto
    val radius = with(androidx.compose.ui.platform.LocalDensity.current) { 48.dp.toPx() / 8f }
    val blur = remember(radius) { BlurEffect(radius, radius, TileMode.Clamp) }
    val small = rememberGraphicsLayer()
    val done = rememberGraphicsLayer()
    DisposableEffect(done, picture) {
        Frost.coverBlur = done; Frost.coverSharp = picture; Frost.coverReady = false; Frost.coverBlurReady = false
        onDispose {
            if (Frost.coverBlur === done) { Frost.coverBlur = null; Frost.coverBlurReady = false }
            if (Frost.coverSharp === picture) { Frost.coverSharp = null; Frost.coverReady = false }
        }
    }
    this.drawWithContent {
        if (!E.quiet && E.playingState && io.github.bozkow.vibevisualizer.engine.AnimatedCover.ready) E.drawTick
        picture.record { this@drawWithContent.drawContent() }
        drawLayer(picture)
        if (!liquid && !E.quiet && size.width > 0f && size.height > 0f) {
            val f = 8f
            val sw = (size.width / f).roundToInt().coerceAtLeast(1)
            val sh = (size.height / f).roundToInt().coerceAtLeast(1)
            small.renderEffect = blur
            small.record(IntSize(sw, sh)) { scale(1f / f, 1f / f, pivot = Offset.Zero) { drawLayer(picture) } }
            done.compositingStrategy = androidx.compose.ui.graphics.layer.CompositingStrategy.Offscreen
            done.record(IntSize(sw, sh)) { drawLayer(small) }
            if (Frost.coverBlur === done) Frost.coverBlurReady = true
        }
        if (Frost.coverSharp === picture) Frost.coverReady = picture.size.width > 0
    }
}

/** Share the displayed Canvas: inherit its blur; add material frost only where it is still sharp. */
fun Modifier.liveCanvasFrost(enabled: Boolean, alreadyBlurred: Boolean, reduced: Boolean = false): Modifier = composed {
    if (!enabled) return@composed Modifier
    val scene = rememberGraphicsLayer()
    val small = rememberGraphicsLayer()
    val done = rememberGraphicsLayer()
    scene.compositingStrategy = androidx.compose.ui.graphics.layer.CompositingStrategy.Offscreen
    done.compositingStrategy = androidx.compose.ui.graphics.layer.CompositingStrategy.Offscreen
    val needsBlur = !liquidOn() && !alreadyBlurred
    val radius = with(androidx.compose.ui.platform.LocalDensity.current) { 48.dp.toPx() / 8f }
    val blur = remember(radius) { BlurEffect(radius, radius, TileMode.Clamp) }
    androidx.compose.runtime.SideEffect { Frost.canvasAlreadyBlurred = alreadyBlurred }
    DisposableEffect(scene, done) {
        Frost.videoScene = scene; Frost.videoSceneBlur = done; Frost.sceneReady = false; Frost.sceneBlurReady = false
        onDispose {
            if (Frost.videoScene === scene) { Frost.videoScene = null; Frost.sceneReady = false }
            if (Frost.videoSceneBlur === done) { Frost.videoSceneBlur = null; Frost.sceneBlurReady = false }
        }
    }
    Modifier.onGloballyPositioned { Frost.videoAt = it.positionInWindow() }.onSizeChanged { Frost.sceneSize = it }.drawWithContent {
        // Once the entire Canvas is blurred, its finished scene needs no full-screen offscreen texture.
        // Sharp video and blur transitions retain full resolution. All glass reuses this ONE finished result.
        val (sw, sh) = if (reduced) canvasTextureSize(size.width, size.height, 384) else size.width.roundToInt() to size.height.roundToInt()
        if (sw < 1 || sh < 1) return@drawWithContent
        val displayW = size.width; val displayH = size.height
        scene.record(IntSize(sw, sh)) {
            scale(sw / displayW, sh / displayH, pivot = Offset.Zero) { this@drawWithContent.drawContent() }
        }
        if (Frost.videoScene === scene) {
            Frost.sceneTextureSize = IntSize(sw, sh)
            Frost.sceneReady = true
        }
        scale(size.width / sw, size.height / sh, pivot = Offset.Zero) { drawLayer(scene) }
        if (needsBlur && !E.quiet && size.width > 0f && size.height > 0f) {
            val f = 8f
            val sw = (size.width / f).roundToInt().coerceAtLeast(1)
            val sh = (size.height / f).roundToInt().coerceAtLeast(1)
            small.renderEffect = blur
            small.record(IntSize(sw, sh)) { scale(1f / f, 1f / f, pivot = Offset.Zero) { drawLayer(scene) } }
            done.record(IntSize(sw, sh)) { drawLayer(small) }
            if (Frost.videoSceneBlur === done) {
                Frost.sceneBlurTextureSize = IntSize(sw, sh)
                Frost.sceneBlurReady = true
            }
        }
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
    val scene = Frost.videoScene
    if (io.github.bozkow.vibevisualizer.engine.CanvasVideo.cvmode && Frost.sceneReady && scene != null && scene.size.width > 0) {
        val finished = if (liquidOn() || Frost.canvasAlreadyBlurred) scene
            else if (Frost.sceneBlurReady) Frost.videoSceneBlur ?: scene else scene
        translate(-at.x, -at.y) { Frost.bg?.let { drawLayer(it) } }
        // Cache builders must observe the recording size as well as readiness. Switching between
        // sharp/full-size and blurred/reduced-size scenes otherwise leaves a stale sampling transform.
        val texture = if (finished === scene) Frost.sceneTextureSize else Frost.sceneBlurTextureSize
        translate(Frost.videoAt.x - at.x, Frost.videoAt.y - at.y) {
            scale(Frost.sceneSize.width / texture.width.coerceAtLeast(1).toFloat(), Frost.sceneSize.height / texture.height.coerceAtLeast(1).toFloat(), pivot = Offset.Zero) { drawLayer(finished) }
        }
        if (Frost.upright) Frost.lyrics?.let { l -> if (l.size.width > 0) translate(-at.x, Frost.lyricsY - at.y) { scale(8f, 8f, pivot = Offset.Zero) { drawLayer(l) } } }
        return
    }
    if (Frost.upright) {
        translate(-at.x, -at.y) {
            val c = Frost.cover
            val background = Frost.bg
            if (Frost.bgReady && background != null) drawLayer(background)
            else when (S.bgStyle) {
                // (a still background's picture itself: drawn through its own blur, that would be worked out again for each of these)
                "artist", "cover" -> if (stillImg != null) drawImage(stillImg, IntOffset.Zero, IntSize(stillImg.width, stillImg.height), IntOffset.Zero, IntSize((c.right + c.left).roundToInt().coerceAtLeast(1), (c.bottom + c.top).roundToInt().coerceAtLeast(1)), filterQuality = FilterQuality.Low)
                "color" -> drawRect(Visuals.plainColor())
                else -> Frost.bg?.let { drawLayer(it) }
            }
            val v = Frost.video; val scr = Frost.screen
            if (v != null && v.size.width > 0 && scr.width > 0) scale(scr.width / v.size.width.toFloat(), scr.height / v.size.height.toFloat(), pivot = Offset.Zero) { drawLayer(v) }
            if (Frost.gradeW > 0f && !io.github.bozkow.vibevisualizer.engine.CanvasVideo.cvlive) screenGrade(Frost.gradeW, Frost.gradeH, Frost.gradeTop, Frost.gradeShade)
            Frost.lyrics?.let { l -> if (l.size.width > 0) translate(0f, Frost.lyricsY) { scale(8f, 8f, pivot = Offset.Zero) { drawLayer(l) } } }
        }
        return
    }
    val v = Frost.video; val scr = Frost.screen
    val coverLayer = if (liquidOn()) Frost.coverSharp else Frost.coverBlur
    if (v != null && v.size.width > 0 && scr.width > 0) {
        translate(-at.x, -at.y) { scale(scr.width / v.size.width.toFloat(), scr.height / v.size.height.toFloat(), pivot = Offset.Zero) { drawLayer(v) } }
    } else if ((if (liquidOn()) Frost.coverReady else Frost.coverBlurReady) && coverLayer?.let { it.size.width > 0 } == true) {
        val layer = coverLayer; val c = Frost.cover
        translate(c.left - at.x, c.top - at.y) {
            scale(c.width / layer.size.width, c.height / layer.size.height, pivot = Offset.Zero) { drawLayer(layer) }
        }
    } else if (img != null) {
        val c = Frost.cover; val iw = img.width; val ih = img.height
        val k = iw / (if (c.width > 0f) c.width else maxOf(size.width, 1f))
        val sw = (size.width * k).roundToInt().coerceIn(1, iw); val sh = (size.height * k).roundToInt().coerceIn(1, ih)
        val sx = ((at.x - c.left) * k).roundToInt().coerceIn(0, iw - sw); val sy = ((at.y - c.top) * k).roundToInt().coerceIn(0, ih - sh)
        drawImage(img, IntOffset(sx, sy), IntSize(sw, sh), IntOffset.Zero, IntSize(size.width.roundToInt() + 1, size.height.roundToInt() + 1), filterQuality = FilterQuality.Low)
    }
    // The shared glass body supplies the tint in both orientations; don't darken landscape twice.
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
