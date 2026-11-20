package io.github.bozkow.vibevisualizer.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Silent sharp-pattern check of the production backdrop/lens path; excluded from release builds.
 * Enable xLiquid (Low/reduced motion off) through DebugReceiver first; this does not change preferences.
 * Start with --ez refract false for an undistorted reference screenshot.
 * The pattern deliberately contains edges: tinted gradients alone cannot verify refraction.
 */
class GlassPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val backdrop = rememberBackdrop()
                val refract = intent.getBooleanExtra("refract", true)
                Box(Modifier.fillMaxSize()) {
                    Canvas(Modifier.fillMaxSize().backdropSource(backdrop)) {
                        drawRect(Color(0xFF172235))
                        val step = 20.dp.toPx()
                        var x = 0f
                        while (x < size.width) {
                            drawRect(Color(0xFFB1CFD5), Offset(x, 0f), Size(step / 2, size.height))
                            x += step
                        }
                        var y = 0f
                        while (y < size.height) {
                            drawLine(Color(0xFFFFB267), Offset(0f, y), Offset(size.width, y), 2.dp.toPx())
                            y += step
                        }
                    }
                    CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {
                        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceEvenly,
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (!liquidOn()) "Enable Liquid Glass first" else if (refract) "Production lens" else "Undistorted reference", color = Color.White)
                            PreviewSurface(Modifier.fillMaxWidth().height(64.dp), RoundedCornerShape(32.dp), refract)
                            PreviewSurface(Modifier.size(120.dp), CircleShape, refract)
                            PreviewSurface(Modifier.fillMaxWidth().height(130.dp),
                                RoundedCornerShape(topStart = 28.dp, topEnd = 4.dp, bottomEnd = 28.dp, bottomStart = 4.dp), refract)
                            PreviewSurface(Modifier.fillMaxWidth().height(12.dp), RoundedCornerShape(6.dp), refract)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewSurface(modifier: Modifier, shape: Shape, refract: Boolean) {
    Box(modifier.clip(shape).then(if (refract) Modifier.liquidSurface(shape) else Modifier)
        .drawBehind { glassBody(0f) }.border(1.dp, GlassRim, shape))
}
