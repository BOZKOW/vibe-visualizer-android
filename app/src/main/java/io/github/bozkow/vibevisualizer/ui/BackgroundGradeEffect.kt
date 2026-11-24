package io.github.bozkow.vibevisualizer.ui

import android.os.Build
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import io.github.bozkow.vibevisualizer.engine.BackgroundGrade
import kotlin.math.sqrt

/** Shared low-resolution grade: Rec.709 saturation, gentle contrast, explicit gamut clamp BEFORE brightness. */
internal fun backgroundGradeNative(saturation: Float, brightness: Float): android.graphics.RenderEffect {
    val effect = if (Build.VERSION.SDK_INT >= 33) {
        val shader = android.graphics.RuntimeShader("""
            uniform shader content;
            uniform float sat;
            uniform float bri;
            uniform float contrast;
            half4 main(float2 p) {
                half4 c = content.eval(p);
                half l = dot(c.rgb, half3(0.213, 0.715, 0.072));
                half3 rgb = (mix(half3(l), c.rgb, sat) - 0.5) * contrast + 0.5;
                return half4(clamp(rgb, 0.0, 1.0) * bri, c.a);
            }
        """)
        shader.setFloatUniform("sat", saturation); shader.setFloatUniform("bri", brightness)
        shader.setFloatUniform("contrast", BackgroundGrade.CONTRAST)
        android.graphics.RenderEffect.createRuntimeShaderEffect(shader, "content")
    } else {
        val saturationMatrix = android.graphics.ColorMatrix().apply { setSaturation(saturation) }
        val light = android.graphics.ColorMatrix(floatArrayOf(brightness,0f,0f,0f,0f, 0f,brightness,0f,0f,0f, 0f,0f,brightness,0f,0f, 0f,0f,0f,1f,0f))
        android.graphics.RenderEffect.createChainEffect(
            android.graphics.RenderEffect.createColorFilterEffect(android.graphics.ColorMatrixColorFilter(light)),
            android.graphics.RenderEffect.createColorFilterEffect(android.graphics.ColorMatrixColorFilter(saturationMatrix)))
    }
    return effect
}

/** Radius in the shared 96px ambient texture, matching CanvasBlur and the lyrics-only Canvas option. */
internal fun backgroundBlurRadius(amount: Int, wide: Boolean = false): Float {
    val rf = amount.coerceIn(0, 100) * .025
    val sigma = 2f * sqrt(.36 + rf * (rf + 1)).toFloat() * (if (wide) 2f else 1f)
    return ((sigma - .5f) / .57735f).coerceAtLeast(.5f)
}

internal fun backgroundGradeEffect(saturation: Float, brightness: Float): RenderEffect =
    backgroundGradeNative(saturation, brightness).asComposeRenderEffect()
