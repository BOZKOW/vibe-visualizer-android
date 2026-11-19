package io.github.bozkow.vibevisualizer.spotify

/** Artwork addresses published by media descriptions are not always web URLs. */
internal fun mediaArtworkAddress(value: String?): String = value.orEmpty().let { uri -> when {
    uri.startsWith("spotify:image:") -> "https://i.scdn.co/image/" + uri.removePrefix("spotify:image:")
    listOf("https:", "http:", "content:", "file:", "android.resource:").any(uri::startsWith) -> uri
    else -> ""
} }
