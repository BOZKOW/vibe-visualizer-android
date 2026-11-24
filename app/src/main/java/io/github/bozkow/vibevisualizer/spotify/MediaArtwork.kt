package io.github.bozkow.vibevisualizer.spotify

/** Artwork addresses published by media descriptions are not always web URLs. */
internal fun mediaArtworkAddress(value: String?): String = value.orEmpty().let { uri -> when {
    uri.startsWith("spotify:image:") -> "https://i.scdn.co/image/" + uri.removePrefix("spotify:image:")
    listOf("https:", "http:", "content:", "file:", "android.resource:").any(uri::startsWith) -> uri
    else -> ""
} }

/** Only known resolution markers, never a different album/image ID or an arbitrary URL rewrite. */
internal fun fullArtworkAddress(url: String): String = url.replace(
    Regex("^(https://i\\.scdn\\.co/image/ab67616d)(?:00004851|00001e02)"), "$1" + "0000b273")
