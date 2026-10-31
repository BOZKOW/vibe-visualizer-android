package io.github.bozkow.vibevisualizer.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spicy Lyrics' status page and feed as they were on 2026-10-31 (all online, a maintenance planned for Nov 1). */
class SpicyStatusTest {
    private fun res(n: String) = javaClass.classLoader!!.getResource("spicystatus/$n")!!.readText()
    private val utc = java.util.TimeZone.getTimeZone("UTC")
    private fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int) = java.util.Calendar.getInstance(utc).apply { clear(); set(y, mo - 1, d, h, mi) }.timeInMillis

    @Test
    fun `all online, with the planned maintenance and its start`() {
        val (now, plan) = SpicyStatus.read(res("page.txt"), res("feed.rss"), at(2026, 10, 31, 18, 30))
        assertNull(now)
        assertEquals("Generic Backend Maintenance", plan!!.name)
        assertEquals(at(2026, 11, 1, 14, 30), plan.at)
        assertTrue(plan.maint)
    }

    @Test
    fun `a planned maintenance long past is no longer planned`() {
        assertNull(SpicyStatus.read(res("page.txt"), res("feed.rss"), at(2026, 11, 3, 12, 0)).second)
    }

    @Test
    fun `down, the newest report that touches the lyrics names it`() {
        val page = "Spicy Lyrics status Some services are down API Downtime"
        val feed = """<rss><channel>
            <item><title>Maintenance: Unison EU SLNode Maintenance</title><link>https://status.spicylyrics.org/maintenance/2</link><pubDate>Sat, 31 Oct 2026 15:15:32 -0000</pubDate><category>Maintenance</category><description>Requests will fall back to the US node, service will not be impacted.</description></item>
            <item><title>API is experiencing issues</title><link>https://status.spicylyrics.org/incident/1</link><pubDate>Sat, 31 Oct 2026 15:00:00 -0000</pubDate><category>Incident</category><description>We're noticing failed requests</description></item>
            </channel></rss>"""
        val now = SpicyStatus.read(page, feed, at(2026, 10, 31, 15, 30)).first!!
        assertEquals("API is experiencing issues", now.name)
        assertEquals(false, now.maint)
        assertEquals("https://status.spicylyrics.org/incident/1", now.link)
    }

    @Test
    fun `not the status page (a captive portal or an error page) says nothing`() {
        assertNull(SpicyStatus.read("<html>Sign in to the Wi-Fi</html>", "", at(2026, 10, 31, 15, 30)).first)
    }
}
