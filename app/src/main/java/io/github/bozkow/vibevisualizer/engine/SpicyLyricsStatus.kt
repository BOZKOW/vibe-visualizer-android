package io.github.bozkow.vibevisualizer.engine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.bozkow.vibevisualizer.core.Net
import io.github.bozkow.vibevisualizer.core.Server
import io.github.bozkow.vibevisualizer.core.Vibe
import io.github.bozkow.vibevisualizer.core.wall
import kotlinx.coroutines.launch

/** Spicy Lyrics' own status page (status.spicylyrics.org, on Better Stack; public, no key): read every 10 minutes while
 *  Spicy Lyrics is one of your sources and the app is on screen, and at once after a lookup there fails, so the top of
 *  the screen can say when it's down or under maintenance, and Settings what's planned. The page's headline ("All
 *  services are online", or not) says whether something's wrong now; its feed names it; the page has a planned
 *  maintenance's start. */
object SpicyStatus {
    const val PAGE = "https://status.spicylyrics.org"
    private const val FEED = "$PAGE/feed.rss"

    /** What's wrong now ([maint]: maintenance, else an outage) or what's planned ([at]: its start, ms; 0 unknown). */
    class Event(val name: String, val text: String, val link: String, val maint: Boolean, val at: Long = 0)

    var now by mutableStateOf<Event?>(null); private set       // down or under maintenance now
    var planned by mutableStateOf<Event?>(null); private set   // the next planned maintenance
    var checkedAt by mutableLongStateOf(0L); private set       // 0: not read yet (or couldn't be)
    private var askedAt = 0L

    /** Spicy Lyrics is one of the sources on (it needs your server). */
    fun used() = Server.set && Lyr.provList().any { it.k == "spicy" && it.on }

    /** Every few frames: read again when it's been 10 minutes. */
    fun tick() { if (E.visible && used() && wall() - askedAt > 10 * 60_000) check() }

    /** A lookup at Spicy Lyrics failed: is it them? (At most once a minute.) */
    fun failed() { if (used() && wall() - askedAt > 60_000) check() }

    /** Settings opened: fresh if it's been a couple of minutes. */
    fun fresh() { if (wall() - askedAt > 2 * 60_000) check() }

    fun check() {
        askedAt = wall()
        Vibe.scope.launch {
            val page = runCatching { Net.fetch(PAGE, "spicystatus").takeIf { it.ok }?.text }.getOrNull() ?: return@launch
            val feed = runCatching { Net.fetch(FEED, "spicystatus").takeIf { it.ok }?.text }.getOrNull() ?: ""
            val r = read(page, feed, wall())
            now = r.first; planned = r.second; checkedAt = wall()
        }
    }

    private class Item(val title: String, val link: String, val cat: String, val text: String, val date: Long)

    /** The page and the feed as (what's wrong now, the next planned maintenance), as of [nowMs]. */
    internal fun read(page: String, feed: String, nowMs: Long): Pair<Event?, Event?> {
        val text = plain(page)
        val items = feed.split("<item>").drop(1).mapNotNull { x ->
            fun tag(t: String) = Regex("<$t>(.*?)</$t>", RegexOption.DOT_MATCHES_ALL).find(x)?.groupValues?.get(1)?.let { plain(it) } ?: ""
            val date = runCatching { java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", java.util.Locale.US).parse(tag("pubDate"))?.time }.getOrNull() ?: 0L
            Item(tag("title"), tag("link"), tag("category"), tag("description"), date).takeIf { it.title.isNotEmpty() }
        }
        fun name(t: String) = t.removePrefix("Scheduled maintenance: ").removePrefix("Maintenance: ").removePrefix("Incident: ").trim()
        // a planned maintenance: announced, its start on the page, not begun more than a few hours ago
        val plan = items.firstOrNull { it.title.startsWith("Scheduled maintenance") }?.let { p ->
            val n = name(p.title)
            val at = Regex(Regex.escape(n) + """ ([A-Z][a-z]{2} \d{1,2}, \d{4} at \d{1,2}:\d{2}[ap]m) UTC""").find(text)?.groupValues?.get(1)?.let { s ->
                runCatching { java.text.SimpleDateFormat("MMM d, yyyy 'at' h:mma", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.parse(s)?.time }.getOrNull()
            } ?: 0L
            if (at > 0 && at < nowMs - 6 * 3600_000L) null else if (at == 0L && p.date < nowMs - 7 * 86400_000L) null
            else Event(n, p.text, p.link, true, at)
        }
        // something's wrong now: the headline isn't "All services are online"; the newest report that touches the
        // lyrics names it (one that says the service won't be affected, as a node falling back to another, is left out)
        if (text.contains("All services are online", ignoreCase = true)) return null to plan
        if (!text.contains("Spicy Lyrics", ignoreCase = true)) return null to plan   // not the page it should be
        val last = items.firstOrNull { !it.title.startsWith("Scheduled maintenance") && !Regex("not be (impacted|affected)|no impact", RegexOption.IGNORE_CASE).containsMatchIn(it.text) }
        val maint = last?.cat.equals("Maintenance", ignoreCase = true) || text.contains("under maintenance", ignoreCase = true)
        val ev = Event(last?.let { name(it.title) } ?: if (maint) "Maintenance" else "Spicy Lyrics is having trouble", last?.text ?: "", last?.link ?: PAGE, maint)
        return ev to plan?.takeIf { it.name != ev.name }
    }

    private fun plain(s: String) = s.replace(Regex("<script.*?</script>|<style.*?</style>", RegexOption.DOT_MATCHES_ALL), " ")
        .replace(Regex("<[^>]+>"), " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'")
        .replace(Regex("\\s+"), " ").trim()
}
