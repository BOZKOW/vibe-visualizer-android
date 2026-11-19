package io.github.bozkow.vibevisualizer.spotify

/** Late details refine a song; two known but different IDs/releases identify different recordings. */
internal fun sessionRecordingChanged(oldId: String, newId: String, oldAlbum: String, newAlbum: String, oldDuration: Long, newDuration: Long): Boolean {
    if (oldId.isNotBlank() && newId.isNotBlank()) return oldId != newId
    if (oldAlbum.isNotBlank() && newAlbum.isNotBlank() && !oldAlbum.trim().equals(newAlbum.trim(), ignoreCase = true)) return true
    return oldDuration > 0 && newDuration > 0 && kotlin.math.abs(oldDuration - newDuration) > 1_500
}
