package io.github.bozkow.vibevisualizer

import io.github.bozkow.vibevisualizer.core.Net
import io.github.bozkow.vibevisualizer.core.Server
import io.github.bozkow.vibevisualizer.core.Vibe
import io.github.bozkow.vibevisualizer.lyrics.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Read-only integration check with two known recordings, never the owner's track/history, key or server address. */
internal object SpicyFileProbe {
    fun run() { Vibe.scope.launch {
        val output = JSONObject()
        try {
            val known = SongMeta(title = "Never Gonna Give You Up", artist = "Rick Astley", duration = 213.573, file = "probe-a.mp3")
            val knownId = "4PTG3Z6ehGkBFwjybzWkR8"
            val metadata = SpicyRecording.entity(Net.fetch("https://open.spotify.com/embed/track/$knownId").text)
            val verified = metadata != null && SpicyRecording.matches(known, knownId, metadata)
            output.put("publicMetadataVerified", verified)
            android.util.Log.i("VV-probe", "Public recording metadata verification: $verified")
            val endpoint = Server.url("/api/lyrics")
            if (endpoint == null) output.put("state", "no server configured") else {
                val tests = listOf(
                    SongMeta(title = "Never Gonna Give You Up", artist = "Rick Astley", duration = 213.573, file = "probe-a.mp3"),
                    SongMeta(title = "Blinding Lights", artist = "The Weeknd", duration = 200.04, file = "probe-b.mp3"))
                val results = JSONArray()
                for (want in tests) {
                    val row = JSONObject().put("title", want.title)
                    try {
                        val response = Net.fetch("$endpoint?${spicyQuery(want)}", noCache = true)
                        row.put("status", response.code)
                        val json = response.json()
                        if (json != null) {
                            val body = json.optJSONObject("Body") ?: json
                            row.put("fields", JSONArray(body.keys().asSequence().toList()))
                            val id = SpicyRecording.responseId(json)
                            row.put("recordingId", id ?: JSONObject.NULL)
                            if (response.ok) {
                                val parsed = parseSpicy(json, want.duration)
                                row.put("lines", parsed.lines.size).put("fitsDuration", SpicyRecording.fitsDuration(parsed, want.duration))
                                if (id != null) {
                                    val track = SpicyRecording.entity(Net.fetch("https://open.spotify.com/embed/track/$id").text)
                                    row.put("identityMatches", track != null && SpicyRecording.matches(want, id, track))
                                }
                            }
                        }
                    } catch (e: Exception) { row.put("errorType", e.javaClass.simpleName) }
                    results.put(row)
                }
                output.put("results", results)
            }
        } catch (e: Exception) { output.put("errorType", e.javaClass.simpleName) }
        File(Vibe.app.cacheDir, "spicy-file-probe.json").writeText(output.toString(2))
        android.util.Log.i("VV-probe", "Spicy file probe complete; results in app cache")
    } }
}
