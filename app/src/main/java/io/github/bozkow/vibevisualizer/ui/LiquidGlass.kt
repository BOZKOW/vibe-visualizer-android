package io.github.bozkow.vibevisualizer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.*
import io.github.bozkow.vibevisualizer.core.S
import kotlin.math.roundToInt

/** A source below this surface, never a layer containing the surface itself. */
internal val LocalGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }

private const val LENS = """
uniform shader content;
uniform float2 size;
uniform float4 corners;
uniform float rim;
half4 main(float2 p) {
    float2 c = size * 0.5;
    float radius = p.y < c.y ? (p.x < c.x ? corners.x : corners.y) : (p.x < c.x ? corners.w : corners.z);
    float2 v = p - c;
    float2 q = abs(v) - (c - radius);
    float2 outside = max(q, 0.0);
    float len = length(outside);
    float d = len + min(max(q.x, q.y), 0.0) - radius;
    float t = smoothstep(-rim, 0.0, d);
    float2 normal = len > 0.001 ? sign(v) * outside / max(len, 0.001) :
        (q.x > q.y ? float2(sign(v.x), 0.0) : float2(0.0, sign(v.y)));
    // Magnify the interior as well as bending the rim: an identity interior on a blurred
    // backdrop looks indistinguishable from ordinary frost. Edge bending stays thickness-bound.
    float2 s = clamp(c + v * 0.92 - normal * (rim * 0.5 * t * t), float2(0.5), size - 0.5);
    float2 fringe = normal * min(1.25, rim * 0.035) * t * t;
    half4 g = content.eval(s);
    half4 red = content.eval(clamp(s + fringe, float2(0.5), size - 0.5));
    half4 blue = content.eval(clamp(s - fringe, float2(0.5), size - 0.5));
    return half4(min(red.r, g.a), g.g, min(blue.b, g.a), g.a);
}
"""

/** One cached shader/effect per surface; every corner and the actual visible size participates in its cache key. */
class Lens internal constructor(private val shader: Any?, private val density: Float = 1f) {
    private var w = -1f; private var h = -1f
    private var tl = -1f; private var tr = -1f; private var br = -1f; private var bl = -1f
    private var fx: RenderEffect? = null
    fun effect(width: Float, height: Float, radius: Float, topRight: Float = radius, bottomRight: Float = radius, bottomLeft: Float = radius): RenderEffect? {
        if (Build.VERSION.SDK_INT < 33 || width < 1f || height < 1f) return null
        val sh = shader as? RuntimeShader ?: return null
        val maxR = minOf(width, height) / 2
        val a = radius.coerceIn(0f, maxR); val b = topRight.coerceIn(0f, maxR)
        val c = bottomRight.coerceIn(0f, maxR); val d = bottomLeft.coerceIn(0f, maxR)
        if (w != width || h != height || tl != a || tr != b || br != c || bl != d || fx == null) {
            w = width; h = height; tl = a; tr = b; br = c; bl = d
            sh.setFloatUniform("size", width, height)
            sh.setFloatUniform("corners", a, b, c, d)
            sh.setFloatUniform("rim", minOf(minOf(width, height) * .32f, 18f * density).coerceAtLeast(1f))
            fx = android.graphics.RenderEffect.createRuntimeShaderEffect(sh, "content").asComposeRenderEffect()
        }
        return fx
    }
}

@Composable fun rememberLens(): Lens {
    val enabled = liquidOn()
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    return remember(enabled, density) { Lens(if (enabled) RuntimeShader(LENS) else null, density) }
}

fun liquidOn(): Boolean = Build.VERSION.SDK_INT >= 33 && S.b("xLiquid") && !S.low && !S.calm

internal fun DrawScope.lensEffect(lens: Lens, shape: Shape): RenderEffect? {
    val outline = shape.createOutline(size, layoutDirection, this)
    return when (outline) {
        is Outline.Rounded -> outline.roundRect.let { r -> lens.effect(size.width, size.height,
            r.topLeftCornerRadius.x, r.topRightCornerRadius.x, r.bottomRightCornerRadius.x, r.bottomLeftCornerRadius.x) }
        is Outline.Rectangle -> lens.effect(size.width, size.height, 0f)
        else -> lens.effect(size.width, size.height, minOf(size.width, size.height) / 2)
    }
}

/** Refraction only: callers keep their shared body/rim and clip. Regular glass pays no layer/shader cost. */
internal fun Modifier.liquidSurface(shape: Shape): Modifier = composed {
    if (!liquidOn()) return@composed Modifier
    val backdrop = LocalGlassBackdrop.current
    val frost = LocalFrost.current
    if (backdrop == null && !frost) return@composed Modifier
    val lens = rememberLens()
    val layer = rememberGraphicsLayer()
    val painter = if (backdrop == null) frostPainter() else null
    var at by remember { mutableStateOf(Offset.Zero) }
    Modifier.onGloballyPositioned { at = if (backdrop != null) it.positionInRoot() else it.positionInWindow() }
        .drawBehind {
            if (size.width < 1f || size.height < 1f) return@drawBehind
            layer.renderEffect = lensEffect(lens, shape)
            layer.record(IntSize(size.width.roundToInt(), size.height.roundToInt())) {
                if (backdrop != null) {
                    if (backdrop.layer.size.width > 0) translate(backdrop.at.x - at.x, backdrop.at.y - at.y) { drawLayer(backdrop.layer) }
                } else painter?.invoke(this, at)
            }
            drawLayer(layer)
        }
}

/** Player frost through the same lens. Insets change the visible geometry and sampling origin together. */
fun Modifier.liquidFrost(radius: Dp? = null, inset: Dp = 0.dp): Modifier = composed {
    if (!liquidOn()) return@composed Modifier.frosted(inset, oval = radius == null)
    if (!LocalFrost.current) return@composed Modifier
    val lens = rememberLens()
    val painter = frostPainter()
    val layer = rememberGraphicsLayer()
    var at by remember { mutableStateOf(Offset.Zero) }
    val path = remember { Path() }
    Modifier.onGloballyPositioned { at = it.positionInWindow() }.drawBehind {
        val i = inset.toPx().coerceAtLeast(0f)
        val w = size.width - 2 * i; val h = size.height - 2 * i
        if (w < 1f || h < 1f) return@drawBehind
        val r = radius?.toPx()?.coerceIn(0f, minOf(w, h) / 2) ?: (minOf(w, h) / 2)
        layer.renderEffect = lens.effect(w, h, r)
        layer.record(IntSize(w.roundToInt(), h.roundToInt())) { painter(at + Offset(i, i)) }
        path.reset()
        if (radius == null) path.addOval(Rect(i, i, i + w, i + h))
        else path.addRoundRect(RoundRect(i, i, i + w, i + h, androidx.compose.ui.geometry.CornerRadius(r)))
        clipPath(path) { translate(i, i) { drawLayer(layer) } }
    }
}

/** Same touch target and switch semantics as the standard control; only the experimental material changes. */
@Composable
internal fun LiquidSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier, enabled: Boolean) {
    val x by androidx.compose.animation.core.animateDpAsState(if (checked) 24.dp else 4.dp,
        androidx.compose.animation.core.tween(if (Pop.calm) 0 else 180), label = "liquidSwitch")
    val accent = androidx.compose.material3.MaterialTheme.colorScheme.primary
    val touch = if (onCheckedChange == null) Modifier else Modifier.toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
    Box(modifier.defaultMinSize(minWidth = 52.dp, minHeight = 48.dp).then(touch), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Box(Modifier.size(52.dp, 32.dp).clip(CircleShape).liquidSurface(CircleShape)
            .drawBehind { glassBody(0f, if (enabled) 1f else .35f) }
            .background(if (checked) accent.copy(alpha = if (enabled) .22f else .08f) else Color.Transparent)
            .border(1.dp, if (enabled) GlassRim else GlassRimFaint, CircleShape)) {
            Box(Modifier.align(androidx.compose.ui.Alignment.CenterStart).offset { IntOffset(x.roundToPx(), 0) }.size(24.dp).clip(CircleShape)
                .background(Color.White.copy(alpha = if (enabled) .9f else .35f)))
        }
    }
}
