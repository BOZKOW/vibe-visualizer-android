package io.github.bozkow.vibevisualizer

import android.os.Looper
import android.util.Log
import java.io.File

/** Bounded, app-only sampling for rendering checks. Logs code locations and thread CPU, no content/settings. */
object RenderProbe {
    private fun ticks(): Map<String, Pair<String, Long>> = File("/proc/self/task").listFiles().orEmpty().mapNotNull { dir ->
        runCatching {
            val fields = File(dir, "stat").readText().substringAfterLast(") ").split(' ')
            dir.name to (File(dir, "comm").readText().trim() to (fields[11].toLong() + fields[12].toLong()))
        }.getOrNull()
    }.toMap()
    fun run() {
        Thread({
            val start = ticks(); val counts = HashMap<String, Int>(); val main = Looper.getMainLooper().thread
            repeat(400) {
                val stack = main.stackTrace
                val frame = stack.firstOrNull { it.className.startsWith("androidx.compose") || it.className.startsWith("io.github.bozkow") }
                    ?: stack.firstOrNull()
                if (frame != null) { val key = "${frame.className}.${frame.methodName}"; counts[key] = (counts[key] ?: 0) + 1 }
                Thread.sleep(10)
            }
            val end = ticks()
            counts.entries.sortedByDescending { it.value }.take(14).forEach { Log.i("VV-renderprobe", "stack ${it.value}: ${it.key}") }
            end.mapNotNull { (id, value) -> start[id]?.let { Triple(id, value.first, value.second - it.second) } }
                .sortedByDescending { it.third }.take(10).forEach { Log.i("VV-renderprobe", "cpu_ticks ${it.third}: ${it.second}") }
            Log.i("VV-renderprobe", "DONE")
        }, "VV-render-probe").start()
    }
}
