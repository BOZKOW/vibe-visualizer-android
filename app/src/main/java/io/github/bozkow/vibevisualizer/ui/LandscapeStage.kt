package io.github.bozkow.vibevisualizer.ui

/** Where the Canvas goes on a phone on its side, as Spicy Lyrics lays out its stage (Spicy Lyrics Mobile's LandscapeMetrics, the
 *  desktop's --NowBarWidth and its spacings, AGPL-3.0): in dp, for a page [pageW] by [pageH] inside the bars and the camera.
 *  Everything follows from a cover that isn't drawn: over a Canvas the video takes its place. */
class LandscapeStage(pageW: Float, pageH: Float) {
    /** `calc(min(30cqw, 52cqh) * 1.1)`. */
    private val cover = minOf(pageW * .30f, pageH * .52f) * 1.1f
    private val inner = cover / 4f
    private val edge = maxOf((pageW * .5f - cover - inner) * .7f, 0f)

    /** From the page's outer edge, how far the video is at full strength. */
    val solid = edge + cover

    /** Its soft edge toward the lyrics after that, the only one it has at the side: 45% of the cover. */
    val feather = cover * .45f

    /** From the page's outer edge to where the feather has faded out. */
    val width = solid + feather

    /** Where the lyrics start and how far they stop from the other edge: they run over the feather. */
    val lyricsStart = (pageW * .5f - inner * .3f) * .9f
    val lyricsEndGap = edge * .35f
}
