package io.github.bozkow.vibevisualizer.lyrics

/** Attribution may move from its pinned fallback only once the measured footer is readable in the lyrics viewport. */
internal fun creditsReadable(top: Float, height: Float, viewport: Float, alpha: Float): Boolean =
    height > 0f && viewport > 0f && alpha >= .5f && top >= 0f && top + height <= viewport + 1f

/** Reserve the footer after the final sung line, without moving any still-active lyrics. */
internal fun creditsEndTarget(target: Float, contentBottom: Float, footerHeight: Float, viewport: Float, gap: Float): Float =
    if (footerHeight <= 0f || viewport <= 0f) target else minOf(target, viewport - gap - contentBottom - footerHeight)
