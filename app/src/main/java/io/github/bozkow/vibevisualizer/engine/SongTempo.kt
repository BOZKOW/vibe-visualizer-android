package io.github.bozkow.vibevisualizer.engine

import io.github.bozkow.vibevisualizer.core.Net
import io.github.bozkow.vibevisualizer.core.Vibe
import io.github.bozkow.vibevisualizer.core.wall
import io.github.bozkow.vibevisualizer.lyrics.SongMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder

/** ReccoBeats' documented public catalog API. Sends only identifiers or song text, never audio/files. */
internal object SongTempo {
    private const val BASE = "https://api.reccobeats.com/v1/"
    private const val DAY = 86_400_000L
    private data class Entry(val at: Long, val bpm: Double?)
    private val cache = LinkedHashMap<String,Entry>()
    private val lock = Mutex()
    private var loaded = false
    private var nextRequest = 0L
    private var blockedUntil = 0L
    private val file get() = File(Vibe.app.cacheDir,"song-tempo-v1.json")
    private fun enc(s: String) = URLEncoder.encode(s,"UTF-8")

    suspend fun find(m: SongMeta, network: Boolean): Double? = lock.withLock {
        withContext(Dispatchers.IO) {
            if (!loaded) {
                loaded = true
                runCatching {
                    val json = JSONObject(file.readText())
                    json.keys().forEach { key ->
                        val row = json.getJSONObject(key); val at = row.optLong("at"); val bpm = row.optDouble("bpm")
                        if (wall()-at in 0..7*DAY && bpm.isFinite() && bpm in 30.0..300.0) cache[key] = Entry(at,bpm)
                    }
                }
            }
            val key = tempoKey(m)
            cache[key]?.takeIf { wall()-it.at in 0..(if (it.bpm == null) 300_000L else 7*DAY) }?.let { return@withContext it.bpm }
            if (!network || wall() < blockedUntil) return@withContext null
            val result = try { lookup(m) } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) {
                blockedUntil = maxOf(blockedUntil,wall()+60_000); return@withContext null
            }
            cache[key] = Entry(wall(),result)
            while (cache.size > 200) cache.remove(cache.keys.first())
            if (result != null) runCatching {
                val json = JSONObject()
                cache.forEach { (k,v) -> if (v.bpm != null) json.put(k,JSONObject().put("at",v.at).put("bpm",v.bpm)) }
                val temp = File(file.path+".tmp"); temp.writeText(json.toString()); temp.renameTo(file)
            }
            result
        }
    }

    private suspend fun get(path: String): JSONObject? {
        delay((nextRequest-wall()).coerceAtLeast(0))
        nextRequest = wall()+600
        val r = Net.fetch(BASE+path,"tempo")
        if (r.code == 429) {
            val seconds = r.headers["Retry-After"]?.toLongOrNull()?.coerceIn(1,86400) ?: 60
            blockedUntil = wall()+seconds*1000
            throw IllegalStateException("Tempo service rate limited")
        }
        if (r.code == 404) return null
        check(r.ok) { "Tempo service unavailable" }
        return r.json() ?: error("Invalid tempo response")
    }

    private suspend fun lookup(m: SongMeta): Double? {
        validTempoSpotifyId(m.id)?.let { id ->
            get("audio-features?ids="+enc(id))?.let { catalogueTempo(tempoFeatures(it),spotifyId = id) }?.let { return it }
        }
        validTempoIsrc(m.isrc)?.let { isrc ->
            get("audio-features?ids="+enc(isrc))?.let { catalogueTempo(tempoFeatures(it),isrc = isrc) }?.let { return it }
        }
        if (m.title.isBlank() || m.artist.isBlank()) return null
        // The searchText endpoint searches titles; appending the artist changes the title query.
        for (page in 0..1) {
            val json = get("track/search?searchText="+enc(m.title)+"&size=40&page=$page") ?: return null
            // Missing duration plus a broad, incomplete search cannot identify one recording safely.
            if (m.duration <= 0 && json.optInt("totalPages",1) > 1) return null
            val rows = matchingTempoTracks(m,tempoTracks(json))
            if (rows.isNotEmpty()) {
                val ids = rows.map { it.id }.toSet()
                return get("audio-features?ids="+enc(ids.joinToString(",")))?.let { catalogueTempo(tempoFeatures(it),ids = ids) }
            }
            if (json.optInt("totalPages",1) <= page+1) break
        }
        return null
    }
}
