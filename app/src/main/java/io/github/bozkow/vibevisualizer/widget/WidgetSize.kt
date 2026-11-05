package io.github.bozkow.vibevisualizer.widget

import kotlin.math.roundToInt

/**
 * A widget size from One UI Home, which gives it in dp at the display's default density [stableDpi] while the app runs
 * at [densityDpi], taken back to the app's dp. Ported from Spicy Lyrics Mobile's WidgetFit.launcherDp (AGPL-3.0).
 *
 * Only for a smaller display size (a lower [densityDpi]): there One UI reports a widget a third too big, and the
 * lyrics were drawn for that size and squeezed (Mobile 0.17). At a larger display size the same sum made the widget
 * wider than the screen, so the lyrics were drawn far too small (Mobile 0.19.0, their issue #44): left alone.
 */
internal fun launcherDp(widthDp: Int, heightDp: Int, densityDpi: Int, stableDpi: Int): Pair<Int, Int> {
    if (densityDpi <= 0 || stableDpi <= 0 || densityDpi >= stableDpi) return widthDp to heightDp
    val ratio = densityDpi.toFloat() / stableDpi
    return (widthDp * ratio).roundToInt() to (heightDp * ratio).roundToInt()
}
