package io.github.bozkow.vibevisualizer.engine

import android.content.Context
import android.os.Build
import io.github.bozkow.vibevisualizer.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/** Crash detection: a crash anywhere in the app (also while it only follows the music in the background or draws the
 *  widget) is written down on the phone just before Android closes it: what went wrong and where, the version, the
 *  phone and what was playing. At the next start it shows at the top ("closed unexpectedly", with Copy details) and
 *  goes to the error log on your server. */
object Crashes {
    private var file: File? = null

    /** First thing at every start of the app. */
    fun watch(ctx: Context) {
        val f = File(ctx.filesDir, "last-crash.txt"); file = f
        val before = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            runCatching {
                val sw = StringWriter(); e.printStackTrace(PrintWriter(sw))
                f.writeText(listOf(
                    "Vibe Visualizer for Android " + BuildConfig.VERSION_NAME + " · Android " + Build.VERSION.RELEASE + " · " + Build.MANUFACTURER + " " + Build.MODEL,
                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.ROOT).format(java.util.Date()) + " · thread " + t.name +
                        " · " + runCatching { E.src + (if (E.title.isNotEmpty()) " · " + E.title else "") + (E.settingsPage?.let { " · Settings: $it" } ?: "") }.getOrDefault(""),
                    "",
                    sw.toString().take(20000)).joinToString("\n"))
            }
            before?.uncaughtException(t, e)   // Android closes the app as usual
        }
    }

    /** The app crashed since it was last open: said at the top, and sent to your server's error log. Once. */
    fun tell() {
        val f = file ?: return
        if (!f.exists()) return
        val text = runCatching { f.readText() }.getOrNull(); f.delete()
        if (text.isNullOrBlank()) return
        val what = text.substringAfter("\n\n", "").lineSequence().firstOrNull()?.trim().orEmpty()
        Reports.report("crash", what.ifEmpty { "crash" }, text.lineSequence().drop(1).firstOrNull().orEmpty(), text.substringAfter("\n\n", text))
        E.snack(E.Snack("Vibe Visualizer closed unexpectedly last time", "err", E.Act("Copy details") { Lyr.copy(text); E.copied("Details") }, long = true))
    }
}
