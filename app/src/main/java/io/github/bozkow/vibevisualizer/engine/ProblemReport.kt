package io.github.bozkow.vibevisualizer.engine

import java.net.URLEncoder

/** Deliberately typed/limited diagnostics. Never accept a settings dump, lookup log, crash log or server config. */
object ProblemReport {
    const val ISSUES = "https://github.com/BOZKOW/vibe-visualizer-android/issues"
    data class Details(val version: String, val device: String, val android: String, val orientation: String,
        val glass: String, val fps: String, val low: Boolean, val background: String,
        val canvasBlur: Boolean, val controlsBackdrop: String, val blurAmount: Int)

    fun body(description: String, details: Details?): String = buildString {
        append("## Problem\n\n").append(description.trim()).append("\n")
        if (details != null) {
            append("\n## Technical details\n\n")
            append("- App: ${details.version}\n- Device: ${details.device}\n- Android: ${details.android}\n")
            append("- Orientation: ${details.orientation}\n- Glass: ${details.glass}\n- Frame cap: ${details.fps.takeIf { it.matches(Regex("[0-9]{1,3}")) } ?: "unknown"}\n")
            append("- Low performance mode: ${details.low}\n- Background: ${details.background.takeIf { it in setOf("dynamic", "cover", "artist", "color", "none") } ?: "other"}\n")
            append("- Canvas blur: ${details.canvasBlur}\n- Behind controls: ${details.controlsBackdrop.takeIf { it in setOf("none", "dark", "blur", "both") } ?: "other"}\n- Blur amount: ${details.blurAmount}%\n")
        }
    }

    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")
    fun blankUrl(title: String) = "$ISSUES/new?title=${encode(title.trim())}"
    /** Keep large/Unicode reports out of oversized query strings: the UI offers copy-and-paste instead. */
    fun draftUrl(title: String, report: String): String? =
        (blankUrl(title) + "&body=" + encode(report)).takeIf { it.length <= 7500 }
}
