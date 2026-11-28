package io.github.bozkow.vibevisualizer

import android.os.Bundle
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Animatable2
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import io.github.bozkow.vibevisualizer.engine.DanceAnimation
import kotlinx.coroutines.launch

/** Silent native-codec integration check. Does not change player state or preferences. */
class DancePreviewActivity : ComponentActivity() {
    private var image: AnimatedImageDrawable? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val surface = object : View(this) {
            override fun onDraw(canvas: Canvas) {
                canvas.drawColor(Color.rgb(24,32,48))
                image?.let { drawable ->
                    val scale = minOf(width.toFloat()/drawable.intrinsicWidth,height.toFloat()/drawable.intrinsicHeight)
                    val w = (drawable.intrinsicWidth*scale).toInt(); val h = (drawable.intrinsicHeight*scale).toInt()
                    drawable.setBounds((width-w)/2,(height-h)/2,(width+w)/2,(height+h)/2)
                    drawable.draw(canvas)
                }
                postInvalidateOnAnimation()
            }
        }
        setContentView(surface)
        lifecycleScope.launch {
            val start = android.os.SystemClock.elapsedRealtime()
            val mode = intent.getStringExtra("part")?.takeIf { it in listOf("all","1","2") } ?: "all"
            val clip = DanceAnimation.load(intent.getIntExtra("bpm",120),mode)
            Log.i("VV-dance", "Prepared mode=$mode duration=${clip?.durationMs} loadMs=${android.os.SystemClock.elapsedRealtime()-start}")
            if (clip != null) {
                java.io.File(cacheDir,"dance-sequence-v2-$mode.webp").copyTo(
                    java.io.File(getExternalFilesDir(null),"VV-QA-DANCE-compiled.webp"),overwrite = true)
            }
            image = clip?.image
            image?.repeatCount = 0
            val playingAt = android.os.SystemClock.elapsedRealtime()
            image?.registerAnimationCallback(object : Animatable2.AnimationCallback() {
                override fun onAnimationEnd(drawable: android.graphics.drawable.Drawable?) {
                    Log.i("VV-dance", "Playback complete elapsedMs=${android.os.SystemClock.elapsedRealtime()-playingAt}")
                }
            })
            image?.start()
        }
    }
    override fun onStop() {
        image?.stop()
        image?.clearAnimationCallbacks()
        image?.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
        super.onStop()
    }
}
